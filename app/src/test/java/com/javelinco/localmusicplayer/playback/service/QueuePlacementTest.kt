package com.javelinco.localmusicplayer.playback.service

import org.junit.Assert.assertEquals
import org.junit.Test

class QueuePlacementTest {
    @Test
    fun normalAddAppendsAndShuffleUsesAnUnplayedPosition() {
        assertEquals(5, queueInsertionIndex(currentIndex = 1, itemCount = 5, shuffleEnabled = false) { 0 })
        assertEquals(2, queueInsertionIndex(currentIndex = 1, itemCount = 5, shuffleEnabled = true) { 0 })
        assertEquals(5, queueInsertionIndex(currentIndex = 1, itemCount = 5, shuffleEnabled = true) { 3 })
    }

    @Test
    fun emptyQueueInsertionStartsAtZero() {
        assertEquals(0, queueInsertionIndex(currentIndex = -1, itemCount = 0, shuffleEnabled = true) { 0 })
    }
}
