package com.javelinco.localmusicplayer.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.javelinco.localmusicplayer.data.source.MusicSource
import com.javelinco.localmusicplayer.data.db.IgnoredTrackEntity

@Composable
fun SourcesScreen(
    sources: List<MusicSource>,
    onChooseFolder: () -> Unit,
    onFindAll: () -> Unit,
    onBackgroundScan: () -> Unit,
    onDedicatedScan: () -> Unit,
    ignoredTracks: List<IgnoredTrackEntity> = emptyList(),
    onRestoreIgnoredTrack: (String) -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (sources.isEmpty()) {
            Text("Where is your music?", style = MaterialTheme.typography.headlineSmall)
            Text("Choose only the folders Music, Please! may see. No broad audio permission is needed.")
        } else {
            Text("Music sources", style = MaterialTheme.typography.titleLarge)
        }
        Button(onClick = onChooseFolder, modifier = Modifier.fillMaxWidth()) {
            Text(if (sources.isEmpty()) "Choose a music folder" else "Add another folder")
        }
        Text(
            "Repeat this for every folder you want in your Library.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onFindAll, modifier = Modifier.fillMaxWidth()) {
            Text("Find all device music")
        }
        Text(
            "Whole-device discovery asks Android for optional audio-only access after an explanation.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (sources.isNotEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Scan library", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onBackgroundScan) { Text("Scan quietly") }
                        OutlinedButton(onClick = onDedicatedScan) { Text("Dedicated scan") }
                    }
                }
            }
            LazyColumn {
                items(sources, key = { it.id.value }) { source ->
                    ListItem(
                        headlineContent = { Text(source.label) },
                        supportingContent = { Text(source.kind.name.replace('_', ' ').lowercase()) },
                    )
                }
            }
            Text("Ignored tracks (${ignoredTracks.size})", style = MaterialTheme.typography.titleMedium)
            if (ignoredTracks.isEmpty()) {
                Text("No ignored tracks.")
            } else {
                ignoredTracks.forEach { ignored ->
                    ListItem(
                        headlineContent = { Text(ignored.title ?: ignored.fileName) },
                        supportingContent = { Text(ignored.artist ?: "Unknown artist") },
                        trailingContent = {
                            OutlinedButton(onClick = { onRestoreIgnoredTrack(ignored.ignoreId) }) {
                                Text("Restore to library")
                            }
                        },
                    )
                }
            }
        }
    }
}
