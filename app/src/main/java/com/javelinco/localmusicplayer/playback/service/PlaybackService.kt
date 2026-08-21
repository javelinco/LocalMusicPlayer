package com.javelinco.localmusicplayer.playback.service

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var sessionStore: PlaybackSessionFileStore
    private var shuffleOrderApplied: Boolean = false

    private val persistenceListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.containsAny(
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_PLAYBACK_STATE_CHANGED,
                    Player.EVENT_PLAY_WHEN_READY_CHANGED,
                    Player.EVENT_REPEAT_MODE_CHANGED,
                    Player.EVENT_TIMELINE_CHANGED,
                )
            ) saveSession(player)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        sessionStore = PlaybackSessionFileStore(this)
        sessionStore.load()?.takeIf { it.items.isNotEmpty() }?.let { restored ->
            player.setMediaItems(restored.items.map(PersistedMediaItem::toMediaItem))
            player.seekTo(restored.currentIndex.coerceIn(0, restored.items.lastIndex), restored.positionMs)
            player.repeatMode = restored.repeatMode
            shuffleOrderApplied = restored.shuffleOrderApplied
            player.prepare()
            player.pause()
        }
        player.addListener(persistenceListener)
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            saveSession(player)
            player.removeListener(persistenceListener)
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private fun saveSession(player: Player) {
        if (player.mediaItemCount == 0) return
        val items = (0 until player.mediaItemCount).mapNotNull { index ->
            val item = player.getMediaItemAt(index)
            val uri = item.localConfiguration?.uri ?: return@mapNotNull null
            PersistedMediaItem(
                mediaId = item.mediaId,
                uri = uri.toString(),
                title = item.mediaMetadata.title?.toString().orEmpty(),
                artist = item.mediaMetadata.artist?.toString().orEmpty(),
                album = item.mediaMetadata.albumTitle?.toString().orEmpty(),
            )
        }
        if (items.isEmpty()) return
        sessionStore.save(
            PersistedPlaybackSession(
                items = items,
                currentIndex = player.currentMediaItemIndex.coerceAtLeast(0),
                positionMs = player.currentPosition.coerceAtLeast(0),
                repeatMode = player.repeatMode,
                shuffleOrderApplied = shuffleOrderApplied,
            ),
        )
    }
}
