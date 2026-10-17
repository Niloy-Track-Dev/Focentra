package com.niloy.focentra.data.repository

import android.content.Context
import android.content.Intent
import com.niloy.focentra.data.local.AppDatabase
import com.niloy.focentra.data.local.entity.*
import com.niloy.focentra.data.provider.DaynexaContract
import com.niloy.focentra.data.provider.DaynexaIntegrationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar

class StudyRepository(private val database: AppDatabase, context: Context) {
    
    private val appContext = context.applicationContext
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val allSessions: Flow<List<StudySessionEntity>> = database.sessionDao().getAllSessionsFlow()
    val allSubjects: Flow<List<SubjectEntity>> = database.subjectDao().getAllSubjectsFlow()
    val allTopics: Flow<List<TopicEntity>> = database.topicDao().getAllTopicsFlow()
    val allGoals: Flow<List<StudyGoalEntity>> = database.goalDao().getActiveGoalsFlow()
    val allPresets: Flow<List<PresetEntity>> = database.presetDao().getAllPresetsFlow()
    val allAchievements: Flow<List<AchievementEntity>> = database.achievementDao().getAllAchievementsFlow()
    val allReminders: Flow<List<ReminderEntity>> = database.reminderDao().getAllRemindersFlow()
    val allSettings: Flow<List<SettingEntity>> = database.settingDao().getAllSettingsFlow()
    val allTasks: Flow<List<StudyTaskEntity>> = database.taskDao().getAllTasksFlow()

    suspend fun insertTask(task: StudyTaskEntity) = withContext(Dispatchers.IO) {
        database.taskDao().insertTask(task)
    }

    suspend fun updateTask(task: StudyTaskEntity) = withContext(Dispatchers.IO) {
        database.taskDao().updateTask(task)
    }

    suspend fun deleteTask(task: StudyTaskEntity) = withContext(Dispatchers.IO) {
        database.taskDao().deleteTask(task)
    }

    suspend fun clearCompletedTasks() = withContext(Dispatchers.IO) {
        database.taskDao().clearCompletedTasks()
    }

    suspend fun insertSession(session: StudySessionEntity): Long = withContext(Dispatchers.IO) {
        val id = database.sessionDao().insertSession(session)
        val savedSession = session.copy(id = id)
        checkAndUnlockAchievements()
        sendDaynexaSessionBroadcast(savedSession)
        id
    }

    private fun sendDaynexaSessionBroadcast(session: StudySessionEntity) {
        repositoryScope.launch {
            try {
                // 1. Verify User Consent Gate
                val consent = database.settingDao().getSettingValue(DaynexaContract.SETTING_DAYNEXA_CONSENT)
                if (consent.equals("true", ignoreCase = true)) {
                    
                    // 2. Map to Daynexa Integration API v1 Contract
                    val intent = Intent(DaynexaContract.ACTION_SESSION_COMPLETED).apply {
                        putExtra(DaynexaContract.COLUMN_SESSION_ID, session.id)
                        putExtra(DaynexaContract.COLUMN_SUBJECT, session.subject)
                        putExtra(DaynexaContract.COLUMN_TOPIC, session.topic)
                        putExtra(DaynexaContract.COLUMN_START_TIME, session.startTime)
                        putExtra(DaynexaContract.COLUMN_END_TIME, session.endTime)
                        
                        val durationMin = if (session.actualFocusedSeconds > 0) {
                            (session.actualFocusedSeconds / 60).coerceAtLeast(1)
                        } else {
                            (session.durationSeconds / 60).coerceAtLeast(1)
                        }
                        putExtra(DaynexaContract.COLUMN_DURATION, durationMin)
                        putExtra(DaynexaContract.COLUMN_COMPLETION_STATUS, session.completionStatus.uppercase())
                        
                        val focusScore = DaynexaIntegrationHelper.calculateFocusScore(session)
                        putExtra(DaynexaContract.COLUMN_FOCUS_SCORE, focusScore)
                        putExtra(DaynexaContract.COLUMN_SCHEMA_VERSION, DaynexaContract.SCHEMA_VERSION)
                    }

                    // 3. SECURE BROADCAST: Use permission protection
                    // Only apps that declare com.focentra.permission.READ_SESSIONS in their manifest can receive this.
                    // This prevents data leakage to unauthorized apps.
                    appContext.sendBroadcast(intent, "com.focentra.permission.READ_SESSIONS")
                }
            } catch (e: Exception) {
                // Silently fail integration broadcasts to avoid breaking core app flow
                e.printStackTrace()
            }
        }
    }

