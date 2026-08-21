package com.javelinco.localmusicplayer.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.javelinco.localmusicplayer.core.model.SourceId
import com.javelinco.localmusicplayer.data.source.RoomSourceRegistry
import com.javelinco.localmusicplayer.data.source.SafTreeSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LibraryDaoTest {
    private lateinit var database: LocalMusicDatabase
    private lateinit var libraryDao: LibraryDao
    private lateinit var userDataDao: UserDataDao

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LocalMusicDatabase::class.java,
        ).allowMainThreadQueries().build()
        libraryDao = database.libraryDao()
        userDataDao = database.userDataDao()
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun filenameFallbackSearchFindsUntaggedTrack() = runTest {
        libraryDao.applyScanBatch(
            ScanBatch(
                tracks = listOf(track(id = "one", fileName = "Buried Treasure.mp3")),
                checkpoint = checkpoint(),
            ),
        )

        val results = libraryDao.searchTracks("buried*")

        assertEquals(listOf("one"), results.map { it.trackId })
    }

    @Test
    fun persistedSourceRegistryDeduplicatesProviderUris() = runTest {
        val registry = RoomSourceRegistry(libraryDao)
        registry.add(SafTreeSource(SourceId("one"), "content://tree/music", "Music"))
        registry.add(SafTreeSource(SourceId("two"), "content://tree/music", "Music again"))

        val restored = registry.observeSources().first()

        assertEquals(1, restored.size)
        assertEquals(SourceId("one"), restored.single().id)
    }

    @Test
    fun albumGroupingUsesAlbumArtistAndTitleAndOrdersDiscThenTrack() = runTest {
        libraryDao.applyScanBatch(
            ScanBatch(
                tracks = listOf(
                    track("disc-two", albumArtist = "Various Artists", album = "Collection", disc = 2, number = 1),
                    track("disc-one-track-two", albumArtist = "Various Artists", album = "Collection", disc = 1, number = 2),
                    track("disc-one-track-one", albumArtist = "Various Artists", album = "Collection", disc = 1, number = 1),
                    track("other-artist", albumArtist = "Solo Artist", album = "Collection", disc = 1, number = 1),
                ),
                checkpoint = checkpoint(),
            ),
        )

        val groups = libraryDao.albumGroups()
        val tracks = libraryDao.tracksForAlbum("various artists", "collection")

        assertEquals(2, groups.size)
        assertEquals(
            listOf("disc-one-track-one", "disc-one-track-two", "disc-two"),
            tracks.map { it.trackId },
        )
    }

    @Test
    fun blankMetadataAppearsInUnknownBuckets() = runTest {
        libraryDao.applyScanBatch(
            ScanBatch(
                tracks = listOf(track("unknown")),
                checkpoint = checkpoint(),
            ),
        )

        assertEquals("Unknown Album", libraryDao.albumGroups().single().displayTitle)
        assertEquals("Unknown Artist", libraryDao.artistGroups().single().displayName)
        assertEquals("Unknown Genre", libraryDao.genreGroups().single().displayName)
    }

    @Test
    fun rescanningCatalogDoesNotModifyUserDataAndUnavailableTracksRemainReferenced() = runTest {
        val playlist = PlaylistEntity("playlist", "Road trip", 10, 10)
        val entry = PlaylistEntryEntity("playlist", 0, "old", "Old song", "content://old", 10)
        val favorite = FavoriteEntity("old", "Old song", "content://old", 10)
        val queue = QueueSessionEntity(1, "[\"old\"]", 0, 1234, 10)
        userDataDao.upsertPlaylist(playlist)
        userDataDao.upsertPlaylistEntries(listOf(entry))
        userDataDao.upsertFavorite(favorite)
        userDataDao.saveQueueSession(queue)
        libraryDao.applyScanBatch(
            ScanBatch(listOf(track("old")), checkpoint()),
        )

        val before = userDataDao.snapshot()
        libraryDao.applyScanBatch(
            ScanBatch(listOf(track("new")), checkpoint(cursor = "next")),
        )
        libraryDao.markSourceTracksUnavailable("source")

        assertEquals(before, userDataDao.snapshot())
        assertTrue(userDataDao.playlistEntries("playlist").single().trackId == "old")
        assertFalse(libraryDao.track("old")!!.available)
    }

    private fun checkpoint(cursor: String = "cursor") = ScanCheckpointEntity(
        sourceId = "source",
        cursor = cursor,
        scannedCount = 1,
        updatedAtEpochMs = 10,
    )

    private fun track(
        id: String,
        fileName: String = "$id.mp3",
        albumArtist: String? = null,
        album: String? = null,
        disc: Int? = null,
        number: Int? = null,
    ) = TrackEntity(
        trackId = id,
        sourceId = "source",
        contentUri = "content://music/$id",
        fileName = fileName,
        title = null,
        artist = null,
        albumTitle = album,
        albumArtist = albumArtist,
        genre = null,
        normalizedTitle = "",
        normalizedArtist = "",
        normalizedAlbumTitle = album?.lowercase().orEmpty(),
        normalizedAlbumArtist = albumArtist?.lowercase().orEmpty(),
        normalizedGenre = "",
        discNumber = disc,
        trackNumber = number,
        durationMs = 60_000,
        modifiedAtEpochMs = 10,
        sizeBytes = 1_000,
        available = true,
    )
}
