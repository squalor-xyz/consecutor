package com.squalor.consecutor.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.squalor.consecutor.TrackerDetail
import com.squalor.consecutor.EntryItem
import com.squalor.consecutor.TrackerType
import com.squalor.consecutor.ui.charts.BarChart
import com.squalor.consecutor.ui.charts.ChartMath
import com.squalor.consecutor.ui.charts.LineChart

@Composable
internal fun TrackerDetailScreen(
    detail: TrackerDetail,
    padding: PaddingValues,
    onAddEntry: () -> Unit,
    onEditEntry: (EntryItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
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
                    detail.summary.targetLabel?.let { Text("Target: $it") }
                    detail.summary.reminderLabel?.let { Text("Reminder: $it") }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onAddEntry) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Log entry")
                        }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Recent trend", style = MaterialTheme.typography.titleMedium)
                    val summary = ChartMath.summarize(detail.trend)
                    val chartModifier = Modifier.semantics {
                        contentDescription = ChartMath.summaryText(summary, detail.target != null, detail.tracker.unit)
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
            Text("History", style = MaterialTheme.typography.titleMedium)
        }
        if (detail.entries.isEmpty()) {
            item { Text("No entries yet. Start logging to build history and streaks.") }
        } else {
            items(detail.entries, key = { it.id }) { entry ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditEntry(entry) }
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(entry.effectiveDate.toString(), fontWeight = FontWeight.SemiBold)
                        Text(formatEntryValue(detail.tracker.type, entry.value, detail.tracker.unit))
                        entry.note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }
}