    suspend fun updateSession(session: StudySessionEntity) = withContext(Dispatchers.IO) {
        database.sessionDao().updateSession(session)
        checkAndUnlockAchievements()
    }

    suspend fun deleteSession(session: StudySessionEntity) = withContext(Dispatchers.IO) {
        database.sessionDao().deleteSession(session)
        checkAndUnlockAchievements()
    }

    suspend fun clearAllSessions() = withContext(Dispatchers.IO) {
        database.sessionDao().clearAllSessions()
    }

    suspend fun clearAllAppData() = withContext(Dispatchers.IO) {
        database.clearAllTables()
        AppDatabase.populateInitialData(database)
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

    // JSON Export / Import v2 (Full App State Backup)
    suspend fun exportDataJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", "Focentra")
        root.put("version", 2)
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

        val topicsArray = JSONArray()
        database.topicDao().getAllTopics().forEach { t ->
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("subjectId", t.subjectId)
            obj.put("subjectName", t.subjectName)
            obj.put("name", t.name)
            topicsArray.put(obj)
        }
        root.put("topics", topicsArray)

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

        val goalsArray = JSONArray()
        database.goalDao().getAllGoals().forEach { g ->
            val obj = JSONObject()
            obj.put("periodType", g.periodType)
            obj.put("targetMinutes", g.targetMinutes)
            goalsArray.put(obj)
        }
        root.put("goals", goalsArray)

        val remindersArray = JSONArray()
        database.reminderDao().getAllReminders().forEach { r ->
            val obj = JSONObject()
            obj.put("title", r.title)
            obj.put("hour", r.hour)
            obj.put("minute", r.minute)
            obj.put("daysOfWeek", r.daysOfWeek)
            obj.put("enabled", r.enabled)
            obj.put("message", r.message)
            remindersArray.put(obj)
        }
        root.put("reminders", remindersArray)

        // Include Flashcards and Exam Targets stored in settings
        val flashcardsJson = getSetting("flashcards_json", "")
        if (flashcardsJson.isNotBlank()) {
            root.put("flashcards", JSONArray(flashcardsJson))
        }

        val examTargetsJson = getSetting("exam_targets_json", "")
        if (examTargetsJson.isNotBlank()) {
            root.put("examTargets", JSONArray(examTargetsJson))
        }

        root.toString()
    }

