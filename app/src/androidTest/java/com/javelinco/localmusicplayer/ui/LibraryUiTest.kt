package com.javelinco.localmusicplayer.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.javelinco.localmusicplayer.ui.library.LibraryActions
import com.javelinco.localmusicplayer.ui.library.LibraryScreen
import com.javelinco.localmusicplayer.ui.library.LibraryScreenState
import com.javelinco.localmusicplayer.ui.library.LibraryView
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
}
