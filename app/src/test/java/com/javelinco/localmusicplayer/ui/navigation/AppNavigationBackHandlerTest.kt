package com.javelinco.localmusicplayer.ui.navigation

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.javelinco.localmusicplayer.data.settings.SettingsState
import com.javelinco.localmusicplayer.playback.service.PlaybackUiState
import com.javelinco.localmusicplayer.ui.library.LibraryActions
import com.javelinco.localmusicplayer.ui.library.LibraryScreenState
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppNavigationBackHandlerTest {
    private val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
    private val activity = controller.get()

    @After
    fun tearDown() {
        controller.destroy()
    }

    @Test
    fun systemBackAtRootStaysInsideTheActivity() {
        activity.setContent {
            AppNavigation(
                libraryState = LibraryScreenState(),
                libraryActions = LibraryActions(),
                recentTracks = emptyList(),
                recentPlaylists = emptyList(),
                recentLoaded = true,
                dedicated = false,
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
        shadowOf(activity.mainLooper).idle()

        activity.onBackPressedDispatcher.onBackPressed()
        shadowOf(activity.mainLooper).idle()

        assertFalse(activity.isFinishing)
    }
}
