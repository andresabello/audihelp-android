#include "UsbAudioCapture.h"

void UsbAudioCapture::push(const float* data, int frameCount) {
    int tail = mTail.load(std::memory_order_relaxed);
    for (int i = 0; i < frameCount; ++i) {
        int next = (tail + 1) % CAPACITY;
        if (next == mHead.load(std::memory_order_acquire)) break; // full — drop
        mBuffer[tail] = data[i];
        tail = next;
    }
    mTail.store(tail, std::memory_order_release);
}

float UsbAudioCapture::pop() {
    int head = mHead.load(std::memory_order_relaxed);
    if (head == mTail.load(std::memory_order_acquire)) return 0.f; // empty
    float val = mBuffer[head];
    mHead.store((head + 1) % CAPACITY, std::memory_order_release);
    return val;
}

bool UsbAudioCapture::hasData() const {
    return mHead.load(std::memory_order_acquire) != mTail.load(std::memory_order_acquire);
}

void UsbAudioCapture::clear() {
    mHead.store(0, std::memory_order_release);
    mTail.store(0, std::memory_order_release);
}
