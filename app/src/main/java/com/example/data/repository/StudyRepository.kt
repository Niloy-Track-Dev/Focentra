package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar

class StudyRepository(private val database: AppDatabase) {

    val allSessions: Flow<List<StudySessionEntity>> = database.sessionDao().getAllSessionsFlow()
    val allSubjects: Flow<List<SubjectEntity>> = database.subjectDao().getAllSubjectsFlow()
    val allTopics: Flow<List<TopicEntity>> = database.topicDao().getAllTopicsFlow()
    val allGoals: Flow<List<StudyGoalEntity>> = database.goalDao().getActiveGoalsFlow()
    val allPresets: Flow<List<PresetEntity>> = database.presetDao().getAllPresetsFlow()
    val allAchievements: Flow<List<AchievementEntity>> = database.achievementDao().getAllAchievementsFlow()
    val allReminders: Flow<List<ReminderEntity>> = database.reminderDao().getAllRemindersFlow()
    val allSettings: Flow<List<SettingEntity>> = database.settingDao().getAllSettingsFlow()

    suspend fun insertSession(session: StudySessionEntity): Long = withContext(Dispatchers.IO) {
        val id = database.sessionDao().insertSession(session)
        checkAndUnlockAchievements()
        id
    }

    suspend fun updateSession(session: StudySessionEntity) = withContext(Dispatchers.IO) {
        database.sessionDao().updateSession(session)
        checkAndUnlockAchievements()
    }

    suspend fun deleteSession(session: StudySessionEntity) = withContext(Dispatchers.IO) {
        database.sessionDao().deleteSession(session)
        checkAndUnlockAchievements()
    }

    suspend fun getSessionById(id: Long): StudySessionEntity? = withContext(Dispatchers.IO) {
        database.sessionDao().getSessionById(id)
    }

    suspend fun getAllSessionsList(): List<StudySessionEntity> = withContext(Dispatchers.IO) {
        database.sessionDao().getAllSessions()
    }

    suspend fun getSessionsBetween(start: Long, end: Long): List<StudySessionEntity> = withContext(Dispatchers.IO) {
        database.sessionDao().getSessionsBetween(start, end)
    }

    // Subjects
    suspend fun insertSubject(subject: SubjectEntity): Long = withContext(Dispatchers.IO) {
        database.subjectDao().insertSubject(subject)
    }

    suspend fun updateSubject(subject: SubjectEntity) = withContext(Dispatchers.IO) {
        database.subjectDao().updateSubject(subject)
    }

    suspend fun deleteSubject(subject: SubjectEntity) = withContext(Dispatchers.IO) {
        database.subjectDao().deleteSubject(subject)
    }

    // Topics
    suspend fun insertTopic(topic: TopicEntity): Long = withContext(Dispatchers.IO) {
        database.topicDao().insertTopic(topic)
    }

    suspend fun deleteTopic(topic: TopicEntity) = withContext(Dispatchers.IO) {
        database.topicDao().deleteTopic(topic)
    }

    // Goals
    suspend fun insertGoal(goal: StudyGoalEntity) = withContext(Dispatchers.IO) {
        database.goalDao().insertGoal(goal)
    }

    suspend fun updateGoal(goal: StudyGoalEntity) = withContext(Dispatchers.IO) {
        database.goalDao().updateGoal(goal)
    }

    // Presets
    suspend fun insertPreset(preset: PresetEntity): Long = withContext(Dispatchers.IO) {
        database.presetDao().insertPreset(preset)
    }

    suspend fun updatePreset(preset: PresetEntity) = withContext(Dispatchers.IO) {
        database.presetDao().updatePreset(preset)
    }

    suspend fun deletePreset(preset: PresetEntity) = withContext(Dispatchers.IO) {
        database.presetDao().deletePreset(preset)
    }

    // Reminders
    suspend fun insertReminder(reminder: ReminderEntity): Long = withContext(Dispatchers.IO) {
        database.reminderDao().insertReminder(reminder)
    }

    suspend fun updateReminder(reminder: ReminderEntity) = withContext(Dispatchers.IO) {
        database.reminderDao().updateReminder(reminder)
    }

    suspend fun deleteReminder(reminder: ReminderEntity) = withContext(Dispatchers.IO) {
        database.reminderDao().deleteReminder(reminder)
    }

    // Settings
    suspend fun getSetting(key: String, default: String = ""): String = withContext(Dispatchers.IO) {
        database.settingDao().getSettingValue(key) ?: default
    }

