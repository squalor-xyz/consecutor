package com.squalor.consecutor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Checkbox
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
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
    notificationsEnabled: Boolean,
    archivedCount: Int,
    onOpenArchived: () -> Unit,
    padding: PaddingValues,
    onOpenTracker: (Long) -> Unit,
    onLogToday: (TrackerSummary) -> Unit,
    onToggleToday: (TrackerSummary) -> Unit,
    onEditToday: (TrackerSummary) -> Unit,
    onCreate: () -> Unit
) {
    if (dashboard.isEmpty() && archivedCount == 0) {
        EmptyState(
            text = "No trackers yet.",
            padding = padding,
            actionLabel = "Create tracker",
            onAction = onCreate
        )
        return
    }
    ContentColumn(padding) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (dashboard.isEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("No trackers yet.")
                        TextButton(onClick = onCreate) { Text("Create tracker") }
                    }
                }
            }
            items(dashboard, key = { it.id }) { tracker ->
                TrackerSummaryCard(
                    tracker = tracker,
                    notificationsEnabled = notificationsEnabled,
                    onOpen = { onOpenTracker(tracker.id) },
                    onLogToday = { onLogToday(tracker) },
                    onToggleToday = { onToggleToday(tracker) },
                    onEditToday = { onEditToday(tracker) }
                )
            }
            if (archivedCount > 0) {
                item {
                    TextButton(onClick = onOpenArchived, modifier = Modifier.fillMaxWidth()) {
                        Text("Archived ($archivedCount)")
                    }
                }
            }
        }
    }
}

@Composable
internal fun TrackerSummaryCard(
    tracker: TrackerSummary,
    notificationsEnabled: Boolean,
    onOpen: () -> Unit,
    onLogToday: () -> Unit,
    onToggleToday: () -> Unit,
    onEditToday: () -> Unit
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
                when (tracker.type) {
                    TrackerType.YES_NO -> Checkbox(
                        checked = tracker.doneToday,
                        onCheckedChange = { onToggleToday() },
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .semantics {
                                contentDescription = "${if (tracker.doneToday) "Clear" else "Log"} ${tracker.name} today"
                            }
                    )
                    TrackerType.COUNT -> Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .combinedClickable(
                                role = Role.Button,
                                onClick = onLogToday,
                                onLongClickLabel = "Log custom amount",
                                onLongClick = onEditToday
                            )
                    ) {
                        Text("+1", color = MaterialTheme.colorScheme.primary)
                    }
                    TrackerType.MEASURE -> TextButton(
                        onClick = onEditToday,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Text("Log")
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
            tracker.reminder?.let {
                ReminderLabel(reminderLabel(it), notificationsEnabled)
            }
        }
    }
}

@Composable
internal fun ReminderLabel(label: String, notificationsEnabled: Boolean) {
    Text(
        text = if (notificationsEnabled) label else "$label · Notifications off",
        style = MaterialTheme.typography.bodySmall,
        color = if (notificationsEnabled) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.error
    )
}
