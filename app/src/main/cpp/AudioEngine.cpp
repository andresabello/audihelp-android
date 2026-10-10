#include "AudioEngine.h"
#include <android/log.h>
#include <cstring>

#define TAG "AudioEngine"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)

AudioEngine::AudioEngine() {
    mLimiter.setCeiling(-1.f);
}

AudioEngine::~AudioEngine() {
    stopLiveMode();
    stopTone();
}

// ── Live mode (full-duplex) ────────────────────────────────────────────────────

bool AudioEngine::startLiveMode(int sampleRate, int framesPerBurst) {
    stopLiveMode();

    // ── Input stream (microphone) ─────────────────────────────────────────────
    // Oboe automatically selects the best available input: if the DJI Mic Mini
    // (or any USB audio device) is plugged in via USB-C, Android routes it here.
    // Falls back to the built-in mic otherwise.
    oboe::AudioStreamBuilder inBuilder;
    inBuilder.setDirection(oboe::Direction::Input)
             ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
             ->setInputPreset(oboe::InputPreset::VoiceCommunication) // enables AEC + noise suppression
             ->setFormat(oboe::AudioFormat::Float)
             ->setChannelCount(oboe::ChannelCount::Mono)
             ->setSampleRate(sampleRate);

    if (!tryOpenStream(inBuilder, mInputStream)) {
        LOGE("startLiveMode: failed to open input stream");
        return false;
    }

    int actualRate = mInputStream->getSampleRate();
    mSampleRate = static_cast<float>(actualRate);

    // ── Output stream (headphones / Bluetooth) ────────────────────────────────
    // Stereo so both L and R channels carry signal (wired headphones, haptic
    // headphones with dual drivers, or BT).
    oboe::AudioStreamBuilder outBuilder;
    outBuilder.setDirection(oboe::Direction::Output)
              ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
              ->setFormat(oboe::AudioFormat::Float)
              ->setChannelCount(oboe::ChannelCount::Stereo)
              ->setSampleRate(actualRate)
              ->setFramesPerDataCallback(framesPerBurst)
              ->setDataCallback(this);

    if (!tryOpenStream(outBuilder, mOutputStream)) {
        LOGE("startLiveMode: failed to open output stream");
        mInputStream->close();
        mInputStream.reset();
        return false;
    }

    mInputStream->requestStart();
    mOutputStream->requestStart();
    mMode.store(EngineMode::Live);

    LOGI("Live mode started — rate=%d in=%s out=%s",
         actualRate,
         mInputStream->getSharingMode()  == oboe::SharingMode::Exclusive ? "excl" : "shared",
         mOutputStream->getSharingMode() == oboe::SharingMode::Exclusive ? "excl" : "shared");
    return true;
}

void AudioEngine::stopLiveMode() {
    mMode.store(EngineMode::Stopped);
    if (mOutputStream) { mOutputStream->requestStop(); mOutputStream->close(); mOutputStream.reset(); }
    if (mInputStream)  { mInputStream->requestStop();  mInputStream->close();  mInputStream.reset();  }
}

// ── Tone playback ─────────────────────────────────────────────────────────────

void AudioEngine::startTone(float frequencyHz, float levelDbfs, int patternType) {
    if (!mOutputStream) {
        oboe::AudioStreamBuilder builder;
        builder.setDirection(oboe::Direction::Output)
               ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
               ->setFormat(oboe::AudioFormat::Float)
               ->setChannelCount(oboe::ChannelCount::Stereo)
               ->setDataCallback(this);

        if (!tryOpenStream(builder, mOutputStream)) {
            LOGE("startTone: could not open output stream");
            return;
        }
        mSampleRate = static_cast<float>(mOutputStream->getSampleRate());
        mOutputStream->requestStart();
    }
    mTone.start(frequencyHz, levelDbfs, static_cast<TonePattern>(patternType), mSampleRate);
    mMode.store(EngineMode::Tone);
}

void AudioEngine::stopTone() {
    mTone.stop();
    if (mOutputStream && mMode.load() == EngineMode::Tone) {
        mMode.store(EngineMode::Stopped);
        mOutputStream->requestStop();
        mOutputStream->close();
        mOutputStream.reset();
    }
}

