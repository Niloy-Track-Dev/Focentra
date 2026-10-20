package com.niloy.focentra.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.niloy.focentra.FocentraApp
import com.niloy.focentra.data.local.entity.*
import com.niloy.focentra.data.repository.calculateStreak
import com.niloy.focentra.engine.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

enum class NotificationType {
    REVISION_DUE,
    INACTIVITY_WARNING,
    GOAL_WARNING,
    MOTIVATION
}

data class AppNotification(
    val id: String = UUID.randomUUID().toString(),
    val type: NotificationType,
    val title: String,
    val message: String,
    val subjectName: String = "",
    val chapterName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

enum class NavigationTab {
    DASHBOARD,
    TIMER,
    ANALYTICS,
    CALENDAR,
    HISTORY,
    SUBJECTS,
    ACHIEVEMENTS,
    PRESETS,
    SETTINGS,
    NOTIFICATIONS
}

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
    val notes: String = "",
    val chapterIds: List<Long> = emptyList()
)

data class DashboardWidgetOrder(
    val id: String,
    val title: String,
    val isVisible: Boolean = true
)

private fun toBanglaDigits(input: String): String {
    val map = mapOf(
        '0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪',
        '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯'
    )
    return input.map { map[it] ?: it }.joinToString("")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FocentraApp
    val repository = app.repository
    val timerEngine = app.timerEngine

    // Active Navigation Screen
    private val _currentTab = MutableStateFlow(NavigationTab.DASHBOARD)
    val currentTab: StateFlow<NavigationTab> = _currentTab.asStateFlow()

    // Seen Notification IDs & Unseen Count
    private val _seenNotificationIds = MutableStateFlow<Set<String>>(emptySet())
    val seenNotificationIds: StateFlow<Set<String>> = _seenNotificationIds.asStateFlow()

    fun markNotificationsAsSeen() {
        val activeIds = activeNotifications.value.map { it.id }.toSet()
        _seenNotificationIds.value = _seenNotificationIds.value + activeIds
    }

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
    val dailyTasks = repository.allTasks.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val chapters = repository.allChapters.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addChapter(subjectId: Long, subjectName: String, chapterName: String) {
        if (chapterName.isBlank()) return
        viewModelScope.launch {
            repository.insertChapter(
                ChapterEntity(
                    subjectId = subjectId,
                    subjectName = subjectName,
                    chapterName = chapterName.trim(),
                    tapCount = 0,
                    status = "পড়া শুরু হয় নাই"
                )
            )
        }
    }

    fun handleChapterTap(chapter: ChapterEntity) {
        val now = System.currentTimeMillis()
        val newTapCount = if (chapter.tapCount >= 3) 0 else chapter.tapCount + 1
        val newStatus = when (newTapCount) {
            3 -> "অধ্যায় Completely শেষ"
            2 -> "পড়া শেষ"
            1 -> "পড়া শুরু হয়েছে"
            else -> "পড়া শুরু হয় নাই"
        }
        val compTime = if (newTapCount == 3) now else chapter.completedTimestamp
        viewModelScope.launch {
            repository.updateChapter(chapter.copy(tapCount = newTapCount, status = newStatus, lastStudiedTimestamp = now, completedTimestamp = compTime))
        }
    }

    fun updateChapterStatus(chapter: ChapterEntity, newStatus: String) {
        val now = System.currentTimeMillis()
        val newTapCount = if (newStatus == "অধ্যায় Completely শেষ") 3 else chapter.tapCount
        val compTime = if (newStatus == "অধ্যায় Completely শেষ") now else chapter.completedTimestamp
        viewModelScope.launch {
            repository.updateChapter(chapter.copy(status = newStatus, tapCount = newTapCount, lastStudiedTimestamp = now, completedTimestamp = compTime))
        }
    }

    fun updateChapter(chapter: ChapterEntity) {
        viewModelScope.launch {
            repository.updateChapter(chapter)
        }
    }

    fun deleteChapter(chapter: ChapterEntity) {
        viewModelScope.launch {
            repository.deleteChapter(chapter)
        }
    }

    // Streak Saver Pass Used Month (Stored as YYYY-MM)
    private val _streakPassUsedMonth = MutableStateFlow("")
    val streakPassUsedMonth: StateFlow<String> = _streakPassUsedMonth.asStateFlow()

    fun addTask(title: String, subject: String = "General") {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.insertTask(StudyTaskEntity(title = title.trim(), subject = subject))
        }
    }

    fun toggleTask(task: StudyTaskEntity) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteTask(task: StudyTaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun clearCompletedTasks() {
        viewModelScope.launch {
            repository.clearCompletedTasks()
        }
    }

    fun useStreakSaverPass() {
        val cal = Calendar.getInstance()
        val currentMonthKey = String.format("%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        if (_streakPassUsedMonth.value == currentMonthKey) return

        viewModelScope.launch {
            _streakPassUsedMonth.value = currentMonthKey
            repository.setSetting("streak_pass_month", currentMonthKey)
            _snackbarMessage.emit("Streak Saver Pass activated for this month!")
        }
    }

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

    // Derived states optimized on Dispatchers.Default background threads
    val activeNotifications = combine(chapters, sessions, goals, todayFocusedMinutes) { chapterList, sessionList, goalList, todayMins ->
        kotlinx.coroutines.withContext(Dispatchers.Default) {
            val list = mutableListOf<AppNotification>()
            val now = System.currentTimeMillis()
            val THREE_DAYS_MS = 3 * 24 * 60 * 60 * 1000L
            val FIVE_DAYS_MS = 5 * 24 * 60 * 60 * 1000L

            chapterList.forEach { ch ->
                val isCompleted = ch.tapCount >= 3 || ch.status == "অধ্যায় Completely শেষ"
                if (isCompleted) {
                    val compTime = if (ch.completedTimestamp > 0) ch.completedTimestamp else ch.lastStudiedTimestamp
                    if (now - compTime >= THREE_DAYS_MS) {
                        list.add(
                            AppNotification(
                                type = NotificationType.REVISION_DUE,
                                title = "⚠️ রিভিশন অ্যালার্ট (৩ দিন পেরিয়ে গেছে)",
                                message = "${ch.subjectName} এর «${ch.chapterName}» অধ্যায়টি ৩ দিন আগে শেষ করেছেন। ৩০ মিনিট রিভিশন সম্পন্ন করুন!",
                                subjectName = ch.subjectName,
                                chapterName = ch.chapterName
                            )
                        )
                    }
                } else if (ch.status != "পড়া শুরু হয় নাই" && ch.status.isNotBlank()) {
                    val lastStudy = ch.lastStudiedTimestamp
                    if (now - lastStudy >= FIVE_DAYS_MS) {
                        list.add(
                            AppNotification(
                                type = NotificationType.INACTIVITY_WARNING,
                                title = "🔔 পড়া সম্পূর্ণ করার সতর্কতা (৫ দিন গ্যাপ)",
                                message = "${ch.subjectName} এর «${ch.chapterName}» অধ্যায়টি গত ৫ দিন ধরে পড়া বন্ধ রয়েছে। দ্রুত পড়া সম্পন্ন করুন!",
                                subjectName = ch.subjectName,
                                chapterName = ch.chapterName
                            )
                        )
                    }
                }
            }

            val dailyGoal = goalList.find { it.periodType == "DAILY" }?.targetMinutes ?: 480
            val remainingMins = (dailyGoal - todayMins).coerceAtLeast(0L)
            if (remainingMins > 0) {
                val hours = remainingMins / 60
                val mins = remainingMins % 60
                val hoursBn = toBanglaDigits(hours.toString())
                val minsBn = toBanglaDigits(mins.toString())
                val goalMessage = if (mins == 0L) {
                    "আজকের Daily Study Goal পূরণ করতে আরো $hoursBn ঘন্টা পড়াশোনা করা প্রয়োজন।"
                } else {
                    "আজকের Daily Study Goal পূরণ করতে আরো $hoursBn ঘন্টা $minsBn মিনিট পড়াশোনা করা প্রয়োজন।"
                }
                list.add(
                    AppNotification(
                        type = NotificationType.GOAL_WARNING,
                        title = "🎯 স্টাডি টার্গেট ট্র্যাকার",
                        message = goalMessage
                    )
                )
            }

            list.toList()
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val unseenNotificationCount = combine(activeNotifications, _seenNotificationIds) { active, seen ->
        active.count { it.id !in seen }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val streakInfo = sessions.map { sessionList ->
        kotlinx.coroutines.withContext(Dispatchers.Default) { calculateStreak(sessionList) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, calculateStreak(emptyList()))

    val personalRecords = sessions.map { sessionList ->
        kotlinx.coroutines.withContext(Dispatchers.Default) { StatisticsEngine.calculatePersonalRecords(sessionList) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, PersonalRecords())

    val heatmapData = sessions.map { sessionList ->
        kotlinx.coroutines.withContext(Dispatchers.Default) { StatisticsEngine.buildHeatmapData(sessionList, weeksBack = 16) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val periodStatistics = combine(sessions, _selectedPeriod, goals) { sessionList, period, goalList ->
        kotlinx.coroutines.withContext(Dispatchers.Default) {
            val dailyGoal = goalList.find { it.periodType == "DAILY" }?.targetMinutes ?: 480
            StatisticsEngine.calculatePeriodStats(sessionList, period, dailyGoal)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, PeriodStatistics(TimePeriod.THIS_WEEK))

    // Filtered History list optimized on Dispatchers.Default
    val filteredHistory = combine(sessions, _historySearchQuery, _historySubjectFilter, _historyMinProductivity) { list, query, subFilter, minRating ->
        kotlinx.coroutines.withContext(Dispatchers.Default) {
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
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Exam / Target Countdown Planner Feature
    private val _examTargets = MutableStateFlow<List<ExamTarget>>(emptyList())
    val examTargets: StateFlow<List<ExamTarget>> = _examTargets.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureBanglaSyllabusSeeded()

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

            val streakPass = repository.getSetting("streak_pass_month", "")
            _streakPassUsedMonth.value = streakPass

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

    fun updateSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            repository.updateSubject(subject)
            _snackbarMessage.emit("Subject «${subject.name}» updated.")
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

    // Manual study data input
    fun addManualSession(
        subject: String,
        topic: String = "",
        durationMinutes: Int,
        category: String = "Study",
        notes: String = "",
        rating: Int = 4,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val durationSec = durationMinutes.toLong() * 60L
            val session = StudySessionEntity(
                startTime = timestamp - (durationSec * 1000L),
                endTime = timestamp,
                durationSeconds = durationSec,
                actualFocusedSeconds = durationSec,
                pausedSeconds = 0L,
                sessionType = "MANUAL",
                subject = subject.ifBlank { "General" },
                topic = topic,
                category = category.ifBlank { "Study" },
                notes = notes,
                productivityRating = rating,
                completionStatus = "COMPLETED",
                createdTimestamp = timestamp,
                updatedTimestamp = timestamp
            )
            repository.insertSession(session)
            _snackbarMessage.emit("Study Data saved successfully!")
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

    fun clearAllAppData() {
        viewModelScope.launch {
            repository.clearAllAppData()
            _brainDumpNotes.value = emptyList()
            _examTargets.value = emptyList()
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
