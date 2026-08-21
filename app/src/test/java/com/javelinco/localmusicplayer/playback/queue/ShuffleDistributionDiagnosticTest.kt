package com.javelinco.localmusicplayer.playback.queue

import com.javelinco.localmusicplayer.core.model.TrackId
import org.junit.Assert.assertEquals
import org.junit.Test

class ShuffleDistributionDiagnosticTest {
    @Test
    fun reportsPositionCountsWithoutUsingStatisticsAsAReleaseGate() {
        val input = (0 until 5).map { TrackId(it.toString()) }
        val counts = Array(5) { IntArray(5) }
        repeat(10_000) { seed ->
            input.uniformlyShuffled(SeededRandom(seed.toLong())).forEachIndexed { position, track ->
                counts[track.value.toInt()][position]++
            }
        }
        counts.forEach { row -> assertEquals(10_000, row.sum()) }
        println("Shuffle position diagnostic: ${counts.joinToString { it.contentToString() }}")
    }
}
