package com.javelinco.localmusicplayer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.javelinco.localmusicplayer.core.model.SourceId
import com.javelinco.localmusicplayer.data.scan.ScanPhase
import com.javelinco.localmusicplayer.data.scan.ScanProgress
import com.javelinco.localmusicplayer.data.source.MediaStoreSource
import com.javelinco.localmusicplayer.ui.library.SourcesScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SourcesScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun scanningScreenShowsProgressAndDismissibleResult() {
        var dismissed = false
        compose.setContent {
            MaterialTheme {
                SourcesScreen(
                    sources = listOf(MediaStoreSource(SourceId("device"), "All device music")),
                    onChooseFolder = {},
                    onFindAll = {},
                    onBackgroundScan = {},
                    onDedicatedScan = {},
                    scanProgress = ScanProgress(ScanPhase.METADATA, found = 12, processed = 4),
                    scanMessage = "Scan complete · 12 found · 4 indexed · 3 removed · 0 skipped · 0 errors",
                    onPrioritizeScan = {},
                    onDismissScanMessage = { dismissed = true },
                )
            }
        }

        compose.onNodeWithText("Scanning:", substring = true).assertIsDisplayed()
        compose.onNodeWithText("3 removed", substring = true).assertIsDisplayed()
        compose.onNodeWithContentDescription("Dismiss scan result").performClick()
        compose.runOnIdle { assertTrue(dismissed) }
    }
}
