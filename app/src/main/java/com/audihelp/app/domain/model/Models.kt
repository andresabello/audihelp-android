package com.audihelp.app.domain.model

import com.audihelp.app.domain.enums.EarSide
import com.audihelp.app.domain.enums.EqFilterType
import com.audihelp.app.domain.enums.PatternType
import com.audihelp.app.domain.enums.SessionStatus
import com.audihelp.app.domain.enums.TrialResponse

data class User(
    val id: String,
    val name: String,
    val email: String,
    val avatar: String?,
)

data class HearingTestSession(
    val id: String,
    val earTested: EarSide,
    val status: SessionStatus,
    val notes: String?,
    val startedAt: String?,
    val completedAt: String?,
    val createdAt: String,
    val trials: List<HearingTestTrial> = emptyList(),
    val trialCount: Int = 0,
)

data class HearingTestTrial(
    val id: String,
    val ear: EarSide,
    val frequencyHz: Int,
    val levelDbfs: Float,
    val patternType: PatternType,
    val response: TrialResponse,
    val createdAt: String,
)

data class HearingProfile(
    val id: String,
    val name: String,
    val notes: String?,
    val isActive: Boolean,
    val sourceSessionId: String?,
    val createdAt: String,
    val updatedAt: String,
    val thresholds: List<HearingThreshold> = emptyList(),
    val thresholdCount: Int = 0,
)

data class HearingThreshold(
    val id: String,
    val ear: EarSide,
    val frequencyHz: Int,
    val levelDbfs: Float,
    val response: TrialResponse,
    val createdAt: String,
)

// DSP Config tree
data class DspConfig(
    val eq: List<EqBand> = emptyList(),
    val compressor: CompressorConfig? = null,
    val limiter: LimiterConfig? = null,
    val freqTransform: FreqTransformConfig? = null,
    val modulation: ModulationConfig? = null,
    val gainDb: Float = 0f,
) {
    companion object {
        fun default() = DspConfig(
            eq = listOf(EqBand(1000f, 0f, 1.41f, EqFilterType.PEAK)),
            compressor = CompressorConfig(-20f, 4f, 10f, 200f, 6f),
            limiter = LimiterConfig(-1f),
            gainDb = 0f,
        )
    }
}

data class EqBand(
    val freq: Float,
    val gainDb: Float,
    val q: Float,
    val type: EqFilterType,
    val enabled: Boolean = true,
)

data class CompressorConfig(
    val thresholdDbfs: Float,
    val ratio: Float,
    val attackMs: Float,
    val releaseMs: Float,
    val makeupGainDb: Float = 0f,
)

data class LimiterConfig(val ceilingDbfs: Float)

data class FreqTransformConfig(
    val enabled: Boolean,
    val shiftHz: Float = 0f,
)

data class ModulationConfig(
    val enabled: Boolean,
    val depth: Float = 0f,
    val rateHz: Float = 1f,
)

data class DspPreset(
    val id: String,
    val name: String,
    val description: String?,
    val config: DspConfig,
    val isActive: Boolean,
    val profileId: String?,
    val createdAt: String,
    val updatedAt: String,
)

data class LiveSession(
    val id: String,
    val presetId: String?,
    val profileId: String?,
    val notes: String?,
    val startedAt: String?,
    val endedAt: String?,
    val durationSeconds: Int?,
    val createdAt: String,
)
