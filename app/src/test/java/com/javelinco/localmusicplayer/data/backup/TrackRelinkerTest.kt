package com.javelinco.localmusicplayer.data.backup

import com.javelinco.localmusicplayer.core.model.TrackId
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackRelinkerTest {
    @Test
    fun exactRelativePathWinsAcrossChangedRootUri() {
        val reference = PortableTrackReference("Music/A/song.mp3", 10, 20, "song", "artist")
        val candidates = listOf(
            RelinkCandidate(TrackId("match"), "Music/A/song.mp3", 10, 20, "song", "artist"),
            RelinkCandidate(TrackId("other"), "Else/song.mp3", 10, 20, "song", "artist"),
        )

        assertEquals(RelinkResult.Matched(TrackId("match")), TrackRelinker.relink(reference, candidates))
    }

    @Test
    fun equallyRankedCandidatesStayAmbiguous() {
        val reference = PortableTrackReference(null, 10, 20, "song", "artist")
        val candidates = listOf(
            RelinkCandidate(TrackId("a"), null, 10, 20, "song", "artist"),
            RelinkCandidate(TrackId("b"), null, 10, 20, "song", "artist"),
        )

        assertEquals(
            RelinkResult.Ambiguous(listOf(TrackId("a"), TrackId("b"))),
            TrackRelinker.relink(reference, candidates),
        )
    }
}
