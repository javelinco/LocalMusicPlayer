package com.javelinco.localmusicplayer.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.javelinco.localmusicplayer.data.scan.ScanProgress
import com.javelinco.localmusicplayer.data.source.MusicSource

@Composable
fun SourcesScreen(
    sources: List<MusicSource>,
    progress: ScanProgress?,
    onChooseFolder: () -> Unit,
    onChooseFiles: () -> Unit,
    onFindAll: () -> Unit,
    onBackgroundScan: () -> Unit,
    onDedicatedScan: () -> Unit,
) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("You decide exactly where the app may look. Folder and file choices need no broad audio permission.")
        Button(onClick = onChooseFolder) { Text("Choose a folder") }
        Button(onClick = onChooseFiles) { Text("Choose specific MP3 files") }
        Button(onClick = onFindAll) { Text("Find all music on this device") }
        Text("The last option asks Android for audio-only access after an explanation. It is optional.")
        Button(onClick = onBackgroundScan, enabled = sources.isNotEmpty()) { Text("Scan quietly") }
        Button(onClick = onDedicatedScan, enabled = sources.isNotEmpty()) { Text("Enter dedicated scanning mode") }
        ScanStatus(progress)
        LazyColumn {
            items(sources, key = { it.id.value }) { source ->
                ListItem(headlineContent = { Text(source.label) }, supportingContent = { Text(source.kind.name) })
            }
        }
    }
}
