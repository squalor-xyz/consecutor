package com.squalor.consecutor

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleTracker(bundle: TrackerBundle) {
        cancelTracker(bundle.tracker.id)
        val reminder = bundle.reminder.firstOrNull() ?: return
        if (!reminder.enabled || bundle.tracker.isArchived) return

        val triggerAt = nextTrigger(reminder)
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_TRACKER_ID, bundle.tracker.id)
        }
        val pendingIntent = pendingIntent(bundle.tracker.id, intent)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent
        )
    }

    fun cancelTracker(trackerId: Long) {
        val intent = Intent(context, ReminderReceiver::class.java)
        alarmManager.cancel(pendingIntent(trackerId, intent))
    }

    fun cancelAll(ids: Collection<Long>) = ids.forEach(::cancelTracker)

    fun ensureNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun showNotification(
        context: Context,
        trackerId: Long,
        trackerName: String,
        trackerEmoji: String?,
        trackerType: TrackerType
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val title = listOfNotNull(trackerEmoji, trackerName).joinToString(" ").trim()
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_TRACKER_ID, trackerId)
        val contentIntent = PendingIntent.getActivity(
            context,
            trackerId.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (title.isBlank()) context.getString(R.string.notification_title_fallback) else title)
            .setContentText(context.getString(R.string.notification_text))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        reminderAction(trackerType)?.let { action ->
            val actionIntent = PendingIntent.getBroadcast(
                context,
                trackerId.toInt(),
                Intent(context, ReminderActionReceiver::class.java).putExtra(EXTRA_TRACKER_ID, trackerId),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(0, context.getString(action.labelRes), actionIntent)
        }
        NotificationManagerCompat.from(context).notify(trackerId.toInt(), builder.build())
    }

    private fun nextTrigger(reminder: ReminderEntity): Long =
        nextReminderTrigger(reminder, ZonedDateTime.now())

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
    }
}

/**
 * Next reminder time strictly after [now], in [now]'s zone. Empty weekday set means every day.
 * Pure date math so it can be unit tested without Android.
 */
internal fun nextReminderTrigger(reminder: ReminderEntity, now: ZonedDateTime): Long {
    val allowedDays = reminder.daysOfWeekCsv
        ?.split(",")
        ?.mapNotNull { it.toIntOrNull()?.let(DayOfWeek::of) }
        ?.toSet()
        ?: emptySet()

    var date = now.toLocalDate()
    while (true) {
        val candidate = ZonedDateTime.of(date, LocalTime.of(reminder.hourOfDay, reminder.minuteOfHour), now.zone)
        if (candidate.isAfter(now) && (allowedDays.isEmpty() || date.dayOfWeek in allowedDays)) {
            return candidate.toInstant().toEpochMilli()
        }
        date = date.plusDays(1)
    }
}

internal enum class ReminderAction(@StringRes val labelRes: Int) {
    MARK_DONE(R.string.notification_action_mark_done),
    ADD_ONE(R.string.notification_action_add_one)
}

/** The one-tap notification action for [type]; measurements need a typed value, so they have none. */
internal fun reminderAction(type: TrackerType): ReminderAction? = when (type) {
    TrackerType.YES_NO -> ReminderAction.MARK_DONE
    TrackerType.COUNT -> ReminderAction.ADD_ONE
    TrackerType.MEASURE -> null
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val app = context.applicationContext as? ConsecutorApp
        if (app == null) {
            pendingResult.finish()
            return
        }
        val trackerId = intent.getLongExtra(ReminderScheduler.EXTRA_TRACKER_ID, 0L)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val scheduler = ReminderScheduler(context)
                val bundle = app.repository.getTrackerBundle(trackerId)
                if (bundle == null || bundle.tracker.isArchived || !hasEnabledReminder(bundle)) {
                    scheduler.cancelTracker(trackerId)
                    return@launch
                }
                if (shouldNotify(bundle, LocalDate.now())) {
                    scheduler.ensureNotificationChannel()
                    scheduler.showNotification(
                        context = context,
                        trackerId = trackerId,
                        trackerName = bundle.tracker.name,
                        trackerEmoji = bundle.tracker.emoji,
                        trackerType = bundle.tracker.type
                    )
                }
                scheduler.scheduleTracker(bundle)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/** Logs today from a reminder's action button, then dismisses the reminder. */
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val app = context.applicationContext as? ConsecutorApp
        if (app == null) {
            pendingResult.finish()
            return
        }
        val trackerId = intent.getLongExtra(ReminderScheduler.EXTRA_TRACKER_ID, 0L)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tracker = app.repository.getTrackerBundle(trackerId)?.tracker
                if (tracker != null && !tracker.isArchived && reminderAction(tracker.type) != null) {
                    app.repository.addEntry(trackerId, tracker.type, EntryDraft(LocalDate.now(), 1.0, null))
                }
            } finally {
                NotificationManagerCompat.from(context).cancel(trackerId.toInt())
                pendingResult.finish()
            }
        }
    }
}

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in RESCHEDULE_ACTIONS) return
        val pendingResult = goAsync()
        val app = context.applicationContext as? ConsecutorApp
        if (app == null) {
            pendingResult.finish()
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val scheduler = ReminderScheduler(context)
                scheduler.ensureNotificationChannel()
                app.repository.getReminderBundles().forEach(scheduler::scheduleTracker)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        val RESCHEDULE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED
        )
    }
}
