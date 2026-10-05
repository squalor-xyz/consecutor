package com.squalor.consecutor.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.squalor.consecutor.BuildConfig
import com.squalor.consecutor.R

private const val SOURCE_URL = "https://github.com/squalor-xyz/consecutor"

@OptIn(ExperimentalLayoutApi::class)
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
    onEnableNotifications: () -> Unit,
    onLinkFailed: () -> Unit
) {
    val context = LocalContext.current
    var showLicences by rememberSaveable { mutableStateOf(false) }
    ContentColumn(padding) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(stringResource(R.string.settings_privacy_line), style = MaterialTheme.typography.bodyMedium)
            }
            item {
                SettingsCard(
                    title = stringResource(R.string.settings_csv_title),
                    body = stringResource(R.string.settings_csv_body, stringResource(R.string.settings_export_warning))
                ) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onSaveCsv, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.settings_save_to_file)) }
                        TextButton(onClick = onShareCsv, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.settings_share)) }
                    }
                }
            }
            item {
                SettingsCard(
                    title = stringResource(R.string.settings_backup_title),
                    body = stringResource(R.string.settings_backup_body, stringResource(R.string.settings_export_warning))
                ) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onSaveBackup, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.settings_save_to_file)) }
                        TextButton(onClick = onShareBackup, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.settings_share)) }
                        TextButton(onClick = onImportBackup, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.settings_import_backup)) }
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
                    Button(onClick = onEnableNotifications, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(if (canRequestNotifications) R.string.settings_allow_notifications else R.string.action_open_settings))
                    }
                }
            }
            item {
                SettingsCard(
                    title = stringResource(R.string.settings_about_title, BuildConfig.VERSION_NAME),
                    body = stringResource(R.string.settings_about_body)
                ) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE_URL)))
                                } catch (e: ActivityNotFoundException) {
                                    onLinkFailed()
                                }
                            },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) { Text(stringResource(R.string.settings_source_code)) }
                        TextButton(
                            onClick = { showLicences = true },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) { Text(stringResource(R.string.settings_licences)) }
                    }
                }
            }
        }
    }
    if (showLicences) {
        val notices = remember {
            context.resources.openRawResource(R.raw.third_party_notices).bufferedReader().use { it.readText() }
        }
        AlertDialog(
            onDismissRequest = { showLicences = false },
            title = { Text(stringResource(R.string.settings_licences)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(notices, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showLicences = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text(stringResource(R.string.action_close)) }
            }
        )
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
