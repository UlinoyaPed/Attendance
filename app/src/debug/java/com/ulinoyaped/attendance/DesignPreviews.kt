package com.ulinoyaped.attendance

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
    Student("1", "张三", "2026001"), Student("2", "李晓明", "2026002"),
    Student("3", "王同学", "2026003"), Student("4", "阿依古丽·买买提", "2026004"),
    Student("5", "陈静", "2026005"), Student("6", "赵远", "2026006"),
)

@Composable
private fun PreviewTheme(soft: Boolean = false, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    CompositionLocalProvider(LocalAttendanceLayout provides AttendanceLayout(dark, soft)) {
        MaterialTheme(
            colorScheme = attendanceSurfaces(defaultColorScheme(dark), dark, soft),
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
                val mark = if (status == AttendanceStatus.UNMARKED) null else AttendanceMark(status, if (status == AttendanceStatus.LEAVE) "比赛外出" else "")
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
@Composable
private fun HomePreview() = PreviewTheme {
    ClassActionScreen(ClassGroup("preview", "2026 秋 · 一班", previewStudents), ProfileIconOption.PERSON, AppSettings(), {}, {}, { _, _ -> }, {}, {})
}

@Preview(name = "柔和风格 · 浅色", widthDp = 360, heightDp = 720)
@Preview(name = "柔和风格 · 深色", widthDp = 360, heightDp = 720, uiMode = 0x20)
@Composable
private fun SoftSettingsPreview() = PreviewTheme(soft = true) {
    Column(Modifier.padding(16.dp)) {
        Text("外观与头像", style = MaterialTheme.typography.headlineMedium)
        ThemeSettingsCard(AppSettings(themeSource = ThemeSource.SOFT), { _, _ -> })
    }
}
