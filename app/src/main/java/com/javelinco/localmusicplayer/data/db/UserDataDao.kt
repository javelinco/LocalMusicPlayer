package com.javelinco.localmusicplayer.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDataDao {
    @Upsert
    suspend fun upsertPlaylist(playlist: PlaylistEntity)

    @Upsert
    suspend fun upsertPlaylistEntries(entries: List<PlaylistEntryEntity>)

    @Upsert
    suspend fun upsertFavorite(favorite: FavoriteEntity)

    @Upsert
    suspend fun saveQueueSession(queueSession: QueueSessionEntity)

    @Upsert
    suspend fun upsertSetting(setting: SettingsMetadataEntity)

    @Query("SELECT * FROM playlists ORDER BY playlistId")
    suspend fun playlists(): List<PlaylistEntity>

    @Query("SELECT * FROM playlists ORDER BY name COLLATE NOCASE")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlist_entries WHERE playlistId = :playlistId ORDER BY position")
    suspend fun playlistEntries(playlistId: String): List<PlaylistEntryEntity>

    @Query("SELECT * FROM playlist_entries WHERE playlistId = :playlistId ORDER BY position")
    fun observePlaylistEntries(playlistId: String): Flow<List<PlaylistEntryEntity>>

    @Query("SELECT * FROM playlist_entries ORDER BY playlistId, position")
    suspend fun allPlaylistEntries(): List<PlaylistEntryEntity>

    @Query("SELECT * FROM playlist_entries ORDER BY playlistId, position")
    fun observeAllPlaylistEntries(): Flow<List<PlaylistEntryEntity>>

    @Query("SELECT * FROM favorites ORDER BY trackId")
    suspend fun favorites(): List<FavoriteEntity>

    @Query("SELECT * FROM favorites ORDER BY trackId")
    fun observeFavorites(): Flow<List<FavoriteEntity>>

    @Query("DELETE FROM playlists WHERE playlistId = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Query("UPDATE playlists SET name = :name, modifiedAtEpochMs = :modifiedAt WHERE playlistId = :playlistId")
    suspend fun renamePlaylist(playlistId: String, name: String, modifiedAt: Long)

    @Query("DELETE FROM playlist_entries WHERE entryId = :entryId")
    suspend fun deletePlaylistEntry(entryId: String)

    @Query("UPDATE playlist_entries SET position = :position WHERE entryId = :entryId")
    suspend fun updateEntryPosition(entryId: String, position: Int)

    @Transaction
    suspend fun reorderPlaylistEntries(playlistId: String, orderedEntryIds: List<String>) {
        val existing = playlistEntries(playlistId).map(PlaylistEntryEntity::entryId).toSet()
        require(existing == orderedEntryIds.toSet() && existing.size == orderedEntryIds.size)
        orderedEntryIds.forEachIndexed { position, entryId -> updateEntryPosition(entryId, position) }
    }

    @Query("DELETE FROM favorites WHERE trackId = :trackId")
    suspend fun deleteFavorite(trackId: String)

    @Query("SELECT * FROM queue_session ORDER BY singletonId")
    suspend fun queueSessions(): List<QueueSessionEntity>

    @Query("SELECT * FROM queue_session WHERE singletonId = 1")
    suspend fun queueSession(): QueueSessionEntity?

    @Query("DELETE FROM queue_session")
    suspend fun clearQueueSession()

    @Query("SELECT * FROM settings_metadata ORDER BY key")
    suspend fun settings(): List<SettingsMetadataEntity>

    @Query("DELETE FROM playlist_entries")
    suspend fun clearPlaylistEntries()

    @Query("DELETE FROM playlists")
    suspend fun clearPlaylists()

    @Query("DELETE FROM favorites")
    suspend fun clearFavorites()

    @Query("DELETE FROM settings_metadata")
    suspend fun clearSettings()

    @Transaction
    suspend fun replaceUserData(snapshot: UserDataSnapshot) {
        clearPlaylistEntries()
        clearPlaylists()
        clearFavorites()
        clearQueueSession()
        clearSettings()
        snapshot.playlists.forEach { upsertPlaylist(it) }
        if (snapshot.entries.isNotEmpty()) upsertPlaylistEntries(snapshot.entries)
        snapshot.favorites.forEach { upsertFavorite(it) }
        snapshot.queueSessions.forEach { saveQueueSession(it) }
        snapshot.settings.forEach { upsertSetting(it) }
    }

    @Transaction
    suspend fun snapshot() = UserDataSnapshot(
        playlists = playlists(),
        entries = allPlaylistEntries(),
        favorites = favorites(),
        queueSessions = queueSessions(),
        settings = settings(),
    )
}
