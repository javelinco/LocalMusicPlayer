package com.javelinco.localmusicplayer.library

import com.javelinco.localmusicplayer.data.scan.ScanCoordinator
import com.javelinco.localmusicplayer.data.scan.ScanExecutionMode
import com.javelinco.localmusicplayer.data.scan.ScanPhase
import com.javelinco.localmusicplayer.data.scan.ScanProgress
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
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
        val coordinator = BlockingScanCoordinator()
        val manager = ScanSessionManager(coordinator, this)

        manager.startDedicated {}
        runCurrent()
        assertTrue(manager.dedicated.value)
        manager.leaveDedicated()
        advanceUntilIdle()

        assertTrue(coordinator.cancelRequested)
        assertTrue(coordinator.runFinished)
        assertFalse(manager.dedicated.value)
    }

    @Test
    fun laterSourceAddedDuringScanQueuesAnotherBackgroundPass() = runTest {
        val coordinator = BlockingFirstScanCoordinator()
        val manager = ScanSessionManager(coordinator, this)

        manager.startBackground()
        runCurrent()
        manager.sourceAdded(wasFirstSource = false) {}
        coordinator.releaseFirst.complete(Unit)
        advanceUntilIdle()

        assertEquals(
            listOf(ScanExecutionMode.BACKGROUND, ScanExecutionMode.BACKGROUND),
            coordinator.modes,
        )
    }

    @Test
    fun switchingToDedicatedCheckpointsAndJoinsTheActiveScan() = runTest {
        val coordinator = BlockingScanCoordinator()
        val manager = ScanSessionManager(coordinator, this)
        var playbackStops = 0

        manager.startBackground()
        runCurrent()
        manager.startDedicated { playbackStops++ }
        advanceUntilIdle()

        assertTrue(coordinator.cancelRequested)
        assertTrue(coordinator.runFinished)
        assertEquals(
            listOf(ScanExecutionMode.BACKGROUND, ScanExecutionMode.DEDICATED),
            coordinator.modes,
        )
        assertEquals(1, playbackStops)
    }

    @Test
    fun duplicateDedicatedRequestsAreCoalescedAndLeaveSuppressesTheQueuedStart() = runTest {
        val coordinator = BlockingScanCoordinator()
        val manager = ScanSessionManager(coordinator, this)
        var playbackStops = 0

        manager.startBackground()
        runCurrent()
        manager.startDedicated { playbackStops++ }
        manager.startDedicated { playbackStops++ }
        manager.leaveDedicated()
        advanceUntilIdle()

        assertTrue(coordinator.cancelRequested)
        assertEquals(listOf(ScanExecutionMode.BACKGROUND), coordinator.modes)
        assertEquals(0, playbackStops)
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

    private class BlockingFirstScanCoordinator : ScanCoordinator {
        private val mutableProgress = MutableStateFlow<ScanProgress?>(null)
        override val progress: StateFlow<ScanProgress?> = mutableProgress
        val releaseFirst = CompletableDeferred<Unit>()
        val modes = mutableListOf<ScanExecutionMode>()

        override suspend fun run(mode: ScanExecutionMode) {
            modes += mode
            if (modes.size == 1) releaseFirst.await()
            mutableProgress.value = ScanProgress(phase = ScanPhase.COMPLETE)
        }

        override suspend fun cancelAndCheckpoint() = Unit
    }

    private class BlockingScanCoordinator : ScanCoordinator {
        private val mutableProgress = MutableStateFlow<ScanProgress?>(null)
        override val progress: StateFlow<ScanProgress?> = mutableProgress
        private val release = CompletableDeferred<Unit>()
        val modes = mutableListOf<ScanExecutionMode>()
        var cancelRequested = false
        var runFinished = false

        override suspend fun run(mode: ScanExecutionMode) {
            modes += mode
            if (modes.size == 1) release.await()
            runFinished = true
            mutableProgress.value = ScanProgress(phase = ScanPhase.COMPLETE)
        }

        override suspend fun cancelAndCheckpoint() {
            cancelRequested = true
            release.complete(Unit)
        }
    }
}
