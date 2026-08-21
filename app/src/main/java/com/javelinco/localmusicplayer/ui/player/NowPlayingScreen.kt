package com.javelinco.localmusicplayer.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.javelinco.localmusicplayer.playback.service.PlaybackUiState

@Composable
fun NowPlayingScreen(
    state: PlaybackUiState,
    reducedMotion: Boolean,
    favorite: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onFavorite: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onQueue: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PlayingIndicator(state.isPlaying, reducedMotion)
        Text(state.title.ifBlank { "Nothing queued" })
        Text(state.artist)
        Slider(
            value = state.positionMs.toFloat().coerceAtMost(state.durationMs.toFloat().coerceAtLeast(1f)),
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..state.durationMs.toFloat().coerceAtLeast(1f),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious) { Text("Previous") }
            Button(onClick = onPlayPause) { Text(if (state.isPlaying) "Pause" else "Play") }
            IconButton(onClick = onNext) { Text("Next") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onFavorite) { Text(if (favorite) "Favorited" else "Favorite") }
            Button(onClick = onQueue) { Text("Queue") }
        }
        Button(onClick = onShuffle) { Text(if (state.shuffleEnabled) "Shuffle On" else "Shuffle Off") }
        Button(onClick = onRepeat) {
            Text(
                when (state.repeatMode) {
                    Player.REPEAT_MODE_ALL -> "Repeat All"
                    Player.REPEAT_MODE_ONE -> "Repeat One"
                    else -> "Repeat Off"
                },
            )
        }
    }
}
