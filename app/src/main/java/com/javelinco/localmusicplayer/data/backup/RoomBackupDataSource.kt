package com.javelinco.localmusicplayer.data.backup

import android.net.Uri
import android.provider.DocumentsContract
import com.javelinco.localmusicplayer.BuildConfig
import com.javelinco.localmusicplayer.data.db.FavoriteEntity
import com.javelinco.localmusicplayer.data.db.LibraryDao
import com.javelinco.localmusicplayer.data.db.PlaylistEntity
import com.javelinco.localmusicplayer.data.db.PlaylistEntryEntity
import com.javelinco.localmusicplayer.data.db.SettingsMetadataEntity
import com.javelinco.localmusicplayer.data.db.UserDataDao
import com.javelinco.localmusicplayer.data.db.UserDataSnapshot
import java.util.UUID

class RoomBackupDataSource(
    private val libraryDao: LibraryDao,
    private val userDataDao: UserDataDao,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend fun snapshot(): BackupBundle {
        val users = userDataDao.snapshot()
        val tracks = libraryDao.allTracks().associateBy { it.trackId }
        return BackupBundle(
            manifest = BackupManifest(
                createdAtEpochMs = clock(),
                appVersion = BuildConfig.VERSION_NAME,
            ),
            userData = BackupUserData(
                playlists = users.playlists.map { playlist ->
                    BackupPlaylist(
                        id = playlist.playlistId,
                        name = playlist.name,
                        trackIds = users.entries
                            .filter { it.playlistId == playlist.playlistId }
                            .sortedBy { it.position }
                            .map { it.trackId },
                    )
                },
                favorites = users.favorites.map {
                    BackupFavorite(it.trackId, it.titleSnapshot, it.contentUriSnapshot, it.addedAtEpochMs)
                },
                settings = users.settings.associate { it.key to it.value },
                sources = libraryDao.sources().map {
                    BackupSource(it.sourceId, it.kind, it.location, it.label)
                },
                queueSession = users.queueSessions.singleOrNull()?.queueJson,
                trackReferences = tracks.mapValues { (_, track) ->
                    PortableTrackReference(
                        relativePath = track.contentUri.portablePath(),
                        sizeBytes = track.sizeBytes,
                        durationMs = track.durationMs,
                        normalizedTitle = track.normalizedTitle,
                        normalizedArtist = track.normalizedArtist,
                    )
                },
            ),
        )
    }

    suspend fun restore(bundle: BackupBundle) {
        val now = clock()
        val candidates = libraryDao.allTracks().filter { it.available }.map { track ->
            RelinkCandidate(
                trackId = com.javelinco.localmusicplayer.core.model.TrackId(track.trackId),
                relativePath = track.contentUri.portablePath(),
                sizeBytes = track.sizeBytes,
                durationMs = track.durationMs,
                normalizedTitle = track.normalizedTitle,
                normalizedArtist = track.normalizedArtist,
            )
        }
        val relinked = bundle.userData.trackReferences.mapValues { (_, reference) ->
            (TrackRelinker.relink(reference, candidates) as? RelinkResult.Matched)?.trackId?.value
        }
        fun restoredTrackId(old: String): String = relinked[old] ?: old

        val playlists = bundle.userData.playlists.map {
            PlaylistEntity(it.id, it.name, now, now)
        }
        val entries = bundle.userData.playlists.flatMap { playlist ->
            playlist.trackIds.mapIndexed { position, trackId ->
                PlaylistEntryEntity(
                    entryId = UUID.randomUUID().toString(),
                    playlistId = playlist.id,
                    position = position,
                    trackId = restoredTrackId(trackId),
                    titleSnapshot = trackId,
                    contentUriSnapshot = "",
                    addedAtEpochMs = now,
                )
            }
        }
        val favorites = bundle.userData.favorites.map {
            FavoriteEntity(
                trackId = restoredTrackId(it.trackId),
                titleSnapshot = it.titleSnapshot,
                contentUriSnapshot = it.contentUriSnapshot,
                addedAtEpochMs = it.addedAtEpochMs,
            )
        }
        userDataDao.replaceUserData(
            UserDataSnapshot(
                playlists = playlists,
                entries = entries,
                favorites = favorites,
                queueSessions = emptyList(),
                settings = bundle.userData.settings.map { SettingsMetadataEntity(it.key, it.value) },
            ),
        )
    }
}

private fun String.portablePath(): String? = runCatching {
    val uri = Uri.parse(this)
    runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull() ?: uri.lastPathSegment
}.getOrNull()?.substringAfter(':')?.takeIf(String::isNotBlank)
