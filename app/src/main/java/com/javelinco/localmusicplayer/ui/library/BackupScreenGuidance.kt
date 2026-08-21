package com.javelinco.localmusicplayer.ui.library

internal data class BackupScreenGuidance(
    val folderPath: String?,
    val folderButtonLabel: String,
    val emptyRestoreMessage: String?,
)

internal fun backupScreenGuidance(
    selectedFolder: String?,
    backupCount: Int,
): BackupScreenGuidance = BackupScreenGuidance(
    folderPath = selectedFolder?.let(::backupFolderDisplayPath),
    folderButtonLabel = if (selectedFolder == null) "Choose backup folder" else "Change folder",
    emptyRestoreMessage = when {
        backupCount > 0 -> null
        selectedFolder == null -> "Choose a backup folder in step 1 before looking for backups."
        else -> "No backups found in this folder. Create one above or copy a Music, Please! " +
            "backup ZIP here from your computer."
    },
)
