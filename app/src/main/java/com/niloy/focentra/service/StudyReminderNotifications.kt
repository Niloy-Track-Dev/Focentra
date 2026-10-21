package com.niloy.focentra.service

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.niloy.focentra.FocentraApp
import com.niloy.focentra.MainActivity
import com.niloy.focentra.R
import com.niloy.focentra.data.local.entity.ReminderEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object StudyReminderNotifications {
    const val ALERT_CHANNEL_ID = "focentra_alerts_channel"
    private const val TAG = "StudyReminders"
    private const val ACTION_FIRE_REMINDER = "com.niloy.focentra.action.FIRE_REMINDER"
    private const val EXTRA_REMINDER_ID = "reminder_id"

    fun post(context: Context, notificationId: Int, title: String, message: String): Boolean {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        } catch (exception: SecurityException) {
            Log.w(TAG, "Notification permission was revoked before the reminder could be posted.", exception)
            false
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                ALERT_CHANNEL_ID,
                "Focentra Study Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for revision due, study goals, and scheduled reminders"
            }
        )
    }

    internal fun reminderIntent(context: Context, reminderId: Long): Intent =
        Intent(context, StudyReminderReceiver::class.java).apply {
            action = ACTION_FIRE_REMINDER
            data = Uri.parse("focentra://reminder/$reminderId")
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }

    internal fun reminderId(intent: Intent): Long =
        intent.getLongExtra(EXTRA_REMINDER_ID, -1L)

    internal fun isReminderAction(intent: Intent?): Boolean =
        intent?.action == ACTION_FIRE_REMINDER

    internal fun systemNotificationId(reminderId: Long): Int =
        0x40000000 or (reminderId.toInt() and 0x3FFFFFFF)
}

object StudyReminderScheduler {
    fun scheduleAll(context: Context, reminders: List<ReminderEntity>) {
        reminders.forEach { schedule(context, it) }
    }

    fun schedule(context: Context, reminder: ReminderEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = pendingIntent(context, reminder.id)
        val triggerAtMillis = nextReminderTimeMillis(reminder, System.currentTimeMillis())
        if (!reminder.enabled || triggerAtMillis == null) {
            alarmManager.cancel(pendingIntent)
            return
        }

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    fun cancel(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context, reminderId))
    }

    private fun pendingIntent(context: Context, reminderId: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            StudyReminderNotifications.reminderIntent(context, reminderId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}

class StudyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!StudyReminderNotifications.isReminderAction(intent)) return
        val reminderId = StudyReminderNotifications.reminderId(intent!!)
        if (reminderId <= 0L) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as FocentraApp
                val reminder = app.database.reminderDao().getReminderById(reminderId)
                if (reminder == null || !reminder.enabled) {
                    StudyReminderScheduler.cancel(context, reminderId)
                    return@launch
                }

                StudyReminderNotifications.post(
                    context = context,
                    notificationId = StudyReminderNotifications.systemNotificationId(reminder.id),
                    title = reminder.title,
                    message = reminder.message
                )
                StudyReminderScheduler.schedule(context, reminder)
            } catch (exception: Exception) {
                Log.e("StudyReminders", "Failed to deliver or reschedule reminder $reminderId.", exception)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class ReminderScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action !in setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_MY_PACKAGE_REPLACED
            )
        ) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as FocentraApp
                val reminders = app.repository.allReminders.first()
                StudyReminderScheduler.scheduleAll(context, reminders)
            } catch (exception: Exception) {
                Log.e("StudyReminders", "Failed to restore scheduled reminders.", exception)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
