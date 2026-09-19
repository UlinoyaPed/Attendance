package com.ulinoyaped.attendance

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.ulinoyaped.attendance.data.Student

@Composable
internal fun StudentIdentityText(
    student: Student,
    detail: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(student.name, style = MaterialTheme.typography.titleMedium)
        if (detail.isNotEmpty()) {
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
