package com.javelinco.localmusicplayer.playlists

import com.javelinco.localmusicplayer.core.model.PlaylistEntryId
import com.javelinco.localmusicplayer.core.model.PlaylistId
import com.javelinco.localmusicplayer.core.model.TrackId
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class PlaylistSummary(val id: PlaylistId, val name: String, val trackCount: Int)

data class PlaylistEntry(
    val id: PlaylistEntryId,
    val playlistId: PlaylistId,
    val trackId: TrackId,
)

interface PlaylistRepository {
    fun observePlaylists(): Flow<List<PlaylistSummary>>
    fun observeEntries(id: PlaylistId): Flow<List<PlaylistEntry>>
    fun observeFavorites(): Flow<Set<TrackId>>
    suspend fun create(name: String): PlaylistId
    suspend fun rename(id: PlaylistId, name: String)
    suspend fun delete(id: PlaylistId)
    suspend fun addTracks(id: PlaylistId, tracks: List<TrackId>)
    suspend fun moveEntry(id: PlaylistId, from: Int, to: Int)
    suspend fun removeEntry(id: PlaylistId, entryId: PlaylistEntryId)
    suspend fun setFavorite(track: TrackId, favorite: Boolean)
}

class InMemoryPlaylistRepository(
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) : PlaylistRepository {
    private val mutex = Mutex()
    private val names = MutableStateFlow<Map<PlaylistId, String>>(emptyMap())
    private val entries = MutableStateFlow<Map<PlaylistId, List<PlaylistEntry>>>(emptyMap())
    private val favorites = MutableStateFlow<Set<TrackId>>(emptySet())

    override fun observePlaylists(): Flow<List<PlaylistSummary>> = combine(names, entries) { currentNames, currentEntries ->
        currentNames.map { (id, name) -> PlaylistSummary(id, name, currentEntries[id].orEmpty().size) }
            .sortedBy { it.name.lowercase() }
    }

    override fun observeEntries(id: PlaylistId): Flow<List<PlaylistEntry>> =
        entries.map { it[id].orEmpty() }

    override fun observeFavorites(): Flow<Set<TrackId>> = favorites

    override suspend fun create(name: String): PlaylistId = mutex.withLock {
        val id = PlaylistId(idFactory())
        names.value = names.value + (id to name.trim().ifEmpty { "Untitled playlist" })
        id
    }

    override suspend fun rename(id: PlaylistId, name: String) = mutex.withLock {
        require(id in names.value)
        names.value = names.value + (id to name.trim().ifEmpty { "Untitled playlist" })
    }

    override suspend fun delete(id: PlaylistId) = mutex.withLock {
        names.value = names.value - id
        entries.value = entries.value - id
    }

    override suspend fun addTracks(id: PlaylistId, tracks: List<TrackId>) = mutex.withLock {
        require(id in names.value)
        val additions = tracks.map { track -> PlaylistEntry(PlaylistEntryId(idFactory()), id, track) }
        entries.value = entries.value + (id to (entries.value[id].orEmpty() + additions))
    }

    override suspend fun moveEntry(id: PlaylistId, from: Int, to: Int) = mutex.withLock {
        val reordered = entries.value[id].orEmpty().toMutableList()
        require(from in reordered.indices && to in reordered.indices)
        reordered.add(to, reordered.removeAt(from))
        entries.value = entries.value + (id to reordered)
    }

    override suspend fun removeEntry(id: PlaylistId, entryId: PlaylistEntryId) = mutex.withLock {
        entries.value = entries.value + (id to entries.value[id].orEmpty().filterNot { it.id == entryId })
    }

    override suspend fun setFavorite(track: TrackId, favorite: Boolean) = mutex.withLock {
        favorites.value = if (favorite) favorites.value + track else favorites.value - track
    }
}
