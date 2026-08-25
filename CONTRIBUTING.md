# Contributing to Focentra

Thank you for your interest in contributing to **Focentra**! We welcome contributions from developers, designers, and students of all experience levels.

This guide will walk you through setting up your environment, adhering to code conventions, and submitting high-quality pull requests.

---

## 🧭 Table of Contents
1. [Code of Conduct](#code-of-conduct)
2. [Getting Started](#getting-started)
   - [Prerequisites](#prerequisites)
   - [Forking and Cloning](#forking-and-cloning)
   - [Opening in Android Studio](#opening-in-android-studio)
3. [Development Workflow](#development-workflow)
   - [Branch Naming](#branch-naming)
   - [Commit Message Guidelines](#commit-message-guidelines)
4. [Coding & Architecture Guidelines](#coding--architecture-guidelines)
   - [Kotlin & Jetpack Compose](#kotlin--jetpack-compose)
   - [State Management](#state-management)
   - [Offline-First Principle](#offline-first-principle)
5. [Testing & Quality Assurance](#testing--quality-assurance)
6. [Submitting a Pull Request](#submitting-a-pull-request)
7. [Reporting Bugs & Requesting Features](#reporting-bugs--requesting-features)

---

## 📜 Code of Conduct
Please review our [Code of Conduct](CODE_OF_CONDUCT.md) before participating in this community. We strive to maintain a welcoming, respectful, and productive space for everyone.

---

## 🚀 Getting Started

### Prerequisites
- **JDK**: Java Development Kit 17 (Eclipse Temurin or OpenJDK recommended)
- **Android Studio**: Android Studio Ladybug (2024.2.1+) or newer
- **Android SDK**: Android SDK Platform 36 (compileSdk 36, minSdk 24)
- **Git**: Installed and configured on your machine

### Forking and Cloning
1. Fork the repository on GitHub: Click the **Fork** button in the top-right corner of [https://github.com/NiloyMitra/Focentra](https://github.com/NiloyMitra/Focentra).
2. Clone your fork locally:
   ```bash
   git clone https://github.com/<your-username>/Focentra.git
   cd Focentra
   ```
3. Add the upstream repository as a remote:
   ```bash
   git remote add upstream https://github.com/NiloyMitra/Focentra.git
   ```

### Opening in Android Studio
1. Launch Android Studio.
2. Select **Open** and select the cloned `Focentra` root folder.
3. Allow Gradle to sync dependencies and index the project.
4. Verify local configuration by building the project:
   - On Linux/macOS: `./gradlew assembleDebug`
   - On Windows: `gradlew.bat assembleDebug`

---

## 🔄 Development Workflow

### Branch Naming
Always create a new branch from `main` or `develop` before making changes. Use descriptive prefixes:

| Branch Type | Format | Example |
| ----------- | ------ | ------- |
| **Feature** | `feature/<short-desc>` | `feature/pomodoro-audio-cue` |
| **Bug Fix** | `fix/<short-desc>` | `fix/heatmap-month-offset` |
| **Refactor** | `refactor/<short-desc>` | `refactor/statistics-engine-flow` |
| **Docs** | `docs/<short-desc>` | `docs/update-architecture-diagram` |
| **CI/CD** | `ci/<short-desc>` | `ci/gradle-cache-optimization` |

Example:
```bash
git checkout -b feature/tag-filter-analytics
```

### Commit Message Guidelines
We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```text
<type>(<scope>): <short description in present tense>

[optional body explaining motivation and changes]

[optional footer referencing issues, e.g. Fixes #12]
```

**Allowed Types**:
- `feat`: A new user-facing feature
- `fix`: A bug fix
- `refactor`: Code change that neither fixes a bug nor adds a feature
- `perf`: A code change that improves performance
- `docs`: Documentation only changes
- `style`: Changes that do not affect the meaning of the code (formatting, missing semi-colons)
- `test`: Adding missing tests or correcting existing tests
- `chore`: Changes to build process, Gradle, CI, or auxiliary tools

---

## 🎨 Coding & Architecture Guidelines

### Kotlin & Jetpack Compose
- **Pure Jetpack Compose**: All UI must be written in declarative Jetpack Compose using Material Design 3 (`androidx.compose.material3`).
- **Edge-to-Edge**: Always design for edge-to-edge layouts using `WindowInsets` and `Scaffold`.
- **Accessibility**: Ensure all interactive elements have touch targets of at least `48.dp` and informative `contentDescription` attributes.
- **Compose TestTags**: Primary interactive components should include `Modifier.testTag("action_identifier")`.

### State Management
- Architecture follows **MVVM (Model-View-ViewModel)** with Unidirectional Data Flow (UDF).
- ViewModels expose read-only `StateFlow<T>` streams, and Composables observe them via `collectAsStateWithLifecycle()`.
- Repository classes mediate between ViewModels and the local Room SQLite Database.

### Offline-First Principle
- **No mandatory online services or accounts**: Focentra is strictly offline-first. All core features (timers, subjects, analytics, achievements, export) must work seamlessly without internet connectivity.
- Local data must always be persisted via **Room Database** (`AppDatabase.kt`).

---

## 🧪 Testing & Quality Assurance

Before submitting your changes, ensure all tests pass and your code compiles cleanly:

```bash
# Run unit tests
./gradlew testDebugUnitTest

# Compile debug APK
./gradlew assembleDebug
```

If you add new business logic (such as statistical algorithms, export formatters, or timer state machines), please write corresponding unit tests under `app/src/test/java/com/example/`.

---

## 📥 Submitting a Pull Request

1. Push your branch to your GitHub fork:
   ```bash
   git push origin feature/your-feature-name
   ```
2. Open a Pull Request against the `main` branch of the upstream repository.
3. Fill out the provided **Pull Request Template** completely:
   - State the problem solved or feature added.
   - List the changes made.
   - Include screenshots or GIFs if UI elements were modified.
   - Link any related issue numbers (e.g., `Fixes #42`).
4. Wait for CI checks to pass and engage positively in review discussions!

---

## 🐛 Reporting Bugs & Requesting Features
- **Bug Reports**: Use the [Bug Report Template](.github/ISSUE_TEMPLATE/bug_report.md) with reproduction steps and device details.
- **Feature Requests**: Use the [Feature Request Template](.github/ISSUE_TEMPLATE/feature_request.md) describing the user problem and proposed solution.
- **General Questions**: Use [GitHub Discussions](https://github.com/NiloyMitra/Focentra/discussions).

Thank you for helping make Focentra better! 🚀
