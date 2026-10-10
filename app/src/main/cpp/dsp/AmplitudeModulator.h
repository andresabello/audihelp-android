#pragma once
#include <atomic>
#include <cmath>

class AmplitudeModulator {
public:
    void setParams(bool enabled, float depth, float rateHz, float sampleRate);
    float process(float sample);

private:
    std::atomic<bool>  mEnabled{false};
    std::atomic<float> mDepth{0.f};
    std::atomic<float> mPhaseInc{0.f};
    float mPhase{0.f};
};
