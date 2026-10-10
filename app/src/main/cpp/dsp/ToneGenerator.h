#pragma once
#include <atomic>
#include <cmath>

enum class TonePattern { Sine = 0, Pulse = 1, AM = 2 };

class ToneGenerator {
public:
    void start(float frequencyHz, float levelDbfs, TonePattern pattern, float sampleRate);
    void stop();
    float next();
    bool isRunning() const { return mRunning.load(); }

private:
    std::atomic<bool>  mRunning{false};
    std::atomic<float> mAmplitude{0.f};
    std::atomic<float> mPhaseInc{0.f};
    std::atomic<int>   mPattern{0};
    float mPhase{0.f};
    float mPulsePhase{0.f};

    static constexpr float PI = 3.14159265358979323846f;
    static float dbToLinear(float db) { return std::pow(10.f, db / 20.f); }
};
