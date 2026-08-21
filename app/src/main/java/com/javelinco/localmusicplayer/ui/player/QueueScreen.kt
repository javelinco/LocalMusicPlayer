package com.javelinco.localmusicplayer.ui.player

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.javelinco.localmusicplayer.data.db.TrackEntity

@Composable
fun QueueScreen(tracks: List<TrackEntity>, currentMediaId: String?) {
    LazyColumn {
        itemsIndexed(tracks, key = { _, it -> it.trackId }) { index, track ->
            ListItem(
                headlineContent = { Text(track.title ?: track.fileName) },
                supportingContent = { Text("${index + 1}. ${track.artist ?: "Unknown artist"}") },
                trailingContent = { if (track.trackId == currentMediaId) Text("Playing") },
            )
        }
    }
}
