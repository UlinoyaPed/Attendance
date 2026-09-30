package com.ulinoyaped.attendance.data

// App-wide theme, profile and backup storage remain global. All class behavior can inherit independently.
val classOverrideFields: Set<String> = setOf(
    "materialAvatarCompletesAll",
    "showMaterialProgress",
    "showMaterialOperationHint",
    "compactMaterialRows",
    "showMaterialStatusButton",
    "collapsedResultStatuses",
    "collapse.PRESENT", "collapse.LATE", "collapse.LEAVE", "collapse.ABSENT", "collapse.EXEMPT",
    "absenceReasons",
    "defaultReason",
    "defaultStatus",
    "longPressAction",
    "swipeLeftAction",
    "swipeRightAction",
    "presentIcon",
    "lateIcon",
    "leaveIcon",
    "absentIcon",
    "exemptIcon",
    "presentColor",
    "lateColor",
    "leaveColor",
    "absentColor",
    "exemptColor",
    "groupResultsByStatus",
    "historyTitleMode",
    "showStudentNumbers",
    "showClassStudentCount",
    "showClassOperationHint",
    "showRollCallProgress",
    "showOperationHint",
    "showStatusButton",
    "showReasonsInRollCall",
    "showResultSummary",
    "showEmptyResultGroups",
    "showHistoryStatistics",
    "confirmIncompleteAttendance",
    "compactRollCallRows",
    "exportHeader",
    "exportSummary",
    "exportPresentStudents",
    "exportLateStudents",
    "exportLeaveStudents",
    "exportAbsentStudents",
    "exportExemptStudents",
    "exportStudentNumber",
    "exportReason",
)

val legacyOverrideFields: Set<String> = setOf(
    "collapsedResultStatuses",
    "defaultReason",
    "defaultStatus",
    "longPressAction",
    "swipeLeftAction",
    "swipeRightAction",
    "groupResultsByStatus",
    "showStudentNumbers",
    "showRollCallProgress",
    "showOperationHint",
    "showStatusButton",
    "showReasonsInRollCall",
    "showResultSummary",
    "showEmptyResultGroups",
    "confirmIncompleteAttendance",
    "compactRollCallRows",
)

