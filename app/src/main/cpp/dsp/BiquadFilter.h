#pragma once
#include <cmath>

enum class FilterType { Peak, LowShelf, HighShelf, LowPass, HighPass, Notch };

class BiquadFilter {
public:
    BiquadFilter() = default;

    static BiquadFilter makePeak(float freq, float gainDb, float q, float sampleRate);
    static BiquadFilter makeLowShelf(float freq, float gainDb, float q, float sampleRate);
    static BiquadFilter makeHighShelf(float freq, float gainDb, float q, float sampleRate);
    static BiquadFilter makeLowPass(float freq, float q, float sampleRate);
    static BiquadFilter makeHighPass(float freq, float q, float sampleRate);
    static BiquadFilter makeNotch(float freq, float q, float sampleRate);

    float process(float x);
    void reset();

private:
    float b0{1}, b1{0}, b2{0}, a1{0}, a2{0};
    float z1{0}, z2{0};

    void setCoeffs(float b0, float b1, float b2, float a0, float a1, float a2);
};
