package com.ulinoyaped.attendance

import com.ulinoyaped.attendance.data.AttendanceStatus
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.ui.Alignment
import androidx.compose.runtime.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal data class ClassSettingsScope(
    val name: String,
    val fields: Set<String>,
    val onReset: (String) -> Unit,
    val onResetAll: () -> Unit,
    val onBack: () -> Unit,
)
internal val LocalClassSettingsScope = staticCompositionLocalOf<ClassSettingsScope?> { null }

private val settingFields = mapOf(
    "点按默认选择" to "defaultStatus", "默认未到原因" to "defaultReason",
    "长按姓名" to "longPressAction", "向左滑动" to "swipeLeftAction", "向右滑动" to "swipeRightAction",
    "常用原因" to "absenceReasons", "显示班级人数" to "showClassStudentCount",
    "显示班级页操作提示" to "showClassOperationHint", "显示学生学号" to "showStudentNumbers",
    "显示已处理进度" to "showRollCallProgress", "显示点名操作提示" to "showOperationHint",
    "显示状态按钮" to "showStatusButton", "名单中显示原因" to "showReasonsInRollCall",
    "紧凑点名列表" to "compactRollCallRows", "未点完时二次确认" to "confirmIncompleteAttendance",
    "显示结果统计卡" to "showResultSummary", "显示空结果分类" to "showEmptyResultGroups",
    "显示历史统计" to "showHistoryStatistics", "按状态分类排列" to "groupResultsByStatus",
    "班级与点名时间" to "exportHeader", "到勤统计" to "exportSummary",
    "到场学生明细" to "exportPresentStudents", "迟到学生明细" to "exportLateStudents",
    "请假学生明细" to "exportLeaveStudents", "缺勤学生明细" to "exportAbsentStudents",
    "不参与学生明细" to "exportExemptStudents", "学生学号" to "exportStudentNumber",
    "原因或备注" to "exportReason", "记录标题" to "historyTitleMode",
    "头像快捷确认材料" to "materialAvatarCompletesAll", "显示材料统计" to "showMaterialProgress",
    "显示材料操作提示" to "showMaterialOperationHint", "紧凑材料列表" to "compactMaterialRows",
    "显示材料状态按钮" to "showMaterialStatusButton",
)

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun SettingInheritance(title: String, field: String? = null) {
    val scope = LocalClassSettingsScope.current ?: return
    val key = field ?: settingFields[title] ?: if (title.startsWith("默认折叠")) {
        val status = AttendanceStatus.entries.firstOrNull { title == "默认折叠${it.label}列表" } ?: return
        "collapse.${status.name}"
    } else return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val overridden = key in scope.fields
        Surface(shape = MaterialTheme.shapes.extraSmall, modifier = Modifier.align(Alignment.CenterVertically),
            color = if (overridden) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow) {
            Text((if (field != null) "$title · " else "") + if (overridden) "本班设置" else "跟随全局",
                style = MaterialTheme.typography.labelSmall,
                color = if (overridden) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
        }
        if (overridden) IconButton(onClick = { scope.onReset(key) }) {
            Icon(Icons.AutoMirrored.Filled.Undo, "恢复$title 为全局设置", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
internal fun ClassSettingsSummary(scope: ClassSettingsScope) {
    var confirmReset by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FeatureHeader("${scope.name} · 本班设置", "${scope.fields.size} 项单独设置，其余跟随全局。", Icons.AutoMirrored.Filled.Undo)
        if (scope.fields.isNotEmpty()) TextButton(onClick = { confirmReset = true }) { Text("全部恢复为全局设置") }
    }
    if (confirmReset) AlertDialog(onDismissRequest = { confirmReset = false }, title = { Text("恢复全局设置？") },
        text = { Text("将清除本班的 ${scope.fields.size} 项设置覆写，重新跟随全局设置。") },
        confirmButton = { TextButton(onClick = { scope.onResetAll(); confirmReset = false }) { Text("恢复全局") } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("取消") } })
}
