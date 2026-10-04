package com.squalor.consecutor

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.squalor.consecutor.ui.ArchivedScreen
import com.squalor.consecutor.ui.DashboardScreen
import com.squalor.consecutor.ui.EmptyState
import com.squalor.consecutor.ui.EntryEditorDialog
import com.squalor.consecutor.ui.EntryEditorState
import com.squalor.consecutor.ui.EntryEditorStateSaver
import com.squalor.consecutor.ui.SettingsScreen
import com.squalor.consecutor.ui.TrackerDetailScreen
import com.squalor.consecutor.ui.TrackerEditorDialog
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private enum class Screen {
    DASHBOARD,
    DETAIL,
    SETTINGS,
    ARCHIVED
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(viewModel: TrackerViewModel) {
    val archived by viewModel.archived.collectAsState()
    val pendingDelete by viewModel.pendingDelete.collectAsState()
    var showOverflow by remember { mutableStateOf(false) }
    val dashboard by viewModel.dashboard.collectAsState()
    val pendingImport by viewModel.pendingImport.collectAsState()
    val today by viewModel.today.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var screen by rememberSaveable { mutableStateOf(Screen.DASHBOARD) }
    val selectedId by viewModel.selectedId.collectAsState()
    val detailState by viewModel.detail.collectAsState()
    val selectedDetail = (detailState as? DetailState.Loaded)?.detail
    var editingTrackerId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showNewTrackerDialog by rememberSaveable { mutableStateOf(false) }
    var entryEditorState by rememberSaveable(stateSaver = EntryEditorStateSaver) { mutableStateOf<EntryEditorState?>(null) }
    val returnToDashboard: () -> Unit = {
        screen = Screen.DASHBOARD
        viewModel.select(null)
    }
    BackHandler(enabled = screen != Screen.DASHBOARD, onBack = returnToDashboard)
    LaunchedEffect(screen, selectedId) {
        if (screen == Screen.DETAIL && selectedId == null) screen = Screen.DASHBOARD
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshToday()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.prepareImport(context, uri)
        }
    }
    val saveCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ExportKind.CSV.mimeType)) { uri ->
        if (uri != null) {
            viewModel.exportTo(context, uri, ExportKind.CSV)
        }
    }
    val saveBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ExportKind.BACKUP.mimeType)) { uri ->
        if (uri != null) {
            viewModel.exportTo(context, uri, ExportKind.BACKUP)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.ensureReminderChannel()
        }
    }

    LaunchedEffect(Unit) {
        var snackbarJob: Job? = null
        viewModel.events.collect { event ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarJob?.cancel()
            // Start immediately so the next event can dismiss even a just-created snackbar.
            snackbarJob = launch(start = CoroutineStart.UNDISPATCHED) {
                when (event) {
                    is UiEvent.Deleted -> {
                        returnToDashboard()
                        snackbarHostState.showSnackbar(event.text)
                    }
                    is UiEvent.Message -> snackbarHostState.showSnackbar(event.text)
                    is UiEvent.Logged, is UiEvent.Cleared, is UiEvent.Archived -> {
                        val text = when (event) {
                            is UiEvent.Logged -> event.text
                            is UiEvent.Cleared -> event.text
                            is UiEvent.Archived -> event.text
                        }
                        // The default duration is indefinite while Undo is showing.
                        // The next event dismisses this snackbar.
                        val result = snackbarHostState.showSnackbar(
                            message = text,
                            actionLabel = "Undo"
                        )
                        if (result == SnackbarResult.ActionPerformed) viewModel.undo(event)
                    }
                }
            }
        }
    }

    pendingDelete?.let { pending ->
        val entryLabel = if (pending.entryCount == 1) "entry" else "entries"
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("Delete tracker permanently?") },
            text = { Text("Delete \"${pending.name}\" and its ${pending.entryCount} $entryLabel? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cancelDelete()
                    viewModel.deletePermanently(pending.trackerId)
                }) { Text("Delete permanently", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDelete) { Text("Cancel") }
            }
        )
    }

    pendingImport?.let { pending ->
        val exportDate = pending.exportedAtEpochMs?.let {
            val date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
            "exported ${date.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))}"
        } ?: "export date unknown"
        AlertDialog(
            onDismissRequest = viewModel::cancelImport,
            title = { Text("Replace all data?") },
            text = {
                Text("This backup has ${pending.trackerCount} trackers and ${pending.activeEntryCount} entries ($exportDate). Your current ${pending.currentTrackerCount} trackers will be removed.")
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmImport) {
                    Text("Replace", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelImport) { Text("Cancel") }
            }
        )
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

    selectedDetail?.takeIf { it.tracker.id == editingTrackerId }?.let { detail ->
        TrackerEditorDialog(
            initial = detail,
            onDismiss = { editingTrackerId = null },
            onSave = { draft ->
                maybeRequestNotifications(permissionLauncher, draft)
                viewModel.saveTracker(detail.tracker.id, draft)
                editingTrackerId = null
            }
        )
    }

    entryEditorState?.let { state ->
        EntryEditorDialog(
            state = state,
            today = today,
            otherYesNoDates = selectedDetail
                ?.takeIf { it.tracker.id == state.trackerId }
                ?.entries
                ?.filter { it.id != state.entryId }
                ?.map { it.effectiveDate }
                ?.toSet()
                .orEmpty(),
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
                            Screen.DASHBOARD -> "CONSECUTOR"
                            Screen.DETAIL -> selectedDetail?.tracker
                                ?.let { listOfNotNull(it.emoji, it.name).joinToString(" ").trim() }
                                .orEmpty()
                            Screen.SETTINGS -> "Settings"
                            Screen.ARCHIVED -> "Archived"
                        },
                        letterSpacing = if (screen == Screen.DASHBOARD) 2.sp else TextUnit.Unspecified,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    if (screen != Screen.DASHBOARD) {
                        IconButton(onClick = returnToDashboard) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                                IconButton(onClick = { editingTrackerId = detail.tracker.id }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit tracker")
                                }
                                Box {
                                    IconButton(onClick = { showOverflow = true }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Tracker options")
                                    }
                                    DropdownMenu(expanded = showOverflow, onDismissRequest = { showOverflow = false }) {
                                        DropdownMenuItem(text = { Text("Archive tracker") }, onClick = {
                                            showOverflow = false
                                            viewModel.archive(detail.tracker.id)
                                        })
                                        DropdownMenuItem(text = { Text("Delete permanently") }, onClick = {
                                            showOverflow = false
                                            viewModel.prepareDelete(detail.tracker.id, detail.tracker.name, detail.entries.size)
                                        })
                                    }
                                }
                            }
                        }
                        Screen.SETTINGS, Screen.ARCHIVED -> Unit
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
                Screen.SETTINGS, Screen.ARCHIVED -> Unit
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        when (screen) {
            Screen.DASHBOARD -> DashboardScreen(
                dashboard = dashboard,
                archivedCount = archived.size,
                onOpenArchived = { screen = Screen.ARCHIVED },
                padding = padding,
                onOpenTracker = {
                    viewModel.select(it)
                    screen = Screen.DETAIL
                },
                onLogToday = { summary -> viewModel.logToday(summary) },
                onToggleToday = { summary -> viewModel.toggleToday(summary) },
                onEditToday = { summary -> entryEditorState = EntryEditorState.forToday(summary, today) },
                onCreate = { showNewTrackerDialog = true }
            )
            Screen.DETAIL -> {
                when (val state = detailState) {
                    DetailState.Loading -> Unit
                    DetailState.NotFound -> EmptyState("Tracker not found.", padding = padding)
                    is DetailState.Loaded -> {
                        val detail = state.detail
                        TrackerDetailScreen(
                            detail = detail,
                            padding = padding,
                            onAddEntry = { entryEditorState = EntryEditorState.new(detail) },
                            onEditEntry = { entry -> entryEditorState = EntryEditorState.from(detail, entry) }
                        )
                    }
                }
            }
            Screen.ARCHIVED -> ArchivedScreen(
                archived = archived,
                padding = padding,
                onRestore = { viewModel.restore(it) },
                onDelete = { viewModel.prepareDelete(it.id, it.name) }
            )
            Screen.SETTINGS -> SettingsScreen(
                padding = padding,
                onSaveCsv = { saveCsvLauncher.launch(ExportKind.CSV.fileName(viewModel.today.value)) },
                onShareCsv = { viewModel.share(context, ExportKind.CSV) },
                onSaveBackup = { saveBackupLauncher.launch(ExportKind.BACKUP.fileName(viewModel.today.value)) },
                onShareBackup = { viewModel.share(context, ExportKind.BACKUP) },
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

private fun maybeRequestNotifications(
    launcher: androidx.activity.result.ActivityResultLauncher<String>,
    draft: TrackerDraft
) {
    if (draft.reminderEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
