package com.javelinco.localmusicplayer.ui.library

import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.javelinco.localmusicplayer.data.scan.ScanProgress

@Composable
fun ScanStatus(progress: ScanProgress?) {
    progress ?: return
    if (progress.phase.name != "COMPLETE") LinearProgressIndicator()
    Text("Scanning: ${progress.phase.name.lowercase()} · found ${progress.found} · indexed ${progress.processed} · skipped ${progress.skipped} · errors ${progress.errors}")
}
