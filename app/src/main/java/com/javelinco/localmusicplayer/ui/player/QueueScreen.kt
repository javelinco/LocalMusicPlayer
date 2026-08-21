package com.javelinco.localmusicplayer.ui.player

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.javelinco.localmusicplayer.data.db.TrackEntity
import com.javelinco.localmusicplayer.ui.library.TrackActionCallbacks
import com.javelinco.localmusicplayer.ui.library.TrackActionMenu

@Composable
fun QueueScreen(
    queueTracks: List<TrackEntity>,
    currentMediaId: String?,
    trackActions: TrackActionCallbacks,
) {
    LazyColumn {
        itemsIndexed(queueTracks, key = { index, it -> "$index:${it.trackId}" }) { index, track ->
            ListItem(
                headlineContent = { Text(track.title ?: track.fileName) },
                supportingContent = { Text("${index + 1}. ${track.artist ?: "Unknown artist"}") },
                trailingContent = {
                    androidx.compose.foundation.layout.Row {
                        if (track.trackId == currentMediaId) Text("Playing")
                        TrackActionMenu(track, trackActions)
                    }
                },
            )
        }
    }
}