    suspend fun setSetting(key: String, value: String) = withContext(Dispatchers.IO) {
        database.settingDao().setSetting(SettingEntity(key, value))
    }

    // Achievement evaluation
    suspend fun checkAndUnlockAchievements() = withContext(Dispatchers.IO) {
        val sessions = database.sessionDao().getAllSessions()
        val totalFocusedSeconds = sessions.sumOf { it.actualFocusedSeconds }
        val totalFocusedHours = totalFocusedSeconds / 3600f
        val streak = calculateStreak(sessions)
        val now = System.currentTimeMillis()

        val achievements = database.achievementDao().getAllAchievements().toMutableList()

        for (ach in achievements) {
            var newProgress = ach.progress
            var unlocked = ach.unlocked
            val id = ach.id

            when (id) {
                "first_session" -> {
                    newProgress = if (sessions.isNotEmpty()) 1f else 0f
                    if (newProgress >= 1f) unlocked = true
                }
                "streak_3" -> {
                    newProgress = streak.currentStreak.toFloat()
                    if (newProgress >= 3f) unlocked = true
                }
                "streak_7" -> {
                    newProgress = streak.currentStreak.toFloat()
                    if (newProgress >= 7f) unlocked = true
                }
                "streak_14" -> {
                    newProgress = streak.currentStreak.toFloat()
                    if (newProgress >= 14f) unlocked = true
                }
                "streak_30" -> {
                    newProgress = streak.currentStreak.toFloat()
                    if (newProgress >= 30f) unlocked = true
                }
                "hours_10" -> {
                    newProgress = totalFocusedHours
                    if (newProgress >= 10f) unlocked = true
                }
                "hours_50" -> {
                    newProgress = totalFocusedHours
                    if (newProgress >= 50f) unlocked = true
                }
                "hours_100" -> {
                    newProgress = totalFocusedHours
                    if (newProgress >= 100f) unlocked = true
                }
                "hours_500" -> {
                    newProgress = totalFocusedHours
                    if (newProgress >= 500f) unlocked = true
                }
                "hours_1000" -> {
                    newProgress = totalFocusedHours
                    if (newProgress >= 1000f) unlocked = true
                }
                "night_owl" -> {
                    val hasNightSession = sessions.any {
                        val cal = Calendar.getInstance().apply { timeInMillis = it.startTime }
                        val hour = cal.get(Calendar.HOUR_OF_DAY)
                        hour in 0..4
                    }
                    if (hasNightSession) {
                        newProgress = 1f
                        unlocked = true
                    }
                }
                "early_bird" -> {
                    val hasEarlySession = sessions.any {
                        val cal = Calendar.getInstance().apply { timeInMillis = it.startTime }
                        val hour = cal.get(Calendar.HOUR_OF_DAY)
                        hour in 5..7
                    }
                    if (hasEarlySession) {
                        newProgress = 1f
                        unlocked = true
                    }
                }
                "deep_work_master" -> {
                    val hasDeepWork = sessions.any {
                        it.actualFocusedSeconds >= 5400 && it.productivityRating == 5
                    }
                    if (hasDeepWork) {
                        newProgress = 1f
                        unlocked = true
                    }
                }
            }

            if (newProgress != ach.progress || unlocked != ach.unlocked) {
                database.achievementDao().updateAchievement(
                    ach.copy(
                        progress = newProgress,
                        unlocked = unlocked,
                        unlockedTimestamp = if (unlocked && ach.unlockedTimestamp == 0L) now else ach.unlockedTimestamp
                    )
                )
            }
        }
    }

    // JSON Export / Import
    suspend fun exportDataJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val sessionsArray = JSONArray()
        database.sessionDao().getAllSessions().forEach { s ->
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("startTime", s.startTime)
            obj.put("endTime", s.endTime)
            obj.put("durationSeconds", s.durationSeconds)
            obj.put("actualFocusedSeconds", s.actualFocusedSeconds)
            obj.put("pausedSeconds", s.pausedSeconds)
            obj.put("sessionType", s.sessionType)
            obj.put("subject", s.subject)
            obj.put("topic", s.topic)
            obj.put("category", s.category)
            obj.put("goal", s.goal)
            obj.put("notes", s.notes)
            obj.put("tags", s.tags)
            obj.put("productivityRating", s.productivityRating)
            obj.put("distractionCount", s.distractionCount)
            obj.put("distractionDetails", s.distractionDetails)
            obj.put("mood", s.mood)
            obj.put("energyLevel", s.energyLevel)
            obj.put("location", s.location)
            obj.put("completionStatus", s.completionStatus)
            sessionsArray.put(obj)
        }
        root.put("sessions", sessionsArray)

