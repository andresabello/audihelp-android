package com.audihelp.app.ui.profile

import com.audihelp.app.domain.enums.EarSide
import com.audihelp.app.domain.enums.EqFilterType
import com.audihelp.app.domain.enums.TrialResponse
import com.audihelp.app.domain.model.CompressorConfig
import com.audihelp.app.domain.model.DspConfig
import com.audihelp.app.domain.model.EqBand
import com.audihelp.app.domain.model.FreqTransformConfig
import com.audihelp.app.domain.model.HearingThreshold
import com.audihelp.app.domain.model.LimiterConfig
import com.audihelp.app.domain.model.ModulationConfig

/**
 * dBFS threshold considered "normal" hearing.
 * During testing, a person with healthy hearing typically perceives
 * a tone at ~-70 dBFS (very quiet). Thresholds above (closer to 0) indicate loss.
 */
private const val NORMAL_THRESHOLD_DBFS = -70f

/** EQ gain cap — prevents feedback loops and protects residual hearing. */
private const val MAX_EQ_GAIN_DB = 28f

/**
 * Converts hearing thresholds from an audiogram into a [DspConfig] that compensates
 * for the measured hearing loss at each frequency.
 *
 * ## EQ
 * For each measured frequency the required boost is:
 *   boost = (threshold_dBFS − NORMAL_THRESHOLD_DBFS).coerceIn(0, MAX_EQ_GAIN_DB)
 * e.g. threshold = −30 dBFS → boost = 40 dB → capped at 28 dB.
 * When both ears are measured the average threshold is used.
 *
 * ## Amplitude modulation (vibrotactile feedback)
 * If any frequency has a FEEL or NOTHING response the user cannot consciously
 * hear it. Enabling modulation at 4 Hz converts the audio carrier into a
 * perceivable vibration pattern through bone conduction and skin receptors,
 * giving tactile awareness of sounds that are inaudible through air conduction.
 *
 * ## Compressor / Limiter
 * A soft-ratio compressor keeps transients from exceeding the boosted levels
 * painfully, and a -3 dBFS limiter is the last safety net.
 */
fun thresholdsToDspConfig(thresholds: List<HearingThreshold>): DspConfig {
    if (thresholds.isEmpty()) return DspConfig.default()

    // Average both ears per frequency — use right if only right is present, etc.
    val byFreq: Map<Int, Float> = buildMap {
        val right = thresholds.filter { it.ear == EarSide.RIGHT }.associate { it.frequencyHz to it.levelDbfs }
        val left  = thresholds.filter { it.ear == EarSide.LEFT  }.associate { it.frequencyHz to it.levelDbfs }
        (right.keys + left.keys).toSortedSet().forEach { freq ->
            val r = right[freq]
            val l = left[freq]
            put(freq, when {
                r != null && l != null -> (r + l) / 2f
                r != null -> r
                else -> l!!
            })
        }
    }

    // EQ bands — one per measured frequency
    val eqBands = byFreq.entries.sortedBy { it.key }.mapNotNull { (freq, levelDbfs) ->
        if (!levelDbfs.isFinite() || freq <= 0) return@mapNotNull null
        val boostDb = (levelDbfs - NORMAL_THRESHOLD_DBFS)
            .coerceAtLeast(0f)
            .coerceAtMost(MAX_EQ_GAIN_DB)
            .let { if (it.isFinite()) it else 0f }

        val q = when {
            freq <= 500  -> 0.7f
            freq <= 2000 -> 1.0f
            else         -> 1.41f
        }

        EqBand(freq = freq.toFloat(), gainDb = boostDb, q = q, type = EqFilterType.PEAK)
    }

    // Vibrotactile modulation — needed when the user can only feel or not perceive a frequency
    val tactiFreqs = thresholds.filter {
        it.response == TrialResponse.FEEL || it.response == TrialResponse.NOTHING
    }
    val modulationEnabled = tactiFreqs.isNotEmpty()
    // Higher depth when some frequencies are completely imperceptible
    val modulationDepth = when {
        tactiFreqs.any { it.response == TrialResponse.NOTHING } -> 0.65f
        else -> 0.35f
    }

    // Average loss across all frequencies — drives compressor makeup and master gain
    val avgLossDb = byFreq.values
        .filter { it.isFinite() }
        .map { (it - NORMAL_THRESHOLD_DBFS).coerceAtLeast(0f) }
        .average()
        .let { if (it.isFinite()) it.toFloat() else 0f }

    // Gentle master gain lift proportional to average loss
    val masterGain = (avgLossDb * 0.2f).coerceIn(0f, 12f).let { if (it.isFinite()) it else 0f }

    // Compressor: tighter threshold and moderate ratio to keep boosted peaks controlled
    val compressor = CompressorConfig(
        thresholdDbfs = -24f,
        ratio = 3.5f,
        attackMs = 10f,
        releaseMs = 120f,
        makeupGainDb = (avgLossDb * 0.15f).coerceIn(0f, 8f),
    )

    return DspConfig(
        eq = eqBands,
        compressor = compressor,
        limiter = LimiterConfig(ceilingDbfs = -3f),
        freqTransform = FreqTransformConfig(enabled = false, shiftHz = 0f),
        modulation = ModulationConfig(
            enabled = modulationEnabled,
            depth = if (modulationEnabled) modulationDepth else 0f,
            rateHz = 4f,   // 4 Hz sits in the perceptible vibration range (1–10 Hz)
        ),
        gainDb = masterGain,
    )
}

/**
 * Human-readable description of what the generated config will do.
 * Shown to the user before saving so they understand the result.
 */
fun describeGeneratedConfig(config: DspConfig, thresholds: List<HearingThreshold>): String {
    val lines = mutableListOf<String>()

    val boostBands = config.eq.filter { it.gainDb > 0f }
    if (boostBands.isNotEmpty()) {
        val summary = boostBands.joinToString(", ") { b ->
            val f = if (b.freq >= 1000) "${(b.freq / 1000).toInt()}kHz" else "${b.freq.toInt()}Hz"
            "$f +${b.gainDb.toInt()}dB"
        }
        lines += "EQ boost: $summary"
    } else {
        lines += "No EQ boost needed — hearing thresholds are near normal."
    }

    if (config.modulation?.enabled == true) {
        val tactiFreqs = thresholds
            .filter { it.response == TrialResponse.FEEL || it.response == TrialResponse.NOTHING }
            .map { it.frequencyHz }
            .toSortedSet()
        lines += "Vibrotactile modulation ON (${config.modulation.depth * 100}% depth, ${config.modulation.rateHz}Hz) for $tactiFreqs Hz"
    }

    lines += "Master gain +${config.gainDb.toInt()} dB"
    lines += "Limiter at ${config.limiter?.ceilingDbfs?.toInt()} dBFS"

    return lines.joinToString("\n")
}
