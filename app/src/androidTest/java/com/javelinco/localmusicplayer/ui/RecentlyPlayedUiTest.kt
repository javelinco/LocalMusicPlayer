package com.javelinco.localmusicplayer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.javelinco.localmusicplayer.data.db.RecentPlaylistRow
import com.javelinco.localmusicplayer.data.db.TrackEntity
import com.javelinco.localmusicplayer.home.RecentPlaybackQueue
import com.javelinco.localmusicplayer.ui.home.HomeScreen
import com.javelinco.localmusicplayer.ui.library.TrackActionCallbacks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class RecentlyPlayedUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun tappingRecentSongForwardsCompleteDisplayedQueue() {
        val tracks = listOf(track("first", "First song"), track("second", "Second song"))
        var request: RecentPlaybackQueue? = null

        compose.setContent {
            MaterialTheme {
                HomeScreen(
                    recentTracks = tracks,
                    recentPlaylists = emptyList(),
                    trackActions = trackActions(),
                    onPlayRecentQueue = { request = it },
                    onPlayPlaylist = {},
                    onRemoveRecentTrack = {},
                    onRemoveRecentPlaylist = {},
                )
            }
        }

        compose.onNodeWithText("Second song").performClick()

        compose.runOnIdle {
            assertNotNull(request)
            assertEquals("second", request?.selected?.trackId)
            assertEquals(listOf("first", "second"), request?.tracks?.map { it.trackId })
        }
    }

    @Test
    fun songAndPlaylistMenusRemoveOnlyTheirRecentEntries() {
        var removedTrackId: String? = null
        var removedPlaylistId: String? = null

        compose.setContent {
            MaterialTheme {
                HomeScreen(
                    recentTracks = listOf(track("song", "Recent song")),
                    recentPlaylists = listOf(RecentPlaylistRow("mix", "Recent mix", 3)),
                    trackActions = trackActions(),
                    onPlayRecentQueue = {},
                    onPlayPlaylist = {},
                    onRemoveRecentTrack = { removedTrackId = it },
                    onRemoveRecentPlaylist = { removedPlaylistId = it },
                )
            }
        }

        compose.onNodeWithContentDescription("More actions for Recent song").performClick()
        compose.onNodeWithText("Remove from recently played").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("song", removedTrackId) }

        compose.onNodeWithContentDescription("More actions for Recent mix").performClick()
        compose.onNodeWithText("Remove from recently played").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("mix", removedPlaylistId) }
    }

    private fun trackActions() = TrackActionCallbacks(
        onPlayNow = {},
        onPlayNext = {},
        onAddToQueue = {},
        onAddToPlaylist = {},
        onGoToArtist = {},
        onShowInformation = {},
        onRemoveFromLibrary = {},
    )

    private fun track(id: String, title: String) = TrackEntity(
        trackId = id,
        sourceId = "source",
        contentUri = "content://music/$id",
        fileName = "$id.mp3",
        title = title,
        artist = "Artist",
        albumTitle = "Album",
        albumArtist = "Artist",
        genre = "Genre",
        normalizedTitle = title.lowercase(),
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