// ── Audio callback ────────────────────────────────────────────────────────────

oboe::DataCallbackResult AudioEngine::onAudioReady(oboe::AudioStream* stream,
                                                    void* audioData, int32_t numFrames) {
    auto* output = static_cast<float*>(audioData);
    const int channels = stream->getChannelCount(); // 2 for live/tone, but check anyway
    EngineMode mode = mMode.load();

    if (mode == EngineMode::Live && mInputStream) {
        // Non-blocking read from the input stream into our scratch buffer.
        int32_t toRead = (numFrames <= kMaxFramesPerBurst) ? numFrames : kMaxFramesPerBurst;
        auto readResult = mInputStream->read(mInputBuffer, toRead, 0 /*timeoutNs = non-blocking*/);
        int32_t framesRead = (readResult) ? readResult.value() : 0;

        for (int i = 0; i < numFrames; ++i) {
            float sample = (i < framesRead) ? mInputBuffer[i] : 0.f;

            updateLevel(mInputLevel, sample);
            sample = mEQ.process(sample);
            sample = mCompressor.process(sample);
            sample = mFreqShifter.process(sample);
            sample = mModulator.process(sample);
            sample = sample * mMasterGain.load();
            sample = mLimiter.process(sample);
            updateLevel(mOutputLevel, sample);

            if (channels == 2) {
                output[i * 2]     = sample; // L
                output[i * 2 + 1] = sample; // R
            } else {
                output[i] = sample;
            }
        }

    } else if (mode == EngineMode::Tone) {
        for (int i = 0; i < numFrames; ++i) {
            float sample = mTone.next();
            sample = mLimiter.process(sample);
            updateLevel(mOutputLevel, sample);
            if (channels == 2) {
                output[i * 2]     = sample;
                output[i * 2 + 1] = sample;
            } else {
                output[i] = sample;
            }
        }

    } else {
        memset(output, 0, sizeof(float) * numFrames * channels);
    }

    return oboe::DataCallbackResult::Continue;
}

// ── Helpers ───────────────────────────────────────────────────────────────────

template<typename Builder>
bool AudioEngine::tryOpenStream(Builder& builder, std::shared_ptr<oboe::AudioStream>& stream) {
    for (auto sharing : { oboe::SharingMode::Exclusive, oboe::SharingMode::Shared }) {
        builder.setSharingMode(sharing);
        if (builder.openStream(stream) == oboe::Result::OK) return true;
    }
    return false;
}

void AudioEngine::updateLevel(std::atomic<float>& meter, float sample) {
    float absVal  = std::fabs(sample);
    float current = meter.load(std::memory_order_relaxed);
    float next    = absVal > current ? absVal : current * 0.9999f;
    meter.store(next, std::memory_order_relaxed);
}

float AudioEngine::getInputLevelDbfs()  const { return linearToDb(mInputLevel.load());  }
float AudioEngine::getOutputLevelDbfs() const { return linearToDb(mOutputLevel.load()); }

// ── DSP setters ───────────────────────────────────────────────────────────────

void AudioEngine::setEqBand(int index, float freq, float gainDb, float q, int filterType, float sr) {
    mEQ.setBand(index, freq, gainDb, q, static_cast<FilterType>(filterType), sr);
}
void AudioEngine::setEqBandCount(int n)           { mEQ.setBandCount(n); }
void AudioEngine::setLimiterCeiling(float ceiling) { mLimiter.setCeiling(ceiling); }
void AudioEngine::setMasterGain(float gainDb)      { mMasterGain.store(dbToLinear(gainDb)); }

void AudioEngine::setCompressor(float threshold, float ratio, float attack, float release, float makeup) {
    mCompressor.setParams(threshold, ratio, attack, release, makeup, mSampleRate);
}
void AudioEngine::setFreqShift(bool enabled, float shiftHz) {
    mFreqShifter.setParams(enabled, shiftHz, mSampleRate);
}
void AudioEngine::setModulation(bool enabled, float depth, float rateHz) {
    mModulator.setParams(enabled, depth, rateHz, mSampleRate);
}
