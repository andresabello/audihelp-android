#include "AmplitudeModulator.h"
#include <cmath>

static constexpr float PI = 3.14159265358979323846f;

void AmplitudeModulator::setParams(bool enabled, float depth, float rateHz, float sampleRate) {
    mEnabled.store(enabled);
    mDepth.store(depth);
    mPhaseInc.store(2.f * PI * rateHz / sampleRate);
}

float AmplitudeModulator::process(float sample) {
    if (!mEnabled.load()) return sample;

    float d = mDepth.load();
    float mod = 1.f - d * 0.5f + d * 0.5f * std::sin(mPhase);

    mPhase += mPhaseInc.load();
    if (mPhase > 2.f * PI) mPhase -= 2.f * PI;

    return sample * mod;
}
