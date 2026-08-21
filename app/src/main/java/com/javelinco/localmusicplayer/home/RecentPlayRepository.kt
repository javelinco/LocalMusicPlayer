package com.javelinco.localmusicplayer.home

import com.javelinco.localmusicplayer.data.db.RecentPlayDao
import com.javelinco.localmusicplayer.data.db.RecentPlayEntity
import com.javelinco.localmusicplayer.data.db.RecentPlaylistRow
import com.javelinco.localmusicplayer.data.db.TrackEntity
import kotlinx.coroutines.flow.Flow

enum class RecentPlayKind { TRACK, PLAYLIST }

class RecentPlayRepository(
    private val dao: RecentPlayDao,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun observeRecentTracks(limit: Int = DISPLAY_LIMIT): Flow<List<TrackEntity>> =
        dao.observeRecentTracks(limit)

    fun observeRecentPlaylists(limit: Int = DISPLAY_LIMIT): Flow<List<RecentPlaylistRow>> =
        dao.observeRecentPlaylists(limit)

    suspend fun recordTrack(trackId: String, playedAtEpochMs: Long = clock()) {
        record(RecentPlayKind.TRACK, trackId, playedAtEpochMs)
    }

    suspend fun recordPlaylist(playlistId: String, playedAtEpochMs: Long = clock()) {
        record(RecentPlayKind.PLAYLIST, playlistId, playedAtEpochMs)
    }

    suspend fun removeTrack(trackId: String) {
        dao.remove(RecentPlayKind.TRACK.name, trackId)
    }

    suspend fun removePlaylist(playlistId: String) {
        dao.remove(RecentPlayKind.PLAYLIST.name, playlistId)
    }

    private suspend fun record(kind: RecentPlayKind, itemId: String, playedAtEpochMs: Long) {
        dao.record(RecentPlayEntity(kind.name, itemId, playedAtEpochMs), HISTORY_LIMIT)
    }

    private companion object {
        const val DISPLAY_LIMIT = 5
        const val HISTORY_LIMIT = 20
    }
}
