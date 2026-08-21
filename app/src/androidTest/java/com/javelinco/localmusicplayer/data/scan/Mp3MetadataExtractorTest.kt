package com.javelinco.localmusicplayer.data.scan

import android.Manifest
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.javelinco.localmusicplayer.core.model.SourceId
import com.javelinco.localmusicplayer.data.source.SourceEntry
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Mp3MetadataExtractorTest {
    @Test
    fun extractsADeviceMp3AndRejectsCorruptInput() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.grantRuntimePermission(
            context.packageName,
            Manifest.permission.READ_MEDIA_AUDIO,
        )
        val entry = firstDeviceMp3(context)
        assumeTrue("No MP3 was available on the connected device", entry != null)
        val extractor = AndroidMp3MetadataExtractor(context.contentResolver)

        val metadata = extractor.extract(entry!!)

        assertTrue(metadata.durationMs == null || metadata.durationMs >= 0)

        val corrupt = File(context.cacheDir, "corrupt.mp3").apply {
            writeBytes(byteArrayOf(0x49, 0x44, 0x33, 0x00))
        }
        val corruptEntry = entry.copy(
            stableId = "corrupt",
            contentUri = Uri.fromFile(corrupt).toString(),
            displayName = "corrupt.mp3",
        )
        assertTrue(runCatching { extractor.extract(corruptEntry) }.isFailure)
    }

    private fun firstDeviceMp3(context: android.content.Context): SourceEntry? {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_MODIFIED,
        )
        return context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Audio.Media.MIME_TYPE} = ? AND ${MediaStore.Audio.Media.IS_MUSIC} != 0",
            arrayOf("audio/mpeg"),
            "${MediaStore.Audio.Media._ID} ASC",
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val id = cursor.getLong(0)
            SourceEntry(
                sourceId = SourceId("device"),
                stableId = id.toString(),
                contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString(),
                displayName = cursor.getString(1),
                mimeType = "audio/mpeg",
                sizeBytes = cursor.getLong(2),
                modifiedAtEpochMs = cursor.getLong(3) * 1_000,
            )
        }
    }
}
