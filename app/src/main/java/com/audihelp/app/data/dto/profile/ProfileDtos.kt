package com.audihelp.app.data.dto.profile

import com.audihelp.app.domain.enums.EarSide
import com.audihelp.app.domain.enums.TrialResponse
import com.audihelp.app.domain.model.HearingProfile
import com.audihelp.app.domain.model.HearingThreshold
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HearingProfileDto(
    val id: String,
    val name: String,
    val notes: String?,
    @Json(name = "is_active")         val isActive: Boolean,
    @Json(name = "source_session_id") val sourceSessionId: String?,
    @Json(name = "created_at")        val createdAt: String,
    @Json(name = "updated_at")        val updatedAt: String,
    val thresholds: List<HearingThresholdDto>?,
    @Json(name = "threshold_count")   val thresholdCount: Int?,
)

@JsonClass(generateAdapter = true)
data class HearingThresholdDto(
    val id: String,
    val ear: String,
    @Json(name = "frequency_hz") val frequencyHz: Int,
    @Json(name = "level_dbfs")   val levelDbfs: Float,
    val response: String,
    @Json(name = "created_at")   val createdAt: String,
)

@JsonClass(generateAdapter = true)
data class CreateProfileRequest(
    val name: String,
    val notes: String? = null,
    @Json(name = "source_session_id") val sourceSessionId: String? = null,
)

@JsonClass(generateAdapter = true)
data class AddThresholdRequest(
    val ear: String,
    @Json(name = "frequency_hz") val frequencyHz: Int,
    @Json(name = "level_dbfs")   val levelDbfs: Float,
    val response: String,
)

fun HearingProfileDto.toDomain() = HearingProfile(
    id              = id,
    name            = name,
    notes           = notes,
    isActive        = isActive,
    sourceSessionId = sourceSessionId,
    createdAt       = createdAt,
    updatedAt       = updatedAt,
    thresholds      = thresholds?.map { it.toDomain() } ?: emptyList(),
    thresholdCount  = thresholdCount ?: thresholds?.size ?: 0,
)

fun HearingThresholdDto.toDomain() = HearingThreshold(
    id          = id,
    ear         = EarSide.entries.firstOrNull { it.value == ear } ?: EarSide.LEFT,
    frequencyHz = frequencyHz,
    levelDbfs   = levelDbfs,
    response    = TrialResponse.entries.firstOrNull { it.value == response } ?: TrialResponse.NOTHING,
    createdAt   = createdAt,
)
