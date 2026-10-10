#include "BiquadFilter.h"
#include <cmath>

static constexpr float PI = 3.14159265358979323846f;

void BiquadFilter::setCoeffs(float b0_, float b1_, float b2_, float a0, float a1_, float a2_) {
    b0 = b0_ / a0;  b1 = b1_ / a0;  b2 = b2_ / a0;
    a1 = a1_ / a0;  a2 = a2_ / a0;
}

float BiquadFilter::process(float x) {
    float y = b0 * x + z1;
    z1 = b1 * x - a1 * y + z2;
    z2 = b2 * x - a2 * y;
    return y;
}

void BiquadFilter::reset() { z1 = z2 = 0.f; }

BiquadFilter BiquadFilter::makePeak(float freq, float gainDb, float q, float sr) {
    BiquadFilter f;
    float A  = std::pow(10.f, gainDb / 40.f);
    float w0 = 2.f * PI * freq / sr;
    float cw = std::cos(w0), sw = std::sin(w0);
    float alpha = sw / (2.f * q);
    f.setCoeffs(1 + alpha * A, -2 * cw, 1 - alpha * A,
                1 + alpha / A, -2 * cw, 1 - alpha / A);
    return f;
}

BiquadFilter BiquadFilter::makeLowShelf(float freq, float gainDb, float q, float sr) {
    BiquadFilter f;
    float A  = std::pow(10.f, gainDb / 40.f);
    float w0 = 2.f * PI * freq / sr;
    float cw = std::cos(w0), sw = std::sin(w0);
    float alpha = sw / 2.f * std::sqrt((A + 1.f/A) * (1.f/q - 1.f) + 2.f);
    float sq = 2.f * std::sqrt(A) * alpha;
    f.setCoeffs( A*((A+1)-(A-1)*cw+sq),  2*A*((A-1)-(A+1)*cw),    A*((A+1)-(A-1)*cw-sq),
                    (A+1)+(A-1)*cw+sq,  -2*((A-1)+(A+1)*cw),          (A+1)+(A-1)*cw-sq);
    return f;
}

BiquadFilter BiquadFilter::makeHighShelf(float freq, float gainDb, float q, float sr) {
    BiquadFilter f;
    float A  = std::pow(10.f, gainDb / 40.f);
    float w0 = 2.f * PI * freq / sr;
    float cw = std::cos(w0), sw = std::sin(w0);
    float alpha = sw / 2.f * std::sqrt((A + 1.f/A) * (1.f/q - 1.f) + 2.f);
    float sq = 2.f * std::sqrt(A) * alpha;
    f.setCoeffs( A*((A+1)+(A-1)*cw+sq), -2*A*((A-1)+(A+1)*cw),    A*((A+1)+(A-1)*cw-sq),
                    (A+1)-(A-1)*cw+sq,   2*((A-1)-(A+1)*cw),          (A+1)-(A-1)*cw-sq);
    return f;
}

BiquadFilter BiquadFilter::makeLowPass(float freq, float q, float sr) {
    BiquadFilter f;
    float w0 = 2.f * PI * freq / sr;
    float cw = std::cos(w0), sw = std::sin(w0);
    float alpha = sw / (2.f * q);
    f.setCoeffs((1-cw)/2, 1-cw, (1-cw)/2, 1+alpha, -2*cw, 1-alpha);
    return f;
}

BiquadFilter BiquadFilter::makeHighPass(float freq, float q, float sr) {
    BiquadFilter f;
    float w0 = 2.f * PI * freq / sr;
    float cw = std::cos(w0), sw = std::sin(w0);
    float alpha = sw / (2.f * q);
    f.setCoeffs((1+cw)/2, -(1+cw), (1+cw)/2, 1+alpha, -2*cw, 1-alpha);
    return f;
}

BiquadFilter BiquadFilter::makeNotch(float freq, float q, float sr) {
    BiquadFilter f;
    float w0 = 2.f * PI * freq / sr;
    float cw = std::cos(w0), sw = std::sin(w0);
    float alpha = sw / (2.f * q);
    f.setCoeffs(1, -2*cw, 1, 1+alpha, -2*cw, 1-alpha);
    return f;
}
