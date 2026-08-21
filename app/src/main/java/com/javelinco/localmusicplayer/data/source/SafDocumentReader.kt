package com.javelinco.localmusicplayer.data.source

import android.content.ContentResolver
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class SafDocumentReader(
    private val contentResolver: ContentResolver,
) : SourceReader {
    override fun enumerate(source: MusicSource, checkpoint: String?): Flow<SourceEntry> = flow {
        require(source is SafDocumentSource)
        if (checkpoint == source.documentUri) return@flow
        val uri = android.net.Uri.parse(source.documentUri)
        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                if (name.endsWith(".mp3", true) || contentResolver.getType(uri) == SourcePickerContracts.MP3_MIME_TYPE) {
                    emit(
                        SourceEntry(
                            sourceId = source.id,
                            stableId = source.documentUri,
                            contentUri = source.documentUri,
                            displayName = name,
                            mimeType = contentResolver.getType(uri),
                            sizeBytes = cursor.nullableLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE)),
                            modifiedAtEpochMs = null,
                        ),
                    )
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}
