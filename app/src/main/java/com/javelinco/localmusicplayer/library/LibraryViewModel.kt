package com.javelinco.localmusicplayer.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.javelinco.localmusicplayer.AppContainer
import com.javelinco.localmusicplayer.core.model.TrackId
import com.javelinco.localmusicplayer.core.model.PlaylistEntryId
import com.javelinco.localmusicplayer.core.model.PlaylistId
import com.javelinco.localmusicplayer.data.db.PlaylistEntryEntity
import com.javelinco.localmusicplayer.data.db.TrackEntity
import com.javelinco.localmusicplayer.data.scan.ScanProgress
import com.javelinco.localmusicplayer.data.settings.SettingsState
import com.javelinco.localmusicplayer.data.settings.ThemePreference
import com.javelinco.localmusicplayer.data.source.MusicSource
import com.javelinco.localmusicplayer.playlists.PlaylistSummary
import com.javelinco.localmusicplayer.ui.library.LibraryView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(private val container: AppContainer) : ViewModel() {
    private val librarySearchEngine = LibrarySearchEngine(container.database.libraryDao())
    val tracks = container.database.libraryDao().observeAvailableTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val sources: StateFlow<List<MusicSource>> = container.sourceRegistry.observeSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val playlists: StateFlow<List<PlaylistSummary>> = container.playlistRepository.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val playlistEntries: StateFlow<List<PlaylistEntryEntity>> =
        container.database.userDataDao().observeAllPlaylistEntries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val favorites: StateFlow<Set<TrackId>> = container.playlistRepository.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
    val settings: StateFlow<SettingsState> = container.settings.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsState())
    val scanProgress: StateFlow<ScanProgress?> = container.scanCoordinator.progress

    private val mutableSearchResults = MutableStateFlow<List<TrackEntity>>(emptyList())
    val searchResults = mutableSearchResults.asStateFlow()
    private val mutableLibrarySearchResult = MutableStateFlow<LibrarySearchResult?>(null)
    val librarySearchResult = mutableLibrarySearchResult.asStateFlow()
    private val mutableLibraryView = MutableStateFlow(LibraryView.TRACKS)
    val libraryView = mutableLibraryView.asStateFlow()
    private val mutableSearchOpen = MutableStateFlow(false)
    val searchOpen = mutableSearchOpen.asStateFlow()
    private val mutableStatus = MutableStateFlow<String?>(null)
    val status = mutableStatus.asStateFlow()
    private val scanSession = ScanSessionManager(container.scanCoordinator, viewModelScope)
    val dedicated = scanSession.dedicated
    val scanMessage = scanSession.message
    private val mutableBackupNames = MutableStateFlow<List<String>>(emptyList())
    val backupNames = mutableBackupNames.asStateFlow()
    private var searchJob: Job? = null

    fun search(query: String, filter: SearchFilter = SearchFilter.ALL) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(180)
            mutableSearchResults.value = container.libraryRepository.search(query, filter)
        }
    }

    fun selectLibraryView(view: LibraryView) {
        mutableLibraryView.value = view
        mutableLibrarySearchResult.value = null
        viewModelScope.launch { container.settings.setLibraryView(view) }
    }

    fun openLibrarySearch() {
        mutableSearchOpen.value = true
    }

    fun closeLibrarySearch() {
        searchJob?.cancel()
        mutableSearchOpen.value = false
        mutableLibrarySearchResult.value = null
    }

    fun searchLibrary(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            mutableLibrarySearchResult.value = null
            return
        }
        val selectedView = mutableLibraryView.value
        searchJob = viewModelScope.launch {
            delay(180)
            mutableLibrarySearchResult.value = librarySearchEngine.search(
                view = selectedView,
                query = query,
                playlists = playlists.value,
            )
        }
    }

    fun startBackgroundScan() {
        scanSession.startBackground()
    }

    fun enterDedicatedScan(stopPlayback: () -> Unit) {
        scanSession.startDedicated(stopPlayback)
    }

    fun leaveDedicatedScan() {
        scanSession.leaveDedicated()
    }

    fun onSourceAdded(wasFirstSource: Boolean, stopPlayback: () -> Unit) {
        scanSession.sourceAdded(wasFirstSource, stopPlayback)
    }

    fun prioritizeScan(stopPlayback: () -> Unit) {
        scanSession.prioritize(stopPlayback)
    }

    fun setFavorite(trackId: String, favorite: Boolean) {
        viewModelScope.launch { container.playlistRepository.setFavorite(TrackId(trackId), favorite) }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch { container.playlistRepository.create(name) }
    }

    fun renamePlaylist(id: String, name: String) {
        viewModelScope.launch { container.playlistRepository.rename(PlaylistId(id), name) }
    }

    fun deletePlaylist(id: String) {
        viewModelScope.launch { container.playlistRepository.delete(PlaylistId(id)) }
    }

    fun addTrackToPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch {
            container.playlistRepository.addTracks(PlaylistId(playlistId), listOf(TrackId(trackId)))
        }
    }

    fun removePlaylistEntry(playlistId: String, entryId: String) {
        viewModelScope.launch {
            container.playlistRepository.removeEntry(PlaylistId(playlistId), PlaylistEntryId(entryId))
        }
    }

    fun movePlaylistEntry(playlistId: String, from: Int, to: Int) {
        viewModelScope.launch { container.playlistRepository.moveEntry(PlaylistId(playlistId), from, to) }
    }

    fun setTheme(theme: ThemePreference) {
        viewModelScope.launch { container.settings.setTheme(theme) }
    }

    fun setReducedMotion(enabled: Boolean) {
        viewModelScope.launch { container.settings.setReducedMotion(enabled) }
    }

    fun selectBackupFolder(uri: String) {
        viewModelScope.launch {
            container.settings.setBackupTreeUri(uri)
            mutableStatus.value = "Backup folder selected"
            val manager = container.backupManager(uri)
            manager.createAutomaticIfDue()
            mutableBackupNames.value = manager.listBackups()
        }
    }

    fun refreshBackups() {
        viewModelScope.launch {
            settings.value.backupTreeUri?.let { uri ->
                mutableBackupNames.value = container.backupManager(uri).listBackups()
            }
        }
    }

    fun createManualBackup() {
        viewModelScope.launch {
            val uri = settings.value.backupTreeUri
            if (uri == null) {
                mutableStatus.value = "Choose a backup folder first"
                return@launch
            }
            val manager = container.backupManager(uri)
            val name = manager.createManual()
            mutableBackupNames.value = manager.listBackups()
            mutableStatus.value = "Created $name"
        }
    }

    fun restoreBackup(name: String) {
        viewModelScope.launch {
            val uri = settings.value.backupTreeUri ?: return@launch
            runCatching { container.backupManager(uri).restore(name) }
                .onSuccess { mutableStatus.value = "Restore complete; unavailable tracks remain visible in playlists" }
                .onFailure { mutableStatus.value = "Restore failed: ${it.message}" }
        }
    }

    fun runDailyBackupIfConfigured() {
        viewModelScope.launch {
            settings.first { it.backupTreeUri != null }.backupTreeUri?.let { uri ->
                runCatching { container.backupManager(uri).createAutomaticIfDue() }
            }
        }
    }

    init {
        viewModelScope.launch {
            settings.collect { mutableLibraryView.value = it.libraryView }
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = LibraryViewModel(container) as T
    }
}
