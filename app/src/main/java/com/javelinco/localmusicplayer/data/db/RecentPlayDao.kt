package com.javelinco.localmusicplayer.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

data class RecentPlaylistRow(
    val playlistId: String,
    val name: String,
    val trackCount: Int,
)

@Dao
interface RecentPlayDao {
    @Upsert
    suspend fun upsert(row: RecentPlayEntity)

    @Query("DELETE FROM recent_plays WHERE kind = :kind AND itemId = :itemId")
    suspend fun remove(kind: String, itemId: String)

    @Query(
        """
        DELETE FROM recent_plays
        WHERE kind = :kind AND itemId NOT IN (
            SELECT itemId FROM recent_plays
            WHERE kind = :kind
            ORDER BY playedAtEpochMs DESC, itemId
            LIMIT :keep
        )
        """,
    )
    suspend fun trim(kind: String, keep: Int)

    @Transaction
    suspend fun record(row: RecentPlayEntity, keep: Int = 20) {
        upsert(row)
        trim(row.kind, keep)
    }

    @Query(
        """
        SELECT tracks.* FROM recent_plays
        INNER JOIN tracks ON tracks.trackId = recent_plays.itemId
        WHERE recent_plays.kind = 'TRACK' AND tracks.available = 1
        ORDER BY recent_plays.playedAtEpochMs DESC, recent_plays.itemId
        LIMIT :limit
        """,
    )
    fun observeRecentTracks(limit: Int): Flow<List<TrackEntity>>

    @Query(
        """
        SELECT playlists.playlistId AS playlistId,
            playlists.name AS name,
            COUNT(playlist_entries.entryId) AS trackCount
        FROM recent_plays
        INNER JOIN playlists ON playlists.playlistId = recent_plays.itemId
        LEFT JOIN playlist_entries ON playlist_entries.playlistId = playlists.playlistId
        WHERE recent_plays.kind = 'PLAYLIST'
        GROUP BY playlists.playlistId, playlists.name, recent_plays.playedAtEpochMs
        ORDER BY recent_plays.playedAtEpochMs DESC, recent_plays.itemId
        LIMIT :limit
        """,
    )
    fun observeRecentPlaylists(limit: Int): Flow<List<RecentPlaylistRow>>
}
