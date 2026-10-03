package com.squalor.consecutor.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import com.squalor.consecutor.TrackerDetail
import com.squalor.consecutor.TrackerType
import com.squalor.consecutor.TrackerDraft
import com.squalor.consecutor.TargetPeriod

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TrackerEditorDialog(
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
