package com.javelinco.localmusicplayer.library

import com.javelinco.localmusicplayer.data.scan.ScanCoordinator
import com.javelinco.localmusicplayer.data.scan.ScanExecutionMode
import com.javelinco.localmusicplayer.data.scan.ScanPhase
import com.javelinco.localmusicplayer.data.scan.ScanProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScanSessionManagerTest {
    @Test
    fun firstSourceUsesDedicatedAndCompletionLeavesModeWithSummary() = runTest {
        val coordinator = RecordingScanCoordinator()
        val manager = ScanSessionManager(coordinator, this)
        var playbackStops = 0

        manager.sourceAdded(wasFirstSource = true) { playbackStops++ }
        advanceUntilIdle()

        assertEquals(listOf(ScanExecutionMode.DEDICATED), coordinator.modes)
        assertEquals(1, playbackStops)
        assertFalse(manager.dedicated.value)
        assertEquals("Scan complete · 7 indexed · 1 skipped · 0 errors", manager.message.value)
    }

    @Test
    fun laterSourceUsesBackgroundWithoutStoppingPlayback() = runTest {
        val coordinator = RecordingScanCoordinator()
        val manager = ScanSessionManager(coordinator, this)
        var playbackStops = 0

        manager.sourceAdded(wasFirstSource = false) { playbackStops++ }
        advanceUntilIdle()

        assertEquals(listOf(ScanExecutionMode.BACKGROUND), coordinator.modes)
        assertEquals(0, playbackStops)
        assertFalse(manager.dedicated.value)
    }

    @Test
    fun failureLeavesDedicatedModeAndPublishesFailure() = runTest {
        val coordinator = RecordingScanCoordinator(failure = IllegalStateException("broken source"))
        val manager = ScanSessionManager(coordinator, this)

        manager.startDedicated {}
        advanceUntilIdle()

        assertFalse(manager.dedicated.value)
        assertEquals("Scan failed: broken source", manager.message.value)
    }

    @Test
    fun intentionalExitRequestsCheckpointBeforeLeaving() = runTest {
        val coordinator = RecordingScanCoordinator()
        val manager = ScanSessionManager(coordinator, this)

        manager.leaveDedicated()
        advanceUntilIdle()

        assertTrue(coordinator.cancelRequested)
        assertFalse(manager.dedicated.value)
    }

    private class RecordingScanCoordinator(
        private val failure: Throwable? = null,
    ) : ScanCoordinator {
        private val mutableProgress = MutableStateFlow<ScanProgress?>(null)
        override val progress: StateFlow<ScanProgress?> = mutableProgress
        val modes = mutableListOf<ScanExecutionMode>()
        var cancelRequested = false

        override suspend fun run(mode: ScanExecutionMode) {
            modes += mode
            failure?.let { throw it }
            mutableProgress.value = ScanProgress(
                phase = ScanPhase.COMPLETE,
                found = 8,
                processed = 7,
                skipped = 1,
                errors = 0,
                determinate = true,
            )
        }

        override suspend fun cancelAndCheckpoint() {
            cancelRequested = true
        }
    }
}
