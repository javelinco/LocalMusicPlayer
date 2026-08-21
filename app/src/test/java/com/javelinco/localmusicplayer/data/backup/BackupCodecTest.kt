package com.javelinco.localmusicplayer.data.backup

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {
    @Test
    fun roundTripContainsOnlyManifestAndUserData() {
        val backup = BackupBundle(
            BackupManifest(createdAtEpochMs = 10, appVersion = "0.1.0"),
            BackupUserData(playlists = listOf(BackupPlaylist("p", "Mix", listOf("a", "a")))),
        )

        val encoded = BackupCodec.encode(backup)
        val entries = BackupCodec.entryNames(encoded)

        assertEquals(setOf("manifest.json", "user-data.json"), entries)
        assertFalse(entries.any { it.endsWith(".mp3") || "artwork" in it || "tracks" in it })
        assertEquals(backup, BackupCodec.decode(encoded))
    }

    @Test
    fun rejectsNewerSchemaAndPathTraversal() {
        val newer = BackupBundle(
            BackupManifest(schemaVersion = 99, createdAtEpochMs = 1, appVersion = "future"),
            BackupUserData(),
        )
        assertThrows(InvalidBackupException::class.java) { BackupCodec.decode(BackupCodec.encode(newer)) }

        val malicious = zipOf("../escape" to "bad")
        assertThrows(InvalidBackupException::class.java) { BackupCodec.decode(malicious) }
    }

    @Test
    fun automaticRetentionLeavesManualFilesUntouched() {
        val names = (1..9).map { "LocalMusicPlayer-auto-$it.zip" } + listOf("LocalMusicPlayer-manual-kept.zip")
        val deletion = BackupRetention.filesToDelete(names, keepAutomatic = 7)

        assertEquals(2, deletion.size)
        assertTrue(deletion.all { "-auto-" in it })
    }

    private fun zipOf(vararg entries: Pair<String, String>) = ByteArrayOutputStream().use { output ->
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, value) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(value.toByteArray())
                zip.closeEntry()
            }
        }
        output.toByteArray()
    }
}
