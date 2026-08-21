package com.javelinco.localmusicplayer.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.javelinco.localmusicplayer.playlists.PlaylistSummary

internal data class PendingPlaylistAddition(
    val label: String,
    val trackIds: List<String>,
)

@Composable
internal fun PlaylistPickerDialog(
    request: PendingPlaylistAddition,
    playlists: List<PlaylistSummary>,
    onChoose: (String) -> Unit,
    onGoToPlaylists: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ${request.label} to playlist") },
        text = {
            if (playlists.isEmpty()) {
                Column {
                    Text("Create a playlist first.")
                    TextButton(onClick = onGoToPlaylists) { Text("Go to playlists") }
                }
            } else {
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(playlists, key = { it.id.value }) { playlist ->
                        ListItem(
                            headlineContent = { Text(playlist.name) },
                            supportingContent = { Text("${playlist.trackCount} tracks") },
                            modifier = Modifier.clickable { onChoose(playlist.id.value) },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
