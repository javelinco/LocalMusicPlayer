package com.javelinco.localmusicplayer.playback.queue

import com.javelinco.localmusicplayer.core.model.TrackId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueEngineTest {
    private val a = TrackId("a")
    private val b = TrackId("b")
    private val c = TrackId("c")

    @Test
    fun playViewKeepsItemsBehindCursorAndPreviousUsesThreeSecondRule() {
        val engine = QueueEngine(random = SequenceRandom())
        engine.dispatch(QueueCommand.PlayView(listOf(a, b, c), b))

        assertEquals(QueueEffect.SeekToStart(b), engine.dispatch(QueueCommand.Previous(3_001)))
        assertEquals(QueueEffect.Play(a), engine.dispatch(QueueCommand.Previous(0)))
        assertEquals(listOf(b, c), engine.state.value.upcoming)
    }

    @Test
    fun repeatOneAffectsNaturalCompletionButNeverBlocksManualNext() {
        val engine = QueueEngine(random = SequenceRandom())
        engine.dispatch(QueueCommand.PlayView(listOf(a, b, c), b))
        engine.dispatch(QueueCommand.SetRepeat(RepeatMode.ONE))

        assertEquals(QueueEffect.Play(b), engine.dispatch(QueueCommand.TrackEnded))
        assertEquals(QueueEffect.Play(c), engine.dispatch(QueueCommand.Next))
    }

    @Test
    fun shuffleToggleKeepsCurrentAndHistoryAndUsesEveryUnplayedTrackOnce() {
        val engine = QueueEngine(random = SequenceRandom(0, 0))
        engine.dispatch(QueueCommand.PlayView(listOf(a, b, c), a))
        engine.dispatch(QueueCommand.Next)
        val current = engine.state.value.current
        val history = engine.state.value.history

        engine.dispatch(QueueCommand.SetShuffle(true))

        assertEquals(current, engine.state.value.current)
        assertEquals(history, engine.state.value.history)
        assertEquals(setOf(c), engine.state.value.upcoming.toSet())
        assertEquals(engine.state.value.upcoming.size, engine.state.value.upcoming.distinct().size)
    }

    @Test
    fun playNextOverridesShuffleAndAddUsesRandomUnplayedPosition() {
        val d = TrackId("d")
        val e = TrackId("e")
        val engine = QueueEngine(random = SequenceRandom(0, 1, 0))
        engine.dispatch(QueueCommand.SetShuffle(true))
        engine.dispatch(QueueCommand.PlayView(listOf(a, b, c), a))

        engine.dispatch(QueueCommand.PlayNext(listOf(d)))
        assertEquals(d, engine.state.value.upcoming.first())
        engine.dispatch(QueueCommand.Add(listOf(e)))

        assertTrue(e in engine.state.value.upcoming)
        assertEquals(5, engine.state.value.sourceOrder.size)
    }

    @Test
    fun repeatAllCreatesFreshShuffleCycleAndUnavailableTracksAreSkipped() {
        val engine = QueueEngine(random = SequenceRandom(0, 0, 1, 0))
        engine.dispatch(QueueCommand.SetShuffle(true))
        engine.dispatch(QueueCommand.SetRepeat(RepeatMode.ALL))
        engine.dispatch(QueueCommand.PlayView(listOf(a, b, c), a))
        engine.dispatch(QueueCommand.SetUnavailable(setOf(b)))

        repeat(2) { engine.dispatch(QueueCommand.Next) }
        engine.dispatch(QueueCommand.TrackEnded)

        assertFalse(engine.state.value.current == b)
        assertEquals(setOf(a, b, c), engine.state.value.cycleOrder.toSet())
    }

    @Test
    fun savedSessionRestoresExactPermutationPaused() {
        val engine = QueueEngine(random = SequenceRandom(0, 0))
        engine.dispatch(QueueCommand.SetShuffle(true))
        engine.dispatch(QueueCommand.PlayView(listOf(a, b, c), a))
        engine.dispatch(QueueCommand.Next)
        val saved = engine.snapshot(positionMs = 12_345)

        val restored = QueueEngine.restore(saved, SequenceRandom())

        assertEquals(engine.state.value.cycleOrder, restored.state.value.cycleOrder)
        assertEquals(engine.state.value.current, restored.state.value.current)
        assertEquals(12_345, restored.state.value.positionMs)
        assertFalse(restored.state.value.playing)
        assertEquals(saved, QueueSessionCodec.decode(QueueSessionCodec.encode(saved)))
    }

    @Test
    fun reorderAndRemoveEditOnlyTheCurrentQueue() {
        val engine = QueueEngine(random = SequenceRandom())
        engine.dispatch(QueueCommand.PlayView(listOf(a, b, c), a))

        engine.dispatch(QueueCommand.Move(1, 0))
        engine.dispatch(QueueCommand.Remove(b))

        assertEquals(listOf(c), engine.state.value.upcoming)
        assertEquals(listOf(a, c), engine.state.value.sourceOrder)
    }
}
