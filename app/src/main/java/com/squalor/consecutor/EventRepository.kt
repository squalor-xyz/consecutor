package com.squalor.consecutor

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Repository layer that encapsulates data operations and business logic.
 */
class EventRepository(private val eventDao: EventDao) {
    val allEvents: Flow<List<Event>> = eventDao.getAll()

    /**
     * Inserts a new event into the database.
     */
    suspend fun insert(event: Event) {
        eventDao.insert(event)
    }

    /**
     * Updates an existing event in the database.
     */
    suspend fun update(event: Event) {
        eventDao.update(event)
    }

    /**
     * Deletes an event from the database.
     */
    suspend fun delete(event: Event) {
        eventDao.delete(event)
    }

    /**
     * Increments an event's counters based on the last incremented date.
     * - If last incremented was yesterday, consecutive count increases.
     * - Otherwise, consecutive count resets to 1.
     * - Total count always increments.
     */
    suspend fun incrementEvent(event: Event) {
        val today = LocalDate.now()
        val lastIncremented = event.lastIncremented
        val newConsecutiveCount = when {
            lastIncremented == null -> 1 // First increment
            lastIncremented == today -> event.consecutiveCount // Same day, no change
            lastIncremented == today.minusDays(1) -> event.consecutiveCount + 1 // Consecutive day
            else -> 1 // Streak broken
        }
        val updatedEvent = event.copy(
            consecutiveCount = newConsecutiveCount,
            totalCount = event.totalCount + 1,
            lastIncremented = today
        )
        update(updatedEvent)
    }
}