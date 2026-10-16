package com.audihelp.app.audio

import com.audihelp.app.domain.enums.EqFilterType
import com.audihelp.app.domain.model.DspConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioEngineController @Inject constructor() {

    private var handle: Long = nativeCreate()

    fun create() {
        if (handle == 0L) handle = nativeCreate()
    }

    fun destroy() {
        if (handle != 0L) { nativeDestroy(handle); handle = 0L }
    }

    var isRunning: Boolean = false
        private set

    /** Opens Oboe input (mic) + output (headphones/BT) and starts processing. */
    fun startLiveMode(sampleRate: Int = 48000, framesPerBurst: Int = 192): Boolean {
        val ok = nativeStartLiveMode(handle, sampleRate, framesPerBurst)
        if (ok) isRunning = true
        return ok
    }

    fun stopLiveMode() {
        nativeStopLiveMode(handle)
        isRunning = false
    }

    fun startTone(frequencyHz: Float, levelDbfs: Float, patternType: Int) =
        nativeStartTone(handle, frequencyHz, levelDbfs, patternType)

    fun stopTone() = nativeStopTone(handle)

    fun applyDspPreset(config: DspConfig) {
        config.eq.forEachIndexed { i, band ->
            nativeSetEqBand(handle, i, band.freq, band.gainDb, band.q, band.type.ordinal)
        }
        nativeSetEqBandCount(handle, config.eq.size)
        config.compressor?.let {
            nativeSetCompressor(handle, it.thresholdDbfs, it.ratio, it.attackMs, it.releaseMs, it.makeupGainDb)
        }
        config.limiter?.let { nativeSetLimiterCeiling(handle, it.ceilingDbfs) }
        config.freqTransform?.let { nativeSetFreqShift(handle, it.enabled, it.shiftHz) }
        config.modulation?.let { nativeSetModulation(handle, it.enabled, it.depth, it.rateHz) }
        nativeSetMasterGain(handle, config.gainDb)
    }

    fun setEqBand(index: Int, freq: Float, gainDb: Float, q: Float, type: EqFilterType) =
        nativeSetEqBand(handle, index, freq, gainDb, q, type.ordinal)

    fun setEqBandCount(count: Int) = nativeSetEqBandCount(handle, count)
    fun setCompressor(threshold: Float, ratio: Float, attack: Float, release: Float, makeup: Float) =
        nativeSetCompressor(handle, threshold, ratio, attack, release, makeup)
    fun setLimiterCeiling(ceiling: Float) = nativeSetLimiterCeiling(handle, ceiling)
    fun setFreqShift(enabled: Boolean, shiftHz: Float) = nativeSetFreqShift(handle, enabled, shiftHz)
    fun setModulation(enabled: Boolean, depth: Float, rateHz: Float) = nativeSetModulation(handle, enabled, depth, rateHz)
    fun setMasterGain(gainDb: Float) = nativeSetMasterGain(handle, gainDb)

    fun getInputLevelDbfs(): Float  = nativeGetInputLevelDbfs(handle)
    fun getOutputLevelDbfs(): Float = nativeGetOutputLevelDbfs(handle)

    private external fun nativeCreate(): Long
    private external fun nativeDestroy(handle: Long)
    private external fun nativeStartLiveMode(handle: Long, sampleRate: Int, framesPerBurst: Int): Boolean
    private external fun nativeStopLiveMode(handle: Long)
    private external fun nativeStartTone(handle: Long, frequencyHz: Float, levelDbfs: Float, patternType: Int)
    private external fun nativeStopTone(handle: Long)
    private external fun nativeSetEqBand(handle: Long, bandIndex: Int, freq: Float, gainDb: Float, q: Float, filterType: Int)
    private external fun nativeSetEqBandCount(handle: Long, count: Int)
    private external fun nativeSetCompressor(handle: Long, thresholdDbfs: Float, ratio: Float, attackMs: Float, releaseMs: Float, makeupGainDb: Float)
    private external fun nativeSetLimiterCeiling(handle: Long, ceilingDbfs: Float)
    private external fun nativeSetFreqShift(handle: Long, enabled: Boolean, shiftHz: Float)
    private external fun nativeSetModulation(handle: Long, enabled: Boolean, depth: Float, rateHz: Float)
    private external fun nativeSetMasterGain(handle: Long, gainDb: Float)
    private external fun nativeGetInputLevelDbfs(handle: Long): Float
    private external fun nativeGetOutputLevelDbfs(handle: Long): Float

    companion object {
        init { System.loadLibrary("audihelp_engine") }
    }
}
