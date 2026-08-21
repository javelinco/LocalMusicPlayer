package com.javelinco.localmusicplayer.ui

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.javelinco.localmusicplayer.data.db.TrackEntity
import com.javelinco.localmusicplayer.ui.library.LibraryActions
import com.javelinco.localmusicplayer.ui.library.LibraryScreen
import com.javelinco.localmusicplayer.ui.library.LibraryScreenState
import com.javelinco.localmusicplayer.ui.library.LibraryView
import com.javelinco.localmusicplayer.ui.library.TrackList
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LibraryUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun firstRunStartsInTracksWithContextualSourceActions() {
        compose.setContent {
            LibraryScreen(
                state = LibraryScreenState(selectedView = LibraryView.TRACKS),
                actions = LibraryActions(),
            )
        }

        compose.onNodeWithText("Tracks").assertIsDisplayed()
        compose.onNodeWithContentDescription("Search Tracks").assertIsDisplayed()
        compose.onNodeWithContentDescription("Library tools").assertIsDisplayed()
        compose.onNodeWithText("Where is your music?").assertIsDisplayed()
        compose.onNodeWithText("Choose a folder").assertIsDisplayed()
    }

    @Test fun tracksRenderAsSeparateClickableCards() {
        var playedTrackId: String? = null
        compose.setContent {
            TrackList(
                tracks = listOf(track("one", "First track"), track("two", "Second track")),
                onPlay = { playedTrackId = it.trackId },
            )
        }

        compose.onNodeWithTag("track-card:one").assertIsDisplayed().assertHasClickAction()
        compose.onNodeWithTag("track-card:two").assertIsDisplayed().assertHasClickAction().performClick()
        compose.runOnIdle { assertEquals("two", playedTrackId) }
    }

    private fun track(id: String, title: String) = TrackEntity(
        trackId = id,
        sourceId = "source",
        contentUri = "content://music/$id",
        fileName = "$title.mp3",
        title = title,
        artist = "Artist $id",
        albumTitle = "Album $id",
        albumArtist = "Artist $id",
        genre = "Genre",
        normalizedTitle = title.lowercase(),
        normalizedArtist = "artist $id",
        normalizedAlbumTitle = "album $id",
        normalizedAlbumArtist = "artist $id",
        normalizedGenre = "genre",
        discNumber = 1,
        trackNumber = 1,
        durationMs = 180_000,
        modifiedAtEpochMs = 1,
        sizeBytes = 1,
        available = true,
    )
}
