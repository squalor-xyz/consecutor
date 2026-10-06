package com.squalor.consecutor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderSchedulerTest {
    private val zone = ZoneId.of("America/New_York")

    private fun reminder(
        hour: Int,
        minute: Int,
        daysCsv: String? = null
    ): ReminderEntity = ReminderEntity(
        trackerId = 1,
        enabled = true,
        hourOfDay = hour,
        minuteOfHour = minute,
        daysOfWeekCsv = daysCsv
    )

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): ZonedDateTime =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone)

    private fun millis(dateTime: ZonedDateTime): Long = dateTime.toInstant().toEpochMilli()

    // 2026-04-21 is a Tuesday.

    @Test
    fun `daily reminder in the past schedules tomorrow at the time`() {
        val trigger = nextReminderTrigger(reminder(8, 30), at(2026, 4, 21, 10, 0))

        assertEquals(millis(at(2026, 4, 22, 8, 30)), trigger)
    }

    @Test
    fun `daily reminder in the future today schedules today`() {
        val trigger = nextReminderTrigger(reminder(20, 15), at(2026, 4, 21, 10, 0))

        assertEquals(millis(at(2026, 4, 21, 20, 15)), trigger)
    }

    @Test
    fun `weekday restricted reminder skips non-matching days`() {
        // Mon, Wed, Fri only; Tuesday now -> Wednesday.
        val trigger = nextReminderTrigger(reminder(7, 0, daysCsv = "1,3,5"), at(2026, 4, 21, 10, 0))

        assertEquals(millis(at(2026, 4, 22, 7, 0)), trigger)
    }

    @Test
    fun `weekday restricted reminder later today on an allowed day schedules today`() {
        val trigger = nextReminderTrigger(reminder(18, 0, daysCsv = "2"), at(2026, 4, 21, 10, 0))

        assertEquals(millis(at(2026, 4, 21, 18, 0)), trigger)
    }

    @Test
    fun `weekday restricted reminder already past today waits a full week`() {
        val trigger = nextReminderTrigger(reminder(7, 0, daysCsv = "2"), at(2026, 4, 21, 10, 0))

        assertEquals(millis(at(2026, 4, 28, 7, 0)), trigger)
    }

    @Test
    fun `empty days means every day`() {
        val trigger = nextReminderTrigger(reminder(9, 0, daysCsv = null), at(2026, 4, 21, 10, 0))

        assertEquals(millis(at(2026, 4, 22, 9, 0)), trigger)
    }

    @Test
    fun `trigger is strictly after now`() {
        val now = at(2026, 4, 21, 10, 0)
        val trigger = nextReminderTrigger(reminder(10, 0), now)

        assertTrue("Trigger must be strictly after now", Instant.ofEpochMilli(trigger).isAfter(now.toInstant()))
        assertEquals(millis(at(2026, 4, 22, 10, 0)), trigger)
    }

    @Test
    fun `reminder time inside DST gap resolves to a real instant on that day`() {
        // 2026-03-08 02:00-03:00 does not exist in New York; ZonedDateTime shifts 02:30 to 03:30 EDT.
        val trigger = nextReminderTrigger(reminder(2, 30), at(2026, 3, 8, 1, 0))

        assertEquals(millis(ZonedDateTime.of(2026, 3, 8, 3, 30, 0, 0, zone)), trigger)
    }

    @Test
    fun `reminder uses the zone of now rather than the system default`() {
        val tokyo = ZoneId.of("Asia/Tokyo")
        val trigger = nextReminderTrigger(reminder(9, 0), ZonedDateTime.of(2026, 4, 21, 8, 0, 0, 0, tokyo))

        assertEquals(millis(ZonedDateTime.of(2026, 4, 21, 9, 0, 0, 0, tokyo)), trigger)
    }

    @Test
    fun `yes_no gets mark done`() {
        assertEquals(ReminderAction.MARK_DONE, reminderAction(TrackerType.YES_NO))
    }

    @Test
    fun `count gets plus one`() {
        assertEquals(ReminderAction.ADD_ONE, reminderAction(TrackerType.COUNT))
    }

    @Test
    fun `measure gets no action`() {
        assertNull(reminderAction(TrackerType.MEASURE))
    }
}
