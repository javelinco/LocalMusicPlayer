package com.javelinco.localmusicplayer.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class AppScreenHeaderTest {
    @Test
    fun everyDestinationHasTheExpectedScreenTitle() {
        val expected = mapOf(
            Destination.HOME to "Recently played",
            Destination.LIBRARY to "Library",
            Destination.MORE to "More",
            Destination.NOW_PLAYING to "Now playing",
            Destination.QUEUE to "Queue",
            Destination.MUSIC_FOLDERS to "Music folders & scanning",
            Destination.BACKUP to "Backup & restore",
            Destination.SETTINGS to "Appearance",
        )

        assertEquals(
            expected,
            Destination.entries.associateWith { screenHeaderTitle(it, homeIsPlaying = false) },
        )
    }

    @Test
    fun homeUsesNowPlayingTitleWhilePlaybackIsActive() {
        assertEquals("Now playing", screenHeaderTitle(Destination.HOME, homeIsPlaying = true))
    }
}
