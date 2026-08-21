package com.javelinco.localmusicplayer.home

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.javelinco.localmusicplayer.data.db.LocalMusicDatabase
import com.javelinco.localmusicplayer.data.db.PlaylistEntity
import com.javelinco.localmusicplayer.data.db.ScanBatch
import com.javelinco.localmusicplayer.data.db.ScanCheckpointEntity
import com.javelinco.localmusicplayer.data.db.TrackEntity
import kotlinx.coroutines.flow.first
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
class RecentPlayRepositoryTest {
    private lateinit var database: LocalMusicDatabase
    private lateinit var repository: RecentPlayRepository

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LocalMusicDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = RecentPlayRepository(database.recentPlayDao())
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun duplicateTrackMovesToFrontAndOnlyFiveAvailableTracksAreShown() = runTest {
        database.libraryDao().applyScanBatch(
            ScanBatch(
                tracks = (1..6).map { track("track-$it") },
                checkpoint = checkpoint(),
            ),
        )
        (1..6).forEach { repository.recordTrack("track-$it", it.toLong()) }
        repository.recordTrack("track-2", 10)

        assertEquals(
            listOf("track-2", "track-6", "track-5", "track-4", "track-3"),
            repository.observeRecentTracks().first().map { it.trackId },
        )

        database.libraryDao().markTracksUnavailable(listOf("track-2"))

        assertEquals(
            listOf("track-6", "track-5", "track-4", "track-3", "track-1"),
            repository.observeRecentTracks().first().map { it.trackId },
        )
    }

    @Test
    fun deletedPlaylistIsOmittedWithoutAffectingTrackHistory() = runTest {
        val userData = database.userDataDao()
        userData.upsertPlaylist(PlaylistEntity("morning", "Morning", 1, 1))
        userData.upsertPlaylist(PlaylistEntity("driving", "Driving", 2, 2))
        repository.recordPlaylist("morning", 10)
        repository.recordPlaylist("driving", 20)

        assertEquals(
            listOf("driving", "morning"),
            repository.observeRecentPlaylists().first().map { it.playlistId },
        )

        userData.deletePlaylist("driving")

        assertEquals(
            listOf("morning"),
            repository.observeRecentPlaylists().first().map { it.playlistId },
        )
        assertEquals(emptyList<TrackEntity>(), repository.observeRecentTracks().first())
    }

    private fun checkpoint() = ScanCheckpointEntity("source", "cursor", 6, 10)

    private fun track(id: String) = TrackEntity(
        trackId = id,
        sourceId = "source",
        contentUri = "content://music/$id",
        fileName = "$id.mp3",
        title = id,
        artist = "Artist",
        albumTitle = "Album",
        albumArtist = "Artist",
        genre = "Genre",
        normalizedTitle = id,
        normalizedArtist = "artist",
        normalizedAlbumTitle = "album",
        normalizedAlbumArtist = "artist",
        normalizedGenre = "genre",
        discNumber = 1,
        trackNumber = 1,
        durationMs = 60_000,
        modifiedAtEpochMs = 10,
        sizeBytes = 1_000,
        available = true,
    )
}
