package com.squalor.consecutor

import android.app.Application
import android.os.StrictMode

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
    }

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { TrackerRepository(database, database.trackerDao()) }
    val reminderScheduler by lazy { ReminderScheduler(this) }
}