fun AppSettings.mergeClassSettings(values: AppSettings, fields: Set<String>): AppSettings {
    val merged = copy(
        materialAvatarCompletesAll = if ("materialAvatarCompletesAll" in fields) values.materialAvatarCompletesAll else materialAvatarCompletesAll,
        showMaterialProgress = if ("showMaterialProgress" in fields) values.showMaterialProgress else showMaterialProgress,
        showMaterialOperationHint = if ("showMaterialOperationHint" in fields) values.showMaterialOperationHint else showMaterialOperationHint,
        compactMaterialRows = if ("compactMaterialRows" in fields) values.compactMaterialRows else compactMaterialRows,
        showMaterialStatusButton = if ("showMaterialStatusButton" in fields) values.showMaterialStatusButton else showMaterialStatusButton,
        collapsedResultStatuses = AttendanceStatus.entries.filter { status ->
            status != AttendanceStatus.UNMARKED && if ("collapsedResultStatuses" in fields || "collapse.${status.name}" in fields)
                status in values.collapsedResultStatuses else status in collapsedResultStatuses
        }.toSet(),
        absenceReasons = if ("absenceReasons" in fields) values.absenceReasons else absenceReasons,
        defaultReason = if ("defaultReason" in fields) values.defaultReason else defaultReason,
        defaultStatus = if ("defaultStatus" in fields) values.defaultStatus else defaultStatus,
        longPressAction = if ("longPressAction" in fields) values.longPressAction else longPressAction,
        swipeLeftAction = if ("swipeLeftAction" in fields) values.swipeLeftAction else swipeLeftAction,
        swipeRightAction = if ("swipeRightAction" in fields) values.swipeRightAction else swipeRightAction,
        presentIcon = if ("presentIcon" in fields) values.presentIcon else presentIcon,
        lateIcon = if ("lateIcon" in fields) values.lateIcon else lateIcon,
        leaveIcon = if ("leaveIcon" in fields) values.leaveIcon else leaveIcon,
        absentIcon = if ("absentIcon" in fields) values.absentIcon else absentIcon,
        exemptIcon = if ("exemptIcon" in fields) values.exemptIcon else exemptIcon,
        presentColor = if ("presentColor" in fields) values.presentColor else presentColor,
        lateColor = if ("lateColor" in fields) values.lateColor else lateColor,
        leaveColor = if ("leaveColor" in fields) values.leaveColor else leaveColor,
        absentColor = if ("absentColor" in fields) values.absentColor else absentColor,
        exemptColor = if ("exemptColor" in fields) values.exemptColor else exemptColor,
        groupResultsByStatus = if ("groupResultsByStatus" in fields) values.groupResultsByStatus else groupResultsByStatus,
        historyTitleMode = if ("historyTitleMode" in fields) values.historyTitleMode else historyTitleMode,
        showStudentNumbers = if ("showStudentNumbers" in fields) values.showStudentNumbers else showStudentNumbers,
        showClassStudentCount = if ("showClassStudentCount" in fields) values.showClassStudentCount else showClassStudentCount,
        showClassOperationHint = if ("showClassOperationHint" in fields) values.showClassOperationHint else showClassOperationHint,
        showRollCallProgress = if ("showRollCallProgress" in fields) values.showRollCallProgress else showRollCallProgress,
        showOperationHint = if ("showOperationHint" in fields) values.showOperationHint else showOperationHint,
        showStatusButton = if ("showStatusButton" in fields) values.showStatusButton else showStatusButton,
        showReasonsInRollCall = if ("showReasonsInRollCall" in fields) values.showReasonsInRollCall else showReasonsInRollCall,
        showResultSummary = if ("showResultSummary" in fields) values.showResultSummary else showResultSummary,
        showEmptyResultGroups = if ("showEmptyResultGroups" in fields) values.showEmptyResultGroups else showEmptyResultGroups,
        showHistoryStatistics = if ("showHistoryStatistics" in fields) values.showHistoryStatistics else showHistoryStatistics,
        confirmIncompleteAttendance = if ("confirmIncompleteAttendance" in fields) values.confirmIncompleteAttendance else confirmIncompleteAttendance,
        compactRollCallRows = if ("compactRollCallRows" in fields) values.compactRollCallRows else compactRollCallRows,
        exportHeader = if ("exportHeader" in fields) values.exportHeader else exportHeader,
        exportSummary = if ("exportSummary" in fields) values.exportSummary else exportSummary,
        exportPresentStudents = if ("exportPresentStudents" in fields) values.exportPresentStudents else exportPresentStudents,
        exportLateStudents = if ("exportLateStudents" in fields) values.exportLateStudents else exportLateStudents,
        exportLeaveStudents = if ("exportLeaveStudents" in fields) values.exportLeaveStudents else exportLeaveStudents,
        exportAbsentStudents = if ("exportAbsentStudents" in fields) values.exportAbsentStudents else exportAbsentStudents,
        exportExemptStudents = if ("exportExemptStudents" in fields) values.exportExemptStudents else exportExemptStudents,
        exportStudentNumber = if ("exportStudentNumber" in fields) values.exportStudentNumber else exportStudentNumber,
        exportReason = if ("exportReason" in fields) values.exportReason else exportReason,

    )
    return merged.copy(defaultReason = merged.defaultReason.takeIf { it in merged.absenceReasons }.orEmpty())
}

fun AppSettings.withDisplayOption(option: DisplayOption, enabled: Boolean): AppSettings {
    resultCollapseOptions[option]?.let { status ->
        return copy(collapsedResultStatuses = if (enabled) collapsedResultStatuses + status else collapsedResultStatuses - status)
    }
    return when (option) {
        DisplayOption.STUDENT_NUMBERS -> copy(showStudentNumbers = enabled)
        DisplayOption.CLASS_STUDENT_COUNT -> copy(showClassStudentCount = enabled)
        DisplayOption.CLASS_OPERATION_HINT -> copy(showClassOperationHint = enabled)
        DisplayOption.ROLL_CALL_PROGRESS -> copy(showRollCallProgress = enabled)
        DisplayOption.OPERATION_HINT -> copy(showOperationHint = enabled)
        DisplayOption.STATUS_BUTTON -> copy(showStatusButton = enabled)
        DisplayOption.REASONS_IN_ROLL_CALL -> copy(showReasonsInRollCall = enabled)
        DisplayOption.RESULT_SUMMARY -> copy(showResultSummary = enabled)
        DisplayOption.EMPTY_RESULT_GROUPS -> copy(showEmptyResultGroups = enabled)
        DisplayOption.HISTORY_STATISTICS -> copy(showHistoryStatistics = enabled)
        DisplayOption.CONFIRM_INCOMPLETE -> copy(confirmIncompleteAttendance = enabled)
        DisplayOption.COMPACT_ROLL_CALL -> copy(compactRollCallRows = enabled)
        DisplayOption.EXPORT_LATE -> copy(exportLateStudents = enabled)
        DisplayOption.EXPORT_LEAVE -> copy(exportLeaveStudents = enabled)
        DisplayOption.EXPORT_ABSENT -> copy(exportAbsentStudents = enabled)
        DisplayOption.EXPORT_EXEMPT -> copy(exportExemptStudents = enabled)
        DisplayOption.MATERIAL_AVATAR -> copy(materialAvatarCompletesAll = enabled)
        DisplayOption.MATERIAL_PROGRESS -> copy(showMaterialProgress = enabled)
        DisplayOption.MATERIAL_HINT -> copy(showMaterialOperationHint = enabled)
        DisplayOption.MATERIAL_COMPACT -> copy(compactMaterialRows = enabled)
        DisplayOption.MATERIAL_STATUS_BUTTON -> copy(showMaterialStatusButton = enabled)
        else -> this
    }
}

