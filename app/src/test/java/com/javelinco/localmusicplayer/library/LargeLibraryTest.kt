package com.javelinco.localmusicplayer.library

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.javelinco.localmusicplayer.data.db.LocalMusicDatabase
import com.javelinco.localmusicplayer.data.db.SourceEntity
import com.javelinco.localmusicplayer.data.db.TrackEntity
import com.javelinco.localmusicplayer.data.db.TrackSearchFts
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LargeLibraryTest {
    private lateinit var database: LocalMusicDatabase

    @Before fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            LocalMusicDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After fun closeDatabase() {
        if (::database.isInitialized) database.close()
    }

    @Test fun fiftyThousandTracksRemainCorrectAndSearchable() = runTest {
        val dao = database.libraryDao()
        dao.upsertSources(listOf(SourceEntity("large", "SAF_TREE", "content://large", "Large", true)))
        (0 until 50_000).chunked(1_000).forEach { indexes ->
            val tracks = indexes.map(::track)
            dao.upsertTracks(tracks)
            dao.insertSearchRows(tracks.map(TrackSearchFts::from))
        }
        val repository = LibraryRepository(dao)
        repository.search("uniqueneedle")
        var results = emptyList<TrackEntity>()
        val elapsed = measureTimeMillis { results = repository.search("uniqueneedle") }

        assertEquals(listOf("track-49999"), results.map { it.trackId })
        assertTrue("Warm search took ${elapsed}ms", elapsed < 100)
    }

    private fun track(index: Int) = TrackEntity(
        trackId = "track-$index",
        sourceId = "large",
        contentUri = "content://large/$index",
        fileName = "file-$index.mp3",
        title = if (index == 49_999) "UniqueNeedle" else "Track $index",
        artist = "Artist ${index % 100}",
        albumTitle = "Album ${index % 1_000}",
        albumArtist = null,
        genre = "Genre ${index % 12}",
        normalizedTitle = if (index == 49_999) "uniqueneedle" else "track $index",
        normalizedArtist = "artist ${index % 100}",
        normalizedAlbumTitle = "album ${index % 1_000}",
        normalizedAlbumArtist = "artist ${index % 100}",
        normalizedGenre = "genre ${index % 12}",
        discNumber = 1,
        trackNumber = index % 20,
        durationMs = 180_000,
        modifiedAtEpochMs = index.toLong(),
        sizeBytes = 4_000_000,
        available = true,
    )
}
