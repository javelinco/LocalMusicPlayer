package com.javelinco.localmusicplayer.ui.library

import com.javelinco.localmusicplayer.data.db.AlbumSummary
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

    @Test
    fun albumMatchesCompositeIdentityAndUsesDiscTrackFilenameOrder() {
        val album = AlbumSummary("chosen artist", "shared", "Chosen Artist", "Shared", 3)
        val tracks = listOf(
            track(
                "disc-two",
                artist = "chosen artist",
                genre = "rock",
                albumArtist = "chosen artist",
                album = "shared",
                disc = 2,
                number = 1,
            ),
            track(
                "other-artist",
                artist = "other artist",
                genre = "rock",
                albumArtist = "other artist",
                album = "shared",
                disc = 1,
                number = 1,
            ),
            track(
                "track-two",
                artist = "chosen artist",
                genre = "rock",
                albumArtist = "chosen artist",
                album = "shared",
                disc = 1,
                number = 2,
            ),
            track(
                "track-one",
                artist = "chosen artist",
                genre = "rock",
                albumArtist = "chosen artist",
                album = "shared",
                disc = 1,
                number = 1,
            ),
        )

        assertEquals(
            listOf("track-one", "track-two", "disc-two"),
            tracksForAlbum(album, tracks).map(TrackEntity::trackId),
        )
    }

    private fun track(
        id: String,
        artist: String,
        genre: String,
        albumArtist: String = artist,
        album: String = "album",
        disc: Int = 1,
        number: Int = 1,
    ) = TrackEntity(
        trackId = id,
        sourceId = "source",
        contentUri = "content://music/$id",
        fileName = "$id.mp3",
        title = id,
        artist = artist,
        albumTitle = album,
        albumArtist = albumArtist,
        genre = genre,
        normalizedTitle = id,
        normalizedArtist = artist,
        normalizedAlbumTitle = album,
        normalizedAlbumArtist = albumArtist,
        normalizedGenre = genre,
        discNumber = disc,
        trackNumber = number,
        durationMs = 1,
        modifiedAtEpochMs = 1,
        sizeBytes = 1,
        available = true,
    )
}
