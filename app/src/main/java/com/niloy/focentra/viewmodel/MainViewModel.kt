package com.niloy.focentra.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.niloy.focentra.FocentraApp
import com.niloy.focentra.data.local.entity.*
import com.niloy.focentra.data.repository.calculateStreak
import com.niloy.focentra.engine.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class NavigationTab {
    DASHBOARD,
    TIMER,
    ANALYTICS,
    CALENDAR,
    HISTORY,
    SUBJECTS,
    FLASHCARDS,
    ACHIEVEMENTS,
    PRESETS,
    SETTINGS
}

enum class SrsRating(val label: String, val intervalBonus: String) {
    AGAIN("Again", "<10m"),
    HARD("Hard", "+1d"),
    GOOD("Good", "+3d"),
    EASY("Easy", "+7d")
}

data class FlashcardItem(
    val id: String = UUID.randomUUID().toString(),
    val subject: String,
    val question: String,
    val answer: String,
    val hint: String = "",
    val isMastered: Boolean = false,
    val reviewCount: Int = 0,
    val intervalDays: Int = 1,
    val easeFactor: Float = 2.5f,
    val nextReviewEpochMs: Long = System.currentTimeMillis()
)

data class BrainDumpNote(
    val id: String = UUID.randomUUID().toString(),
    val note: String,
    val timestamp: Long = System.currentTimeMillis(),
    val subject: String = "General",
    val isDone: Boolean = false
)

data class ExamTarget(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val targetDateEpochMs: Long,
    val targetHours: Float = 50f,
    val subject: String = "General",
    val notes: String = ""
)

