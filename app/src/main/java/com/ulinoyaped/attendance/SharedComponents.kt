package com.ulinoyaped.attendance

import com.ulinoyaped.attendance.ui.theme.AttendanceText
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.ulinoyaped.attendance.data.Student

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StudentIdentityText(
    student: Student,
    detail: String,
    modifier: Modifier = Modifier,
    statusText: String? = null,
    statusTint: Color? = null,
    detailMaxLines: Int = 1,
) {
    // Reserve three CJK glyphs (the longest attendance label), including font scaling.
    // Switching 到 / 迟到 / 不参与 then does not move the detail between flow lines.
    val statusWidth = with(LocalDensity.current) { (AttendanceText.status.fontSize.toPx() * 3).toDp() }
    Column(modifier = modifier) {
        Text(student.name, style = AttendanceText.studentName)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Status gets measured first and never shares the detail's ellipsis budget.
            if (!statusText.isNullOrBlank()) Text(statusText, style = AttendanceText.status,
                modifier = Modifier.widthIn(min = statusWidth),
                color = statusTint ?: MaterialTheme.colorScheme.onSurfaceVariant)
            if (detail.isNotEmpty()) Text(
                detail, style = AttendanceText.detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = detailMaxLines, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Only genuine groups use a panel. Ordinary list rows stay flat. */
@Composable
internal fun AttendancePanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    ) { Column(content = content) }
}

@Composable
internal fun AttendanceListRow(
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(), color = if (accent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f).compositeOver(MaterialTheme.colorScheme.surface) else MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.small,
        border = if (accent) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)) else null,
    ) {
        Column {
            content()
            if (!accent) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
        }
    }
}
