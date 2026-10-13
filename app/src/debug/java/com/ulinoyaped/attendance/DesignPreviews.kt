package com.ulinoyaped.attendance

import android.os.Build
import androidx.compose.material.icons.filled.Palette
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ulinoyaped.attendance.data.*
import com.ulinoyaped.attendance.ui.theme.*

// Debug-only fixtures: previews never access repositories or a user's stored classes.
private val previewStudents = listOf(
    Student("1", "张三", "202610080001"), Student("2", "李晓明", "2026002"),
    Student("3", "王同学", "2026003"), Student("4", "阿依古丽·买买提", "2026004"),
    Student("5", "陈静", "2026005"), Student("6", "赵远", "2026006"),
)

@Composable
private fun PreviewTheme(source: ThemeSource = ThemeSource.ATTENDANCE, seed: String = "#5559C9", content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val soft = source == ThemeSource.SOFT
    val context = LocalContext.current
    val dynamic = if (source == ThemeSource.SYSTEM && Build.VERSION.SDK_INT >= 31) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else null
    CompositionLocalProvider(LocalAttendanceLayout provides AttendanceLayout(dark, soft)) {
        MaterialTheme(
            colorScheme = attendanceColorScheme(source, seed, dark, dynamic),
            shapes = attendanceShapes(soft), typography = AttendanceTypography,
        ) { Surface(color = MaterialTheme.colorScheme.background, content = content) }
    }
}

@Preview(name = "学生行 · Attendance 浅色", widthDp = 360, heightDp = 700)
@Preview(name = "学生行 · Attendance 深色", widthDp = 360, heightDp = 700, uiMode = 0x20)
@Preview(name = "学生行 · 大字体窄屏", widthDp = 320, heightDp = 800, fontScale = 1.5f)
@Composable
private fun StudentRowsPreview() = PreviewTheme {
    val settings = AppSettings()
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        item { Text("学生行与状态", style = MaterialTheme.typography.headlineMedium) }
        item { RollCallSearchTools("", {}, RollCallFilter.ALL, {}) }
        AttendanceStatus.entries.forEachIndexed { index, status ->
            item {
                val mark = if (status == AttendanceStatus.UNMARKED) null else AttendanceMark(status, if (status == AttendanceStatus.LEAVE) "比赛外出，本周五返回，已获班主任批准" else "")
                RollCallItem(
                    previewStudents[index], mark, mark?.let { settings.iconFor(it.status) }, mark?.let { settings.colorFor(it.status) },
                    showStudentNumber = true, showReason = true, showStatusButton = true, compact = false,
                    onTogglePresent = {}, onLongPress = {}, onSwipeLeft = {}, onSwipeRight = {}, swipeSettings = settings, onEdit = {},
                )
            }
        }
    }
}

@Preview(name = "材料 · 浅色", widthDp = 360, heightDp = 760)
@Preview(name = "材料 · 深色", widthDp = 360, heightDp = 760, uiMode = 0x20)
@Composable
private fun MaterialsPreview() = PreviewTheme {
    val task = MaterialTask(
        "preview", "秋季活动材料收集", 0, 0,
        materials = listOf(MaterialItem("a", "活动登记表"), MaterialItem("b", "家长知情同意书")),
        participants = previewStudents.take(3), records = listOf(
            MaterialRecord("1", "a", MaterialRecordStatus.COMPLETED),
            MaterialRecord("1", "b", MaterialRecordStatus.COMPLETED),
            MaterialRecord("2", "a", MaterialRecordStatus.REJECTED),
        ),
    )
    MaterialTaskScreen(task, AppSettings(), {}, { _, _, _ -> }, { _, _ -> }, { task.records }, { _, _ -> }, {}, {})
}

@Preview(name = "主页 · 浅色", widthDp = 360, heightDp = 640)
@Preview(name = "主页 · 深色", widthDp = 360, heightDp = 640, uiMode = 0x20)
@Preview(name = "主页 · 窄屏大字体", widthDp = 320, heightDp = 780, fontScale = 1.5f)
@Composable
private fun HomePreview() = PreviewTheme {
    ClassActionScreen(ClassGroup("preview", "2026 秋 · 一班", previewStudents), ProfileIconOption.PERSON, AppSettings(), {}, {}, { _, _ -> }, {}, {})
}

@Preview(name = "柔和风格 · 浅色", widthDp = 360, heightDp = 720)
@Preview(name = "柔和风格 · 深色", widthDp = 360, heightDp = 720, uiMode = 0x20)
@Composable
private fun SoftSettingsPreview() = PreviewTheme(ThemeSource.SOFT) {
    Column(Modifier.padding(16.dp)) {
        Text("外观与头像", style = MaterialTheme.typography.headlineMedium)
        ThemeSettingsCard(AppSettings(themeSource = ThemeSource.SOFT), { _, _ -> })
    }
}

