package com.javelinco.localmusicplayer.playback.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackHistoryTrackerTest {
    @Test
    fun recordsOnlyWhenAnItemActuallyStartsAndFollowsAutomaticTransitions() {
        val tracker = PlaybackHistoryTracker()
        tracker.queueStarted(playlistId = "mix")

        assertNull(tracker.onPlaybackState(mediaId = "one", isPlaying = false))
        assertEquals(
            PlaybackHistoryRecord(trackId = "one", playlistId = "mix"),
            tracker.onPlaybackState(mediaId = "one", isPlaying = true),
        )
        assertNull(tracker.onPlaybackState(mediaId = "one", isPlaying = true))
        assertEquals(
            PlaybackHistoryRecord(trackId = "two", playlistId = null),
            tracker.onPlaybackState(mediaId = "two", isPlaying = true),
        )
    }

    @Test
    fun explicitReplayOfTheSameTrackCreatesANewHistoryEvent() {
        val tracker = PlaybackHistoryTracker()
        tracker.queueStarted(playlistId = null)
        assertEquals("one", tracker.onPlaybackState("one", true)?.trackId)

        tracker.queueStarted(playlistId = null)

        assertEquals("one", tracker.onPlaybackState("one", true)?.trackId)
    }
}
