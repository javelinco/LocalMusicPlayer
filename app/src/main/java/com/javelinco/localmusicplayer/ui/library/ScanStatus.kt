package com.javelinco.localmusicplayer.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.javelinco.localmusicplayer.data.scan.ScanPhase
import com.javelinco.localmusicplayer.data.scan.ScanProgress

@Composable
fun ScanStatus(progress: ScanProgress?) {
    progress ?: return
    if (progress.phase.name != "COMPLETE") LinearProgressIndicator()
    Text("Scanning: ${progress.phase.name.lowercase()} · found ${progress.found} · indexed ${progress.processed} · skipped ${progress.skipped} · errors ${progress.errors}")
}

@Composable
fun ScanFeedback(
    progress: ScanProgress?,
    message: String?,
    onPrioritizeScan: () -> Unit,
    onDismissMessage: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        progress?.takeIf { it.phase != ScanPhase.COMPLETE }?.let { activeProgress ->
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ScanStatus(activeProgress)
                    Button(onClick = onPrioritizeScan) { Text("Prioritize scan") }
                }
            }
        }
        message?.let { result ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        result,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismissMessage) {
                        Icon(Icons.Rounded.Close, "Dismiss scan result")
                    }
                }
            }
        }
    }
}