data class DashboardWidgetOrder(
    val id: String,
    val title: String,
    val isVisible: Boolean = true
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FocentraApp
    val repository = app.repository
    val timerEngine = app.timerEngine

    // Active Navigation Screen
    private val _currentTab = MutableStateFlow(NavigationTab.DASHBOARD)
    val currentTab: StateFlow<NavigationTab> = _currentTab.asStateFlow()

    // Full screen focus mode state
    private val _isFullScreenFocus = MutableStateFlow(false)
    val isFullScreenFocus: StateFlow<Boolean> = _isFullScreenFocus.asStateFlow()

    // Selected Statistics Period
    private val _selectedPeriod = MutableStateFlow(TimePeriod.THIS_WEEK)
    val selectedPeriod: StateFlow<TimePeriod> = _selectedPeriod.asStateFlow()

    // Selected Calendar Date (epoch ms)
    private val _selectedCalendarDate = MutableStateFlow(System.currentTimeMillis())
    val selectedCalendarDate: StateFlow<Long> = _selectedCalendarDate.asStateFlow()

    // History Search & Filter State
    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    private val _historySubjectFilter = MutableStateFlow<String?>(null)
    val historySubjectFilter: StateFlow<String?> = _historySubjectFilter.asStateFlow()

    private val _historyMinProductivity = MutableStateFlow(0)
    val historyMinProductivity: StateFlow<Int> = _historyMinProductivity.asStateFlow()

    // Theme & Settings State (Initialized synchronously to prevent white flash)
    private val _currentTheme = MutableStateFlow(app.getInitialTheme())
    val currentTheme: StateFlow<String> = _currentTheme.asStateFlow()

    private val _language = MutableStateFlow(app.getInitialLanguage())
    val language: StateFlow<String> = _language.asStateFlow()

    // Daynexa Integration State
    private val _isDaynexaConnected = MutableStateFlow(false)
    val isDaynexaConnected: StateFlow<Boolean> = _isDaynexaConnected.asStateFlow()

    private val _daynexaConnectedAt = MutableStateFlow(0L)
    val daynexaConnectedAt: StateFlow<Long> = _daynexaConnectedAt.asStateFlow()

    // Snackbar event channel
    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    // Last deleted session for undo
    private var lastDeletedSession: StudySessionEntity? = null

    // Room DB streams
    val sessions = repository.allSessions.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val subjects = repository.allSubjects.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val topics = repository.allTopics.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val goals = repository.allGoals.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val presets = repository.allPresets.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val achievements = repository.allAchievements.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val reminders = repository.allReminders.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val settings = repository.allSettings.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Derived states
    val streakInfo = sessions.map { calculateStreak(it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, calculateStreak(emptyList()))

    val personalRecords = sessions.map { StatisticsEngine.calculatePersonalRecords(it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, PersonalRecords())

    val heatmapData = sessions.map { StatisticsEngine.buildHeatmapData(it, weeksBack = 16) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val periodStatistics = combine(sessions, _selectedPeriod, goals) { sessionList, period, goalList ->
        val dailyGoal = goalList.find { it.periodType == "DAILY" }?.targetMinutes ?: 480
        StatisticsEngine.calculatePeriodStats(sessionList, period, dailyGoal)
    }.stateIn(viewModelScope, SharingStarted.Lazily, PeriodStatistics(TimePeriod.THIS_WEEK))

    // Filtered History list
    val filteredHistory = combine(sessions, _historySearchQuery, _historySubjectFilter, _historyMinProductivity) { list, query, subFilter, minRating ->
        list.filter { s ->
            val matchesQuery = query.isBlank() ||
                    s.subject.contains(query, ignoreCase = true) ||
                    s.topic.contains(query, ignoreCase = true) ||
                    s.notes.contains(query, ignoreCase = true) ||
                    s.tags.contains(query, ignoreCase = true)

            val matchesSubject = subFilter == null || s.subject.equals(subFilter, ignoreCase = true)
            val matchesRating = minRating == 0 || s.productivityRating >= minRating
            matchesQuery && matchesSubject && matchesRating
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Today's focused minutes
    val todayFocusedMinutes = sessions.map { list ->
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = cal.timeInMillis
        list.filter { it.startTime >= startOfToday }.sumOf { it.actualFocusedSeconds } / 60
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0L)

    // Exam / Target Countdown Planner Feature
    private val _examTargets = MutableStateFlow<List<ExamTarget>>(emptyList())
    val examTargets: StateFlow<List<ExamTarget>> = _examTargets.asStateFlow()

    init {
        viewModelScope.launch {
            val themeSetting = repository.getSetting("theme", app.getInitialTheme())
            _currentTheme.value = themeSetting
            app.saveCachedTheme(themeSetting)

            val langSetting = repository.getSetting("language", app.getInitialLanguage())
            _language.value = langSetting
            app.saveCachedLanguage(langSetting)

            val daynexaEnabled = repository.getSetting("daynexa_integration_enabled", "false")
            _isDaynexaConnected.value = daynexaEnabled.equals("true", ignoreCase = true)

            val daynexaTime = repository.getSetting("daynexa_connected_timestamp", "0")
            _daynexaConnectedAt.value = daynexaTime.toLongOrNull() ?: 0L

            loadPersistentUserData()
        }
    }

    private suspend fun loadPersistentUserData() {
        try {
            // Load Brain Dumps
            val bdJson = repository.getSetting("brain_dumps_json", "")
            if (bdJson.isNotBlank()) {
                val array = org.json.JSONArray(bdJson)
                val list = mutableListOf<BrainDumpNote>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        BrainDumpNote(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            note = obj.optString("note", ""),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            subject = obj.optString("subject", "General"),
                            isDone = obj.optBoolean("isDone", false)
                        )
                    )
                }
                _brainDumpNotes.value = list
            }

            // Load Flashcards
            val fcJson = repository.getSetting("flashcards_json", "")
            if (fcJson.isNotBlank()) {
                val array = org.json.JSONArray(fcJson)
                val list = mutableListOf<FlashcardItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        FlashcardItem(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            subject = obj.optString("subject", "General"),
                            question = obj.optString("question", ""),
                            answer = obj.optString("answer", ""),
                            hint = obj.optString("hint", ""),
                            isMastered = obj.optBoolean("isMastered", false),
                            reviewCount = obj.optInt("reviewCount", 0),
                            intervalDays = obj.optInt("intervalDays", 1),
                            easeFactor = obj.optDouble("easeFactor", 2.5).toFloat(),
                            nextReviewEpochMs = obj.optLong("nextReviewEpochMs", System.currentTimeMillis())
                        )
                    )
                }
                _flashcards.value = list
            }

            // Load Exam Targets
            val etJson = repository.getSetting("exam_targets_json", "")
            if (etJson.isNotBlank()) {
                val array = org.json.JSONArray(etJson)
                val list = mutableListOf<ExamTarget>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        ExamTarget(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            title = obj.optString("title", ""),
                            targetDateEpochMs = obj.optLong("targetDateEpochMs", System.currentTimeMillis() + 86400000L * 30),
                            targetHours = obj.optDouble("targetHours", 50.0).toFloat(),
                            subject = obj.optString("subject", "General"),
                            notes = obj.optString("notes", "")
                        )
                    )
                }
                _examTargets.value = list
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun persistBrainDumps() {
        viewModelScope.launch {
            try {
                val array = org.json.JSONArray()
                _brainDumpNotes.value.forEach { note ->
                    val obj = org.json.JSONObject().apply {
                        put("id", note.id)
                        put("note", note.note)
                        put("timestamp", note.timestamp)
                        put("subject", note.subject)
                        put("isDone", note.isDone)
                    }
                    array.put(obj)
                }
                repository.setSetting("brain_dumps_json", array.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun persistFlashcards() {
        viewModelScope.launch {
            try {
                val array = org.json.JSONArray()
                _flashcards.value.forEach { card ->
                    val obj = org.json.JSONObject().apply {
                        put("id", card.id)
                        put("subject", card.subject)
                        put("question", card.question)
                        put("answer", card.answer)
                        put("hint", card.hint)
                        put("isMastered", card.isMastered)
                        put("reviewCount", card.reviewCount)
                        put("intervalDays", card.intervalDays)
                        put("easeFactor", card.easeFactor.toDouble())
                        put("nextReviewEpochMs", card.nextReviewEpochMs)
                    }
                    array.put(obj)
                }
                repository.setSetting("flashcards_json", array.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun persistExamTargets() {
        viewModelScope.launch {
            try {
                val array = org.json.JSONArray()
                _examTargets.value.forEach { target ->
                    val obj = org.json.JSONObject().apply {
                        put("id", target.id)
                        put("title", target.title)
                        put("targetDateEpochMs", target.targetDateEpochMs)
                        put("targetHours", target.targetHours.toDouble())
                        put("subject", target.subject)
                        put("notes", target.notes)
                    }
                    array.put(obj)
                }
                repository.setSetting("exam_targets_json", array.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setDaynexaConsent(enabled: Boolean) {
        _isDaynexaConnected.value = enabled
        val now = if (enabled) System.currentTimeMillis() else 0L
        _daynexaConnectedAt.value = now
        viewModelScope.launch {
            repository.setSetting("daynexa_integration_enabled", if (enabled) "true" else "false")
            repository.setSetting("daynexa_connected_timestamp", now.toString())
            if (enabled) {
                _snackbarMessage.emit("Daynexa Integration connected (API v1).")
            } else {
                _snackbarMessage.emit("Daynexa Integration disconnected.")
            }
        }
    }

    fun navigateTo(tab: NavigationTab) {
        _currentTab.value = tab
    }

    fun setFullScreenFocus(enabled: Boolean) {
        _isFullScreenFocus.value = enabled
    }

    fun setPeriod(period: TimePeriod) {
        _selectedPeriod.value = period
    }

    fun setCalendarDate(epochMs: Long) {
        _selectedCalendarDate.value = epochMs
    }

    fun setHistorySearchQuery(q: String) {
        _historySearchQuery.value = q
    }

    fun setHistorySubjectFilter(sub: String?) {
        _historySubjectFilter.value = sub
    }

    fun setHistoryMinProductivity(rating: Int) {
        _historyMinProductivity.value = rating
    }

    fun setTheme(theme: String) {
        _currentTheme.value = theme
        app.saveCachedTheme(theme)
        viewModelScope.launch {
            repository.setSetting("theme", theme)
        }
    }

    fun setLanguage(lang: String) {
        _language.value = lang
        app.saveCachedLanguage(lang)
        viewModelScope.launch {
            repository.setSetting("language", lang)
        }
    }

    // Timer Actions
    fun startTimerWithPreset(preset: PresetEntity) {
        val mode = when (preset.type) {
            "POMODORO" -> SessionMode.POMODORO
            "STOPWATCH" -> SessionMode.STOPWATCH
            else -> SessionMode.COUNTDOWN
        }
        timerEngine.configureSession(
            mode = mode,
            subject = preset.subject,
            topic = preset.topic,
            durationMinutes = preset.durationMinutes,
            pomodoroFocusMin = preset.pomodoroFocusMin,
            pomodoroShortBreakMin = preset.pomodoroShortBreakMin,
            pomodoroLongBreakMin = preset.pomodoroLongBreakMin,
            pomodoroRounds = preset.pomodoroRounds,
            tags = if (preset.tags.isNotBlank()) preset.tags.split(",") else emptyList()
        )
        timerEngine.startSession()
        _currentTab.value = NavigationTab.TIMER
    }

    fun startQuickTimer(durationMinutes: Int, subject: String, mode: SessionMode = SessionMode.COUNTDOWN) {
        timerEngine.configureSession(
            mode = mode,
            subject = subject,
            durationMinutes = durationMinutes
        )
        timerEngine.startSession()
        _currentTab.value = NavigationTab.TIMER
    }

    // Subject operations
    fun createSubject(name: String, colorHex: String, iconName: String, targetHours: Float, description: String) {
        viewModelScope.launch {
            repository.insertSubject(
                SubjectEntity(
                    name = name,
                    colorHex = colorHex,
                    iconName = iconName,
                    targetHours = targetHours,
                    description = description,
                    orderIndex = subjects.value.size
                )
            )
            _snackbarMessage.emit("Subject «$name» created.")
        }
    }

    fun deleteSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            repository.deleteSubject(subject)
            _snackbarMessage.emit("Subject «${subject.name}» deleted.")
        }
    }

    fun createTopic(subjectId: Long, subjectName: String, name: String) {
        viewModelScope.launch {
            repository.insertTopic(TopicEntity(subjectId = subjectId, subjectName = subjectName, name = name))
        }
    }

    fun deleteTopic(topic: TopicEntity) {
        viewModelScope.launch {
            repository.deleteTopic(topic)
        }
    }

    // Goal operations
    fun updateGoalTarget(periodType: String, newTargetMinutes: Int) {
        viewModelScope.launch {
            val existing = goals.value.find { it.periodType == periodType }
            if (existing != null) {
                repository.updateGoal(existing.copy(targetMinutes = newTargetMinutes))
            } else {
                repository.insertGoal(StudyGoalEntity(periodType = periodType, targetMinutes = newTargetMinutes))
            }
            _snackbarMessage.emit("Updated $periodType goal to ${newTargetMinutes / 60}h ${newTargetMinutes % 60}m.")
        }
    }

    // Preset operations
    fun savePreset(preset: PresetEntity) {
        viewModelScope.launch {
            if (preset.id == 0L) {
                repository.insertPreset(preset)
            } else {
                repository.updatePreset(preset)
            }
            _snackbarMessage.emit("Preset «${preset.title}» saved.")
        }
    }

    fun deletePreset(preset: PresetEntity) {
        viewModelScope.launch {
            repository.deletePreset(preset)
            _snackbarMessage.emit("Preset «${preset.title}» deleted.")
        }
    }

    // Session edits & delete
    fun updateSession(session: StudySessionEntity) {
        viewModelScope.launch {
            repository.updateSession(session.copy(updatedTimestamp = System.currentTimeMillis()))
            _snackbarMessage.emit("Session updated.")
        }
    }

    fun deleteSession(session: StudySessionEntity) {
        lastDeletedSession = session
        viewModelScope.launch {
            repository.deleteSession(session)
            _snackbarMessage.emit("Session deleted. Tap Undo to restore.")
        }
    }

    fun undoDeleteSession() {
        val session = lastDeletedSession ?: return
        viewModelScope.launch {
            repository.insertSession(session)
            lastDeletedSession = null
            _snackbarMessage.emit("Session restored.")
        }
    }

    // Reminders
    fun toggleReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.updateReminder(reminder.copy(enabled = !reminder.enabled))
        }
    }

    fun saveReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            if (reminder.id == 0L) {
                repository.insertReminder(reminder)
            } else {
                repository.updateReminder(reminder)
            }
            _snackbarMessage.emit("Reminder saved.")
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
        }
    }

    // Export & Import
    suspend fun exportJson(): String {
        return repository.exportDataJson()
    }

    suspend fun exportCsv(): String {
        return repository.exportSessionsCsv()
    }

    suspend fun importJson(json: String, replace: Boolean): Boolean {
        val success = repository.importDataJson(json, replace)
        if (success) {
            _snackbarMessage.emit("Backup imported successfully.")
        } else {
            _snackbarMessage.emit("Import failed: Invalid backup format.")
        }
        return success
    }

    // Brain Dump & Focus Notes Feature
    private val _brainDumpNotes = MutableStateFlow<List<BrainDumpNote>>(emptyList())
    val brainDumpNotes: StateFlow<List<BrainDumpNote>> = _brainDumpNotes.asStateFlow()

    fun addBrainDumpNote(text: String, subject: String = "General") {
        if (text.isBlank()) return
        val newNote = BrainDumpNote(note = text.trim(), subject = subject)
        _brainDumpNotes.value = listOf(newNote) + _brainDumpNotes.value
        persistBrainDumps()
        viewModelScope.launch {
            _snackbarMessage.emit("Thought captured to Brain Dump!")
        }
    }

    fun toggleBrainDumpDone(id: String) {
        _brainDumpNotes.value = _brainDumpNotes.value.map {
            if (it.id == id) it.copy(isDone = !it.isDone) else it
        }
        persistBrainDumps()
    }

    fun deleteBrainDumpNote(id: String) {
        _brainDumpNotes.value = _brainDumpNotes.value.filter { it.id != id }
        persistBrainDumps()
    }

    fun clearCompletedBrainDumps() {
        _brainDumpNotes.value = _brainDumpNotes.value.filter { !it.isDone }
        persistBrainDumps()
    }

    // Active Recall Flashcards Feature
    private val _flashcards = MutableStateFlow<List<FlashcardItem>>(emptyList())
    val flashcards: StateFlow<List<FlashcardItem>> = _flashcards.asStateFlow()

    fun addFlashcard(subject: String, question: String, answer: String, hint: String = "") {
        if (question.isBlank() || answer.isBlank()) return
        val newCard = FlashcardItem(
            subject = subject.ifBlank { "General" },
            question = question.trim(),
            answer = answer.trim(),
            hint = hint.trim()
        )
        _flashcards.value = _flashcards.value + newCard
        persistFlashcards()
        viewModelScope.launch {
            _snackbarMessage.emit("Flashcard created successfully!")
        }
    }

    fun toggleFlashcardMastered(id: String) {
        _flashcards.value = _flashcards.value.map {
            if (it.id == id) it.copy(isMastered = !it.isMastered, reviewCount = it.reviewCount + 1) else it
        }
        persistFlashcards()
    }

    fun gradeFlashcard(id: String, rating: SrsRating) {
        _flashcards.value = _flashcards.value.map { card ->
            if (card.id == id) {
                val newReviewCount = card.reviewCount + 1
                val newInterval = when (rating) {
                    SrsRating.AGAIN -> 1
                    SrsRating.HARD -> (card.intervalDays * 1.2f).toInt().coerceAtLeast(1)
                    SrsRating.GOOD -> (card.intervalDays * card.easeFactor).toInt().coerceAtLeast(2)
                    SrsRating.EASY -> (card.intervalDays * (card.easeFactor + 0.5f)).toInt().coerceAtLeast(4)
                }
                val newEase = when (rating) {
                    SrsRating.AGAIN -> (card.easeFactor - 0.2f).coerceAtLeast(1.3f)
                    SrsRating.HARD -> (card.easeFactor - 0.15f).coerceAtLeast(1.3f)
                    SrsRating.GOOD -> card.easeFactor
                    SrsRating.EASY -> (card.easeFactor + 0.15f).coerceAtMost(3.0f)
                }
                val isMasteredNow = card.isMastered || rating == SrsRating.EASY || (rating == SrsRating.GOOD && newReviewCount >= 3)
                val nextReview = System.currentTimeMillis() + (newInterval.toLong() * 86400000L)
                card.copy(
                    reviewCount = newReviewCount,
                    intervalDays = newInterval,
                    easeFactor = newEase,
                    isMastered = isMasteredNow,
                    nextReviewEpochMs = nextReview
                )
            } else card
        }
        persistFlashcards()
    }

    fun deleteFlashcard(id: String) {
        _flashcards.value = _flashcards.value.filter { it.id != id }
        persistFlashcards()
    }

    // Exam Target Planner Methods
    fun addExamTarget(
        title: String,
        targetDateEpochMs: Long,
        targetHours: Float = 50f,
        subject: String = "General",
        notes: String = ""
    ) {
        if (title.isBlank()) return
        val newTarget = ExamTarget(
            title = title.trim(),
            targetDateEpochMs = targetDateEpochMs,
            targetHours = targetHours.coerceAtLeast(1f),
            subject = subject.ifBlank { "General" },
            notes = notes.trim()
        )
        _examTargets.value = _examTargets.value + newTarget
        persistExamTargets()
        viewModelScope.launch {
            _snackbarMessage.emit("Target milestone created!")
        }
    }

    fun deleteExamTarget(id: String) {
        _examTargets.value = _examTargets.value.filter { it.id != id }
        persistExamTargets()
    }

    fun editExamTarget(updated: ExamTarget) {
        _examTargets.value = _examTargets.value.map {
            if (it.id == updated.id) updated else it
        }
        persistExamTargets()
    }

    fun generateShareableStudyReport(): String {
        val stats = periodStatistics.value
        val streak = streakInfo.value
        val df = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val dateStr = df.format(Date())

        val totalHrs = stats.totalFocusedSeconds / 3600
        val totalMins = (stats.totalFocusedSeconds % 3600) / 60

        val sb = StringBuilder()
        sb.append("====================================\n")
        sb.append("      FOCENTRA STUDY REPORT\n")
        sb.append("          $dateStr\n")
        sb.append("====================================\n\n")
        sb.append("Total Focus Time: ${totalHrs}h ${totalMins}m\n")
        sb.append("Sessions Completed: ${stats.sessionCount}\n")
        sb.append("Average Session: ${stats.averageSessionMinutes} min\n")
        sb.append("Focus Score: ${stats.focusScore}/100\n")
        sb.append("Current Streak: ${streak.currentStreak} Days\n")
        sb.append("Longest Streak: ${streak.longestStreak} Days\n")
        sb.append("Primary Subject: ${stats.mostStudiedSubject}\n")
        sb.append("Top Output Day: ${stats.mostProductiveDay}\n")
        sb.append("Peak Concentration: ${stats.mostProductiveHour}\n\n")
        sb.append("Keep learning with discipline! - Focentra Offline Study OS")
        return sb.toString()
    }
}
