package com.javelinco.localmusicplayer.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

@Composable
fun BackupScreen(
    selectedFolder: String?,
    backupNames: List<String>,
    status: String?,
    onChooseFolder: () -> Unit,
    onManualBackup: () -> Unit,
    onRefresh: () -> Unit,
    onRestore: (String) -> Unit,
) {
    var pendingRestore by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Backups contain playlists, favorites, settings, queue metadata, source descriptions, and portable track references—never audio, artwork, or the search index.")
        Text("Automatic backups run at most daily and keep seven. Manual and safety backups are never rotated automatically.")
        Text(if (selectedFolder == null) "No USB-visible backup folder selected" else "Backup folder selected")
        Button(onClick = onChooseFolder) { Text("Choose backup folder") }
        Button(onClick = onManualBackup, enabled = selectedFolder != null) { Text("Create manual backup") }
        Button(onClick = onRefresh, enabled = selectedFolder != null) { Text("Refresh backup list") }
        status?.let { Text(it) }
        LazyColumn {
            items(backupNames, key = { it }) { name ->
                ListItem(
                    headlineContent = { Text(name) },
                    trailingContent = { Button(onClick = { pendingRestore = name }) { Text("Restore") } },
                )
            }
        }
    }
    pendingRestore?.let { name ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Restore this backup?") },
            text = { Text("Current user data will be replaced only after the backup validates and a safety backup succeeds. Music files and the scan index are not changed.") },
            confirmButton = {
                TextButton(onClick = { pendingRestore = null; onRestore(name) }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Cancel") } },
        )
    }
}
