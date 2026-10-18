# Changelog

All notable changes to **Focentra** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [4.1.0] - 2026-10-17

- **Exam Goals Card Details**: Removed "Tap for details" text and directly displayed target note details on card.
- **Syllabus Chapter Edit & Confirmation Dialog**: Added Edit Chapter modal dialog and safety delete confirmation dialog for syllabus chapters in SubjectsScreen.
- **Add Reminder UI Fix**: Resolved text layout/wrapping issue for minute field in reminder dialog.
- **Version Bump**: Incremented `versionCode` to `20` and `versionName` to `4.1.0`.

## [4.0.0] - 2026-10-17

## [3.9.0] - 2026-10-17

### Added
- **Syllabus & Chapter Sequential Tracking System**: Add chapters to subjects with custom total class count input (e.g. 20 classes).
- **Sequential Class Unlocking**: Class N+1 is disabled until Class N is completed.
- **Revision Classes Requirement**: Chapter completion requires finishing all regular classes plus Revision Class 01 & Revision Class 02.
- **Exam Milestone Syllabus Integration**: Links chapters and sequential class progress directly to upcoming exam goals.

### Updated
- **Version Bump**: Incremented `versionCode` to `18` and `versionName` to `3.9.0`.

## [3.8.0] - 2026-10-17

### Added
- **AI Adaptive Exam Pacing & Re-balancing Engine**: Dynamically calculates re-balanced focus requirements and study pacing.
- **Cognitive Fatigue & Burnout Predictor**: Real-time brain energy meter (0-100%), recommended break duration, and burnout risk index.
- **Smart Focus Peak Window Advisor**: Analyses hourly productivity to recommend peak concentration study windows.
- **Subject Retention & Spaced Memory Decay Scale**: Predicts memory retention percentages based on time elapsed since last study session.

### Updated
- **Version Bump**: Incremented `versionCode` to `17` and `versionName` to `3.8.0`.

## [3.7.0] - 2026-10-17

### Added
- **Super Smooth UI & Database Optimization**: Added Room indices on `StudySessionEntity` (`startTime`, `subject`, `completionStatus`) for microsecond query response.
- **Background Dispatcher Offloading**: Shifted all ViewModel Flow calculations and filtering to `Dispatchers.Default`.
- **Exam Goals Card Details Area**: Utilized whitespace under "X Days Left" to display target dates and detail hints cleanly.
- **Full App State Backup & Restoration**: Verified 100% complete import/export of subjects, colors, descriptions, distractions, topics, goals, presets, reminders, and exam targets.

### Updated
- **Version Bump**: Incremented `versionCode` to `16` and `versionName` to `3.7.0`.

## [3.6.0] - 2026-10-17

### Added
- **Dynamic Progress Level Trend Curve Chart**: Live trend graph tracking focus minutes and scores over time.
- **Enhanced Exam Goals & Milestones Card UI**: Optimized card spacing to display target dates and detail hints.
- **Complete Application State Backup**: Full streaming JSON/CSV import and export of subjects, presets, goals, reminders, and exam targets.

### Updated
- **Version Bump**: Incremented `versionCode` to `15` and `versionName` to `3.6.0`.

### Planned
- Home screen widget for fast study timer launch
- Wear OS companion module support
- Additional custom soundscapes for background white noise

---

## [3.5.0] - 2026-08-28

### Added
- **Full App 12-Hour Time Format**: Standardized all date and time displays, reminder alarms, and history logs to 12-hour AM/PM format. Added AM/PM toggle selector to study reminder dialogs.
- **Redesigned Exam Goals & Milestones Detail Popup**:
  - Re-organized layout cleanly separating title, subject/topic, syllabus/description, progress bar, daily pace, and target date.
  - Added full **Edit Option** via `EditExamTargetDialog` allowing editing of Title, Subject, Target Hours, Days Left, and Syllabus/Notes directly from popup and milestone cards.
- **Sub-Screen Navigation Bar Fix**: Hidden floating bottom navigation bar on Study Presets screen (`NavigationTab.PRESETS`) to clean up sub-screen layout.
- **Enhanced APK Signing**: Explicitly enabled v1 and v2 signature schemes in release/debug build configurations to eliminate package installer legitimacy warnings.

### Changed
- **Version Bump**: Incremented `versionCode` to `14` and `versionName` to `3.5.0`.

---

## [3.4.0] - 2026-08-28

### Added
- **Manual Study Data Input System**: Added manual session entry dialog on TimerScreen to log subject, topic, duration, productivity rating, category, and notes directly.
- **Strict 4-Option Import-Export System**:
  - JSON Export (File)
  - JSON Import (File)
  - CSV Text Export (Share/Copy)
  - CSV Text Import (Paste/Import)
