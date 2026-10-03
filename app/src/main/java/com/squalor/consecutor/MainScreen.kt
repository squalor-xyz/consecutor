package com.squalor.consecutor

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.squalor.consecutor.ui.DashboardScreen
import com.squalor.consecutor.ui.EmptyState
import com.squalor.consecutor.ui.EntryEditorDialog
import com.squalor.consecutor.ui.EntryEditorState
import com.squalor.consecutor.ui.EntryEditorStateSaver
import com.squalor.consecutor.ui.SettingsScreen
import com.squalor.consecutor.ui.TrackerDetailScreen
import com.squalor.consecutor.ui.TrackerEditorDialog

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
                    viewModel.select(it)
                    screen = Screen.DETAIL
                },
                onQuickLog = { summary -> viewModel.quickLog(summary) }
            )
            Screen.DETAIL -> {
                when (val state = detailState) {
                    DetailState.Loading -> Unit
                    DetailState.NotFound -> EmptyState("Tracker not found.")
                    is DetailState.Loaded -> {
                        val detail = state.detail
                        TrackerDetailScreen(
                            detail = detail,
                            padding = padding,
                            onAddEntry = { entryEditorState = EntryEditorState.new(detail) },
                            onEditEntry = { entry -> entryEditorState = EntryEditorState.from(detail, entry) },
                            onArchive = {
                                viewModel.archiveTracker(detail.tracker.id)
                                screen = Screen.DASHBOARD
                                viewModel.select(null)
                            }
                        )
                    }
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

private fun maybeRequestNotifications(
    launcher: androidx.activity.result.ActivityResultLauncher<String>,
    draft: TrackerDraft
) {
    if (draft.reminderEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
