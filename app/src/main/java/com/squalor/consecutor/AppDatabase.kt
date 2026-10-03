package com.squalor.consecutor

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [TrackerEntity::class, EntryEntity::class, TargetEntity::class, ReminderEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackerDao(): TrackerDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "consecutor.db"
                )
                    .apply {
                        // From 1.0 on every schema change needs an explicit Migration.
                        if (BuildConfig.DEBUG) {
                            fallbackToDestructiveMigration()
                        }
                    }
                    .build()
                    .also { instance = it }
            }
        }
    }
}
