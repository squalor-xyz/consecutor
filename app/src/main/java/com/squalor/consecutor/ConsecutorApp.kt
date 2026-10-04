package com.squalor.consecutor

import android.app.Application
import android.os.StrictMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ConsecutorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .penaltyLog()
                    .build()
            )
        }
        reminderScheduler.ensureNotificationChannel()
        appScope.launch(Dispatchers.IO) {
            ExportCache.clear(ExportCache.exportsDir(this@ConsecutorApp))
        }
    }

    private val appScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { TrackerRepository(database, database.trackerDao()) }
    val reminderScheduler by lazy { ReminderScheduler(this) }
}
