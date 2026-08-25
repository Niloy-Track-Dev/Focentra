# 🏛️ Focentra System Architecture

**Focentra** is designed following modern Android architecture best practices, leveraging **Clean Architecture**, **MVVM (Model-View-ViewModel)**, and **Unidirectional Data Flow (UDF)**.

The application is completely **offline-first**, storing all state and history locally in an AndroidX Room SQLite database.

---

## 📐 High-Level Architecture Overview

```text
               ┌───────────────────────────────┐
               │    JETPACK COMPOSE UI LAYER   │
               │  (Dashboard, Timer, Focus,    │
               │   Analytics, Calendar, Notes) │
               └───────────────┬───────────────┘
                               │ StateFlow<UiState>
                               │ Events / User Intents
                               ▼
               ┌───────────────────────────────┐
               │        VIEWMODEL LAYER        │
               │   (MainViewModel Coordinator) │
               └───────┬───────────────┬───────┘
                       │               │
       ┌───────────────▼─┐           ┌─▼───────────────┐
       │ DOMAIN ENGINES  │           │   BG SERVICE    │
       │• StatisticsEngine           │• StudyTimerSvc  │
       │• TimerEngine    │           │• WhiteNoisePlay │
       └───────┬─────────┘           └─────────────────┘
               │
               ▼
       ┌───────────────────────────────────────────────┐
       │               STUDY REPOSITORY                │
       └───────┬───────────────────────────────┬───────┘
               │                               │
               ▼                               ▼
       ┌─────────────────┐           ┌─────────────────┐
       │   ROOM SQLITE   │           │    DATASTORE    │
       │(Sessions, Subj) │           │ (Settings, Pref)│
       └─────────────────┘           └─────────────────┘
```

---

## 📦 Layer Breakdown

### 1. UI Layer (`com.example.ui`)
- **Declarative Compose UI**: Written 100% in Jetpack Compose using Material Design 3.
- **Screens**:
  - `DashboardScreen`: Daily study goal progress ring, quick stats, active streak, subject velocity, and recent sessions.
  - `TimerScreen`: Dynamic countdown, stopwatch, and pomodoro mode selectors, ambient noise player, and distraction counter.
  - `FullScreenFocusScreen`: Distraction-free AMOLED flip clock with wake-lock and anti-burn-in dimming.
  - `AnalyticsScreen`: Period filters, 16-week study heatmap, bar charts, 24-hour productivity distribution, and offline insights.
  - `HistoryScreen` & `CalendarScreen`: Month-view heatmap calendar, log entries, and search filters.
  - `SubjectsScreen`: Subject hierarchy, color coding, topic assignments, and target hours.
  - `AchievementsScreen`: Milestone badges and streak progress.
  - `SettingsScreen`: Theme toggles, study reminders, soundscape options, and JSON/CSV backup utilities.

### 2. State & Engine Layer (`com.example.engine` & `com.example.viewmodel`)
- **`MainViewModel`**: Single source of truth managing UI state using Kotlin Coroutines and `MutableStateFlow`.
- **`StatisticsEngine`**: Pure algorithmic engine computing:
  - **Focus Score ($0 - 100$)**: Algorithmic formulation factoring in productivity star ratings, pause-to-work ratios, and distraction frequencies.
  - **Time Period Aggregations**: Precise timestamp boundary calculations across Today, Yesterday, This Week, Last Week, This Month, Last Month, This Year, and All Time.
  - **16-Week Heatmap Grid**: Generates a GitHub-style activity grid measuring daily study intensity.
  - **24-Hour Peak Productivity Curve**: Analyzes start timestamps to uncover optimal cognitive focus hours.
- **`TimerEngine`**: Deterministic state machine governing Countdown, Stopwatch, and Pomodoro phase transitions.

### 3. Background Services (`com.example.service`)
- **`StudyTimerService`**: Foreground Service running with a persistent notification, ensuring study sessions never get terminated by Android battery optimizations.
- **`WhiteNoisePlayer`**: Local synthesized ambient audio playback (Rain, White Noise, Cafe, Stream, Ambient, Forest).

### 4. Data Layer (`com.example.data`)
- **`StudyRepository`**: Central repository providing Flow streams from Room DAOs.
- **Room SQLite Entities**:
  - `StudySessionEntity`: Represents each completed or saved study interval with duration, distraction logs, subject, mood, rating, and timestamps.
  - `SubjectEntity`: Custom subjects with associated hex colors, icons, and target hours.
  - `TopicEntity`: Sub-topics linked to parent subjects.
  - `AchievementEntity`: Gamification badges and unlock status.
  - `ReminderEntity`: Configured daily study alarm schedules.
  - `PresetEntity`: Custom timer duration presets.
- **Data Portability**:
  - Full JSON serialization for backup and restore with clipboard paste support.
  - Universal CSV exporter for spreadsheet integrations (Notion, Google Sheets, Microsoft Excel).

---

## 🔒 Privacy & Local Storage Invariants
- **No Cloud Backend**: There are no remote database calls, Firebase data sinks, or third-party tracking libraries.
- **Zero Telemetry**: User study habits, notes, and metrics remain strictly confined to the host device's internal application sandbox.
- **Full Data Ownership**: Users can backup all database tables to raw JSON or CSV at any time from the Settings menu.
