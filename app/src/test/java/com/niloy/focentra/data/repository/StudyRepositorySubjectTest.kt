package com.niloy.focentra.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.niloy.focentra.data.local.AppDatabase
import com.niloy.focentra.data.local.entity.ChapterEntity
import com.niloy.focentra.data.local.entity.PresetEntity
import com.niloy.focentra.data.local.entity.SubjectEntity
import com.niloy.focentra.data.local.entity.StudySessionEntity
import com.niloy.focentra.data.local.entity.StudyTaskEntity
import com.niloy.focentra.data.local.entity.TopicEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StudyRepositorySubjectTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: StudyRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = StudyRepository(database, context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun renamingSubjectKeepsTopicsAndChaptersAttached() = runBlocking {
        val subject = SubjectEntity(name = "Physics")
        val subjectId = repository.insertSubject(subject)
        database.topicDao().insertTopic(TopicEntity(subjectId = subjectId, subjectName = "Physics", name = "Motion"))
        repository.insertChapter(ChapterEntity(subjectId = subjectId, subjectName = "Physics", chapterName = "Vectors"))
        repository.insertSession(
            StudySessionEntity(
                startTime = 1L,
                endTime = 2L,
                durationSeconds = 1L,
                actualFocusedSeconds = 1L,
                pausedSeconds = 0L,
                sessionType = "COUNTDOWN",
                subject = "Physics"
            )
        )
        repository.insertTask(StudyTaskEntity(title = "Review vectors", subject = "Physics"))
        repository.insertPreset(PresetEntity(title = "Physics focus", type = "COUNTDOWN", subject = "Physics"))

        repository.updateSubject(subject.copy(id = subjectId, name = "Mechanics"))

        assertEquals("Mechanics", database.topicDao().getAllTopics().single().subjectName)
        assertEquals("Mechanics", database.chapterDao().getAllChaptersFlow().first().single().subjectName)
        assertEquals("Mechanics", database.sessionDao().getAllSessions().single().subject)
        assertEquals("Mechanics", database.taskDao().getAllTasksFlow().first().single().subject)
        assertEquals("Mechanics", database.presetDao().getAllPresets().single().subject)
    }

    @Test
    fun deletingSubjectRemovesItsTopicsAndChapters() = runBlocking {
        val subject = SubjectEntity(name = "Chemistry")
        val subjectId = repository.insertSubject(subject)
        database.topicDao().insertTopic(TopicEntity(subjectId = subjectId, subjectName = "Chemistry", name = "Atoms"))
        repository.insertChapter(ChapterEntity(subjectId = subjectId, subjectName = "Chemistry", chapterName = "Matter"))

        repository.deleteSubject(subject.copy(id = subjectId))

        assertTrue(database.topicDao().getAllTopics().isEmpty())
        assertTrue(database.chapterDao().getAllChaptersFlow().first().isEmpty())
        assertTrue(database.subjectDao().getAllSubjects().isEmpty())
    }
}
