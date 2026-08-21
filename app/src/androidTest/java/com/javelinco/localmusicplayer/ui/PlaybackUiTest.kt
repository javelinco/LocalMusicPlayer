package com.javelinco.localmusicplayer.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
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
                favorite = false,
                {}, {}, {}, {}, {}, {}, {}, {},
            )
        }
        listOf("Previous", "Play", "Next", "Favorite", "Queue", "Shuffle Off", "Repeat One").forEach {
            compose.onNodeWithText(it).assertIsDisplayed()
        }
    }
}
