package com.javelinco.localmusicplayer.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.javelinco.localmusicplayer.data.db.TrackEntity

@Composable
fun LibraryScreen(
    tracks: List<TrackEntity>,
    favorites: Set<String>,
    onPlay: (TrackEntity) -> Unit,
    onFavorite: (TrackEntity, Boolean) -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }
    val labels = listOf("Tracks", "Artists", "Albums", "Genres")
    Column {
        ScrollableTabRow(selectedTabIndex = tab) {
            labels.forEachIndexed { index, label ->
                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label) })
            }
        }
        when (tab) {
            0 -> TrackList(tracks, favorites, onPlay, onFavorite)
            1 -> MetadataListScreen(tracks.groupBy { it.artist ?: "Unknown Artist" }.mapValues { it.value.size })
            2 -> MetadataListScreen(tracks.groupBy { it.albumTitle ?: "Unknown Album" }.mapValues { it.value.size })
            else -> MetadataListScreen(tracks.groupBy { it.genre ?: "Unknown Genre" }.mapValues { it.value.size })
        }
    }
}

@Composable
fun TrackList(
    tracks: List<TrackEntity>,
    favorites: Set<String>,
    onPlay: (TrackEntity) -> Unit,
    onFavorite: (TrackEntity, Boolean) -> Unit,
) {
    if (tracks.isEmpty()) {
        Text("No scanned MP3s yet. Add a source, then start a scan.")
        return
    }
    LazyColumn {
        items(tracks, key = TrackEntity::trackId) { track ->
            ListItem(
                headlineContent = { Text(track.title ?: track.fileName) },
                supportingContent = { Text(listOfNotNull(track.artist, track.albumTitle).joinToString(" — ").ifBlank { track.fileName }) },
                trailingContent = {
                    FilterChip(
                        selected = track.trackId in favorites,
                        onClick = { onFavorite(track, track.trackId !in favorites) },
                        label = { Text(if (track.trackId in favorites) "★" else "☆") },
                    )
                },
                modifier = Modifier.clickable { onPlay(track) },
            )
        }
    }
}
