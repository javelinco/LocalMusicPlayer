package com.javelinco.localmusicplayer.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.javelinco.localmusicplayer.data.db.RecentPlaylistRow
import com.javelinco.localmusicplayer.data.settings.SettingsState
import com.javelinco.localmusicplayer.playback.service.PlaybackUiState
import com.javelinco.localmusicplayer.ui.library.LibraryActions
import com.javelinco.localmusicplayer.ui.library.LibraryScreenState
import com.javelinco.localmusicplayer.ui.navigation.AppNavigation
import com.javelinco.localmusicplayer.ui.navigation.PrimaryDestination
import com.javelinco.localmusicplayer.ui.navigation.PrimaryNavigationBar
import org.junit.Rule
import org.junit.Test

class NavigationUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun persistentNavigationContainsOnlyHomeLibraryAndMore() {
        compose.setContent { PrimaryNavigationBar(PrimaryDestination.LIBRARY) {} }

        listOf("Home", "Library", "More").forEach {
            compose.onNodeWithText(it).assertIsDisplayed()
        }
        listOf("Search", "Playlists", "Sources").forEach {
            compose.onAllNodesWithText(it).assertCountEquals(0)
        }
    }

    @Test fun dedicatedScanReturnsToTheLibraryThatStartedIt() {
        lateinit var showDedicated: (Boolean) -> Unit
        compose.setContent {
            var dedicated by remember { mutableStateOf(false) }
            showDedicated = { dedicated = it }
            AppNavigation(
                libraryState = LibraryScreenState(),
                libraryActions = LibraryActions(),
                recentTracks = emptyList(),
                recentPlaylists = listOf(RecentPlaylistRow("mix", "Recent mix", 1)),
                recentLoaded = true,
                dedicated = dedicated,
                settings = SettingsState(),
                playback = PlaybackUiState(controllerReady = true, connected = true),
                backupNames = emptyList(),
                status = null,
                onLeaveDedicated = {},
                onPrevious = {},
                onPlayPause = {},
                onNext = {},
                onSeek = {},
                onShuffle = {},
                onRepeat = {},
                onChooseBackupFolder = {},
                onManualBackup = {},
                onRefreshBackups = {},
                onRestore = {},
                onTheme = {},
                onReducedMotion = {},
            )
        }

        compose.waitForIdle()
        compose.onNodeWithText("Library").performClick()
        compose.onNodeWithText("Where is your music?").assertIsDisplayed()
        compose.runOnUiThread { showDedicated(true) }
        compose.onNodeWithText("Dedicated scanning").assertIsDisplayed()
        compose.runOnUiThread { showDedicated(false) }
        compose.onNodeWithText("Where is your music?").assertIsDisplayed()
    }
}
