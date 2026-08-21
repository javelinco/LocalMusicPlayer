package com.javelinco.localmusicplayer.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.javelinco.localmusicplayer.data.db.AlbumSummary
import com.javelinco.localmusicplayer.data.db.NamedGroupSummary
import com.javelinco.localmusicplayer.data.db.PlaylistEntryEntity
import com.javelinco.localmusicplayer.data.db.TrackEntity
import com.javelinco.localmusicplayer.data.scan.ScanPhase
import com.javelinco.localmusicplayer.data.scan.ScanProgress
import com.javelinco.localmusicplayer.data.source.MusicSource
import com.javelinco.localmusicplayer.library.LibrarySearchResult
import com.javelinco.localmusicplayer.playlists.PlaylistSummary

data class LibraryScreenState(
    val selectedView: LibraryView = LibraryView.TRACKS,
    val tracks: List<TrackEntity> = emptyList(),
    val artists: List<NamedGroupSummary> = emptyList(),
    val albums: List<AlbumSummary> = emptyList(),
    val genres: List<NamedGroupSummary> = emptyList(),
    val playlists: List<PlaylistSummary> = emptyList(),
    val playlistEntries: List<PlaylistEntryEntity> = emptyList(),
    val sources: List<MusicSource> = emptyList(),
    val scanProgress: ScanProgress? = null,
    val scanMessage: String? = null,
    val searchOpen: Boolean = false,
    val searchQuery: String = "",
    val searchResult: LibrarySearchResult? = null,
)

@Suppress("LongParameterList")
data class LibraryActions(
    val onSelectView: (LibraryView) -> Unit = {},
    val onOpenSearch: () -> Unit = {},
    val onCloseSearch: () -> Unit = {},
    val onSearch: (String) -> Unit = {},
    val onPlayTrack: (TrackEntity) -> Unit = {},
    val onPlayPlaylist: (String) -> Unit = {},
    val onChooseFolder: () -> Unit = {},
    val onChooseFiles: () -> Unit = {},
    val onFindAll: () -> Unit = {},
    val onBackgroundScan: () -> Unit = {},
    val onDedicatedScan: () -> Unit = {},
    val onPrioritizeScan: () -> Unit = {},
    val onDismissScanMessage: () -> Unit = {},
    val onCreatePlaylist: (String) -> Unit = {},
    val onRenamePlaylist: (String, String) -> Unit = { _, _ -> },
    val onDeletePlaylist: (String) -> Unit = {},
    val onAddTracksToPlaylist: (String, List<String>) -> Unit = { _, _ -> },
    val onRemovePlaylistEntry: (String, String) -> Unit = { _, _ -> },
    val onMovePlaylistEntry: (String, Int, Int) -> Unit = { _, _, _ -> },
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun LibraryScreen(state: LibraryScreenState, actions: LibraryActions) {
    var menuExpanded by remember { mutableStateOf(false) }
    var toolsOpen by remember { mutableStateOf(false) }
    var openedGroup by remember(state.selectedView) { mutableStateOf<OpenedMetadataGroup?>(null) }
    var pendingAddition by remember { mutableStateOf<PendingPlaylistAddition?>(null) }

    fun requestTrackAddition(track: TrackEntity) {
        pendingAddition = PendingPlaylistAddition(track.title ?: track.fileName, listOf(track.trackId))
    }

    fun requestGroupAddition(view: LibraryView, group: NamedGroupSummary) {
        val trackIds = tracksForMetadataGroup(view, group.normalizedName, state.tracks)
            .map(TrackEntity::trackId)
        pendingAddition = PendingPlaylistAddition(group.displayName, trackIds)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Library", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Row {
                IconButton(onClick = if (state.searchOpen) actions.onCloseSearch else actions.onOpenSearch) {
                    Icon(
                        if (state.searchOpen) Icons.Rounded.Close else Icons.Rounded.Search,
                        if (state.searchOpen) "Close search" else "Search ${state.selectedView.label}",
                    )
                }
                IconButton(onClick = { toolsOpen = !toolsOpen }) {
                    Icon(Icons.Rounded.FolderOpen, "Library tools")
                }
            }
        }
        ExposedDropdownMenuBox(expanded = menuExpanded, onExpandedChange = { menuExpanded = it }) {
            Button(
                onClick = { menuExpanded = true },
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            ) {
                Text(state.selectedView.label)
                Icon(Icons.Rounded.ArrowDropDown, null)
            }
            ExposedDropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                LibraryView.entries.forEach { view ->
                    DropdownMenuItem(
                        text = { Text(view.label) },
                        onClick = {
                            menuExpanded = false
                            actions.onSelectView(view)
                        },
                    )
                }
            }
        }
        if (state.searchOpen) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = actions.onSearch,
                label = { Text("Search ${state.selectedView.label.lowercase()}") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
        state.scanProgress?.takeIf { it.phase != ScanPhase.COMPLETE }?.let { progress ->
            Card(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScanStatus(progress)
                    Button(onClick = actions.onPrioritizeScan) { Text("Prioritize scan") }
                }
            }
        }
        state.scanMessage?.let { message ->
            Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        message,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = actions.onDismissScanMessage) {
                        Icon(Icons.Rounded.Close, "Dismiss scan result")
                    }
                }
            }
        }
        if (state.sources.isEmpty() || toolsOpen) {
            SourcesScreen(
                sources = state.sources,
                onChooseFolder = actions.onChooseFolder,
                onChooseFiles = actions.onChooseFiles,
                onFindAll = actions.onFindAll,
                onBackgroundScan = actions.onBackgroundScan,
                onDedicatedScan = actions.onDedicatedScan,
            )
            return@Column
        }

        openedGroup?.let { opened ->
            val matchingTracks = remember(state.tracks, opened) {
                tracksForMetadataGroup(opened.view, opened.group.normalizedName, state.tracks)
            }
            MetadataDetailScreen(
                title = opened.group.displayName,
                parentLabel = opened.view.label,
                tracks = matchingTracks,
                onBack = { openedGroup = null },
                onPlayTrack = actions.onPlayTrack,
                onAddTrack = ::requestTrackAddition,
                onAddAll = { requestGroupAddition(opened.view, opened.group) },
            )
            return@Column
        }

        when (val result = state.searchResult) {
            is LibrarySearchResult.Tracks -> TrackList(result.items, actions.onPlayTrack, ::requestTrackAddition)
            is LibrarySearchResult.NamedGroups -> MetadataListScreen(
                groups = result.items,
                onOpen = { openedGroup = OpenedMetadataGroup(state.selectedView, it) },
                onAddToPlaylist = { requestGroupAddition(state.selectedView, it) },
            )
            is LibrarySearchResult.Albums -> AlbumList(result.items)
            is LibrarySearchResult.Playlists -> PlaylistScreen(
                result.items,
                state.playlistEntries,
                state.tracks,
                actions.onPlayPlaylist,
                actions.onCreatePlaylist,
                actions.onRenamePlaylist,
                actions.onDeletePlaylist,
                actions.onAddTracksToPlaylist,
                actions.onRemovePlaylistEntry,
                actions.onMovePlaylistEntry,
            )
            null -> LibraryBrowseContent(
                state = state,
                actions = actions,
                onOpenGroup = { openedGroup = OpenedMetadataGroup(state.selectedView, it) },
                onAddTrack = ::requestTrackAddition,
                onAddGroup = { requestGroupAddition(state.selectedView, it) },
            )
        }
    }

    pendingAddition?.let { request ->
        PlaylistPickerDialog(
            request = request,
            playlists = state.playlists,
            onChoose = { playlistId ->
                actions.onAddTracksToPlaylist(playlistId, request.trackIds)
                pendingAddition = null
            },
            onGoToPlaylists = {
                pendingAddition = null
                openedGroup = null
                actions.onSelectView(LibraryView.PLAYLISTS)
            },
            onDismiss = { pendingAddition = null },
        )
    }
}

