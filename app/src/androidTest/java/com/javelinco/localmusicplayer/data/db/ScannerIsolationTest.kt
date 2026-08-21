package com.javelinco.localmusicplayer.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScannerIsolationTest {
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
    fun applyingScanBatchCannotRewriteUserTables() = runTest {
        val userData = database.userDataDao()
        userData.upsertPlaylist(PlaylistEntity("p", "Favorites mix", 1, 1))
        userData.upsertPlaylistEntries(
            listOf(PlaylistEntryEntity("p", 0, "missing", "Missing", "content://missing", 1)),
        )
        userData.upsertFavorite(FavoriteEntity("missing", "Missing", "content://missing", 1))
        userData.saveQueueSession(QueueSessionEntity(1, "[\"missing\"]", 0, 0, 1))
        val before = userData.snapshot()

        database.libraryDao().applyScanBatch(
            ScanBatch(
                tracks = emptyList(),
                checkpoint = ScanCheckpointEntity("source", "complete", 0, 2),
                errors = listOf(ScanErrorEntity(sourceId = "source", contentUri = null, message = "test", occurredAtEpochMs = 2)),
            ),
        )

        assertEquals(before, userData.snapshot())
    }
}
