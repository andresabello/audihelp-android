package com.audihelp.app.data.repository

import com.audihelp.app.data.local.db.LiveSessionDao
import com.audihelp.app.data.local.db.LiveSessionEntity
import com.audihelp.app.data.local.db.toDomain
import com.audihelp.app.domain.model.LiveSession
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveSessionRepository @Inject constructor(private val dao: LiveSessionDao) {

    suspend fun getSessions(): Result<List<LiveSession>> = runCatching {
        dao.getAll().map { it.toDomain() }
    }

    suspend fun startSession(presetId: String?, profileId: String?): Result<LiveSession> = runCatching {
        val now = Instant.now().toString()
        val entity = LiveSessionEntity(
            id = UUID.randomUUID().toString(),
            presetId = presetId,
            profileId = profileId,
            notes = null,
            startedAt = now,
            endedAt = null,
            createdAt = now,
        )
        dao.insert(entity)
        entity.toDomain()
    }

    suspend fun endSession(id: String): Result<LiveSession> = runCatching {
        val entity = dao.getById(id) ?: error("Live session not found: $id")
        val updated = entity.copy(endedAt = Instant.now().toString())
        dao.update(updated)
        updated.toDomain()
    }

    suspend fun deleteSession(id: String): Result<Unit> = runCatching {
        dao.delete(id)
    }
}
