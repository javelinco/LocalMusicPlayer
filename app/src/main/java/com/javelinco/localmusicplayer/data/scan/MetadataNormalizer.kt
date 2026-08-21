package com.javelinco.localmusicplayer.data.scan

import com.javelinco.localmusicplayer.data.source.SourceEntry
import java.text.Normalizer
import java.util.Locale

data class RawMp3Metadata(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val albumArtist: String? = null,
    val genre: String? = null,
    val trackNumber: String? = null,
    val discNumber: String? = null,
    val durationMs: Long? = null,
    val compilation: Boolean = false,
)

data class NormalizedTrackMetadata(
    val title: String,
    val artist: String?,
    val albumTitle: String?,
    val albumArtist: String?,
    val genre: String?,
    val normalizedTitle: String,
    val normalizedArtist: String,
    val normalizedAlbumTitle: String,
    val normalizedAlbumArtist: String,
    val normalizedGenre: String,
    val trackNumber: Int?,
    val discNumber: Int?,
    val durationMs: Long,
)

object MetadataNormalizer {
    fun normalize(raw: RawMp3Metadata, entry: SourceEntry): NormalizedTrackMetadata {
        val title = raw.title.cleaned() ?: entry.displayName.removeSuffixIgnoreCase(".mp3").ifBlank { entry.displayName }
        val artist = raw.artist.cleaned()
        val album = raw.album.cleaned()
        val albumArtist = raw.albumArtist.cleaned()
            ?: if (raw.compilation) "Various Artists" else artist
        val genre = raw.genre.cleaned()
        return NormalizedTrackMetadata(
            title = title,
            artist = artist,
            albumTitle = album,
            albumArtist = albumArtist,
            genre = genre,
            normalizedTitle = title.searchKey(),
            normalizedArtist = artist.searchKey(),
            normalizedAlbumTitle = album.searchKey(),
            normalizedAlbumArtist = albumArtist.searchKey(),
            normalizedGenre = genre.searchKey(),
            trackNumber = raw.trackNumber.leadingNumber(),
            discNumber = raw.discNumber.leadingNumber(),
            durationMs = raw.durationMs?.coerceAtLeast(0) ?: 0,
        )
    }
}

private fun String?.cleaned(): String? = this?.trim()?.takeIf(String::isNotEmpty)

private fun String?.searchKey(): String = this?.let {
    Normalizer.normalize(it, Normalizer.Form.NFKD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)
        .trim()
        .replace(Regex("\\s+"), " ")
}.orEmpty()

private fun String?.leadingNumber(): Int? = this?.trim()?.substringBefore('/')?.toIntOrNull()

private fun String.removeSuffixIgnoreCase(suffix: String): String =
    if (endsWith(suffix, ignoreCase = true)) dropLast(suffix.length) else this
