package com.ulinoyaped.attendance

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test

class RecentOperationUndoTest {
    @Test fun newOperationReplacesOldSnackbarAndRestoresOnlyLatestSnapshot() = runBlocking {
        val undo = RecentOperationUndo(this)
        var firstRestored = false
        var restored = 0
        undo.offer("first") { firstRestored = true }
        yield()
        val stale = undo.host.currentSnackbarData!!
        undo.offer("second") { restored++ }
        yield()
        assertEquals("second", undo.host.currentSnackbarData!!.visuals.message)
        stale.performAction()
        yield()
        assertFalse(firstRestored)
        assertEquals(0, restored)
        undo.host.currentSnackbarData!!.performAction()
        yield()
        assertEquals(1, restored)
        assertNull(undo.host.currentSnackbarData)
        undo.clear()
    }

    @Test fun dismissalAndLeavingScreenDoNotRestoreAnything() = runBlocking {
        val undo = RecentOperationUndo(this)
        var restored = false
        undo.offer("dismiss") { restored = true }
        yield()
        undo.host.currentSnackbarData!!.dismiss()
        yield()
        assertFalse(restored)
        undo.offer("leave") { restored = true }
        yield()
        val stale = undo.host.currentSnackbarData!!
        undo.clear()
        stale.performAction()
        yield()
        assertFalse(restored)
        assertNull(undo.host.currentSnackbarData)
    }
}
