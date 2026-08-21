package com.javelinco.localmusicplayer.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.javelinco.localmusicplayer.data.scan.ScanPhase
import com.javelinco.localmusicplayer.data.scan.ScanProgress
import com.javelinco.localmusicplayer.ui.library.DedicatedScanScreen
import org.junit.Rule
import org.junit.Test

class DedicatedScanUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun dedicatedModeExplainsExclusivityAndExplicitExit() {
        compose.setContent { DedicatedScanScreen(ScanProgress(ScanPhase.METADATA, found = 12, processed = 8)) {} }
        compose.onNodeWithText("Dedicated scanning").assertIsDisplayed()
        compose.onNodeWithText("Leave scanning mode and save progress").assertIsDisplayed()
        compose.onNodeWithText("Scanning:", substring = true).assertIsDisplayed()
    }
}
