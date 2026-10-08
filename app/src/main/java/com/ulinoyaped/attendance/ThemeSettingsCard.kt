package com.ulinoyaped.attendance

import android.os.Build
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ulinoyaped.attendance.data.AppSettings
import com.ulinoyaped.attendance.data.ThemeSource

@Composable
internal fun ThemeSettingsCard(settings: AppSettings, onSetTheme: (ThemeSource, String) -> Unit) {
    var seed by remember(settings.themeSeed) { mutableStateOf(settings.themeSeed) }
    val valid = Regex("#[0-9a-fA-F]{6}").matches(seed)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("主题风格", style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 12.dp))
        ThemeSource.entries.forEach { source ->
            val (name, description) = when (source) {
                ThemeSource.ATTENDANCE -> "Attendance" to "墨蓝强调 · 中性背景 · 紧凑清晰"
                ThemeSource.SOFT -> "柔和" to "温和底色 · 更圆润的分组与留白"
                ThemeSource.SYSTEM -> "系统动态" to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    "使用壁纸配色，保留 Attendance 的界面层级" else "Android 12 以下使用 Attendance 配色"
                ThemeSource.CUSTOM -> "自定义" to "选择主色，背景与状态色保持协调"
            }
            Row(
                Modifier.fillMaxWidth().selectable(
                    selected = settings.themeSource == source,
                    role = androidx.compose.ui.semantics.Role.RadioButton,
                    onClick = { onSetTheme(source, settings.themeSeed) },
                ).padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = settings.themeSource == source, onClick = null)
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(name, style = MaterialTheme.typography.titleMedium)
                    Text(description, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (settings.themeSource == ThemeSource.CUSTOM) {
            Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = seed, onValueChange = { seed = it.take(7) },
                    label = { Text("主颜色 #RRGGBB") }, isError = !valid,
                    supportingText = { Text(if (valid) "用于按钮、选中态和进度；自动适配深浅色。" else "请输入六位十六进制颜色，例如 #315A74") },
                    singleLine = true, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("墨蓝" to "#315A74", "紫" to "#6750A4", "绿" to "#386A54", "橙" to "#8B5000").forEach { (label, color) ->
                        TextButton(onClick = { seed = color; onSetTheme(ThemeSource.CUSTOM, color) }) { Text(label) }
                    }
                }
                Button(shape = MaterialTheme.shapes.small, onClick = { onSetTheme(ThemeSource.CUSTOM, seed) }, enabled = valid) { Text("应用主色") }
            }
        }
    }
}
