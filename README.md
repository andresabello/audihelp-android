# Audihelp Android

Audihelp is an Android hearing-assistance app that processes audio in real-time using a native C++ DSP engine (via [Oboe](https://github.com/google/oboe)) and exposes a Kotlin/Compose UI for hearing tests, live audio enhancement, and DSP preset management.

## Features

- **Live audio processing** — low-latency capture and playback via AAudio exclusive mode, routed through a configurable DSP chain (parametric EQ, biquad filters, compressor, limiter, amplitude modulator, frequency shifter, tone generator)
- **USB audio support** — auto-detects USB microphones (e.g. DJI Mic Mini) on plug-in
- **Hearing test** — on-device audiogram assessment that maps results to a personalised DSP preset
- **DSP Lab** — manual preset builder with per-band EQ controls
- **Profile & audiogram** — stores hearing history locally (Room) and syncs with a backend API
- **Google OAuth** — authentication via Chrome Custom Tabs with a deep-link callback (`audihelp://auth/callback`)

## Tech stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM, Hilt DI, ViewModel |
| Audio engine | C++17, Oboe, AAudio |
| Networking | Retrofit + Moshi |
| Local storage | Room, DataStore |
| Build | Gradle (Kotlin DSL), CMake 3.22, NDK 27 |
| Min SDK | 26 (Android 8.0) |

## Prerequisites

- Android Studio Hedgehog or later
- Android NDK **27.2.12479018** (install via SDK Manager → SDK Tools → NDK)
- CMake **3.22.1** (install via SDK Manager → SDK Tools → CMake)
- JDK 17

## Getting started

1. Clone the repo and open it in Android Studio.
2. Create `local.properties` in the project root if it does not already exist and set your SDK path:
   ```
   sdk.dir=/path/to/your/Android/sdk
   ```
3. Sync Gradle, then build and run on a device or emulator (API 26+).

> **Dev shortcut:** `LOGIN_ENABLED` is set to `false` in `defaultConfig`, so the app launches directly into the main flow without requiring credentials.

## Running the tests

### Unit tests (JVM)

```bash
./gradlew test
```

Results are written to `app/build/reports/tests/testDebugUnitTest/`.

### Instrumented tests (requires a connected device or emulator)

```bash
./gradlew connectedAndroidTest
```

Results are written to `app/build/reports/androidTests/connected/`.

### Single test class

```bash
# Unit
./gradlew testDebugUnitTest --tests "com.audihelp.app.ExampleUnitTest"

# Instrumented
./gradlew connectedDebugAndroidTest --tests "com.audihelp.app.ExampleInstrumentedTest"
```
