package com.squalor.consecutor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.squalor.consecutor.R
import com.squalor.consecutor.TrackerDetail
import com.squalor.consecutor.EntryItem
import com.squalor.consecutor.TrackerType
import com.squalor.consecutor.ui.charts.BarChart
import com.squalor.consecutor.ui.charts.CalendarHeatmap
import com.squalor.consecutor.ui.charts.ChartMath
import com.squalor.consecutor.ui.charts.chartSummaryText
import com.squalor.consecutor.ui.charts.LineChart
import java.time.LocalDate

@Composable
internal fun TrackerDetailScreen(
    detail: TrackerDetail,
    padding: PaddingValues,
    today: LocalDate,
    onAddEntry: () -> Unit,
    onAddEntryForDate: (LocalDate) -> Unit,
    onEditEntry: (EntryItem) -> Unit
) {
    ContentColumn(padding) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, top = 16.dp, end = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            listOfNotNull(detail.tracker.emoji, detail.tracker.name).joinToString(" ").trim(),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        detail.tracker.description?.let { Text(it) }
                        StatusLine(detail.summary)
                        StreakLine(detail.summary)
                        val summary = detail.summary
                        val targetPeriod = summary.targetPeriod
                        val targetValue = summary.targetValue
                        if (targetPeriod != null && targetValue != null) {
                            Text(stringResource(R.string.detail_target, targetLabel(summary.type, targetPeriod, targetValue, summary.unit)))
                        }
                        summary.reminder?.let { Text(stringResource(R.string.detail_reminder, reminderLabel(it))) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onAddEntry, modifier = Modifier.heightIn(min = 48.dp)) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.detail_log_entry))
                            }
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.detail_recent_trend), style = MaterialTheme.typography.titleMedium)
                        val chartSummary = chartSummaryText(ChartMath.summarize(detail.trend), detail.target != null, detail.tracker.unit)
                        val chartModifier = Modifier.semantics {
                            contentDescription = chartSummary
                        }
                        if (detail.tracker.type == TrackerType.MEASURE) {
                            LineChart(detail.trend, detail.tracker.unit, chartModifier)
                        } else {
                            BarChart(detail.trend, detail.target?.targetValue, chartModifier)
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    CalendarHeatmap(
                        type = detail.tracker.type,
                        target = detail.target,
                        entries = detail.entries,
                        today = today,
                        onDayClick = { date ->
                            val entry = detail.entries
                                .filter { it.effectiveDate == date }
                                .maxByOrNull { it.occurredAtEpochMs }
                            if (entry != null) onEditEntry(entry) else onAddEntryForDate(date)
                        },
                        // 360dp - 24dp outer padding = seven non-overlapping 48dp day targets.
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }
            item {
                Text(stringResource(R.string.detail_history), style = MaterialTheme.typography.titleMedium)
            }
            if (detail.entries.isEmpty()) {
                item { Text(stringResource(R.string.detail_no_entries)) }
            } else {
                items(detail.entries, key = { it.id }) { entry ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditEntry(entry) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(formatDate(entry.effectiveDate), fontWeight = FontWeight.SemiBold)
                            Text(formatEntryValue(detail.tracker.type, entry.value, detail.tracker.unit))
                            entry.note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
        }
    }
}
