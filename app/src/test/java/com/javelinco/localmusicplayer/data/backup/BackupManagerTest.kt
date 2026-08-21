package com.javelinco.localmusicplayer.data.backup

import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupManagerTest {
    @Test
    fun automaticBackupIsDailyAtomicAndRotatesOnlyAutomaticFiles() = runTest {
        val storage = FakeBackupStorage()
        val clock = { Instant.parse("2026-08-20T12:00:00Z").toEpochMilli() }
        val manager = BackupManager(storage, { bundle(clock()) }, clock)
        (1..8).forEach { storage.files["LocalMusicPlayer-auto-2026081$it-000000.zip"] = validBytes(it.toLong()) }
        storage.files["LocalMusicPlayer-manual-kept.zip"] = validBytes(1)

        assertTrue(manager.createAutomaticIfDue())
        assertFalse(manager.createAutomaticIfDue())

        assertEquals(7, storage.files.keys.count { "-auto-" in it })
        assertTrue("LocalMusicPlayer-manual-kept.zip" in storage.files)
        assertTrue(storage.files.keys.none { it.endsWith(".tmp") })
    }

    @Test
    fun invalidRestoreNeverMutatesAndSafetyBackupPrecedesRestore() = runTest {
        val storage = FakeBackupStorage().apply { files["bad.zip"] = byteArrayOf(1, 2, 3) }
        val events = mutableListOf<String>()
        val manager = BackupManager(
            storage = storage,
            snapshot = { events += "snapshot"; bundle(10) },
            nowEpochMs = { 10 },
            restore = { events += "restore" },
        )

        val failure = runCatching { manager.restore("bad.zip") }.exceptionOrNull()
        assertTrue(failure is InvalidBackupException)
        assertTrue(events.isEmpty())

        storage.files["good.zip"] = validBytes(20)
        manager.restore("good.zip")
        assertEquals(listOf("snapshot", "restore"), events)
        assertTrue(storage.files.keys.any { "-safety-" in it })
    }

    private fun bundle(now: Long) = BackupBundle(
        BackupManifest(createdAtEpochMs = now, appVersion = "test"),
        BackupUserData(settings = mapOf("theme" to "dark")),
    )

    private fun validBytes(now: Long) = BackupCodec.encode(bundle(now))
}

private class FakeBackupStorage : BackupStorage {
    val files = linkedMapOf<String, ByteArray>()

    override suspend fun listNames(): List<String> = files.keys.toList()
    override suspend fun read(name: String): ByteArray = files.getValue(name)
    override suspend fun write(name: String, bytes: ByteArray) { files[name] = bytes }
    override suspend fun delete(name: String) { files.remove(name) }
    override suspend fun promote(temporaryName: String, finalName: String) {
        files[finalName] = files.remove(temporaryName) ?: error("missing temporary backup")
    }
}
