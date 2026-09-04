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

enum class StatusIconOption(val label: String) {
    CHECK("对勾"),
    PERSON("人物"),
    SCHEDULE("时钟"),
    EVENT_BUSY("日历叉号"),
    CLOSE("叉号"),
    WARNING("警告"),
    STAR("星标"),
    HELP("问号"),
}

data class AppSettings(
    val absenceReasons: List<String> = listOf("病假", "事假", "公假", "早退", "其他"),
    val defaultReason: String = "",
    val defaultStatus: AttendanceStatus = AttendanceStatus.PRESENT,
    val longPressAction: GestureAction = GestureAction.EDIT,
    val swipeLeftAction: GestureAction = GestureAction.ABSENT,
    val swipeRightAction: GestureAction = GestureAction.PRESENT,
    val presentIcon: StatusIconOption = StatusIconOption.CHECK,
    val lateIcon: StatusIconOption = StatusIconOption.SCHEDULE,
    val leaveIcon: StatusIconOption = StatusIconOption.EVENT_BUSY,
    val absentIcon: StatusIconOption = StatusIconOption.CLOSE,
    val exportHeader: Boolean = true,
    val exportSummary: Boolean = true,
    val exportPresentStudents: Boolean = true,
    val exportStudentNumber: Boolean = true,
    val exportReason: Boolean = true,
)

fun AppSettings.iconFor(status: AttendanceStatus): StatusIconOption = when (status) {
    AttendanceStatus.PRESENT -> presentIcon
    AttendanceStatus.LATE -> lateIcon
    AttendanceStatus.LEAVE -> leaveIcon
    AttendanceStatus.ABSENT -> absentIcon
    AttendanceStatus.UNMARKED -> StatusIconOption.HELP
}

data class ImportedStudent(
    val name: String,
    val studentNumber: String,
)
