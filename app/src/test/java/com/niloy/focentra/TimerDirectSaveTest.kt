package com.niloy.focentra

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.niloy.focentra.data.local.AppDatabase
import com.niloy.focentra.data.repository.StudyRepository
import com.niloy.focentra.engine.SessionMode
import com.niloy.focentra.engine.TimerEngine
import com.niloy.focentra.engine.TimerStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TimerDirectSaveTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: StudyRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getDatabase(context)
        repository = StudyRepository(database, context)
    }

    @Test
    fun finishAndSaveSessionDirectlyInsertsSessionAndResetsToIdle() = runBlocking {
        val engine = TimerEngine.getInstance(context, repository)

        engine.configureSession(
            mode = SessionMode.COUNTDOWN,
            subject = "Physics",
            selectedChapter = "Vectors",
            durationMinutes = 25
        )
        engine.startSession()

        assertEquals(TimerStatus.RUNNING, engine.uiState.first().status)

        engine.finishAndSaveSessionDirectly(isEarly = false)

        val state = engine.uiState.first()
        assertEquals(TimerStatus.IDLE, state.status)
        assertFalse(state.showCompletionSheet)
        assertNull(state.completedSessionData)

        val sessions = repository.allSessions.first()
        assertTrue("Database should contain saved session", sessions.isNotEmpty())
        val savedSession = sessions.last()
        assertEquals("Physics", savedSession.subject)
    }
}
