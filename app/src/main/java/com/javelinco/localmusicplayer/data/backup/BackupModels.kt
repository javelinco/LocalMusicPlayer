package com.javelinco.localmusicplayer.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupManifest(
    val format: String = FORMAT,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val createdAtEpochMs: Long,
    val appVersion: String,
) {
    companion object {
        const val FORMAT = "LocalMusicPlayerBackup"
        const val CURRENT_SCHEMA_VERSION = 2
    }
}

@Serializable
data class BackupPlaylist(
    val id: String,
    val name: String,
    val trackIds: List<String>,
)

@Serializable
data class BackupFavorite(
    val trackId: String,
    val titleSnapshot: String = "",
    val contentUriSnapshot: String = "",
    val addedAtEpochMs: Long = 0,
)

@Serializable
data class BackupIgnoredTrack(
    val oldTrackId: String,
    val reference: PortableTrackReference,
    val title: String? = null,
    val artist: String? = null,
    val fileName: String = "",
    val ignoredAtEpochMs: Long = 0,
)

@Serializable
data class BackupSource(
    val id: String,
    val kind: String,
    val location: String,
    val label: String,
)

@Serializable
data class PortableTrackReference(
    val relativePath: String?,
    val sizeBytes: Long,
    val durationMs: Long,
    val normalizedTitle: String,
    val normalizedArtist: String,
)

@Serializable
data class BackupUserData(
    val playlists: List<BackupPlaylist> = emptyList(),
    val favorites: List<BackupFavorite> = emptyList(),
    val settings: Map<String, String> = emptyMap(),
    val sources: List<BackupSource> = emptyList(),
    val queueSession: String? = null,
    val trackReferences: Map<String, PortableTrackReference> = emptyMap(),
    val ignoredTracks: List<BackupIgnoredTrack> = emptyList(),
)

data class BackupBundle(
    val manifest: BackupManifest,
    val userData: BackupUserData,
)

class InvalidBackupException(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)
