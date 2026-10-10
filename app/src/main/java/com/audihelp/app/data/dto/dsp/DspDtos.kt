package com.audihelp.app.data.dto.dsp

import com.audihelp.app.domain.enums.EqFilterType
import com.audihelp.app.domain.model.CompressorConfig
import com.audihelp.app.domain.model.DspConfig
import com.audihelp.app.domain.model.DspPreset
import com.audihelp.app.domain.model.EqBand
import com.audihelp.app.domain.model.FreqTransformConfig
import com.audihelp.app.domain.model.LimiterConfig
import com.audihelp.app.domain.model.ModulationConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DspPresetDto(
    val id: String,
    val name: String,
    val description: String?,
    val config: DspConfigDto,
    @Json(name = "is_active")   val isActive: Boolean,
    @Json(name = "created_at")  val createdAt: String,
    @Json(name = "updated_at")  val updatedAt: String,
)

@JsonClass(generateAdapter = true)
data class DspConfigDto(
    val eq: List<EqBandDto>?,
    val compressor: CompressorConfigDto?,
    val limiter: LimiterConfigDto?,
    @Json(name = "freq_transform") val freqTransform: FreqTransformConfigDto?,
    val modulation: ModulationConfigDto?,
    @Json(name = "gain_db") val gainDb: Float?,
)

@JsonClass(generateAdapter = true)
data class EqBandDto(
    val freq: Float,
    @Json(name = "gain_db") val gainDb: Float,
    val q: Float,
    val type: String,
)

@JsonClass(generateAdapter = true)
data class CompressorConfigDto(
    @Json(name = "threshold_dbfs") val thresholdDbfs: Float,
    val ratio: Float,
    @Json(name = "attack_ms")  val attackMs: Float,
    @Json(name = "release_ms") val releaseMs: Float,
    @Json(name = "makeup_gain_db") val makeupGainDb: Float?,
)

@JsonClass(generateAdapter = true)
data class LimiterConfigDto(@Json(name = "ceiling_dbfs") val ceilingDbfs: Float)

@JsonClass(generateAdapter = true)
data class FreqTransformConfigDto(
    val enabled: Boolean,
    @Json(name = "shift_hz") val shiftHz: Float?,
)

@JsonClass(generateAdapter = true)
data class ModulationConfigDto(
    val enabled: Boolean,
    val depth: Float?,
    @Json(name = "rate_hz") val rateHz: Float?,
)

@JsonClass(generateAdapter = true)
data class SavePresetRequest(
    val name: String,
    val description: String?,
    val config: DspConfigDto,
)

fun DspPresetDto.toDomain() = DspPreset(
    id          = id,
    name        = name,
    description = description,
    isActive    = isActive,
    config      = config.toDomain(),
    profileId   = null,
    createdAt   = createdAt,
    updatedAt   = updatedAt,
)

fun DspConfigDto.toDomain() = DspConfig(
    eq           = eq?.map { it.toDomain() } ?: emptyList(),
    compressor   = compressor?.toDomain(),
    limiter      = limiter?.toDomain(),
    freqTransform= freqTransform?.toDomain(),
    modulation   = modulation?.toDomain(),
    gainDb       = gainDb ?: 0f,
)

fun EqBandDto.toDomain() = EqBand(
    freq   = freq,
    gainDb = gainDb,
    q      = q,
    type   = EqFilterType.entries.firstOrNull { it.value == type } ?: EqFilterType.PEAK,
)

fun CompressorConfigDto.toDomain() = CompressorConfig(
    thresholdDbfs = thresholdDbfs,
    ratio         = ratio,
    attackMs      = attackMs,
    releaseMs     = releaseMs,
    makeupGainDb  = makeupGainDb ?: 0f,
)

fun LimiterConfigDto.toDomain()       = LimiterConfig(ceilingDbfs)
fun FreqTransformConfigDto.toDomain() = FreqTransformConfig(enabled, shiftHz ?: 0f)
fun ModulationConfigDto.toDomain()    = ModulationConfig(enabled, depth ?: 0f, rateHz ?: 1f)

fun DspConfig.toDto() = DspConfigDto(
    eq           = eq.map { EqBandDto(it.freq, it.gainDb, it.q, it.type.value) },
    compressor   = compressor?.let { CompressorConfigDto(it.thresholdDbfs, it.ratio, it.attackMs, it.releaseMs, it.makeupGainDb) },
    limiter      = limiter?.let { LimiterConfigDto(it.ceilingDbfs) },
    freqTransform= freqTransform?.let { FreqTransformConfigDto(it.enabled, it.shiftHz) },
    modulation   = modulation?.let { ModulationConfigDto(it.enabled, it.depth, it.rateHz) },
    gainDb       = gainDb,
)
