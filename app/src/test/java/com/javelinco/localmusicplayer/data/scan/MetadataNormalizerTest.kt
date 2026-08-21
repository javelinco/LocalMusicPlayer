package com.javelinco.localmusicplayer.data.scan

import com.javelinco.localmusicplayer.core.model.SourceId
import com.javelinco.localmusicplayer.data.source.SourceEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MetadataNormalizerTest {
    private val entry = SourceEntry(
        SourceId("source"), "stable", "content://song", "Fallback Name.mp3", "audio/mpeg", 1, 2,
    )

    @Test
    fun blankTagsFallBackToFilenameWithoutInventingArtistOrAlbum() {
        val normalized = MetadataNormalizer.normalize(RawMp3Metadata(), entry)

        assertEquals("Fallback Name", normalized.title)
        assertEquals("fallback name", normalized.normalizedTitle)
        assertNull(normalized.artist)
        assertEquals("", normalized.normalizedArtist)
        assertEquals("", normalized.normalizedAlbumTitle)
    }

    @Test
    fun albumArtistFallsBackToTrackArtistUnlessCompilation() {
        val normal = MetadataNormalizer.normalize(
            RawMp3Metadata(artist = "Solo Artist", album = "Record"),
            entry,
        )
        val compilation = MetadataNormalizer.normalize(
            RawMp3Metadata(artist = "Guest", album = "Collection", compilation = true),
            entry,
        )

        assertEquals("Solo Artist", normal.albumArtist)
        assertEquals("Various Artists", compilation.albumArtist)
        assertEquals("various artists", compilation.normalizedAlbumArtist)
    }

    @Test
    fun fractionTrackAndDiscNumbersKeepLeadingNumber() {
        val normalized = MetadataNormalizer.normalize(
            RawMp3Metadata(trackNumber = "07/12", discNumber = "2/3"),
            entry,
        )

        assertEquals(7, normalized.trackNumber)
        assertEquals(2, normalized.discNumber)
    }
}
