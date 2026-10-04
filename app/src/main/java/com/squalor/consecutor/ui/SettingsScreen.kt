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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.squalor.consecutor.R

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
            Text(stringResource(R.string.settings_privacy_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.settings_privacy_body),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        item {
            SettingsCard(
                title = stringResource(R.string.settings_csv_title),
                body = stringResource(R.string.settings_csv_body, stringResource(R.string.settings_export_warning))
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onSaveCsv) { Text(stringResource(R.string.settings_save_to_file)) }
                    TextButton(onClick = onShareCsv) { Text(stringResource(R.string.settings_share)) }
                }
            }
        }
        item {
            SettingsCard(
                title = stringResource(R.string.settings_backup_title),
                body = stringResource(R.string.settings_backup_body, stringResource(R.string.settings_export_warning))
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onSaveBackup) { Text(stringResource(R.string.settings_save_to_file)) }
                    TextButton(onClick = onShareBackup) { Text(stringResource(R.string.settings_share)) }
                    TextButton(onClick = onImportBackup) { Text(stringResource(R.string.settings_import_backup)) }
                }
            }
        }
        item {
            SettingsCard(
                title = stringResource(
                    if (notificationsEnabled) R.string.settings_notifications_on_title else R.string.settings_notifications_off_title
                ),
                body = stringResource(
                    if (notificationsEnabled) R.string.settings_notifications_on_body else R.string.settings_notifications_off_body
                )
            ) {
                Button(onClick = onEnableNotifications) {
                    Text(stringResource(if (canRequestNotifications) R.string.settings_allow_notifications else R.string.action_open_settings))
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
