package com.ulinoyaped.attendance.data

import org.junit.Assert.*
import org.junit.Test

class SettingsOverridesTest {
    @Test fun oneOverrideDoesNotFreezeOtherGlobalSettings() {
        val global = AppSettings()
        val group = ClassGroup("a", "A", attendanceSettings = ClassAttendanceSettings(
            values = global.copy(showStudentNumbers = false), overriddenFields = setOf("showStudentNumbers"),
        ))
        val changed = global.copy(swipeLeftAction = GestureAction.LEAVE, exportReason = false, absentColor = StatusColorOption.PURPLE)
        val effective = changed.forClass(group)
        assertFalse(effective.showStudentNumbers)
        assertEquals(GestureAction.LEAVE, effective.swipeLeftAction)
        assertFalse(effective.exportReason)
        assertEquals(StatusColorOption.PURPLE, effective.absentColor)
        assertEquals(changed, changed.forClass(group.copy(attendanceSettings = null)))
    }

    @Test fun localReasonsAppearanceExportsHistoryAndMaterialsOverrideIndependently() {
        val global = AppSettings()
        val local = global.copy(absenceReasons = listOf("local"), defaultReason = "local", absentIcon = StatusIconOption.WARNING,
            absentColor = StatusColorOption.PURPLE, exportStudentNumber = false, historyTitleMode = HistoryTitleMode.TIME,
            materialAvatarCompletesAll = false, compactMaterialRows = true)
        val fields = setOf("absenceReasons", "defaultReason", "absentIcon", "absentColor", "exportStudentNumber",
            "historyTitleMode", "materialAvatarCompletesAll", "compactMaterialRows")
        val custom = ClassAttendanceSettings(values = local, overriddenFields = fields)
        val group = ClassGroup("a", "A", attendanceSettings = custom)
        assertEquals(local, global.forClass(group))
        assertEquals(global, global.forClass(ClassGroup("b", "B")))
        val restored = global.forClass(group.copy(attendanceSettings = custom.copy(overriddenFields = fields - "absentColor")))
        assertEquals(global.absentColor, restored.absentColor)
        assertEquals(StatusIconOption.WARNING, restored.absentIcon)
        assertEquals("local", restored.defaultReason)
        val inheritedReasons = global.forClass(group.copy(attendanceSettings = custom.copy(overriddenFields = fields - "absenceReasons")))
        assertEquals(global.absenceReasons, inheritedReasons.absenceReasons)
        assertEquals("", inheritedReasons.defaultReason)
    }

    @Test fun oneCollapsedCategoryDoesNotFreezeOrResetOthers() {
        val global = AppSettings(collapsedResultStatuses = setOf(AttendanceStatus.LEAVE))
        val custom = ClassAttendanceSettings(values = global.copy(collapsedResultStatuses = setOf(AttendanceStatus.LEAVE, AttendanceStatus.ABSENT)),
            overriddenFields = setOf("collapse.ABSENT"))
        val group = ClassGroup("a", "A", attendanceSettings = custom)
        assertEquals(setOf(AttendanceStatus.LEAVE, AttendanceStatus.ABSENT), global.forClass(group).collapsedResultStatuses)
        val changed = global.copy(collapsedResultStatuses = setOf(AttendanceStatus.LATE))
        assertEquals(setOf(AttendanceStatus.LATE, AttendanceStatus.ABSENT), changed.forClass(group).collapsedResultStatuses)
        assertEquals(setOf(AttendanceStatus.LATE), changed.forClass(group.copy(attendanceSettings = custom.copy(overriddenFields = emptySet()))).collapsedResultStatuses)
    }

    @Test fun legacyClassOptionsRemainAppliedWhileNewOptionsInherit() {
        val global = AppSettings(absentColor = StatusColorOption.BLUE, compactMaterialRows = true)
        val legacy = ClassAttendanceSettings(defaultStatus = AttendanceStatus.LEAVE, showStudentNumbers = false)
        val effective = global.forClass(ClassGroup("a", "A", attendanceSettings = legacy))
        assertEquals(AttendanceStatus.LEAVE, effective.defaultStatus)
        assertFalse(effective.showStudentNumbers)
        assertEquals(StatusColorOption.BLUE, effective.absentColor)
        assertTrue(effective.compactMaterialRows)
    }
}
