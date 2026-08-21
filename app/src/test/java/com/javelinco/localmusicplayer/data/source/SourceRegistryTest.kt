package com.javelinco.localmusicplayer.data.source

import com.javelinco.localmusicplayer.core.model.SourceId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceRegistryTest {
    @Test
    fun samePersistedUriDoesNotCreateTwoFolderSources() = runTest {
        val registry = InMemorySourceRegistry()

        registry.add(SafTreeSource(SourceId("a"), "content://tree/music", "Music"))
        registry.add(SafTreeSource(SourceId("b"), "content://tree/music", "Music again"))

        val sources = registry.observeSources().first()
        assertEquals(1, sources.size)
        assertEquals("Music", sources.single().label)
    }

    @Test
    fun samePersistedUriDoesNotCreateTwoDocumentSources() = runTest {
        val registry = InMemorySourceRegistry()

        registry.add(SafDocumentSource(SourceId("a"), "content://file/song", "Song.mp3"))
        registry.add(SafDocumentSource(SourceId("b"), "content://file/song", "Renamed.mp3"))

        assertEquals(1, registry.observeSources().first().size)
    }

    @Test
    fun mediaStorePermissionLossDoesNotDisableSafSources() = runTest {
        val tree = SafTreeSource(SourceId("tree"), "content://tree/music", "Music")
        val document = SafDocumentSource(SourceId("file"), "content://file/song", "Song.mp3")
        val mediaStore = MediaStoreSource(SourceId("device"), "All device music")
        val registry = InMemorySourceRegistry(listOf(tree, document, mediaStore))

        registry.setAvailability(SourceKind.MEDIA_STORE, false)

        val sources = registry.observeSources().first()
        assertTrue(sources.filter { it.kind != SourceKind.MEDIA_STORE }.all { it.available })
        assertFalse(sources.single { it.kind == SourceKind.MEDIA_STORE }.available)
    }

    @Test
    fun removingSourcePublishesNewSnapshot() = runTest {
        val registry = InMemorySourceRegistry(
            listOf(SafTreeSource(SourceId("tree"), "content://tree/music", "Music")),
        )

        registry.remove(SourceId("tree"))

        assertTrue(registry.observeSources().first().isEmpty())
    }

    @Test
    fun mediaStoreIsASingleLogicalSource() = runTest {
        val registry = InMemorySourceRegistry()

        registry.add(MediaStoreSource(SourceId("first"), "Device music"))
        registry.add(MediaStoreSource(SourceId("second"), "All music"))

        assertEquals(1, registry.observeSources().first().size)
    }
}
