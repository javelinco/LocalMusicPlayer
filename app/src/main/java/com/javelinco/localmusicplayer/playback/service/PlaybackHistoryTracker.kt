package com.javelinco.localmusicplayer.playback.service

data class PlaybackHistoryRecord(
    val trackId: String,
    val playlistId: String?,
)

class PlaybackHistoryTracker {
    private var lastRecordedMediaId: String? = null
    private var pendingPlaylistId: String? = null

    fun queueStarted(playlistId: String?) {
        lastRecordedMediaId = null
        pendingPlaylistId = playlistId
    }

    fun onPlaybackState(mediaId: String?, isPlaying: Boolean): PlaybackHistoryRecord? {
        if (!isPlaying || mediaId == null || mediaId == lastRecordedMediaId) return null
        lastRecordedMediaId = mediaId
        return PlaybackHistoryRecord(mediaId, pendingPlaylistId).also {
            pendingPlaylistId = null
        }
    }
}
