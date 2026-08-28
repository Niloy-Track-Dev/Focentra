package com.niloy.focentra.data.provider

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.util.Log
import com.niloy.focentra.data.local.AppDatabase
import com.niloy.focentra.data.local.entity.StudySessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.math.roundToInt

/**
 * Secure, Read-Only Android ContentProvider implementing the Daynexa Integration API v1.
 * Exposes completed study session records to authorized local applications (Daynexa)
 * strictly with prior user consent enabled in Focentra Settings.
 */
class DaynexaSessionsProvider : ContentProvider() {

    companion object {
        private const val TAG = "DaynexaProvider"

        private const val CODE_SESSIONS = 1
        private const val CODE_SESSION_ID = 2

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(DaynexaContract.AUTHORITY, DaynexaContract.PATH_SESSIONS, CODE_SESSIONS)
            addURI(DaynexaContract.AUTHORITY, "${DaynexaContract.PATH_SESSIONS}/#", CODE_SESSION_ID)
        }
    }

    override fun onCreate(): Boolean {
        Log.d(TAG, "DaynexaSessionsProvider initialized successfully.")
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val context = context ?: return null
        val matchCode = uriMatcher.match(uri)
        if (matchCode != CODE_SESSIONS && matchCode != CODE_SESSION_ID) {
            Log.w(TAG, "Rejected query: Invalid or unsupported URI: $uri")
            throw IllegalArgumentException("Unsupported URI: $uri")
        }

        // 1. Verify User Consent Gate
        val isConsentGranted = runBlocking(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(context)
                val setting = db.settingDao().getSettingValue(DaynexaContract.SETTING_DAYNEXA_CONSENT)
                setting.equals("true", ignoreCase = true)
            } catch (e: Exception) {
                Log.e(TAG, "Error checking user consent in database", e)
                false
            }
        }

        val requestedColumns = if (projection != null && projection.isNotEmpty()) {
            projection
        } else {
            DaynexaContract.DEFAULT_PROJECTION
        }

        val cursor = MatrixCursor(requestedColumns)

        if (!isConsentGranted) {
            Log.i(TAG, "Query blocked: User consent for Daynexa integration is not enabled in Settings.")
            // Return empty cursor to protect user privacy when consent is disabled
            return cursor
        }

        // 2. Fetch Sessions from Focentra Room SQLite Database
        val sessionEntities: List<StudySessionEntity> = runBlocking(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(context)
                if (matchCode == CODE_SESSION_ID) {
                    val sessionId = ContentUris.parseId(uri)
                    val single = db.sessionDao().getSessionById(sessionId)
                    if (single != null) listOf(single) else emptyList()
                } else {
                    db.sessionDao().getAllSessions()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to query sessions from Room database", e)
                emptyList()
            }
        }

        // 3. Map actual entities into public Daynexa API v1 Schema format
        for (session in sessionEntities) {
            val rowBuilder = cursor.newRow()

            val durationMin = if (session.actualFocusedSeconds > 0) {
                (session.actualFocusedSeconds / 60).coerceAtLeast(1)
            } else if (session.durationSeconds > 0) {
                (session.durationSeconds / 60).coerceAtLeast(1)
            } else {
                0L
            }

            val isCompleted = if (session.completionStatus.equals("COMPLETED", ignoreCase = true)) 1 else 0
            val sessionFocusScore = calculateSessionFocusScore(session)

            for (columnName in requestedColumns) {
                when (columnName) {
                    DaynexaContract.COLUMN_SESSION_ID -> rowBuilder.add(session.id)
                    DaynexaContract.COLUMN_SUBJECT -> rowBuilder.add(session.subject)
                    DaynexaContract.COLUMN_TOPIC -> rowBuilder.add(session.topic)
                    DaynexaContract.COLUMN_CATEGORY -> rowBuilder.add(session.category)
                    DaynexaContract.COLUMN_START_TIME -> rowBuilder.add(session.startTime)
                    DaynexaContract.COLUMN_END_TIME -> rowBuilder.add(session.endTime)
                    DaynexaContract.COLUMN_DURATION_MINUTES -> rowBuilder.add(durationMin)
                    DaynexaContract.COLUMN_COMPLETED -> rowBuilder.add(isCompleted)
                    DaynexaContract.COLUMN_FOCUS_SCORE -> rowBuilder.add(sessionFocusScore)
                    DaynexaContract.COLUMN_SCHEMA_VERSION -> rowBuilder.add(DaynexaContract.SCHEMA_VERSION)
                    else -> rowBuilder.add(null)
                }
            }
        }

        // Notify URI watcher if context allows
        cursor.setNotificationUri(context.contentResolver, uri)
        return cursor
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            CODE_SESSIONS -> DaynexaContract.CONTENT_TYPE_DIR
            CODE_SESSION_ID -> DaynexaContract.CONTENT_TYPE_ITEM
            else -> null
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        throw UnsupportedOperationException("Focentra Daynexa Integration ContentProvider is strictly READ-ONLY.")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        throw UnsupportedOperationException("Focentra Daynexa Integration ContentProvider is strictly READ-ONLY.")
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        throw UnsupportedOperationException("Focentra Daynexa Integration ContentProvider is strictly READ-ONLY.")
    }

    /**
     * Calculates an individualized 0-100 Focus Score for an individual session
     * based on productivity rating, distraction count, pause ratio, and completion status.
     */
    private fun calculateSessionFocusScore(session: StudySessionEntity): Int {
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
