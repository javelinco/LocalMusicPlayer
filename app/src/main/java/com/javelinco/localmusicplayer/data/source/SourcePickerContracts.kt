package com.javelinco.localmusicplayer.data.source

import androidx.activity.result.contract.ActivityResultContracts
import com.javelinco.localmusicplayer.core.model.SourceId

object SourcePickerContracts {
    const val MP3_MIME_TYPE = "audio/mpeg"

    val chooseFolder = ActivityResultContracts.OpenDocumentTree()
    val chooseFiles = ActivityResultContracts.OpenMultipleDocuments()
    val requestPermission = ActivityResultContracts.RequestPermission()
}

enum class AcquisitionCommand {
    OPEN_FOLDER,
    OPEN_MP3_FILES,
    SHOW_DEVICE_PERMISSION_EXPLANATION,
    REQUEST_MEDIA_AUDIO_PERMISSION,
}

class SourceAcquisitionCoordinator {
    var devicePermissionWasRequested: Boolean = false
        private set

    fun chooseFolder() = AcquisitionCommand.OPEN_FOLDER

    fun chooseFiles() = AcquisitionCommand.OPEN_MP3_FILES

    fun findAllDeviceMusic() = AcquisitionCommand.SHOW_DEVICE_PERMISSION_EXPLANATION

    fun confirmDeviceMusicExplanation(): AcquisitionCommand {
        devicePermissionWasRequested = true
        return AcquisitionCommand.REQUEST_MEDIA_AUDIO_PERMISSION
    }
}

data class SelectedDocument(
    val uri: String,
    val displayName: String,
)

class SourceSelectionHandler(
    private val registry: SourceRegistry,
    private val permissionStore: SafPermissionStore,
    private val idFactory: () -> SourceId,
) {
    suspend fun registerFolder(uri: String, label: String) {
        permissionStore.takeReadPermission(uri)
        registry.add(SafTreeSource(idFactory(), uri, label))
    }

    suspend fun registerDocuments(documents: List<SelectedDocument>) {
        documents.distinctBy(SelectedDocument::uri).forEach { document ->
            permissionStore.takeReadPermission(document.uri)
            registry.add(
                SafDocumentSource(idFactory(), document.uri, document.displayName),
            )
        }
    }
}
