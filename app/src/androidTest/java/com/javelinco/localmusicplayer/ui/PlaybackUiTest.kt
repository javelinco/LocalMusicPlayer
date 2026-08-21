package com.javelinco.localmusicplayer.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.media3.common.Player
import com.javelinco.localmusicplayer.playback.service.PlaybackUiState
import com.javelinco.localmusicplayer.ui.player.NowPlayingScreen
import org.junit.Rule
import org.junit.Test

class PlaybackUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun directControlsAndExplicitModesAreVisible() {
        compose.setContent {
            NowPlayingScreen(
                PlaybackUiState(title = "Song", repeatMode = Player.REPEAT_MODE_ONE),
                reducedMotion = false,
                {}, {}, {}, {}, {}, {}, {},
            )
        }

        listOf("Previous", "Play", "Next", "Shuffle off", "Repeat one", "Queue").forEach {
            compose.onNodeWithContentDescription(it).assertIsDisplayed()
        }
        compose.onNodeWithTag("transport-controls").assertIsDisplayed()
        compose.onNodeWithTag("playback-modes").assertIsDisplayed()
        compose.onAllNodesWithText("Favorite").assertCountEquals(0)
        compose.onAllNodesWithContentDescription("Music is playing").assertCountEquals(0)
    }
}
