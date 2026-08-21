package com.javelinco.localmusicplayer

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.javelinco.localmusicplayer.core.model.SourceId
import com.javelinco.localmusicplayer.data.source.AndroidSafPermissionStore
import com.javelinco.localmusicplayer.data.source.MediaStoreSource
import com.javelinco.localmusicplayer.data.source.SelectedDocument
import com.javelinco.localmusicplayer.data.source.SourceAcquisitionCoordinator
import com.javelinco.localmusicplayer.data.source.SourcePickerContracts
import com.javelinco.localmusicplayer.data.source.SourceSelectionHandler
import java.util.UUID
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val acquisition = SourceAcquisitionCoordinator()
    private var showDevicePermissionExplanation by mutableStateOf(false)

    private val app: LocalMusicPlayerApp
        get() = application as LocalMusicPlayerApp

    private val selectionHandler by lazy {
        SourceSelectionHandler(
            registry = app.sourceRegistry,
            permissionStore = AndroidSafPermissionStore(contentResolver),
            idFactory = { SourceId(UUID.randomUUID().toString()) },
        )
    }

    private val folderPicker = registerForActivityResult(SourcePickerContracts.chooseFolder) { uri ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            selectionHandler.registerFolder(uri.toString(), uri.lastPathSegment ?: "Selected folder")
        }
    }

    private val filePicker = registerForActivityResult(SourcePickerContracts.chooseFiles) { uris ->
        lifecycleScope.launch {
            selectionHandler.registerDocuments(
                uris.map { uri -> SelectedDocument(uri.toString(), displayName(uri)) },
            )
        }
    }

    private val devicePermission = registerForActivityResult(SourcePickerContracts.requestPermission) { granted ->
        if (granted) {
            lifecycleScope.launch {
                app.sourceRegistry.add(
                    MediaStoreSource(SourceId("media-store"), "All music on this device"),
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Text("Choose exactly where LocalMusicPlayer may look for MP3s.")
                        Button(onClick = { folderPicker.launch(null) }) { Text("Choose a folder") }
                        Button(onClick = { filePicker.launch(arrayOf(SourcePickerContracts.MP3_MIME_TYPE)) }) {
                            Text("Choose MP3 files")
                        }
                        Button(onClick = { showDevicePermissionExplanation = true }) {
                            Text("Find all music on this device")
                        }
                    }
                    if (showDevicePermissionExplanation) {
                        AlertDialog(
                            onDismissRequest = { showDevicePermissionExplanation = false },
                            title = { Text("Allow device-wide music access?") },
                            text = {
                                Text("Android will grant access only to audio files. Selected folders and files work without this permission.")
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showDevicePermissionExplanation = false
                                        acquisition.confirmDeviceMusicExplanation()
                                        devicePermission.launch(Manifest.permission.READ_MEDIA_AUDIO)
                                    },
                                ) { Text("Continue") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDevicePermissionExplanation = false }) {
                                    Text("Cancel")
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    private fun displayName(uri: Uri): String =
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: uri.lastPathSegment ?: "Selected MP3"
}
