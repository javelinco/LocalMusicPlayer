package com.javelinco.localmusicplayer.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.javelinco.localmusicplayer.data.db.RecentPlaylistRow
import com.javelinco.localmusicplayer.data.db.TrackEntity
import com.javelinco.localmusicplayer.home.RecentPlaybackQueue
import com.javelinco.localmusicplayer.home.recentPlaybackQueue
import com.javelinco.localmusicplayer.ui.library.TrackActionCallbacks
import com.javelinco.localmusicplayer.ui.library.TrackActionMenu

@Composable
fun HomeScreen(
    recentTracks: List<TrackEntity>,
    recentPlaylists: List<RecentPlaylistRow>,
    trackActions: TrackActionCallbacks,
    onPlayRecentQueue: (RecentPlaybackQueue) -> Unit,
    onPlayPlaylist: (String) -> Unit,
    onRemoveRecentTrack: (String) -> Unit,
    onRemoveRecentPlaylist: (String) -> Unit,
) {
    fun playRecent(track: TrackEntity) {
        recentPlaybackQueue(track.trackId, recentTracks)?.let(onPlayRecentQueue)
    }
    val recentTrackActions = trackActions.copy(onPlayNow = ::playRecent)
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("Recently played", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        LazyColumn {
            if (recentTracks.isNotEmpty()) {
                item { Text("Songs", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 18.dp)) }
                items(recentTracks, key = { "track:${it.trackId}" }) { track ->
                    ListItem(
                        leadingContent = { Icon(Icons.Rounded.History, null) },
                        headlineContent = { Text(track.title ?: track.fileName) },
                        supportingContent = { Text(track.artist ?: "Unknown artist") },
                        trailingContent = {
                            TrackActionMenu(
                                track,
                                recentTrackActions,
                                onRemoveFromRecentlyPlayed = { onRemoveRecentTrack(it.trackId) },
                            )
                        },
                        modifier = Modifier.clickable { playRecent(track) },
                    )
                }
            }
            if (recentPlaylists.isNotEmpty()) {
                item { Text("Playlists", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 18.dp)) }
                items(recentPlaylists, key = { "playlist:${it.playlistId}" }) { playlist ->
                    ListItem(
                        leadingContent = { Icon(Icons.Rounded.History, null) },
                        headlineContent = { Text(playlist.name) },
                        supportingContent = { Text("${playlist.trackCount} tracks") },
                        trailingContent = {
                            RecentPlaylistActionMenu(playlist, onRemoveRecentPlaylist)
                        },
                        modifier = Modifier.clickable { onPlayPlaylist(playlist.playlistId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentPlaylistActionMenu(
    playlist: RecentPlaylistRow,
    onRemoveRecentPlaylist: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Rounded.MoreVert, "More actions for ${playlist.name}")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("Remove from recently played") },
            onClick = {
                expanded = false
                onRemoveRecentPlaylist(playlist.playlistId)
            },
        )
    }
}
