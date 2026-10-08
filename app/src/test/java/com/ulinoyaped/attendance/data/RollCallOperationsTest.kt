package com.ulinoyaped.attendance.data

import org.junit.Assert.*
import org.junit.Test

class RollCallOperationsTest {
    private val students = listOf(Student("a", "张三", "2026001"), Student("b", "李四", "2026002"), Student("c", "王五", "003"))

    @Test fun batchDefaultsProtectManualStatusAndReason() {
        val before = mapOf("a" to AttendanceMark(AttendanceStatus.LEAVE, "比赛"), "b" to AttendanceMark(AttendanceStatus.LATE, "堵车"))
        val after = batchAttendanceMarks(before, setOf("a", "b", "c"), AttendanceMark(AttendanceStatus.PRESENT, "不应保留"))
        assertEquals(before["a"], after["a"])
        assertEquals(before["b"], after["b"])
        assertEquals(AttendanceMark(AttendanceStatus.PRESENT), after["c"])
        assertEquals(2, before.size) // Undo snapshot wasn't mutated.
    }

    @Test fun explicitOverwriteAffectsOnlySelectedStudents() {
        val before = mapOf("a" to AttendanceMark(AttendanceStatus.LEAVE, "比赛"), "b" to AttendanceMark(AttendanceStatus.ABSENT, "旧原因"))
        val after = batchAttendanceMarks(before, setOf("a", "c"), AttendanceMark(AttendanceStatus.LATE, "晚到"), overwrite = true)
        assertEquals(AttendanceMark(AttendanceStatus.LATE, "晚到"), after["a"])
        assertEquals(AttendanceMark(AttendanceStatus.LATE, "晚到"), after["c"])
        assertEquals(before["b"], after["b"])
        assertEquals("比赛", before["a"]?.reason)
    }

    @Test fun reasonOnlyDoesNotInventStatusesOrReasonsForPresentStudents() {
        val before = mapOf("a" to AttendanceMark(AttendanceStatus.EXEMPT, "原原因"), "b" to AttendanceMark(AttendanceStatus.PRESENT))
        val after = batchAttendanceReasons(before, setOf("a", "b", "c"), "新原因")
        assertEquals(AttendanceStatus.EXEMPT, after["a"]?.status)
        assertEquals("新原因", after["a"]?.reason)
        assertEquals(before["b"], after["b"])
        assertFalse(after.containsKey("c"))
        assertEquals("", batchAttendanceReasons(after, setOf("a"), "")["a"]?.reason)
    }

    @Test fun filtersSearchNameAndNumberWithoutChangingData() {
        val marks = mapOf("a" to AttendanceMark(AttendanceStatus.LEAVE), "b" to AttendanceMark(AttendanceStatus.PRESENT))
        val original = marks.toMap()
        assertEquals(listOf(students[0]), filterRollCallStudents(students, marks, " 张 ", RollCallFilter.ALL))
        assertEquals(listOf(students[1]), filterRollCallStudents(students, marks, "6002", RollCallFilter.MARKED))
        assertEquals(listOf(students[2]), filterRollCallStudents(students, marks, "", RollCallFilter.UNMARKED))
        assertEquals(listOf(students[0]), filterRollCallStudents(students, marks, "", RollCallFilter.EXCEPTIONS))
        assertTrue(filterRollCallStudents(students, marks, "王", RollCallFilter.ABSENT).isEmpty())
        assertEquals(original, marks)
    }

    @Test fun exceptionIncludesAllFourNonPresentStatusesAndNoUnmarked() {
        assertEquals(setOf(AttendanceStatus.LATE, AttendanceStatus.LEAVE, AttendanceStatus.ABSENT, AttendanceStatus.EXEMPT),
            AttendanceStatus.entries.filter { it.isException() }.toSet())
    }
}
