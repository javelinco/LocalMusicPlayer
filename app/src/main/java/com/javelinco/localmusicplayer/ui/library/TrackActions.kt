package com.javelinco.localmusicplayer.ui.library

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.javelinco.localmusicplayer.data.db.TrackEntity

data class TrackActionCallbacks(
    val onPlayNow: (TrackEntity) -> Unit,
    val onPlayNext: (TrackEntity) -> Unit,
    val onAddToQueue: (TrackEntity) -> Unit,
    val onAddToPlaylist: (TrackEntity) -> Unit,
    val onGoToArtist: (TrackEntity) -> Unit,
    val onShowInformation: (TrackEntity) -> Unit,
    val onRemoveFromLibrary: (TrackEntity) -> Unit,
)

@Composable
fun TrackActionMenu(track: TrackEntity, actions: TrackActionCallbacks) {
    var expanded by remember { mutableStateOf(false) }
    var confirmRemoval by remember { mutableStateOf(false) }
    val title = track.title ?: track.fileName

    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Rounded.MoreVert, "More actions for $title")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(text = { Text("Play now") }, onClick = { expanded = false; actions.onPlayNow(track) })
        DropdownMenuItem(text = { Text("Play next") }, onClick = { expanded = false; actions.onPlayNext(track) })
        DropdownMenuItem(text = { Text("Add to queue") }, onClick = { expanded = false; actions.onAddToQueue(track) })
        DropdownMenuItem(text = { Text("Add to playlist") }, onClick = { expanded = false; actions.onAddToPlaylist(track) })
        DropdownMenuItem(text = { Text("Go to artist") }, onClick = { expanded = false; actions.onGoToArtist(track) })
        DropdownMenuItem(text = { Text("Track information") }, onClick = { expanded = false; actions.onShowInformation(track) })
        DropdownMenuItem(text = { Text("Remove from library") }, onClick = { expanded = false; confirmRemoval = true })
    }
    if (confirmRemoval) {
        AlertDialog(
            onDismissRequest = { confirmRemoval = false },
            title = { Text("Remove $title from library?") },
            text = {
                Text(
                    "The MP3 file will not be deleted or modified. The track will stay hidden " +
                        "during future scans until you restore it from Ignored tracks.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmRemoval = false
                    actions.onRemoveFromLibrary(track)
                }) { Text("Remove from library") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemoval = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
fun TrackInformationDialog(track: TrackEntity, sourceDescription: String, onDismiss: () -> Unit) {
    fun value(text: String?): String = text?.takeIf(String::isNotBlank) ?: "Unknown"
    val minutes = track.durationMs / 60_000
    val seconds = (track.durationMs / 1_000) % 60
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(track.title ?: track.fileName) },
        text = {
            Text(
                listOf(
                    "Title: ${value(track.title)}",
                    "Artist: ${value(track.artist)}",
                    "Album: ${value(track.albumTitle)}",
                    "Genre: ${value(track.genre)}",
                    "Duration: $minutes:${seconds.toString().padStart(2, '0')}",
                    "Filename: ${track.fileName}",
                    "Source: $sourceDescription",
                ).joinToString("\n"),
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
