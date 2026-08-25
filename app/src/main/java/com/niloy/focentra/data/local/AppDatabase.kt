package com.niloy.focentra.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.niloy.focentra.data.local.dao.*
import com.niloy.focentra.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        StudySessionEntity::class,
        SubjectEntity::class,
        TopicEntity::class,
        StudyGoalEntity::class,
        PresetEntity::class,
        AchievementEntity::class,
        ReminderEntity::class,
        SettingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): StudySessionDao
    abstract fun subjectDao(): SubjectDao
    abstract fun topicDao(): TopicDao
    abstract fun goalDao(): StudyGoalDao
    abstract fun presetDao(): PresetDao
    abstract fun achievementDao(): AchievementDao
    abstract fun reminderDao(): ReminderDao
    abstract fun settingDao(): SettingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "focentra_database.db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            val subjectDao = database.subjectDao()
            val topicDao = database.topicDao()
            val goalDao = database.goalDao()
            val presetDao = database.presetDao()
            val achievementDao = database.achievementDao()
            val reminderDao = database.reminderDao()
            val settingDao = database.settingDao()

            // Initial Subjects
            val mathId = subjectDao.insertSubject(
                SubjectEntity(name = "Mathematics", colorHex = "#6366F1", iconName = "Calculate", targetHours = 25f, description = "Calculus, Linear Algebra, Statistics", orderIndex = 0)
            )
            val progId = subjectDao.insertSubject(
                SubjectEntity(name = "Programming", colorHex = "#10B981", iconName = "Code", targetHours = 30f, description = "Algorithms, Kotlin, Android, Systems", orderIndex = 1)
            )
            val physicsId = subjectDao.insertSubject(
                SubjectEntity(name = "Physics", colorHex = "#F59E0B", iconName = "Science", targetHours = 20f, description = "Mechanics, Electromagnetism, Quantum", orderIndex = 2)
            )
            val chemId = subjectDao.insertSubject(
                SubjectEntity(name = "Chemistry", colorHex = "#EC4899", iconName = "Biotech", targetHours = 15f, description = "Organic, Inorganic, Physical chemistry", orderIndex = 3)
            )
            val engId = subjectDao.insertSubject(
                SubjectEntity(name = "English & Literature", colorHex = "#8B5CF6", iconName = "MenuBook", targetHours = 12f, description = "Reading, Writing, Grammar", orderIndex = 4)
            )
            subjectDao.insertSubject(
                SubjectEntity(name = "General Study", colorHex = "#06B6D4", iconName = "School", targetHours = 10f, description = "General revision and research", orderIndex = 5)
            )

            // Topics
            topicDao.insertTopic(TopicEntity(subjectId = mathId, subjectName = "Mathematics", name = "Calculus"))
            topicDao.insertTopic(TopicEntity(subjectId = mathId, subjectName = "Mathematics", name = "Linear Algebra"))
            topicDao.insertTopic(TopicEntity(subjectId = mathId, subjectName = "Mathematics", name = "Discrete Math"))
            topicDao.insertTopic(TopicEntity(subjectId = progId, subjectName = "Programming", name = "Data Structures & Algorithms"))
            topicDao.insertTopic(TopicEntity(subjectId = progId, subjectName = "Programming", name = "Android Development"))
            topicDao.insertTopic(TopicEntity(subjectId = physicsId, subjectName = "Physics", name = "Electromagnetism"))
            topicDao.insertTopic(TopicEntity(subjectId = physicsId, subjectName = "Physics", name = "Classical Mechanics"))
            topicDao.insertTopic(TopicEntity(subjectId = chemId, subjectName = "Chemistry", name = "Organic Synthesis"))
            topicDao.insertTopic(TopicEntity(subjectId = engId, subjectName = "English & Literature", name = "Essay Writing"))

            // Initial Goals
            goalDao.insertGoal(StudyGoalEntity(periodType = "DAILY", targetMinutes = 480, subjectFilter = "")) // 8 hours
            goalDao.insertGoal(StudyGoalEntity(periodType = "WEEKLY", targetMinutes = 2400, subjectFilter = "")) // 40 hours
            goalDao.insertGoal(StudyGoalEntity(periodType = "MONTHLY", targetMinutes = 9600, subjectFilter = "")) // 160 hours
            goalDao.insertGoal(StudyGoalEntity(periodType = "YEARLY", targetMinutes = 90000, subjectFilter = "")) // 1500 hours

            // Presets
            presetDao.insertPreset(PresetEntity(title = "Classic Pomodoro", type = "POMODORO", durationMinutes = 25, subject = "General Study", pomodoroFocusMin = 25, pomodoroShortBreakMin = 5, pomodoroLongBreakMin = 15, pomodoroRounds = 4))
            presetDao.insertPreset(PresetEntity(title = "Deep Work Block", type = "COUNTDOWN", durationMinutes = 90, subject = "Programming", pomodoroFocusMin = 90, tags = "#deepwork,#flow"))
            presetDao.insertPreset(PresetEntity(title = "50/10 Study Cycle", type = "POMODORO", durationMinutes = 50, subject = "Mathematics", pomodoroFocusMin = 50, pomodoroShortBreakMin = 10, pomodoroLongBreakMin = 20, pomodoroRounds = 4))
            presetDao.insertPreset(PresetEntity(title = "Quick Revision Sprint", type = "COUNTDOWN", durationMinutes = 15, subject = "Physics", tags = "#revision"))
            presetDao.insertPreset(PresetEntity(title = "Free Focus Stopwatch", type = "STOPWATCH", durationMinutes = 0, subject = "General Study"))

            // Achievements
            val achievements = listOf(
                AchievementEntity(id = "first_session", title = "First Step", description = "Complete your very first focus study session", iconName = "EmojiEvents", category = "MILESTONE", target = 1f),
                AchievementEntity(id = "streak_3", title = "Momentum", description = "Maintain a 3-day study streak", iconName = "LocalFireDepartment", category = "STREAK", target = 3f),
                AchievementEntity(id = "streak_7", title = "Habit Builder", description = "Maintain a 7-day study streak", iconName = "LocalFireDepartment", category = "STREAK", target = 7f),
                AchievementEntity(id = "streak_14", title = "Unstoppable", description = "Maintain a 14-day study streak", iconName = "LocalFireDepartment", category = "STREAK", target = 14f),
                AchievementEntity(id = "streak_30", title = "Iron Will", description = "Maintain a 30-day study streak", iconName = "Whatshot", category = "STREAK", target = 30f),
                AchievementEntity(id = "hours_10", title = "Bronze Scholar", description = "Accumulate 10 hours of focused study time", iconName = "Timer", category = "TIME", target = 10f),
                AchievementEntity(id = "hours_50", title = "Silver Scholar", description = "Accumulate 50 hours of focused study time", iconName = "WorkspacePremium", category = "TIME", target = 50f),
                AchievementEntity(id = "hours_100", title = "Gold Scholar", description = "Accumulate 100 hours of focused study time", iconName = "School", category = "TIME", target = 100f),
                AchievementEntity(id = "hours_500", title = "Diamond Scholar", description = "Accumulate 500 hours of focused study time", iconName = "Diamond", category = "TIME", target = 500f),
                AchievementEntity(id = "hours_1000", title = "Grandmaster Mind", description = "Accumulate 1,000 hours of focused study time", iconName = "MilitaryTech", category = "TIME", target = 1000f),
                AchievementEntity(id = "daily_goals_30", title = "Goal Crusher", description = "Complete daily study goals on 30 separate days", iconName = "GpsFixed", category = "GOAL", target = 30f),
                AchievementEntity(id = "night_owl", title = "Night Owl", description = "Complete a study session past midnight (12 AM - 4 AM)", iconName = "NightsStay", category = "SPECIAL", target = 1f),
                AchievementEntity(id = "early_bird", title = "Early Bird", description = "Complete a study session in early morning (5 AM - 7 AM)", iconName = "WbSunny", category = "SPECIAL", target = 1f),
                AchievementEntity(id = "deep_work_master", title = "Deep Work Master", description = "Complete a continuous 90+ minute focused session with 5/5 productivity", iconName = "Psychology", category = "SPECIAL", target = 1f)
            )
            achievementDao.insertAchievements(achievements)

            // Reminders
            reminderDao.insertReminder(ReminderEntity(title = "Morning Focus Session", hour = 9, minute = 0, message = "Ready for today's high-priority study?"))
            reminderDao.insertReminder(ReminderEntity(title = "Evening Goal Checkup", hour = 20, minute = 0, message = "Keep your study streak alive today!"))

            // Default Settings
            settingDao.setSetting(SettingEntity("theme", "midnight"))
            settingDao.setSetting(SettingEntity("screen_awake", "true"))
            settingDao.setSetting(SettingEntity("sound_enabled", "true"))
            settingDao.setSetting(SettingEntity("vibration_intensity", "MEDIUM"))
            settingDao.setSetting(SettingEntity("anim_intensity", "STANDARD"))
            settingDao.setSetting(SettingEntity("language", "en"))
            settingDao.setSetting(SettingEntity("onboarding_completed", "true"))
        }
    }
}
