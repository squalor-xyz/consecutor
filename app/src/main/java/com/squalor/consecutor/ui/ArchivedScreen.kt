package com.squalor.consecutor.ui

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.squalor.consecutor.TrackerEntity

@Composable
internal fun ArchivedScreen(
    archived: List<TrackerEntity>,
    padding: PaddingValues,
    onRestore: (Long) -> Unit,
    onDelete: (TrackerEntity) -> Unit
) {
    if (archived.isEmpty()) {
        EmptyState("No archived trackers.", padding = padding)
        return
    }
    ContentColumn(padding) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(archived, key = { it.id }) { tracker ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(listOfNotNull(tracker.emoji, tracker.name).joinToString(" "), style = MaterialTheme.typography.titleMedium)
                        Row {
                            TextButton(onClick = { onRestore(tracker.id) }) { Text("Restore") }
                            TextButton(onClick = { onDelete(tracker) }) {
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
