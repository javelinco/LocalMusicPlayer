package com.javelinco.localmusicplayer.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.javelinco.localmusicplayer.data.db.RecentPlaylistRow
import com.javelinco.localmusicplayer.data.db.TrackEntity
import com.javelinco.localmusicplayer.data.settings.SettingsState
import com.javelinco.localmusicplayer.data.settings.ThemePreference
import com.javelinco.localmusicplayer.playback.service.PlaybackUiState
import com.javelinco.localmusicplayer.ui.home.HomeScreen
import com.javelinco.localmusicplayer.ui.library.BackupScreen
import com.javelinco.localmusicplayer.ui.library.DedicatedScanScreen
import com.javelinco.localmusicplayer.ui.library.LibraryActions
import com.javelinco.localmusicplayer.ui.library.LibraryScreen
import com.javelinco.localmusicplayer.ui.library.LibraryScreenState
import com.javelinco.localmusicplayer.ui.library.LibraryView
import com.javelinco.localmusicplayer.ui.library.PendingPlaylistAddition
import com.javelinco.localmusicplayer.ui.library.PlaylistPickerDialog
import com.javelinco.localmusicplayer.ui.library.TrackActionCallbacks
import com.javelinco.localmusicplayer.ui.library.TrackInformationDialog
import com.javelinco.localmusicplayer.ui.player.MiniPlayer
import com.javelinco.localmusicplayer.ui.player.NowPlayingScreen
import com.javelinco.localmusicplayer.ui.player.QueueScreen

enum class PrimaryDestination(val label: String) { HOME("Home"), LIBRARY("Library"), MORE("More") }

internal fun chooseInitialPrimaryDestination(
    recentLoaded: Boolean,
    playbackReady: Boolean,
    hasRecent: Boolean,
    isPlaying: Boolean,
): PrimaryDestination? = when {
    !recentLoaded || !playbackReady -> null
    hasRecent || isPlaying -> PrimaryDestination.HOME
    else -> PrimaryDestination.LIBRARY
}

private enum class Destination { HOME, LIBRARY, MORE, NOW_PLAYING, QUEUE, BACKUP, SETTINGS }

@Composable
fun PrimaryNavigationBar(selected: PrimaryDestination, onSelect: (PrimaryDestination) -> Unit) {
    NavigationBar {
        PrimaryDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = destination == selected,
                onClick = { onSelect(destination) },
                icon = {
                    Icon(
                        when (destination) {
                            PrimaryDestination.HOME -> Icons.Rounded.Home
                            PrimaryDestination.LIBRARY -> Icons.Rounded.LibraryMusic
                            PrimaryDestination.MORE -> Icons.Rounded.MoreHoriz
                        },
                        destination.label,
                    )
                },
                label = { Text(destination.label) },
            )
        }
    }
}