@Preview(name = "自定义 · 窄屏大字体", widthDp = 320, heightDp = 1000, fontScale = 1.5f)
@Preview(name = "自定义 · 深色窄屏大字体", widthDp = 320, heightDp = 1000, fontScale = 1.5f, uiMode = 0x20)
@Composable
private fun CustomAppearancePreview() = PreviewTheme(ThemeSource.CUSTOM, "#FF0000") {
    LazyColumn(Modifier.padding(16.dp)) {
        item { ThemeSettingsCard(AppSettings(themeSource = ThemeSource.CUSTOM, themeSeed = "#FF0000"), { _, _ -> }) }
        item {
            CompositionLocalProvider(LocalClassSettingsScope provides ClassSettingsScope("一班", setOf("exemptIcon", "exemptColor"), {}, {}, {})) {
                Column {
                StatusAppearanceRow("不参与", StatusIconOption.REMOVE, StatusColorOption.TEAL, {}, {})
                StatusAppearanceRow("缺勤", StatusIconOption.WARNING, StatusColorOption.PURPLE, {}, {})
                }
            }
        }
    }
}

@Preview(name = "历史 · 窄屏大字体", widthDp = 320, heightDp = 900, fontScale = 1.5f)
@Preview(name = "历史 · 深色窄屏大字体", widthDp = 320, heightDp = 900, fontScale = 1.5f, uiMode = 0x20)
@Composable
private fun HistoryPreview() = PreviewTheme {
    val time = 1791417600000L
    val entries = previewStudents.map { AttendanceEntry(it.id, it.name, it.studentNumber, AttendanceStatus.LEAVE, "比赛外出，本周五返回") }
    LazyColumn(Modifier.padding(16.dp)) {
        item { Text("历史与结果", style = MaterialTheme.typography.headlineMedium) }
        item { DraftHistoryItem("2026 秋季 · 综合实践课程一班", RollCallDraft("draft", "class", time, time, entries), HistoryTitleMode.CLASS_NAME, {}, {}) }
        item { GlobalHistoryItem("2026 秋季 · 综合实践课程一班", AttendanceSession("session", "class", time, entries), HistoryTitleMode.TIME, true, {}, {}, {}) }
        item { ResultEntryItem(entries.last(), AppSettings(), {}) }
        item {
            MaterialHistoryItem("2026 秋季 · 综合实践课程一班",
                MaterialTask("task", "家长知情同意与活动材料收集", time, time, time,
                    listOf(MaterialItem("form", "知情同意书")), previewStudents), AppSettings(), {})
        }
    }
}

@Preview(name = "动态色 · 状态分工", widthDp = 360, heightDp = 500, apiLevel = 35)
@Preview(name = "动态色 · 深色状态分工", widthDp = 360, heightDp = 500, apiLevel = 35, uiMode = 0x20)
@Composable
private fun DynamicStatusPreview() = PreviewTheme(ThemeSource.SYSTEM) {
    Column(Modifier.padding(16.dp)) {
        val settings = AppSettings()
        AttendanceStatus.entries.filter { it != AttendanceStatus.UNMARKED }.forEachIndexed { index, status ->
            RollCallItem(previewStudents[index], AttendanceMark(status), settings.iconFor(status), settings.colorFor(status),
                true, true, false, false, {}, {}, {}, {}, settings, {})
        }
    }
}

@Preview(name = "相同自定义图标颜色 · 窄屏深色", widthDp = 320, heightDp = 900, fontScale = 1.5f, uiMode = 0x20)
@Composable
private fun SameMarkersPreview() = PreviewTheme(ThemeSource.CUSTOM, "#FF0000") {
    val settings = AppSettings()
    LazyColumn(Modifier.padding(16.dp)) {
        AttendanceStatus.entries.filter { it != AttendanceStatus.UNMARKED }.forEachIndexed { index, status ->
            item {
                RollCallItem(previewStudents[index], AttendanceMark(status, "活动外出，预计周五返校"),
                    StatusIconOption.PERSON, StatusColorOption.PRIMARY, true, true, false, false, {}, {}, {}, {}, settings, {})
            }
        }
    }
}

@Preview(name = "设置分组 · 浅色", widthDp = 360, heightDp = 780)
@Preview(name = "设置分组 · 深色大字体", widthDp = 320, heightDp = 780, fontScale = 1.5f, uiMode = 0x20)
@Composable
private fun SettingsGroupsPreview() = PreviewTheme {
    LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { FeatureHeader("点名操作", "点按默认状态、长按与左右滑动", androidx.compose.material.icons.Icons.Default.Palette) }
        item { SettingsGroup("点名手势") {
            SettingRow("点按默认选择", "到场", {})
            SettingRow("默认未到原因", "比赛外出，预计本周五下午返校", {})
        } }
        item { CompositionLocalProvider(LocalClassSettingsScope provides ClassSettingsScope("一班", setOf("confirmIncompleteAttendance"), {}, {}, {})) {
            SettingsGroup("结束点名") { SwitchSettingRow("未点完时二次确认", true, {}) }
        } }
        item { ThemeSettingsCard(AppSettings(), { _, _ -> }) }
    }
}

@Preview(name = "长原因选择 · 窄屏大字体", widthDp = 320, heightDp = 780, fontScale = 1.5f)
@Composable
private fun LongReasonSheetPreview() = PreviewTheme {
    PreferenceChoiceSheet("默认未到原因", (1..100).map { "原因$it" }, "原因95", { it }, {}, {})
}
