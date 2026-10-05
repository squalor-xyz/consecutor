package com.squalor.consecutor.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import android.text.format.DateFormat
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import com.squalor.consecutor.EditorLimits
import com.squalor.consecutor.NumberRules
import com.squalor.consecutor.R
import com.squalor.consecutor.TrackerField
import com.squalor.consecutor.resolveTargetValue
import com.squalor.consecutor.validateTrackerForm
import com.squalor.consecutor.TrackerDetail
import com.squalor.consecutor.TrackerType
import com.squalor.consecutor.TrackerDraft
import com.squalor.consecutor.TargetPeriod

internal val ReminderDaysSaver = listSaver<Set<DayOfWeek>, Int>(
    // listSaver treats an empty list as unsaved, so include a count for an empty selection.
    save = { days -> listOf(days.size) + days.map(DayOfWeek::getValue) },
    restore = { values -> values.drop(1).map(DayOfWeek::of).toSet() }
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun TrackerEditorDialog(
    initial: TrackerDetail?,
    onDismiss: () -> Unit,
    onSave: (TrackerDraft) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initial?.tracker?.name.orEmpty()) }
    var emoji by rememberSaveable { mutableStateOf(initial?.tracker?.emoji.orEmpty()) }
    var description by rememberSaveable { mutableStateOf(initial?.tracker?.description.orEmpty()) }
    var unit by rememberSaveable { mutableStateOf(initial?.tracker?.unit.orEmpty()) }
    var type by rememberSaveable { mutableStateOf(initial?.tracker?.type ?: TrackerType.YES_NO) }
    var targetEnabled by rememberSaveable { mutableStateOf(initial?.target != null) }
    var targetPeriod by rememberSaveable { mutableStateOf(initial?.target?.period ?: TargetPeriod.DAILY) }
    var targetValue by rememberSaveable { mutableStateOf(initial?.target?.targetValue?.let { NumberRules.formatForInput(it) } ?: "1") }
    val reminder = initial?.reminder
    var reminderEnabled by rememberSaveable { mutableStateOf(reminder?.enabled ?: false) }
    var reminderHour by rememberSaveable { mutableIntStateOf(reminder?.hourOfDay ?: 20) }
    var reminderMinute by rememberSaveable { mutableIntStateOf(reminder?.minuteOfHour ?: 0) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var reminderDays by rememberSaveable(
        stateSaver = ReminderDaysSaver
    ) { mutableStateOf(reminder?.daysOfWeek ?: emptySet()) }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    val typeLocked = initial?.entries?.isNotEmpty() == true
    val weekDays = remember(locale) {
        val first = WeekFields.of(locale).firstDayOfWeek
        DayOfWeek.entries.sortedBy { Math.floorMod(it.value - first.value, 7) }
    }
    val errors = validateTrackerForm(
        name, emoji, description, unit, type, targetEnabled, targetPeriod, targetValue, locale
    )
    fun errorFor(field: TrackerField) = if (showErrors) errors[field] else null

    EditorDialog(
        title = stringResource(if (initial == null) R.string.editor_title_new_tracker else R.string.editor_title_edit_tracker),
        onDismiss = onDismiss,
        onConfirm = {
            showErrors = true
            if (errors.isNotEmpty()) return@EditorDialog
            val parsedTarget = resolveTargetValue(type, targetEnabled, targetPeriod, targetValue, locale)
            onSave(
                TrackerDraft(
                    name = name,
                    emoji = emoji.ifBlank { null },
                    description = description.ifBlank { null },
                    type = type,
                    unit = if (type == TrackerType.YES_NO) null else unit.ifBlank { null },
                    colorHex = initial?.tracker?.colorHex ?: "#1F6FEB",
                    targetPeriod = if (targetEnabled && type != TrackerType.MEASURE) targetPeriod else null,
                    targetValue = parsedTarget,
                    reminderEnabled = reminderEnabled,
                    reminderHour = reminderHour,
                    reminderMinute = reminderMinute,
                    reminderDays = reminderDays
                )
            )
        }
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.editor_name)) },
                    isError = errorFor(TrackerField.NAME) != null,
                    supportingText = errorSupportingText(errorFor(TrackerField.NAME), EditorLimits.NAME),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it },
                    label = { Text(stringResource(R.string.editor_emoji)) },
                    isError = errorFor(TrackerField.EMOJI) != null,
                    supportingText = errorSupportingText(errorFor(TrackerField.EMOJI)),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.editor_description)) },
                    isError = errorFor(TrackerField.DESCRIPTION) != null,
                    supportingText = errorSupportingText(errorFor(TrackerField.DESCRIPTION), EditorLimits.DESCRIPTION),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Text(stringResource(R.string.editor_type), fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TrackerType.entries.forEach { option ->
                        FilterChip(
                            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                            selected = option == type,
                            onClick = { type = option },
                            enabled = !typeLocked,
                            label = {
                                Text(
                                    stringResource(
                                        when (option) {
                                            TrackerType.YES_NO -> R.string.editor_type_yes_no
                                            TrackerType.COUNT -> R.string.editor_type_count
                                            TrackerType.MEASURE -> R.string.editor_type_measure
                                        }
                                    )
                                )
                            }
                        )
                    }
                }
                if (typeLocked) {
                    Text(stringResource(R.string.editor_type_locked), style = MaterialTheme.typography.bodySmall)
                }
            }
            if (type != TrackerType.YES_NO) {
                item {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text(stringResource(R.string.editor_unit)) },
                        isError = errorFor(TrackerField.UNIT) != null,
                        supportingText = errorSupportingText(errorFor(TrackerField.UNIT), EditorLimits.UNIT),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            if (type != TrackerType.MEASURE) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .toggleable(value = targetEnabled, role = Role.Switch, onValueChange = { targetEnabled = it }),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.editor_track_target), modifier = Modifier.weight(1f))
                        Switch(checked = targetEnabled, onCheckedChange = null)
                    }
                }
                if (targetEnabled) {
                    item {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TargetPeriod.entries.forEach { option ->
                                FilterChip(
                                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                                    selected = option == targetPeriod,
                                    onClick = { targetPeriod = option },
                                    label = {
                                        Text(
                                            stringResource(
                                                when (option) {
                                                    TargetPeriod.DAILY -> R.string.editor_period_daily
                                                    TargetPeriod.WEEKLY -> R.string.editor_period_weekly
                                                }
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                    if (type != TrackerType.YES_NO || targetPeriod == TargetPeriod.WEEKLY) {
                        item {
                            OutlinedTextField(
                                value = targetValue,
                                onValueChange = { targetValue = it },
                                label = {
                                    Text(stringResource(if (type == TrackerType.YES_NO) R.string.editor_days_per_week else R.string.editor_target_value))
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                isError = errorFor(TrackerField.TARGET) != null,
                                supportingText = errorSupportingText(errorFor(TrackerField.TARGET)),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .toggleable(value = reminderEnabled, role = Role.Switch, onValueChange = { reminderEnabled = it }),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.editor_reminder), modifier = Modifier.weight(1f))
                    Switch(checked = reminderEnabled, onCheckedChange = null)
                }
            }
            if (reminderEnabled) {
                item {
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) {
                        Text(
                            LocalTime.of(reminderHour, reminderMinute)
                                .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
                        )
                    }
                }
                item {
                    Text(stringResource(R.string.editor_reminder_days), fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        weekDays.forEach { day ->
                            FilterChip(
                                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                                selected = day in reminderDays,
                                onClick = {
                                    reminderDays = if (day in reminderDays) reminderDays - day else reminderDays + day
                                },
                                label = { Text(day.getDisplayName(TextStyle.SHORT, locale)) }
                            )
                        }
                    }
                    if (reminderDays.isEmpty()) {
                        Text(stringResource(R.string.editor_reminder_days_hint), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            is24Hour = DateFormat.is24HourFormat(LocalContext.current)
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(modifier = Modifier.heightIn(min = 48.dp), onClick = {
                    reminderHour = timeState.hour
                    reminderMinute = timeState.minute
                    showTimePicker = false
                }) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
