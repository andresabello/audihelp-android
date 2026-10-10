#pragma once
#include <atomic>
#include <cmath>

class Compressor {
public:
    void setParams(float thresholdDbfs, float ratio, float attackMs, float releaseMs,
                   float makeupGainDb, float sampleRate);

    float process(float sample);

private:
    std::atomic<float> mThreshold{-20.f};
    std::atomic<float> mRatio{4.f};
    std::atomic<float> mMakeup{1.f};

    float mAttackCoeff{0.f};
    float mReleaseCoeff{0.f};
    float mEnvelope{0.f};

    static float dbToLinear(float db) { return std::pow(10.f, db / 20.f); }
    static float linearToDb(float lin) { return 20.f * std::log10(lin + 1e-10f); }
};
