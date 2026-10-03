package com.squalor.consecutor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import com.squalor.consecutor.TrackerDetail
import com.squalor.consecutor.TrackerType
import com.squalor.consecutor.EntryItem
import com.squalor.consecutor.EntryDraft

internal data class EntryEditorState(
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

internal val EntryEditorStateSaver = listSaver<EntryEditorState?, Any?>(
    save = { state ->
        state?.let {
            listOf(
                it.trackerId,
                it.trackerType.name,
                it.unit,
                it.entryId,
                it.existingDate.toString(),
                it.existingValue,
                it.existingNote
            )
        } ?: emptyList()
    },
    restore = { values ->
        if (values.isEmpty()) null else EntryEditorState(
            trackerId = values[0] as Long,
            trackerType = TrackerType.valueOf(values[1] as String),
            unit = values[2] as String?,
            entryId = values[3] as Long?,
            existingDate = LocalDate.parse(values[4] as String),
            existingValue = values[5] as Double?,
            existingNote = values[6] as String?
        )
    }
)

@Composable
internal fun EntryEditorDialog(
    state: EntryEditorState,
    onDismiss: () -> Unit,
    onSave: (EntryDraft) -> Unit,
    onDelete: (() -> Unit)?
) {
    var dateText by rememberSaveable { mutableStateOf(state.existingDate.toString()) }
    var valueText by rememberSaveable { mutableStateOf(state.existingValue?.toString().orEmpty()) }
    var note by rememberSaveable { mutableStateOf(state.existingNote.orEmpty()) }

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
