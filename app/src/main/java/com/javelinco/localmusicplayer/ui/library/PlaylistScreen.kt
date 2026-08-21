package com.javelinco.localmusicplayer.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.javelinco.localmusicplayer.playlists.PlaylistSummary

@Composable
fun PlaylistScreen(playlists: List<PlaylistSummary>, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    Column {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Playlist name") })
        Button(onClick = { onCreate(name); name = "" }) { Text("Create playlist") }
        LazyColumn {
            items(playlists, key = { it.id.value }) { playlist ->
                ListItem(
                    headlineContent = { Text(playlist.name) },
                    supportingContent = { Text("${playlist.trackCount} tracks") },
                )
            }
        }
    }
}
