package com.squalor.consecutor.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
internal fun rememberNotificationPermissionState(refreshKey: Int = 0): State<Boolean> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val granted = remember(context, refreshKey) {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    DisposableEffect(context, lifecycleOwner, granted) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted.value = NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return granted
}

// Keep automatic reminder-save prompts to one per install, including across restarts.
internal fun notificationPermissionRequested(context: Context): Boolean =
    context.getSharedPreferences("notification_permission", Context.MODE_PRIVATE)
        .getBoolean("requested", false)

internal fun recordNotificationPermissionRequest(context: Context) {
    context.getSharedPreferences("notification_permission", Context.MODE_PRIVATE)
        .edit().putBoolean("requested", true).apply()
}

internal fun canRequestNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    ) return false
    if (!notificationPermissionRequested(context)) return true
    var current = context
    while (current is ContextWrapper) {
        if (current is Activity) {
            return ActivityCompat.shouldShowRequestPermissionRationale(
                current, Manifest.permission.POST_NOTIFICATIONS
            )
        }
        current = current.baseContext
    }
    return false
}

internal fun openNotificationSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    )
}
