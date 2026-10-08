package com.ulinoyaped.attendance

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** One short-lived action. Replacement cancels the old callback instead of queuing snackbars. */
internal class RecentOperationUndo(private val scope: CoroutineScope) {
    val host = SnackbarHostState()
    private var job: Job? = null
    private var generation = 0

    fun clear() {
        generation++
        job?.cancel()
        job = null
        host.currentSnackbarData?.dismiss()
    }

    fun offer(message: String, restore: () -> Unit) {
        clear()
        val current = generation
        job = scope.launch {
            val result = host.showSnackbar(message, actionLabel = "撤销", withDismissAction = true, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed && current == generation) restore()
        }
    }
}

@Composable
internal fun rememberRecentOperationUndo(key: Any): RecentOperationUndo {
    val scope = rememberCoroutineScope()
    val undo = remember(key) { RecentOperationUndo(scope) }
    DisposableEffect(undo) { onDispose { undo.clear() } }
    return undo
}
