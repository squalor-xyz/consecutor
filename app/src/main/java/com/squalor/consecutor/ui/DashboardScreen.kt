package com.squalor.consecutor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.squalor.consecutor.TrackerSummary
import com.squalor.consecutor.TrackerType

@Composable
internal fun DashboardScreen(
    dashboard: List<TrackerSummary>,
    padding: PaddingValues,
    onOpenTracker: (Long) -> Unit,
    onQuickLog: (TrackerSummary) -> Unit
) {
    if (dashboard.isEmpty()) {
        EmptyState(
            text = "Create your first tracker to start logging habits, events, or measurements."
        )
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Local-first tracker dashboard",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Track real history, derive consecutive streaks, and keep exports portable.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        items(dashboard, key = { it.id }) { tracker ->
            TrackerSummaryCard(
                tracker = tracker,
                onOpen = { onOpenTracker(tracker.id) },
                onQuickLog = { onQuickLog(tracker) }
            )
        }
    }
}

@Composable
internal fun TrackerSummaryCard(
    tracker: TrackerSummary,
    onOpen: () -> Unit,
    onQuickLog: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = listOfNotNull(tracker.emoji, tracker.name).joinToString(" ").trim(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (tracker.type != TrackerType.MEASURE) {
                    TextButton(onClick = onQuickLog) {
                        Text("Quick log")
                    }
                }
            }
            tracker.description?.takeIf { it.isNotBlank() }?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricChip("Current", tracker.currentStreak.toString())
                MetricChip("Longest", tracker.longestStreak.toString())
                MetricChip("Total", formatValue(tracker.totalValue, tracker.unit))
            }
            tracker.targetLabel?.let { AssistChip(onClick = {}, label = { Text(it) }) }
            tracker.reminderLabel?.let { AssistChip(onClick = {}, leadingIcon = { Icon(Icons.Default.Notifications, null) }, label = { Text(it) }) }
            tracker.lastEntryDate?.let {
                Text("Last entry: $it", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                "Recent completion ${(tracker.completionRate * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall
            )
            LinearProgressIndicator(progress = { tracker.completionRate }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun MetricChip(label: String, value: String) {
    AssistChip(
        onClick = {},
        label = { Text("$label: $value") }
    )
}
