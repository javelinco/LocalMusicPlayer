package com.javelinco.localmusicplayer.playback.service

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.javelinco.localmusicplayer.data.db.TrackEntity

object MediaItemMapper {
    fun toMediaItem(track: TrackEntity): MediaItem {
        val uri = Uri.parse(track.contentUri)
        require(uri.scheme in LOCAL_SCHEMES) { "Playback URI must be local: ${uri.scheme}" }
        return MediaItem.Builder()
            .setMediaId(track.trackId)
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title ?: track.fileName)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.albumTitle)
                    .setAlbumArtist(track.albumArtist)
                    .setTrackNumber(track.trackNumber)
                    .setDiscNumber(track.discNumber)
                    .build(),
            )
            .build()
    }

    private val LOCAL_SCHEMES = setOf("content", "file", "android.resource")
}
