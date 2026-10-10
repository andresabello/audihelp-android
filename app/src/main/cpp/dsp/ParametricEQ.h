#pragma once
#include "BiquadFilter.h"
#include <atomic>
#include <array>
#include <mutex>

static constexpr int MAX_EQ_BANDS = 8;

struct EqBandParams {
    float freq{1000.f};
    float gainDb{0.f};
    float q{1.41f};
    FilterType type{FilterType::Peak};
    bool enabled{true};
};

class ParametricEQ {
public:
    void setBand(int index, float freq, float gainDb, float q, FilterType type, float sampleRate);
    void setBandCount(int n);
    void reset();

    float process(float sample);

private:
    std::array<BiquadFilter, MAX_EQ_BANDS> mFilters{};
    std::array<EqBandParams, MAX_EQ_BANDS> mParams{};
    std::mutex mMutex;
    int mBandCount{0};
};
