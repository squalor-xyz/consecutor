package com.squalor.consecutor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.squalor.consecutor.TrackerDetail
import com.squalor.consecutor.EntryItem

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
        contentPadding = PaddingValues(16.dp),
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
                    detail.trend.forEach { point ->
                        val progress = ((point.value ?: 0.0) / maxOf(detail.target?.targetValue ?: 1.0, 1.0)).toFloat().coerceIn(0f, 1f)
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(point.date.toString(), modifier = Modifier.width(110.dp), style = MaterialTheme.typography.bodySmall)
                                Text(formatValue(point.value ?: 0.0, detail.tracker.unit), style = MaterialTheme.typography.bodySmall)
                            }
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                            )
                        }
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
