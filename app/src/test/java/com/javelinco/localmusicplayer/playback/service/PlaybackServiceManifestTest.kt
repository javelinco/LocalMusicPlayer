package com.javelinco.localmusicplayer.playback.service

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlaybackServiceManifestTest {
    @Test
    fun removingTheAppTaskStopsPlaybackService() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val serviceInfo = context.packageManager.getServiceInfo(
            ComponentName(context, PlaybackService::class.java),
            PackageManager.ComponentInfoFlags.of(0),
        )

        assertTrue(serviceInfo.flags and ServiceInfo.FLAG_STOP_WITH_TASK != 0)
    }
}
