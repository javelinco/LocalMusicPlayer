package com.javelinco.localmusicplayer.data.source

import android.Manifest
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.javelinco.localmusicplayer.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PermissionFlowTest {
    @Test
    fun startupDoesNotChangeDeviceWideAudioPermission() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val permissionBeforeLaunch = context.checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO)

        ActivityScenario.launch(MainActivity::class.java).use {
            assertEquals(
                permissionBeforeLaunch,
                context.checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO),
            )
        }
    }
}
