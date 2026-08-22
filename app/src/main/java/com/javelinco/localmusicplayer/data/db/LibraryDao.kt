package com.javelinco.localmusicplayer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlin.math.abs

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIgnoredTrack(track: IgnoredTrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIgnoredTracks(tracks: List<IgnoredTrackEntity>)

    @Query("SELECT * FROM ignored_tracks ORDER BY ignoredAtEpochMs DESC, ignoreId")
    fun observeIgnoredTracks(): Flow<List<IgnoredTrackEntity>>

    @Query("SELECT * FROM ignored_tracks ORDER BY ignoredAtEpochMs DESC, ignoreId")
    suspend fun ignoredTracks(): List<IgnoredTrackEntity>

    @Query("SELECT * FROM ignored_tracks WHERE ignoreId = :ignoreId")
    suspend fun ignoredTrack(ignoreId: String): IgnoredTrackEntity?

    @Query("DELETE FROM ignored_tracks WHERE ignoreId = :ignoreId")
    suspend fun deleteIgnoredTrack(ignoreId: String)

    @Query("DELETE FROM ignored_tracks")
    suspend fun deleteIgnoredTracks()

    @Query("UPDATE tracks SET available = :available WHERE trackId = :trackId")
    suspend fun setTrackAvailability(trackId: String, available: Boolean)

    @Query(
        "UPDATE ignored_tracks SET trackId = :trackId, sourceId = :sourceId, contentUri = :contentUri " +
            "WHERE ignoreId = :ignoreId",
    )
    suspend fun linkIgnoredTrack(ignoreId: String, trackId: String, sourceId: String, contentUri: String)

    @Transaction
    suspend fun ignoreTrack(trackId: String, ignoredAtEpochMs: Long) {
        val track = track(trackId) ?: return
        insertIgnoredTrack(
            IgnoredTrackEntity(
                ignoreId = track.trackId,
                trackId = track.trackId,
                sourceId = track.sourceId,
                contentUri = track.contentUri,
                relativePath = track.contentUri.substringAfter(':').takeIf(String::isNotBlank),
                fileName = track.fileName,
                title = track.title,
                artist = track.artist,
                normalizedTitle = track.normalizedTitle,
                normalizedArtist = track.normalizedArtist,
                durationMs = track.durationMs,
                sizeBytes = track.sizeBytes,
                ignoredAtEpochMs = ignoredAtEpochMs,
            ),
        )
        setTrackAvailability(track.trackId, false)
    }

    @Transaction
    suspend fun restoreIgnoredTrack(ignoreId: String) {
        val ignored = ignoredTrack(ignoreId) ?: return
        deleteIgnoredTrack(ignoreId)
        ignored.trackId?.let { setTrackAvailability(it, true) }
    }

    @Transaction
    suspend fun replaceIgnoredTracks(tracks: List<IgnoredTrackEntity>) {
        deleteIgnoredTracks()
        if (tracks.isNotEmpty()) insertIgnoredTracks(tracks)
        tracks.mapNotNull(IgnoredTrackEntity::trackId).forEach { setTrackAvailability(it, false) }
    }

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
            val ignoredMatches = matchIgnoredTracks(batch.tracks, ignoredTracks())
            ignoredMatches.forEach { (trackId, ignored) ->
                batch.tracks.find { it.trackId == trackId }?.let { track ->
                    linkIgnoredTrack(ignored.ignoreId, track.trackId, track.sourceId, track.contentUri)
                }
            }
            val indexedTracks = batch.tracks.map { track ->
                if (track.trackId in ignoredMatches) track.copy(available = false) else track
            }
            upsertTracks(indexedTracks)
            val trackIds = batch.tracks.map(TrackEntity::trackId)
            deleteSearchRows(trackIds)
            val searchable = indexedTracks.filter(TrackEntity::available)
            if (searchable.isNotEmpty()) insertSearchRows(searchable.map(TrackSearchFts::from))
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
    fun observeAlbumGroups(): Flow<List<AlbumSummary>>

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
    fun observeArtistGroups(): Flow<List<NamedGroupSummary>>

    @Query(
        """
        SELECT normalizedArtist AS normalizedName,
            CASE WHEN normalizedArtist = '' THEN 'Unknown Artist'
                ELSE COALESCE(MAX(artist), 'Unknown Artist') END AS displayName,
            COUNT(*) AS trackCount
        FROM tracks
        WHERE available = 1 AND normalizedArtist LIKE '%' || :query || '%'
        GROUP BY normalizedArtist
        ORDER BY displayName COLLATE NOCASE
        LIMIT :limit
        """,
    )
    suspend fun searchArtistGroups(query: String, limit: Int = 200): List<NamedGroupSummary>

    @Query(
        """
        SELECT normalizedAlbumArtist, normalizedAlbumTitle,
            CASE WHEN normalizedAlbumArtist = '' THEN 'Unknown Artist'
                ELSE COALESCE(MAX(albumArtist), 'Unknown Artist') END AS displayArtist,
            CASE WHEN normalizedAlbumTitle = '' THEN 'Unknown Album'
                ELSE COALESCE(MAX(albumTitle), 'Unknown Album') END AS displayTitle,
            COUNT(*) AS trackCount
        FROM tracks
        WHERE available = 1 AND (
            normalizedAlbumTitle LIKE '%' || :query || '%' OR
            normalizedAlbumArtist LIKE '%' || :query || '%'
        )
        GROUP BY normalizedAlbumArtist, normalizedAlbumTitle
        ORDER BY displayArtist COLLATE NOCASE, displayTitle COLLATE NOCASE
        LIMIT :limit
        """,
    )
    suspend fun searchAlbumGroups(query: String, limit: Int = 200): List<AlbumSummary>

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
    fun observeGenreGroups(): Flow<List<NamedGroupSummary>>

    @Query(
        """
        SELECT normalizedGenre AS normalizedName,
            CASE WHEN normalizedGenre = '' THEN 'Unknown Genre'
                ELSE COALESCE(MAX(genre), 'Unknown Genre') END AS displayName,
            COUNT(*) AS trackCount
        FROM tracks
        WHERE available = 1 AND normalizedGenre LIKE '%' || :query || '%'
        GROUP BY normalizedGenre
        ORDER BY displayName COLLATE NOCASE
        LIMIT :limit
        """,
    )
    suspend fun searchGenreGroups(query: String, limit: Int = 200): List<NamedGroupSummary>

    @Query("SELECT * FROM tracks WHERE trackId = :trackId")
    suspend fun track(trackId: String): TrackEntity?

    @Query("SELECT trackId FROM tracks WHERE sourceId = :sourceId")
    suspend fun trackIdsForSource(sourceId: String): List<String>

    @Query("SELECT * FROM tracks WHERE sourceId = :sourceId ORDER BY trackId")
    suspend fun tracksForSource(sourceId: String): List<TrackEntity>

    @Query("SELECT * FROM scan_checkpoints WHERE sourceId = :sourceId")
    suspend fun checkpointForSource(sourceId: String): ScanCheckpointEntity?

    @Query("DELETE FROM scan_checkpoints WHERE sourceId = :sourceId")
    suspend fun clearCheckpoint(sourceId: String)

    @Query("UPDATE tracks SET available = 0 WHERE trackId IN (:trackIds)")
    suspend fun markTracksUnavailable(trackIds: List<String>)

    @Query("UPDATE tracks SET available = 0 WHERE available = 1 AND trackId IN (:trackIds)")
    suspend fun markAvailableTracksUnavailable(trackIds: List<String>): Int

    @Query("UPDATE tracks SET available = 0 WHERE sourceId = :sourceId")
    suspend fun markSourceTracksUnavailable(sourceId: String)
}

