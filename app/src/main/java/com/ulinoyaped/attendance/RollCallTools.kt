package com.ulinoyaped.attendance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ulinoyaped.attendance.data.AttendanceMark
import com.ulinoyaped.attendance.data.AttendanceStatus
import com.ulinoyaped.attendance.data.RollCallFilter
import com.ulinoyaped.attendance.data.isException

@Composable
internal fun RollCallSearchTools(
    query: String, onQueryChange: (String) -> Unit,
    filter: RollCallFilter, onFilterChange: (RollCallFilter) -> Unit,
) {
    val focus = LocalFocusManager.current
    var showStatusMenu by remember { mutableStateOf(false) }
    Column {
        OutlinedTextField(
            value = query, onValueChange = { onQueryChange(it.take(120)) },
            label = { Text("搜索姓名或学号") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
            trailingIcon = { if (query.isNotEmpty()) TextButton(onClick = { onQueryChange("") }) { Text("清除") } },
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LazyRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(listOf(RollCallFilter.ALL, RollCallFilter.UNMARKED, RollCallFilter.EXCEPTIONS, RollCallFilter.MARKED)) { option ->
                    FilterChip(selected = filter == option, onClick = { focus.clearFocus(); onFilterChange(option) }, label = { Text(option.label) })
                }
            }
            Box {
                FilterChip(
                    selected = filter.status != null,
                    onClick = { focus.clearFocus(); showStatusMenu = true },
                    label = { Text(if (filter.status != null) filter.label else "状态") },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "选择异常状态") },
                )
                DropdownMenu(expanded = showStatusMenu, onDismissRequest = { showStatusMenu = false }) {
                    RollCallFilter.entries.filter { it.status != null }.forEach { option ->
                        DropdownMenuItem(text = { Text(option.label) }, onClick = {
                            showStatusMenu = false
                            onFilterChange(option)
                        })
                    }
                }
            }
        }
    }
}

@Composable
internal fun AttendanceBatchDialog(
    count: Int,
    hiddenCount: Int,
    markedCount: Int,
    exceptionCount: Int,
    presetReasons: List<String>,
    onDismiss: () -> Unit,
    onApply: (AttendanceMark, Boolean, Boolean) -> Unit,
) {
    var status by remember { mutableStateOf(AttendanceStatus.PRESENT) }
    var reason by remember { mutableStateOf("") }
    var overwrite by remember { mutableStateOf(false) }
    var reasonsOnly by remember { mutableStateOf(false) }
    val affected = if (reasonsOnly) exceptionCount else if (overwrite) count else count - markedCount
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("批量处理 $count 名学生") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            if (hiddenCount > 0) Text(
                "将处理全部 $count 名已选学生，其中 $hiddenCount 人当前未显示。",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { reasonsOnly = !reasonsOnly }) {
                Checkbox(checked = reasonsOnly, onCheckedChange = { reasonsOnly = it })
                Text("仅修改原因，保持状态")
            }
            if (reasonsOnly) {
                Text("只修改已选的异常学生；到场及未处理学生跳过。", style = MaterialTheme.typography.bodySmall)
            } else {
                AttendanceStatus.entries.filter { it != AttendanceStatus.UNMARKED }.forEach { option ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { status = option }) {
                        RadioButton(selected = status == option, onClick = { status = option })
                        Text(option.label)
                    }
                }
                if (markedCount > 0) Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { overwrite = !overwrite },
                ) {
                    Checkbox(checked = overwrite, onCheckedChange = { overwrite = it })
                    Text("覆盖 $markedCount 人的已有状态和原因")
                }
            }
            if (reasonsOnly || status.isException()) {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it.take(240).filterNot { c -> c.isISOControl() || c == '\u2028' || c == '\u2029' } },
                    label = { Text("原因（留空则清除原因）") }, modifier = Modifier.fillMaxWidth(), maxLines = 3,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(presetReasons) { preset -> AssistChip(onClick = { reason = preset }, label = { Text(preset) }) }
                }
            }
            Text(
                "将修改 $affected 人" + if (!reasonsOnly && !overwrite) "，跳过 $markedCount 名已处理学生。" else "。",
                modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall,
            )
        } },
        confirmButton = { TextButton(
            enabled = affected > 0,
            onClick = { onApply(AttendanceMark(status, reason.trim()), overwrite, reasonsOnly) },
        ) { Text(if (overwrite && !reasonsOnly) "确认覆盖并应用" else "应用") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
