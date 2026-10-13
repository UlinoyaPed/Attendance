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
import androidx.compose.ui.graphics.compositeOver
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
    val background = if (dark) Color(0xFF12121C) else Color(0xFFF4F3FB)
    val surface = if (dark) Color(0xFF1B1B27) else Color.White
    fun layer(amount: Float) = accent.primary.copy(alpha = amount).compositeOver(surface)
    return accent.copy(
        background = if (soft && !dark) Color(0xFFFAF6F3) else background,
        onBackground = if (dark) Color(0xFFE9E6F2) else Color(0xFF252339),
        surface = if (soft && !dark) Color(0xFFFFFDFA) else surface,
        onSurface = if (dark) Color(0xFFE9E6F2) else Color(0xFF252339),
        onSurfaceVariant = if (dark) Color(0xFFC0BDCF) else Color(0xFF5D5A70),
        surfaceDim = if (dark) background else layer(0.10f),
        surfaceBright = if (dark) layer(0.12f) else surface,
        surfaceContainerLowest = background,
        surfaceContainerLow = layer(if (soft) 0.035f else 0.05f),
        surfaceContainer = layer(0.07f),
        surfaceContainerHigh = layer(0.09f),
        surfaceContainerHighest = layer(0.11f),
        surfaceVariant = layer(0.09f),
        outline = if (dark) Color(0xFF9994B0) else Color(0xFF827B9A),
        outlineVariant = if (dark) Color(0xFF464255) else Color(0xFFE0DBED),
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
        primary = Color(0xFFC4C1FF), onPrimary = Color(0xFF272466),
        primaryContainer = Color(0xFF393586), onPrimaryContainer = Color(0xFFE8E2FF),
        secondary = Color(0xFFBCC8EA), onSecondary = Color(0xFF283650),
        secondaryContainer = Color(0xFF3D4B68), onSecondaryContainer = Color(0xFFDFE7FF),
        tertiary = Color(0xFF8CD3D6), onTertiary = Color(0xFF00373B),
        tertiaryContainer = Color(0xFF005054), onTertiaryContainer = Color(0xFFB8F0F0),
        inversePrimary = Color(0xFF5559C9), surfaceTint = Color(0xFFC4C1FF),
    ) else lightColorScheme(
        primary = Color(0xFF5559C9), onPrimary = Color.White,
        primaryContainer = Color(0xFFE8E5FF), onPrimaryContainer = Color(0xFF302C70),
        secondary = Color(0xFF52617F), onSecondary = Color.White,
        secondaryContainer = Color(0xFFE3EAFF), onSecondaryContainer = Color(0xFF2C3A55),
        tertiary = Color(0xFF006B70), onTertiary = Color.White,
        tertiaryContainer = Color(0xFFD6F3F2), onTertiaryContainer = Color(0xFF00474B),
        inversePrimary = Color(0xFFC4C1FF), surfaceTint = Color(0xFF5559C9),
    ), dark,
)

/** Shared by runtime, previews, and tests; dynamic roles are supplied only on API 31+. */
internal fun attendanceColorScheme(source: ThemeSource, seed: String, dark: Boolean, dynamic: ColorScheme? = null): ColorScheme {
    val accent = when (source) {
        ThemeSource.ATTENDANCE, ThemeSource.SOFT -> defaultColorScheme(dark)
        ThemeSource.SYSTEM -> dynamic ?: defaultColorScheme(dark)
        ThemeSource.CUSTOM -> customColorScheme((0xFF000000L or seed.removePrefix("#").toLong(16)).toInt(), dark)
    }
    return attendanceSurfaces(accent, dark, source == ThemeSource.SOFT)
}

@Composable
fun AttendanceTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val repository = remember { AttendanceRepository.getInstance(context.applicationContext) }
    val settings by repository.settings.collectAsStateWithLifecycle()
    val dark = isSystemInDarkTheme()
    val soft = settings.themeSource == ThemeSource.SOFT
    val colors = remember(settings.themeSource, settings.themeSeed, dark, context.resources.configuration) {
        val dynamic = if (settings.themeSource == ThemeSource.SYSTEM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else null
        attendanceColorScheme(settings.themeSource, settings.themeSeed, dark, dynamic)
    }
    CompositionLocalProvider(LocalAttendanceLayout provides AttendanceLayout(dark, soft)) {
        MaterialTheme(colorScheme = colors, shapes = attendanceShapes(soft), typography = AttendanceTypography, content = content)
    }
}
