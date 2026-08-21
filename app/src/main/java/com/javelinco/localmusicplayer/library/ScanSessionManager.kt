package com.javelinco.localmusicplayer.library

import com.javelinco.localmusicplayer.data.scan.ScanCoordinator
import com.javelinco.localmusicplayer.data.scan.ScanExecutionMode
import com.javelinco.localmusicplayer.data.scan.ScanPhase
import com.javelinco.localmusicplayer.data.scan.ScanProgress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScanSessionManager(
    private val coordinator: ScanCoordinator,
    private val scope: CoroutineScope,
) {
    private val mutableDedicated = MutableStateFlow(false)
    val dedicated = mutableDedicated.asStateFlow()
    private val mutableMessage = MutableStateFlow<String?>(null)
    val message = mutableMessage.asStateFlow()
    private var scanJob: Job? = null

    fun sourceAdded(wasFirstSource: Boolean, stopPlayback: () -> Unit) {
        if (wasFirstSource) startDedicated(stopPlayback) else startBackground()
    }

    fun startBackground() {
        if (scanJob?.isActive == true) return
        scanJob = scope.launch { runScan(ScanExecutionMode.BACKGROUND) }
    }

    fun startDedicated(stopPlayback: () -> Unit) {
        scanJob?.cancel()
        scanJob = scope.launch { runDedicated(stopPlayback) }
    }

    fun prioritize(stopPlayback: () -> Unit) {
        val previous = scanJob
        scanJob = scope.launch {
            coordinator.cancelAndCheckpoint()
            previous?.join()
            runDedicated(stopPlayback)
        }
    }

    fun leaveDedicated() {
        scope.launch {
            coordinator.cancelAndCheckpoint()
            mutableDedicated.value = false
        }
    }

    private suspend fun runDedicated(stopPlayback: () -> Unit) {
        stopPlayback()
        mutableDedicated.value = true
        try {
            runScan(ScanExecutionMode.DEDICATED)
        } finally {
            mutableDedicated.value = false
        }
    }

    private suspend fun runScan(mode: ScanExecutionMode) {
        mutableMessage.value = null
        try {
            coordinator.run(mode)
            coordinator.progress.value
                ?.takeIf { it.phase == ScanPhase.COMPLETE }
                ?.let { mutableMessage.value = it.completionMessage() }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            mutableMessage.value = "Scan failed: ${error.message ?: "unknown error"}"
        }
    }
}

private fun ScanProgress.completionMessage() =
    "Scan complete · $processed indexed · $skipped skipped · $errors errors"
