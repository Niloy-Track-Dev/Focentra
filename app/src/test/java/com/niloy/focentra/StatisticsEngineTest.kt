package com.niloy.focentra

import com.niloy.focentra.data.local.entity.StudySessionEntity
import com.niloy.focentra.engine.StatisticsEngine
import com.niloy.focentra.engine.TimePeriod
import com.niloy.focentra.data.repository.calculateStreak
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class StatisticsEngineTest {
    @Test
    fun lastMonthExcludesTheFirstDayOfThisMonth() {
        val currentMonthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val lastMonthSession = session(currentMonthStart.timeInMillis - 1_000L, 60L)
        val currentMonthSession = session(currentMonthStart.timeInMillis + 60 * 60 * 1000L, 120L)

        val stats = StatisticsEngine.calculatePeriodStats(
            listOf(lastMonthSession, currentMonthSession),
            TimePeriod.LAST_MONTH
        )

        assertEquals(60L, stats.totalFocusedSeconds)
        assertEquals(1, stats.sessionCount)
    }

    @Test
    fun weeklyPersonalRecordSumsSessionsAcrossTheWeek() {
        val calendar = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.MARCH, 9, 12, 0, 0) // Monday
        }
        val firstDay = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 2) // Wednesday
        val thirdDay = calendar.timeInMillis

        val records = StatisticsEngine.calculatePersonalRecords(
            listOf(session(firstDay, 5 * 60 * 60L), session(thirdDay, 4 * 60 * 60L))
        )

        assertEquals(9 * 60L, records.maxWeekMinutes)
    }

    @Test
    fun dailyTotalsAccumulateSubMinuteSessionsBeforeConvertingToMinutes() {
        val now = System.currentTimeMillis()
        val sessions = List(120) { session(now, 30L) }

        val records = StatisticsEngine.calculatePersonalRecords(sessions)
        val todayKey = Calendar.getInstance().apply { timeInMillis = now }.let {
            String.format("%04d-%02d-%02d", it.get(Calendar.YEAR), it.get(Calendar.MONTH) + 1, it.get(Calendar.DAY_OF_MONTH))
        }
        val todayHeatmap = StatisticsEngine.buildHeatmapData(sessions)
            .single { it.dateKey == todayKey }

        assertEquals(60L, records.maxDayMinutes)
        assertEquals(60L, todayHeatmap.studyMinutes)
        assertEquals(1, todayHeatmap.level)
    }

    @Test
    fun longestStreakCountsConsecutiveDaysAcrossDaylightSavingTime() {
        val originalTimeZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
            val sessions = listOf(
                session(dateMillis(2026, Calendar.MARCH, 7), 60L),
                session(dateMillis(2026, Calendar.MARCH, 8), 60L),
                session(dateMillis(2026, Calendar.MARCH, 9), 60L)
            )

            assertEquals(3, calculateStreak(sessions).longestStreak)
        } finally {
            TimeZone.setDefault(originalTimeZone)
        }
    }

    private fun session(startTime: Long, focusedSeconds: Long) = StudySessionEntity(
        startTime = startTime,
        endTime = startTime + focusedSeconds * 1_000L,
        durationSeconds = focusedSeconds,
        actualFocusedSeconds = focusedSeconds,
        pausedSeconds = 0L,
        sessionType = "COUNTDOWN",
        subject = "Study"
    )

    private fun dateMillis(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, 12, 0, 0)
        }.timeInMillis
}
