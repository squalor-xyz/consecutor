package com.squalor.consecutor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private const val EXPORT_WARNING = "Exports are unencrypted and contain your full history. Store them somewhere private."

@Composable
internal fun SettingsScreen(
    padding: PaddingValues,
    onSaveCsv: () -> Unit,
    onShareCsv: () -> Unit,
    onSaveBackup: () -> Unit,
    onShareBackup: () -> Unit,
    onImportBackup: () -> Unit,
    notificationsEnabled: Boolean,
    canRequestNotifications: Boolean,
    onEnableNotifications: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Privacy-first defaults", style = MaterialTheme.typography.titleLarge)
            Text(
                "Consecutor keeps tracker data on-device, uses explicit export/import for portability, and avoids account-based sync in v1.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        item {
            SettingsCard(
                title = "CSV export",
                body = "Save or share a spreadsheet-friendly snapshot of trackers and entries. $EXPORT_WARNING"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onSaveCsv) { Text("Save to file…") }
                    TextButton(onClick = onShareCsv) { Text("Share…") }
                }
            }
        }
        item {
            SettingsCard(
                title = "Full backup",
                body = "Export or import the full app state as a versioned JSON backup file. Import previews the backup before replacing your current data. $EXPORT_WARNING"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onSaveBackup) { Text("Save to file…") }
                    TextButton(onClick = onShareBackup) { Text("Share…") }
                    TextButton(onClick = onImportBackup) { Text("Import backup") }
                }
            }
        }
        item {
            SettingsCard(
                title = "Notifications: ${if (notificationsEnabled) "On" else "Off"}",
                body = if (notificationsEnabled) {
                    "Reminders are local only."
                } else {
                    "Reminders are local only. Enable notifications to receive your scheduled reminders."
                }
            ) {
                Button(onClick = onEnableNotifications) {
                    Text(if (canRequestNotifications) "Allow notifications" else "Open settings")
                }
            }
        }
    }
}

@Composable
internal fun SettingsCard(
    title: String,
    body: String,
    action: @Composable () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium)
            action()
        }
    }
}
