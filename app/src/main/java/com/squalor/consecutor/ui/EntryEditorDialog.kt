package com.squalor.consecutor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import com.squalor.consecutor.EditorLimits
import com.squalor.consecutor.EntryField
import com.squalor.consecutor.NumberRules
import com.squalor.consecutor.R
import com.squalor.consecutor.validateEntryForm
import com.squalor.consecutor.TrackerSummary
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
        fun forToday(summary: TrackerSummary, today: LocalDate): EntryEditorState = EntryEditorState(
            trackerId = summary.id,
            trackerType = summary.type,
            unit = summary.unit,
            entryId = null,
            existingDate = today,
            existingValue = if (summary.type == TrackerType.MEASURE) null else 1.0,
            existingNote = null
        )

        fun new(detail: TrackerDetail): EntryEditorState = EntryEditorState(
            trackerId = detail.tracker.id,
            trackerType = detail.tracker.type,
            unit = detail.tracker.unit,
            entryId = null,
            existingDate = LocalDate.now(),
            existingValue = if (detail.tracker.type == TrackerType.YES_NO) 1.0 else null,
            existingNote = null
        )

        fun forDate(detail: TrackerDetail, date: LocalDate): EntryEditorState = new(detail).copy(existingDate = date)

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun EntryEditorDialog(
    state: EntryEditorState,
    today: LocalDate,
    otherYesNoDates: Set<LocalDate>,
    onDismiss: () -> Unit,
    onSave: (EntryDraft) -> Unit,
    onDelete: (() -> Unit)?
) {
    var dateText by rememberSaveable { mutableStateOf(state.existingDate.toString()) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var valueText by rememberSaveable {
        mutableStateOf(
            state.existingValue?.let { NumberRules.formatForInput(it) }
                ?: if (state.trackerType == TrackerType.COUNT) "1" else ""
        )
    }
    var note by rememberSaveable { mutableStateOf(state.existingNote.orEmpty()) }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    val date = LocalDate.parse(dateText)
    val errors = validateEntryForm(state.trackerType, date, valueText, note, today, otherYesNoDates, locale)
    fun errorFor(field: EntryField) = if (showErrors) errors[field] else null

    EditorDialog(
        title = stringResource(if (state.entryId == null) R.string.entry_title_log else R.string.entry_title_edit),
        onDismiss = onDismiss,
        onConfirm = {
            showErrors = true
            if (errors.isNotEmpty()) return@EditorDialog
            val value = when (state.trackerType) {
                TrackerType.YES_NO -> 1.0
                else -> NumberRules.parseDecimal(valueText, locale) ?: return@EditorDialog
            }
            onSave(
                EntryDraft(
                    effectiveDate = date,
                    value = value,
                    note = note.ifBlank { null }
                )
            )
        }
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = date == today,
                    onClick = { dateText = today.toString() },
                    label = { Text(stringResource(R.string.entry_today)) }
                )
                FilterChip(
                    selected = date == today.minusDays(1),
                    onClick = { dateText = today.minusDays(1).toString() },
                    label = { Text(stringResource(R.string.entry_yesterday)) }
                )
            }
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
            }
            errorSupportingText(errorFor(EntryField.DATE))?.invoke()
            if (state.trackerType != TrackerType.YES_NO) {
                OutlinedTextField(
                    value = valueText,
                    onValueChange = { valueText = it },
                    label = {
                        Text(
                            if (state.unit != null) stringResource(R.string.entry_value_with_unit, state.unit)
                            else stringResource(R.string.entry_value)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = errorFor(EntryField.VALUE) != null,
                    supportingText = errorSupportingText(errorFor(EntryField.VALUE)),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.entry_note)) },
                isError = errorFor(EntryField.NOTE) != null,
                supportingText = errorSupportingText(errorFor(EntryField.NOTE), EditorLimits.NOTE),
                modifier = Modifier.fillMaxWidth()
            )
            onDelete?.let {
                TextButton(onClick = it) {
                    Text(stringResource(R.string.action_delete))
                }
            }
        }
    }
    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.toPickerMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = isSelectable(utcTimeMillis, today)
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { dateText = it.fromPickerMillis().toString() }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
