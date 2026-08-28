package com.niloy.focentra.data.provider

import android.net.Uri

/**
 * Daynexa Integration API v1 Contract.
 * 
 * Provides official constants, column names, URIs, and schema version
 * for local App-to-App ContentProvider communication between Focentra and Daynexa.
 */
object DaynexaContract {

    /**
     * The ContentProvider Authority for Focentra study session data.
     */
    const val AUTHORITY = "com.focentra.provider"

    /**
     * Base Content URI.
     */
    val BASE_CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY")

    /**
     * Path for study sessions endpoint.
     */
    const val PATH_SESSIONS = "sessions"

    /**
     * Sessions Content URI: content://com.focentra.provider/sessions
     */
    val SESSIONS_URI: Uri = BASE_CONTENT_URI.buildUpon().appendPath(PATH_SESSIONS).build()

    /**
     * Schema Version of the Daynexa Integration API.
     */
    const val SCHEMA_VERSION = 1

    /**
     * MIME types for directory and single item queries.
     */
    const val CONTENT_TYPE_DIR = "vnd.android.cursor.dir/vnd.com.focentra.session"
    const val CONTENT_TYPE_ITEM = "vnd.android.cursor.item/vnd.com.focentra.session"

    /**
     * Setting key stored in Focentra local settings to track user consent.
     */
    const val SETTING_DAYNEXA_CONSENT = "daynexa_integration_enabled"
    const val SETTING_DAYNEXA_CONNECTED_AT = "daynexa_connected_timestamp"

    // ==========================================
    // Exposed Column Names (Schema Version 1)
    // ==========================================

    /** Unique persistent session identifier (Long) */
    const val COLUMN_SESSION_ID = "sessionId"

    /** Study subject name, e.g. "Mathematics", "Programming" (String) */
    const val COLUMN_SUBJECT = "subject"

    /** Specific sub-topic studied, e.g. "Calculus", "Algorithms" (String) */
    const val COLUMN_TOPIC = "topic"

    /** Study category, e.g. "Study", "Deep Work" (String) */
    const val COLUMN_CATEGORY = "category"

    /** Session start timestamp in epoch milliseconds (Long) */
    const val COLUMN_START_TIME = "startTime"

    /** Session end timestamp in epoch milliseconds (Long) */
    const val COLUMN_END_TIME = "endTime"

    /** Total focused duration in minutes (Int / Long) */
    const val COLUMN_DURATION_MINUTES = "durationMinutes"

    /** Completion status flag: 1 if COMPLETED, 0 if CANCELLED or EARLY_FINISH (Int) */
    const val COLUMN_COMPLETED = "completed"

    /** Calculated Focus Score (0 - 100) based on productivity and distractions (Int) */
    const val COLUMN_FOCUS_SCORE = "focusScore"

    /** Current schema version: always 1 (Int) */
    const val COLUMN_SCHEMA_VERSION = "schemaVersion"

    /**
     * Complete list of all supported columns for projection.
     */
    val DEFAULT_PROJECTION = arrayOf(
        COLUMN_SESSION_ID,
        COLUMN_SUBJECT,
        COLUMN_TOPIC,
        COLUMN_CATEGORY,
        COLUMN_START_TIME,
        COLUMN_END_TIME,
        COLUMN_DURATION_MINUTES,
        COLUMN_COMPLETED,
        COLUMN_FOCUS_SCORE,
        COLUMN_SCHEMA_VERSION
    )
}
