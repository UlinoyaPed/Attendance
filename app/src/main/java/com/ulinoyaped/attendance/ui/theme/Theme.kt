package com.ulinoyaped.attendance.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF4663A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDEE7FF),
    onPrimaryContainer = Color(0xFF203A70),
    secondary = Color(0xFF33776F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6EFE7),
    onSecondaryContainer = Color(0xFF164D46),
    tertiary = Color(0xFF89618F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF2DEF3),
    onTertiaryContainer = Color(0xFF573B60),
    error = Color(0xFFAF414A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDADD),
    onErrorContainer = Color(0xFF721D28),
    background = Color(0xFFF5F7FC),
    onBackground = Color(0xFF222B3A),
    surface = Color(0xFFF8F9FD),
    onSurface = Color(0xFF222B3A),
    surfaceVariant = Color(0xFFE2E7F2),
    onSurfaceVariant = Color(0xFF505B70),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF0F3FB),
    surfaceContainer = Color(0xFFE9EEF8),
    surfaceContainerHigh = Color(0xFFE3E9F5),
    surfaceContainerHighest = Color(0xFFDCE3F0),
    outline = Color(0xFF758197),
    outlineVariant = Color(0xFFC6CEDF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB0C6FF),
    onPrimary = Color(0xFF193263),
    primaryContainer = Color(0xFF304A7B),
    onPrimaryContainer = Color(0xFFDEE7FF),
    secondary = Color(0xFF91D4C5),
    onSecondary = Color(0xFF093D35),
    secondaryContainer = Color(0xFF24564E),
    onSecondaryContainer = Color(0xFFD0F3E8),
    tertiary = Color(0xFFDEB5E3),
    onTertiary = Color(0xFF49304F),
    tertiaryContainer = Color(0xFF65466D),
    onTertiaryContainer = Color(0xFFF6DDF9),
    error = Color(0xFFFFADB5),
    onError = Color(0xFF650F22),
    errorContainer = Color(0xFF872C3A),
    onErrorContainer = Color(0xFFFFDADD),
    background = Color(0xFF111722),
    onBackground = Color(0xFFE1E7F3),
    surface = Color(0xFF141B27),
    onSurface = Color(0xFFE1E7F3),
    surfaceVariant = Color(0xFF3C4658),
    onSurfaceVariant = Color(0xFFC0C9DB),
    surfaceContainerLowest = Color(0xFF0E131D),
    surfaceContainerLow = Color(0xFF1B2433),
    surfaceContainer = Color(0xFF222D3E),
    surfaceContainerHigh = Color(0xFF2B3648),
    surfaceContainerHighest = Color(0xFF354054),
    outline = Color(0xFF8995AB),
    outlineVariant = Color(0xFF424D61),
)

@Composable
fun AttendanceTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}