@Composable
private fun LibraryBrowseContent(
    state: LibraryScreenState,
    actions: LibraryActions,
    onOpenGroup: (NamedGroupSummary) -> Unit,
    onAddTrack: (TrackEntity) -> Unit,
    onAddGroup: (NamedGroupSummary) -> Unit,
) {
    when (state.selectedView) {
        LibraryView.TRACKS -> TrackList(state.tracks, actions.onPlayTrack, onAddTrack)
        LibraryView.ARTISTS -> MetadataListScreen(state.artists, onOpenGroup, onAddGroup)
        LibraryView.ALBUMS -> AlbumList(state.albums)
        LibraryView.GENRES -> MetadataListScreen(state.genres, onOpenGroup, onAddGroup)
        LibraryView.PLAYLISTS -> PlaylistScreen(
            state.playlists,
            state.playlistEntries,
            state.tracks,
            actions.onPlayPlaylist,
            actions.onCreatePlaylist,
            actions.onRenamePlaylist,
            actions.onDeletePlaylist,
            actions.onAddTracksToPlaylist,
            actions.onRemovePlaylistEntry,
            actions.onMovePlaylistEntry,
        )
    }
}

@Composable
fun TrackList(
    tracks: List<TrackEntity>,
    onPlay: (TrackEntity) -> Unit,
    onAddToPlaylist: ((TrackEntity) -> Unit)? = null,
) {
    if (tracks.isEmpty()) {
        Text("No scanned MP3s yet.", modifier = Modifier.padding(top = 18.dp))
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(tracks, key = TrackEntity::trackId) { track ->
            Card(
                onClick = { onPlay(track) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("track-card:${track.trackId}"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = track.title ?: track.fileName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = listOfNotNull(track.artist, track.albumTitle)
                                .joinToString(" — ")
                                .ifBlank { track.fileName },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    onAddToPlaylist?.let { onAdd ->
                        IconButton(onClick = { onAdd(track) }) {
                            Icon(
                                Icons.AutoMirrored.Rounded.PlaylistAdd,
                                "Add ${track.title ?: track.fileName} to playlist",
                            )
                        }
                    }
                }
            }
        }
    }
}

internal data class OpenedMetadataGroup(
    val view: LibraryView,
    val group: NamedGroupSummary,
)

internal fun tracksForMetadataGroup(
    view: LibraryView,
    normalizedName: String,
    tracks: List<TrackEntity>,
): List<TrackEntity> = when (view) {
    LibraryView.ARTISTS -> tracks.filter { it.normalizedArtist == normalizedName }
    LibraryView.GENRES -> tracks.filter { it.normalizedGenre == normalizedName }
    else -> emptyList()
}

@Composable
private fun AlbumList(albums: List<AlbumSummary>) {
    LazyColumn {
        items(albums, key = { "${it.normalizedAlbumArtist}:${it.normalizedAlbumTitle}" }) { album ->
            ListItem(
                headlineContent = { Text(album.displayTitle) },
                supportingContent = { Text("${album.displayArtist} · ${album.trackCount} tracks") },
            )
        }
    }
}
