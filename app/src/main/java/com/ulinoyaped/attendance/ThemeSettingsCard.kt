package com.ulinoyaped.attendance

import android.os.Build
import androidx.compose.foundation.clickable
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
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("主题配色", style = MaterialTheme.typography.titleMedium)
            ThemeSource.entries.forEach { source ->
                Row(
                    Modifier.fillMaxWidth().clickable { onSetTheme(source, settings.themeSeed) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = settings.themeSource == source, onClick = { onSetTheme(source, settings.themeSeed) })
                    Text(if (source == ThemeSource.SYSTEM) "系统主题色" else "自定义颜色")
                }
            }
            if (settings.themeSource == ThemeSource.SYSTEM) {
                Text(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "跟随系统壁纸配色，自动适配深浅色模式。"
                    else "当前系统不支持动态取色，使用默认 Material 配色。",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                OutlinedTextField(
                    value = seed,
                    onValueChange = { seed = it.take(7) },
                    label = { Text("主题颜色 #RRGGBB") },
                    isError = !valid,
                    supportingText = { Text(if (valid) "由 Material 自动生成协调的颜色与深浅色搭配。" else "请输入六位十六进制颜色，例如 #6750A4") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("紫" to "#6750A4", "蓝" to "#386AAB", "绿" to "#386A54", "橙" to "#8B5000").forEach { (label, color) ->
                        TextButton(onClick = { seed = color; onSetTheme(ThemeSource.CUSTOM, color) }) { Text(label) }
                    }
                }
                Button(onClick = { onSetTheme(ThemeSource.CUSTOM, seed) }, enabled = valid) { Text("应用颜色") }
            }
        }
    }
}
