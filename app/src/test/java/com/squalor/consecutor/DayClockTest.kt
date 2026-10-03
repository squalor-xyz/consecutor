package com.squalor.consecutor

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class DayClockTest {
    @Test
    fun `at 23 59 30 it is 30000`() {
        val now = ZonedDateTime.parse("2026-04-21T23:59:30Z")
        assertEquals(30_000L, millisUntilNextMidnight(now))
    }

    @Test
    fun `at midnight it is 86400000`() {
        val now = ZonedDateTime.parse("2026-04-21T00:00:00Z")
        assertEquals(86_400_000L, millisUntilNextMidnight(now))
    }

    @Test
    fun `spring forward day in New York is 23 hours long`() {
        val zone = ZoneId.of("America/New_York")
        val now = LocalDate.of(2026, 3, 8).atStartOfDay(zone)
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(zone)
        val expected = Duration.between(now, nextMidnight).toMillis()
        assertEquals(Duration.ofHours(23).toMillis(), expected)
        assertEquals(expected, millisUntilNextMidnight(now))
    }
}
