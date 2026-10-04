package com.squalor.consecutor

import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Whether a reminder alarm for [bundle] should post a notification on [today]. With a target, notify
 * until the current period's target is met; without one, notify until something is logged today.
 */
internal fun shouldNotify(
    bundle: TrackerBundle,
    today: LocalDate,
    weekFields: WeekFields = WeekFields.of(Locale.getDefault())
): Boolean {
    if (bundle.tracker.isArchived || !hasEnabledReminder(bundle)) return false
    val summary = TrackerAnalytics.toSummary(bundle, today, weekFields)
    return if (summary.periodTarget != null) !summary.periodMet else !summary.doneToday
}

internal fun hasEnabledReminder(bundle: TrackerBundle): Boolean =
    bundle.reminder.firstOrNull()?.enabled == true
