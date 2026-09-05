package com.ulinoyaped.attendance.data

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

class InputSafetyTest {
    @Test fun csvFormulaPrefixesAreNeutralized() {
        for (value in listOf("=1+1", "+1", "-2", "@SUM(A1)", "  =1+1", "=SUM(1,1)")) {
            assertTrue(safeCsvCell(value).startsWith("\"'"))
        }
        assertEquals("\"001\"", safeCsvCell("001"))
        assertEquals("\"A,\"\"B\"\"\"", safeCsvCell("A,\"B\""))
    }

    @Test fun quotedNewlinesNeverCreateExtraStudents() {
        assertThrows(IllegalArgumentException::class.java) { parseStudentList("学号,姓名\n001,\"Alice\n999,Mallory\"") }
        assertThrows(IllegalArgumentException::class.java) { parseStudentList("001,\"Alice\rMallory\"") }
        assertThrows(IllegalArgumentException::class.java) { parseStudentList("001,\"Alice") }
        assertThrows(IllegalArgumentException::class.java) { safeCsvCell("Alice\rMallory") }
    }

    @Test fun csvQuotesAndRecordSeparatorsRoundTrip() {
        val rows = parseStudentList("学号,姓名\r\n001,\"A,\"\"B\"\"\"\r\n002,Carol")
        assertEquals(2, rows.size)
        assertEquals(ImportedStudent("A,\"B\"", "001"), rows[0])
        assertEquals("002", rows[1].studentNumber)
    }

    @Test fun actualStreamLengthIsLimited() {
        assertEquals("abcd", "abcd".byteInputStream().use { it.readBoundedText(4) })
        assertThrows(IllegalArgumentException::class.java) { "abcde".byteInputStream().use { it.readBoundedText(4) } }
        assertThrows(IllegalArgumentException::class.java) { parseStudentList("x".repeat(MAX_ROSTER_BYTES + 1)) }
    }

    @Test fun excessiveJsonNestingIsRejectedBeforeParsing() {
        assertThrows(IllegalArgumentException::class.java) {
            requireShallowJson("[".repeat(33) + "0" + "]".repeat(33))
        }
        requireShallowJson("""{"note":"[not nesting]"}""")
    }

    private fun backup(): JSONObject = JSONObject("""{
      "formatVersion":1,
      "classes":[{"id":"c","name":"Demo","students":[{"id":"s","name":"Alice","studentNumber":"001"}]}],
      "sessions":[{"id":"h","classId":"c","createdAt":1,"entries":[{"studentId":"removed-student","studentName":"Bob","status":"PRESENT"}]}],
      "rollCallDrafts":[], "settings":{"absenceReasons":["病假"],"defaultStatus":"PRESENT"}
    }""")

    @Test fun historySnapshotsMayReferToRemovedStudents() { validateBackup(backup()) }

    @Test fun duplicateIdsAndDanglingReferencesAreRejected() {
        val duplicateClass = backup().apply { getJSONArray("classes").put(getJSONArray("classes").getJSONObject(0)) }
        assertThrows(IllegalArgumentException::class.java) { validateBackup(duplicateClass) }
        val duplicateStudent = backup().apply {
            val students = getJSONArray("classes").getJSONObject(0).getJSONArray("students")
            students.put(students.getJSONObject(0))
        }
        assertThrows(IllegalArgumentException::class.java) { validateBackup(duplicateStudent) }
        val dangling = backup().apply { getJSONArray("sessions").getJSONObject(0).put("classId", "missing") }
        assertThrows(IllegalArgumentException::class.java) { validateBackup(dangling) }
    }

    @Test fun unknownAndUnmarkedStatusesAreRejected() {
        for (status in listOf("UNKNOWN", "UNMARKED")) {
            val root = backup()
            root.getJSONArray("sessions").getJSONObject(0).getJSONArray("entries").getJSONObject(0).put("status", status)
            assertThrows(IllegalArgumentException::class.java) { validateBackup(root) }
        }
        val root = backup().apply { getJSONObject("settings").put("defaultStatus", "UNMARKED") }
        assertThrows(IllegalArgumentException::class.java) { validateBackup(root) }
    }
}
