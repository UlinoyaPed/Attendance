package com.ulinoyaped.attendance.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ulinoyaped.attendance.data.StatusColorOption

// Small controls / inputs / list groups / feature panels / dialogs and sheets.
internal fun attendanceShapes(soft: Boolean) = if (soft) Shapes(
    RoundedCornerShape(8.dp), RoundedCornerShape(12.dp), RoundedCornerShape(16.dp),
    RoundedCornerShape(22.dp), RoundedCornerShape(28.dp),
) else Shapes(
    RoundedCornerShape(4.dp), RoundedCornerShape(8.dp), RoundedCornerShape(12.dp),
    RoundedCornerShape(16.dp), RoundedCornerShape(24.dp),
)

private fun text(size: Int, height: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = FontFamily.SansSerif, fontWeight = weight,
    fontSize = size.sp, lineHeight = height.sp, letterSpacing = 0.sp,
)
internal val AttendanceTypography = Typography(
    displayLarge = text(48, 56, FontWeight.Medium), displayMedium = text(40, 48, FontWeight.Medium),
    displaySmall = text(32, 40, FontWeight.Medium),
    headlineLarge = text(28, 36, FontWeight.SemiBold), headlineMedium = text(24, 32, FontWeight.SemiBold),
    headlineSmall = text(22, 30, FontWeight.Medium).copy(fontFeatureSettings = "tnum"),
    titleLarge = text(20, 28, FontWeight.SemiBold), titleMedium = text(16, 24, FontWeight.Medium),
    titleSmall = text(14, 20, FontWeight.Medium),
    bodyLarge = text(16, 24), bodyMedium = text(14, 20), bodySmall = text(12, 18),
    labelLarge = text(14, 20, FontWeight.Medium), labelMedium = text(12, 18, FontWeight.Medium),
    labelSmall = text(11, 16, FontWeight.Medium),
)

internal data class AttendanceLayout(val dark: Boolean = false, val soft: Boolean = false) {
    val pageInset = 16.dp
    val rowInset = 12.dp
    val rowGap = if (soft) 6.dp else 2.dp
    val rowPadding = if (soft) 12.dp else 8.dp
    val groupGap = if (soft) 16.dp else 12.dp
}
internal val LocalAttendanceLayout = staticCompositionLocalOf { AttendanceLayout() }

/** Business colors are independent of the theme seed; PRIMARY is an explicit user override. */
internal fun semanticColor(option: StatusColorOption, dark: Boolean, primary: Color): Color = when (option) {
    StatusColorOption.PRIMARY -> primary
    StatusColorOption.GREEN -> if (dark) Color(0xFF91C6AA) else Color(0xFF2D7053)
    StatusColorOption.AMBER -> if (dark) Color(0xFFE0BF82) else Color(0xFF85570B)
    StatusColorOption.BLUE -> if (dark) Color(0xFF9BBDE2) else Color(0xFF315F94)
    StatusColorOption.RED -> if (dark) Color(0xFFE2A3A8) else Color(0xFFA73F48)
    StatusColorOption.PURPLE -> if (dark) Color(0xFFC6AEDC) else Color(0xFF73538C)
    StatusColorOption.TEAL -> if (dark) Color(0xFF98C7CB) else Color(0xFF2E7278)
    StatusColorOption.GRAY -> if (dark) Color(0xFFB5C0CA) else Color(0xFF5E6B76)
}

@Composable
internal fun statusTone(option: StatusColorOption): Color =
    semanticColor(option, LocalAttendanceLayout.current.dark, MaterialTheme.colorScheme.primary)

/** Opaque, restrained tonal fills: no swipe background bleeding through a row. */
@Composable
internal fun statusSurface(color: Color): Color = color.copy(
    alpha = if (LocalAttendanceLayout.current.dark) 0.10f else 0.06f,
).compositeOver(MaterialTheme.colorScheme.surface)

internal object AttendanceText {
    val studentName: TextStyle @Composable get() = MaterialTheme.typography.titleMedium
    val detail: TextStyle @Composable get() = MaterialTheme.typography.bodySmall
    val status: TextStyle @Composable get() = MaterialTheme.typography.labelMedium
    val statistic: TextStyle @Composable get() = MaterialTheme.typography.headlineSmall
}
