package com.javelinco.localmusicplayer.data.scan

import com.javelinco.localmusicplayer.data.db.ScanErrorEntity
import com.javelinco.localmusicplayer.data.db.TrackEntity
import com.javelinco.localmusicplayer.data.source.MusicSource
import com.javelinco.localmusicplayer.data.source.SourceEntry
import com.javelinco.localmusicplayer.data.source.SourceReader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield

class DefaultScanCoordinator(
    private val sourceProvider: suspend () -> List<MusicSource>,
    private val readerFactory: (MusicSource) -> SourceReader,
    private val extractor: Mp3MetadataExtractor,
    private val catalog: ScanCatalog,
    private val runtimeGate: ScanRuntimeGate = AlwaysAvailableScanRuntimeGate,
    private val batchSize: Int = 100,
    private val clock: () -> Long = System::currentTimeMillis,
) : ScanCoordinator {
    private val runMutex = Mutex()
    private val mutableProgress = MutableStateFlow<ScanProgress?>(null)
    override val progress: StateFlow<ScanProgress?> = mutableProgress.asStateFlow()

    @Volatile
    private var cancellationRequested = false

    override suspend fun run(mode: ScanExecutionMode) = runMutex.withLock {
        cancellationRequested = false
        if (mode == ScanExecutionMode.DEDICATED) runtimeGate.enterDedicated()
        try {
            mutableProgress.value = ScanProgress(ScanPhase.ENUMERATING)
            for (source in sourceProvider().filter(MusicSource::available)) {
                if (cancellationRequested) break
                scanSource(source, mode)
            }
            if (!cancellationRequested) {
                mutableProgress.value = mutableProgress.value?.copy(
                    phase = ScanPhase.COMPLETE,
                    determinate = true,
                )
            }
        } finally {
            if (mode == ScanExecutionMode.DEDICATED) runtimeGate.leaveDedicated()
        }
    }

    override suspend fun cancelAndCheckpoint() {
        cancellationRequested = true
    }

    private suspend fun scanSource(source: MusicSource, mode: ScanExecutionMode) {
        val tracks = mutableListOf<TrackEntity>()
        val errors = mutableListOf<ScanErrorEntity>()
        val seenTrackIds = catalog.existingTrackIds(source.id).toMutableSet()
        var checkpoint = catalog.checkpoint(source.id)
        readerFactory(source).enumerate(source, checkpoint)
            .takeWhile { !cancellationRequested }
            .collect { entry ->
            if (mode == ScanExecutionMode.BACKGROUND) {
                runtimeGate.awaitBackgroundWindow()
                yield()
            }
            updateProgress { it.copy(phase = ScanPhase.METADATA, found = it.found + 1) }
            checkpoint = entry.stableId
            if (!entry.isMp3()) {
                updateProgress { it.copy(skipped = it.skipped + 1) }
            } else {
                runCatching { extractor.extract(entry) }
                    .onSuccess { raw ->
                        val track = MetadataNormalizer.normalize(raw, entry).toTrack(entry)
                        tracks += track
                        seenTrackIds += track.trackId
                        updateProgress { it.copy(processed = it.processed + 1) }
                    }
                    .onFailure { error ->
                        errors += ScanErrorEntity(
                            sourceId = source.id.value,
                            contentUri = entry.contentUri,
                            message = error.message ?: error::class.java.simpleName,
                            occurredAtEpochMs = clock(),
                        )
                        updateProgress { it.copy(errors = it.errors + 1) }
                    }
            }
            if (tracks.size + errors.size >= batchSize || cancellationRequested) {
                flush(source, tracks, errors, checkpoint)
            }
        }
        flush(source, tracks, errors, checkpoint)
        if (!cancellationRequested) {
            updateProgress { it.copy(phase = ScanPhase.RECONCILING) }
            catalog.reconcile(source.id, seenTrackIds)
        }
    }

    private suspend fun flush(
        source: MusicSource,
        tracks: MutableList<TrackEntity>,
        errors: MutableList<ScanErrorEntity>,
        checkpoint: String?,
    ) {
        if (tracks.isEmpty() && errors.isEmpty() && checkpoint == null) return
        updateProgress { it.copy(phase = ScanPhase.INDEXING) }
        catalog.applyBatch(CatalogScanBatch(source.id, tracks.toList(), errors.toList(), checkpoint))
        tracks.clear()
        errors.clear()
    }

    private fun updateProgress(transform: (ScanProgress) -> ScanProgress) {
        mutableProgress.value = transform(mutableProgress.value ?: ScanProgress(ScanPhase.ENUMERATING))
    }
}

private fun SourceEntry.isMp3() =
    mimeType == "audio/mpeg" || displayName.endsWith(".mp3", ignoreCase = true)

private fun NormalizedTrackMetadata.toTrack(entry: SourceEntry) = TrackEntity(
    trackId = "${entry.sourceId.value}:${entry.stableId}",
    sourceId = entry.sourceId.value,
    contentUri = entry.contentUri,
    fileName = entry.displayName,
    title = title,
    artist = artist,
    albumTitle = albumTitle,
    albumArtist = albumArtist,
    genre = genre,
    normalizedTitle = normalizedTitle,
    normalizedArtist = normalizedArtist,
    normalizedAlbumTitle = normalizedAlbumTitle,
    normalizedAlbumArtist = normalizedAlbumArtist,
    normalizedGenre = normalizedGenre,
    discNumber = discNumber,
    trackNumber = trackNumber,
    durationMs = durationMs,
    modifiedAtEpochMs = entry.modifiedAtEpochMs ?: 0,
    sizeBytes = entry.sizeBytes ?: 0,
    available = true,
)
