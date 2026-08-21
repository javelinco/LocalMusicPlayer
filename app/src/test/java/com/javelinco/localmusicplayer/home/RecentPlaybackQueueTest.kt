package com.javelinco.localmusicplayer.home

import com.javelinco.localmusicplayer.data.db.TrackEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecentPlaybackQueueTest {
    @Test
    fun selectedTrackKeepsTheCompleteDisplayedOrder() {
        val first = track("first")
        val selected = track("selected")
        val last = track("last")

        val queue = recentPlaybackQueue(selected.trackId, listOf(first, selected, last))

        assertEquals("selected", queue?.selected?.trackId)
        assertEquals(listOf("first", "selected", "last"), queue?.tracks?.map { it.trackId })
    }

    @Test
    fun missingSelectionDoesNotStartAQueue() {
        assertNull(recentPlaybackQueue("missing", listOf(track("present"))))
    }

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
