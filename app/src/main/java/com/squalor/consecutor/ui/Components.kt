package com.squalor.consecutor.ui

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.squalor.consecutor.NumberRules
import com.squalor.consecutor.R
import com.squalor.consecutor.TargetPeriod
import com.squalor.consecutor.TrackerSummary
import com.squalor.consecutor.TrackerType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun EmptyState(
    text: String,
    padding: PaddingValues = PaddingValues(),
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    ContentColumn(padding) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = text)
            if (actionLabel != null && onAction != null) {
                Button(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp)) { Text(actionLabel) }
            }
        }
    }
}

@Composable
internal fun ContentColumn(padding: PaddingValues, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
            content()
        }
    }
}

@Composable
internal fun StatusLine(summary: TrackerSummary, modifier: Modifier = Modifier) {
    Text(statusText(summary), style = MaterialTheme.typography.bodyMedium, modifier = modifier)
}

@Composable
internal fun StreakLine(summary: TrackerSummary, modifier: Modifier = Modifier) {
    streakText(summary)?.let {
        Text(it, style = MaterialTheme.typography.bodyMedium, modifier = modifier)
    }
}

@Composable
internal fun statusText(summary: TrackerSummary, spoken: Boolean = false): String {
    val weekly = summary.targetPeriod == TargetPeriod.WEEKLY
    val target = summary.periodTarget
    return when (summary.type) {
        TrackerType.YES_NO -> {
            val today = stringResource(if (summary.doneToday) R.string.status_done_today else R.string.status_not_logged_today)
            if (weekly && target != null) {
                stringResource(
                    if (spoken) R.string.status_yes_no_weekly_spoken else R.string.status_yes_no_weekly,
                    today,
                    NumberRules.formatNumber(summary.periodValue),
                    NumberRules.formatNumber(target)
                )
            } else {
                today
            }
        }
        TrackerType.COUNT -> when {
            target != null && weekly ->
                stringResource(R.string.status_this_week, progress(summary.periodValue, target, summary.unit, spoken))
            target != null ->
                stringResource(R.string.status_today, progress(summary.todayValue, target, summary.unit, spoken))
            else -> stringResource(R.string.status_today, formatValue(summary.todayValue, summary.unit))
        }
        TrackerType.MEASURE -> summary.lastEntryDate
            ?.let { stringResource(R.string.status_last_logged, formatDate(it)) }
            ?: stringResource(R.string.status_not_logged_yet)
    }
}

@Composable
internal fun streakText(summary: TrackerSummary): String? {
    val period = summary.targetPeriod ?: return null
    if (summary.currentStreak <= 0) return stringResource(R.string.streak_none, summary.longestStreak)
    val plural = if (period == TargetPeriod.WEEKLY) R.plurals.streak_weeks else R.plurals.streak_days
    return pluralStringResource(plural, summary.currentStreak, summary.currentStreak, summary.longestStreak)
}

internal fun formatDate(date: LocalDate): String = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

@Composable
private fun progress(value: Double, target: Double, unit: String?, spoken: Boolean): String {
    val amount = if (spoken) {
        stringResource(R.string.status_progress_spoken, NumberRules.formatNumber(value), NumberRules.formatNumber(target))
    } else {
        "${NumberRules.formatNumber(value)} / ${NumberRules.formatNumber(target)}"
    }
    return listOfNotNull(amount, unit).joinToString(" ")
}

internal fun formatValue(value: Double, unit: String?): String {
    return listOf(NumberRules.formatNumber(value), unit).filterNotNull().joinToString(" ")
}

@Composable
internal fun formatEntryValue(type: TrackerType, value: Double?, unit: String?): String {
    return when (type) {
        TrackerType.YES_NO -> formatValue(1.0, unit)
        TrackerType.COUNT -> formatValue(value ?: 1.0, unit)
        TrackerType.MEASURE -> value?.let { formatValue(it, unit) } ?: stringResource(R.string.entry_no_value)
    }
}
