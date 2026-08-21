package com.javelinco.localmusicplayer.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.javelinco.localmusicplayer.playback.service.PlaybackUiState

@Composable
fun MiniPlayer(
    state: PlaybackUiState,
    reducedMotion: Boolean,
    onOpen: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
) {
    if (!state.hasSession) return
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PlayingIndicator(state.isPlaying, reducedMotion)
            Column(Modifier.weight(1f)) {
                Text(state.title.ifBlank { "Unknown track" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(state.artist.ifBlank { "Unknown artist" }, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onPrevious) { Text("|◀") }
            IconButton(onClick = onPlayPause) { Text(if (state.isPlaying) "Ⅱ" else "▶") }
            IconButton(onClick = onNext) { Text("▶|") }
        }
    }
}
