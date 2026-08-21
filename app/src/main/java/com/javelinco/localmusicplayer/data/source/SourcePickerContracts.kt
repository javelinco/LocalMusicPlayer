package com.javelinco.localmusicplayer.data.source

import androidx.activity.result.contract.ActivityResultContracts
import com.javelinco.localmusicplayer.core.model.SourceId

object SourcePickerContracts {
    const val MP3_MIME_TYPE = "audio/mpeg"

    val chooseFolder = ActivityResultContracts.OpenDocumentTree()
    val requestPermission = ActivityResultContracts.RequestPermission()
}

enum class AcquisitionCommand {
    OPEN_FOLDER,
    SHOW_DEVICE_PERMISSION_EXPLANATION,
    REQUEST_MEDIA_AUDIO_PERMISSION,
}

class SourceAcquisitionCoordinator {
    var devicePermissionWasRequested: Boolean = false
        private set

    fun chooseFolder() = AcquisitionCommand.OPEN_FOLDER

    fun findAllDeviceMusic() = AcquisitionCommand.SHOW_DEVICE_PERMISSION_EXPLANATION

    fun confirmDeviceMusicExplanation(): AcquisitionCommand {
        devicePermissionWasRequested = true
        return AcquisitionCommand.REQUEST_MEDIA_AUDIO_PERMISSION
    }
}

class SourceSelectionHandler(
    private val registry: SourceRegistry,
    private val permissionStore: SafPermissionStore,
    private val idFactory: () -> SourceId,
) {
    suspend fun registerFolder(uri: String, label: String) {
        permissionStore.takeReadPermission(uri)
        registry.add(SafTreeSource(idFactory(), uri, label))
    }
}
