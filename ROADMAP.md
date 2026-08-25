# 🗺️ Focentra Project Roadmap

This document outlines the development trajectory of **Focentra**, categorizing completed milestones, active work, and planned innovations.

---

## 🟢 Completed (v1.3.1)

- [x] **UI Polish & Clean Architecture**:
  - [x] Migrated all source package declarations and directory structures from `com.example` to `com.niloy.focentra`.
  - [x] Removed Quick Start FAB from Dashboard to minimize cognitive load and layout clutter.
  - [x] Fixed layout paddings in Flashcards Screen for optimal Floating Action Button placement above the bottom navigation bar.
  - [x] Enhanced AppBar semantics with concise titling.

---

## 🟢 Completed (v1.3.0)

- [x] **Persistent Quick Start Floating Action Button (FAB)**:
  - [x] Persistent home screen FAB component to instantly launch a predefined 25-minute study timer with 1 tap.
  - [x] Smart state detection (Active Session vs. New Quick Launch).
- [x] **UI Streamlining & AMOLED Focus Mode**:
  - [x] Removed full-screen theme preset clutter for maximum simplicity and performance.
  - [x] Pure high-contrast OLED dark focus environment.
- [x] **Active Recall Flashcards Studio**:
  - [x] Interactive 3D flip cards (`graphicsLayer { rotationY }`) for formulas, questions, and answers.
  - [x] Subject filtering and custom card creation with hints.
  - [x] Mastery score progress tracker with one-tap status updates.
- [x] **Focus Brain Dump & Distraction Shield**:
  - [x] On-the-fly thought capture pad in Timer Screen, AMOLED Focus Mode, and Dashboard.
  - [x] Time-stamped thought list with checkboxes and instant clear.
- [x] **Shareable Performance Card Exporter**:
  - [x] High-contrast PNG performance card generator for social media sharing.
  - [x] Formatted text report exporter.
- [x] **Core Focus Timer Engine**:
  - [x] Countdown timer with customizable preset durations.
  - [x] Open-ended Stopwatch mode for freeform deep work.
  - [x] Pomodoro timer with automated work intervals, short breaks, and long breaks.
  - [x] Background execution with Android Foreground Service (`StudyTimerService`) and active notification bar controls.
  - [x] Distraction logging and focus rating (1 to 5 stars).
  - [x] Ambient sound player (Rain, White Noise, Cafe, Stream, Ambient, Forest).
- [x] **Full-Screen Immersive Focus**:
  - [x] Minimalist dark canvas with flip-clock aesthetics and customizable theme presets.
  - [x] Anti-burn-in display protection for OLED/AMOLED screens.
  - [x] Screen wake-lock management during active sessions.
- [x] **Subject & Topic Architecture**:
  - [x] Custom subjects with color palettes and Material Symbols.
  - [x] Hierarchical sub-topic assignment and target goal tracking.
  - [x] Progress rings and velocity indicators.
- [x] **Analytics & Productivity Engine**:
  - [x] Multi-period filtering (Today, Yesterday, This Week, Last Week, This Month, Last Month, This Year, All Time).
  - [x] GitHub-style 16-week study heatmap with density levels.
  - [x] Bar charts for daily/weekly distributions.
  - [x] 24-hour peak productivity curve.
  - [x] Focus Score calculation (0–100) combining rating, distractions, and pause ratio.
  - [x] Smart offline analytical insights.
- [x] **History & Calendar**:
  - [x] Month-view study calendar showing focused days and durations with balanced 7-column grid.
  - [x] Detailed session log list with search and filter capabilities.
  - [x] Session detail inspector with deletion and note viewing.
- [x] **Gamification & Habits**:
  - [x] Daily study streak tracking (Current Streak, Longest Streak, Active Days).
  - [x] Unlockable milestone badges (First Step, 10-Hour Club, Night Owl, Consistency Champion, etc.).
- [x] **Privacy & Data Portability**:
  - [x] 100% offline local SQLite persistence via AndroidX Room.
  - [x] JSON database backup and restore with clipboard paste support.
  - [x] Universal CSV spreadsheet export for Excel, Google Sheets, and Notion.
- [x] **Modern Architecture & UI**:
  - [x] 100% Jetpack Compose with Material 3 dynamic color theming.
  - [x] Floating bottom navigation pill with haptics.
  - [x] Scheduled study reminders with repeat day filters.

---

## 🟡 In Progress

- [ ] **Home Screen Widget**:
  - Glance-based Compose AppWidget for quick timer starting directly from the Android home screen.
- [ ] **Expanded Audio Synthesizer**:
  - Additional binaural beats (Alpha/Theta frequencies) and brown noise presets.

---

## 🔵 Planned

- [ ] **Custom Timer Presets Library**:
  - User-shareable preset templates (e.g., "Ultradian 90/20", "52/17 Desk Routine").
- [ ] **Goal Pacing & Exam Countdown**:
  - Countdown widget towards specific exam/deadline dates with daily required target hours.
- [ ] **Wear OS Companion**:
  - Independent or synced Wear OS tile and focus timer app.
- [ ] **Encrypted Local Backups**:
  - Optional passphrase encryption for exported JSON backup files.

---

## 💡 Future Ideas & Research

- [ ] **Do Not Disturb (DND) Auto-Toggle**:
  - Optional permission-gated trigger to silence notifications during active sessions.
- [ ] **Desktop / Web Companion**:
  - Kotlin Multiplatform (KMP) desktop port for synchronized local study tracking across laptop and mobile.
- [ ] **Local LLM Study Assistant**:
  - On-device AI (e.g. Gemini Nano via AICore) for contextual offline study plan recommendations.

---

*Suggestions and feedback are welcome! Feel free to open a [Feature Request](https://github.com/Niloy-Track-Dev/Focentra/issues/new?template=feature_request.md).*
