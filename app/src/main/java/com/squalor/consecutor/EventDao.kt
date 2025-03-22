package com.squalor.consecutor

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for performing CRUD operations on the events table.
 */
@Dao
interface EventDao {
    @Query("SELECT * FROM events")
    fun getAll(): Flow<List<Event>>

    @Insert
    suspend fun insert(event: Event)

    @Update
    suspend fun update(event: Event)

    @Delete
    suspend fun delete(event: Event)
}