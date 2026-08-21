package com.javelinco.localmusicplayer.data.scan

import com.javelinco.localmusicplayer.core.model.SourceId
import com.javelinco.localmusicplayer.data.db.LibraryDao
import com.javelinco.localmusicplayer.data.db.ScanBatch
import com.javelinco.localmusicplayer.data.db.ScanCheckpointEntity

class RoomScanCatalog(
    private val libraryDao: LibraryDao,
    private val clock: () -> Long = System::currentTimeMillis,
) : ScanCatalog {
    override suspend fun checkpoint(sourceId: SourceId): String? =
        libraryDao.checkpointForSource(sourceId.value)?.cursor

    override suspend fun existingTrackIds(sourceId: SourceId): Set<String> =
        libraryDao.trackIdsForSource(sourceId.value).toSet()

    override suspend fun applyBatch(batch: CatalogScanBatch) {
        val previousCount = libraryDao.checkpointForSource(batch.sourceId.value)?.scannedCount ?: 0
        libraryDao.applyScanBatch(
            ScanBatch(
                tracks = batch.tracks,
                checkpoint = ScanCheckpointEntity(
                    sourceId = batch.sourceId.value,
                    cursor = batch.checkpoint,
                    scannedCount = previousCount + batch.tracks.size + batch.errors.size,
                    updatedAtEpochMs = clock(),
                ),
                errors = batch.errors,
            ),
        )
    }

    override suspend fun reconcile(sourceId: SourceId, seenTrackIds: Set<String>) {
        val missing = libraryDao.trackIdsForSource(sourceId.value).filterNot(seenTrackIds::contains)
        if (missing.isNotEmpty()) libraryDao.markTracksUnavailable(missing)
    }
}
