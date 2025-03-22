package com.squalor.consecutor

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel that manages UI-related data and operations for events.
 */
class EventViewModel(private val repository: EventRepository) : ViewModel() {
    val allEvents: Flow<List<Event>> = repository.allEvents

    fun insert(event: Event) = viewModelScope.launch {
        repository.insert(event)
    }

    fun update(event: Event) = viewModelScope.launch {
        repository.update(event)
    }

    fun delete(event: Event) = viewModelScope.launch {
        repository.delete(event)
    }

    fun incrementEvent(event: Event) = viewModelScope.launch {
        repository.incrementEvent(event)
    }

    /**
     * Exports all events to a CSV file and shares it via an Intent.
     */
    fun exportData(context: Context) = viewModelScope.launch {
        val events = repository.allEvents.first() // Get current list
        val csv = exportEventsToCsv(events)
        val file = File(context.getExternalFilesDir(null), "events.csv")
        file.writeText(csv)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share CSV"))
    }

    private fun exportEventsToCsv(events: List<Event>): String {
        val sb = StringBuilder()
        sb.append("ID,Name,Emoji,Consecutive Count,Total Count,Last Incremented\n")
        events.forEach { event ->
            sb.append("${event.id},${event.name},${event.emoji ?: ""},${event.consecutiveCount},${event.totalCount},${event.lastIncremented ?: ""}\n")
        }
        return sb.toString()
    }
}