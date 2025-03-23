package com.squalor.consecutor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.rememberDismissState
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material3.ExperimentalMaterial3Api
import com.squalor.consecutor.ui.AddEventDialog
import java.time.format.DateTimeFormatter

/**
 * Main screen displaying a list of events with swipe-to-delete and FAB for adding new events.
 */
@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
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
                    confirmStateChange = {
                        if (it == DismissValue.DismissedToStart) {
                            viewModel.delete(event)
                            true
                        } else {
                            false
                        }
                    }
                )
                SwipeToDismiss(
                    state = dismissState,
                    directions = setOf(DismissDirection.EndToStart),
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
 * Displays a single event with its details and an increment action.
 */
@Composable
fun EventItem(event: Event, onIncrement: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onIncrement),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji (if present)
            event.emoji?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(end = 8.dp),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Event name
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                // Last incremented date (if present)
                event.lastIncremented?.let { date ->
                    Text(
                        text = "Last: ${date.format(DateTimeFormatter.ISO_LOCAL_DATE)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                // Counts
                Text(
                    text = "Consecutive: ${event.consecutiveCount} | Total: ${event.totalCount}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}