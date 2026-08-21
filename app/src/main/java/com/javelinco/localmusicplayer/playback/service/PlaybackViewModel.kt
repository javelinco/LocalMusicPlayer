package com.javelinco.localmusicplayer.playback.service

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.javelinco.localmusicplayer.data.db.TrackEntity
import java.security.SecureRandom
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlaybackUiState(
    val connected: Boolean = false,
    val hasSession: Boolean = false,
    val currentMediaId: String? = null,
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
)

class PlaybackViewModel(application: Application) : AndroidViewModel(application) {
    private val connection = PlaybackController(application)
    private val mutableState = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = mutableState.asStateFlow()
    private var controller: androidx.media3.session.MediaController? = null
    private var tracks: List<TrackEntity> = emptyList()
    private var progressJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = update(player)
    }

    init {
        val future = connection.connect()
        future.addListener(
            {
                runCatching { future.get() }.onSuccess { connected ->
                    controller = connected
                    connected.addListener(listener)
                    update(connected)
                }
            },
            application.mainExecutor,
        )
    }

    fun play(track: TrackEntity, view: List<TrackEntity>) {
        tracks = view.filter(TrackEntity::available)
        val player = controller ?: return
        val index = tracks.indexOfFirst { it.trackId == track.trackId }.coerceAtLeast(0)
        player.setMediaItems(tracks.map(MediaItemMapper::toMediaItem), index, 0)
        player.prepare()
        player.play()
    }

    fun togglePlayPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun previous() {
        controller?.seekToPrevious()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0))
    }

    fun toggleShuffle() {
        val player = controller ?: return
        if (player.mediaItemCount == 0) return
        val enabling = !mutableState.value.shuffleEnabled
        val currentId = player.currentMediaItem?.mediaId ?: return
        val position = player.currentPosition
        val ordered = if (enabling) {
            val remaining = tracks.filterNot { it.trackId == currentId }.toMutableList()
            val random = SecureRandom()
            for (i in remaining.lastIndex downTo 1) {
                val j = random.nextInt(i + 1)
                val temporary = remaining[i]
                remaining[i] = remaining[j]
                remaining[j] = temporary
            }
            listOfNotNull(tracks.find { it.trackId == currentId }) + remaining
        } else {
            tracks
        }
        val index = ordered.indexOfFirst { it.trackId == currentId }.coerceAtLeast(0)
        player.setMediaItems(ordered.map(MediaItemMapper::toMediaItem), index, position)
        player.prepare()
        if (mutableState.value.isPlaying) player.play()
        mutableState.value = mutableState.value.copy(shuffleEnabled = enabling)
    }

    fun cycleRepeat() {
        val player = controller ?: return
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun stopForDedicatedScan() {
        controller?.pause()
    }

    private fun update(player: Player) {
        val metadata = player.mediaMetadata
        mutableState.value = mutableState.value.copy(
            connected = true,
            hasSession = player.mediaItemCount > 0,
            currentMediaId = player.currentMediaItem?.mediaId,
            title = metadata.title?.toString().orEmpty(),
            artist = metadata.artist?.toString().orEmpty(),
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0),
            durationMs = player.duration.takeIf { it > 0 } ?: 0,
            repeatMode = player.repeatMode,
        )
        if (player.isPlaying && progressJob?.isActive != true) {
            progressJob = viewModelScope.launch {
                while (true) {
                    delay(500)
                    controller?.let(::update)
                }
            }
        } else if (!player.isPlaying) {
            progressJob?.cancel()
            progressJob = null
        }
    }

    override fun onCleared() {
        controller?.removeListener(listener)
        connection.release()
    }
}
