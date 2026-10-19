package com.niloy.focentra.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
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
        SettingEntity::class,
        StudyTaskEntity::class,
        ChapterEntity::class
    ],
    version = 4,
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
    abstract fun taskDao(): StudyTaskDao
    abstract fun chapterDao(): ChapterDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_sessions_startTime` ON `study_sessions` (`startTime`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_sessions_subject` ON `study_sessions` (`subject`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_sessions_completionStatus` ON `study_sessions` (`completionStatus`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `daily_tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `subject` TEXT NOT NULL, `dateEpochMs` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL, `createdTimestamp` INTEGER NOT NULL)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `chapters` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `subjectId` INTEGER NOT NULL, `subjectName` TEXT NOT NULL, `chapterName` TEXT NOT NULL, `totalClasses` INTEGER NOT NULL, `completedClassesCount` INTEGER NOT NULL, `isRevision1Completed` INTEGER NOT NULL, `isRevision2Completed` INTEGER NOT NULL, `orderIndex` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_chapters_subjectId` ON `chapters` (`subjectId`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `chapters` ADD COLUMN `tapCount` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `chapters` ADD COLUMN `status` TEXT NOT NULL DEFAULT 'পড়া শুরু হয় নাই'")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "focentra_database.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
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
            val achievementDao = database.achievementDao()
            val reminderDao = database.reminderDao()
            val settingDao = database.settingDao()
            val subjectDao = database.subjectDao()
            val chapterDao = database.chapterDao()

            // Pre-populate Bangla 1st Paper
            val banglaSubjectId = subjectDao.insertSubject(
                SubjectEntity(
                    name = "বাংলা ১ম পত্র",
                    colorHex = "#E11D48",
                    iconName = "MenuBook",
                    targetHours = 30f,
                    description = "উচ্চমাধ্যমিক বাংলা প্রথম পত্র সিলেবাস"
                )
            )

            val banglaChapters = listOf(
                "১. বাঙ্গালার নব্য লেখকদিগের প্রতি নিবেদন (গদ্য)",
                "২. অপরিচিতা (গদ্য)",
                "৩. বিলাসী (গদ্য)",
                "৪. গৃহ (গদ্য)",
                "৫. আহ্বান (গদ্য)",
                "৬. আমার পথ (গদ্য)",
                "৭. মানব-কল্যাণ (গদ্য)",
                "৮. মাসি-পিসি (গদ্য)",
                "৯. বায়ান্নর দিনগুলো (গদ্য)",
                "১০. রেইনকোট (গদ্য)",
                "১১. নেকলেস (গদ্য)",
                "১২. অর্ধাঙ্গী / সাহিত্যের খেলা (গদ্য)",
                "১৩. বিভীষণের প্রতি মেঘনাদ (পদ্য)",
                "১৪. সোনার তরী (পদ্য)",
                "১৫. বিদ্রোহী (পদ্য)",
                "১৬. প্রতিদান (পদ্য)",
                "১৭. সুচেতনা (পদ্য)",
                "১৮. তাহারেই পড়ে মনে (পদ্য)",
                "১৯. পদ্মা (পদ্য)",
                "২০. ১৮ বছর বয়স (পদ্য)",
                "২১. ফেব্রুয়ারি ১৯৬৯ (পদ্য)",
                "২২. আমি কিংবদন্তির কথা বলছি (পদ্য)",
                "২৩. নূরলদীনের কথা মনে পড়ে যায় (পদ্য)",
                "২৪. ছবি (পদ্য)",
                "২৫. লালসালু (সহপাঠ - উপন্যাস)",
                "২৬. সিরাজউদ্দৌলা (সহপাঠ - নাটক)"
            )

            banglaChapters.forEachIndexed { index, title ->
                chapterDao.insertChapter(
                    ChapterEntity(
                        subjectId = banglaSubjectId,
                        subjectName = "বাংলা ১ম পত্র",
                        chapterName = title,
                        tapCount = 0,
                        status = "পড়া শুরু হয় নাই",
                        orderIndex = index
                    )
                )
            }

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
