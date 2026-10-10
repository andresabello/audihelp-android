package com.audihelp.app.data.local.db

import com.audihelp.app.data.dto.dsp.DspConfigDto
import com.audihelp.app.data.dto.dsp.toDomain
import com.audihelp.app.data.dto.dsp.toDto
import com.audihelp.app.domain.enums.EarSide
import com.audihelp.app.domain.enums.PatternType
import com.audihelp.app.domain.enums.SessionStatus
import com.audihelp.app.domain.enums.TrialResponse
import com.audihelp.app.domain.model.DspConfig
import com.audihelp.app.domain.model.DspPreset
import com.audihelp.app.domain.model.HearingProfile
import com.audihelp.app.domain.model.HearingTestSession
import com.audihelp.app.domain.model.HearingTestTrial
import com.audihelp.app.domain.model.HearingThreshold
import com.audihelp.app.domain.model.LiveSession
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

private val moshi: Moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

private val dspConfigAdapter = moshi.adapter(DspConfigDto::class.java)

fun DspConfig.toJson(): String = dspConfigAdapter.toJson(this.toDto())

fun String.toDspConfig(): DspConfig =
    dspConfigAdapter.fromJson(this)?.toDomain() ?: DspConfig.default()

// HearingTestSession
fun HearingTestSessionEntity.toDomain(trials: List<HearingTestTrialEntity> = emptyList()): HearingTestSession =
    HearingTestSession(
        id = id,
        earTested = EarSide.entries.firstOrNull { it.value == earTested } ?: EarSide.LEFT,
        status = SessionStatus.entries.firstOrNull { it.value == status } ?: SessionStatus.PENDING,
        notes = notes,
        startedAt = startedAt,
        completedAt = completedAt,
        createdAt = createdAt,
        trials = trials.map { it.toDomain() },
        trialCount = trials.size,
    )

fun HearingTestSession.toEntity(): HearingTestSessionEntity =
    HearingTestSessionEntity(
        id = id,
        earTested = earTested.value,
        status = status.value,
        notes = notes,
        startedAt = startedAt,
        completedAt = completedAt,
        createdAt = createdAt,
    )

// HearingTestTrial
fun HearingTestTrialEntity.toDomain(): HearingTestTrial =
    HearingTestTrial(
        id = id,
        ear = EarSide.entries.firstOrNull { it.value == ear } ?: EarSide.LEFT,
        frequencyHz = frequencyHz,
        levelDbfs = levelDbfs,
        patternType = PatternType.entries.firstOrNull { it.value == patternType } ?: PatternType.TONE,
        response = TrialResponse.entries.firstOrNull { it.value == response } ?: TrialResponse.NOTHING,
        createdAt = createdAt,
    )

// HearingProfile
fun HearingProfileEntity.toDomain(thresholds: List<HearingThresholdEntity> = emptyList()): HearingProfile =
    HearingProfile(
        id = id,
        name = name,
        notes = notes,
        isActive = isActive,
        sourceSessionId = sourceSessionId,
        createdAt = createdAt,
        updatedAt = createdAt,
        thresholds = thresholds.map { it.toDomain() },
        thresholdCount = if (thresholds.isNotEmpty()) thresholds.size else thresholdCount,
    )

fun HearingProfile.toEntity(): HearingProfileEntity =
    HearingProfileEntity(
        id = id,
        name = name,
        notes = notes,
        isActive = isActive,
        sourceSessionId = sourceSessionId,
        createdAt = createdAt,
        thresholdCount = thresholdCount,
    )

// HearingThreshold
fun HearingThresholdEntity.toDomain(): HearingThreshold =
    HearingThreshold(
        id = id,
        ear = EarSide.entries.firstOrNull { it.value == ear } ?: EarSide.LEFT,
        frequencyHz = frequencyHz,
        levelDbfs = levelDbfs,
        response = TrialResponse.entries.firstOrNull { it.value == response } ?: TrialResponse.NOTHING,
        createdAt = createdAt,
    )

fun HearingThreshold.toEntity(profileId: String): HearingThresholdEntity =
    HearingThresholdEntity(
        id = id,
        profileId = profileId,
        ear = ear.value,
        frequencyHz = frequencyHz,
        levelDbfs = levelDbfs,
        response = response.value,
        createdAt = createdAt,
    )

// DspPreset
fun DspPresetEntity.toDomain(): DspPreset =
    DspPreset(
        id = id,
        name = name,
        description = description,
        config = configJson.toDspConfig(),
        isActive = isActive,
        profileId = profileId,
        createdAt = createdAt,
        updatedAt = createdAt,
    )

fun DspPreset.toEntity(): DspPresetEntity =
    DspPresetEntity(
        id = id,
        name = name,
        description = description,
        configJson = config.toJson(),
        isActive = isActive,
        profileId = profileId,
        createdAt = createdAt,
    )

// LiveSession
fun LiveSessionEntity.toDomain(): LiveSession =
    LiveSession(
        id = id,
        presetId = presetId,
        profileId = profileId,
        notes = notes,
        startedAt = startedAt,
        endedAt = endedAt,
        durationSeconds = null,
        createdAt = createdAt,
    )

fun LiveSession.toEntity(): LiveSessionEntity =
    LiveSessionEntity(
        id = id,
        presetId = presetId,
        profileId = profileId,
        notes = notes,
        startedAt = startedAt,
        endedAt = endedAt,
        createdAt = createdAt,
    )
