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
    val attendanceSettings: ClassAttendanceSettings? = null,
)

enum class AttendanceStatus(val label: String) {
    UNMARKED("未点"),
    PRESENT("到"),
    LATE("迟到"),
    LEAVE("请假"),
    ABSENT("缺勤"),
    EXEMPT("不参与"),
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
    EXEMPT("标记不参与"),
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
    REMOVE("减号"),
}

enum class StatusColorOption(val label: String) {
    PRIMARY("主题色"),
    GREEN("绿色"),
    AMBER("琥珀色"),
    BLUE("蓝色"),
    RED("红色"),
    PURPLE("紫色"),
    TEAL("青色"),
    GRAY("灰色"),
}

enum class HistoryTitleMode(val label: String) {
    CLASS_NAME("班级名称"),
    TIME("点名时间"),
}

data class ClassAttendanceSettings(
    val defaultReason: String = "",
    val defaultStatus: AttendanceStatus = AttendanceStatus.PRESENT,
    val longPressAction: GestureAction = GestureAction.EDIT,
    val swipeLeftAction: GestureAction = GestureAction.ABSENT,
    val swipeRightAction: GestureAction = GestureAction.PRESENT,
    val groupResultsByStatus: Boolean = true,
)

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
    val exemptIcon: StatusIconOption = StatusIconOption.REMOVE,
    val presentColor: StatusColorOption = StatusColorOption.GREEN,
    val lateColor: StatusColorOption = StatusColorOption.AMBER,
    val leaveColor: StatusColorOption = StatusColorOption.BLUE,
    val absentColor: StatusColorOption = StatusColorOption.RED,
    val exemptColor: StatusColorOption = StatusColorOption.GRAY,
    val groupResultsByStatus: Boolean = true,
    val historyTitleMode: HistoryTitleMode = HistoryTitleMode.CLASS_NAME,
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
    AttendanceStatus.EXEMPT -> exemptIcon
    AttendanceStatus.UNMARKED -> StatusIconOption.HELP
}

fun AppSettings.colorFor(status: AttendanceStatus): StatusColorOption = when (status) {
    AttendanceStatus.PRESENT -> presentColor
    AttendanceStatus.LATE -> lateColor
    AttendanceStatus.LEAVE -> leaveColor
    AttendanceStatus.ABSENT -> absentColor
    AttendanceStatus.EXEMPT -> exemptColor
    AttendanceStatus.UNMARKED -> StatusColorOption.GRAY
}

fun AppSettings.forClass(group: ClassGroup): AppSettings {
    val custom = group.attendanceSettings ?: return this
    return copy(
        defaultReason = custom.defaultReason.takeIf { it in absenceReasons }.orEmpty(),
        defaultStatus = custom.defaultStatus,
        longPressAction = custom.longPressAction,
        swipeLeftAction = custom.swipeLeftAction,
        swipeRightAction = custom.swipeRightAction,
        groupResultsByStatus = custom.groupResultsByStatus,
    )
}

fun AppSettings.toClassSettings(): ClassAttendanceSettings = ClassAttendanceSettings(
    defaultReason = defaultReason,
    defaultStatus = defaultStatus,
    longPressAction = longPressAction,
    swipeLeftAction = swipeLeftAction,
    swipeRightAction = swipeRightAction,
    groupResultsByStatus = groupResultsByStatus,
)

data class ImportedStudent(
    val name: String,
    val studentNumber: String,
)
