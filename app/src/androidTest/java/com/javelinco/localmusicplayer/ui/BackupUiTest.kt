package com.javelinco.localmusicplayer.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.javelinco.localmusicplayer.ui.library.BackupScreen
import org.junit.Rule
import org.junit.Test

class BackupUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun policyAndFolderChoiceAreClear() {
        compose.setContent { BackupScreen(null, emptyList(), null, {}, {}, {}, {}) }
        compose.onNodeWithText("Choose backup folder").assertIsDisplayed()
        compose.onNodeWithText("Automatic backups run at most daily", substring = true).assertIsDisplayed()
        compose.onNodeWithText("No USB-visible backup folder selected").assertIsDisplayed()
    }
}
