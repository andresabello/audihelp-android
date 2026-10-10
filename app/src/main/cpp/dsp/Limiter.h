#pragma once
#include <atomic>
#include <cmath>

class Limiter {
public:
    void setCeiling(float ceilingDbfs);
    float process(float sample);

private:
    std::atomic<float> mCeiling{0.94f}; // -0.5 dBFS default
};
