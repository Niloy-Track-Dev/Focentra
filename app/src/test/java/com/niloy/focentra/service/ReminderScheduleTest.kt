package com.niloy.focentra.service

import com.niloy.focentra.data.local.entity.ReminderEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ReminderScheduleTest {
    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun weekdayReminderMovesToNextSelectedDayWhenTodaysTimeHasPassed() {
        val now = timeMillis(2026, Calendar.OCTOBER, 19, 9, 1)
        val reminder = ReminderEntity(title = "Weekday study", hour = 9, minute = 0, daysOfWeek = "Mon,Tue,Wed,Thu,Fri")

        assertEquals(
            timeMillis(2026, Calendar.OCTOBER, 20, 9, 0),
            nextReminderTimeMillis(reminder, now, utc)
        )
    }

    @Test
    fun reminderSchedulesTodayWhenItsSelectedTimeIsStillAhead() {
        val now = timeMillis(2026, Calendar.OCTOBER, 19, 8, 59)
        val reminder = ReminderEntity(title = "Morning study", hour = 9, minute = 0, daysOfWeek = "MON")

        assertEquals(
            timeMillis(2026, Calendar.OCTOBER, 19, 9, 0),
            nextReminderTimeMillis(reminder, now, utc)
        )
    }

    @Test
    fun weekendAliasSchedulesSaturday() {
        val now = timeMillis(2026, Calendar.OCTOBER, 23, 18, 0)
        val reminder = ReminderEntity(title = "Weekend study", hour = 9, minute = 0, daysOfWeek = "Weekends")

        assertEquals(
            timeMillis(2026, Calendar.OCTOBER, 24, 9, 0),
            nextReminderTimeMillis(reminder, now, utc)
        )
    }

    @Test
    fun disabledOrInvalidReminderHasNoNextAlarm() {
        val now = timeMillis(2026, Calendar.OCTOBER, 19, 8, 0)

        assertNull(nextReminderTimeMillis(ReminderEntity(title = "Disabled", hour = 9, minute = 0, enabled = false), now, utc))
        assertNull(nextReminderTimeMillis(ReminderEntity(title = "Invalid", hour = 24, minute = 0), now, utc))
        assertNull(nextReminderTimeMillis(ReminderEntity(title = "Unknown", hour = 9, minute = 0, daysOfWeek = "holiday"), now, utc))
    }

    private fun timeMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(utc).apply {
            clear()
            set(year, month, day, hour, minute)
        }.timeInMillis
}
