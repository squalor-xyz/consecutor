package com.squalor.consecutor

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate

private enum class Screen {
    DASHBOARD,
    DETAIL,
    SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(viewModel: TrackerViewModel) {
    val dashboard by viewModel.dashboard.collectAsState()
    val message by viewModel.message.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var screen by rememberSaveable { mutableStateOf(Screen.DASHBOARD) }
    var selectedTrackerId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showTrackerEditor by remember { mutableStateOf<TrackerDetail?>(null) }
    var showNewTrackerDialog by remember { mutableStateOf(false) }
    var entryEditorState by remember { mutableStateOf<EntryEditorState?>(null) }
    val selectedDetail by selectedTrackerId?.let { viewModel.trackerDetail(it).collectAsState(initial = null) }
        ?: remember { mutableStateOf<TrackerDetail?>(null) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.importBackup(context, uri)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.ensureReminderChannel()
        }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    if (showNewTrackerDialog) {
        TrackerEditorDialog(
            initial = null,
            onDismiss = { showNewTrackerDialog = false },
            onSave = { draft ->
                maybeRequestNotifications(permissionLauncher, draft)
                viewModel.saveTracker(existingTrackerId = null, draft = draft)
                showNewTrackerDialog = false
            }
        )
    }

    showTrackerEditor?.let { detail ->
        TrackerEditorDialog(
            initial = detail,
            onDismiss = { showTrackerEditor = null },
            onSave = { draft ->
                maybeRequestNotifications(permissionLauncher, draft)
                viewModel.saveTracker(detail.tracker.id, draft)
                showTrackerEditor = null
            }
        )
    }

    entryEditorState?.let { state ->
        EntryEditorDialog(
            state = state,
            onDismiss = { entryEditorState = null },
            onSave = { draft ->
                if (state.entryId == null) {
                    viewModel.addEntry(state.trackerId, state.trackerType, draft)
                } else {
                    viewModel.updateEntry(state.entryId, state.trackerId, state.trackerType, draft)
                }
                entryEditorState = null
            },
            onDelete = if (state.entryId == null) null else {
                {
                    viewModel.deleteEntry(state.entryId, state.trackerId)
                    entryEditorState = null
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (screen) {
                            Screen.DASHBOARD -> "Consecutor"
                            Screen.DETAIL -> "Tracker"
                            Screen.SETTINGS -> "Settings"
                        }
                    )
                },
                navigationIcon = {
                    if (screen != Screen.DASHBOARD) {
                        IconButton(onClick = {
                            screen = Screen.DASHBOARD
                            selectedTrackerId = null
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    when (screen) {
                        Screen.DASHBOARD -> {
                            IconButton(onClick = { screen = Screen.SETTINGS }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                        }
                        Screen.DETAIL -> {
                            selectedDetail?.let { detail ->
                                IconButton(onClick = { showTrackerEditor = detail }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit tracker")
                                }
                            }
                        }
                        Screen.SETTINGS -> Unit
                    }
                }
            )
        },
        floatingActionButton = {
            when (screen) {
                Screen.DASHBOARD -> {
                    FloatingActionButton(onClick = { showNewTrackerDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add tracker")
                    }
                }
                Screen.DETAIL -> {
                    selectedDetail?.let { detail ->
                        FloatingActionButton(onClick = {
                            entryEditorState = EntryEditorState.new(detail)
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Add entry")
                        }
                    }
                }
                Screen.SETTINGS -> Unit
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        when (screen) {
            Screen.DASHBOARD -> DashboardScreen(
                dashboard = dashboard,
                padding = padding,
                onOpenTracker = {
                    selectedTrackerId = it
                    screen = Screen.DETAIL
                },
                onQuickLog = { summary -> viewModel.quickLog(summary) }
            )
            Screen.DETAIL -> {
                if (selectedTrackerId == null) {
                    EmptyState("Select a tracker.")
                } else {
                    selectedDetail?.let {
                        TrackerDetailScreen(
                            detail = it,
                            padding = padding,
                            onAddEntry = { entryEditorState = EntryEditorState.new(it) },
                            onEditEntry = { entry -> entryEditorState = EntryEditorState.from(it, entry) },
                            onArchive = {
                                viewModel.archiveTracker(it.tracker.id)
                                screen = Screen.DASHBOARD
                                selectedTrackerId = null
                            }
                        )
                    } ?: EmptyState("Tracker not found.")
                }
            }
            Screen.SETTINGS -> SettingsScreen(
                padding = padding,
                onExportCsv = { viewModel.exportCsv(context) },
                onExportBackup = { viewModel.exportBackup(context) },
                onImportBackup = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                onEnableNotifications = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )
        }
    }
}

@Composable
private fun DashboardScreen(
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
private fun TrackerSummaryCard(
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
private fun MetricChip(label: String, value: String) {
    AssistChip(
        onClick = {},
        label = { Text("$label: $value") }
    )
}

@Composable
private fun TrackerDetailScreen(
    detail: TrackerDetail,
    padding: PaddingValues,
    onAddEntry: () -> Unit,
    onEditEntry: (EntryItem) -> Unit,
    onArchive: () -> Unit
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
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricChip("Current streak", detail.summary.currentStreak.toString())
                        MetricChip("Longest", detail.summary.longestStreak.toString())
                    }
                    detail.summary.targetLabel?.let { Text("Target: $it") }
                    detail.summary.reminderLabel?.let { Text("Reminder: $it") }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onAddEntry) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Log entry")
                        }
                        TextButton(onClick = onArchive) {
                            Text("Archive tracker")
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
                        val progress = (point.value / maxOf(detail.target?.targetValue ?: 1.0, 1.0)).toFloat().coerceIn(0f, 1f)
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(point.date.toString(), modifier = Modifier.width(110.dp), style = MaterialTheme.typography.bodySmall)
                                Text(formatValue(point.value, detail.tracker.unit), style = MaterialTheme.typography.bodySmall)
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

@Composable
private fun SettingsScreen(
    padding: PaddingValues,
    onExportCsv: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onEnableNotifications: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Privacy-first defaults", style = MaterialTheme.typography.titleLarge)
            Text(
                "Consecutor keeps tracker data on-device, uses explicit export/import for portability, and avoids account-based sync in v1.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        item {
            SettingsCard(
                title = "CSV export",
                body = "Share a spreadsheet-friendly snapshot of trackers and entries."
            ) { Button(onClick = onExportCsv) { Text("Export CSV") } }
        }
        item {
            SettingsCard(
                title = "Full backup",
                body = "Export or import the full app state as a versioned JSON backup file."
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onExportBackup) { Text("Export backup") }
                    TextButton(onClick = onImportBackup) { Text("Import backup") }
                }
            }
        }
        item {
            SettingsCard(
                title = "Notifications",
                body = "Reminders are local only. Android 13+ needs notification permission."
            ) {
                Button(onClick = onEnableNotifications) {
                    Text("Allow notifications")
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    body: String,
    action: @Composable () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium)
            action()
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, modifier = Modifier.padding(24.dp))
    }
}

private fun maybeRequestNotifications(
    launcher: androidx.activity.result.ActivityResultLauncher<String>,
    draft: TrackerDraft
) {
    if (draft.reminderEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

@Composable
private fun TrackerEditorDialog(
    initial: TrackerDetail?,
    onDismiss: () -> Unit,
    onSave: (TrackerDraft) -> Unit
) {
    var name by remember { mutableStateOf(initial?.tracker?.name.orEmpty()) }
    var emoji by remember { mutableStateOf(initial?.tracker?.emoji.orEmpty()) }
    var description by remember { mutableStateOf(initial?.tracker?.description.orEmpty()) }
    var unit by remember { mutableStateOf(initial?.tracker?.unit.orEmpty()) }
    var type by remember { mutableStateOf(initial?.tracker?.type ?: TrackerType.YES_NO) }
    var targetEnabled by remember { mutableStateOf(initial?.target != null) }
    var targetPeriod by remember { mutableStateOf(initial?.target?.period ?: TargetPeriod.DAILY) }
    var targetValue by remember { mutableStateOf(initial?.target?.targetValue?.toString() ?: "1") }
    val reminder = initial?.reminder
    var reminderEnabled by remember { mutableStateOf(reminder?.enabled ?: false) }
    var reminderHour by remember { mutableStateOf(reminder?.hourOfDay?.toString() ?: "20") }
    var reminderMinute by remember { mutableStateOf(reminder?.minuteOfHour?.toString() ?: "00") }
    var reminderDays by remember { mutableStateOf(reminder?.daysOfWeek ?: emptySet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New tracker" else "Edit tracker") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                }
                item {
                    OutlinedTextField(value = emoji, onValueChange = { emoji = it }, label = { Text("Emoji") }, modifier = Modifier.fillMaxWidth())
                }
                item {
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                }
                item {
                    Text("Type", fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TrackerType.entries.forEach { option ->
                            FilterChip(
                                selected = option == type,
                                onClick = { type = option },
                                label = { Text(option.name.replace("_", " ")) }
                            )
                        }
                    }
                }
                if (type != TrackerType.YES_NO) {
                    item {
                        OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit") }, modifier = Modifier.fillMaxWidth())
                    }
                }
                if (type != TrackerType.MEASURE) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Track streak target", modifier = Modifier.weight(1f))
                            Switch(checked = targetEnabled, onCheckedChange = { targetEnabled = it })
                        }
                    }
                    if (targetEnabled) {
                        item {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TargetPeriod.entries.forEach { option ->
                                    FilterChip(
                                        selected = option == targetPeriod,
                                        onClick = { targetPeriod = option },
                                        label = { Text(option.name.lowercase().replaceFirstChar(Char::titlecase)) }
                                    )
                                }
                            }
                        }
                        item {
                            OutlinedTextField(
                                value = targetValue,
                                onValueChange = { targetValue = it },
                                label = { Text("Target value") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Reminder", modifier = Modifier.weight(1f))
                        Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                    }
                }
                if (reminderEnabled) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = reminderHour,
                                onValueChange = { reminderHour = it },
                                label = { Text("Hour") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = reminderMinute,
                                onValueChange = { reminderMinute = it },
                                label = { Text("Minute") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Text("Reminder days", fontWeight = FontWeight.SemiBold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DayOfWeek.entries.forEach { day ->
                                FilterChip(
                                    selected = day in reminderDays,
                                    onClick = {
                                        reminderDays = if (day in reminderDays) reminderDays - day else reminderDays + day
                                    },
                                    label = { Text(day.name.take(3)) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedTarget = if (targetEnabled && type != TrackerType.MEASURE) {
                        targetValue.toDoubleOrNull()?.takeIf { it > 0.0 } ?: return@Button
                    } else {
                        null
                    }
                    val parsedHour = reminderHour.toIntOrNull() ?: 20
                    val parsedMinute = reminderMinute.toIntOrNull() ?: 0
                    if (name.isBlank()) return@Button
                    onSave(
                        TrackerDraft(
                            name = name,
                            emoji = emoji.ifBlank { null },
                            description = description.ifBlank { null },
                            type = type,
                            unit = unit.ifBlank { null },
                            colorHex = initial?.tracker?.colorHex ?: "#1F6FEB",
                            targetPeriod = if (targetEnabled && type != TrackerType.MEASURE) targetPeriod else null,
                            targetValue = parsedTarget,
                            reminderEnabled = reminderEnabled,
                            reminderHour = parsedHour.coerceIn(0, 23),
                            reminderMinute = parsedMinute.coerceIn(0, 59),
                            reminderDays = reminderDays
                        )
                    )
                }
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private data class EntryEditorState(
    val trackerId: Long,
    val trackerType: TrackerType,
    val unit: String?,
    val entryId: Long?,
    val existingDate: LocalDate,
    val existingValue: Double?,
    val existingNote: String?
) {
    companion object {
        fun new(detail: TrackerDetail): EntryEditorState = EntryEditorState(
            trackerId = detail.tracker.id,
            trackerType = detail.tracker.type,
            unit = detail.tracker.unit,
            entryId = null,
            existingDate = LocalDate.now(),
            existingValue = if (detail.tracker.type == TrackerType.YES_NO) 1.0 else null,
            existingNote = null
        )

        fun from(detail: TrackerDetail, entry: EntryItem): EntryEditorState = EntryEditorState(
            trackerId = detail.tracker.id,
            trackerType = detail.tracker.type,
            unit = detail.tracker.unit,
            entryId = entry.id,
            existingDate = entry.effectiveDate,
            existingValue = entry.value,
            existingNote = entry.note
        )
    }
}

@Composable
private fun EntryEditorDialog(
    state: EntryEditorState,
    onDismiss: () -> Unit,
    onSave: (EntryDraft) -> Unit,
    onDelete: (() -> Unit)?
) {
    var dateText by remember { mutableStateOf(state.existingDate.toString()) }
    var valueText by remember { mutableStateOf(state.existingValue?.toString().orEmpty()) }
    var note by remember { mutableStateOf(state.existingNote.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (state.entryId == null) "Log entry" else "Edit entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.trackerType != TrackerType.YES_NO) {
                    OutlinedTextField(
                        value = valueText,
                        onValueChange = { valueText = it },
                        label = { Text("Value ${state.unit?.let { "($it)" } ?: ""}".trim()) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val date = runCatching { LocalDate.parse(dateText) }.getOrNull() ?: return@Button
                val value = when (state.trackerType) {
                    TrackerType.YES_NO -> 1.0
                    TrackerType.COUNT -> valueText.toDoubleOrNull()
                    TrackerType.MEASURE -> valueText.toDoubleOrNull() ?: return@Button
                }
                onSave(
                    EntryDraft(
                        effectiveDate = date,
                        value = value,
                        note = note.ifBlank { null }
                    )
                )
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                onDelete?.let {
                    TextButton(onClick = it) {
                        Text("Delete")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

private fun formatValue(value: Double, unit: String?): String {
    val number = if (value == value.toInt().toDouble()) value.toInt().toString() else value.toString()
    return listOf(number, unit).filterNotNull().joinToString(" ")
}

private fun formatEntryValue(type: TrackerType, value: Double?, unit: String?): String {
    return when (type) {
        TrackerType.YES_NO -> formatValue(1.0, unit)
        TrackerType.COUNT -> formatValue(value ?: 1.0, unit)
        TrackerType.MEASURE -> value?.let { formatValue(it, unit) } ?: "No value"
    }
}
