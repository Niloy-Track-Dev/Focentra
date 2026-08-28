package com.niloy.focentra.data.provider

import com.niloy.focentra.data.local.entity.StudySessionEntity
import kotlin.math.roundToInt

/**
 * Helper for Daynexa Integration API.
 * Provides consistent focus score calculation and data mapping.
 */
object DaynexaIntegrationHelper {

    /**
     * Calculates an individualized 0-100 Focus Score for an individual session
     * based on productivity rating, distraction count, pause ratio, and completion status.
     * 
     * This logic is shared between the ContentProvider (Pull) and Broadcast (Push) systems
     * to ensure data consistency.
     */
    fun calculateFocusScore(session: StudySessionEntity): Int {
        // Base rating score: up to 50 points (rating 1 to 5)
        val ratingPoints = (session.productivityRating.coerceIn(1, 5) / 5.0f) * 50f

        // Distraction penalty: -5 points per distraction (up to -25 points)
        val distractionPenalty = (session.distractionCount * 5f).coerceAtMost(25f)

        // Completion reward: +25 points for completed, +10 for early finish, 0 for cancelled
        val completionPoints = when (session.completionStatus.uppercase()) {
            "COMPLETED" -> 25f
            "EARLY_FINISH" -> 15f
            else -> 0f
        }

        // Pause factor: up to 25 points based on focus vs pause ratio
        val totalSec = session.actualFocusedSeconds + session.pausedSeconds
        val pausePoints = if (totalSec > 0) {
            (1.0f - (session.pausedSeconds.toFloat() / totalSec)) * 25f
        } else {
            25f
        }

        val total = ratingPoints + (25f - distractionPenalty) + completionPoints + pausePoints
        return (total / 125f * 100f).roundToInt().coerceIn(0, 100)
    }
}
