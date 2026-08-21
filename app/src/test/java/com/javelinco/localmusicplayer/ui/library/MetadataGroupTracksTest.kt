package com.javelinco.localmusicplayer.ui.library

import com.javelinco.localmusicplayer.data.db.TrackEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class MetadataGroupTracksTest {
    @Test
    fun artistMatchesExactlyAndKeepsLibraryOrder() {
        val tracks = listOf(
            track("second", artist = "chosen", genre = "jazz"),
            track("other", artist = "another", genre = "jazz"),
            track("first", artist = "chosen", genre = "rock"),
        )

        val result = tracksForMetadataGroup(LibraryView.ARTISTS, "chosen", tracks)

        assertEquals(listOf("second", "first"), result.map(TrackEntity::trackId))
    }

    @Test
    fun genreMatchesExactlyAndKeepsLibraryOrder() {
        val tracks = listOf(
            track("third", artist = "one", genre = "jazz"),
            track("other", artist = "two", genre = "rock"),
            track("first", artist = "three", genre = "jazz"),
        )

        val result = tracksForMetadataGroup(LibraryView.GENRES, "jazz", tracks)

        assertEquals(listOf("third", "first"), result.map(TrackEntity::trackId))
    }

    private fun track(id: String, artist: String, genre: String) = TrackEntity(
        trackId = id,
        sourceId = "source",
        contentUri = "content://music/$id",
        fileName = "$id.mp3",
        title = id,
        artist = artist,
        albumTitle = "album",
        albumArtist = artist,
        genre = genre,
        normalizedTitle = id,
        normalizedArtist = artist,
        normalizedAlbumTitle = "album",
        normalizedAlbumArtist = artist,
        normalizedGenre = genre,
        discNumber = 1,
        trackNumber = 1,
        durationMs = 1,
        modifiedAtEpochMs = 1,
        sizeBytes = 1,
        available = true,
    )
}
