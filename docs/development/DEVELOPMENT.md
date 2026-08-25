# Development Guide

This guide covers setting up your local development environment, compiling the project, running automated unit tests, and debugging **Focentra**.

---

## 🛠️ Workstation Prerequisites

Ensure you have the following software installed on your machine:
- **Operating System**: macOS, Linux, or Windows (WSL2 recommended for Windows)
- **Java Development Kit**: JDK 17 (Eclipse Temurin or OpenJDK 17)
- **Android Studio**: Android Studio Ladybug (2024.2.1) or Meerkat+
- **Android SDK Components**:
  - Android SDK Platform 36 (`compileSdk 36`)
  - Android SDK Build-Tools 36.0.0
  - Android NDK (if building native modules)

---

## ⚙️ Initial Setup

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/Niloy-Track-Dev/Focentra.git
   cd Focentra
   ```

2. **Prepare Environment File**:
   Copy the example environment configuration file:
   ```bash
   cp .env.example .env
   ```

3. **Open in Android Studio**:
   - Open Android Studio.
   - Click **File** > **Open** and select the root directory of the project.
   - Wait for Gradle sync and indexing to finish.

---

## 🏗️ Gradle Tasks

Execute standard Gradle tasks using the wrapper from the project root:

### 1. Compiling & Building APKs
```bash
# Build Debug APK
./gradlew assembleDebug

# Build Release APK (requires signing credentials or uses fallback keystore)
./gradlew assembleRelease
```
The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

### 2. Running Automated Tests
```bash
# Run JVM Unit Tests
./gradlew testDebugUnitTest

# Run Tests with Stacktrace
./gradlew testDebugUnitTest --stacktrace --info
```

### 3. Running Lint and Static Analysis
```bash
# Run Android Lint
./gradlew lintDebug
```

---

## 📱 Running on Device or Emulator

1. In Android Studio, select your target device or emulator from the device toolbar.
2. Select the `app` run configuration.
3. Click **Run** (or press `Shift + F10`).
4. Grant runtime notification permissions when prompted to enable foreground timer alerts.

---

## 🧪 Testing Guidelines

- **Unit Tests**: Place JVM unit tests in `app/src/test/java/com/example/`.
- **Frameworks Used**:
  - JUnit 4
  - Robolectric (for Android context and resource verification)
  - Kotlin Coroutines Test (`runTest`, `StandardTestDispatcher`)
- **Key Modules to Test**:
  - `StatisticsEngine`: Period date filtering, focus score bounds, peak hour detection.
  - `TimerEngine`: State transitions, remaining seconds countdown, pause accumulation.
  - `StudyRepository`: JSON import parsing, CSV formatting, streak calculations.
