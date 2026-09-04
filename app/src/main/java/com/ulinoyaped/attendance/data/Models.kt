package com.ulinoyaped.attendance.data

data class Student(
    val id: String,
    val name: String,
    val studentNumber: String = "",
)

data class ClassGroup(
    val id: String,
    val name: String,
    val students: List<Student> = emptyList(),
)

enum class AttendanceStatus(val label: String) {
    UNMARKED("未点"),
    PRESENT("到"),
    LATE("迟到"),
    LEAVE("请假"),
    ABSENT("缺勤"),
}

data class AttendanceEntry(
    val studentId: String,
    val studentName: String,
    val studentNumber: String,
    val status: AttendanceStatus,
    val reason: String = "",
)

data class AttendanceSession(
    val id: String,
    val classId: String,
    val createdAt: Long,
    val entries: List<AttendanceEntry>,
)

enum class GestureAction(val label: String) {
    EDIT("打开状态选择"),
    PRESENT("标记到场"),
    LATE("标记迟到"),
    LEAVE("标记请假"),
    ABSENT("标记缺勤"),
    CLEAR("清除标记"),
}

data class AppSettings(
    val absenceReasons: List<String> = listOf("病假", "事假", "公假", "早退", "其他"),
    val defaultReason: String = "",
    val defaultStatus: AttendanceStatus = AttendanceStatus.PRESENT,
    val longPressAction: GestureAction = GestureAction.EDIT,
    val swipeLeftAction: GestureAction = GestureAction.ABSENT,
    val swipeRightAction: GestureAction = GestureAction.PRESENT,
)

data class ImportedStudent(
    val name: String,
    val studentNumber: String,
)
