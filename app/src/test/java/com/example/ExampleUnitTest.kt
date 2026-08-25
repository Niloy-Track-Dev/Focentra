package com.niloy.focentra

import com.niloy.focentra.data.local.entity.StudySessionEntity
import com.niloy.focentra.data.repository.calculateStreak
import com.niloy.focentra.engine.StatisticsEngine
import com.niloy.focentra.engine.TimePeriod
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testEmptySessions_returnsDefaultStatistics() {
        val stats = StatisticsEngine.calculatePeriodStats(emptyList(), TimePeriod.TODAY)
        assertEquals(0L, stats.totalFocusedSeconds)
        assertEquals(0, stats.sessionCount)
        assertEquals(0, stats.focusScore)
    }

    @Test
    fun testSingleSession_calculatesFocusScoreAndDuration() {
        val now = System.currentTimeMillis()
        val session = StudySessionEntity(
            id = 1L,
            startTime = now,
            endTime = now + 1500_000L,
            durationSeconds = 1500L,
            actualFocusedSeconds = 1500L,
            pausedSeconds = 0L,
            sessionType = "COUNTDOWN",
            subject = "Physics",
            topic = "Thermodynamics",
            productivityRating = 5,
            distractionCount = 0,
            completionStatus = "COMPLETED"
        )

        val stats = StatisticsEngine.calculatePeriodStats(listOf(session), TimePeriod.ALL_TIME)
        assertEquals(1500L, stats.totalFocusedSeconds)
        assertEquals(1, stats.sessionCount)
        assertEquals("Physics", stats.mostStudiedSubject)
        assertTrue(stats.focusScore >= 80)
    }

    @Test
    fun testStreakCalculation_singleDay() {
        val now = System.currentTimeMillis()
        val session = StudySessionEntity(
            id = 1L,
            startTime = now,
            endTime = now + 1800_000L,
            durationSeconds = 1800L,
            actualFocusedSeconds = 1800L,
            pausedSeconds = 0L,
            sessionType = "STOPWATCH",
            subject = "Mathematics"
        )
        val streak = calculateStreak(listOf(session))
        assertTrue(streak.currentStreak >= 1)
        assertTrue(streak.longestStreak >= 1)
    }

    @Test
    fun testPersonalRecords_findsLongestSession() {
        val now = System.currentTimeMillis()
        val s1 = StudySessionEntity(
            id = 1L,
            startTime = now,
            endTime = now + 1200_000L,
            durationSeconds = 1200L,
            actualFocusedSeconds = 1200L,
            pausedSeconds = 0L,
            sessionType = "COUNTDOWN",
            subject = "Biology"
        )
        val s2 = StudySessionEntity(
            id = 2L,
            startTime = now + 3600_000L,
            endTime = now + 7200_000L,
            durationSeconds = 3600L,
            actualFocusedSeconds = 3600L,
            pausedSeconds = 0L,
            sessionType = "POMODORO",
            subject = "Higher Math"
        )
        val records = StatisticsEngine.calculatePersonalRecords(listOf(s1, s2))
        assertEquals(60L, records.longestSessionMinutes)
        assertEquals("Higher Math", records.longestSessionSubject)
        assertEquals(2, records.totalAllTimeSessions)
        assertEquals(80L, records.totalAllTimeMinutes)
    }
}
