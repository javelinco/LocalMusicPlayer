package com.javelinco.localmusicplayer.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

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

    @Query("SELECT * FROM playlist_entries WHERE playlistId = :playlistId ORDER BY position")
    suspend fun playlistEntries(playlistId: String): List<PlaylistEntryEntity>

    @Query("SELECT * FROM playlist_entries ORDER BY playlistId, position")
    suspend fun allPlaylistEntries(): List<PlaylistEntryEntity>

    @Query("SELECT * FROM favorites ORDER BY trackId")
    suspend fun favorites(): List<FavoriteEntity>

    @Query("SELECT * FROM queue_session ORDER BY singletonId")
    suspend fun queueSessions(): List<QueueSessionEntity>

    @Query("SELECT * FROM settings_metadata ORDER BY key")
    suspend fun settings(): List<SettingsMetadataEntity>

    @Transaction
    suspend fun snapshot() = UserDataSnapshot(
        playlists = playlists(),
        entries = allPlaylistEntries(),
        favorites = favorites(),
        queueSessions = queueSessions(),
        settings = settings(),
    )
}
