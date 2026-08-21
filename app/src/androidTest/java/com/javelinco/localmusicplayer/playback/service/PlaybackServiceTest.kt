package com.javelinco.localmusicplayer.playback.service

import android.content.ComponentName
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.TimeUnit
import java.util.concurrent.CountDownLatch
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaybackServiceTest {
    @Test
    fun mediaSessionExposesPreviousAndNextAndRestoresPaused() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val future = MediaController.Builder(
            context,
            SessionToken(context, ComponentName(context, PlaybackService::class.java)),
        ).buildAsync()
        val controller = future.get(15, TimeUnit.SECONDS)
        val commandsChanged = CountDownLatch(1)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            controller.addListener(object : Player.Listener {
                override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {
                    commandsChanged.countDown()
                }
            })
            controller.setMediaItems(
                listOf(
                    MediaItem.fromUri("content://local/one"),
                    MediaItem.fromUri("content://local/two"),
                ),
            )
        }
        assertTrue(commandsChanged.await(10, TimeUnit.SECONDS))
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            try {
                assertTrue(controller.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS))
                assertTrue(controller.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT))
                assertFalse(controller.playWhenReady)
            } finally {
                MediaController.releaseFuture(future)
            }
        }
    }
}
