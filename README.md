<div align="center">

# ⏱️ Focentra

### *Distraction-Free Offline Study Timer & Deep Work Analytics for Android*

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Offline First](https://img.shields.io/badge/Architecture-100%25_Offline_First-00897B?style=for-the-badge&logo=sqlite&logoColor=white)](#-privacy--offline-first-guarantee)
[![CI](https://img.shields.io/github/actions/workflow/status/NiloyMitra/Focentra/android-ci.yml?branch=main&style=for-the-badge&logo=githubactions&logoColor=white&label=CI)](https://github.com/NiloyMitra/Focentra/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-F59E0B?style=for-the-badge)](LICENSE)
[![Latest Release](https://img.shields.io/github/v/release/NiloyMitra/Focentra?style=for-the-badge&color=8B5CF6&label=Release)](https://github.com/NiloyMitra/Focentra/releases)

<br/>

**Focentra** is an open-source, privacy-first personal productivity and study timer application crafted for Android. Designed from the ground up for students, researchers, and engineers, Focentra empowers you to build laser-focused study habits with powerful interval timers, ambient soundscapes, multi-period analytics, and visual heatmaps—**without requiring an account, internet connection, or cloud subscription.**

<br/>

[📥 Download Latest APK](#-download-focentra) • [✨ Key Features](#-key-features) • [🏛️ Architecture](#-project-architecture) • [🛠️ Developer Setup](#-setup-instructions-for-developers) • [🤝 Contributing](#-contributing)

---

</div>

## 📖 Table of Contents
- [✨ Why Focentra?](#-why-focentra)
- [🚀 Key Features](#-key-features)
- [📸 Screenshots & Visual Preview](#-screenshots--visual-preview)
- [📥 Download Focentra](#-download-focentra)
  - [Latest APK Release](#latest-apk-release)
  - [Installation Guide](#installation-guide)
- [🔒 Privacy & Offline-First Guarantee](#-privacy--offline-first-guarantee)
- [🏛️ Project Architecture](#-project-architecture)
- [💻 Tech Stack & Libraries](#-tech-stack--libraries)
- [🗄️ Local Database & Storage Model](#-local-database--storage-model)
- [🔄 Backup, Import & Export](#-backup-import--export)
- [🛠️ Setup Instructions for Developers](#-setup-instructions-for-developers)
  - [Prerequisites](#prerequisites)
  - [Cloning and Building](#cloning-and-building)
- [🧪 Testing & Quality Assurance](#-testing--quality-assurance)
- [🤖 GitHub Actions & CI/CD Pipeline](#-github-actions--cicd-pipeline)
- [🗺️ Roadmap](#-roadmap)
- [📝 Changelog](#-changelog)
- [🐛 Reporting Issues & Feature Requests](#-reporting-issues--feature-requests)
- [🤝 Contributing](#-contributing)
- [📄 License](#-license)
- [👨‍💻 Author & Developer](#-author--developer)
- [📬 Contact & Community](#-contact--community)

---

## 💡 Why Focentra?

Modern productivity apps are often bloated with mandatory sign-ins, intrusive push notifications, aggressive subscription paywalls, and persistent cloud tracking. 

**Focentra takes a fundamentally different approach:**
- **Zero Friction**: Launch the app and start studying immediately. No registration, no email verification, no logins.
- **True Ownership**: All study logs, notes, productivity ratings, and streaks reside 100% locally on your device in an encrypted Room SQLite database.
- **Deep Work Focus**: Flexible study modes (Countdown, Stopwatch, and Pomodoro) combined with built-in ambient white noise and distraction tracking.
- **Actionable Insights**: GitHub-style 16-week heatmap matrices, 24-hour concentration curves, and algorithmic Focus Scores help you uncover your peak study hours.

---

## 🚀 Key Features

| Category | Features Included |
| :--- | :--- |
| ⏱️ **Timer Engine** | • **3 Study Modes**: Countdown timer with presets, open-ended Stopwatch, and structured multi-cycle Pomodoro.<br/>• **Background Service**: Uninterrupted session tracking via Android Foreground Service with interactive notifications.<br/>• **Distraction Logging**: One-tap distraction counter with tags to measure focus friction.<br/>• **Ambient Soundscape**: Built-in sound generator (Rain, Cafe, White Noise, Stream, Forest, Ambient). |
| 🌌 **Immersive Focus** | • **Full-Screen Focus Mode**: Minimalist AMOLED dark canvas with flip-clock aesthetics.<br/>• **Anti-Burn-In Protection**: Subtle pixel shifting for OLED displays.<br/>• **Wake-Lock Management**: Keeps the screen awake during active deep work blocks. |
| 📚 **Subjects & Topics** | • **Hierarchical Organization**: Organize sessions by subject and granular sub-topics.<br/>• **Custom Aesthetics**: Associate custom Material symbols and hex color palettes.<br/>• **Target Velocity**: Set target hours per subject and monitor progress rings. |
| 📊 **Analytics & Heatmap** | • **Multi-Period Filtering**: Aggregate data across Today, Yesterday, This Week, Last Week, This Month, Last Month, This Year, and All Time.<br/>• **16-Week Heatmap Grid**: Visual GitHub-style activity grid measuring daily study intensity.<br/>• **Focus Score ($0 - 100$)**: Algorithmic score evaluating productivity ratings, pause ratio, and distraction rates.<br/>• **Peak Hour Distribution**: 24-hour hourly curve highlighting your optimal cognitive window. |
| 🏆 **Habits & Badges** | • **Streak Engine**: Daily active streak counter with longest streak milestones.<br/>• **Unlockable Achievements**: Earn milestone badges (First Step, 10-Hour Club, Night Owl, Weekend Warrior, Consistency Champion). |
| 📅 **History & Calendar** | • **Monthly Heatmap Calendar**: Interactive calendar view detailing study volume per day.<br/>• **Session Inspector**: Searchable session history log with detailed metrics and notes. |
| 🛡️ **Data Portability** | • **Full JSON Backup & Restore**: One-tap backup with clipboard paste integration.<br/>• **CSV Spreadsheet Export**: Export raw data formatted for Microsoft Excel, Google Sheets, and Notion. |
| 🎨 **UI & Customization** | • **Material Design 3**: Dynamic color matching Android 12+ wallpaper palettes.<br/>• **Study Reminders**: Customizable daily reminders with weekday/weekend repeat schedules.<br/>• **Edge-to-Edge Experience**: Modern floating navigation bar with responsive tactile haptics. |

---

## 📸 Screenshots & Visual Preview

<!-- Visual preview layout for Focentra -->
<div align="center">

| Dashboard & Goal Progress | Active Focus Timer | Immersive OLED Mode |
| :---: | :---: | :---: |
| <img src="docs/screenshots/dashboard.png" width="240" alt="Dashboard Screen" onerror="this.src='https://placehold.co/1080x2400/1e1b4b/ffffff?text=Dashboard+Screen';" /> | <img src="docs/screenshots/timer.png" width="240" alt="Timer Screen" onerror="this.src='https://placehold.co/1080x2400/1e1b4b/ffffff?text=Timer+Screen';" /> | <img src="docs/screenshots/fullscreen_focus.png" width="240" alt="Full Screen Focus" onerror="this.src='https://placehold.co/1080x2400/1e1b4b/ffffff?text=Full+Screen+Focus';" /> |

| Multi-Period Analytics | Study Heatmap & History | Subjects & Targets |
| :---: | :---: | :---: |
| <img src="docs/screenshots/statistics.png" width="240" alt="Analytics Screen" onerror="this.src='https://placehold.co/1080x2400/1e1b4b/ffffff?text=Analytics+Screen';" /> | <img src="docs/screenshots/calendar.png" width="240" alt="Calendar Screen" onerror="this.src='https://placehold.co/1080x2400/1e1b4b/ffffff?text=Calendar+Screen';" /> | <img src="docs/screenshots/subjects.png" width="240" alt="Subjects Screen" onerror="this.src='https://placehold.co/1080x2400/1e1b4b/ffffff?text=Subjects+Screen';" /> |

</div>

> ℹ️ *For screenshot contribution specs and guidelines, see [`docs/screenshots/README.md`](docs/screenshots/README.md).*

---

## 📥 Download Focentra

### Latest APK Release
You can download the latest pre-compiled Android APK directly from the [GitHub Releases page](https://github.com/NiloyMitra/Focentra/releases).

| Release Type | Channel | Minimum OS | Download Link |
| :--- | :--- | :--- | :--- |
| **Stable APK (v1.0.0)** | GitHub Releases | Android 7.0+ (API 24) | [**Download `apk-release.apk`**](https://github.com/NiloyMitra/Focentra/releases/latest) |
| **Debug Build (CI Artifact)** | GitHub Actions | Android 7.0+ (API 24) | [**Download from Actions**](https://github.com/NiloyMitra/Focentra/actions) |

### Installation Guide
1. Download `apk-release.apk` on your Android device from the link above.
2. Open your device's **Downloads** folder and tap the APK file.
3. If prompted, enable **"Install unknown apps"** or **"Allow from this source"** in your device Settings.
4. Tap **Install** and launch **Focentra**.

---

## 🔒 Privacy & Offline-First Guarantee

Focentra is engineered with an unwavering commitment to digital privacy:

```text
┌─────────────────────────────────────────────────────────────┐
│                 FOCENTRA PRIVACY CONTRACT                   │
├─────────────────────────────────────────────────────────────┤
│  ✓  NO Account or Login Required                            │
│  ✓  NO Cloud Database Synchronization                       │
│  ✓  NO Analytics SDKs, Ad Networks, or Third-Party Trackers │
│  ✓  NO Background Data Transmissions                        │
│  ✓  100% of your notes, metrics, and timers stay on-device  │
└─────────────────────────────────────────────────────────────┘
```

Your data is stored exclusively inside your device's sandboxed SQLite database via AndroidX Room. You maintain total ownership of your study history and can export it as raw JSON or CSV at any time.

---

## 🏛️ Project Architecture

Focentra follows **Clean Architecture** and **MVVM** principles with **Unidirectional Data Flow (UDF)**:

```text
                               ┌───────────────────────────┐
                               │       User Interaction    │
                               └─────────────┬─────────────┘
                                             │
                                             ▼
                               ┌───────────────────────────┐
                               │   Jetpack Compose UI      │
                               │  (Material 3 Composables) │
                               └─────────────▲─────────────┘
                                             │ StateFlow
                                             ▼
                               ┌───────────────────────────┐
                               │       MainViewModel       │
                               │   (State & Orchestration) │
                               └─────────────▲─────────────┘
                                             │
                      ┌──────────────────────┴──────────────────────┐
                      ▼                                             ▼
       ┌─────────────────────────────┐               ┌─────────────────────────────┐
       │   Timer / Stats Engines     │               │       StudyRepository       │
       │ (Pure Algorithmic Logic)    │               │  (Single Source of Truth)   │
       └─────────────────────────────┘               └──────────────┬──────────────┘
                                                                    │
                                             ┌──────────────────────┴──────────────────────┐
                                             ▼                                             ▼
                              ┌─────────────────────────────┐               ┌─────────────────────────────┐
                              │    Room SQLite Database     │               │     Android DataStore       │
                              │ (Sessions, Subjects, Badges)│               │    (User Preferences)       │
                              └─────────────────────────────┘               └─────────────────────────────┘
```

> 📖 *For an in-depth architectural breakdown, check out [`docs/architecture/ARCHITECTURE.md`](docs/architecture/ARCHITECTURE.md).*

---

## 💻 Tech Stack & Libraries

Focentra is built with cutting-edge Android development technologies:

- **Language**: [Kotlin 2.0+](https://kotlinlang.org/) (Coroutines, Flow, Serialization)
- **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with [Material Design 3](https://m3.material.io/)
- **Architecture**: MVVM, Clean Architecture, Unidirectional Data Flow
- **Local Database**: [AndroidX Room 2.6+](https://developer.android.com/training/data-storage/room) with SQLite
- **Dependency Processing**: [KSP (Kotlin Symbol Processing)](https://kotlinlang.org/docs/ksp-overview.html)
- **Background Execution**: Android Foreground Service (`StudyTimerService`) & `WakeLock`
- **Preferences**: [AndroidX DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore)
- **Serialization**: [Moshi](https://github.com/square/moshi) & `org.json`
- **Unit & UI Testing**: JUnit 4, Robolectric, Kotlinx Coroutines Test, Roborazzi

---

## 🗄️ Local Database & Storage Model

Focentra organizes local data into structured Room entities:

```text
Room Database: "focentra_study_db"
├── study_sessions     (Session durations, paused seconds, productivity rating, distractions, notes, mood)
├── subjects           (Custom subjects, color hex values, icon identifiers, target hours)
├── topics             (Sub-topics mapped to parent subjects)
├── timer_presets      (Custom countdown & pomodoro presets)
├── reminders          (Daily study alarm configurations and repeat days)
└── achievements       (Unlocked milestone records and timestamps)
```

---

## 🔄 Backup, Import & Export

Focentra provides full data portability under **Settings > Data & Backup**:

1. **Full JSON Backup**:
   - Generates an immutable snapshot containing all sessions, subjects, topics, and presets.
   - Restorable via direct file import or pasting JSON strings from your clipboard.
2. **Universal CSV Spreadsheet Export**:
   - Formats study history into clean comma-separated values compatible with **Microsoft Excel**, **Google Sheets**, and **Notion**.
   - Fields exported: `ID, Date, Subject, Topic, Duration (Minutes), Paused (Sec), Distractions, Productivity Rating, Mood, Notes`.

---

## 🛠️ Setup Instructions for Developers

### Prerequisites
- **JDK**: Java Development Kit 17 (Temurin recommended)
- **Android Studio**: Android Studio Ladybug (2024.2.1) or newer
- **Android SDK**: Android API Level 36 (Minimum SDK 24, Target SDK 36)
- **Git**: Version 2.30+

### Cloning and Building

1. **Clone the repository**:
   ```bash
   git clone https://github.com/NiloyMitra/Focentra.git
   cd Focentra
   ```

2. **Set up environment configuration**:
   ```bash
   cp .env.example .env
   ```

3. **Build the project via Gradle**:
   ```bash
   # Linux / macOS
   ./gradlew assembleDebug

   # Windows
   gradlew.bat assembleDebug
   ```

4. **Run Unit Tests**:
   ```bash
   ./gradlew testDebugUnitTest
   ```

> 📖 *For additional development tips, see [`docs/development/DEVELOPMENT.md`](docs/development/DEVELOPMENT.md).*

---

## 🧪 Testing & Quality Assurance

Focentra utilizes unit tests and Robolectric tests to verify core functionality:

```bash
# Execute standard JVM unit tests (StatisticsEngine, TimerEngine, Repositories)
./gradlew testDebugUnitTest

# Run tests with detailed log output
./gradlew testDebugUnitTest --info
```

Test coverage focuses on:
- **`StatisticsEngineTest`**: Period boundary calculations, focus score formulation, and streak verification.
- **`TimerEngineTest`**: Countdown state machines and pause tracking.
- **`StudyRepositoryTest`**: JSON import deserialization and CSV format generation.

---

## 🤖 GitHub Actions & CI/CD Pipeline

The repository is equipped with automated CI/CD workflows:

- **`.github/workflows/android-ci.yml`**:
  - Automatically triggers on pushes and pull requests to `main` and `develop`.
  - Sets up JDK 17, caches Gradle artifacts, executes `./gradlew testDebugUnitTest`, and compiles `./gradlew assembleDebug`.
- **`.github/workflows/build-release-apk.yml`**:
  - Automatically triggers when pushing a version tag (e.g. `v1.0.0`) or on manual workflow dispatch.
  - Builds and signs the release APK, stores artifacts, and attaches binaries to GitHub Releases.

---

## 🗺️ Roadmap

- [x] **v1.0.0 - Core Launch**: Countdown, Stopwatch, and Pomodoro timer modes.
- [x] **v1.0.0 - Advanced Analytics**: 16-week study heatmap, Focus Score, and hourly breakdown.
- [x] **v1.0.0 - Soundscapes & OLED Mode**: Ambient audio player and full-screen flip clock.
- [x] **v1.0.0 - Data Portability**: Full JSON backup/restore and CSV spreadsheet exporter.
- [ ] **v1.1.0 - Glance AppWidget**: Home-screen study widget for one-tap timer launches.
- [ ] **v1.2.0 - Tag Filtering**: Deep analytics filtering by customizable tags (`#exam`, `#reading`).
- [ ] **v2.0.0 - Wear OS Module**: Standalone Wear OS companion app.

> 🗺️ *See the complete [`ROADMAP.md`](ROADMAP.md) for upcoming milestones and ideas.*

---

## 📝 Changelog

Detailed release notes and historical changes are maintained in [`CHANGELOG.md`](CHANGELOG.md).

---

## 🐛 Reporting Issues & Feature Requests

Found a bug or have a suggestion?
- **Bug Reports**: Open an issue using our [Bug Report Template](.github/ISSUE_TEMPLATE/bug_report.md).
- **Feature Requests**: Propose ideas using our [Feature Request Template](.github/ISSUE_TEMPLATE/feature_request.md).
- **Security Inquiries**: Please review our [Security Policy](SECURITY.md) for responsible private disclosure.

---

## 🤝 Contributing

Contributions of all kinds are welcome! Whether you are fixing a bug, adding a new soundscape, improving documentation, or polishing the UI:

1. Read our [Code of Conduct](CODE_OF_CONDUCT.md).
2. Check out the [Contributing Guide](CONTRIBUTING.md) for branch naming conventions and coding standards.
3. Submit a Pull Request using our [PR Template](.github/pull_request_template.md).

---

## 📄 License

Focentra is released as open-source software under the permissive **[MIT License](LICENSE)**.

```text
Copyright (c) 2026 Niloy Mitra

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction...
```

---

## 👨‍💻 Author & Developer

<table align="center">
  <tr>
    <td align="center">
      <br />
      <b>Niloy Mitra</b><br />
      <sub>Student Developer & Aspiring Software Engineer</sub><br />
      <a href="https://github.com/NiloyMitra">GitHub Profile</a>
    </td>
  </tr>
</table>

**Niloy Mitra** is a student developer and aspiring Software Engineer passionate about crafting clean, robust, and offline-first mobile applications with Kotlin and Jetpack Compose.

---

## 📬 Contact & Community

- **Project Repository**: [https://github.com/NiloyMitra/Focentra](https://github.com/NiloyMitra/Focentra)
- **Bug Reports & Issues**: [GitHub Issues](https://github.com/NiloyMitra/Focentra/issues)
- **Community Discussions**: [GitHub Discussions](https://github.com/NiloyMitra/Focentra/discussions)
- **Developer Email**: `niloymitra889@gmail.com`

---

<div align="center">

*Built with ❤️ and Kotlin for focused learners worldwide.*

**[⬆ Back to Top](#️-focentra)**

</div>
