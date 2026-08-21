package com.javelinco.localmusicplayer.data.source

import com.javelinco.localmusicplayer.core.model.SourceId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface SourceRegistry {
    fun observeSources(): Flow<List<MusicSource>>

    suspend fun add(source: MusicSource)

    suspend fun remove(id: SourceId)

    suspend fun setAvailability(id: SourceId, available: Boolean)

    suspend fun setAvailability(kind: SourceKind, available: Boolean)
}

class InMemorySourceRegistry(
    initialSources: List<MusicSource> = emptyList(),
) : SourceRegistry {
    private val mutex = Mutex()
    private val sources = MutableStateFlow(initialSources.deduplicated())

    override fun observeSources(): Flow<List<MusicSource>> = sources.asStateFlow()

    override suspend fun add(source: MusicSource) {
        mutex.withLock {
            val alreadyRegistered = sources.value.any {
                it.id == source.id || (it.kind == source.kind && it.identity == source.identity)
            }
            if (!alreadyRegistered) {
                sources.value = sources.value + source
            }
        }
    }

    override suspend fun remove(id: SourceId) {
        mutex.withLock {
            sources.value = sources.value.filterNot { it.id == id }
        }
    }

    override suspend fun setAvailability(id: SourceId, available: Boolean) {
        mutex.withLock {
            sources.value = sources.value.map { source ->
                if (source.id == id) source.withAvailability(available) else source
            }
        }
    }

    override suspend fun setAvailability(kind: SourceKind, available: Boolean) {
        mutex.withLock {
            sources.value = sources.value.map { source ->
                if (source.kind == kind) source.withAvailability(available) else source
            }
        }
    }
}

private fun List<MusicSource>.deduplicated(): List<MusicSource> = buildList {
    this@deduplicated.forEach { candidate ->
        val alreadyRegistered = any {
            it.id == candidate.id || (it.kind == candidate.kind && it.identity == candidate.identity)
        }
        if (!alreadyRegistered) add(candidate)
    }
}
