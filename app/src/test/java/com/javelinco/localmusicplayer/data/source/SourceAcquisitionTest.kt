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
    fun folderActionNeverRequestsDeviceWidePermission() {
        val coordinator = SourceAcquisitionCoordinator()

        assertEquals(AcquisitionCommand.OPEN_FOLDER, coordinator.chooseFolder())
        assertFalse(coordinator.devicePermissionWasRequested)
    }

    @Test
    fun individualFileAcquisitionIsNotExposed() {
        assertFalse(AcquisitionCommand.entries.any { it.name == "OPEN_MP3_FILES" })
        assertFalse(SourceAcquisitionCoordinator::class.java.methods.any { it.name == "chooseFiles" })
        assertFalse(SourceSelectionHandler::class.java.methods.any { it.name == "registerDocuments" })
    }

    @Test
    fun selectedFoldersAppendDistinctTreesAndTakeReadGrants() = runTest {
        val registry = InMemorySourceRegistry()
        val permissions = RecordingSafPermissionStore()
        var nextId = 0
        val handler = SourceSelectionHandler(registry, permissions) { SourceId("folder-${nextId++}") }

        handler.registerFolder("content://tree/music", "Music")
        handler.registerFolder("content://tree/concerts", "Concerts")
        handler.registerFolder("content://tree/music", "Music again")

        assertEquals(
            listOf("content://tree/music", "content://tree/concerts"),
            registry.observeSources().first().map { it.identity },
        )
        assertEquals(setOf("content://tree/music", "content://tree/concerts"), permissions.taken)
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
