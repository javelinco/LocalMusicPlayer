package com.javelinco.localmusicplayer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    @Upsert
    suspend fun upsertSources(sources: List<SourceEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSource(source: SourceEntity): Long

    @Query("SELECT * FROM sources ORDER BY label COLLATE NOCASE, sourceId")
    fun observeSources(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM sources ORDER BY label COLLATE NOCASE, sourceId")
    suspend fun sources(): List<SourceEntity>

    @Query("DELETE FROM sources WHERE sourceId = :sourceId")
    suspend fun deleteSource(sourceId: String)

    @Query("UPDATE sources SET available = :available WHERE sourceId = :sourceId")
    suspend fun setSourceAvailability(sourceId: String, available: Boolean)

    @Query("UPDATE sources SET available = :available WHERE kind = :kind")
    suspend fun setSourceKindAvailability(kind: String, available: Boolean)

    @Upsert
    suspend fun upsertTracks(tracks: List<TrackEntity>)

    @Query("DELETE FROM track_search_fts WHERE trackId IN (:trackIds)")
    suspend fun deleteSearchRows(trackIds: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchRows(rows: List<TrackSearchFts>)

    @Upsert
    suspend fun saveCheckpoint(checkpoint: ScanCheckpointEntity)

    @Insert
    suspend fun insertScanErrors(errors: List<ScanErrorEntity>)

    @Transaction
    suspend fun applyScanBatch(batch: ScanBatch) {
        if (batch.tracks.isNotEmpty()) {
            upsertTracks(batch.tracks)
            val trackIds = batch.tracks.map(TrackEntity::trackId)
            deleteSearchRows(trackIds)
            insertSearchRows(batch.tracks.map(TrackSearchFts::from))
        }
        if (batch.errors.isNotEmpty()) insertScanErrors(batch.errors)
        saveCheckpoint(batch.checkpoint)
    }

    @Query(
        """
        SELECT tracks.* FROM tracks
        INNER JOIN track_search_fts ON tracks.trackId = track_search_fts.trackId
        WHERE track_search_fts MATCH :query AND tracks.available = 1
        ORDER BY tracks.normalizedArtist, tracks.normalizedAlbumTitle,
            COALESCE(tracks.discNumber, 1), COALESCE(tracks.trackNumber, 0), tracks.fileName COLLATE NOCASE
        LIMIT :limit
        """,
    )
    suspend fun searchTracks(query: String, limit: Int = 200): List<TrackEntity>

    @Query(
        """
        SELECT * FROM tracks WHERE available = 1
        ORDER BY normalizedArtist, normalizedAlbumTitle,
            COALESCE(discNumber, 1), COALESCE(trackNumber, 0), fileName COLLATE NOCASE
        """,
    )
    fun observeAvailableTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY trackId")
    suspend fun allTracks(): List<TrackEntity>

    @Query(
        """
        SELECT normalizedAlbumArtist, normalizedAlbumTitle,
            CASE WHEN normalizedAlbumArtist = '' THEN 'Unknown Artist'
                ELSE COALESCE(MAX(albumArtist), 'Unknown Artist') END AS displayArtist,
            CASE WHEN normalizedAlbumTitle = '' THEN 'Unknown Album'
                ELSE COALESCE(MAX(albumTitle), 'Unknown Album') END AS displayTitle,
            COUNT(*) AS trackCount
        FROM tracks WHERE available = 1
        GROUP BY normalizedAlbumArtist, normalizedAlbumTitle
        ORDER BY displayArtist COLLATE NOCASE, displayTitle COLLATE NOCASE
        """,
    )
    suspend fun albumGroups(): List<AlbumSummary>

    @Query(
        """
        SELECT * FROM tracks
        WHERE available = 1
            AND normalizedAlbumArtist = :normalizedAlbumArtist
            AND normalizedAlbumTitle = :normalizedAlbumTitle
        ORDER BY COALESCE(discNumber, 1), COALESCE(trackNumber, 0), fileName COLLATE NOCASE
        """,
    )
    suspend fun tracksForAlbum(
        normalizedAlbumArtist: String,
        normalizedAlbumTitle: String,
    ): List<TrackEntity>

    @Query(
        """
        SELECT normalizedArtist AS normalizedName,
            CASE WHEN normalizedArtist = '' THEN 'Unknown Artist'
                ELSE COALESCE(MAX(artist), 'Unknown Artist') END AS displayName,
            COUNT(*) AS trackCount
        FROM tracks WHERE available = 1
        GROUP BY normalizedArtist
        ORDER BY displayName COLLATE NOCASE
        """,
    )
    suspend fun artistGroups(): List<NamedGroupSummary>

    @Query(
        """
        SELECT normalizedGenre AS normalizedName,
            CASE WHEN normalizedGenre = '' THEN 'Unknown Genre'
                ELSE COALESCE(MAX(genre), 'Unknown Genre') END AS displayName,
            COUNT(*) AS trackCount
        FROM tracks WHERE available = 1
        GROUP BY normalizedGenre
        ORDER BY displayName COLLATE NOCASE
        """,
    )
    suspend fun genreGroups(): List<NamedGroupSummary>

    @Query("SELECT * FROM tracks WHERE trackId = :trackId")
    suspend fun track(trackId: String): TrackEntity?

    @Query("SELECT trackId FROM tracks WHERE sourceId = :sourceId")
    suspend fun trackIdsForSource(sourceId: String): List<String>

    @Query("SELECT * FROM scan_checkpoints WHERE sourceId = :sourceId")
    suspend fun checkpointForSource(sourceId: String): ScanCheckpointEntity?

    @Query("UPDATE tracks SET available = 0 WHERE trackId IN (:trackIds)")
    suspend fun markTracksUnavailable(trackIds: List<String>)

    @Query("UPDATE tracks SET available = 0 WHERE sourceId = :sourceId")
    suspend fun markSourceTracksUnavailable(sourceId: String)
}