        val subjectsArray = JSONArray()
        database.subjectDao().getAllSubjects().forEach { s ->
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("name", s.name)
            obj.put("colorHex", s.colorHex)
            obj.put("iconName", s.iconName)
            obj.put("targetHours", s.targetHours)
            obj.put("description", s.description)
            subjectsArray.put(obj)
        }
        root.put("subjects", subjectsArray)

        val presetsArray = JSONArray()
        database.presetDao().getAllPresets().forEach { p ->
            val obj = JSONObject()
            obj.put("title", p.title)
            obj.put("type", p.type)
            obj.put("durationMinutes", p.durationMinutes)
            obj.put("subject", p.subject)
            obj.put("pomodoroFocusMin", p.pomodoroFocusMin)
            obj.put("pomodoroShortBreakMin", p.pomodoroShortBreakMin)
            obj.put("pomodoroLongBreakMin", p.pomodoroLongBreakMin)
            obj.put("pomodoroRounds", p.pomodoroRounds)
            presetsArray.put(obj)
        }
        root.put("presets", presetsArray)

        root.toString(2)
    }

    suspend fun exportSessionsCsv(): String = withContext(Dispatchers.IO) {
        val sessions = database.sessionDao().getAllSessions()
        val sb = StringBuilder()
        sb.append("ID,Start Time,End Time,Total Duration (s),Focused Duration (s),Paused Duration (s),Type,Subject,Topic,Productivity,Distractions,Mood,Energy,Notes\n")
        sessions.forEach { s ->
            sb.append("${s.id},${s.startTime},${s.endTime},${s.durationSeconds},${s.actualFocusedSeconds},${s.pausedSeconds},\"${s.sessionType}\",\"${s.subject}\",\"${s.topic}\",${s.productivityRating},${s.distractionCount},\"${s.mood}\",\"${s.energyLevel}\",\"${s.notes.replace("\"", "\"\"")}\"\n")
        }
        sb.toString()
    }

    suspend fun importDataJson(jsonString: String, replaceExisting: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val trimmed = jsonString.trim()
            if (trimmed.isEmpty()) return@withContext false

            if (replaceExisting) {
                database.sessionDao().clearAllSessions()
            }

            val sessionList = mutableListOf<StudySessionEntity>()

            fun parseSessionObject(obj: JSONObject): StudySessionEntity {
                val sTime = obj.optLong("startTime", System.currentTimeMillis())
                val dur = obj.optLong("durationSeconds", obj.optLong("actualFocusedSeconds", 1500L))
                val actDur = obj.optLong("actualFocusedSeconds", dur)
                val eTime = obj.optLong("endTime", sTime + (actDur * 1000L))

                return StudySessionEntity(
                    id = if (replaceExisting) obj.optLong("id", 0L) else 0L,
                    startTime = sTime,
                    endTime = eTime,
                    durationSeconds = dur,
                    actualFocusedSeconds = actDur,
                    pausedSeconds = obj.optLong("pausedSeconds", 0L),
                    sessionType = obj.optString("sessionType", "COUNTDOWN"),
                    subject = obj.optString("subject", "General"),
                    topic = obj.optString("topic", ""),
                    category = obj.optString("category", "Study"),
                    goal = obj.optString("goal", ""),
                    notes = obj.optString("notes", ""),
                    tags = obj.optString("tags", ""),
                    productivityRating = obj.optInt("productivityRating", 4).coerceIn(1, 5),
                    distractionCount = obj.optInt("distractionCount", 0),
                    distractionDetails = obj.optString("distractionDetails", ""),
                    mood = obj.optString("mood", "GOOD"),
                    energyLevel = obj.optString("energyLevel", "HIGH"),
                    location = obj.optString("location", "Desk"),
                    completionStatus = obj.optString("completionStatus", "COMPLETED")
                )
            }

            if (trimmed.startsWith("{")) {
                val root = JSONObject(trimmed)
                if (root.has("sessions")) {
                    val sessionsArray = root.getJSONArray("sessions")
                    for (i in 0 until sessionsArray.length()) {
                        val obj = sessionsArray.getJSONObject(i)
                        sessionList.add(parseSessionObject(obj))
                    }
                } else if (root.has("startTime") || root.has("subject") || root.has("durationSeconds")) {
                    // Single session object
                    sessionList.add(parseSessionObject(root))
                }

                // Also restore subjects if present
                if (root.has("subjects")) {
                    val subjectsArray = root.getJSONArray("subjects")
                    for (i in 0 until subjectsArray.length()) {
                        val obj = subjectsArray.getJSONObject(i)
                        val sName = obj.optString("name", "")
                        if (sName.isNotBlank()) {
                            database.subjectDao().insertSubject(
                                SubjectEntity(
                                    name = sName,
                                    colorHex = obj.optString("colorHex", "#6366F1"),
                                    iconName = obj.optString("iconName", "School"),
                                    targetHours = obj.optDouble("targetHours", 0.0).toFloat(),
                                    description = obj.optString("description", "")
                                )
                            )
                        }
                    }
                }

                // Also restore presets if present
                if (root.has("presets")) {
                    val presetsArray = root.getJSONArray("presets")
                    for (i in 0 until presetsArray.length()) {
                        val obj = presetsArray.getJSONObject(i)
                        val title = obj.optString("title", "")
                        if (title.isNotBlank()) {
                            database.presetDao().insertPreset(
                                PresetEntity(
                                    title = title,
                                    type = obj.optString("type", "COUNTDOWN"),
                                    durationMinutes = obj.optInt("durationMinutes", 25),
                                    subject = obj.optString("subject", "General"),
                                    pomodoroFocusMin = obj.optInt("pomodoroFocusMin", 25),
                                    pomodoroShortBreakMin = obj.optInt("pomodoroShortBreakMin", 5),
                                    pomodoroLongBreakMin = obj.optInt("pomodoroLongBreakMin", 15),
                                    pomodoroRounds = obj.optInt("pomodoroRounds", 4)
                                )
                            )
                        }
                    }
                }
            } else if (trimmed.startsWith("[")) {
                // Array of sessions
                val sessionsArray = JSONArray(trimmed)
                for (i in 0 until sessionsArray.length()) {
                    val obj = sessionsArray.getJSONObject(i)
                    sessionList.add(parseSessionObject(obj))
                }
            } else {
                return@withContext false
            }

            if (sessionList.isNotEmpty()) {
                database.sessionDao().insertSessions(sessionList)
            }
            checkAndUnlockAchievements()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

data class StreakInfo(
    val currentStreak: Int,
    val longestStreak: Int,
    val isStudiedToday: Boolean
)

fun calculateStreak(sessions: List<StudySessionEntity>): StreakInfo {
    if (sessions.isEmpty()) return StreakInfo(0, 0, false)

    // Group sessions by local date string YYYY-MM-DD
    val datesWithStudy = mutableSetOf<String>()
    val cal = Calendar.getInstance()

    sessions.forEach {
        cal.timeInMillis = it.startTime
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val dateKey = String.format("%04d-%02d-%02d", y, m, d)
        datesWithStudy.add(dateKey)
    }

    val todayCal = Calendar.getInstance()
    val todayKey = String.format("%04d-%02d-%02d", todayCal.get(Calendar.YEAR), todayCal.get(Calendar.MONTH) + 1, todayCal.get(Calendar.DAY_OF_MONTH))

    val isStudiedToday = datesWithStudy.contains(todayKey)

    // Check consecutive days starting from today or yesterday
    var currentStreak = 0
    val checkCal = Calendar.getInstance()
    if (!isStudiedToday) {
        checkCal.add(Calendar.DAY_OF_YEAR, -1)
    }

    while (true) {
        val k = String.format("%04d-%02d-%02d", checkCal.get(Calendar.YEAR), checkCal.get(Calendar.MONTH) + 1, checkCal.get(Calendar.DAY_OF_MONTH))
        if (datesWithStudy.contains(k)) {
            currentStreak++
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            break
        }
    }

    // Calculate longest streak by sorting unique dates
    val sortedDates = datesWithStudy.sorted()
    var longestStreak = 0
    var tempStreak = 0
    var prevCal: Calendar? = null

    for (dStr in sortedDates) {
        val parts = dStr.split("-").map { it.toInt() }
        val c = Calendar.getInstance().apply {
            set(parts[0], parts[1] - 1, parts[2], 0, 0, 0)
        }
        if (prevCal == null) {
            tempStreak = 1
        } else {
            val diff = (c.timeInMillis - prevCal.timeInMillis) / (1000 * 60 * 60 * 24)
            if (diff == 1L) {
                tempStreak++
            } else {
                tempStreak = 1
            }
        }
        if (tempStreak > longestStreak) longestStreak = tempStreak
        prevCal = c
    }

    if (currentStreak > longestStreak) longestStreak = currentStreak

    return StreakInfo(currentStreak, longestStreak, isStudiedToday)
}
