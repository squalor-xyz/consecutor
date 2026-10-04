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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.squalor.consecutor.TargetPeriod
import com.squalor.consecutor.TrackerSummary
import com.squalor.consecutor.TrackerType

@Composable
internal fun DashboardScreen(
    dashboard: List<TrackerSummary>,
    padding: PaddingValues,
    onOpenTracker: (Long) -> Unit,
    onQuickLog: (TrackerSummary) -> Unit,
    onCreate: () -> Unit
) {
    if (dashboard.isEmpty()) {
        EmptyState(
            text = "No trackers yet.",
            padding = padding,
            actionLabel = "Create tracker",
            onAction = onCreate
        )
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
        colors = if (tracker.doneToday) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        } else {
            CardDefaults.cardColors()
        },
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
            StatusLine(tracker)
            StreakLine(tracker)
            tracker.completionRate?.let { rate ->
                val window = if (tracker.targetPeriod == TargetPeriod.WEEKLY) "8 weeks" else "14 days"
                Text("Last $window: ${(rate * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(progress = { rate }, modifier = Modifier.fillMaxWidth())
            }
            tracker.reminderLabel?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
