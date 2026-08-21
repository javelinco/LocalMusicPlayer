package com.javelinco.localmusicplayer.playback.queue

import com.javelinco.localmusicplayer.core.model.TrackId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class QueueEngine private constructor(
    initialState: QueueState,
    private val random: RandomSource,
) {
    constructor(random: RandomSource = SecureRandomSource()) : this(QueueState(), random)

    private val mutableState = MutableStateFlow(initialState)
    val state: StateFlow<QueueState> = mutableState.asStateFlow()

    fun dispatch(command: QueueCommand): QueueEffect = when (command) {
        is QueueCommand.PlayView -> playView(command)
        QueueCommand.Next -> advance(allowRepeatAll = true)
        is QueueCommand.Previous -> previous(command.positionMs)
        QueueCommand.TrackEnded -> trackEnded()
        is QueueCommand.SetShuffle -> setShuffle(command.enabled)
        is QueueCommand.SetRepeat -> update { copy(repeatMode = command.mode) }
        is QueueCommand.PlayNext -> playNext(command.tracks)
        is QueueCommand.Add -> add(command.tracks)
        is QueueCommand.Move -> move(command.from, command.to)
        is QueueCommand.Remove -> remove(command.track)
        is QueueCommand.SetUnavailable -> update { copy(unavailable = command.tracks) }
    }

    fun snapshot(positionMs: Long = state.value.positionMs): QueueSessionDto = state.value.let { value ->
        QueueSessionDto(
            sourceOrder = value.sourceOrder.map(TrackId::value),
            cycleOrder = value.cycleOrder.map(TrackId::value),
            current = value.current?.value,
            behind = value.behind.map(TrackId::value),
            history = value.history.map(TrackId::value),
            upcoming = value.upcoming.map(TrackId::value),
            shuffleEnabled = value.shuffleEnabled,
            repeatMode = value.repeatMode.name,
            unavailable = value.unavailable.mapTo(linkedSetOf(), TrackId::value),
            positionMs = positionMs,
        )
    }

    private fun playView(command: QueueCommand.PlayView): QueueEffect {
        val tracks = command.tracks.distinct()
        require(command.selected in tracks)
        val selectedIndex = tracks.indexOf(command.selected)
        val shuffled = if (state.value.shuffleEnabled) {
            tracks.filterNot { it == command.selected }.uniformlyShuffled(random)
        } else {
            tracks.drop(selectedIndex + 1)
        }
        mutableState.value = state.value.copy(
            sourceOrder = tracks,
            cycleOrder = if (state.value.shuffleEnabled) listOf(command.selected) + shuffled else tracks,
            current = command.selected,
            behind = if (state.value.shuffleEnabled) emptyList() else tracks.take(selectedIndex),
            history = emptyList(),
            upcoming = shuffled,
            positionMs = 0,
            playing = true,
        )
        return QueueEffect.Play(command.selected)
    }

    private fun previous(positionMs: Long): QueueEffect {
        val value = state.value
        val current = value.current ?: return QueueEffect.None
        if (positionMs > PREVIOUS_RESTART_THRESHOLD_MS) {
            mutableState.value = value.copy(positionMs = 0)
            return QueueEffect.SeekToStart(current)
        }
        val previous = value.history.lastOrNull() ?: value.behind.lastOrNull() ?: return QueueEffect.None
        val fromHistory = value.history.isNotEmpty()
        mutableState.value = value.copy(
            current = previous,
            history = if (fromHistory) value.history.dropLast(1) else value.history,
            behind = if (fromHistory) value.behind else value.behind.dropLast(1),
            upcoming = listOf(current) + value.upcoming,
            positionMs = 0,
            playing = true,
        )
        return QueueEffect.Play(previous)
    }

    private fun trackEnded(): QueueEffect {
        val value = state.value
        if (value.repeatMode == RepeatMode.ONE && value.current != null) {
            mutableState.value = value.copy(positionMs = 0, playing = true)
            return QueueEffect.Play(value.current)
        }
        return advance(allowRepeatAll = true)
    }

    private fun advance(allowRepeatAll: Boolean): QueueEffect {
        val value = state.value
        val playableIndex = value.upcoming.indexOfFirst { it !in value.unavailable }
        if (playableIndex >= 0) {
            val next = value.upcoming[playableIndex]
            mutableState.value = value.copy(
                current = next,
                history = value.current?.let { value.history + it } ?: value.history,
                upcoming = value.upcoming.drop(playableIndex + 1),
                positionMs = 0,
                playing = true,
            )
            return QueueEffect.Play(next)
        }
        if (allowRepeatAll && value.repeatMode == RepeatMode.ALL) return startFreshCycle(value)
        mutableState.value = value.copy(playing = false)
        return QueueEffect.StopAtEnd
    }

    private fun startFreshCycle(value: QueueState): QueueEffect {
        val cycle = if (value.shuffleEnabled) value.sourceOrder.uniformlyShuffled(random) else value.sourceOrder
        val playableIndex = cycle.indexOfFirst { it !in value.unavailable }
        if (playableIndex < 0) {
            mutableState.value = value.copy(cycleOrder = cycle, playing = false)
            return QueueEffect.StopAtEnd
        }
        val next = cycle[playableIndex]
        mutableState.value = value.copy(
            cycleOrder = cycle,
            current = next,
            behind = cycle.take(playableIndex),
            history = value.current?.let { value.history + it } ?: value.history,
            upcoming = cycle.drop(playableIndex + 1),
            positionMs = 0,
            playing = true,
        )
        return QueueEffect.Play(next)
    }

    private fun setShuffle(enabled: Boolean): QueueEffect {
        val value = state.value
        if (enabled == value.shuffleEnabled) return QueueEffect.None
        val remaining = if (enabled) {
            value.upcoming.uniformlyShuffled(random)
        } else {
            val pending = value.upcoming.toSet()
            value.sourceOrder.filter { it in pending }
        }
        mutableState.value = value.copy(
            shuffleEnabled = enabled,
            cycleOrder = value.current?.let { listOf(it) + remaining } ?: remaining,
            upcoming = remaining,
        )
        return QueueEffect.None
    }

    private fun playNext(tracks: List<TrackId>): QueueEffect {
        val additions = tracks.distinct().filterNot { it == state.value.current }
        if (additions.isEmpty()) return QueueEffect.None
        val value = state.value
        mutableState.value = value.copy(
            sourceOrder = insertAfterCurrent(value.sourceOrder, value.current, additions),
            cycleOrder = value.cycleOrder + additions.filterNot(value.cycleOrder::contains),
            upcoming = additions + value.upcoming.filterNot(additions::contains),
        )
        return QueueEffect.None
    }

    private fun add(tracks: List<TrackId>): QueueEffect {
        val value = state.value
        val additions = tracks.distinct().filterNot(value.sourceOrder::contains)
        val upcoming = value.upcoming.toMutableList()
        additions.forEach { track ->
            val index = if (value.shuffleEnabled) random.nextInt(upcoming.size + 1) else upcoming.size
            upcoming.add(index, track)
        }
        mutableState.value = value.copy(
            sourceOrder = value.sourceOrder + additions,
            cycleOrder = value.cycleOrder + additions,
            upcoming = upcoming,
        )
        return QueueEffect.None
    }

    private fun move(from: Int, to: Int): QueueEffect {
        val value = state.value
        if (from !in value.upcoming.indices || to !in value.upcoming.indices || from == to) return QueueEffect.None
        val upcoming = value.upcoming.toMutableList()
        upcoming.add(to, upcoming.removeAt(from))
        mutableState.value = value.copy(upcoming = upcoming, cycleOrder = listOfNotNull(value.current) + upcoming)
        return QueueEffect.None
    }

    private fun remove(track: TrackId): QueueEffect {
        val value = state.value
        if (track == value.current) {
            mutableState.value = value.copy(sourceOrder = value.sourceOrder - track)
            return advance(allowRepeatAll = false)
        }
        mutableState.value = value.copy(
            sourceOrder = value.sourceOrder - track,
            cycleOrder = value.cycleOrder - track,
            behind = value.behind - track,
            upcoming = value.upcoming - track,
        )
        return QueueEffect.None
    }

    private fun update(transform: QueueState.() -> QueueState): QueueEffect {
        mutableState.value = state.value.transform()
        return QueueEffect.None
    }

    companion object {
        private const val PREVIOUS_RESTART_THRESHOLD_MS = 3_000L

        fun restore(session: QueueSessionDto, random: RandomSource = SecureRandomSource()): QueueEngine {
            fun List<String>.ids() = map(::TrackId)
            return QueueEngine(
                QueueState(
                    sourceOrder = session.sourceOrder.ids(),
                    cycleOrder = session.cycleOrder.ids(),
                    current = session.current?.let(::TrackId),
                    behind = session.behind.ids(),
                    history = session.history.ids(),
                    upcoming = session.upcoming.ids(),
                    shuffleEnabled = session.shuffleEnabled,
                    repeatMode = RepeatMode.valueOf(session.repeatMode),
                    unavailable = session.unavailable.mapTo(linkedSetOf(), ::TrackId),
                    positionMs = session.positionMs,
                    playing = false,
                ),
                random,
            )
        }
    }
}

private fun insertAfterCurrent(
    source: List<TrackId>,
    current: TrackId?,
    additions: List<TrackId>,
): List<TrackId> {
    val withoutDuplicates = source.filterNot(additions::contains)
    val index = withoutDuplicates.indexOf(current).takeIf { it >= 0 }?.plus(1) ?: withoutDuplicates.size
    return withoutDuplicates.take(index) + additions + withoutDuplicates.drop(index)
}
