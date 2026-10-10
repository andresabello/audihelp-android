#include "FrequencyShifter.h"
#include <cmath>

static constexpr float PI = 3.14159265358979323846f;

// Hilbert all-pass coefficients for 48 kHz (Zolzer "DAFX" appendix)
const std::array<float, FrequencyShifter::STAGES> FrequencyShifter::kCoeffsA = {
    0.4797f, 0.8765f, 0.9760f, 0.9975f, 0.9999f, 0.9999f, 0.9999f, 0.9999f
};
const std::array<float, FrequencyShifter::STAGES> FrequencyShifter::kCoeffsB = {
    0.1615f, 0.7335f, 0.9437f, 0.9900f, 0.9989f, 0.9999f, 0.9999f, 0.9999f
};

void FrequencyShifter::setParams(bool enabled, float shiftHz, float sampleRate) {
    mEnabled.store(enabled);
    mPhaseInc.store(2.f * PI * shiftHz / sampleRate);
}

float FrequencyShifter::processAllpassChain(float x,
    std::array<float, STAGES>& state,
    const std::array<float, STAGES>& coeffs)
{
    for (int i = 0; i < STAGES; ++i) {
        float y = -coeffs[i] * x + state[i];
        state[i] = coeffs[i] * y + x;
        x = y;
    }
    return x;
}

float FrequencyShifter::process(float sample) {
    if (!mEnabled.load()) return sample;

    float real = processAllpassChain(sample, mStateA, kCoeffsA);
    float imag = processAllpassChain(sample, mStateB, kCoeffsB);

    float phaseInc = mPhaseInc.load();
    float cosP = std::cos(mPhase);
    float sinP = std::sin(mPhase);

    mPhase += phaseInc;
    if (mPhase > 2.f * PI) mPhase -= 2.f * PI;

    // Upper sideband: real*cos - imag*sin
    return real * cosP - imag * sinP;
}
