package com.niloy.focentra.service

import com.niloy.focentra.data.local.entity.ReminderEntity
import java.util.Calendar
import java.util.TimeZone

internal fun nextReminderTimeMillis(
    reminder: ReminderEntity,
    nowMillis: Long,
    timeZone: TimeZone = TimeZone.getDefault()
): Long? {
    if (!reminder.enabled || reminder.hour !in 0..23 || reminder.minute !in 0..59) return null

    val selectedDays = parseReminderDays(reminder.daysOfWeek)
    if (selectedDays.isEmpty()) return null

    for (daysAhead in 0..7) {
        val candidate = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, daysAhead)
            set(Calendar.HOUR_OF_DAY, reminder.hour)
            set(Calendar.MINUTE, reminder.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (candidate.get(Calendar.DAY_OF_WEEK) in selectedDays && candidate.timeInMillis > nowMillis) {
            return candidate.timeInMillis
        }
    }
    return null
}

private fun parseReminderDays(daysOfWeek: String): Set<Int> {
    val normalized = daysOfWeek.trim().lowercase()
    if (normalized.isEmpty() || normalized in setOf("every day", "everyday", "daily")) {
        return (Calendar.SUNDAY..Calendar.SATURDAY).toSet()
    }
    if (normalized in setOf("weekdays", "weekday", "mon-fri")) {
        return (Calendar.MONDAY..Calendar.FRIDAY).toSet()
    }
    if (normalized in setOf("weekends", "weekend", "sat-sun")) {
        return setOf(Calendar.SATURDAY, Calendar.SUNDAY)
    }
    val tokens = normalized.split(',', ';', ' ').map { it.trim() }.filter { it.isNotEmpty() }

    val dayByToken = mapOf(
        "sun" to Calendar.SUNDAY, "sunday" to Calendar.SUNDAY,
        "mon" to Calendar.MONDAY, "monday" to Calendar.MONDAY,
        "tue" to Calendar.TUESDAY, "tues" to Calendar.TUESDAY, "tuesday" to Calendar.TUESDAY,
        "wed" to Calendar.WEDNESDAY, "weds" to Calendar.WEDNESDAY, "wednesday" to Calendar.WEDNESDAY,
        "thu" to Calendar.THURSDAY, "thur" to Calendar.THURSDAY, "thurs" to Calendar.THURSDAY, "thursday" to Calendar.THURSDAY,
        "fri" to Calendar.FRIDAY, "friday" to Calendar.FRIDAY,
        "sat" to Calendar.SATURDAY, "saturday" to Calendar.SATURDAY
    )
    return tokens.mapNotNull(dayByToken::get).toSet()
}
