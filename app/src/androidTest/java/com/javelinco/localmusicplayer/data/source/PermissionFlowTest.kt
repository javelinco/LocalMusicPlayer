package com.javelinco.localmusicplayer.data.source

import android.Manifest
import android.content.pm.PackageManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.javelinco.localmusicplayer.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PermissionFlowTest {
    @Test
    fun startupDoesNotRequestDeviceWideAudioPermission() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        InstrumentationRegistry.getInstrumentation().uiAutomation.runCatching {
            revokeRuntimePermission(context.packageName, Manifest.permission.READ_MEDIA_AUDIO)
        }

        ActivityScenario.launch(MainActivity::class.java).use {
            assertEquals(
                PackageManager.PERMISSION_DENIED,
                context.checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO),
            )
        }
    }
}
