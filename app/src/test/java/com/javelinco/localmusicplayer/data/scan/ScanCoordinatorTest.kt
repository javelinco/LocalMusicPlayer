package com.javelinco.localmusicplayer.data.scan

import com.javelinco.localmusicplayer.core.model.SourceId
import com.javelinco.localmusicplayer.data.db.ScanErrorEntity
import com.javelinco.localmusicplayer.data.db.TrackEntity
import com.javelinco.localmusicplayer.data.source.MusicSource
import com.javelinco.localmusicplayer.data.source.SafTreeSource
import com.javelinco.localmusicplayer.data.source.SourceEntry
import com.javelinco.localmusicplayer.data.source.SourceReader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanCoordinatorTest {
    private val source = SafTreeSource(SourceId("source"), "content://tree/music", "Music")

    @Test
    fun corruptAndNonMp3EntriesDoNotStopTheScan() = runTest {
        val catalog = RecordingCatalog()
        val coordinator = DefaultScanCoordinator(
            sourceProvider = { listOf(source) },
            readerFactory = { ListReader(entries()) },
            extractor = FakeExtractor(),
            catalog = catalog,
            batchSize = 2,
        )

        coordinator.run(ScanExecutionMode.DEDICATED)

        assertEquals(listOf("one", "two"), catalog.tracks.map { it.trackId.substringAfterLast(':') })
        assertEquals(1, catalog.errors.size)
        assertEquals(ScanPhase.COMPLETE, coordinator.progress.value?.phase)
        assertEquals(1L, coordinator.progress.value?.skipped)
        assertEquals(1L, coordinator.progress.value?.errors)
        assertTrue(catalog.reconciled)
    }

    @Test
    fun cancellationCheckpointsAndResumeDoesNotRepeatCompletedEntry() = runTest {
        val catalog = RecordingCatalog()
        lateinit var coordinator: DefaultScanCoordinator
        catalog.afterBatch = { coordinator.cancelAndCheckpoint() }
        coordinator = DefaultScanCoordinator(
            sourceProvider = { listOf(source) },
            readerFactory = { ListReader(entries().filter { it.displayName.endsWith(".mp3") }) },
            extractor = FakeExtractor(),
            catalog = catalog,
            batchSize = 1,
        )

        coordinator.run(ScanExecutionMode.BACKGROUND)
        val firstPass = catalog.tracks.map(TrackEntity::trackId)
        catalog.afterBatch = null
        coordinator.run(ScanExecutionMode.BACKGROUND)

        assertEquals(firstPass.distinct(), firstPass)
        assertEquals(2, catalog.tracks.map(TrackEntity::trackId).distinct().size)
        assertEquals(ScanPhase.COMPLETE, coordinator.progress.value?.phase)
    }

    private fun entries() = listOf(
        entry("one", "One.mp3"),
        entry("skip", "Notes.txt", "text/plain"),
        entry("bad", "Bad.mp3"),
        entry("two", "Two.mp3"),
    )

    private fun entry(id: String, name: String, mime: String = "audio/mpeg") = SourceEntry(
        source.id,
        id,
        "content://music/$id",
        name,
        mime,
        1,
        1,
    )

    private class ListReader(private val entries: List<SourceEntry>) : SourceReader {
        override fun enumerate(source: MusicSource, checkpoint: String?): Flow<SourceEntry> = flow {
            val start = checkpoint?.let { value -> entries.indexOfFirst { it.stableId == value } + 1 } ?: 0
            entries.drop(start.coerceAtLeast(0)).forEach { emit(it) }
        }
    }

    private class FakeExtractor : Mp3MetadataExtractor {
        override suspend fun extract(entry: SourceEntry): RawMp3Metadata {
            if (entry.stableId == "bad") error("corrupt")
            return RawMp3Metadata(title = entry.displayName.removeSuffix(".mp3"), durationMs = 10)
        }

        override suspend fun extractArtwork(entry: SourceEntry): ByteArray? = null
    }

    private class RecordingCatalog : ScanCatalog {
        val tracks = mutableListOf<TrackEntity>()
        val errors = mutableListOf<ScanErrorEntity>()
        private val checkpoints = mutableMapOf<String, String?>()
        var reconciled = false
        var afterBatch: (suspend () -> Unit)? = null

        override suspend fun checkpoint(sourceId: SourceId): String? = checkpoints[sourceId.value]

        override suspend fun applyBatch(batch: CatalogScanBatch) {
            tracks += batch.tracks
            errors += batch.errors
            checkpoints[batch.sourceId.value] = batch.checkpoint
            afterBatch?.invoke()
        }

        override suspend fun reconcile(sourceId: SourceId, seenTrackIds: Set<String>) {
            reconciled = true
        }
    }
}
