#include <jni.h>
#include <cstdint>
#include "../AudioEngine.h"

static AudioEngine* toEngine(jlong handle) {
    return reinterpret_cast<AudioEngine*>(static_cast<intptr_t>(handle));
}

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_audihelp_app_audio_AudioEngineController_nativeCreate(JNIEnv*, jobject) {
    return static_cast<jlong>(reinterpret_cast<intptr_t>(new AudioEngine()));
}

JNIEXPORT void JNICALL
Java_com_audihelp_app_audio_AudioEngineController_nativeDestroy(JNIEnv*, jobject, jlong handle) {
    delete toEngine(handle);
}

JNIEXPORT jboolean JNICALL
Java_com_audihelp_app_audio_AudioEngineController_nativeStartLiveMode(JNIEnv*, jobject, jlong handle,
    jint sampleRate, jint framesPerBurst)
{
    return toEngine(handle)->startLiveMode(sampleRate, framesPerBurst);
}

JNIEXPORT void JNICALL
Java_com_audihelp_app_audio_AudioEngineController_nativeStopLiveMode(JNIEnv*, jobject, jlong handle) {
    toEngine(handle)->stopLiveMode();
}

JNIEXPORT void JNICALL
Java_com_audihelp_app_audio_AudioEngineController_nativeStartTone(JNIEnv*, jobject, jlong handle,
    jfloat frequencyHz, jfloat levelDbfs, jint patternType)
{
    toEngine(handle)->startTone(frequencyHz, levelDbfs, patternType);
}

JNIEXPORT void JNICALL
Java_com_audihelp_app_audio_AudioEngineController_nativeStopTone(JNIEnv*, jobject, jlong handle) {
    toEngine(handle)->stopTone();
}

JNIEXPORT void JNICALL
Java_com_audihelp_app_audio_AudioEngineController_nativeSetEqBand(JNIEnv*, jobject, jlong handle,
    jint bandIndex, jfloat freq, jfloat gainDb, jfloat q, jint filterType)
{
    toEngine(handle)->setEqBand(bandIndex, freq, gainDb, q, filterType, 48000.f);
}

JNIEXPORT void JNICALL
Java_com_audihelp_app_audio_AudioEngineController_nativeSetEqBandCount(JNIEnv*, jobject, jlong handle, jint count) {
    toEngine(handle)->setEqBandCount(count);
}

JNIEXPORT void JNICALL
Java_com_audihelp_app_audio_AudioEngineController_nativeSetCompressor(JNIEnv*, jobject, jlong handle,
    jfloat thresholdDbfs, jfloat ratio, jfloat attackMs, jfloat releaseMs, jfloat makeupGainDb)
{
    toEngine(handle)->setCompressor(thresholdDbfs, ratio, attackMs, releaseMs, makeupGainDb);
}

JNIEXPORT void JNICALL
Java_com_audihelp_app_audio_AudioEngineController_nativeSetLimiterCeiling(JNIEnv*, jobject, jlong handle, jfloat ceilingDbfs) {
    toEngine(handle)->setLimiterCeiling(ceilingDbfs);
}

JNIEXPORT void JNICALL
Java_com_audihelp_app_audio_AudioEngineController_nativeSetFreqShift(JNIEnv*, jobject, jlong handle,
    jboolean enabled, jfloat shiftHz)
{
    toEngine(handle)->setFreqShift(enabled, shiftHz);
}

JNIEXPORT void JNICALL
Java_com_audihelp_app_audio_AudioEngineController_nativeSetModulation(JNIEnv*, jobject, jlong handle,
    jboolean enabled, jfloat depth, jfloat rateHz)
{
    toEngine(handle)->setModulation(enabled, depth, rateHz);
}

JNIEXPORT void JNICALL
Java_com_audihelp_app_audio_AudioEngineController_nativeSetMasterGain(JNIEnv*, jobject, jlong handle, jfloat gainDb) {
    toEngine(handle)->setMasterGain(gainDb);
}

JNIEXPORT jfloat JNICALL
Java_com_audihelp_app_audio_AudioEngineController_nativeGetInputLevelDbfs(JNIEnv*, jobject, jlong handle) {
    return toEngine(handle)->getInputLevelDbfs();
}

JNIEXPORT jfloat JNICALL
Java_com_audihelp_app_audio_AudioEngineController_nativeGetOutputLevelDbfs(JNIEnv*, jobject, jlong handle) {
    return toEngine(handle)->getOutputLevelDbfs();
}

} // extern "C"
