package com.javelinco.localmusicplayer.ui

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.javelinco.localmusicplayer.core.model.SourceId
import com.javelinco.localmusicplayer.core.model.PlaylistId
import com.javelinco.localmusicplayer.data.db.NamedGroupSummary
import com.javelinco.localmusicplayer.data.db.PlaylistEntryEntity
import com.javelinco.localmusicplayer.data.db.TrackEntity
import com.javelinco.localmusicplayer.data.source.SafTreeSource
import com.javelinco.localmusicplayer.ui.library.LibraryActions
import com.javelinco.localmusicplayer.ui.library.LibraryScreen
import com.javelinco.localmusicplayer.ui.library.LibraryScreenState
import com.javelinco.localmusicplayer.ui.library.LibraryView
import com.javelinco.localmusicplayer.ui.library.PlaylistScreen
import com.javelinco.localmusicplayer.ui.library.TrackList
import com.javelinco.localmusicplayer.playlists.PlaylistSummary
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

    @Test fun scanResultCanBeDismissed() {
        var dismissed = false
        compose.setContent {
            LibraryScreen(
                state = LibraryScreenState(
                    scanMessage = "Scan complete",
                    sources = listOf(source()),
                ),
                actions = LibraryActions(onDismissScanMessage = { dismissed = true }),
            )
        }

        compose.onNodeWithContentDescription("Dismiss scan result").performClick()
        compose.runOnIdle { assertEquals(true, dismissed) }
    }

    @Test fun trackAddsToChosenPlaylistWithoutPlaying() {
        var played = false
        var addition: Pair<String, List<String>>? = null
        compose.setContent {
            LibraryScreen(
                state = LibraryScreenState(
                    tracks = listOf(track("one", "First track")),
                    playlists = listOf(playlist()),
                    sources = listOf(source()),
                ),
                actions = LibraryActions(
                    onPlayTrack = { played = true },
                    onAddTracksToPlaylist = { playlistId, trackIds -> addition = playlistId to trackIds },
                ),
            )
        }

        compose.onNodeWithContentDescription("Add First track to playlist").performClick()
        compose.onNodeWithText("Road Mix").performClick()
        compose.runOnIdle {
            assertEquals(false, played)
            assertEquals("mix" to listOf("one"), addition)
        }
    }

    @Test fun emptyPlaylistPickerRoutesToPlaylistCreation() {
        var selectedView: LibraryView? = null
        compose.setContent {
            LibraryScreen(
                state = LibraryScreenState(
                    tracks = listOf(track("one", "First track")),
                    sources = listOf(source()),
                ),
                actions = LibraryActions(onSelectView = { selectedView = it }),
            )
        }

        compose.onNodeWithContentDescription("Add First track to playlist").performClick()
        compose.onNodeWithText("Create a playlist first.").assertIsDisplayed()
        compose.onNodeWithText("Go to playlists").performClick()
        compose.runOnIdle { assertEquals(LibraryView.PLAYLISTS, selectedView) }
    }

    @Test fun artistOpensMatchingTracksAndAddsAllInLibraryOrder() {
        var addition: Pair<String, List<String>>? = null
        compose.setContent {
            LibraryScreen(
                state = LibraryScreenState(
                    selectedView = LibraryView.ARTISTS,
                    tracks = listOf(
                        track("one", "First track", artist = "Artist One"),
                        track("two", "Second track", artist = "Artist One"),
                        track("three", "Other track", artist = "Artist Two"),
                    ),
                    artists = listOf(
                        NamedGroupSummary("artist one", "Artist One", 2),
                        NamedGroupSummary("artist two", "Artist Two", 1),
                    ),
                    playlists = listOf(playlist()),
                    sources = listOf(source()),
                ),
                actions = LibraryActions(
                    onAddTracksToPlaylist = { playlistId, trackIds -> addition = playlistId to trackIds },
                ),
            )
        }

        compose.onNodeWithText("Artist One").performClick()
        compose.onNodeWithText("First track").assertIsDisplayed()
        compose.onNodeWithText("Second track").assertIsDisplayed()
        compose.onAllNodesWithText("Other track").assertCountEquals(0)
        compose.onNodeWithText("Add all to playlist").performClick()
        compose.onNodeWithText("Road Mix").performClick()
        compose.runOnIdle { assertEquals("mix" to listOf("one", "two"), addition) }
    }

    @Test fun artistAddsDirectlyFromItsLibraryRow() {
        var addition: Pair<String, List<String>>? = null
        compose.setContent {
            LibraryScreen(
                state = LibraryScreenState(
                    selectedView = LibraryView.ARTISTS,
                    tracks = listOf(
                        track("one", "First track", artist = "Artist One"),
                        track("two", "Second track", artist = "Artist One"),
                    ),
                    artists = listOf(NamedGroupSummary("artist one", "Artist One", 2)),
                    playlists = listOf(playlist()),
                    sources = listOf(source()),
                ),
                actions = LibraryActions(
                    onAddTracksToPlaylist = { playlistId, trackIds -> addition = playlistId to trackIds },
                ),
            )
        }

        compose.onNodeWithContentDescription("Add Artist One to playlist").performClick()
        compose.onNodeWithText("Road Mix").performClick()
        compose.runOnIdle { assertEquals("mix" to listOf("one", "two"), addition) }
    }

    @Test fun genreOpensMatchingTracksAndAddsAllInLibraryOrder() {
        var addition: Pair<String, List<String>>? = null
        compose.setContent {
            LibraryScreen(
                state = LibraryScreenState(
                    selectedView = LibraryView.GENRES,
                    tracks = listOf(
                        track("one", "First track", genre = "Jazz"),
                        track("two", "Second track", genre = "Jazz"),
                        track("three", "Other track", genre = "Rock"),
                    ),
                    genres = listOf(
                        NamedGroupSummary("jazz", "Jazz", 2),
                        NamedGroupSummary("rock", "Rock", 1),
                    ),
                    playlists = listOf(playlist()),
                    sources = listOf(source()),
                ),
                actions = LibraryActions(
                    onAddTracksToPlaylist = { playlistId, trackIds -> addition = playlistId to trackIds },
                ),
            )
        }

        compose.onNodeWithText("Jazz").performClick()
        compose.onNodeWithText("First track").assertIsDisplayed()
        compose.onNodeWithText("Second track").assertIsDisplayed()
        compose.onAllNodesWithText("Other track").assertCountEquals(0)
        compose.onNodeWithText("Add all to playlist").performClick()
        compose.onNodeWithText("Road Mix").performClick()
        compose.runOnIdle { assertEquals("mix" to listOf("one", "two"), addition) }
    }

    @Test fun playlistRowsClearlyOpenTheirTracks() {
        val playlist = playlist(trackCount = 1)
        val track = track("one", "First track")
        compose.setContent {
            PlaylistScreen(
                playlists = listOf(playlist),
                entries = listOf(
                    PlaylistEntryEntity("entry", "mix", 0, "one", "First track", track.contentUri, 1),
                ),
                tracks = listOf(track),
                onPlay = {},
                onCreate = {},
                onRename = { _, _ -> },
                onDelete = {},
                onAdd = { _, _ -> },
                onRemove = { _, _ -> },
                onMove = { _, _, _ -> },
            )
        }

        compose.onNodeWithContentDescription("Open Road Mix").assertIsDisplayed()
        compose.onNodeWithText("Road Mix").performClick()
        compose.onNodeWithText("Playlist order").assertIsDisplayed()
        compose.onAllNodesWithText("First track").assertCountEquals(2)
    }

    private fun source() = SafTreeSource(SourceId("source"), "content://music", "Music")

    private fun playlist(trackCount: Int = 0) = PlaylistSummary(PlaylistId("mix"), "Road Mix", trackCount)

    private fun track(
        id: String,
        title: String,
        artist: String = "Artist $id",
        genre: String = "Genre",
    ) = TrackEntity(
        trackId = id,
        sourceId = "source",
        contentUri = "content://music/$id",
        fileName = "$title.mp3",
        title = title,
        artist = artist,
        albumTitle = "Album $id",
        albumArtist = "Artist $id",
        genre = genre,
        normalizedTitle = title.lowercase(),
        normalizedArtist = artist.lowercase(),
        normalizedAlbumTitle = "album $id",
        normalizedAlbumArtist = "artist $id",
        normalizedGenre = genre.lowercase(),
        discNumber = 1,
        trackNumber = 1,
        durationMs = 180_000,
        modifiedAtEpochMs = 1,
        sizeBytes = 1,
        available = true,
    )
}