    suspend fun exportDataJsonStream(outputStream: java.io.OutputStream) = withContext(Dispatchers.IO) {
        val writer = android.util.JsonWriter(outputStream.bufferedWriter(Charsets.UTF_8))
        writer.use { jWriter ->
            jWriter.beginObject()
            jWriter.name("app").value("Focentra")
            jWriter.name("version").value(2)
            jWriter.name("exportedAt").value(System.currentTimeMillis())

            jWriter.name("sessions")
            jWriter.beginArray()
            database.sessionDao().getAllSessions().forEach { s ->
                jWriter.beginObject()
                jWriter.name("id").value(s.id)
                jWriter.name("startTime").value(s.startTime)
                jWriter.name("endTime").value(s.endTime)
                jWriter.name("durationSeconds").value(s.durationSeconds)
                jWriter.name("actualFocusedSeconds").value(s.actualFocusedSeconds)
                jWriter.name("pausedSeconds").value(s.pausedSeconds)
                jWriter.name("sessionType").value(s.sessionType)
                jWriter.name("subject").value(s.subject)
                jWriter.name("topic").value(s.topic)
                jWriter.name("category").value(s.category)
                jWriter.name("goal").value(s.goal)
                jWriter.name("notes").value(s.notes)
                jWriter.name("tags").value(s.tags)
                jWriter.name("productivityRating").value(s.productivityRating)
                jWriter.name("distractionCount").value(s.distractionCount)
                jWriter.name("distractionDetails").value(s.distractionDetails)
                jWriter.name("mood").value(s.mood)
                jWriter.name("energyLevel").value(s.energyLevel)
                jWriter.name("location").value(s.location)
                jWriter.name("completionStatus").value(s.completionStatus)
                jWriter.endObject()
            }
            jWriter.endArray()

            jWriter.name("subjects")
            jWriter.beginArray()
            database.subjectDao().getAllSubjects().forEach { s ->
                jWriter.beginObject()
                jWriter.name("id").value(s.id)
                jWriter.name("name").value(s.name)
                jWriter.name("colorHex").value(s.colorHex)
                jWriter.name("iconName").value(s.iconName)
                jWriter.name("targetHours").value(s.targetHours.toDouble())
                jWriter.name("description").value(s.description)
                jWriter.endObject()
            }
            jWriter.endArray()

            jWriter.name("topics")
            jWriter.beginArray()
            database.topicDao().getAllTopics().forEach { t ->
                jWriter.beginObject()
                jWriter.name("id").value(t.id)
                jWriter.name("subjectId").value(t.subjectId)
                jWriter.name("subjectName").value(t.subjectName)
                jWriter.name("name").value(t.name)
                jWriter.endObject()
            }
            jWriter.endArray()

            jWriter.name("presets")
            jWriter.beginArray()
            database.presetDao().getAllPresets().forEach { p ->
                jWriter.beginObject()
                jWriter.name("title").value(p.title)
                jWriter.name("type").value(p.type)
                jWriter.name("durationMinutes").value(p.durationMinutes)
                jWriter.name("subject").value(p.subject)
                jWriter.name("pomodoroFocusMin").value(p.pomodoroFocusMin)
                jWriter.name("pomodoroShortBreakMin").value(p.pomodoroShortBreakMin)
                jWriter.name("pomodoroLongBreakMin").value(p.pomodoroLongBreakMin)
                jWriter.name("pomodoroRounds").value(p.pomodoroRounds)
                jWriter.endObject()
            }
            jWriter.endArray()

            jWriter.name("goals")
            jWriter.beginArray()
            database.goalDao().getAllGoals().forEach { g ->
                jWriter.beginObject()
                jWriter.name("periodType").value(g.periodType)
                jWriter.name("targetMinutes").value(g.targetMinutes)
                jWriter.endObject()
            }
            jWriter.endArray()

            jWriter.name("reminders")
            jWriter.beginArray()
            database.reminderDao().getAllReminders().forEach { r ->
                jWriter.beginObject()
                jWriter.name("title").value(r.title)
                jWriter.name("hour").value(r.hour)
                jWriter.name("minute").value(r.minute)
                jWriter.name("daysOfWeek").value(r.daysOfWeek)
                jWriter.name("enabled").value(r.enabled)
                jWriter.name("message").value(r.message)
                jWriter.endObject()
            }
            jWriter.endArray()

            val examTargetsJson = getSetting("exam_targets_json", "")
            if (examTargetsJson.isNotBlank()) {
                jWriter.name("examTargets").value(examTargetsJson)
            }

            jWriter.endObject()
        }
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

                // Restore subjects if present
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

                // Restore topics if present
                if (root.has("topics")) {
                    val topicsArray = root.getJSONArray("topics")
                    for (i in 0 until topicsArray.length()) {
                        val obj = topicsArray.getJSONObject(i)
                        val tName = obj.optString("name", "")
                        val sName = obj.optString("subjectName", "General")
                        if (tName.isNotBlank()) {
                            database.topicDao().insertTopic(
                                TopicEntity(
                                    subjectId = obj.optLong("subjectId", 0L),
                                    subjectName = sName,
                                    name = tName
                                )
                            )
                        }
                    }
                }

                // Restore presets if present
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

                // Restore goals if present
                if (root.has("goals")) {
                    val goalsArray = root.getJSONArray("goals")
                    for (i in 0 until goalsArray.length()) {
                        val obj = goalsArray.getJSONObject(i)
                        val periodType = obj.optString("periodType", "")
                        if (periodType.isNotBlank()) {
                            database.goalDao().insertGoal(
                                StudyGoalEntity(
                                    periodType = periodType,
                                    targetMinutes = obj.optInt("targetMinutes", 480)
                                )
                            )
                        }
                    }
                }

                // Restore reminders if present
                if (root.has("reminders")) {
                    val remindersArray = root.getJSONArray("reminders")
                    for (i in 0 until remindersArray.length()) {
                        val obj = remindersArray.getJSONObject(i)
                        val title = obj.optString("title", "")
                        if (title.isNotBlank()) {
                            database.reminderDao().insertReminder(
                                ReminderEntity(
                                    title = title,
                                    hour = obj.optInt("hour", 9),
                                    minute = obj.optInt("minute", 0),
                                    daysOfWeek = obj.optString("daysOfWeek", "MON,TUE,WED,THU,FRI,SAT,SUN"),
                                    enabled = obj.optBoolean("enabled", true),
                                    message = obj.optString("message", "Time for focus study session")
                                )
                            )
                        }
                    }
                }

                // Restore flashcards if present
                if (root.has("flashcards")) {
                    database.settingDao().setSetting(
                        com.niloy.focentra.data.local.entity.SettingEntity("flashcards_json", root.getJSONArray("flashcards").toString())
                    )
                }

                // Restore exam targets if present
                if (root.has("examTargets")) {
                    database.settingDao().setSetting(
                        com.niloy.focentra.data.local.entity.SettingEntity("exam_targets_json", root.getJSONArray("examTargets").toString())
                    )
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
                // Chunk batch insertions to prevent SQLite transaction limits and memory spikes
                sessionList.chunked(500).forEach { chunk ->
                    database.sessionDao().insertSessions(chunk)
                }
            }
            checkAndUnlockAchievements()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importDataJsonStream(inputStream: java.io.InputStream, replaceExisting: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            if (replaceExisting) {
                database.sessionDao().clearAllSessions()
            }

            val reader = android.util.JsonReader(inputStream.bufferedReader(Charsets.UTF_8))
            val sessionList = mutableListOf<StudySessionEntity>()

            fun readSessionObject(jReader: android.util.JsonReader): StudySessionEntity {
                var sTime = System.currentTimeMillis()
                var dur = 1500L
                var actDur = 1500L
                var eTime = 0L
                var sType = "COUNTDOWN"
                var subj = "General"
                var top = ""
                var cat = "Study"
                var gl = ""
                var nts = ""
                var tgs = ""
                var prod = 4
                var distCount = 0
                var distDetails = ""
                var md = "GOOD"
                var energy = "HIGH"
                var loc = "Desk"
                var compStatus = "COMPLETED"

                jReader.beginObject()
                while (jReader.hasNext()) {
                    when (jReader.nextName()) {
                        "startTime" -> sTime = jReader.nextLong()
                        "durationSeconds" -> dur = jReader.nextLong()
                        "actualFocusedSeconds" -> actDur = jReader.nextLong()
                        "endTime" -> eTime = jReader.nextLong()
                        "sessionType" -> sType = jReader.nextString()
                        "subject" -> subj = jReader.nextString()
                        "topic" -> top = jReader.nextString()
                        "category" -> cat = jReader.nextString()
                        "goal" -> gl = jReader.nextString()
                        "notes" -> nts = jReader.nextString()
                        "tags" -> tgs = jReader.nextString()
                        "productivityRating" -> prod = jReader.nextInt().coerceIn(1, 5)
                        "distractionCount" -> distCount = jReader.nextInt()
                        "distractionDetails" -> distDetails = jReader.nextString()
                        "mood" -> md = jReader.nextString()
                        "energyLevel" -> energy = jReader.nextString()
                        "location" -> loc = jReader.nextString()
                        "completionStatus" -> compStatus = jReader.nextString()
                        else -> jReader.skipValue()
                    }
                }
                jReader.endObject()

                if (eTime == 0L) eTime = sTime + (actDur * 1000L)

                return StudySessionEntity(
                    startTime = sTime,
                    endTime = eTime,
                    durationSeconds = dur,
                    actualFocusedSeconds = actDur,
                    pausedSeconds = 0L,
                    sessionType = sType,
                    subject = subj,
                    topic = top,
                    category = cat,
                    goal = gl,
                    notes = nts,
                    tags = tgs,
                    productivityRating = prod,
                    distractionCount = distCount,
                    distractionDetails = distDetails,
                    mood = md,
                    energyLevel = energy,
                    location = loc,
                    completionStatus = compStatus
                )
            }

            reader.beginObject()
            while (reader.hasNext()) {
                val name = reader.nextName()
                if (name == "sessions") {
                    reader.beginArray()
                    while (reader.hasNext()) {
                        val session = readSessionObject(reader)
                        sessionList.add(session)
                        if (sessionList.size >= 500) {
                            database.sessionDao().insertSessions(sessionList)
                            sessionList.clear()
                        }
                    }
                    reader.endArray()
                } else {
                    reader.skipValue()
                }
            }
            reader.endObject()

            if (sessionList.isNotEmpty()) {
                database.sessionDao().insertSessions(sessionList)
            }
            checkAndUnlockAchievements()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback for simple/other JSON formats
            try {
                val content = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                importDataJson(content, replaceExisting)
            } catch (e2: Exception) {
                e2.printStackTrace()
                false
            }
        }
    }

