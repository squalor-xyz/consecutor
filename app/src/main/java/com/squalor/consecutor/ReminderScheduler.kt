package com.squalor.consecutor

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleTracker(bundle: TrackerBundle) {
        cancelTracker(bundle.tracker.id)
        val reminder = bundle.reminder.firstOrNull() ?: return
        if (!reminder.enabled || bundle.tracker.isArchived) return

        val triggerAt = nextTrigger(reminder)
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_TRACKER_ID, bundle.tracker.id)
            putExtra(EXTRA_TRACKER_NAME, bundle.tracker.name)
            putExtra(EXTRA_TRACKER_EMOJI, bundle.tracker.emoji)
        }
        val pendingIntent = pendingIntent(bundle.tracker.id, intent)
        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        }
    }

    fun cancelTracker(trackerId: Long) {
        val intent = Intent(context, ReminderReceiver::class.java)
        alarmManager.cancel(pendingIntent(trackerId, intent))
    }

    fun ensureNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Tracker reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminders for scheduled trackers."
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun showNotification(context: Context, trackerId: Long, trackerName: String, trackerEmoji: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val title = listOfNotNull(trackerEmoji, trackerName).joinToString(" ").trim()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(if (title.isBlank()) "Tracker reminder" else title)
            .setContentText("Log today's progress in Consecutor.")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(trackerId.toInt(), notification)
    }

    private fun nextTrigger(reminder: ReminderEntity): Long {
        val zoneId = ZoneId.systemDefault()
        var next = LocalDateTime.now(zoneId)
            .withHour(reminder.hourOfDay)
            .withMinute(reminder.minuteOfHour)
            .withSecond(0)
            .withNano(0)

        if (!next.isAfter(LocalDateTime.now(zoneId))) {
            next = next.plusDays(1)
        }

        val allowedDays = reminder.daysOfWeekCsv
            ?.split(",")
            ?.mapNotNull { it.toIntOrNull()?.let(DayOfWeek::of) }
            ?.toSet()
            ?: emptySet()

        while (allowedDays.isNotEmpty() && next.dayOfWeek !in allowedDays) {
            next = next.plusDays(1)
        }
        return next.atZone(zoneId).toInstant().toEpochMilli()
    }

    private fun pendingIntent(trackerId: Long, intent: Intent): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            trackerId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val CHANNEL_ID = "tracker_reminders"
        const val EXTRA_TRACKER_ID = "tracker_id"
        const val EXTRA_TRACKER_NAME = "tracker_name"
        const val EXTRA_TRACKER_EMOJI = "tracker_emoji"
    }

    private fun canScheduleExactAlarms(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val scheduler = ReminderScheduler(context)
        scheduler.ensureNotificationChannel()
        val trackerId = intent.getLongExtra(ReminderScheduler.EXTRA_TRACKER_ID, 0L)
        scheduler.showNotification(
            context = context,
            trackerId = trackerId,
            trackerName = intent.getStringExtra(ReminderScheduler.EXTRA_TRACKER_NAME).orEmpty(),
            trackerEmoji = intent.getStringExtra(ReminderScheduler.EXTRA_TRACKER_EMOJI)
        )
        val app = context.applicationContext as? ConsecutorApp
        if (app == null) {
            pendingResult.finish()
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            app.repository.getReminderBundles()
                .firstOrNull { it.tracker.id == trackerId }
                ?.let(scheduler::scheduleTracker)
            pendingResult.finish()
        }
    }
}

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }
        val pendingResult = goAsync()
        val app = context.applicationContext as? ConsecutorApp
        if (app == null) {
            pendingResult.finish()
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            val scheduler = ReminderScheduler(context)
            scheduler.ensureNotificationChannel()
            app.repository.getReminderBundles().forEach(scheduler::scheduleTracker)
            pendingResult.finish()
        }
    }
}