- **Large File Streaming & Performance Optimization**: Refactored JSON Export/Import and CSV Import to use `JsonWriter`, `JsonReader`, and chunked 500-record Room DB batch operations on `Dispatchers.IO` to process large files smoothly without OOM or UI freezing.
- **Study Preset Edit Capability**: Enabled editing existing presets with prefilled settings in `PresetsScreen`.
- **Character Limits & Details Popup**:
  - Added strict character limits and character counter indicators for Exam Goals & Milestones (Title max 50, Subject max 30, Notes max 200) and Study Presets (Title max 50, Topic max 40).
  - Added clean popup detail dialogs (`PresetDetailDialog` & `ExamMilestoneDetailDialog`) to display full untruncated notes and info cleanly.

### Removed
- **Flashcards System Completely**: Removed `FlashcardsScreen.kt`, navigation tab, dashboard cards, and ViewModel logic as requested.
- **All Themes Except Midnight Dark Theme**: Enforced Midnight Dark Theme as the sole default theme and removed theme preview selection cards from Settings.
- **Click Ripple Shadow Effects**: Disabled global click ripple feedback across the UI via Material 3 `LocalRippleConfiguration`.

### Changed
- **Version Bump**: Updated `versionCode` to `13` and `versionName` to `3.4.0`.

---

## [1.4.0] - 2026-08-28

### Added
- **Daynexa Integration API (Final v1 Contract)**:
  - Implemented secure, read-only Android `ContentProvider` (`DaynexaSessionsProvider`) with authority `com.focentra.provider`.
  - Added **Push Broadcast System**: Real-time `com.focentra.ACTION_SESSION_COMPLETED` events sent upon session save.
  - Finalized Schema v1 Contract: `sessionId`, `subject`, `topic`, `startTime`, `endTime`, `duration` (min), `completionStatus` (String), `focusScore`, and `schemaVersion`.
  - Enforced strict read-only guarantees (mutations reject with `UnsupportedOperationException`).
  - Defined secure custom permission `com.focentra.permission.READ_SESSIONS` to protect both Provider access and Broadcast delivery.
- **Integrations Hub & User Consent Controls**:
  - Added dedicated `INTEGRATIONS` section in `SettingsScreen` with live status indicator and Daynexa companion card.
  - Transparent authorization consent dialog detailing provider authority, endpoints, schema format, and privacy invariants.
  - One-tap Disconnect toggle to instantly revoke data access and stop all outgoing broadcasts.
- **Unit & Robolectric Test Suite**:
  - Comprehensive test coverage in `DaynexaSessionsProviderTest` and `DaynexaBroadcastTest` verifying pull/push data integrity, secure permission enforcement, and consent gate logic.

### Changed
- **Version Bump**: Incremented `versionCode` to `6` and `versionName` to `1.4.0`.
- **Documentation**: Updated `README.md`, `ROADMAP.md`, and `ARCHITECTURE.md` with finalized API v1 specifications.

---

## [1.3.2] - 2026-08-25

### Changed
- **Version Bump**: Updated `versionCode` to `5` and `versionName` to `1.3.2` to ensure consistent and correct chronological versioning history.
- **Documentation**: Updated `README.md` setup commands to correctly recommend the Gradle wrapper (`./gradlew`) rather than the global Gradle installation.

---

## [1.3.1] - 2026-08-25

### Changed
- **Architectural Refactoring**:
  - Fully migrated all source package declarations and directory structures from `com.example` to `com.niloy.focentra` to align with the production application ID.
- **UI Redesign & Polish**:
  - Removed the `Quick Start (25m)` Floating Action Button from the Home Screen (`DashboardScreen`) as it cluttered the UI and felt out of place.
  - Adjusted padding and layout constraints in `FlashcardsScreen` to properly align the "New Flashcard" button with the screen's bottom navigation bar, preventing awkward floating.
  - Improved typography and semantics by changing the Top App Bar title from "Recall Flashcards" to a cleaner "Flashcards".
- **Version Bump**: Updated `versionCode` to `4` and `versionName` to `1.3.1`.

---

## [1.3.0] - 2026-08-25

### Added
- **Persistent 'Quick Start' Floating Action Button (FAB)**:
  - Added a persistent `ExtendedFloatingActionButton` on the Home (`DashboardScreen`) allowing users to launch a predefined 25-minute study timer with one tap.
  - Dynamically updates icon and state based on whether a study session is active, paused, or idle.

### Removed
- **10 Full-Screen UI Theme Presets**:
  - Removed full-screen theme selection presets to simplify application architecture and eliminate unnecessary clutter.
  - Restored clean, high-contrast OLED dark canvas for Full-Screen Focus Mode.

### Changed
- **Version Bump**: Updated `versionCode` to `3` and `versionName` to `1.3.0`.
- **Documentation & Presentation**: Updated GitHub presentation (`README.md`, `ROADMAP.md`, `CHANGELOG.md`) reflecting the Quick Start FAB component and version 1.3.0.

---

## [1.2.1] - 2026-08-25

### Added
- **Active Recall Flashcards Studio**:
  - Dedicated `FlashcardsScreen` for creating, reviewing, and organizing subject-based study cards.
  - Interactive 3D perspective card flip animation (`graphicsLayer { rotationY }`) revealing solution and optional clue hints.
  - Mastery progress indicator bar calculating percentage of mastered cards per subject deck.
  - Quick access integrated into Top Bar actions, More menu, and Dashboard cards.
