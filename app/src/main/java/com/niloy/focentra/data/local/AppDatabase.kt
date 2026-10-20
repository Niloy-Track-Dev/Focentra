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

data class SyllabusData(
    val name: String,
    val colorHex: String,
    val iconName: String,
    val targetHours: Float,
    val description: String,
    val chapters: List<String>
)

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
    version = 5,
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

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `chapters` ADD COLUMN `lastStudiedTimestamp` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `chapters` ADD COLUMN `completedTimestamp` INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "focentra_database.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
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

            // Initial seed subjects and chapters configuration
            val syllabiToSeed = listOf(
                SyllabusData(
                    name = "বাংলা ১ম পত্র",
                    colorHex = "#E11D48",
                    iconName = "MenuBook",
                    targetHours = 30f,
                    description = "উচ্চমাধ্যমিক বাংলা প্রথম পত্র সিলেবাস",
                    chapters = listOf(
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
                ),
                SyllabusData(
                    name = "বাংলা ২য় পত্র",
                    colorHex = "#F59E0B",
                    iconName = "MenuBook",
                    targetHours = 30f,
                    description = "উচ্চমাধ্যমিক বাংলা দ্বিতীয় পত্র সিলেবাস",
                    chapters = listOf(
                        "১. বাংলা উচ্চারণের নিয়ম (ব্যাকরণ)",
                        "২. বাংলা বানানের নিয়ম (ব্যাকরণ)",
                        "৩. বাংলা ভাষার ব্যাকরণিক শব্দশ্রেণি (ব্যাকরণ)",
                        "৪. বাংলা শব্দ গঠন: উপসর্গ, প্রত্যয় ও সমাস (ব্যাকরণ)",
                        "৫. বাক্য তত্ত্ব: বাক্যের শ্রেণিবিভাগ ও রূপান্তর (ব্যাকরণ)",
                        "৬. বাংলা ভাষার অপপ্রয়োগ ও শুদ্ধ প্রয়োগ (ব্যাকরণ)",
                        "৭. পারিভাষিক শব্দ অথবা অনুবাদ (নির্মিতি)",
                        "৮. দিনলিপি লিখন অথবা প্রতিবেদন তৈরি (নির্মিতি)",
                        "৯. বৈদ্যুতিন চিঠি (Email) অথবা আবেদনপত্র (নির্মিতি)",
                        "১০. সারাংশ/সারসংক্ষেপ অথবা ভাব-সম্প্রসারণ (নির্মিতি)",
                        "১১. সংলাপ লিখন অথবা ক্ষুদে গল্প রচনা (নির্মিতি)",
                        "১২. প্রবন্ধ-নিবন্ধ লিখন (রচনা) (নির্মিতি)"
                    )
                ),
                SyllabusData(
                    name = "English 1st Paper",
                    colorHex = "#3B82F6",
                    iconName = "School",
                    targetHours = 30f,
                    description = "HSC English 1st Paper Syllabus",
                    chapters = listOf(
                        "1. Unit: People or Institutions Making History",
                        "2. Unit: Education and Life",
                        "3. Unit: Youthful Achievers",
                        "4. Unit: Human Relationship",
                        "5. Unit: Art and Craft",
                        "6. Unit: Tour and Travels",
                        "7. Unit: Environment and Nature",
                        "8. Unit: Myths and Literature",
                        "9. Unit: Human Rights",
                        "10. Unit: Diaspora",
                        "11. Unit: Peace and Conflict",
                        "12. Reading: Seen Comprehension Passages & Poems",
                        "13. Reading: Unseen Comprehension Passages",
                        "14. Reading: Information Transfer (Table Completion)",
                        "15. Reading: Flow Chart Creation",
                        "16. Reading: Summary Writing",
                        "17. Reading: Cloze Test with Clues",
                        "18. Reading: Cloze Test without Clues",
                        "19. Reading: Rearranging Sentences",
                        "20. Writing: Graph and Chart Interpretation & Analysis",
                        "21. Writing: Story Completing (Based on hints)",
                        "22. Writing: Informal Letters",
                        "23. Writing: E-mail Writing"
                    )
                ),
                SyllabusData(
                    name = "English 2nd Paper",
                    colorHex = "#06B6D4",
                    iconName = "School",
                    targetHours = 30f,
                    description = "HSC English 2nd Paper Syllabus",
                    chapters = listOf(
                        "1. Prepositions: Appropriate Prepositions & Use in Context",
                        "2. Special Uses of Some Words and Phrases",
                        "3. Completing Sentences (Conditionals, Infinitives, Gerunds, Clauses)",
                        "4. Right Form of Verbs (Subject-Verb Agreement & Tenses)",
                        "5. Narrative Style (Direct to Indirect Speech)",
                        "6. Modifiers (Pre-modifiers & Post-modifiers)",
                        "7. Sentence Connectors (Logical Linkers)",
                        "8. Synonyms and Antonyms",
                        "9. Punctuation (Capitalization & Marks)",
                        "10. Formal Letters (Job Application with CV, Complaint, Inquiry)",
                        "11. Paragraph Writing 1: Listing, Definition, Descriptive",
                        "12. Paragraph Writing 2: Comparison-Contrast /Cause-Effect"
                    )
                ),
                SyllabusData(
                    name = "পদার্থবিজ্ঞান ১ম পত্র",
                    colorHex = "#8B5CF6",
                    iconName = "Psychology",
                    targetHours = 30f,
                    description = "উচ্চমাধ্যমিক পদার্থবিজ্ঞান প্রথম পত্র সিলেবাস",
                    chapters = listOf(
                        "১. ভৌত জগৎ ও পরিমাপ",
                        "২. ভেক্টর",
                        "৩. গতিবিদ্যা",
                        "৪. নিউটনীয় বলবিদ্যা",
                        "৫. কাজ, শক্তি ও ক্ষমতা",
                        "৬. মহাকর্ষ ও অভিকর্ষ",
                        "৭. পদার্থের গাঠনিক ধর্ম",
                        "৮. পর্যাবৃত্ত গতি",
                        "৯. তরঙ্গ",
                        "১০. আদর্শ গ্যাস ও গ্যাসের গতিতত্ত্ব"
                    )
                ),
                SyllabusData(
                    name = "পদার্থবিজ্ঞান ২য় পত্র",
                    colorHex = "#A855F7",
                    iconName = "Psychology",
                    targetHours = 30f,
                    description = "উচ্চমাধ্যমিক পদার্থবিজ্ঞান দ্বিতীয় পত্র সিলেবাস",
                    chapters = listOf(
                        "১. তাপগতিবিদ্যা",
                        "২. স্থির তড়িৎ",
                        "৩. চল তড়িৎ",
                        "৪. তড়িৎ প্রবাহের চৌম্বক ক্রিয়া ও চৌম্বকত্ব",
                        "৫. তড়িৎ চৌম্বকীয় আবেশ ও পরিবর্তী প্রবাহ",
                        "৬. জ্যামিতিক আলোকবিজ্ঞান",
                        "৭. ভৌত আলোকবিজ্ঞান",
                        "৮. আধুনিক পদার্থবিজ্ঞানের সূচনা",
                        "৯. পরমাণুর মডেল ও নিউক্লিয়ার পদার্থবিজ্ঞান",
                        "১০. সেমিকন্ডাক্টর ও ইলেকট্রনিক্স",
                        "১১. জ্যোতির্বিজ্ঞান"
                    )
                ),
                SyllabusData(
                    name = "রসায়ন ১ম পত্র",
                    colorHex = "#10B981",
                    iconName = "Science",
                    targetHours = 30f,
                    description = "উচ্চমাধ্যমিক রসায়ন প্রথম পত্র সিলেবাস",
                    chapters = listOf(
                        "১. ল্যাবরেটরির নিরাপদ ব্যবহার",
                        "২. গুণগত রসায়ন",
                        "৩. মৌলের পর্যায়বৃত্ত ধর্ম ও রাসায়নিক বন্ধন",
                        "৪. রাসায়নিক পরিবর্তন",
                        "৫. কর্মমুখী রসায়ন"
                    )
                ),
                SyllabusData(
                    name = "রসায়ন ২য় পত্র",
                    colorHex = "#059669",
                    iconName = "Science",
                    targetHours = 30f,
                    description = "উচ্চমাধ্যমিক রসায়ন দ্বিতীয় পত্র সিলেবাস",
                    chapters = listOf(
                        "১. পরিবেশ রসায়ন",
                        "২. জৈব রসায়ন",
                        "৩. পরিমাণগত রসায়ন",
                        "৪. তড়িৎ রসায়ন",
                        "৫. অর্থনৈতিক রসায়ন"
                    )
                ),
                SyllabusData(
                    name = "জীববিজ্ঞান ১ম পত্র (উদ্ভিদ)",
                    colorHex = "#22C55E",
                    iconName = "LocalFlorist",
                    targetHours = 30f,
                    description = "উচ্চমাধ্যমিক জীববিজ্ঞান প্রথম পত্র (উদ্ভিদবিজ্ঞান) সিলেবাস",
                    chapters = listOf(
                        "১. কোষ ও এর গঠন",
                        "২. কোষ বিভাজন",
                        "৩. কোষ রসায়ন",
                        "৪. অণুজীব",
                        "৫. শৈবাল ও ছত্রাক",
                        "৬. ব্রায়োফাইটা ও টেরিডোফাইটা",
                        "৭. নগ্নবীজী ও আবৃতবীজী উদ্ভিদ",
                        "৮. টিস্যু ও টিস্যুতন্ত্র",
                        "৯. উদ্ভিদ শারীরতত্ত্ব",
                        "১০. উদ্ভিদ প্রজনন",
                        "১১. জীবপ্রযুক্তি",
                        "১২. জীবের পরিবেশ, বিস্তার ও সংরক্ষণ"
                    )
                ),
                SyllabusData(
                    name = "জীববিজ্ঞান ২য় পত্র (প্রাণী)",
                    colorHex = "#16A34A",
                    iconName = "Pets",
                    targetHours = 30f,
                    description = "উচ্চমাধ্যমিক জীববিজ্ঞান দ্বিতীয় পত্র (প্রাণীবিজ্ঞান) সিলেবাস",
                    chapters = listOf(
                        "১. প্রাণীর বিভিন্নতা ও শ্রেণিবিন্যাস",
                        "২. প্রাণীর পরিচিতি: হাইড্রা, ঘাসফড়িং, রুই মাছ",
                        "৩. মানব শারীরতত্ত্ব: পরিপাক ও শোষণ",
                        "৪. মানব শারীরতত্ত্ব: রক্ত ও সংবহন",
                        "৫. মানব শারীরতত্ত্ব: শ্বসন ও শ্বাসক্রিয়া",
                        "৬. মানব শারীরতত্ত্ব: বর্জ্য ও নিষ্কাশন",
                        "৭. মানব শারীরতত্ত্ব: চলন ও অঙ্গচালনা",
                        "৮. মানব শারীরতত্ত্ব: সমন্বয় ও নিয়ন্ত্রণ",
                        "৯. প্রাণীর জীবনের ধারাবাহিকতা",
                        "১০. মানবদেহের প্রতিরক্ষা",
                        "১১. জিনতত্ত্ব ও বিবর্তন",
                        "১২. আচরণ"
                    )
                ),
                SyllabusData(
                    name = "তথ্য ও যোগাযোগ প্রযুক্তি",
                    colorHex = "#EC4899",
                    iconName = "Computer",
                    targetHours = 30f,
                    description = "উচ্চমাধ্যমিক তথ্য ও যোগাযোগ প্রযুক্তি (ICT) সিলেবাস",
                    chapters = listOf(
                        "১. তথ্য ও যোগাযোগ প্রযুক্তি: বিশ্ব ও বাংলাদেশ প্রেক্ষিত",
                        "২. কমিউনিকেশন সিস্টেমস ও নেটওয়ার্কিং",
                        "৩. সংখ্যা পদ্ধতি ও ডিজিটাল ডিভাইস",
                        "৪. ওয়েব ডিজাইন পরিচিতি এবং এইচটিএমএল",
                        "৫. প্রোগ্রামিং ভাষা",
                        "৬. ডাটাবেজ ম্যানেজমেন্ট সিস্টেম"
                    )
                )
            )

            syllabiToSeed.forEach { syllabus ->
                val subjectId = subjectDao.insertSubject(
                    SubjectEntity(
                        name = syllabus.name,
                        colorHex = syllabus.colorHex,
                        iconName = syllabus.iconName,
                        targetHours = syllabus.targetHours,
                        description = syllabus.description
                    )
                )
                syllabus.chapters.forEachIndexed { index, title ->
                    chapterDao.insertChapter(
                        ChapterEntity(
                            subjectId = subjectId,
                            subjectName = syllabus.name,
                            chapterName = title,
                            tapCount = 0,
                            status = "পড়া শুরু হয় নাই",
                            orderIndex = index
                        )
                    )
                }
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
