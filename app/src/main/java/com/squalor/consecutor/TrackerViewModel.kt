package com.squalor.consecutor

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import java.time.LocalDate
import java.time.ZonedDateTime

sealed interface DetailState {
    object Loading : DetailState
    data class Loaded(val detail: TrackerDetail) : DetailState
    object NotFound : DetailState
}

@OptIn(ExperimentalCoroutinesApi::class)
class TrackerViewModel(
    private val repository: TrackerRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {
    private val _today = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = _today

    private val _selectedId = MutableStateFlow<Long?>(null)
    val selectedId: StateFlow<Long?> = _selectedId

    val detail: StateFlow<DetailState> = _selectedId.flatMapLatest { id ->
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
        _selectedId.value = id
    }

    fun saveTracker(existingTrackerId: Long?, draft: TrackerDraft) = viewModelScope.launch {
        runCatching {
            existingTrackerId?.also { repository.updateTracker(it, draft) } ?: repository.createTracker(draft)
        }.onSuccess {
            rescheduleReminders()
            _message.value = if (existingTrackerId == null) "Tracker created." else "Tracker updated."
        }.onFailure {
            _message.value = "Unable to save tracker."
        }
    }

    fun archiveTracker(trackerId: Long) = viewModelScope.launch {
        repository.archiveTracker(trackerId)
        reminderScheduler.cancelTracker(trackerId)
        _message.value = "Tracker archived."
    }

    fun addEntry(trackerId: Long, trackerType: TrackerType, draft: EntryDraft) = viewModelScope.launch {
        repository.addEntry(trackerId, trackerType, draft)
        _message.value = "Entry added."
    }

    fun updateEntry(entryId: Long, trackerId: Long, trackerType: TrackerType, draft: EntryDraft) = viewModelScope.launch {
        repository.updateEntry(entryId, trackerId, trackerType, draft)
        _message.value = "Entry updated."
    }

    fun deleteEntry(entryId: Long, trackerId: Long) = viewModelScope.launch {
        repository.deleteEntry(entryId, trackerId)
        _message.value = "Entry deleted."
    }

    fun quickLog(summary: TrackerSummary) = viewModelScope.launch {
        repository.quickLog(summary, today.value)
        _message.value = "Logged ${summary.name}."
    }

    fun exportCsv(context: Context) = viewModelScope.launch {
        runCatching {
            repository.exportCsv(context)
        }.onSuccess { file ->
            shareFile(context, file, "text/csv", "Share CSV")
        }.onFailure {
            _message.value = "Unable to export CSV."
        }
    }

    fun exportBackup(context: Context) = viewModelScope.launch {
        runCatching {
            repository.exportBackup(context)
        }.onSuccess { file ->
            shareFile(context, file, "application/json", "Share backup")
        }.onFailure {
            _message.value = "Unable to export backup."
        }
    }

    fun importBackup(context: Context, uri: Uri) = viewModelScope.launch {
        val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        if (raw.isNullOrBlank()) {
            _message.value = "Unable to read backup file."
            return@launch
        }
        runCatching {
            repository.importBackup(raw)
        }.onSuccess {
            rescheduleReminders()
            _message.value = "Backup imported."
        }.onFailure {
            val detail = it.message?.takeIf { m -> m.isNotBlank() } ?: it.javaClass.simpleName
            _message.value = "Backup import failed: $detail"
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
