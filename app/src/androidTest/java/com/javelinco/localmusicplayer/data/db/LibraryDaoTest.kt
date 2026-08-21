package com.javelinco.localmusicplayer.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.javelinco.localmusicplayer.core.model.SourceId
import com.javelinco.localmusicplayer.data.source.RoomSourceRegistry
import com.javelinco.localmusicplayer.data.source.SafTreeSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryDaoTest {
    private lateinit var database: LocalMusicDatabase

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LocalMusicDatabase::class.java,
        ).build()
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun ftsAndAlbumOrderingUseTheDeviceSqliteEngine() = runTest {
        val dao = database.libraryDao()
        val sources = RoomSourceRegistry(dao)
        sources.add(SafTreeSource(SourceId("one"), "content://tree/music", "Music"))
        sources.add(SafTreeSource(SourceId("two"), "content://tree/music", "Music again"))
        dao.applyScanBatch(
            ScanBatch(
                tracks = listOf(
                    track("second", "Hidden Phrase.mp3", disc = 1, number = 2),
                    track("first", "Opening.mp3", disc = 1, number = 1),
                ),
                checkpoint = ScanCheckpointEntity("source", "done", 2, 10),
            ),
        )

        assertEquals("second", dao.searchTracks("hidden*").single().trackId)
        assertEquals(listOf("first", "second"), dao.tracksForAlbum("artist", "album").map { it.trackId })
        assertEquals(1, sources.observeSources().first().size)
    }

    private fun track(id: String, fileName: String, disc: Int, number: Int) = TrackEntity(
        trackId = id,
        sourceId = "source",
        contentUri = "content://music/$id",
        fileName = fileName,
        title = null,
        artist = "Artist",
        albumTitle = "Album",
        albumArtist = "Artist",
        genre = null,
        normalizedTitle = "",
        normalizedArtist = "artist",
        normalizedAlbumTitle = "album",
        normalizedAlbumArtist = "artist",
        normalizedGenre = "",
        discNumber = disc,
        trackNumber = number,
        durationMs = 1,
        modifiedAtEpochMs = 1,
        sizeBytes = 1,
        available = true,
    )
}