- **Focus Brain Dump & Distraction Shield**:
  - Instant thought-capture dialog (`BrainDumpDialog`) enabling learners to record off-topic ideas or tasks during study sessions without interrupting timer flow.
  - Accessible directly from `TimerScreen`, `FullScreenFocusScreen` (AMOLED mode), and `DashboardScreen`.
  - Checkable thought items with time stamps and one-tap clear for completed items.

### Changed
- **Shareable Study Report & PNG Card**:
  - Optimized the "Share PNG" and "Share Text" buttons in `AnalyticsScreen` with uniform heights (`46.dp`), explicit text padding, and single-line constraints to ensure perfectly symmetrical alignment on all screen sizes.
  - Updated high-contrast bitmap card generation for crisp social media sharing.
- **System & Version Updates**:
  - Version bumped to `1.2.1`.
  - Synchronized Settings screen database info string to reflect v1.2.1.

---

## [1.2.0] - 2026-08-25

### Added
- **Full-Screen Focus UI Preset Themes (10 Presets)**:
  - Custom full-screen themes for Timer, Pomodoro, and Stopwatch modes (*OLED Midnight*, *Cyberpunk Neon*, *Nordic Aurora*, *Sunset Amber*, *Tokyo Night*, *Emerald Oasis*, *Matrix Green*, *Solar Crimson*, *Deep Cosmos*, *Zen Slate*).
  - Theme selection integrated directly in `SettingsScreen` under Appearance & Themes with interactive preview swatches and a live "Preview" button.
  - Persistent theme configuration cached locally via `DataStore` and application preferences.
- **System Stability & Android 14+ Compatibility**:
  - Updated `StudyTimerService` foreground notification dispatch with `FOREGROUND_SERVICE_TYPE_SPECIAL_USE` for Android 14 (API level 34+) compatibility.
  - Enhanced grid alignment in `CalendarScreen` month view to ensure 7-column layout balance.

### Changed
- **Version Set**: Set `versionCode` to `2` and `versionName` to `1.2.0`.
- **Documentation & Presentation**: Synchronized GitHub presentation (`README.md`, `ROADMAP.md`, `CHANGELOG.md`) detailing all core features, architecture, and full-screen theme presets.

---

## [1.1.0] - 2026-08-25

### Changed
- **Version**: Pre-release version `1.1.0`.
- **Developer Info & Contact**:
  - Replaced temporary learning status descriptions with a clean, professional profile layout.
  - Added direct **Gmail** contact integration with one-tap email composer and clipboard fallback (`niloymitra889@gmail.com`).
  - Added direct **GitHub repository & profile** link integration (`https://github.com/Niloy-Track-Dev/Focentra`).
  - Polished the About dialog design with Material 3 elevated cards and typography.

### Fixed
- Verified and fine-tuned all UI layouts, text contrast ratios, and touch targets across all screens.
- Synchronized CI/CD GitHub Actions release pipeline and artifact naming with v1.1.0.

---

## [1.0.0] - 2026-08-25

### Added
- **Focus Timer Engine**:
  - Countdown, Stopwatch, and classic Pomodoro multi-interval study modes.
  - Background execution with Android Foreground Service (`StudyTimerService`) and interactive notification controls.
  - Distraction logging counter with optional reason/distraction tag tracking.
  - Ambient white noise audio generator (Rain, White Noise, Cafe, Stream, Ambient, Forest).
  - Immersive Full-Screen Focus Mode with flip-down AMOLED dark theme and anti-burn-in dimming.
- **Subject & Topic Management**:
  - Color-coded subjects with custom icon selection.
  - Sub-topic hierarchical organization with specific goal durations.
  - Subject study velocity, completion percentage, and target progress tracking.
- **Analytics & Insights**:
  - Interactive multi-period analytics (Today, Yesterday, This Week, Last Week, This Month, Last Month, This Year, All Time).
  - 16-week GitHub-style study heatmap grid with intensity levels.
  - Hourly productivity distribution curves (00:00 to 23:00).
  - Algorithmic Focus Score calculation (0–100) factoring productivity ratings, distractions, pause ratios, and completion status.
  - Automated smart offline productivity insights and tips.
- **Milestones & Gamification**:
  - Unlockable achievement badges based on total focus hours, streaks, and subject mastery.
  - Study streak tracking (Current Streak, Longest Streak, Active Days).
- **Data Portability & Security**:
  - Full offline database architecture built on AndroidX Room and SQLite.
  - 100% on-device data guarantee with zero telemetry, zero analytics trackers, and zero account requirements.
  - Full JSON backup export and import with clipboard integration.
  - Universal CSV spreadsheet export for Excel, Google Sheets, and Notion.
- **Modern UI & Theming**:
  - Jetpack Compose with Material Design 3 dynamic color scheme and edge-to-edge layout.
  - Floating pill navigation bar with haptic feedback.
  - Customizable study reminders and repeat schedules.
