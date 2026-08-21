package com.javelinco.localmusicplayer.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.javelinco.localmusicplayer.data.db.TrackEntity
import com.javelinco.localmusicplayer.data.scan.ScanProgress
import com.javelinco.localmusicplayer.data.settings.SettingsState
import com.javelinco.localmusicplayer.data.settings.ThemePreference
import com.javelinco.localmusicplayer.data.source.MusicSource
import com.javelinco.localmusicplayer.library.SearchFilter
import com.javelinco.localmusicplayer.playback.service.PlaybackUiState
import com.javelinco.localmusicplayer.playlists.PlaylistSummary
import com.javelinco.localmusicplayer.ui.library.BackupScreen
import com.javelinco.localmusicplayer.ui.library.DedicatedScanScreen
import com.javelinco.localmusicplayer.ui.library.LibraryScreen
import com.javelinco.localmusicplayer.ui.library.PlaylistScreen
import com.javelinco.localmusicplayer.ui.library.SearchScreen
import com.javelinco.localmusicplayer.ui.library.SourcesScreen
import com.javelinco.localmusicplayer.ui.player.MiniPlayer
import com.javelinco.localmusicplayer.ui.player.NowPlayingScreen
import com.javelinco.localmusicplayer.ui.player.QueueScreen

private enum class Destination(val label: String) {
    LIBRARY("Library"), SEARCH("Search"), PLAYLISTS("Playlists"), SOURCES("Sources"), MORE("More"),
    NOW_PLAYING("Now Playing"), QUEUE("Queue"), BACKUP("Backup"), SETTINGS("Settings"),
}

@Composable
@Suppress("LongParameterList")
fun AppNavigation(
    tracks: List<TrackEntity>,
    searchResults: List<TrackEntity>,
    sources: List<MusicSource>,
    playlists: List<PlaylistSummary>,
    favoriteIds: Set<String>,
    scanProgress: ScanProgress?,
    dedicated: Boolean,
    settings: SettingsState,
    playback: PlaybackUiState,
    backupNames: List<String>,
    status: String?,
    onPlay: (TrackEntity) -> Unit,
    onFavorite: (TrackEntity, Boolean) -> Unit,
    onSearch: (String, SearchFilter) -> Unit,
    onChooseFolder: () -> Unit,
    onChooseFiles: () -> Unit,
    onFindAll: () -> Unit,
    onBackgroundScan: () -> Unit,
    onDedicatedScan: () -> Unit,
    onLeaveDedicated: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onChooseBackupFolder: () -> Unit,
    onManualBackup: () -> Unit,
    onRefreshBackups: () -> Unit,
    onRestore: (String) -> Unit,
    onTheme: (ThemePreference) -> Unit,
    onReducedMotion: (Boolean) -> Unit,
) {
    if (dedicated) {
        DedicatedScanScreen(scanProgress, onLeaveDedicated)
        return
    }
    var destination by remember { mutableStateOf(Destination.LIBRARY) }
    val primary = Destination.entries.take(5)
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Column {
                MiniPlayer(playback, settings.reducedMotion, { destination = Destination.NOW_PLAYING }, onPrevious, onPlayPause, onNext)
                NavigationBar {
                    primary.forEach { item ->
                        NavigationBarItem(
                            selected = destination == item,
                            onClick = { destination = item },
                            icon = { Text(item.label.take(1)) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            Text(destination.label)
            HorizontalDivider()
            when (destination) {
                Destination.LIBRARY -> LibraryScreen(tracks, favoriteIds, onPlay, onFavorite)
                Destination.SEARCH -> SearchScreen(searchResults, favoriteIds, onSearch, onPlay, onFavorite)
                Destination.PLAYLISTS -> PlaylistScreen(playlists, onCreatePlaylist)
                Destination.SOURCES -> SourcesScreen(sources, scanProgress, onChooseFolder, onChooseFiles, onFindAll, onBackgroundScan, onDedicatedScan)
                Destination.MORE -> Column {
                    Button(onClick = { destination = Destination.NOW_PLAYING }) { Text("Now Playing") }
                    Button(onClick = { destination = Destination.QUEUE }) { Text("Queue") }
                    Button(onClick = { destination = Destination.BACKUP }) { Text("Backup and restore") }
                    Button(onClick = { destination = Destination.SETTINGS }) { Text("Appearance") }
                    Text("Offline only · MP3 · no telemetry · no internet permission")
                }
                Destination.NOW_PLAYING -> NowPlayingScreen(
                    playback,
                    settings.reducedMotion,
                    playback.currentMediaId in favoriteIds,
                    onPrevious,
                    onPlayPause,
                    onNext,
                    onSeek,
                    { playback.currentMediaId?.let { id -> tracks.find { it.trackId == id }?.let { onFavorite(it, id !in favoriteIds) } } },
                    onShuffle,
                    onRepeat,
                    { destination = Destination.QUEUE },
                )
                Destination.QUEUE -> QueueScreen(tracks, playback.currentMediaId)
                Destination.BACKUP -> BackupScreen(settings.backupTreeUri, backupNames, status, onChooseBackupFolder, onManualBackup, onRefreshBackups, onRestore)
                Destination.SETTINGS -> Column {
                    ThemePreference.entries.forEach { theme ->
                        Button(onClick = { onTheme(theme) }) { Text("Theme: ${theme.name.lowercase()}") }
                    }
                    Button(onClick = { onReducedMotion(!settings.reducedMotion) }) {
                        Text(if (settings.reducedMotion) "Reduced motion: on" else "Reduced motion: off")
                    }
                }
            }
        }
    }
}
