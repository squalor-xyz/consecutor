package com.squalor.consecutor

import android.app.Application
import net.sqlcipher.database.SQLiteDatabase

/**
 * Application class to provide singleton access to the encrypted database and repository.
 */
class ConsecutorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SQLiteDatabase.loadLibs(this) // Load SQLCipher native libraries
    }

    // Lazy initialization ensures the database is created only when needed
    val database by lazy { AppDatabase.getDatabase(this, "mysecretpassphrase") }
    // TODO: in a production app, use Android Keystore to generate and store it securely.
    val repository by lazy { EventRepository(database.eventDao()) }
}