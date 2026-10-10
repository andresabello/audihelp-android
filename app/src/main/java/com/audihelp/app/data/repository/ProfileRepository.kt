package com.audihelp.app.data.repository

import com.audihelp.app.data.local.db.HearingTestDao
import com.audihelp.app.data.local.db.HearingProfileEntity
import com.audihelp.app.data.local.db.HearingThresholdEntity
import com.audihelp.app.data.local.db.ProfileDao
import com.audihelp.app.data.local.db.toDomain
import com.audihelp.app.domain.enums.EarSide
import com.audihelp.app.domain.enums.TrialResponse
import com.audihelp.app.domain.model.HearingProfile
import com.audihelp.app.domain.model.HearingThreshold
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val profileDao: ProfileDao,
    private val hearingTestDao: HearingTestDao,
) {

    suspend fun getProfiles(): Result<List<HearingProfile>> = runCatching {
        profileDao.getAllProfiles().map { entity ->
            val thresholds = profileDao.getThresholds(entity.id)
            entity.toDomain(thresholds)
        }
    }

    suspend fun createProfile(
        name: String,
        notes: String? = null,
        sourceSessionId: String? = null,
    ): Result<HearingProfile> = runCatching {
        val now = Instant.now().toString()
        val profileId = UUID.randomUUID().toString()
        val entity = HearingProfileEntity(
            id = profileId,
            name = name,
            notes = notes,
            isActive = false,
            sourceSessionId = sourceSessionId,
            createdAt = now,
            thresholdCount = 0,
        )
        profileDao.insertProfile(entity)

        // If sourceSessionId provided, copy completed trials as thresholds
        val copiedThresholds = if (sourceSessionId != null) {
            val trials = hearingTestDao.getTrialsForSession(sourceSessionId)
            trials.map { trial ->
                HearingThresholdEntity(
                    id = UUID.randomUUID().toString(),
                    profileId = profileId,
                    ear = trial.ear,
                    frequencyHz = trial.frequencyHz,
                    levelDbfs = trial.levelDbfs,
                    response = trial.response,
                    createdAt = now,
                )
            }.also { thresholds ->
                thresholds.forEach { profileDao.insertThreshold(it) }
                profileDao.refreshThresholdCount(profileId)
            }
        } else {
            emptyList()
        }

        val updated = profileDao.getProfile(profileId) ?: entity
        updated.toDomain(copiedThresholds)
    }

    suspend fun getProfile(id: String): Result<HearingProfile> = runCatching {
        val entity = profileDao.getProfile(id) ?: error("Profile not found: $id")
        val thresholds = profileDao.getThresholds(id)
        entity.toDomain(thresholds)
    }

    suspend fun activateProfile(id: String): Result<HearingProfile> = runCatching {
        profileDao.deactivateAll()
        profileDao.activate(id)
        val entity = profileDao.getProfile(id) ?: error("Profile not found: $id")
        val thresholds = profileDao.getThresholds(id)
        entity.toDomain(thresholds)
    }

    suspend fun deleteProfile(id: String): Result<Unit> = runCatching {
        profileDao.deleteProfile(id)
    }

    suspend fun addThreshold(
        profileId: String,
        ear: EarSide,
        frequencyHz: Int,
        levelDbfs: Float,
        response: TrialResponse,
    ): Result<HearingThreshold> = runCatching {
        val now = Instant.now().toString()
        val entity = HearingThresholdEntity(
            id = UUID.randomUUID().toString(),
            profileId = profileId,
            ear = ear.value,
            frequencyHz = frequencyHz,
            levelDbfs = levelDbfs,
            response = response.value,
            createdAt = now,
        )
        profileDao.insertThreshold(entity)
        profileDao.refreshThresholdCount(profileId)
        entity.toDomain()
    }
}
