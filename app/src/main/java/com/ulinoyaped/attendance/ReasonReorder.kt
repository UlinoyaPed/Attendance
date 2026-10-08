package com.ulinoyaped.attendance

import androidx.compose.foundation.BorderStroke

import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.abs

internal class ReasonReorderState(
    val listState: LazyListState,
    reasons: List<String>,
) {
    var order by mutableStateOf(reasons)
    var dragging by mutableStateOf<String?>(null)
    private var startOffset = 0f
    private var distance by mutableFloatStateOf(0f)
    private var itemHeight = 0
    private var originalOrder = reasons
    val desiredTop: Float get() = startOffset + distance
    val translation: Float get() = dragging?.let { reason ->
        listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "reason-$reason" }
            ?.let { desiredTop - it.offset }
    } ?: 0f

    fun start(reason: String) {
        val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "reason-$reason" } ?: return
        originalOrder = order
        startOffset = item.offset.toFloat()
        itemHeight = item.size
        distance = 0f
        dragging = reason
    }

    fun drag(amount: Float) { distance += amount; reorder() }

    fun reorder() {
        val reason = dragging ?: return
        val index = order.indexOf(reason)
        if (index < 0) return
        val center = desiredTop + itemHeight / 2f
        val candidate = listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            item.key != "reason-$reason" && item.key is String &&
                (item.key as String).startsWith("reason-") &&
                center >= item.offset && center <= item.offset + item.size
        } ?: return
        val targetReason = (candidate.key as String).removePrefix("reason-")
        val target = order.indexOf(targetReason)
        if (target >= 0 && target != index) {
            val middle = candidate.offset + candidate.size / 2f
            if ((target > index && center > middle) || (target < index && center < middle)) {
                order = order.toMutableList().apply { add(target, removeAt(index)) }
            }
        }
    }

    fun finish(cancelled: Boolean, onReorder: (List<String>) -> Unit) {
        if (dragging == null) return
        dragging = null
        if (cancelled) order = originalOrder
        else if (order != originalOrder) onReorder(order)
    }
}

@Composable
internal fun rememberReasonReorderState(
    listState: LazyListState,
    reasons: List<String>,
): ReasonReorderState {
    val state = remember(listState) { ReasonReorderState(listState, reasons) }
    LaunchedEffect(reasons) { if (state.dragging == null) state.order = reasons }
    val edge = with(LocalDensity.current) { 64.dp.toPx() }
    LaunchedEffect(state.dragging) {
        if (state.dragging == null) return@LaunchedEffect
        var previous = withFrameNanos { it }
        while (state.dragging != null) {
            val now = withFrameNanos { it }
            val seconds = ((now - previous) / 1_000_000_000f).coerceAtMost(0.05f)
            previous = now
            val info = listState.layoutInfo
            val center = state.desiredTop +
                (info.visibleItemsInfo.firstOrNull { it.key == "reason-${state.dragging}" }?.size ?: 0) / 2f
            val speed = when {
                center < info.viewportStartOffset + edge -> -((info.viewportStartOffset + edge - center) / edge).coerceIn(0f, 1f)
                center > info.viewportEndOffset - edge -> ((center - info.viewportEndOffset + edge) / edge).coerceIn(0f, 1f)
                else -> 0f
            }
            if (abs(speed) > 0f) {
                listState.scroll { scrollBy(speed * edge * 8f * seconds) }
                state.reorder()
            }
        }
    }
    return state
}

internal fun LazyListScope.reasonReorderItems(
    state: ReasonReorderState,
    onReorder: (List<String>) -> Unit,
    onDelete: (String) -> Unit,
) {
    items(state.order, key = { "reason-$it" }) { reason ->
        val active = state.dragging == reason
        val latestReorder by rememberUpdatedState(onReorder)
        Card(
            modifier = Modifier.fillMaxWidth()
                .animateItem(placementSpec = if (active) null else spring())
                .zIndex(if (active) 1f else 0f)
                .graphicsLayer { translationY = if (active) state.translation else 0f },
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = if (active) 2.dp else 0.dp),
            colors = CardDefaults.cardColors(containerColor = if (active) MaterialTheme.colorScheme.surfaceContainerLow
                else MaterialTheme.colorScheme.surface),
        ) {
            Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.DragHandle, "长按拖动排序", tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp).pointerInput(reason) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { state.start(reason) },
                            onDragEnd = { state.finish(false, latestReorder) },
                            onDragCancel = { state.finish(true, latestReorder) },
                            onDrag = { change, amount -> change.consume(); state.drag(amount.y) },
                        )
                    }.padding(12.dp),
                )
                Text(reason, Modifier.weight(1f))
                IconButton(onClick = { onDelete(reason) }, enabled = state.dragging == null) {
                    Icon(Icons.Default.Delete, "删除原因")
                }
            }
        }
    }
}
