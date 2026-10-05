package com.squalor.consecutor

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
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

    // Virtual time stands for the monotonic clock; clockOffsetMs stands for a wall-clock change.
    private class FakeClock(start: String, private val currentTimeMs: () -> Long) {
        private val origin = ZonedDateTime.parse(start)
        var clockOffsetMs = 0L
        fun now(): ZonedDateTime = origin.plusNanos((currentTimeMs() + clockOffsetMs) * 1_000_000)
    }

    @Test
    fun `awaitDayBoundary waits until just after midnight`() = runTest {
        val clock = FakeClock("2026-04-21T23:59:30Z") { currentTime }
        var done = false
        backgroundScope.launch { awaitDayBoundary(clock::now, Channel(Channel.CONFLATED)); done = true }
        advanceTimeBy(30_999)
        runCurrent()
        assertFalse(done)
        advanceTimeBy(2)
        runCurrent()
        assertTrue(done)
    }

    @Test
    fun `awaitDayBoundary returns when the clock changes`() = runTest {
        val clock = FakeClock("2026-04-21T12:00:00Z") { currentTime }
        val clockChanges = Channel<Unit>(Channel.CONFLATED)
        var done = false
        backgroundScope.launch { awaitDayBoundary(clock::now, clockChanges); done = true }
        runCurrent()
        assertFalse(done)
        clockChanges.trySend(Unit)
        runCurrent()
        assertTrue(done)
    }

    @Test
    fun `a signal sent before the wait starts is not lost`() = runTest {
        val clock = FakeClock("2026-04-21T12:00:00Z") { currentTime }
        val clockChanges = Channel<Unit>(Channel.CONFLATED)
        clockChanges.trySend(Unit)
        var done = false
        backgroundScope.launch { awaitDayBoundary(clock::now, clockChanges); done = true }
        runCurrent()
        assertTrue(done)
    }

    @Test
    fun `refreshAtDayBoundaries refreshes at natural midnight`() = runTest {
        val clock = FakeClock("2026-04-21T23:59:30Z") { currentTime }
        var refreshes = 0
        backgroundScope.launch { refreshAtDayBoundaries(clock::now, Channel(Channel.CONFLATED)) { refreshes++ } }
        advanceTimeBy(30_999)
        runCurrent()
        assertEquals(0, refreshes)
        advanceTimeBy(2)
        runCurrent()
        assertEquals(1, refreshes)
    }

    @Test
    fun `moving the clock to just before midnight refreshes and re-arms the wait`() = runTest {
        // One hour from midnight; the original delay would not end for another hour.
        val clock = FakeClock("2026-04-21T23:00:00Z") { currentTime }
        val clockChanges = Channel<Unit>(Channel.CONFLATED)
        var refreshes = 0
        backgroundScope.launch { refreshAtDayBoundaries(clock::now, clockChanges) { refreshes++ } }
        runCurrent()

        clock.clockOffsetMs = 59 * 60_000L + 40_000L // 23:59:40
        clockChanges.trySend(Unit)
        runCurrent()
        assertEquals(1, refreshes)

        advanceTimeBy(20_999) // 23:59:40 plus 20.999 s: still before 00:00:01
        runCurrent()
        assertEquals(1, refreshes)
        advanceTimeBy(2)
        runCurrent()
        assertEquals(2, refreshes)
    }
}
