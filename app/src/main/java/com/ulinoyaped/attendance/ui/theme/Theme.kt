package com.ulinoyaped.attendance.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
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

/** Accent roles may be generated; neutral layers and error roles belong to Attendance. */
internal fun attendanceSurfaces(accent: ColorScheme, dark: Boolean, soft: Boolean = false): ColorScheme {
    val background = if (dark) Color(0xFF11161B) else Color(0xFFF7F9FB)
    val surface = if (dark) Color(0xFF151B21) else Color(0xFFFCFDFE)
    return accent.copy(
        background = if (soft && !dark) Color(0xFFFAF8F5) else background,
        onBackground = if (soft) { if (dark) Color(0xFFD6DDE3) else Color(0xFF343B40) }
            else if (dark) Color(0xFFE2E8ED) else Color(0xFF1C2730),
        surface = if (soft && !dark) Color(0xFFFFFDFA) else surface,
        onSurface = if (soft) { if (dark) Color(0xFFD6DDE3) else Color(0xFF343B40) }
            else if (dark) Color(0xFFE2E8ED) else Color(0xFF1C2730),
        onSurfaceVariant = if (dark) Color(0xFFB5C0CA) else Color(0xFF56636F),
        surfaceDim = if (dark) background else Color(0xFFE4E9ED),
        surfaceBright = if (dark) Color(0xFF303840) else surface,
        surfaceContainerLowest = background,
        surfaceContainerLow = if (dark) Color(0xFF1A2128) else Color(0xFFF2F5F7),
        surfaceContainer = if (dark) Color(0xFF1E262E) else Color(0xFFEDF1F4),
        surfaceContainerHigh = if (dark) Color(0xFF242D35) else Color(0xFFE8EDF1),
        surfaceContainerHighest = if (dark) Color(0xFF2A343D) else Color(0xFFE2E8ED),
        surfaceVariant = if (dark) Color(0xFF242D35) else Color(0xFFEDF1F4),
        outline = if (dark) Color(0xFF8996A2) else Color(0xFF75838E),
        outlineVariant = if (dark) Color(0xFF36424D) else Color(0xFFD7DFE6),
        inverseSurface = if (dark) Color(0xFFE2E8ED) else Color(0xFF29343D),
        inverseOnSurface = if (dark) Color(0xFF29343D) else Color(0xFFF2F5F7),
        error = if (dark) Color(0xFFFFB4AB) else Color(0xFFBA1A1A),
        onError = if (dark) Color(0xFF690005) else Color.White,
        errorContainer = if (dark) Color(0xFF410002) else Color(0xFFFFDAD6),
        onErrorContainer = if (dark) Color(0xFFFFDAD6) else Color(0xFF410002),
    )
}

internal fun defaultColorScheme(dark: Boolean): ColorScheme = attendanceSurfaces(
    if (dark) darkColorScheme(
        primary = Color(0xFFA8CDDF), onPrimary = Color(0xFF103344),
        primaryContainer = Color(0xFF264B60), onPrimaryContainer = Color(0xFFD1E9F5),
        secondary = Color(0xFFBCCBD4), onSecondary = Color(0xFF25343E),
        secondaryContainer = Color(0xFF3A4A55), onSecondaryContainer = Color(0xFFD8E5EE),
        tertiary = Color(0xFFBBCABD), onTertiary = Color(0xFF28362B),
        tertiaryContainer = Color(0xFF3E4D40), onTertiaryContainer = Color(0xFFD7E6D8),
        inversePrimary = Color(0xFF315A74), surfaceTint = Color(0xFFA8CDDF),
    ) else lightColorScheme(
        primary = Color(0xFF315A74), onPrimary = Color.White,
        primaryContainer = Color(0xFFDCEBF3), onPrimaryContainer = Color(0xFF193B51),
        secondary = Color(0xFF536875), onSecondary = Color.White,
        secondaryContainer = Color(0xFFE0E9EE), onSecondaryContainer = Color(0xFF304550),
        tertiary = Color(0xFF596D5C), onTertiary = Color.White,
        tertiaryContainer = Color(0xFFE2EAE1), onTertiaryContainer = Color(0xFF334936),
        inversePrimary = Color(0xFFA8CDDF), surfaceTint = Color(0xFF315A74),
    ), dark,
)

@Composable
fun AttendanceTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val repository = remember { AttendanceRepository.getInstance(context.applicationContext) }
    val settings by repository.settings.collectAsStateWithLifecycle()
    val dark = isSystemInDarkTheme()
    val soft = settings.themeSource == ThemeSource.SOFT
    val colors = remember(settings.themeSource, settings.themeSeed, dark, context.resources.configuration) {
        val accents = when (settings.themeSource) {
            ThemeSource.SYSTEM -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else defaultColorScheme(dark)
            ThemeSource.CUSTOM -> customColorScheme(android.graphics.Color.parseColor(settings.themeSeed), dark)
            ThemeSource.ATTENDANCE, ThemeSource.SOFT -> defaultColorScheme(dark)
        }
        attendanceSurfaces(accents, dark, soft)
    }
    CompositionLocalProvider(LocalAttendanceLayout provides AttendanceLayout(dark, soft)) {
        MaterialTheme(colorScheme = colors, shapes = attendanceShapes(soft), typography = AttendanceTypography, content = content)
    }
}
