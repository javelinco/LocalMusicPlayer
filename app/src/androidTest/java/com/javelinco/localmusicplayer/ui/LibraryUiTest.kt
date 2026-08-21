package com.javelinco.localmusicplayer.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.javelinco.localmusicplayer.ui.library.LibraryScreen
import org.junit.Rule
import org.junit.Test

class LibraryUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun metadataDestinationsAndEmptyGuidanceAreVisible() {
        compose.setContent { LibraryScreen(emptyList(), emptySet(), {}, { _, _ -> }) }
        listOf("Tracks", "Artists", "Albums", "Genres").forEach {
            compose.onNodeWithText(it).assertIsDisplayed()
        }
        compose.onNodeWithText("No scanned MP3s yet. Add a source, then start a scan.").assertIsDisplayed()
    }
}
