package com.javelinco.localmusicplayer.ui.library

import org.junit.Assert.assertEquals
import org.junit.Test

class BackupLocationFormatterTest {
    @Test
    fun internalStoragePathIsReadable() {
        assertEquals(
            "Internal storage / Music, Please! Backups",
            backupFolderDisplayPath(
                "content://com.android.externalstorage.documents/tree/" +
                    "primary%3AMusic%2C%20Please%21%20Backups",
            ),
        )
    }

    @Test
    fun removableStoragePathIsReadable() {
        assertEquals(
            "Storage 1234-5678 / Backups / Phone",
            backupFolderDisplayPath(
                "content://com.android.externalstorage.documents/tree/" +
                    "1234-5678%3ABackups%2FPhone",
            ),
        )
    }

    @Test
    fun knownAndUnknownProvidersStillShowALocation() {
        assertEquals("Downloads", backupFolderDisplayPath("content://provider/tree/downloads"))
        assertEquals("Documents", backupFolderDisplayPath("content://provider/tree/home"))
        assertEquals("content://provider", backupFolderDisplayPath("content://provider"))
    }
}
