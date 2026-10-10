#pragma once

#include <oboe/Oboe.h>
#include <atomic>
#include <memory>
#include <cmath>
#include <cstring>

#include "dsp/ParametricEQ.h"
#include "dsp/Compressor.h"
#include "dsp/Limiter.h"
#include "dsp/FrequencyShifter.h"
#include "dsp/AmplitudeModulator.h"
#include "dsp/ToneGenerator.h"

enum class EngineMode { Stopped, Tone, Live };

class AudioEngine : public oboe::AudioStreamDataCallback {
public:
    AudioEngine();
    ~AudioEngine();

    // Live mode: opens an input stream (mic) and output stream (headphones/BT) via Oboe.
    // Oboe routes automatically — DJI Mic Mini via USB-C is preferred input when connected.
    bool startLiveMode(int sampleRate = 48000, int framesPerBurst = 192);
    void stopLiveMode();

    void startTone(float frequencyHz, float levelDbfs, int patternType);
    void stopTone();

    void setEqBand(int index, float freq, float gainDb, float q, int filterType, float sampleRate);
    void setEqBandCount(int n);
    void setCompressor(float threshold, float ratio, float attack, float release, float makeup);
    void setLimiterCeiling(float ceiling);
    void setFreqShift(bool enabled, float shiftHz);
    void setModulation(bool enabled, float depth, float rateHz);
    void setMasterGain(float gainDb);

    float getInputLevelDbfs() const;
    float getOutputLevelDbfs() const;

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream* stream,
                                          void* audioData, int32_t numFrames) override;

private:
    std::shared_ptr<oboe::AudioStream> mOutputStream;
    std::shared_ptr<oboe::AudioStream> mInputStream;
    std::atomic<EngineMode> mMode{EngineMode::Stopped};

    ParametricEQ       mEQ;
    Compressor         mCompressor;
    Limiter            mLimiter;
    FrequencyShifter   mFreqShifter;
    AmplitudeModulator mModulator;
    ToneGenerator      mTone;

    std::atomic<float> mMasterGain{1.f};
    std::atomic<float> mInputLevel{0.f};
    std::atomic<float> mOutputLevel{0.f};
    float mSampleRate{48000.f};

    // Scratch buffer for reading from the input stream inside the output callback.
    // Sized for worst-case framesPerBurst; safe because onAudioReady is single-threaded.
    static constexpr int32_t kMaxFramesPerBurst = 512;
    float mInputBuffer[kMaxFramesPerBurst]{};

    static float dbToLinear(float db) { return std::pow(10.f, db / 20.f); }
    static float linearToDb(float lin) { return 20.f * std::log10(std::fabs(lin) + 1e-10f); }
    void updateLevel(std::atomic<float>& meter, float sample);

    template<typename Builder>
    bool tryOpenStream(Builder& builder, std::shared_ptr<oboe::AudioStream>& stream);
};
