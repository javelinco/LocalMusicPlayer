package com.javelinco.localmusicplayer.library

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.javelinco.localmusicplayer.core.model.PlaylistId
import com.javelinco.localmusicplayer.data.db.LocalMusicDatabase
import com.javelinco.localmusicplayer.data.db.ScanBatch
import com.javelinco.localmusicplayer.data.db.ScanCheckpointEntity
import com.javelinco.localmusicplayer.data.db.TrackEntity
import com.javelinco.localmusicplayer.playlists.PlaylistSummary
import com.javelinco.localmusicplayer.ui.library.LibraryView
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LibrarySearchTest {
    private lateinit var database: LocalMusicDatabase
    private lateinit var search: LibrarySearchEngine

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LocalMusicDatabase::class.java,
        ).allowMainThreadQueries().build()
        search = LibrarySearchEngine(database.libraryDao())
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun querySearchesOnlyTheSelectedLibraryView() = runTest {
        database.libraryDao().applyScanBatch(
            ScanBatch(
                tracks = listOf(track()),
                checkpoint = ScanCheckpointEntity("source", "one", 1, 1),
            ),
        )
        val playlists = listOf(PlaylistSummary(PlaylistId("road"), "Road Mix", 12))

        assertEquals(
            listOf("song"),
            (search.search(LibraryView.TRACKS, "winter", playlists) as LibrarySearchResult.Tracks)
                .items.map { it.trackId },
        )
        assertEquals(
            listOf("North Star"),
            (search.search(LibraryView.ARTISTS, "north", playlists) as LibrarySearchResult.NamedGroups)
                .items.map { it.displayName },
        )
        assertEquals(
            listOf("Blue Rooms"),
            (search.search(LibraryView.ALBUMS, "blue", playlists) as LibrarySearchResult.Albums)
                .items.map { it.displayTitle },
        )
        assertEquals(
            listOf("Ambient",),
            (search.search(LibraryView.GENRES, "ambi", playlists) as LibrarySearchResult.NamedGroups)
                .items.map { it.displayName },
        )
        assertEquals(
            listOf("Road Mix"),
            (search.search(LibraryView.PLAYLISTS, "road", playlists) as LibrarySearchResult.Playlists)
                .items.map { it.name },
        )
    }

    private fun track() = TrackEntity(
        trackId = "song",
        sourceId = "source",
        contentUri = "content://music/song",
        fileName = "winter-song.mp3",
        title = "Winter Song",
        artist = "North Star",
        albumTitle = "Blue Rooms",
        albumArtist = "North Star",
        genre = "Ambient",
        normalizedTitle = "winter song",
        normalizedArtist = "north star",
        normalizedAlbumTitle = "blue rooms",
        normalizedAlbumArtist = "north star",
        normalizedGenre = "ambient",
        discNumber = 1,
        trackNumber = 1,
        durationMs = 60_000,
        modifiedAtEpochMs = 1,
        sizeBytes = 1_000,
        available = true,
    )
}
