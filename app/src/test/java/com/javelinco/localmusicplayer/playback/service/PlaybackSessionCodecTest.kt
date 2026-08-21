package com.javelinco.localmusicplayer.playback.service

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackSessionCodecTest {
    @Test fun sessionRoundTripPreservesOrderPositionAndModes() {
        val session = PersistedPlaybackSession(
            items = listOf(PersistedMediaItem("a", "content://a", "A", "Artist")),
            currentIndex = 0,
            positionMs = 4321,
            repeatMode = 2,
            shuffleOrderApplied = true,
        )
        assertEquals(session, PlaybackSessionCodec.decode(PlaybackSessionCodec.encode(session)))
    }
}
