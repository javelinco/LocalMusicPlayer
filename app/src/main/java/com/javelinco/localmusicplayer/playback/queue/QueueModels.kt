package com.javelinco.localmusicplayer.playback.queue

import com.javelinco.localmusicplayer.core.model.TrackId

enum class RepeatMode { OFF, ALL, ONE }

data class QueueState(
    val sourceOrder: List<TrackId> = emptyList(),
    val cycleOrder: List<TrackId> = emptyList(),
    val current: TrackId? = null,
    val behind: List<TrackId> = emptyList(),
    val history: List<TrackId> = emptyList(),
    val upcoming: List<TrackId> = emptyList(),
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val unavailable: Set<TrackId> = emptySet(),
    val positionMs: Long = 0,
    val playing: Boolean = false,
)

sealed interface QueueCommand {
    data class PlayView(val tracks: List<TrackId>, val selected: TrackId) : QueueCommand
    data object Next : QueueCommand
    data class Previous(val positionMs: Long) : QueueCommand
    data object TrackEnded : QueueCommand
    data class SetShuffle(val enabled: Boolean) : QueueCommand
    data class SetRepeat(val mode: RepeatMode) : QueueCommand
    data class PlayNext(val tracks: List<TrackId>) : QueueCommand
    data class Add(val tracks: List<TrackId>) : QueueCommand
    data class Move(val from: Int, val to: Int) : QueueCommand
    data class Remove(val track: TrackId) : QueueCommand
    data class SetUnavailable(val tracks: Set<TrackId>) : QueueCommand
}

sealed interface QueueEffect {
    data object None : QueueEffect
    data class Play(val track: TrackId) : QueueEffect
    data class SeekToStart(val track: TrackId) : QueueEffect
    data object StopAtEnd : QueueEffect
}

data class QueueSessionDto(
    val sourceOrder: List<String>,
    val cycleOrder: List<String>,
    val current: String?,
    val behind: List<String>,
    val history: List<String>,
    val upcoming: List<String>,
    val shuffleEnabled: Boolean,
    val repeatMode: String,
    val unavailable: Set<String>,
    val positionMs: Long,
)
