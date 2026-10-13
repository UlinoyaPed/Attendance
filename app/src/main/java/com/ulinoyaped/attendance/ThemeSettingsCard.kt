package com.ulinoyaped.attendance

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ulinoyaped.attendance.data.AppSettings
import com.ulinoyaped.attendance.data.ThemeSource
import com.ulinoyaped.attendance.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ThemeSettingsCard(settings: AppSettings, onSetTheme: (ThemeSource, String) -> Unit) {
    var seed by remember(settings.themeSeed) { mutableStateOf(settings.themeSeed) }
    val valid = Regex("#[0-9a-fA-F]{6}").matches(seed)
    val context = LocalContext.current
    val dark = LocalAttendanceLayout.current.dark
    val dynamic = if (Build.VERSION.SDK_INT >= 31) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else null
    SettingsGroup(title = "主题风格", subtitle = "配色、圆角和留白共同构成界面风格") {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(12.dp)) {
            val columns = if (maxWidth < 320.dp && LocalDensity.current.fontScale > 1.2f) 1 else 2
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ThemeSource.entries.chunked(columns).forEach { options ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        options.forEach { source ->
                            val preview = attendanceColorScheme(source, settings.themeSeed, dark, dynamic)
                            val active = settings.themeSource == source
                            val (name, description) = when (source) {
                                ThemeSource.ATTENDANCE -> "Attendance" to "明快蓝紫 · 清晰层次"
                                ThemeSource.SOFT -> "柔和" to "温暖底色 · 圆润留白"
                                ThemeSource.SYSTEM -> "系统动态" to if (Build.VERSION.SDK_INT >= 31) "取自壁纸，随系统变化" else "当前系统使用默认配色"
                                ThemeSource.CUSTOM -> "自定义" to "用你喜欢的主色"
                            }
                            Surface(modifier = Modifier.weight(1f).selectable(active, role = Role.RadioButton,
                                onClick = { onSetTheme(source, settings.themeSeed) }),
                                shape = MaterialTheme.shapes.medium,
                                color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(if (active) 2.dp else 1.dp, if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(preview.primary, preview.secondary, preview.tertiary).forEach { color ->
                                            Surface(Modifier.size(18.dp), shape = CircleShape, color = color) {}
                                        }
                                        Spacer(Modifier.weight(1f))
                                        if (active) Icon(Icons.Default.CheckCircle, "已选择", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                    Text(name, style = MaterialTheme.typography.titleSmall,
                                        color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                    Text(description, style = MaterialTheme.typography.bodySmall,
                                        color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
        if (settings.themeSource == ThemeSource.CUSTOM) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("快捷配色", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("蓝紫" to "#5559C9", "湖蓝" to "#0061A4", "紫" to "#6750A4", "玫红" to "#B83384", "青绿" to "#007D78", "橙" to "#945500").forEach { (label, color) ->
                        FilterChip(selected = settings.themeSeed.equals(color, true), onClick = { seed = color; onSetTheme(ThemeSource.CUSTOM, color) },
                            label = { Text(label) }, leadingIcon = {
                                Surface(Modifier.size(16.dp), shape = CircleShape, color = attendanceColorScheme(ThemeSource.CUSTOM, color, dark).primary) {}
                            })
                    }
                }
                OutlinedTextField(value = seed, onValueChange = { seed = it.take(7) }, label = { Text("主颜色 #RRGGBB") }, isError = !valid,
                    supportingText = { Text(if (valid) "自动生成深浅配色；考勤状态颜色独立保留。" else "请输入六位十六进制颜色，例如 #5559C9") },
                    singleLine = true, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth())
                Button(shape = MaterialTheme.shapes.small, onClick = { onSetTheme(ThemeSource.CUSTOM, seed) }, enabled = valid && !seed.equals(settings.themeSeed, true)) { Text("应用主色") }
            }
        }
    }
}
