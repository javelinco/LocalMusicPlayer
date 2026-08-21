package com.javelinco.localmusicplayer.data.source

import com.javelinco.localmusicplayer.core.model.SourceId
import com.javelinco.localmusicplayer.data.db.LibraryDao
import com.javelinco.localmusicplayer.data.db.SourceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomSourceRegistry(
    private val libraryDao: LibraryDao,
) : SourceRegistry {
    override fun observeSources(): Flow<List<MusicSource>> =
        libraryDao.observeSources().map { sources -> sources.map(SourceEntity::toMusicSource) }

    override suspend fun add(source: MusicSource) {
        libraryDao.insertSource(source.toEntity())
    }

    override suspend fun remove(id: SourceId) {
        libraryDao.deleteSource(id.value)
    }

    override suspend fun setAvailability(id: SourceId, available: Boolean) {
        libraryDao.setSourceAvailability(id.value, available)
    }

    override suspend fun setAvailability(kind: SourceKind, available: Boolean) {
        libraryDao.setSourceKindAvailability(kind.name, available)
    }
}

private fun MusicSource.toEntity() = SourceEntity(
    sourceId = id.value,
    kind = kind.name,
    location = identity,
    label = label,
    available = available,
)

private fun SourceEntity.toMusicSource(): MusicSource = when (SourceKind.valueOf(kind)) {
    SourceKind.SAF_TREE -> SafTreeSource(SourceId(sourceId), location, label, available)
    SourceKind.SAF_DOCUMENT -> SafDocumentSource(SourceId(sourceId), location, label, available)
    SourceKind.MEDIA_STORE -> MediaStoreSource(SourceId(sourceId), label, available)
}
