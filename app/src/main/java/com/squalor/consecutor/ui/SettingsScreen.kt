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

@Composable
internal fun SettingsScreen(
    padding: PaddingValues,
    onExportCsv: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
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
                body = "Share a spreadsheet-friendly snapshot of trackers and entries."
            ) { Button(onClick = onExportCsv) { Text("Export CSV") } }
        }
        item {
            SettingsCard(
                title = "Full backup",
                body = "Export or import the full app state as a versioned JSON backup file."
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onExportBackup) { Text("Export backup") }
                    TextButton(onClick = onImportBackup) { Text("Import backup") }
                }
            }
        }
        item {
            SettingsCard(
                title = "Notifications",
                body = "Reminders are local only. Android 13+ needs notification permission."
            ) {
                Button(onClick = onEnableNotifications) {
                    Text("Allow notifications")
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
