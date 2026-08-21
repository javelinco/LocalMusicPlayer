package com.javelinco.localmusicplayer.data.source

import android.content.ContentResolver
import android.content.ContentUris
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class MediaStoreReader(
    private val contentResolver: ContentResolver,
) : SourceReader {
    override fun enumerate(source: MusicSource, checkpoint: String?): Flow<SourceEntry> = flow {
        require(source is MediaStoreSource)
        val lastId = checkpoint?.toLongOrNull()
        val selectionParts = mutableListOf(
            "${MediaStore.Audio.Media.MIME_TYPE} = ?",
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
        )
        val arguments = mutableListOf(SourcePickerContracts.MP3_MIME_TYPE)
        if (lastId != null) {
            selectionParts += "${MediaStore.Audio.Media._ID} > ?"
            arguments += lastId.toString()
        }
        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            PROJECTION,
            selectionParts.joinToString(" AND "),
            arguments.toTypedArray(),
            "${MediaStore.Audio.Media._ID} ASC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val modifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            while (cursor.moveToNext()) {
                currentCoroutineContext().ensureActive()
                val id = cursor.getLong(idColumn)
                emit(
                    SourceEntry(
                        sourceId = source.id,
                        stableId = id.toString(),
                        contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString(),
                        displayName = cursor.getString(nameColumn),
                        mimeType = SourcePickerContracts.MP3_MIME_TYPE,
                        sizeBytes = cursor.nullableLong(sizeColumn),
                        modifiedAtEpochMs = cursor.nullableLong(modifiedColumn)?.times(1_000),
                    ),
                )
            }
        }
    }.flowOn(Dispatchers.IO)

    private companion object {
        val PROJECTION = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_MODIFIED,
        )
    }
}
