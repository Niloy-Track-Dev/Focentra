package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.engine.SessionMode
import com.example.engine.TimerEngine
import com.example.engine.TimerStatus
import com.example.engine.TimerUiState

class StudyTimerService : Service() {

    companion object {
        const val CHANNEL_ID = "focentra_study_timer_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PAUSE = "com.niloy.focentra.ACTION_PAUSE"
        const val ACTION_RESUME = "com.niloy.focentra.ACTION_RESUME"
        const val ACTION_FINISH = "com.niloy.focentra.ACTION_FINISH"
        const val ACTION_STOP = "com.niloy.focentra.ACTION_STOP"

        private const val EXTRA_SUBJECT = "extra_subject"
        private const val EXTRA_TIME_TEXT = "extra_time_text"
        private const val EXTRA_IS_PAUSED = "extra_is_paused"
        private const val EXTRA_MODE = "extra_mode"

        fun startOrUpdate(context: Context, state: TimerUiState) {
            val intent = Intent(context, StudyTimerService::class.java).apply {
                putExtra(EXTRA_SUBJECT, state.subject)
                putExtra(EXTRA_IS_PAUSED, state.status == TimerStatus.PAUSED)
                putExtra(EXTRA_MODE, state.mode.name)

                val timeFormatted = when (state.mode) {
                    SessionMode.COUNTDOWN, SessionMode.POMODORO -> {
                        val min = state.remainingSeconds / 60
                        val sec = state.remainingSeconds % 60
                        String.format("%02d:%02d remaining", min, sec)
                    }
                    SessionMode.STOPWATCH -> {
                        val hrs = state.focusedSeconds / 3600
                        val min = (state.focusedSeconds % 3600) / 60
                        val sec = state.focusedSeconds % 60
                        if (hrs > 0) String.format("%02d:%02d:%02d focused", hrs, min, sec)
                        else String.format("%02d:%02d focused", min, sec)
                    }
                }
                putExtra(EXTRA_TIME_TEXT, timeFormatted)
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, StudyTimerService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action != null) {
            handleNotificationAction(action)
            return START_STICKY
        }

        val subject = intent?.getStringExtra(EXTRA_SUBJECT) ?: "Focus Study"
        val timeText = intent?.getStringExtra(EXTRA_TIME_TEXT) ?: "Session in progress"
        val isPaused = intent?.getBooleanExtra(EXTRA_IS_PAUSED, false) ?: false

        val notification = buildNotification(subject, timeText, isPaused)
        try {
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return START_STICKY
    }

    private fun handleNotificationAction(action: String) {
        val app = applicationContext as? com.example.FocentraApp
        val engine = app?.timerEngine
        when (action) {
            ACTION_PAUSE -> engine?.pauseSession()
            ACTION_RESUME -> engine?.resumeSession()
            ACTION_FINISH -> engine?.finishSession(isEarly = false)
            ACTION_STOP -> engine?.cancelSession()
        }
    }

    private fun buildNotification(subject: String, timeText: String, isPaused: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeAction = if (isPaused) {
            val resumeIntent = Intent(this, StudyTimerService::class.java).apply { action = ACTION_RESUME }
            val resumePending = PendingIntent.getService(this, 1, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action.Builder(android.R.drawable.ic_media_play, "Resume", resumePending).build()
        } else {
            val pauseIntent = Intent(this, StudyTimerService::class.java).apply { action = ACTION_PAUSE }
            val pausePending = PendingIntent.getService(this, 2, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action.Builder(android.R.drawable.ic_media_pause, "Pause", pausePending).build()
        }

        val finishIntent = Intent(this, StudyTimerService::class.java).apply { action = ACTION_FINISH }
        val finishPending = PendingIntent.getService(this, 3, finishIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val finishAction = NotificationCompat.Action.Builder(android.R.drawable.checkbox_on_background, "Finish", finishPending).build()

        val statusLabel = if (isPaused) "Paused" else "Studying"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focentra — $statusLabel: $subject")
            .setContentText(timeText)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(pauseResumeAction)
            .addAction(finishAction)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Study Focus Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live study session progress and controls"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
