package com.javelinco.localmusicplayer.playback.queue

import com.javelinco.localmusicplayer.core.model.TrackId
import org.junit.Assert.assertEquals
import org.junit.Test

class ShufflePropertyTest {
    @Test
    fun fisherYatesProducesAPermutationForManySizesAndSeeds() {
        for (size in 0..200) {
            val input = (0 until size).map { TrackId(it.toString()) }
            repeat(20) { seed ->
                val shuffled = input.uniformlyShuffled(SeededRandom(seed.toLong()))
                assertEquals(input.toSet(), shuffled.toSet())
                assertEquals(input.size, shuffled.size)
            }
        }
    }

    @Test
    fun deterministicDrawsValidateInclusiveFisherYatesBounds() {
        val input = listOf(TrackId("a"), TrackId("b"), TrackId("c"))

        assertEquals(
            listOf(TrackId("b"), TrackId("c"), TrackId("a")),
            input.uniformlyShuffled(SequenceRandom(0, 0)),
        )
    }
}
