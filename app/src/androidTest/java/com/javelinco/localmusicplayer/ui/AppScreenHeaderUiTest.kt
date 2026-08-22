package com.javelinco.localmusicplayer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.javelinco.localmusicplayer.ui.components.AppScreenHeader
import org.junit.Rule
import org.junit.Test

class AppScreenHeaderUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun headerIdentifiesTheAppAndCurrentScreen() {
        compose.setContent {
            MaterialTheme { AppScreenHeader("Queue") }
        }

        compose.onNodeWithText("Music, Please!").assertIsDisplayed()
        compose.onNodeWithText("Queue").assertIsDisplayed()
    }
}
