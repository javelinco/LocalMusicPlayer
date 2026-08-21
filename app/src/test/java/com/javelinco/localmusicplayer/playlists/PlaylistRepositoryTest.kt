package com.javelinco.localmusicplayer.playlists

import com.javelinco.localmusicplayer.core.model.TrackId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistRepositoryTest {
    @Test
    fun duplicateTracksHaveStableIndependentEntriesAndCanBeReordered() = runTest {
        val repository = InMemoryPlaylistRepository()
        val playlist = repository.create("Mix")
        repository.addTracks(playlist, listOf(TrackId("a"), TrackId("a"), TrackId("b")))
        val original = repository.observeEntries(playlist).first()

        repository.moveEntry(playlist, 1, 0)
        val moved = repository.observeEntries(playlist).first()

        assertEquals(listOf(TrackId("a"), TrackId("a"), TrackId("b")), moved.map(PlaylistEntry::trackId))
        assertEquals(original[1].id, moved[0].id)
        assertEquals(original[0].id, moved[1].id)
    }

    @Test
    fun renameDeleteAndFavoriteAreExplicit() = runTest {
        val repository = InMemoryPlaylistRepository()
        val playlist = repository.create("Old")
        repository.rename(playlist, "New")
        repository.setFavorite(TrackId("a"), true)

        assertEquals("New", repository.observePlaylists().first().single().name)
        assertTrue(repository.observeFavorites().first().contains(TrackId("a")))

        repository.setFavorite(TrackId("a"), false)
        repository.delete(playlist)
        assertFalse(repository.observeFavorites().first().contains(TrackId("a")))
        assertTrue(repository.observePlaylists().first().isEmpty())
    }

    @Test
    fun m3uRoundTripPreservesOrderDuplicatesAndContentUris() {
        val items = listOf(
            M3uEntry("Song A", "content://music/a"),
            M3uEntry("Song A", "content://music/a"),
            M3uEntry("Song B", "content://music/b"),
        )

        assertEquals(items, M3uCodec.decode(M3uCodec.encode(items)))
    }
}
