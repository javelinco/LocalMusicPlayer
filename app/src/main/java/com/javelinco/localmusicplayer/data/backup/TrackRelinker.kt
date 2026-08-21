package com.javelinco.localmusicplayer.data.backup

import com.javelinco.localmusicplayer.core.model.TrackId
import kotlin.math.abs

data class RelinkCandidate(
    val trackId: TrackId,
    val relativePath: String?,
    val sizeBytes: Long,
    val durationMs: Long,
    val normalizedTitle: String,
    val normalizedArtist: String,
)

sealed interface RelinkResult {
    data class Matched(val trackId: TrackId) : RelinkResult
    data class Ambiguous(val trackIds: List<TrackId>) : RelinkResult
    data object Unavailable : RelinkResult
}

object TrackRelinker {
    private const val DURATION_TOLERANCE_MS = 2_000L

    fun relink(reference: PortableTrackReference, candidates: List<RelinkCandidate>): RelinkResult {
        reference.relativePath?.let { path ->
            val exact = candidates.filter { it.relativePath.equals(path, ignoreCase = true) }
            if (exact.size == 1) return RelinkResult.Matched(exact.single().trackId)
            if (exact.size > 1) return RelinkResult.Ambiguous(exact.map { it.trackId }.sortedBy { it.value })
        }

        val scored = candidates.mapNotNull { candidate ->
            var score = 0
            if (candidate.sizeBytes == reference.sizeBytes) score += 4
            if (abs(candidate.durationMs - reference.durationMs) <= DURATION_TOLERANCE_MS) score += 3
            if (candidate.normalizedTitle == reference.normalizedTitle && reference.normalizedTitle.isNotBlank()) score += 2
            if (candidate.normalizedArtist == reference.normalizedArtist && reference.normalizedArtist.isNotBlank()) score += 1
            score.takeIf { it >= 5 }?.let { candidate to it }
        }
        val bestScore = scored.maxOfOrNull { it.second } ?: return RelinkResult.Unavailable
        val best = scored.filter { it.second == bestScore }.map { it.first.trackId }.sortedBy { it.value }
        return if (best.size == 1) RelinkResult.Matched(best.single()) else RelinkResult.Ambiguous(best)
    }
}

