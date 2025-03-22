package com.squalor.consecutor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.swipetodismiss.SwipeToDismiss
import com.google.accompanist.swipetodismiss.rememberDismissState
import kotlinx.coroutines.flow.Flow

/**
 * Main screen displaying a list of events with swipe-to-delete and FAB for adding new events.
 */
@Composable
fun EventListScreen(viewModel: EventViewModel) {
    val events by viewModel.allEvents.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddEventDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, emoji ->
                viewModel.insert(Event(name = name, emoji = emoji))
                showAddDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Consecutor") },
                actions = {
                    val context = LocalContext.current
                    IconButton(onClick = { viewModel.exportData(context) }) {
                        Icon(Icons.Default.Share, contentDescription = "Export Data")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Event")
            }
        }
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            items(events.size) { index ->
                val event = events[index]
                val dismissState = rememberDismissState(
                    confirmValueChange = {
                        if (it == androidx.compose.material.DismissValue.DismissedToStart) {
                            viewModel.delete(event)
                            true
                        } else {
                            false
                        }
                    }
                )
                SwipeToDismiss(
                    state = dismissState,
                    directions = setOf(androidx.compose.material.DismissDirection.EndToStart),
                    background = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Red)
                                .padding(16.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                        }
                    },
                    dismissContent = {
                        EventItem(event, onIncrement = { viewModel.incrementEvent(event) })
                    }
                )
            }
        }
    }
}

/**
 * Displays a single event with its name, emoji, and counters.
 */
@Composable
fun EventItem(event: Event, onIncrement: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onIncrement() }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            event.emoji?.let { Text(it, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 16.dp)) }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(event.name, style = MaterialTheme.typography.headlineSmall)
                Text("Consecutive: ${event.consecutiveCount}")
                Text("Total: ${event.totalCount}")
            }
        }
    }
}

/**
 * Dialog for adding a new event with name and optional emoji.
 */
@Composable
fun AddEventDialog(onDismiss: () -> Unit, onAdd: (String, String?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Event") },
        text = {
            Column {
                TextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                TextField(value = emoji, onValueChange = { emoji = it }, label = { Text("Emoji (optional)") })
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onAdd(name, emoji.takeIf { it.isNotBlank() }) }) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}