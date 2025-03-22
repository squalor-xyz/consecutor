package com.squalor.consecutor

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Represents an event that the user can track with counters and metadata.
 */
@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,                // Name of the event (e.g., "Visit Jim")
    val emoji: String? = null,       // Optional emoji for visual distinction
    val consecutiveCount: Int = 0,   // Number of consecutive days the event has been incremented
    val totalCount: Int = 0,         // Total number of times the event has been incremented
    val lastIncremented: LocalDate? = null, // Date of the last increment
    val folder: String? = null       // Optional folder for organization
    // Future fields: reminderTime, labels (as a separate table)
)