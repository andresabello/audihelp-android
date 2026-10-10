#pragma once
#include <atomic>
#include <array>
#include <cstring>

// Lock-free SPSC ring buffer for USB audio frames pushed from Kotlin.
// Producer: Kotlin UsbDeviceManager bulk-transfer thread (via JNI nativePushUsbAudio)
// Consumer: Oboe audio callback thread (onAudioReady)
class UsbAudioCapture {
public:
    static constexpr int CAPACITY = 8192; // float samples

    void push(const float* data, int frameCount);
    float pop();
    bool hasData() const;
    void clear();

private:
    std::array<float, CAPACITY> mBuffer{};
    std::atomic<int> mHead{0};
    std::atomic<int> mTail{0};
};
