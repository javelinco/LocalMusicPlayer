package com.javelinco.localmusicplayer.ui.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupScreenGuidanceTest {
    @Test
    fun noFolderPointsBackToTheFirstStep() {
        val guidance = backupScreenGuidance(null, backupCount = 0)

        assertNull(guidance.folderPath)
        assertEquals("Choose backup folder", guidance.folderButtonLabel)
        assertEquals(
            "Choose a backup folder in step 1 before looking for backups.",
            guidance.emptyRestoreMessage,
        )
    }

    @Test
    fun configuredFolderShowsItsPathAndUsefulEmptyState() {
        val guidance = backupScreenGuidance(
            "content://provider/tree/primary%3AMusic%2C%20Please%21%20Backups",
            backupCount = 0,
        )

        assertEquals("Internal storage / Music, Please! Backups", guidance.folderPath)
        assertEquals("Change folder", guidance.folderButtonLabel)
        assertEquals(
            "No backups found in this folder. Create one above or copy a Music, Please! " +
                "backup ZIP here from your computer.",
            guidance.emptyRestoreMessage,
        )
    }

    @Test
    fun availableBackupsSuppressTheEmptyState() {
        val guidance = backupScreenGuidance(
            "content://provider/tree/primary%3ABackups",
            backupCount = 1,
        )

        assertNull(guidance.emptyRestoreMessage)
    }
}
