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
    val situations: List<ClassSituation> = emptyList(),
)

data class SituationAssignment(
    val studentId: String,
    val status: AttendanceStatus,
    val reason: String = "",
)

data class ClassSituation(
    val id: String,
    val name: String,
    val assignments: List<SituationAssignment> = emptyList(),
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

data class RollCallDraft(
    val id: String,
    val classId: String,
    val createdAt: Long,
    val updatedAt: Long,
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

enum class DisplayOption {
    STUDENT_NUMBERS,
    CLASS_STUDENT_COUNT,
    CLASS_OPERATION_HINT,
    ROLL_CALL_PROGRESS,
    OPERATION_HINT,
    STATUS_BUTTON,
    REASONS_IN_ROLL_CALL,
    RESULT_SUMMARY,
    EMPTY_RESULT_GROUPS,
    HISTORY_STATISTICS,
    CONFIRM_INCOMPLETE,
    COMPACT_ROLL_CALL,
    COLLAPSE_PRESENT,
    COLLAPSE_LATE,
    COLLAPSE_LEAVE,
    COLLAPSE_ABSENT,
    COLLAPSE_EXEMPT,
    EXPORT_LATE,
    EXPORT_LEAVE,
    EXPORT_ABSENT,
    EXPORT_EXEMPT,
}

data class ClassAttendanceSettings(
    val collapsedResultStatuses: Set<AttendanceStatus> = emptySet(),
    val defaultReason: String = "",
    val defaultStatus: AttendanceStatus = AttendanceStatus.PRESENT,
    val longPressAction: GestureAction = GestureAction.EDIT,
    val swipeLeftAction: GestureAction = GestureAction.ABSENT,
    val swipeRightAction: GestureAction = GestureAction.PRESENT,
    val groupResultsByStatus: Boolean = true,
    val showStudentNumbers: Boolean = true,
    val showRollCallProgress: Boolean = true,
    val showOperationHint: Boolean = true,
    val showStatusButton: Boolean = true,
    val showReasonsInRollCall: Boolean = true,
    val showResultSummary: Boolean = true,
    val showEmptyResultGroups: Boolean = false,
    val confirmIncompleteAttendance: Boolean = true,
    val compactRollCallRows: Boolean = false,
)

val resultCollapseOptions: Map<DisplayOption, AttendanceStatus> = linkedMapOf(
    DisplayOption.COLLAPSE_PRESENT to AttendanceStatus.PRESENT,
    DisplayOption.COLLAPSE_LATE to AttendanceStatus.LATE,
    DisplayOption.COLLAPSE_LEAVE to AttendanceStatus.LEAVE,
    DisplayOption.COLLAPSE_ABSENT to AttendanceStatus.ABSENT,
    DisplayOption.COLLAPSE_EXEMPT to AttendanceStatus.EXEMPT,
)

data class AppSettings(
    val collapsedResultStatuses: Set<AttendanceStatus> = emptySet(),
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
    val showStudentNumbers: Boolean = true,
    val showClassStudentCount: Boolean = true,
    val showClassOperationHint: Boolean = true,
    val showRollCallProgress: Boolean = true,
    val showOperationHint: Boolean = true,
    val showStatusButton: Boolean = true,
    val showReasonsInRollCall: Boolean = true,
    val showResultSummary: Boolean = true,
    val showEmptyResultGroups: Boolean = false,
    val showHistoryStatistics: Boolean = true,
    val confirmIncompleteAttendance: Boolean = true,
    val compactRollCallRows: Boolean = false,
    val exportHeader: Boolean = true,
    val exportSummary: Boolean = true,
    val exportPresentStudents: Boolean = true,
    val exportLateStudents: Boolean = true,
    val exportLeaveStudents: Boolean = true,
    val exportAbsentStudents: Boolean = true,
    val exportExemptStudents: Boolean = true,
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
        collapsedResultStatuses = custom.collapsedResultStatuses,
        showStudentNumbers = custom.showStudentNumbers,
        showRollCallProgress = custom.showRollCallProgress,
        showOperationHint = custom.showOperationHint,
        showStatusButton = custom.showStatusButton,
        showReasonsInRollCall = custom.showReasonsInRollCall,
        showResultSummary = custom.showResultSummary,
        showEmptyResultGroups = custom.showEmptyResultGroups,
        confirmIncompleteAttendance = custom.confirmIncompleteAttendance,
        compactRollCallRows = custom.compactRollCallRows,
    )
}

fun AppSettings.toClassSettings(): ClassAttendanceSettings = ClassAttendanceSettings(
    defaultReason = defaultReason,
    defaultStatus = defaultStatus,
    longPressAction = longPressAction,
    swipeLeftAction = swipeLeftAction,
    swipeRightAction = swipeRightAction,
    groupResultsByStatus = groupResultsByStatus,
    collapsedResultStatuses = collapsedResultStatuses,
    showStudentNumbers = showStudentNumbers,
    showRollCallProgress = showRollCallProgress,
    showOperationHint = showOperationHint,
    showStatusButton = showStatusButton,
    showReasonsInRollCall = showReasonsInRollCall,
    showResultSummary = showResultSummary,
    showEmptyResultGroups = showEmptyResultGroups,
    confirmIncompleteAttendance = confirmIncompleteAttendance,
    compactRollCallRows = compactRollCallRows,
)

data class ImportedStudent(
    val name: String,
    val studentNumber: String,
)
