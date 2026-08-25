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

data class FlashcardItem(
    val id: String = UUID.randomUUID().toString(),
    val subject: String,
    val question: String,
    val answer: String,
    val hint: String = "",
    val isMastered: Boolean = false,
    val reviewCount: Int = 0
)

data class BrainDumpNote(
    val id: String = UUID.randomUUID().toString(),
    val note: String,
    val timestamp: Long = System.currentTimeMillis(),
    val subject: String = "General",
    val isDone: Boolean = false
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

    init {
        viewModelScope.launch {
            val themeSetting = repository.getSetting("theme", app.getInitialTheme())
            _currentTheme.value = themeSetting
            app.saveCachedTheme(themeSetting)

            val langSetting = repository.getSetting("language", app.getInitialLanguage())
            _language.value = langSetting
            app.saveCachedLanguage(langSetting)
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
    private val _brainDumpNotes = MutableStateFlow<List<BrainDumpNote>>(
        listOf(
            BrainDumpNote(
                note = "Review calculus chain rule formulas before tomorrow's test",
                subject = "Mathematics"
            ),
            BrainDumpNote(
                note = "Outline bibliography for term research paper",
                subject = "General"
            )
        )
    )
    val brainDumpNotes: StateFlow<List<BrainDumpNote>> = _brainDumpNotes.asStateFlow()

    fun addBrainDumpNote(text: String, subject: String = "General") {
        if (text.isBlank()) return
        val newNote = BrainDumpNote(note = text.trim(), subject = subject)
        _brainDumpNotes.value = listOf(newNote) + _brainDumpNotes.value
        viewModelScope.launch {
            _snackbarMessage.emit("Thought captured to Brain Dump!")
        }
    }

    fun toggleBrainDumpDone(id: String) {
        _brainDumpNotes.value = _brainDumpNotes.value.map {
            if (it.id == id) it.copy(isDone = !it.isDone) else it
        }
    }

    fun deleteBrainDumpNote(id: String) {
        _brainDumpNotes.value = _brainDumpNotes.value.filter { it.id != id }
    }

    fun clearCompletedBrainDumps() {
        _brainDumpNotes.value = _brainDumpNotes.value.filter { !it.isDone }
    }

    // Active Recall Flashcards Feature
    private val _flashcards = MutableStateFlow<List<FlashcardItem>>(
        listOf(
            FlashcardItem(
                subject = "Mathematics",
                question = "What is the derivative of sin(x)?",
                answer = "cos(x)",
                hint = "Trigonometric standard derivative"
            ),
            FlashcardItem(
                subject = "Mathematics",
                question = "What is Euler's Formula relating complex numbers and trigonometry?",
                answer = "e^(ix) = cos(x) + i*sin(x)",
                hint = "Connects exponential and trigonometric functions"
            ),
            FlashcardItem(
                subject = "Physics",
                question = "State Newton's Second Law of Motion.",
                answer = "Force = mass × acceleration (F = ma)",
                hint = "Relationship between force, mass, and acceleration"
            ),
            FlashcardItem(
                subject = "Physics",
                question = "What is the speed of light in a vacuum (c)?",
                answer = "Approximately 3 × 10^8 m/s (299,792,458 m/s)",
                hint = "Universal physical constant"
            ),
            FlashcardItem(
                subject = "Computer Science",
                question = "What is the time complexity of binary search on a sorted array?",
                answer = "O(log n)",
                hint = "Halves search space each step"
            ),
            FlashcardItem(
                subject = "Chemistry",
                question = "What is Avogadro's constant?",
                answer = "6.022 × 10^23 particles per mole",
                hint = "Number of units in one mole of any substance"
            )
        )
    )
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
        viewModelScope.launch {
            _snackbarMessage.emit("Flashcard created successfully!")
        }
    }

    fun toggleFlashcardMastered(id: String) {
        _flashcards.value = _flashcards.value.map {
            if (it.id == id) it.copy(isMastered = !it.isMastered, reviewCount = it.reviewCount + 1) else it
        }
    }

    fun deleteFlashcard(id: String) {
        _flashcards.value = _flashcards.value.filter { it.id != id }
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
