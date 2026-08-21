package com.javelinco.localmusicplayer.playback.service

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class PersistedMediaItem(
    val mediaId: String,
    val uri: String,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
) {
    fun toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(mediaId)
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .build(),
        )
        .build()
}

@Serializable
data class PersistedPlaybackSession(
    val items: List<PersistedMediaItem>,
    val currentIndex: Int,
    val positionMs: Long,
    val repeatMode: Int,
    val shuffleOrderApplied: Boolean,
)

object PlaybackSessionCodec {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = false }
    fun encode(session: PersistedPlaybackSession): String = json.encodeToString(session)
    fun decode(value: String): PersistedPlaybackSession = json.decodeFromString(value)
}

class PlaybackSessionFileStore(context: Context) {
    private val sessionFile = context.filesDir.resolve("playback-session-v1.json")

    fun load(): PersistedPlaybackSession? = runCatching {
        sessionFile.takeIf { it.isFile }?.readText()?.let(PlaybackSessionCodec::decode)
    }.getOrNull()

    fun save(session: PersistedPlaybackSession) {
        val temporary = sessionFile.resolveSibling("${sessionFile.name}.tmp")
        temporary.writeText(PlaybackSessionCodec.encode(session))
        if (!temporary.renameTo(sessionFile)) {
            sessionFile.writeText(temporary.readText())
            temporary.delete()
        }
    }
}
