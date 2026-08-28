package com.niloy.focentra

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.niloy.focentra.data.local.AppDatabase
import com.niloy.focentra.data.local.entity.SettingEntity
import com.niloy.focentra.data.local.entity.StudySessionEntity
import com.niloy.focentra.data.provider.DaynexaContract
import com.niloy.focentra.data.repository.StudyRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DaynexaBroadcastTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: StudyRepository
    private lateinit var shadowApplication: ShadowApplication

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getDatabase(context)
        repository = StudyRepository(database, context)
        shadowApplication = shadowOf(context.applicationContext as android.app.Application)
    }

    @Test
    fun testBroadcast_whenConsentEnabled_isSentOnSessionInsert() = runBlocking {
        // 1. Enable consent
        database.settingDao().setSetting(
            SettingEntity(key = DaynexaContract.SETTING_DAYNEXA_CONSENT, value = "true")
        )

        val startTime = System.currentTimeMillis() - 3600000
        val endTime = System.currentTimeMillis()

        // 2. Insert session via repository
        val session = StudySessionEntity(
            startTime = startTime,
            endTime = endTime,
            durationSeconds = 3600,
            actualFocusedSeconds = 3600,
            pausedSeconds = 0,
            sessionType = "COUNTDOWN",
            subject = "Mathematics",
            topic = "Calculus",
            productivityRating = 5,
            completionStatus = "COMPLETED"
        )
        
        repository.insertSession(session)

        // Give it a moment for the asynchronous broadcast coroutine to finish
        // Since it runs on Dispatchers.IO, we need a small real-time delay or controlled waiting
        delay(500)
        
        // 3. Verify broadcast was sent
        val broadcastIntents = shadowApplication.broadcastIntents
        val matchingIntent = broadcastIntents.find { it.action == DaynexaContract.ACTION_SESSION_COMPLETED }
        
        assertNotNull("Broadcast intent should be sent", matchingIntent)
        assertEquals("Mathematics", matchingIntent?.getStringExtra(DaynexaContract.COLUMN_SUBJECT))
        assertEquals("COMPLETED", matchingIntent?.getStringExtra(DaynexaContract.COLUMN_COMPLETION_STATUS))
        assertEquals(60L, matchingIntent?.getLongExtra(DaynexaContract.COLUMN_DURATION, 0L))
    }

    @Test
    fun testBroadcast_whenConsentDisabled_isNotSent() = runBlocking {
        // 1. Disable consent
        database.settingDao().setSetting(
            SettingEntity(key = DaynexaContract.SETTING_DAYNEXA_CONSENT, value = "false")
        )

        // 2. Insert session
        repository.insertSession(
            StudySessionEntity(
                startTime = System.currentTimeMillis(),
                endTime = System.currentTimeMillis(),
                durationSeconds = 3600,
                actualFocusedSeconds = 3600,
                pausedSeconds = 0,
                sessionType = "COUNTDOWN",
                subject = "History",
                completionStatus = "COMPLETED"
            )
        )

        delay(500)

        // 3. Verify no broadcast sent
        val broadcastIntents = shadowApplication.broadcastIntents
        val matchingIntent = broadcastIntents.find { it.action == DaynexaContract.ACTION_SESSION_COMPLETED }
        assertEquals(null, matchingIntent)
    }
}
