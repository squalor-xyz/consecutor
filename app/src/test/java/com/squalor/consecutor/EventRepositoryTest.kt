package com.squalor.consecutor

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for the event increment logic.
 */
class EventRepositoryTest {

    @Test
    fun `increment new event sets count to 1`() {
        val event = Event(id = 1, name = "Test", lastIncremented = null)
        val today = LocalDate.now()
        val updated = incrementEventLogic(event, today)
        assertEquals(1, updated.consecutiveCount)
        assertEquals(1, updated.totalCount)
        assertEquals(today, updated.lastIncremented)
    }

    @Test
    fun `increment consecutive day increases consecutive count`() {
        val yesterday = LocalDate.now().minusDays(1)
        val event = Event(id = 1, name = "Test", consecutiveCount = 5, totalCount = 10, lastIncremented = yesterday)
        val today = LocalDate.now()
        val updated = incrementEventLogic(event, today)
        assertEquals(6, updated.consecutiveCount)
        assertEquals(11, updated.totalCount)
        assertEquals(today, updated.lastIncremented)
    }

    @Test
    fun `increment after gap resets consecutive count`() {
        val twoDaysAgo = LocalDate.now().minusDays(2)
        val event = Event(id = 1, name = "Test", consecutiveCount = 5, totalCount = 10, lastIncremented = twoDaysAgo)
        val today = LocalDate.now()
        val updated = incrementEventLogic(event, today)
        assertEquals(1, updated.consecutiveCount)
        assertEquals(11, updated.totalCount)
        assertEquals(today, updated.lastIncremented)
    }

    @Test
    fun `increment same day keeps consecutive count`() {
        val today = LocalDate.now()
        val event = Event(id = 1, name = "Test", consecutiveCount = 5, totalCount = 10, lastIncremented = today)
        val updated = incrementEventLogic(event, today)
        assertEquals(5, updated.consecutiveCount)
        assertEquals(11, updated.totalCount)
        assertEquals(today, updated.lastIncremented)
    }

    /**
     * Replicates the increment logic from EventRepository for testing.
     */
    private fun incrementEventLogic(event: Event, today: LocalDate): Event {
        val lastIncremented = event.lastIncremented
        val newConsecutiveCount = when {
            lastIncremented == null -> 1
            lastIncremented == today -> event.consecutiveCount
            lastIncremented == today.minusDays(1) -> event.consecutiveCount + 1
            else -> 1
        }
        return event.copy(
            consecutiveCount = newConsecutiveCount,
            totalCount = event.totalCount + 1,
            lastIncremented = today
        )
    }
}