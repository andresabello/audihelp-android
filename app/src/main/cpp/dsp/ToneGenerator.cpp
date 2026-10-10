#include "ToneGenerator.h"
#include <cmath>

void ToneGenerator::start(float frequencyHz, float levelDbfs, TonePattern pattern, float sampleRate) {
    mAmplitude.store(dbToLinear(levelDbfs));
    mPhaseInc.store(2.f * PI * frequencyHz / sampleRate);
    mPattern.store(static_cast<int>(pattern));
    mPhase = 0.f;
    mPulsePhase = 0.f;
    mRunning.store(true);
}

void ToneGenerator::stop() { mRunning.store(false); }

float ToneGenerator::next() {
    if (!mRunning.load()) return 0.f;

    float amp = mAmplitude.load();
    float inc = mPhaseInc.load();
    float sample = 0.f;

    switch (static_cast<TonePattern>(mPattern.load())) {
        case TonePattern::Sine:
            sample = amp * std::sin(mPhase);
            break;
        case TonePattern::Pulse:
            // 20% duty-cycle pulse at frequency
            sample = amp * (mPhase < 0.2f * 2.f * PI ? 1.f : 0.f);
            break;
        case TonePattern::AM: {
            // Carrier at freq, modulated at 4 Hz
            float modInc = 2.f * PI * 4.f / (2.f * PI / inc);
            sample = amp * std::sin(mPhase) * (0.5f + 0.5f * std::sin(mPulsePhase));
            mPulsePhase += modInc;
            if (mPulsePhase > 2.f * PI) mPulsePhase -= 2.f * PI;
            break;
        }
    }

    mPhase += inc;
    if (mPhase > 2.f * PI) mPhase -= 2.f * PI;

    return sample;
}
