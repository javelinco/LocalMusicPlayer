package com.javelinco.localmusicplayer.home

import com.javelinco.localmusicplayer.data.db.TrackEntity

data class RecentPlaybackQueue(
    val selected: TrackEntity,
    val tracks: List<TrackEntity>,
)

fun recentPlaybackQueue(
    selectedTrackId: String,
    displayedTracks: List<TrackEntity>,
): RecentPlaybackQueue? {
    val selected = displayedTracks.find { it.trackId == selectedTrackId } ?: return null
    return RecentPlaybackQueue(selected, displayedTracks.toList())
}
