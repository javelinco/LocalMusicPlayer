package com.javelinco.localmusicplayer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.javelinco.localmusicplayer.ui.library.BackupScreen
import com.javelinco.localmusicplayer.ui.components.AppScreenHeader
import org.junit.Rule
import org.junit.Test

class BackupUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun noFolderScreenIdentifiesTheAppAndTheFirstStep() {
        compose.setContent {
            MaterialTheme {
                Column {
                    AppScreenHeader("Backup & restore")
                    BackupScreen(null, emptyList(), null, {}, {}, {}, {})
                }
            }
        }

        compose.onNodeWithText("Music, Please!").assertIsDisplayed()
        compose.onNodeWithText("Backup & restore").assertIsDisplayed()
        compose.onNodeWithText("1. Choose a backup location").assertIsDisplayed()
        compose.onNodeWithText("Choose backup folder").assertIsDisplayed()
        compose.onNodeWithText("Choose a backup folder in step 1", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun configuredScreenShowsTheReadablePathAndNextActions() {
        compose.setContent {
            MaterialTheme {
                BackupScreen(
                    selectedFolder = "content://provider/tree/" +
                        "primary%3AMusic%2C%20Please%21%20Backups",
                    backupNames = emptyList(),
                    status = null,
                    onChooseFolder = {},
                    onManualBackup = {},
                    onRefresh = {},
                    onRestore = {},
                )
            }
        }

        compose.onNodeWithText("Current backup location").assertIsDisplayed()
        compose.onNodeWithText("Internal storage / Music, Please! Backups").assertIsDisplayed()
        compose.onNodeWithText("Change folder").assertIsDisplayed()
        compose.onNodeWithText("2. Create a backup").assertIsDisplayed()
        compose.onNodeWithText("Create backup now").assertIsDisplayed()
        compose.onNodeWithText("3. Restore from a backup")
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("No backups found in this folder", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }
}
