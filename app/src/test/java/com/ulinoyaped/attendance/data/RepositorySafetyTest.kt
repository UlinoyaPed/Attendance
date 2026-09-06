package com.ulinoyaped.attendance.data

import android.content.SharedPreferences
import java.lang.reflect.Proxy
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class RepositorySafetyTest {
    private class Store {
        val values = java.util.concurrent.ConcurrentHashMap<String, Any>()
        val commits = mutableListOf<Map<String, Any>>()
        @Volatile var fail = false
        val preferences = Proxy.newProxyInstance(
            SharedPreferences::class.java.classLoader, arrayOf(SharedPreferences::class.java),
        ) { _, method, args ->
            when (method.name) {
                "getString" -> values[args!![0] as String] ?: args[1]
                "getAll" -> values.toMap()
                "contains" -> values.containsKey(args!![0] as String)
                "edit" -> editor()
                else -> null
            }
        } as SharedPreferences

        private fun editor(): SharedPreferences.Editor {
            val changes = mutableMapOf<String, Any>()
            return Proxy.newProxyInstance(
                SharedPreferences.Editor::class.java.classLoader, arrayOf(SharedPreferences.Editor::class.java),
            ) { proxy, method, args ->
                when (method.name) {
                    "putString" -> { changes[args!![0] as String] = args[1]!!; proxy }
                    "commit" -> if (fail) false else {
                        values.putAll(changes)
                        commits.add(changes.toMap())
                        true
                    }
                    else -> error("Unexpected editor method: ${method.name}")
                }
            } as SharedPreferences.Editor
        }
    }

    @Test fun invalidBackupDoesNotWriteAnything() {
        val store = Store()
        val repository = AttendanceRepository(store.preferences)
        repository.addClass("Demo")
        assertTrue(repository.awaitSaved())
        val before = store.values.toMap()
        val bad = JSONObject(repository.exportBackup())
        bad.getJSONArray("classes").put(bad.getJSONArray("classes").getJSONObject(0))
        assertFalse(repository.importBackup(bad.toString()))
        assertEquals(before, store.values.toMap())
    }

    @Test fun finishingPersistsHistoryAndClearsDraftTogether() {
        val store = Store()
        val repository = AttendanceRepository(store.preferences)
        repository.addClass("Demo")
        val classId = repository.classes.value.single().id
        repository.addStudent(classId, "Alice", "001")
        val student = repository.classes.value.single().students.single()
        val entries = listOf(AttendanceEntry(student.id, student.name, student.studentNumber, AttendanceStatus.PRESENT))
        repository.saveRollCallDraft(classId, entries)
        assertTrue(repository.awaitSaved())
        val before = store.commits.size
        repository.saveSession(classId, entries)
        assertTrue(repository.awaitSaved())
        assertEquals(before + 1, store.commits.size)
        val snapshot = JSONObject(store.values.getValue("verified_snapshot") as String)
        assertEquals(1, snapshot.getJSONArray("sessions").length())
        assertEquals(0, snapshot.getJSONArray("rollCallDrafts").length())
    }

    @Test fun corruptDataIsPreservedAndWritesStop() {
        val store = Store()
        store.values["classes"] = "corrupt original"
        val repository = AttendanceRepository(store.preferences)
        assertNotNull(repository.storageError.value)
        repository.addClass("Must not replace original")
        assertFalse(repository.awaitSaved())
        assertEquals("corrupt original", store.values["classes"])
        assertTrue(store.commits.isEmpty())
    }

    @Test fun failedWriteIsReportedAndRecoveryUsesConfirmedSnapshot() {
        val store = Store()
        val repository = AttendanceRepository(store.preferences)
        repository.addClass("Saved")
        assertTrue(repository.awaitSaved())
        store.fail = true
        repository.addClass("Unsaved")
        assertFalse(repository.awaitSaved())
        assertNotNull(repository.storageError.value)
        store.fail = false
        assertTrue(repository.recoverPrevious())
        assertEquals(listOf("Saved"), repository.classes.value.map { it.name })
    }

    @Test fun duplicateReasonsAreNormalizedOnRestore() {
        val repository = AttendanceRepository(Store().preferences)
        val root = JSONObject(repository.exportBackup())
        root.getJSONObject("settings").put("absenceReasons", org.json.JSONArray(listOf("病假", "病假")))
        assertTrue(repository.importBackup(root.toString()))
        assertEquals(listOf("病假"), repository.settings.value.absenceReasons)
        assertEquals(1, JSONObject(repository.exportBackup()).getJSONObject("settings").getJSONArray("absenceReasons").length())
    }

    @Test fun removingSharedReasonUpdatesClassSettingsAtomically() {
        val store = Store()
        val repository = AttendanceRepository(store.preferences)
        repository.addClass("Demo")
        val id = repository.classes.value.single().id
        repository.setClassAttendanceSettings(id, ClassAttendanceSettings(defaultReason = "病假"))
        assertTrue(repository.awaitSaved())
        val before = store.commits.size
        repository.removeAbsenceReason("病假")
        assertTrue(repository.awaitSaved())
        assertEquals(before + 1, store.commits.size)
        assertEquals("", repository.classes.value.single().attendanceSettings?.defaultReason)
    }

    @Test fun situationsOverwriteOneStudentAtomically() {
        val repository = AttendanceRepository(Store().preferences)
        repository.addClass("Demo")
        val classId = repository.classes.value.single().id
        repository.addStudent(classId, "Alice", "001")
        repository.addSituation(classId, "长期安排")
        val group = repository.classes.value.single()
        val studentId = group.students.single().id
        val situationId = group.situations.single().id
        repository.setSituationAssignment(classId, situationId, studentId, AttendanceStatus.LEAVE, "集训")
        repository.setSituationAssignment(classId, situationId, studentId, AttendanceStatus.EXEMPT, "不参加本课程")
        val assignment = repository.classes.value.single().situations.single().assignments.single()
        assertEquals(AttendanceStatus.EXEMPT, assignment.status)
        assertEquals("不参加本课程", assignment.reason)
    }

    @Test fun multipleDraftsAreIndependentAndFinishingRemovesOnlyCurrentOne() {
        val repository = AttendanceRepository(Store().preferences)
        repository.addClass("Demo")
        val classId = repository.classes.value.single().id
        repository.addStudent(classId, "Alice", "001")
        val student = repository.classes.value.single().students.single()
        val entries = listOf(AttendanceEntry(student.id, student.name, student.studentNumber, AttendanceStatus.PRESENT))
        repository.saveRollCallDraft("draft-1", classId, 1, entries)
        repository.saveRollCallDraft("draft-2", classId, 2, entries.map { it.copy(status = AttendanceStatus.EXEMPT, reason = "长期请假") })
        repository.saveSession(classId, entries, "draft-1")
        assertEquals(listOf("draft-2"), repository.drafts.value.map { it.id })
    }
}
