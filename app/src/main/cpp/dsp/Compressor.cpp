#include "Compressor.h"
#include <cmath>

void Compressor::setParams(float thresholdDbfs, float ratio, float attackMs, float releaseMs,
                           float makeupGainDb, float sampleRate) {
    mThreshold.store(thresholdDbfs);
    mRatio.store(ratio);
    mMakeup.store(dbToLinear(makeupGainDb));
    mAttackCoeff  = std::exp(-1.f / (sampleRate * attackMs  * 0.001f));
    mReleaseCoeff = std::exp(-1.f / (sampleRate * releaseMs * 0.001f));
}

float Compressor::process(float sample) {
    float absIn = std::fabs(sample);

    // RMS-ish envelope follower
    float coeff = absIn > mEnvelope ? mAttackCoeff : mReleaseCoeff;
    mEnvelope = coeff * mEnvelope + (1.f - coeff) * absIn;

    float envDb = linearToDb(mEnvelope);
    float thresh = mThreshold.load();
    float ratio  = mRatio.load();

    float gainDb = 0.f;
    if (envDb > thresh) {
        gainDb = (thresh - envDb) * (1.f - 1.f / ratio);
    }

    return sample * dbToLinear(gainDb) * mMakeup.load();
}