@Composable
@Suppress("LongParameterList")
fun AppNavigation(
    libraryState: LibraryScreenState,
    libraryActions: LibraryActions,
    recentTracks: List<TrackEntity>,
    recentPlaylists: List<RecentPlaylistRow>,
    recentLoaded: Boolean,
    dedicated: Boolean,
    settings: SettingsState,
    playback: PlaybackUiState,
    backupNames: List<String>,
    status: String?,
    onLeaveDedicated: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onChooseBackupFolder: () -> Unit,
    onManualBackup: () -> Unit,
    onRefreshBackups: () -> Unit,
    onRestore: (String) -> Unit,
    onTheme: (ThemePreference) -> Unit,
    onReducedMotion: (Boolean) -> Unit,
) {
    var destination by rememberSaveable { mutableStateOf<Destination?>(null) }
    var pendingPlaylistTrack by remember { mutableStateOf<TrackEntity?>(null) }
    var pendingInformationTrack by remember { mutableStateOf<TrackEntity?>(null) }
    var requestedArtist by remember { mutableStateOf<String?>(null) }
    if (dedicated) {
        DedicatedScanScreen(libraryState.scanProgress, onLeaveDedicated)
        return
    }
    LaunchedEffect(recentLoaded, playback.controllerReady) {
        if (destination == null) {
            destination = when (chooseInitialPrimaryDestination(
                recentLoaded = recentLoaded,
                playbackReady = playback.controllerReady,
                hasRecent = recentTracks.isNotEmpty() || recentPlaylists.isNotEmpty(),
                isPlaying = playback.isPlaying,
            )) {
                PrimaryDestination.HOME -> Destination.HOME
                PrimaryDestination.LIBRARY -> Destination.LIBRARY
                PrimaryDestination.MORE -> Destination.MORE
                null -> null
            }
        }
    }
    val current = destination ?: Destination.LIBRARY
    val trackActions = TrackActionCallbacks(
        onPlayNow = libraryActions.onPlayTrack,
        onPlayNext = libraryActions.onPlayNext,
        onAddToQueue = libraryActions.onAddToQueue,
        onAddToPlaylist = { pendingPlaylistTrack = it },
        onGoToArtist = { track ->
            requestedArtist = track.normalizedArtist
            libraryActions.onSelectView(LibraryView.ARTISTS)
            destination = Destination.LIBRARY
        },
        onShowInformation = { pendingInformationTrack = it },
        onRemoveFromLibrary = libraryActions.onRemoveTrackFromLibrary,
    )
    val primary = when (current) {
        Destination.HOME, Destination.NOW_PLAYING -> PrimaryDestination.HOME
        Destination.LIBRARY -> PrimaryDestination.LIBRARY
        else -> PrimaryDestination.MORE
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Column {
                MiniPlayer(
                    playback,
                    onOpen = { destination = Destination.NOW_PLAYING },
                    onPrevious = onPrevious,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                )
                PrimaryNavigationBar(primary) { selected ->
                    destination = when (selected) {
                        PrimaryDestination.HOME -> Destination.HOME
                        PrimaryDestination.LIBRARY -> Destination.LIBRARY
                        PrimaryDestination.MORE -> Destination.MORE
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            when (current) {
                Destination.HOME -> if (playback.isPlaying) {
                    NowPlayingScreen(
                        playback,
                        settings.reducedMotion,
                        onPrevious,
                        onPlayPause,
                        onNext,
                        onSeek,
                        onShuffle,
                        onRepeat,
                        { destination = Destination.QUEUE },
                    )
                } else {
                    HomeScreen(
                        recentTracks,
                        recentPlaylists,
                        trackActions,
                        libraryActions.onPlayPlaylist,
                    )
                }
                Destination.LIBRARY -> LibraryScreen(
                    libraryState.copy(requestedArtist = requestedArtist),
                    libraryActions.copy(onArtistRequestConsumed = { requestedArtist = null }),
                )
                Destination.MORE -> MoreScreen(
                    onBackup = { destination = Destination.BACKUP },
                    onSettings = { destination = Destination.SETTINGS },
                )
                Destination.NOW_PLAYING -> NowPlayingScreen(
                    playback,
                    settings.reducedMotion,
                    onPrevious,
                    onPlayPause,
                    onNext,
                    onSeek,
                    onShuffle,
                    onRepeat,
                    { destination = Destination.QUEUE },
                )
                Destination.QUEUE -> QueueScreen(
                    playback.queueTracks,
                    playback.currentMediaId,
                    trackActions,
                )
                Destination.BACKUP -> BackupScreen(
                    settings.backupTreeUri,
                    backupNames,
                    status,
                    onChooseBackupFolder,
                    onManualBackup,
                    onRefreshBackups,
                    onRestore,
                )
                Destination.SETTINGS -> SettingsScreen(settings, onTheme, onReducedMotion)
            }
        }
    }
    pendingPlaylistTrack?.let { track ->
        PlaylistPickerDialog(
            request = PendingPlaylistAddition(track.title ?: track.fileName, listOf(track.trackId)),
            playlists = libraryState.playlists,
            onChoose = { playlistId ->
                libraryActions.onAddTracksToPlaylist(playlistId, listOf(track.trackId))
                pendingPlaylistTrack = null
            },
            onGoToPlaylists = {
                pendingPlaylistTrack = null
                libraryActions.onSelectView(LibraryView.PLAYLISTS)
                destination = Destination.LIBRARY
            },
            onDismiss = { pendingPlaylistTrack = null },
        )
    }
    pendingInformationTrack?.let { track ->
        val source = libraryState.sources.find { it.id.value == track.sourceId }
        TrackInformationDialog(
            track,
            listOfNotNull(source?.label, source?.identity).joinToString(" — ").ifBlank { "Unknown" },
            onDismiss = { pendingInformationTrack = null },
        )
    }
}

@Composable
private fun MoreScreen(
    onBackup: () -> Unit,
    onSettings: () -> Unit,
) {
    Column {
        Text("More", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        ListItem(
            leadingContent = { Icon(Icons.Rounded.Backup, null) },
            headlineContent = { Text("Backup and restore") },
            modifier = Modifier.clickable(onClick = onBackup),
        )
        ListItem(
            leadingContent = { Icon(Icons.Rounded.Palette, null) },
            headlineContent = { Text("Appearance") },
            modifier = Modifier.clickable(onClick = onSettings),
        )
        Text("Offline only · MP3 · no telemetry · no internet permission")
    }
}

@Composable
private fun SettingsScreen(
    settings: SettingsState,
    onTheme: (ThemePreference) -> Unit,
    onReducedMotion: (Boolean) -> Unit,
) {
    Column {
        Text("Appearance", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        ThemePreference.entries.forEach { theme ->
            ListItem(
                headlineContent = { Text(theme.name.lowercase().replaceFirstChar(Char::uppercase)) },
                supportingContent = { if (settings.theme == theme) Text("Selected") },
                modifier = Modifier.clickable { onTheme(theme) },
            )
        }
        ListItem(
            headlineContent = { Text("Reduced motion") },
            supportingContent = { Text(if (settings.reducedMotion) "On" else "Off") },
            modifier = Modifier.clickable { onReducedMotion(!settings.reducedMotion) },
        )
    }
}
