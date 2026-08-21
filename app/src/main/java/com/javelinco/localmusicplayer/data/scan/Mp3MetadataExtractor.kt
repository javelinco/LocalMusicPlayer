package com.javelinco.localmusicplayer.data.scan

import android.content.ContentResolver
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.javelinco.localmusicplayer.data.source.SourceEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface Mp3MetadataExtractor {
    suspend fun extract(entry: SourceEntry): RawMp3Metadata
    suspend fun extractArtwork(entry: SourceEntry): ByteArray?
}

class AndroidMp3MetadataExtractor(
    private val contentResolver: ContentResolver,
) : Mp3MetadataExtractor {
    override suspend fun extract(entry: SourceEntry): RawMp3Metadata = withRetriever(entry) { retriever ->
        RawMp3Metadata(
            title = retriever.value(MediaMetadataRetriever.METADATA_KEY_TITLE),
            artist = retriever.value(MediaMetadataRetriever.METADATA_KEY_ARTIST),
            album = retriever.value(MediaMetadataRetriever.METADATA_KEY_ALBUM),
            albumArtist = retriever.value(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST),
            genre = retriever.value(MediaMetadataRetriever.METADATA_KEY_GENRE),
            trackNumber = retriever.value(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER),
            discNumber = retriever.value(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER),
            durationMs = retriever.value(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull(),
            compilation = retriever.value(MediaMetadataRetriever.METADATA_KEY_COMPILATION) in setOf("1", "true", "yes"),
        )
    }

    override suspend fun extractArtwork(entry: SourceEntry): ByteArray? =
        withRetriever(entry) { it.embeddedPicture }

    private suspend fun <T> withRetriever(
        entry: SourceEntry,
        block: (MediaMetadataRetriever) -> T,
    ): T = withContext(Dispatchers.IO) {
        contentResolver.openAssetFileDescriptor(Uri.parse(entry.contentUri), "r")?.use { descriptor ->
            MediaMetadataRetriever().use { retriever ->
                if (descriptor.declaredLength >= 0) {
                    retriever.setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.declaredLength)
                } else {
                    retriever.setDataSource(descriptor.fileDescriptor)
                }
                block(retriever)
            }
        } ?: error("Cannot open ${entry.contentUri}")
    }

    private fun MediaMetadataRetriever.value(key: Int): String? = extractMetadata(key)?.trim()?.ifEmpty { null }
}
