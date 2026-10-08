package com.ulinoyaped.attendance

import com.ulinoyaped.attendance.data.AttendanceStatus
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
        val prefix = if (field != null) "$title · " else ""
        Text(prefix + if (key in scope.fields) "本班覆写" else "跟随全局",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(androidx.compose.ui.Alignment.CenterVertically),
            color = if (key in scope.fields) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        if (key in scope.fields) Text("恢复全局", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(androidx.compose.ui.Alignment.CenterVertically).clickable(onClickLabel = "恢复$title 为全局设置", role = androidx.compose.ui.semantics.Role.Button) { scope.onReset(key) }.padding(vertical = 6.dp, horizontal = 4.dp))
    }
}

@Composable
internal fun ClassSettingsSummary(scope: ClassSettingsScope) {
    Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("${scope.name} · ${scope.fields.size} 项覆写", style = MaterialTheme.typography.titleMedium)
        Text("改动自动保存，仅影响本班。未覆写项随全局设置更新。", style = MaterialTheme.typography.bodySmall)
        if (scope.fields.isNotEmpty()) TextButton(onClick = scope.onResetAll) { Text("全部恢复为全局设置") }
    }
}
