package com.audihelp.app.data.dto.livesession

import com.audihelp.app.domain.model.LiveSession
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LiveSessionDto(
    val id: String,
    @Json(name = "preset_id")         val presetId: String?,
    @Json(name = "profile_id")        val profileId: String?,
    val notes: String?,
    @Json(name = "started_at")        val startedAt: String?,
    @Json(name = "ended_at")          val endedAt: String?,
    @Json(name = "duration_seconds")  val durationSeconds: Int?,
    @Json(name = "created_at")        val createdAt: String,
)

@JsonClass(generateAdapter = true)
data class CreateLiveSessionRequest(
    @Json(name = "preset_id")  val presetId: String? = null,
    @Json(name = "profile_id") val profileId: String? = null,
    val notes: String? = null,
)

fun LiveSessionDto.toDomain() = LiveSession(
    id              = id,
    presetId        = presetId,
    profileId       = profileId,
    notes           = notes,
    startedAt       = startedAt,
    endedAt         = endedAt,
    durationSeconds = durationSeconds,
    createdAt       = createdAt,
)
