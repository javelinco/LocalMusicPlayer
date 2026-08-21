package com.javelinco.localmusicplayer.library

import com.javelinco.localmusicplayer.data.db.AlbumSummary
import com.javelinco.localmusicplayer.data.db.LibraryDao
import com.javelinco.localmusicplayer.data.db.NamedGroupSummary
import com.javelinco.localmusicplayer.data.db.TrackEntity
import com.javelinco.localmusicplayer.playlists.PlaylistSummary
import com.javelinco.localmusicplayer.ui.library.LibraryView
import java.util.Locale

sealed interface LibrarySearchResult {
    data class Tracks(val items: List<TrackEntity>) : LibrarySearchResult
    data class NamedGroups(val items: List<NamedGroupSummary>) : LibrarySearchResult
    data class Albums(val items: List<AlbumSummary>) : LibrarySearchResult
    data class Playlists(val items: List<PlaylistSummary>) : LibrarySearchResult
}

class LibrarySearchEngine(
    private val libraryDao: LibraryDao,
    private val trackSearch: LibraryRepository = LibraryRepository(libraryDao),
) {
    suspend fun search(
        view: LibraryView,
        query: String,
        playlists: List<PlaylistSummary>,
    ): LibrarySearchResult {
        val normalizedQuery = query.trim().lowercase(Locale.ROOT)
        return when (view) {
            LibraryView.TRACKS -> LibrarySearchResult.Tracks(trackSearch.search(query))
            LibraryView.ARTISTS -> LibrarySearchResult.NamedGroups(
                libraryDao.searchArtistGroups(normalizedQuery),
            )
            LibraryView.ALBUMS -> LibrarySearchResult.Albums(
                libraryDao.searchAlbumGroups(normalizedQuery),
            )
            LibraryView.GENRES -> LibrarySearchResult.NamedGroups(
                libraryDao.searchGenreGroups(normalizedQuery),
            )
            LibraryView.PLAYLISTS -> LibrarySearchResult.Playlists(
                playlists.filter { it.name.contains(query.trim(), ignoreCase = true) }.take(200),
            )
        }
    }
}
