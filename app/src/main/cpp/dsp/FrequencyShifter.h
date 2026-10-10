#pragma once
#include <atomic>
#include <array>
#include <cmath>

// Single-sideband frequency shifter using Hilbert filter pair + heterodyne.
// Uses an all-pass IIR approximation of the Hilbert transform (90° phase shift).
class FrequencyShifter {
public:
    void setParams(bool enabled, float shiftHz, float sampleRate);
    float process(float sample);

private:
    std::atomic<bool>  mEnabled{false};
    std::atomic<float> mPhaseInc{0.f};
    float mPhase{0.f};

    // Two-path all-pass Hilbert approximation (8 stages each)
    static constexpr int STAGES = 8;
    std::array<float, STAGES> mStateA{}, mStateB{};
    static const std::array<float, STAGES> kCoeffsA;
    static const std::array<float, STAGES> kCoeffsB;

    float processAllpassChain(float x, std::array<float, STAGES>& state,
                              const std::array<float, STAGES>& coeffs);
};
