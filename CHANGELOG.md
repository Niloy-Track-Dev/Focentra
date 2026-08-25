# Changelog

All notable changes to **Focentra** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Planned
- Home screen widget for fast study timer launch
- Wear OS companion module support
- Additional custom soundscapes for background white noise

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
