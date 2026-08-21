package com.javelinco.localmusicplayer.playback.queue

import com.javelinco.localmusicplayer.data.db.QueueSessionEntity
import com.javelinco.localmusicplayer.data.db.UserDataDao
import java.nio.charset.StandardCharsets
import java.util.Base64

interface QueueSessionStore {
    suspend fun load(): QueueSessionDto?
    suspend fun save(session: QueueSessionDto)
    suspend fun clear()
}

class RoomQueueSessionStore(
    private val userDataDao: UserDataDao,
    private val clock: () -> Long = System::currentTimeMillis,
) : QueueSessionStore {
    override suspend fun load(): QueueSessionDto? = userDataDao.queueSession()?.let { entity ->
        QueueSessionCodec.decode(entity.queueJson).copy(positionMs = entity.positionMs)
    }

    override suspend fun save(session: QueueSessionDto) {
        userDataDao.saveQueueSession(
            QueueSessionEntity(
                queueJson = QueueSessionCodec.encode(session),
                currentIndex = session.sourceOrder.indexOf(session.current),
                positionMs = session.positionMs,
                updatedAtEpochMs = clock(),
            ),
        )
    }

    override suspend fun clear() = userDataDao.clearQueueSession()
}

object QueueSessionCodec {
    fun encode(session: QueueSessionDto): String = listOf(
        "1",
        session.sourceOrder.encoded(),
        session.cycleOrder.encoded(),
        session.current.orEmpty().encodedValue(),
        session.behind.encoded(),
        session.history.encoded(),
        session.upcoming.encoded(),
        session.shuffleEnabled.toString(),
        session.repeatMode,
        session.unavailable.toList().encoded(),
        session.positionMs.toString(),
    ).joinToString("\n")

    fun decode(value: String): QueueSessionDto {
        val fields = value.split('\n')
        require(fields.size == 11 && fields[0] == "1") { "Unsupported queue session" }
        return QueueSessionDto(
            sourceOrder = fields[1].decodedList(),
            cycleOrder = fields[2].decodedList(),
            current = fields[3].decodedValue().ifEmpty { null },
            behind = fields[4].decodedList(),
            history = fields[5].decodedList(),
            upcoming = fields[6].decodedList(),
            shuffleEnabled = fields[7].toBooleanStrict(),
            repeatMode = fields[8],
            unavailable = fields[9].decodedList().toSet(),
            positionMs = fields[10].toLong(),
        )
    }

    private fun List<String>.encoded() = joinToString(",") { it.encodedValue() }
    private fun String.decodedList() = if (isEmpty()) emptyList() else split(',').map { it.decodedValue() }
    private fun String.encodedValue() = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(toByteArray(StandardCharsets.UTF_8))
    private fun String.decodedValue() = String(Base64.getUrlDecoder().decode(this), StandardCharsets.UTF_8)
}
