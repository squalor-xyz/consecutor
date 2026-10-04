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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed interface DetailState {
    object Loading : DetailState
    data class Loaded(val detail: TrackerDetail) : DetailState
    object NotFound : DetailState
}

data class PendingTrackerDelete(val trackerId: Long, val name: String, val entryCount: Int)

data class PendingImport(
    val raw: String,
    val trackerCount: Int,
    val activeEntryCount: Int,
    val exportedAtEpochMs: Long?,
    val currentTrackerCount: Int
)

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

    val archived: StateFlow<List<TrackerEntity>> = repository.observeArchivedTrackers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _pendingDelete = MutableStateFlow<PendingTrackerDelete?>(null)
    val pendingDelete: StateFlow<PendingTrackerDelete?> = _pendingDelete

    private val eventChannel = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = eventChannel.receiveAsFlow()
    private val toggleMutex = Mutex()

    private val _pendingImport = MutableStateFlow<PendingImport?>(null)
    val pendingImport: StateFlow<PendingImport?> = _pendingImport
    private var importContext: Context? = null
    private var importJob: Job? = null

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
                .onSuccess { success?.let { eventChannel.send(UiEvent.Message(it)) } }
                .onFailure {
                    if (it is CancellationException) throw it
                    eventChannel.send(UiEvent.Message(failure))
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

    fun archive(id: Long) = launchAction(null, "Unable to archive tracker.") {
        repository.archiveTracker(id)
        reminderScheduler.cancelTracker(id)
        select(null)
        eventChannel.send(UiEvent.Archived("Tracker archived.", id))
    }

    private suspend fun restoreTracker(id: Long) {
        repository.unarchiveTracker(id)
        repository.getTrackerBundle(id)?.let(reminderScheduler::scheduleTracker)
    }

    fun restore(id: Long) = launchAction("Tracker restored.", "Unable to restore tracker.") {
        restoreTracker(id)
    }

    fun prepareDelete(id: Long, name: String, entryCount: Int? = null) =
        launchAction(null, "Unable to load tracker entry count.") {
            _pendingDelete.value = PendingTrackerDelete(id, name, entryCount ?: repository.countActiveEntries(id))
        }

    fun cancelDelete() {
        _pendingDelete.value = null
    }

    fun deletePermanently(id: Long) = launchAction(null, "Unable to delete tracker.") {
        repository.deleteTracker(id)
        reminderScheduler.cancelTracker(id)
        select(null)
        eventChannel.send(UiEvent.Deleted("Tracker deleted.", id))
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
        launchAction(null, "Unable to delete entry.") {
            repository.deleteEntry(entryId, trackerId)
            eventChannel.send(UiEvent.Cleared("Entry deleted", trackerId, listOf(entryId)))
        }

    fun logToday(summary: TrackerSummary) {
        val date = today.value
        launchAction(null, "Unable to log ${summary.name}.") {
            val entryId = repository.addEntry(summary.id, summary.type, EntryDraft(date, 1.0, null))
            eventChannel.send(loggedEvent(summary, entryId))
        }
    }

    fun clearToday(summary: TrackerSummary) = launchAction(null, "Unable to clear ${summary.name}.") {
        repository.softDeleteEntries(summary.id, summary.todayEntryIds)
        eventChannel.send(UiEvent.Cleared("Cleared ${summary.name}", summary.id, summary.todayEntryIds))
    }

    fun toggleToday(summary: TrackerSummary) {
        val date = today.value
        launchAction(null, "Unable to log ${summary.name}.") {
            toggleMutex.withLock {
                when (val result = repository.toggleToday(summary.id, date)) {
                    is TrackerRepository.TodayToggleResult.Logged -> eventChannel.send(loggedEvent(summary, result.entryId))
                    is TrackerRepository.TodayToggleResult.Cleared -> eventChannel.send(
                        UiEvent.Cleared("Cleared ${summary.name}", summary.id, result.entryIds)
                    )
                }
            }
        }
    }

    private fun loggedEvent(summary: TrackerSummary, entryId: Long) = UiEvent.Logged(
        "Logged ${summary.name}" + if (summary.type == TrackerType.COUNT) " (+1)" else "",
        summary.id,
        entryId
    )

    fun undo(event: UiEvent) = launchAction(null, "Unable to undo.") {
        when (event) {
            is UiEvent.Logged -> repository.deleteEntry(event.entryId, event.trackerId)
            is UiEvent.Cleared -> repository.restoreEntries(event.trackerId, event.entryIds)
            is UiEvent.Archived -> restoreTracker(event.trackerId)
            is UiEvent.Message, is UiEvent.Deleted -> Unit
        }
    }

    fun exportTo(context: Context, uri: Uri, kind: ExportKind) = viewModelScope.launch {
        runCatching {
            withContext(Dispatchers.IO) {
                val bytes = exportText(kind).toByteArray()
                val stream = context.contentResolver.openOutputStream(uri, "wt")
                    ?: throw IOException("No output stream for $uri")
                stream.use { it.write(bytes) }
            }
        }.onSuccess {
            eventChannel.send(UiEvent.Message("Saved ${kind.label}."))
        }.onFailure {
            if (it is CancellationException) throw it
            eventChannel.send(UiEvent.Message("Unable to save ${kind.label}."))
        }
    }

    fun share(context: Context, kind: ExportKind) = viewModelScope.launch {
        runCatching {
            withContext(Dispatchers.IO) {
                val dir = ExportCache.exportsDir(context)
                ExportCache.clear(dir)
                val file = File(dir, kind.fileName(today.value))
                file.writeText(exportText(kind))
                file
            }
        }.onSuccess { file ->
            shareFile(context, file, kind.mimeType, kind.chooserTitle)
        }.onFailure {
            if (it is CancellationException) throw it
            eventChannel.send(UiEvent.Message("Unable to export ${kind.label}."))
        }
    }

    private suspend fun exportText(kind: ExportKind): String = when (kind) {
        ExportKind.CSV -> repository.entriesCsv()
        ExportKind.BACKUP -> repository.backupJson()
    }

    fun prepareImport(context: Context, uri: Uri) {
        if (importJob?.isActive == true) return
        _pendingImport.value = null
        importContext = null
        val appContext = context.applicationContext
        importJob = viewModelScope.launch {
            runCatching {
                val raw = withContext(Dispatchers.IO) {
                    val stream = appContext.contentResolver.openInputStream(uri)
                        ?: throw IOException("Unable to open backup file.")
                    stream.use(::readBackupText)
                }
                val payload = withContext(Dispatchers.Default) { BackupCodec.decode(raw) }
                PendingImport(
                    raw = raw,
                    trackerCount = payload.trackers.size,
                    activeEntryCount = payload.trackers.sumOf { tracker -> tracker.entries.count { !it.isDeleted } },
                    exportedAtEpochMs = payload.exportedAtEpochMs,
                    currentTrackerCount = repository.getTrackerCount()
                )
            }.onSuccess {
                importContext = appContext
                _pendingImport.value = it
            }.onFailure {
                if (it is CancellationException) throw it
                val text = if (it is BackupFormatException) it.message else null
                eventChannel.send(UiEvent.Message(text ?: "Unable to read backup file."))
            }
        }
    }

    fun cancelImport() {
        _pendingImport.value = null
        importContext = null
    }

    fun confirmImport() {
        if (importJob?.isActive == true) return
        val pending = _pendingImport.value ?: return
        val context = importContext ?: return
        cancelImport()
        importJob = viewModelScope.launch {
            val savedCopy = runCatching {
                withContext(Dispatchers.IO) {
                    if (repository.getTrackerCount() == 0) return@withContext false
                    val dir = File(context.filesDir, "backups")
                    if (!dir.isDirectory && !dir.mkdirs()) throw IOException("Unable to create backup directory.")
                    val text = repository.backupJson()
                    val timestamp = ZonedDateTime.now(ZoneOffset.UTC)
                        .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT))
                    val file = File(dir, "pre-import-$timestamp.json")
                    if (!file.createNewFile()) throw IOException("A safety backup already exists for this second.")
                    try {
                        file.writeText(text)
                    } catch (failure: Exception) {
                        file.delete()
                        throw failure
                    }
                    BackupFiles.prune(dir)
                    true
                }
            }.getOrElse {
                if (it is CancellationException) throw it
                eventChannel.send(UiEvent.Message("Unable to save a safety backup. Your data was not replaced."))
                return@launch
            }
            runCatching {
                reminderScheduler.cancelAll(repository.getReminderBundles().map { it.tracker.id })
                withContext(Dispatchers.IO) { repository.importBackup(pending.raw) }
                rescheduleReminders()
            }.onSuccess {
                eventChannel.send(UiEvent.Message(if (savedCopy) {
                    "Backup imported. A copy of your previous data was saved in the app's private storage."
                } else {
                    "Backup imported."
                }))
            }.onFailure {
                // Re-arm the current database's reminders after a failed replacement.
                withContext(NonCancellable) { runCatching { rescheduleReminders() } }
                if (it is CancellationException) throw it
                eventChannel.send(UiEvent.Message(if (it is BackupFormatException) {
                    "Backup import failed: ${it.message}"
                } else {
                    "Backup import failed."
                }))
            }
        }
    }

    fun ensureReminderChannel() {
        reminderScheduler.ensureNotificationChannel()
    }

    private suspend fun rescheduleReminders() {
        repository.getReminderBundles().forEach { bundle ->
            reminderScheduler.scheduleTracker(bundle)
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }
}

/** Enforces the byte limit even when the document provider reports no size. */
internal fun readBackupText(stream: InputStream): String {
    val limit = 20 * 1024 * 1024
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val read = stream.read(buffer)
        if (read == -1) break
        if (output.size() + read > limit) {
            throw BackupFormatException("That file is too large to be a backup.")
        }
        output.write(buffer, 0, read)
    }
    return output.toString(Charsets.UTF_8.name())
}
