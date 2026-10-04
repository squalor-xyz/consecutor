package com.squalor.consecutor.ui

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.squalor.consecutor.NumberRules
import com.squalor.consecutor.R
import com.squalor.consecutor.ReminderConfig
import com.squalor.consecutor.TargetPeriod
import com.squalor.consecutor.TrackerType
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields

@Composable
internal fun targetLabel(type: TrackerType, period: TargetPeriod, value: Double, unit: String?): String {
    if (type == TrackerType.YES_NO) {
        return when (period) {
            TargetPeriod.DAILY -> stringResource(R.string.label_target_per_day, NumberRules.formatNumber(1.0))
            TargetPeriod.WEEKLY -> {
                val days = value.toInt()
                pluralStringResource(R.plurals.label_target_days_per_week, days, NumberRules.formatNumber(value))
            }
        }
    }
    val amount = listOfNotNull(NumberRules.formatNumber(value), unit?.takeIf { it.isNotBlank() }).joinToString(" ")
    return when (period) {
        TargetPeriod.DAILY -> stringResource(R.string.label_target_per_day, amount)
        TargetPeriod.WEEKLY -> stringResource(R.string.label_target_per_week, amount)
    }
}

@Composable
internal fun reminderLabel(config: ReminderConfig): String {
    val context = LocalContext.current
    val locale = LocalLocale.current.platformLocale
    val pattern = DateFormat.getBestDateTimePattern(locale, if (DateFormat.is24HourFormat(context)) "Hm" else "hm")
    val time = LocalTime.of(config.hourOfDay, config.minuteOfHour).format(DateTimeFormatter.ofPattern(pattern, locale))
    val days = config.daysOfWeek
    if (days.isEmpty() || days.size == 7) {
        return stringResource(R.string.label_reminder_every_day, time)
    }
    val firstDay = WeekFields.of(locale).firstDayOfWeek
    val dayNames = days
        .sortedBy { Math.floorMod(it.value - firstDay.value, 7) }
        .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, locale) }
    return stringResource(R.string.label_reminder_days_at, dayNames, time)
}