private fun matchIgnoredTracks(
    tracks: List<TrackEntity>,
    ignored: List<IgnoredTrackEntity>,
): Map<String, IgnoredTrackEntity> {
    val result = linkedMapOf<String, IgnoredTrackEntity>()
    ignored.forEach { rule ->
        tracks.find { it.trackId == rule.trackId || it.trackId == rule.ignoreId }?.let {
            result[it.trackId] = rule
            return@forEach
        }
        rule.relativePath?.let { path ->
            val exact = tracks.filter { it.contentUri.substringAfter(':').equals(path, ignoreCase = true) }
            if (exact.size == 1) {
                result[exact.single().trackId] = rule
                return@forEach
            }
        }
        val scored = tracks.map { track ->
            var score = 0
            if (track.sizeBytes == rule.sizeBytes) score += 4
            if (abs(track.durationMs - rule.durationMs) <= 2_000) score += 3
            if (rule.normalizedTitle.isNotBlank() && track.normalizedTitle == rule.normalizedTitle) score += 2
            if (rule.normalizedArtist.isNotBlank() && track.normalizedArtist == rule.normalizedArtist) score += 1
            track to score
        }.filter { it.second >= 5 }
        val best = scored.maxOfOrNull(Pair<TrackEntity, Int>::second) ?: return@forEach
        val candidates = scored.filter { it.second == best }
        if (candidates.size == 1) result[candidates.single().first.trackId] = rule
    }
    return result
}
