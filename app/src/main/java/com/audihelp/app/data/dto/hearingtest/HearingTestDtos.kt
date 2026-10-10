package com.audihelp.app.data.dto.hearingtest

import com.audihelp.app.domain.enums.EarSide
import com.audihelp.app.domain.enums.PatternType
import com.audihelp.app.domain.enums.SessionStatus
import com.audihelp.app.domain.enums.TrialResponse
import com.audihelp.app.domain.model.HearingTestSession
import com.audihelp.app.domain.model.HearingTestTrial
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HearingTestSessionDto(
    val id: String,
    @Json(name = "ear_tested")   val earTested: String,
    val status: String,
    val notes: String?,
    @Json(name = "started_at")   val startedAt: String?,
    @Json(name = "completed_at") val completedAt: String?,
    @Json(name = "created_at")   val createdAt: String,
    val trials: List<HearingTestTrialDto>?,
    @Json(name = "trial_count")  val trialCount: Int?,
)

@JsonClass(generateAdapter = true)
data class HearingTestTrialDto(
    val id: String,
    val ear: String,
    @Json(name = "frequency_hz") val frequencyHz: Int,
    @Json(name = "level_dbfs")   val levelDbfs: Float,
    @Json(name = "pattern_type") val patternType: String,
    val response: String,
    @Json(name = "created_at")   val createdAt: String,
)

@JsonClass(generateAdapter = true)
data class StartSessionRequest(
    @Json(name = "ear_tested") val earTested: String,
    val notes: String? = null,
)

@JsonClass(generateAdapter = true)
data class RecordTrialRequest(
    val ear: String,
    @Json(name = "frequency_hz") val frequencyHz: Int,
    @Json(name = "level_dbfs")   val levelDbfs: Float,
    @Json(name = "pattern_type") val patternType: String,
    val response: String,
)

fun HearingTestSessionDto.toDomain() = HearingTestSession(
    id           = id,
    earTested    = EarSide.entries.firstOrNull { it.value == earTested } ?: EarSide.BOTH,
    status       = SessionStatus.entries.firstOrNull { it.value == status } ?: SessionStatus.IN_PROGRESS,
    notes        = notes,
    startedAt    = startedAt,
    completedAt  = completedAt,
    createdAt    = createdAt,
    trials       = trials?.map { it.toDomain() } ?: emptyList(),
    trialCount   = trialCount ?: trials?.size ?: 0,
)

fun HearingTestTrialDto.toDomain() = HearingTestTrial(
    id           = id,
    ear          = EarSide.entries.firstOrNull { it.value == ear } ?: EarSide.LEFT,
    frequencyHz  = frequencyHz,
    levelDbfs    = levelDbfs,
    patternType  = PatternType.entries.firstOrNull { it.value == patternType } ?: PatternType.TONE,
    response     = TrialResponse.entries.firstOrNull { it.value == response } ?: TrialResponse.NOTHING,
    createdAt    = createdAt,
)
