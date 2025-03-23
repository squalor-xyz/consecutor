package com.squalor.consecutor.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Column

@Composable
fun AddEventDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val nameState = remember { mutableStateOf("") }
    val emojiState = remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Event") },
        text = {
            Column {
                TextField(
                    value = nameState.value,
                    onValueChange = { nameState.value = it },
                    label = { Text("Name") }
                )
                TextField(
                    value = emojiState.value,
                    onValueChange = { emojiState.value = it },
                    label = { Text("Emoji (optional)") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameState.value.isNotBlank()) {
                        onAdd(nameState.value, emojiState.value.takeIf { it.isNotBlank() })
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}