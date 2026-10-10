cmake_minimum_required(VERSION 3.22.1)
project(audihelp_engine VERSION 1.0)

# Oboe via Prefab (added as Gradle dependency)
find_package(oboe REQUIRED CONFIG)

set(AUDIHELP_SRC
    AudioEngine.cpp
    jni/AudioEngineJni.cpp
    dsp/BiquadFilter.cpp
    dsp/ParametricEQ.cpp
    dsp/Compressor.cpp
    dsp/Limiter.cpp
    dsp/FrequencyShifter.cpp
    dsp/AmplitudeModulator.cpp
    dsp/ToneGenerator.cpp
    usb/UsbAudioCapture.cpp
)

add_library(audihelp_engine SHARED ${AUDIHELP_SRC})

target_include_directories(audihelp_engine PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

target_link_libraries(audihelp_engine
    oboe::oboe
    android
    log
    atomic
)

target_compile_options(audihelp_engine PRIVATE
    -std=c++17
    -O2
    -ffast-math
    -DNDEBUG
    -Wall
)
