package com.ulinoyaped.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
internal fun AccentIcon(icon: ImageVector, tint: Color = MaterialTheme.colorScheme.primary) {
    Surface(shape = MaterialTheme.shapes.medium,
        color = tint.copy(alpha = 0.12f).compositeOver(MaterialTheme.colorScheme.surface),
        modifier = Modifier.size(42.dp)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp)) }
    }
}

@Composable
internal fun FeatureHeader(title: String, subtitle: String, icon: ImageVector, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = scheme.primaryContainer) {
        Row(Modifier.background(Brush.linearGradient(listOf(scheme.primaryContainer, lerp(scheme.primaryContainer, scheme.surface, 0.55f))))
            .padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            AccentIcon(icon)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = scheme.onPrimaryContainer)
                if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = scheme.onPrimaryContainer)
            }
        }
    }
}

/** A settings choice is applied on tap. Long lists scroll and start near the current choice. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> PreferenceChoiceSheet(
    title: String,
    choices: List<T>,
    selected: T,
    label: (T) -> String,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit,
    icon: ((T) -> ImageVector)? = null,
    tint: @Composable (T) -> Color = { MaterialTheme.colorScheme.primary },
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (choices.indexOf(selected) - 2).coerceAtLeast(0))
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = MaterialTheme.shapes.extraLarge, containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 8.dp))
            val scope = LocalClassSettingsScope.current
            Text(if (scope == null) "选择后自动保存" else "选择后仅应用于${scope.name}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 12.dp))
            LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.6f).dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                itemsIndexed(choices) { _, choice ->
                    val active = choice == selected
                    Surface(shape = MaterialTheme.shapes.medium,
                        color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow) {
                        Row(Modifier.fillMaxWidth().selectable(active, role = Role.RadioButton, onClick = { onSelect(choice) })
                            .heightIn(min = 56.dp).padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            icon?.let { Icon(it(choice), null, tint = tint(choice), modifier = Modifier.size(22.dp)) }
                            Text(label(choice), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f),
                                color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                            if (active) Icon(Icons.Default.Check, "当前选择", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End).padding(vertical = 8.dp)) { Text("关闭") }
        }
    }
}
