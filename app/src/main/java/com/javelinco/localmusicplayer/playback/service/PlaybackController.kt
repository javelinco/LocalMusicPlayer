package com.javelinco.localmusicplayer.playback.service

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

class PlaybackController(context: Context) {
    private val applicationContext = context.applicationContext
    private var future: ListenableFuture<MediaController>? = null

    fun connect(): ListenableFuture<MediaController> =
        future ?: MediaController.Builder(
            applicationContext,
            SessionToken(applicationContext, ComponentName(applicationContext, PlaybackService::class.java)),
        ).buildAsync().also { future = it }

    fun release() {
        future?.let(MediaController::releaseFuture)
        future = null
    }
}
