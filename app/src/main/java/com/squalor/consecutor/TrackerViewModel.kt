package com.squalor.consecutor

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZonedDateTime

sealed interface DetailState {
    object Loading : DetailState
    data class Loaded(val detail: TrackerDetail) : DetailState
    object NotFound : DetailState
}

@OptIn(ExperimentalCoroutinesApi::class)
class TrackerViewModel(
    private val savedState: SavedStateHandle,
    private val repository: TrackerRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {
    private val _today = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = _today

    val selectedId: StateFlow<Long?> = savedState.getStateFlow("selectedId", null)

    val detail: StateFlow<DetailState> = selectedId.flatMapLatest { id ->
        if (id == null) {
            flowOf<DetailState>(DetailState.Loading)
        } else {
            repository.observeTrackerDetail(id, today)
                .map<TrackerDetail?, DetailState> { detail ->
                    detail?.let { DetailState.Loaded(it) } ?: DetailState.NotFound
                }
                .onStart { emit(DetailState.Loading) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailState.Loading)

    val dashboard: StateFlow<List<TrackerSummary>> = repository.observeDashboard(today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    init {
        viewModelScope.launch {
            while (true) {
                delay(millisUntilNextMidnight(ZonedDateTime.now()) + 1_000)
                refreshToday()
            }
        }
    }

    fun refreshToday() {
        _today.value = LocalDate.now()
    }

    fun select(id: Long?) {
        savedState["selectedId"] = id
    }

    private fun launchAction(success: String?, failure: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { success?.let { _message.value = it } }
                .onFailure {
                    if (it is CancellationException) throw it
                    _message.value = failure
                }
        }
    }

    fun saveTracker(existingTrackerId: Long?, draft: TrackerDraft) = launchAction(
        success = if (existingTrackerId == null) "Tracker created." else "Tracker updated.",
        failure = "Unable to save tracker."
    ) {
        if (existingTrackerId != null) {
            repository.updateTracker(existingTrackerId, draft)
        } else {
            repository.createTracker(draft)
        }
        rescheduleReminders()
    }

    fun archiveTracker(trackerId: Long) = launchAction("Tracker archived.", "Unable to archive tracker.") {
        repository.archiveTracker(trackerId)
        reminderScheduler.cancelTracker(trackerId)
    }

    fun addEntry(trackerId: Long, trackerType: TrackerType, draft: EntryDraft) =
        launchAction("Entry added.", "Unable to add entry.") {
            repository.addEntry(trackerId, trackerType, draft)
        }

    fun updateEntry(entryId: Long, trackerId: Long, trackerType: TrackerType, draft: EntryDraft) =
        launchAction("Entry updated.", "Unable to update entry.") {
            repository.updateEntry(entryId, trackerId, trackerType, draft)
        }

    fun deleteEntry(entryId: Long, trackerId: Long) =
        launchAction("Entry deleted.", "Unable to delete entry.") {
            repository.deleteEntry(entryId, trackerId)
        }

    fun quickLog(summary: TrackerSummary) =
        launchAction("Logged ${summary.name}.", "Unable to log ${summary.name}.") {
            repository.quickLog(summary, today.value)
        }

    fun exportCsv(context: Context) = viewModelScope.launch {
        runCatching {
            repository.exportCsv(context)
        }.onSuccess { file ->
            shareFile(context, file, "text/csv", "Share CSV")
        }.onFailure {
            if (it is CancellationException) throw it
            _message.value = "Unable to export CSV."
        }
    }

    fun exportBackup(context: Context) = viewModelScope.launch {
        runCatching {
            repository.exportBackup(context)
        }.onSuccess { file ->
            shareFile(context, file, "application/json", "Share backup")
        }.onFailure {
            if (it is CancellationException) throw it
            _message.value = "Unable to export backup."
        }
    }

    fun importBackup(context: Context, uri: Uri) = viewModelScope.launch {
        val raw = runCatching {
            withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }
        }.getOrElse {
            if (it is CancellationException) throw it
            null
        }
        if (raw.isNullOrBlank()) {
            _message.value = "Unable to read backup file."
            return@launch
        }
        runCatching {
            repository.importBackup(raw)
            rescheduleReminders()
        }.onSuccess {
            _message.value = "Backup imported."
        }.onFailure {
            if (it is CancellationException) throw it
            _message.value = if (it is BackupFormatException) {
                "Backup import failed: ${it.message}"
            } else {
                "Backup import failed."
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun ensureReminderChannel() {
        reminderScheduler.ensureNotificationChannel()
    }

    private suspend fun rescheduleReminders() {
        repository.getReminderBundles().forEach { bundle ->
            reminderScheduler.scheduleTracker(bundle)
        }
    }

    private fun shareFile(context: Context, file: java.io.File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }
}