fun DisplayOption.settingField(): String = when (this) {
    DisplayOption.STUDENT_NUMBERS -> "showStudentNumbers"
    DisplayOption.CLASS_STUDENT_COUNT -> "showClassStudentCount"
    DisplayOption.CLASS_OPERATION_HINT -> "showClassOperationHint"
    DisplayOption.ROLL_CALL_PROGRESS -> "showRollCallProgress"
    DisplayOption.OPERATION_HINT -> "showOperationHint"
    DisplayOption.STATUS_BUTTON -> "showStatusButton"
    DisplayOption.REASONS_IN_ROLL_CALL -> "showReasonsInRollCall"
    DisplayOption.RESULT_SUMMARY -> "showResultSummary"
    DisplayOption.EMPTY_RESULT_GROUPS -> "showEmptyResultGroups"
    DisplayOption.HISTORY_STATISTICS -> "showHistoryStatistics"
    DisplayOption.CONFIRM_INCOMPLETE -> "confirmIncompleteAttendance"
    DisplayOption.COMPACT_ROLL_CALL -> "compactRollCallRows"
    DisplayOption.EXPORT_LATE -> "exportLateStudents"
    DisplayOption.EXPORT_LEAVE -> "exportLeaveStudents"
    DisplayOption.EXPORT_ABSENT -> "exportAbsentStudents"
    DisplayOption.EXPORT_EXEMPT -> "exportExemptStudents"
    DisplayOption.MATERIAL_AVATAR -> "materialAvatarCompletesAll"
    DisplayOption.MATERIAL_PROGRESS -> "showMaterialProgress"
    DisplayOption.MATERIAL_HINT -> "showMaterialOperationHint"
    DisplayOption.MATERIAL_COMPACT -> "compactMaterialRows"
    DisplayOption.MATERIAL_STATUS_BUTTON -> "showMaterialStatusButton"
    else -> "collapse.${resultCollapseOptions.getValue(this).name}"
}

fun AttendanceStatus.settingPrefix(): String = when (this) {
    AttendanceStatus.PRESENT -> "present"
    AttendanceStatus.LATE -> "late"
    AttendanceStatus.LEAVE -> "leave"
    AttendanceStatus.ABSENT -> "absent"
    AttendanceStatus.EXEMPT -> "exempt"
    AttendanceStatus.UNMARKED -> error("未点没有自定义外观")
}

fun AppSettings.withStatusIcon(status: AttendanceStatus, icon: StatusIconOption): AppSettings = when (status) {
    AttendanceStatus.PRESENT -> copy(presentIcon = icon)
    AttendanceStatus.LATE -> copy(lateIcon = icon)
    AttendanceStatus.LEAVE -> copy(leaveIcon = icon)
    AttendanceStatus.ABSENT -> copy(absentIcon = icon)
    AttendanceStatus.EXEMPT -> copy(exemptIcon = icon)
    AttendanceStatus.UNMARKED -> this
}

fun AppSettings.withStatusColor(status: AttendanceStatus, color: StatusColorOption): AppSettings = when (status) {
    AttendanceStatus.PRESENT -> copy(presentColor = color)
    AttendanceStatus.LATE -> copy(lateColor = color)
    AttendanceStatus.LEAVE -> copy(leaveColor = color)
    AttendanceStatus.ABSENT -> copy(absentColor = color)
    AttendanceStatus.EXEMPT -> copy(exemptColor = color)
    AttendanceStatus.UNMARKED -> this
}
