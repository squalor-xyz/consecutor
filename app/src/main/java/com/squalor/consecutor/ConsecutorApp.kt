package com.squalor.consecutor

import android.app.Application

class ConsecutorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        reminderScheduler.ensureNotificationChannel()
    }

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { TrackerRepository(database, database.trackerDao()) }
    val reminderScheduler by lazy { ReminderScheduler(this) }
}
