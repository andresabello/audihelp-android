#include "ParametricEQ.h"

void ParametricEQ::setBand(int index, float freq, float gainDb, float q, FilterType type, float sampleRate) {
    if (index < 0 || index >= MAX_EQ_BANDS) return;
    std::lock_guard<std::mutex> lock(mMutex);
    mParams[index] = {freq, gainDb, q, type, true};
    switch (type) {
        case FilterType::Peak:      mFilters[index] = BiquadFilter::makePeak(freq, gainDb, q, sampleRate);      break;
        case FilterType::LowShelf:  mFilters[index] = BiquadFilter::makeLowShelf(freq, gainDb, q, sampleRate);  break;
        case FilterType::HighShelf: mFilters[index] = BiquadFilter::makeHighShelf(freq, gainDb, q, sampleRate); break;
        case FilterType::LowPass:   mFilters[index] = BiquadFilter::makeLowPass(freq, q, sampleRate);           break;
        case FilterType::HighPass:  mFilters[index] = BiquadFilter::makeHighPass(freq, q, sampleRate);          break;
        case FilterType::Notch:     mFilters[index] = BiquadFilter::makeNotch(freq, q, sampleRate);             break;
    }
}

void ParametricEQ::setBandCount(int n) {
    std::lock_guard<std::mutex> lock(mMutex);
    mBandCount = (n < 0) ? 0 : (n > MAX_EQ_BANDS ? MAX_EQ_BANDS : n);
}

void ParametricEQ::reset() {
    std::lock_guard<std::mutex> lock(mMutex);
    for (auto& f : mFilters) f.reset();
}

float ParametricEQ::process(float sample) {
    // Lock-free read of bandCount (benign race — worst case off-by-one band)
    int n = mBandCount;
    for (int i = 0; i < n; ++i) {
        if (mParams[i].enabled) sample = mFilters[i].process(sample);
    }
    return sample;
}
