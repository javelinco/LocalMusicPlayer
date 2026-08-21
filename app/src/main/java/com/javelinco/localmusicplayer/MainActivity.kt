package com.javelinco.localmusicplayer

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.javelinco.localmusicplayer.core.model.SourceId
import com.javelinco.localmusicplayer.data.source.AndroidSafPermissionStore
import com.javelinco.localmusicplayer.data.source.MediaStoreSource
import com.javelinco.localmusicplayer.data.source.SelectedDocument
import com.javelinco.localmusicplayer.data.source.SourceAcquisitionCoordinator
import com.javelinco.localmusicplayer.data.source.SourcePickerContracts
import com.javelinco.localmusicplayer.data.source.SourceSelectionHandler
import com.javelinco.localmusicplayer.library.LibraryViewModel
import com.javelinco.localmusicplayer.playback.service.PlaybackViewModel
import com.javelinco.localmusicplayer.ui.navigation.AppNavigation
import com.javelinco.localmusicplayer.ui.theme.LocalMusicPlayerTheme
import java.util.UUID
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val acquisition = SourceAcquisitionCoordinator()
    private var showDevicePermissionExplanation by mutableStateOf(false)
    private val app: LocalMusicPlayerApp get() = application as LocalMusicPlayerApp
    private val libraryViewModel: LibraryViewModel by viewModels { LibraryViewModel.Factory(app.container) }
    private val playbackViewModel: PlaybackViewModel by viewModels()

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
            libraryViewModel.startBackgroundScan()
        }
    }

    private val filePicker = registerForActivityResult(SourcePickerContracts.chooseFiles) { uris ->
        lifecycleScope.launch {
            selectionHandler.registerDocuments(uris.map { SelectedDocument(it.toString(), displayName(it)) })
            libraryViewModel.startBackgroundScan()
        }
    }

    private val backupFolderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri ?: return@registerForActivityResult
        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        libraryViewModel.selectBackupFolder(uri.toString())
    }

    private val devicePermission = registerForActivityResult(SourcePickerContracts.requestPermission) { granted ->
        if (granted) {
            lifecycleScope.launch {
                app.sourceRegistry.add(MediaStoreSource(SourceId("media-store"), "All music on this device"))
                libraryViewModel.startBackgroundScan()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        libraryViewModel.runDailyBackupIfConfigured()
        setContent {
            val tracks by libraryViewModel.tracks.collectAsState()
            val searchResults by libraryViewModel.searchResults.collectAsState()
            val sources by libraryViewModel.sources.collectAsState()
            val playlists by libraryViewModel.playlists.collectAsState()
            val favorites by libraryViewModel.favorites.collectAsState()
            val settings by libraryViewModel.settings.collectAsState()
            val scanProgress by libraryViewModel.scanProgress.collectAsState()
            val dedicated by libraryViewModel.dedicated.collectAsState()
            val backups by libraryViewModel.backupNames.collectAsState()
            val status by libraryViewModel.status.collectAsState()
            val playback by playbackViewModel.state.collectAsState()
            LocalMusicPlayerTheme(settings.theme) {
                AppNavigation(
                    tracks = tracks,
                    searchResults = searchResults,
                    sources = sources,
                    playlists = playlists,
                    favoriteIds = favorites.mapTo(linkedSetOf()) { it.value },
                    scanProgress = scanProgress,
                    dedicated = dedicated,
                    settings = settings,
                    playback = playback,
                    backupNames = backups,
                    status = status,
                    onPlay = { playbackViewModel.play(it, tracks) },
                    onFavorite = { track, favorite -> libraryViewModel.setFavorite(track.trackId, favorite) },
                    onSearch = libraryViewModel::search,
                    onChooseFolder = { folderPicker.launch(null) },
                    onChooseFiles = { filePicker.launch(arrayOf(SourcePickerContracts.MP3_MIME_TYPE)) },
                    onFindAll = { showDevicePermissionExplanation = true },
                    onBackgroundScan = libraryViewModel::startBackgroundScan,
                    onDedicatedScan = { libraryViewModel.enterDedicatedScan(playbackViewModel::stopForDedicatedScan) },
                    onLeaveDedicated = libraryViewModel::leaveDedicatedScan,
                    onPrevious = playbackViewModel::previous,
                    onPlayPause = playbackViewModel::togglePlayPause,
                    onNext = playbackViewModel::next,
                    onSeek = playbackViewModel::seekTo,
                    onShuffle = playbackViewModel::toggleShuffle,
                    onRepeat = playbackViewModel::cycleRepeat,
                    onCreatePlaylist = libraryViewModel::createPlaylist,
                    onChooseBackupFolder = { backupFolderPicker.launch(null) },
                    onManualBackup = libraryViewModel::createManualBackup,
                    onRefreshBackups = libraryViewModel::refreshBackups,
                    onRestore = libraryViewModel::restoreBackup,
                    onTheme = libraryViewModel::setTheme,
                    onReducedMotion = libraryViewModel::setReducedMotion,
                )
                if (showDevicePermissionExplanation) DevicePermissionDialog()
            }
        }
    }

    @Composable
    private fun DevicePermissionDialog() {
        AlertDialog(
            onDismissRequest = { showDevicePermissionExplanation = false },
            title = { Text("Allow device-wide music access?") },
            text = { Text("Android will grant audio-only access. Selected folders and files continue to work without it. LocalMusicPlayer has no internet or all-files permission.") },
            confirmButton = {
                TextButton(onClick = {
                    showDevicePermissionExplanation = false
                    acquisition.confirmDeviceMusicExplanation()
                    devicePermission.launch(Manifest.permission.READ_MEDIA_AUDIO)
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showDevicePermissionExplanation = false }) { Text("Cancel") }
            },
        )
    }

    private fun displayName(uri: Uri): String =
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: uri.lastPathSegment ?: "Selected MP3"
}
