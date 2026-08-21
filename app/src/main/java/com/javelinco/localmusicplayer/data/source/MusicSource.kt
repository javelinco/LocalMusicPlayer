package com.javelinco.localmusicplayer.data.source

import com.javelinco.localmusicplayer.core.model.SourceId

enum class SourceKind {
    SAF_TREE,
    SAF_DOCUMENT,
    MEDIA_STORE,
}

sealed interface MusicSource {
    val id: SourceId
    val label: String
    val available: Boolean
    val kind: SourceKind

    val identity: String
}

data class SafTreeSource(
    override val id: SourceId,
    val treeUri: String,
    override val label: String,
    override val available: Boolean = true,
) : MusicSource {
    override val kind = SourceKind.SAF_TREE
    override val identity = treeUri
}

data class SafDocumentSource(
    override val id: SourceId,
    val documentUri: String,
    val displayName: String,
    override val available: Boolean = true,
) : MusicSource {
    override val label = displayName
    override val kind = SourceKind.SAF_DOCUMENT
    override val identity = documentUri
}

data class MediaStoreSource(
    override val id: SourceId,
    override val label: String,
    override val available: Boolean = true,
) : MusicSource {
    override val kind = SourceKind.MEDIA_STORE
    override val identity = "device-audio"
}

internal fun MusicSource.withAvailability(available: Boolean): MusicSource = when (this) {
    is SafTreeSource -> copy(available = available)
    is SafDocumentSource -> copy(available = available)
    is MediaStoreSource -> copy(available = available)
}
