package com.javelinco.localmusicplayer.playlists

import com.javelinco.localmusicplayer.core.model.PlaylistEntryId
import com.javelinco.localmusicplayer.core.model.PlaylistId
import com.javelinco.localmusicplayer.core.model.TrackId
import com.javelinco.localmusicplayer.data.db.FavoriteEntity
import com.javelinco.localmusicplayer.data.db.LibraryDao
import com.javelinco.localmusicplayer.data.db.PlaylistEntity
import com.javelinco.localmusicplayer.data.db.PlaylistEntryEntity
import com.javelinco.localmusicplayer.data.db.UserDataDao
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RoomPlaylistRepository(
    private val dao: UserDataDao,
    private val clock: () -> Long = System::currentTimeMillis,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
    private val libraryDao: LibraryDao? = null,
) : PlaylistRepository {
    override fun observePlaylists(): Flow<List<PlaylistSummary>> =
        combine(dao.observePlaylists(), dao.observeAllPlaylistEntries()) { playlists, entries ->
            val counts = entries.groupingBy(PlaylistEntryEntity::playlistId).eachCount()
            playlists.map { playlist ->
                PlaylistSummary(PlaylistId(playlist.playlistId), playlist.name, counts[playlist.playlistId] ?: 0)
            }
        }

    override fun observeEntries(id: PlaylistId): Flow<List<PlaylistEntry>> =
        dao.observePlaylistEntries(id.value).map { entries -> entries.map(PlaylistEntryEntity::toModel) }

    override fun observeFavorites(): Flow<Set<TrackId>> =
        dao.observeFavorites().map { rows -> rows.mapTo(linkedSetOf()) { TrackId(it.trackId) } }

    override suspend fun create(name: String): PlaylistId {
        val id = PlaylistId(idFactory())
        val now = clock()
        dao.upsertPlaylist(PlaylistEntity(id.value, name.cleanedName(), now, now))
        return id
    }

    override suspend fun rename(id: PlaylistId, name: String) =
        dao.renamePlaylist(id.value, name.cleanedName(), clock())

    override suspend fun delete(id: PlaylistId) = dao.deletePlaylist(id.value)

    override suspend fun addTracks(id: PlaylistId, tracks: List<TrackId>) {
        val start = dao.playlistEntries(id.value).size
        dao.upsertPlaylistEntries(
            tracks.mapIndexed { offset, track ->
                val catalogTrack = libraryDao?.track(track.value)
                PlaylistEntryEntity(
                    entryId = idFactory(),
                    playlistId = id.value,
                    position = start + offset,
                    trackId = track.value,
                    titleSnapshot = catalogTrack?.title ?: catalogTrack?.fileName ?: track.value,
                    contentUriSnapshot = catalogTrack?.contentUri.orEmpty(),
                    addedAtEpochMs = clock(),
                )
            },
        )
    }

    override suspend fun moveEntry(id: PlaylistId, from: Int, to: Int) {
        val entries = dao.playlistEntries(id.value).toMutableList()
        require(from in entries.indices && to in entries.indices)
        entries.add(to, entries.removeAt(from))
        dao.reorderPlaylistEntries(id.value, entries.map(PlaylistEntryEntity::entryId))
    }

    override suspend fun removeEntry(id: PlaylistId, entryId: PlaylistEntryId) =
        dao.deletePlaylistEntry(entryId.value)

    override suspend fun setFavorite(track: TrackId, favorite: Boolean) {
        if (favorite) {
            val catalogTrack = libraryDao?.track(track.value)
            dao.upsertFavorite(
                FavoriteEntity(
                    track.value,
                    catalogTrack?.title ?: catalogTrack?.fileName ?: track.value,
                    catalogTrack?.contentUri.orEmpty(),
                    clock(),
                ),
            )
        } else {
            dao.deleteFavorite(track.value)
        }
    }
}

private fun PlaylistEntryEntity.toModel() =
    PlaylistEntry(PlaylistEntryId(entryId), PlaylistId(playlistId), TrackId(trackId))

private fun String.cleanedName() = trim().ifEmpty { "Untitled playlist" }
