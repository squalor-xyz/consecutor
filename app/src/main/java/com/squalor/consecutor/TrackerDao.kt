package com.squalor.consecutor

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackerDao {
    @Transaction
    @Query("SELECT * FROM trackers ORDER BY isArchived ASC, updatedAtEpochMs DESC")
    fun observeTrackerBundles(): Flow<List<TrackerBundle>>

    @Transaction
    @Query("SELECT * FROM trackers WHERE id = :trackerId")
    fun observeTrackerBundle(trackerId: Long): Flow<TrackerBundle?>

    @Transaction
    @Query("SELECT * FROM trackers ORDER BY id ASC")
    suspend fun getTrackerBundles(): List<TrackerBundle>

    @Insert
    suspend fun insertTracker(tracker: TrackerEntity): Long

    @Update
    suspend fun updateTracker(tracker: TrackerEntity)

    @Insert
    suspend fun insertEntry(entry: EntryEntity): Long

    @Update
    suspend fun updateEntry(entry: EntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTarget(target: TargetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Query("DELETE FROM targets WHERE trackerId = :trackerId")
    suspend fun deleteTargetForTracker(trackerId: Long)

    @Query("DELETE FROM reminders WHERE trackerId = :trackerId")
    suspend fun deleteReminderForTracker(trackerId: Long)

    @Query("SELECT * FROM entries WHERE id = :entryId")
    suspend fun getEntryById(entryId: Long): EntryEntity?

    @Query("DELETE FROM entries")
    suspend fun deleteAllEntries()

    @Query("DELETE FROM targets")
    suspend fun deleteAllTargets()

    @Query("DELETE FROM reminders")
    suspend fun deleteAllReminders()

    @Query("DELETE FROM trackers")
    suspend fun deleteAllTrackers()
}
