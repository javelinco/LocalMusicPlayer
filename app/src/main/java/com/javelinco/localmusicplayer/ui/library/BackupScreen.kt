package com.javelinco.localmusicplayer.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
    val guidance = backupScreenGuidance(selectedFolder, backupNames.size)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") {
            Text(
                "Protect your playlists and app setup. Your MP3 files are never copied into a backup.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item(key = "location") {
            BackupSectionCard("1. Choose a backup location") {
                if (guidance.folderPath == null) {
                    Text(
                        "Choose a folder on your phone that you can easily find from a computer over USB.",
                    )
                    Button(onClick = onChooseFolder) { Text(guidance.folderButtonLabel) }
                } else {
                    Text("Current backup location", fontWeight = FontWeight.SemiBold)
                    Text(
                        guidance.folderPath,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "Automatic, manual, and safety backups are stored here.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(onClick = onChooseFolder) { Text(guidance.folderButtonLabel) }
                }
            }
        }
        item(key = "create") {
            BackupSectionCard("2. Create a backup") {
                Text(
                    "A backup saves playlists, settings, queue state, music-folder descriptions, " +
                        "ignored-track rules, and portable track references. It does not copy audio.",
                )
                Text(
                    "Music, Please! makes at most one automatic backup each day and keeps the newest seven. " +
                        "Manual and safety backups are kept until you remove them yourself.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onManualBackup, enabled = selectedFolder != null) {
                    Text("Create backup now")
                }
                status?.let { message ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            message,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }
        item(key = "restore-header") {
            BackupSectionCard("3. Restore from a backup") {
                Text(
                    "Choose a backup below to restore app data. Music, Please! validates it and creates " +
                        "a safety backup before replacing anything.",
                )
                OutlinedButton(onClick = onRefresh, enabled = selectedFolder != null) {
                    Text("Refresh list")
                }
            }
        }
        guidance.emptyRestoreMessage?.let { message ->
            item(key = "restore-empty") {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        items(backupNames, key = { it }) { name ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                ListItem(
                    headlineContent = { Text(name) },
                    supportingContent = { Text("Music, Please! backup") },
                    trailingContent = {
                        Button(onClick = { pendingRestore = name }) { Text("Restore") }
                    },
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

@Composable
private fun BackupSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}