    suspend fun importSessionsCsvStream(inputStream: java.io.InputStream): Boolean = withContext(Dispatchers.IO) {
        try {
            val reader = inputStream.bufferedReader(Charsets.UTF_8)
            val lines = reader.readLines()
            if (lines.isEmpty()) return@withContext false

            val sessionList = mutableListOf<StudySessionEntity>()
            // Header skipped or checked
            val dataLines = if (lines.first().contains("Start Time", ignoreCase = true) || lines.first().contains("Subject", ignoreCase = true)) {
                lines.drop(1)
            } else {
                lines
            }

            fun parseCsvLine(line: String): List<String> {
                val result = mutableListOf<String>()
                val cur = StringBuilder()
                var inQuotes = false
                for (ch in line) {
                    if (ch == '"') {
                        inQuotes = !inQuotes
                    } else if (ch == ',' && !inQuotes) {
                        result.add(cur.toString().trim())
                        cur.clear()
                    } else {
                        cur.append(ch)
                    }
                }
                result.add(cur.toString().trim())
                return result
            }

            for (line in dataLines) {
                if (line.isBlank()) continue
                val parts = parseCsvLine(line)
                if (parts.size >= 8) {
                    // Standard columns: ID,Start Time,End Time,Total Duration (s),Focused Duration (s),Paused Duration (s),Type,Subject,Topic,Productivity,Distractions,Mood,Energy,Notes
                    val sTime = parts.getOrNull(1)?.toLongOrNull() ?: System.currentTimeMillis()
                    val eTime = parts.getOrNull(2)?.toLongOrNull() ?: System.currentTimeMillis()
                    val totalDur = parts.getOrNull(3)?.toLongOrNull() ?: 1500L
                    val focusedDur = parts.getOrNull(4)?.toLongOrNull() ?: totalDur
                    val pausedDur = parts.getOrNull(5)?.toLongOrNull() ?: 0L
                    val sType = parts.getOrNull(6)?.ifBlank { "COUNTDOWN" } ?: "COUNTDOWN"
                    val subj = parts.getOrNull(7)?.ifBlank { "General" } ?: "General"
                    val top = parts.getOrNull(8) ?: ""
                    val prod = parts.getOrNull(9)?.toIntOrNull()?.coerceIn(1, 5) ?: 4
                    val distCount = parts.getOrNull(10)?.toIntOrNull() ?: 0
                    val md = parts.getOrNull(11) ?: "GOOD"
                    val energy = parts.getOrNull(12) ?: "HIGH"
                    val nts = parts.getOrNull(13) ?: ""

                    sessionList.add(
                        StudySessionEntity(
                            startTime = sTime,
                            endTime = eTime,
                            durationSeconds = totalDur,
                            actualFocusedSeconds = focusedDur,
                            pausedSeconds = pausedDur,
                            sessionType = sType,
                            subject = subj,
                            topic = top,
                            category = "Study",
                            goal = "",
                            notes = nts,
                            tags = "",
                            productivityRating = prod,
                            distractionCount = distCount,
                            distractionDetails = "",
                            mood = md,
                            energyLevel = energy,
                            location = "Desk",
                            completionStatus = "COMPLETED"
                        )
                    )

                    if (sessionList.size >= 500) {
                        database.sessionDao().insertSessions(sessionList)
                        sessionList.clear()
                    }
                }
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
