package com.javelinco.localmusicplayer.data.scan

import com.javelinco.localmusicplayer.core.model.SourceId
import com.javelinco.localmusicplayer.data.db.ScanErrorEntity
import com.javelinco.localmusicplayer.data.db.TrackEntity
import kotlinx.coroutines.flow.StateFlow

enum class ScanPhase { ENUMERATING, METADATA, INDEXING, ARTWORK, RECONCILING, COMPLETE }

enum class ScanExecutionMode { BACKGROUND, DEDICATED }

data class ScanProgress(
    val phase: ScanPhase,
    val found: Long = 0,
    val processed: Long = 0,
    val skipped: Long = 0,
    val errors: Long = 0,
    val determinate: Boolean = false,
)

data class CatalogScanBatch(
    val sourceId: SourceId,
    val tracks: List<TrackEntity>,
    val errors: List<ScanErrorEntity>,
    val checkpoint: String?,
)

interface ScanCatalog {
    suspend fun checkpoint(sourceId: SourceId): String?
    suspend fun existingTrackIds(sourceId: SourceId): Set<String> = emptySet()
    suspend fun applyBatch(batch: CatalogScanBatch)
    suspend fun reconcile(sourceId: SourceId, seenTrackIds: Set<String>)
}

interface ScanCoordinator {
    val progress: StateFlow<ScanProgress?>
    suspend fun run(mode: ScanExecutionMode)
    suspend fun cancelAndCheckpoint()
}

interface ScanRuntimeGate {
    suspend fun enterDedicated() = Unit
    suspend fun leaveDedicated() = Unit
    suspend fun awaitBackgroundWindow() = Unit
}

object AlwaysAvailableScanRuntimeGate : ScanRuntimeGate
