package com.javelinco.localmusicplayer.data.source

import com.javelinco.localmusicplayer.core.model.SourceId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SourceAcquisitionTest {
    @Test
    fun deviceDiscoveryRequiresExplanationBeforePermissionRequest() {
        val coordinator = SourceAcquisitionCoordinator()

        assertEquals(AcquisitionCommand.SHOW_DEVICE_PERMISSION_EXPLANATION, coordinator.findAllDeviceMusic())
        assertEquals(AcquisitionCommand.REQUEST_MEDIA_AUDIO_PERMISSION, coordinator.confirmDeviceMusicExplanation())
    }

    @Test
    fun folderAndFileActionsNeverRequestDeviceWidePermission() {
        val coordinator = SourceAcquisitionCoordinator()

        assertEquals(AcquisitionCommand.OPEN_FOLDER, coordinator.chooseFolder())
        assertEquals(AcquisitionCommand.OPEN_MP3_FILES, coordinator.chooseFiles())
        assertFalse(coordinator.devicePermissionWasRequested)
    }

    @Test
    fun selectedDocumentsRegisterOnlyReturnedUrisAndTakeReadGrants() = runTest {
        val registry = InMemorySourceRegistry()
        val permissions = RecordingSafPermissionStore()
        var nextId = 0
        val handler = SourceSelectionHandler(registry, permissions) { SourceId("id-${nextId++}") }

        handler.registerDocuments(
            listOf(
                SelectedDocument("content://picked/one", "One.mp3"),
                SelectedDocument("content://picked/two", "Two.mp3"),
                SelectedDocument("content://picked/one", "One again.mp3"),
            ),
        )

        assertEquals(
            listOf("content://picked/one", "content://picked/two"),
            registry.observeSources().first().map { it.identity },
        )
        assertEquals(setOf("content://picked/one", "content://picked/two"), permissions.taken)
    }

    @Test
    fun selectedFolderTakesOnlyPersistedReadAccess() = runTest {
        val registry = InMemorySourceRegistry()
        val permissions = RecordingSafPermissionStore()
        val handler = SourceSelectionHandler(registry, permissions) { SourceId("folder") }

        handler.registerFolder("content://tree/music", "Music")

        assertEquals(setOf("content://tree/music"), permissions.taken)
        assertEquals(emptySet<String>(), permissions.released)
    }

    private class RecordingSafPermissionStore : SafPermissionStore {
        val taken = linkedSetOf<String>()
        val released = linkedSetOf<String>()

        override fun takeReadPermission(uri: String) {
            taken += uri
        }

        override fun releaseReadPermission(uri: String) {
            released += uri
        }

        override fun hasReadPermission(uri: String) = uri in taken
    }
}
