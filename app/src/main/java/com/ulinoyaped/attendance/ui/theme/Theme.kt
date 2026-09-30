package com.ulinoyaped.attendance.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.color.MaterialColors
import com.ulinoyaped.attendance.data.AttendanceRepository
import com.ulinoyaped.attendance.data.ThemeSource

// Material generates paired foreground/background tones for both brightness modes.
internal fun customColorScheme(seed: Int, dark: Boolean): ColorScheme {
    val primary = MaterialColors.getColorRoles(seed, !dark)
    val secondary = MaterialColors.getColorRoles(MaterialColors.harmonize(0xFF607D8B.toInt(), seed), !dark)
    val tertiary = MaterialColors.getColorRoles(MaterialColors.harmonize(0xFF806080.toInt(), seed), !dark)
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = Color(primary.accent), onPrimary = Color(primary.onAccent),
        primaryContainer = Color(primary.accentContainer), onPrimaryContainer = Color(primary.onAccentContainer),
        secondary = Color(secondary.accent), onSecondary = Color(secondary.onAccent),
        secondaryContainer = Color(secondary.accentContainer), onSecondaryContainer = Color(secondary.onAccentContainer),
        tertiary = Color(tertiary.accent), onTertiary = Color(tertiary.onAccent),
        tertiaryContainer = Color(tertiary.accentContainer), onTertiaryContainer = Color(tertiary.onAccentContainer),
        surfaceTint = Color(primary.accent),
        inversePrimary = Color(MaterialColors.getColorRoles(seed, dark).accent),
    )
}

@Composable
fun AttendanceTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val repository = remember { AttendanceRepository.getInstance(context.applicationContext) }
    val settings by repository.settings.collectAsStateWithLifecycle()
    val dark = isSystemInDarkTheme()
    val colors = remember(settings.themeSource, settings.themeSeed, dark, context.resources.configuration) {
        if (settings.themeSource == ThemeSource.SYSTEM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            val seed = if (settings.themeSource == ThemeSource.CUSTOM) settings.themeSeed else "#6750A4"
            customColorScheme(android.graphics.Color.parseColor(seed), dark)
        }
    }
    MaterialTheme(colorScheme = colors, content = content)
}
