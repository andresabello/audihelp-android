package com.audihelp.app.data.repository

import com.audihelp.app.data.local.db.HearingTestDao
import com.audihelp.app.data.local.db.HearingTestSessionEntity
import com.audihelp.app.data.local.db.HearingTestTrialEntity
import com.audihelp.app.data.local.db.toDomain
import com.audihelp.app.domain.enums.EarSide
import com.audihelp.app.domain.enums.PatternType
import com.audihelp.app.domain.enums.SessionStatus
import com.audihelp.app.domain.enums.TrialResponse
import com.audihelp.app.domain.model.HearingTestSession
import com.audihelp.app.domain.model.HearingTestTrial
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HearingTestRepository @Inject constructor(private val dao: HearingTestDao) {

    suspend fun getSessions(): Result<List<HearingTestSession>> = runCatching {
        dao.getAllSessions().map { entity ->
            val trials = dao.getTrialsForSession(entity.id)
            entity.toDomain(trials)
        }
    }

    suspend fun startSession(earTested: EarSide, notes: String? = null): Result<HearingTestSession> = runCatching {
        val now = Instant.now().toString()
        val entity = HearingTestSessionEntity(
            id = UUID.randomUUID().toString(),
            earTested = earTested.value,
            status = SessionStatus.IN_PROGRESS.value,
            notes = notes,
            startedAt = now,
            completedAt = null,
            createdAt = now,
        )
        dao.insertSession(entity)
        entity.toDomain()
    }

    suspend fun getSession(id: String): Result<HearingTestSession> = runCatching {
        val entity = dao.getSession(id) ?: error("Session not found: $id")
        val trials = dao.getTrialsForSession(id)
        entity.toDomain(trials)
    }

    suspend fun recordTrial(
        sessionId: String,
        ear: EarSide,
        frequencyHz: Int,
        levelDbfs: Float,
        response: TrialResponse,
        patternType: PatternType = PatternType.TONE,
    ): Result<HearingTestTrial> = runCatching {
        val now = Instant.now().toString()
        val entity = HearingTestTrialEntity(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            ear = ear.value,
            frequencyHz = frequencyHz,
            levelDbfs = levelDbfs,
            patternType = patternType.value,
            response = response.value,
            createdAt = now,
        )
        dao.insertTrial(entity)
        entity.toDomain()
    }

    suspend fun completeSession(id: String): Result<HearingTestSession> = runCatching {
        val entity = dao.getSession(id) ?: error("Session not found: $id")
        val completed = entity.copy(
            status = SessionStatus.COMPLETED.value,
            completedAt = Instant.now().toString(),
        )
        dao.updateSession(completed)
        val trials = dao.getTrialsForSession(id)
        completed.toDomain(trials)
    }

    suspend fun deleteSession(id: String): Result<Unit> = runCatching {
        dao.deleteSession(id)
    }
}
