package com.ulinoyaped.attendance

import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import androidx.compose.runtime.mutableStateOf
import kotlin.math.roundToInt
import androidx.compose.runtime.rememberUpdatedState

internal data class SwipeActionVisual(val label: String, val color: Color, val icon: ImageVector)

@Composable
internal fun SwipeRecordContainer(
    key: String,
    left: SwipeActionVisual,
    right: SwipeActionVisual,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    val latestLeft by rememberUpdatedState(onSwipeLeft)
    val latestRight by rememberUpdatedState(onSwipeRight)
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val triggerDistance = remember(density) { with(density) { 72.dp.toPx() } }
    val maximumDrag = remember(density) { with(density) { 112.dp.toPx() } }
    var horizontalOffset by remember(key) { mutableFloatStateOf(0f) }
    var resetJob by remember(key) { mutableStateOf<Job?>(null) }
    fun resetHorizontalOffset() {
        val start = horizontalOffset
        resetJob?.cancel()
        resetJob = scope.launch {
            animate(initialValue = start, targetValue = 0f) { value, _ ->
                horizontalOffset = value
            }
        }
    }

    val visual = if (horizontalOffset > 0f) right else left
    val swipeColor = visual.color
    val swipeIcon = visual.icon
    val swipeText = visual.label
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerLow
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
    ) {
        if (horizontalOffset != 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    // Fill behind the foreground's rounded corners before clipping labels.
                    .background(swipeColor.copy(alpha = 0.2f).compositeOver(surfaceColor))
                    .drawWithContent {
                        // Reveal only the strip vacated by the foreground card, including
                        // during the return animation. Never draw labels beneath its content.
                        val revealed = horizontalOffset.roundToInt().toFloat()
                            .coerceIn(-size.width, size.width)
                        clipRect(
                            left = if (revealed >= 0f) 0f else size.width + revealed,
                            right = if (revealed >= 0f) revealed else size.width,
                        ) {
                            this@drawWithContent.drawContent()
                        }
                    }
                    .padding(horizontal = 12.dp),
                contentAlignment = if (horizontalOffset > 0f) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (horizontalOffset > 0f) Icon(swipeIcon, null, tint = swipeColor, modifier = Modifier.size(20.dp))
                    Text(swipeText, color = MaterialTheme.colorScheme.onSurface)
                    if (horizontalOffset < 0f) Icon(swipeIcon, null, tint = swipeColor, modifier = Modifier.size(20.dp))
                }
            }
        }
        content(
            Modifier.offset { IntOffset(horizontalOffset.roundToInt(), 0) }
                .pointerInput(key, left, right) {
                    detectHorizontalDragGestures(
                        onDragStart = { resetJob?.cancel(); horizontalOffset = 0f },
                        onDragCancel = { resetHorizontalOffset() },
                        onDragEnd = {
                            when {
                                horizontalOffset >= triggerDistance -> latestRight()
                                horizontalOffset <= -triggerDistance -> latestLeft()
                            }
                            resetHorizontalOffset()
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            horizontalOffset = (horizontalOffset + dragAmount)
                                .coerceIn(-maximumDrag, maximumDrag)
                        },
                    )
                }
        )
    }
}
