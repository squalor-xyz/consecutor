package com.squalor.consecutor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.squalor.consecutor.NumberRules
import com.squalor.consecutor.TargetPeriod
import com.squalor.consecutor.TrackerSummary
import com.squalor.consecutor.TrackerType
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun EmptyState(
    text: String,
    padding: PaddingValues = PaddingValues(),
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = text)
        if (actionLabel != null && onAction != null) {
            Button(onClick = onAction) { Text(actionLabel) }
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

internal fun statusText(summary: TrackerSummary): String {
    val weekly = summary.targetPeriod == TargetPeriod.WEEKLY
    val target = summary.periodTarget
    return when (summary.type) {
        TrackerType.YES_NO -> {
            val today = if (summary.doneToday) "Done today" else "Not logged today"
            if (weekly && target != null) {
                "$today · ${NumberRules.formatNumber(summary.periodValue)} / ${NumberRules.formatNumber(target)} this week"
            } else {
                today
            }
        }
        TrackerType.COUNT -> when {
            target != null && weekly ->
                "${progress(summary.periodValue, target, summary.unit)} this week"
            target != null ->
                "${progress(summary.todayValue, target, summary.unit)} today"
            else -> "${formatValue(summary.todayValue, summary.unit)} today"
        }
        TrackerType.MEASURE -> summary.lastEntryDate
            ?.let { "Last: ${it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}" }
            ?: "Not logged yet"
    }
}

internal fun streakText(summary: TrackerSummary): String? {
    val period = summary.targetPeriod ?: return null
    val unit = if (period == TargetPeriod.WEEKLY) "week" else "day"
    val best = "best ${summary.longestStreak}"
    return if (summary.currentStreak > 0) {
        "${summary.currentStreak}-$unit streak · $best"
    } else {
        "No streak yet · $best"
    }
}

private fun progress(value: Double, target: Double, unit: String?): String =
    listOfNotNull("${NumberRules.formatNumber(value)} / ${NumberRules.formatNumber(target)}", unit).joinToString(" ")

internal fun formatValue(value: Double, unit: String?): String {
    return listOf(NumberRules.formatNumber(value), unit).filterNotNull().joinToString(" ")
}

internal fun formatEntryValue(type: TrackerType, value: Double?, unit: String?): String {
    return when (type) {
        TrackerType.YES_NO -> formatValue(1.0, unit)
        TrackerType.COUNT -> formatValue(value ?: 1.0, unit)
        TrackerType.MEASURE -> value?.let { formatValue(it, unit) } ?: "No value"
    }
}
