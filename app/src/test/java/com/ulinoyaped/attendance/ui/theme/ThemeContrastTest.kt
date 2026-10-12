package com.ulinoyaped.attendance.ui.theme

import com.ulinoyaped.attendance.data.ThemeSource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.ulinoyaped.attendance.data.StatusColorOption
import org.junit.Assert.*
import org.junit.Test

class ThemeContrastTest {
    private fun contrast(a: Color, b: Color): Float {
        val x = a.luminance()
        val y = b.luminance()
        return (maxOf(x, y) + 0.05f) / (minOf(x, y) + 0.05f)
    }

    @Test fun neutralTextAndBusinessColorsAreReadableInBothModes() {
        for (dark in listOf(false, true)) for (soft in listOf(false, true)) {
            val scheme = attendanceSurfaces(defaultColorScheme(dark), dark, soft)
            for (surface in listOf(scheme.surface, scheme.background, scheme.surfaceContainerLow, scheme.surfaceContainer)) {
                assertTrue("main text dark=$dark soft=$soft", contrast(scheme.onSurface, surface) >= 4.5f)
                assertTrue("secondary text dark=$dark soft=$soft", contrast(scheme.onSurfaceVariant, surface) >= 4.5f)
            }
            for (option in StatusColorOption.entries) {
                val color = semanticColor(option, dark, scheme.primary)
                val fill = color.copy(alpha = if (dark) 0.10f else 0.06f).compositeOver(scheme.surface)
                assertTrue("$option on tonal surface dark=$dark", contrast(color, fill) >= 4.5f)
            }
            assertTrue(contrast(scheme.primary, scheme.surface) >= 4.5f)
            assertTrue(contrast(scheme.onPrimary, scheme.primary) >= 4.5f)
            assertTrue(contrast(scheme.inverseOnSurface, scheme.inverseSurface) >= 4.5f)
            assertTrue(contrast(scheme.inversePrimary, scheme.inverseSurface) >= 4.5f)
        }
    }

    @Test fun extremeCustomSeedsKeepNeutralSurfacesAndReadableActions() {
        for (dark in listOf(false, true)) for (seed in listOf(0xFF000000, 0xFFFFFFFF, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFF00)) {
            val scheme = attendanceSurfaces(customColorScheme(seed.toInt(), dark), dark)
            assertEquals(defaultColorScheme(dark).surface, scheme.surface)
            assertTrue("custom action seed=$seed dark=$dark", contrast(scheme.primary, scheme.surface) >= 4.5f)
            assertTrue(contrast(scheme.onPrimary, scheme.primary) >= 4.5f)
            assertTrue(contrast(scheme.inversePrimary, scheme.inverseSurface) >= 4.5f)
            for (option in StatusColorOption.entries.filter { it != StatusColorOption.PRIMARY }) {
                assertEquals(semanticColor(option, dark, defaultColorScheme(dark).primary), semanticColor(option, dark, scheme.primary))
            }
        }
    }
    @Test fun allPresetRoutesKeepStatusSemanticsAndSystemFallback() {
        for (dark in listOf(false, true)) {
            val custom = attendanceColorScheme(ThemeSource.CUSTOM, "#FF0000", dark)
            val standard = attendanceColorScheme(ThemeSource.ATTENDANCE, "#FF0000", dark)
            val fallback = attendanceColorScheme(ThemeSource.SYSTEM, "#FF0000", dark)
            assertEquals(standard.primary, fallback.primary)
            assertEquals(standard.surface, fallback.surface)
            assertEquals(standard.onSurface, fallback.onSurface)
            val dynamic = defaultColorScheme(dark).copy(primary = custom.primary, onPrimary = custom.onPrimary)
            assertEquals(custom.primary, attendanceColorScheme(ThemeSource.SYSTEM, "#315A74", dark, dynamic).primary)
            for (source in ThemeSource.entries) {
                val scheme = attendanceColorScheme(source, "#FF0000", dark, dynamic)
                for (option in StatusColorOption.entries.filter { it != StatusColorOption.PRIMARY }) {
                    val color = semanticColor(option, dark, scheme.primary)
                    assertEquals(semanticColor(option, dark, standard.primary), color)
                    assertTrue(contrast(color, scheme.surface) >= 4.5f)
                }
                // Theme-dependent color is preserved only when the user explicitly chose PRIMARY.
                assertEquals(scheme.primary, semanticColor(StatusColorOption.PRIMARY, dark, scheme.primary))
            }
        }
    }

}
