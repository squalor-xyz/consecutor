package com.squalor.consecutor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.temporal.WeekFields

class ReminderPolicyTest {
    // 2026-04-22 is a Wednesday; the ISO week starts on Monday 2026-04-20.
    private val today = LocalDate.of(2026, 4, 22)

    private fun bundle(
        type: TrackerType,
        entries: List<EntryEntity> = emptyList(),
        target: TargetEntity? = null,
        reminderEnabled: Boolean = true,
        hasReminder: Boolean = true,
        archived: Boolean = false
    ): TrackerBundle = TrackerBundle(
        tracker = TrackerEntity(
            id = 1,
            name = "Read",
            type = type,
            isArchived = archived,
            createdAtEpochMs = 0,
            updatedAtEpochMs = 0
        ),
        entries = entries,
        target = listOfNotNull(target),
        reminder = if (hasReminder) {
            listOf(ReminderEntity(trackerId = 1, enabled = reminderEnabled, hourOfDay = 20, minuteOfHour = 0, daysOfWeekCsv = null))
        } else {
            emptyList()
        }
    )

    private fun entry(date: LocalDate, value: Double? = null) = EntryEntity(
        trackerId = 1,
        effectiveDate = date.toString(),
        occurredAtEpochMs = 0,
        value = value,
        createdAtEpochMs = 0,
        updatedAtEpochMs = 0
    )

    private fun target(period: TargetPeriod, value: Double) =
        TargetEntity(trackerId = 1, period = period, targetValue = value)

    private fun notify(bundle: TrackerBundle) = shouldNotify(bundle, today, WeekFields.ISO)

    @Test
    fun `notifies when nothing is logged today`() {
        for (type in TrackerType.entries) {
            assertTrue(type.name, notify(bundle(type)))
        }
        assertTrue(notify(bundle(TrackerType.YES_NO, target = target(TargetPeriod.DAILY, 1.0))))
        assertTrue(notify(bundle(TrackerType.COUNT, target = target(TargetPeriod.WEEKLY, 3.0))))
    }

    @Test
    fun `skips a daily yes_no that is done today`() {
        val daily = target(TargetPeriod.DAILY, 1.0)
        assertFalse(notify(bundle(TrackerType.YES_NO, listOf(entry(today)), daily)))
        assertTrue(notify(bundle(TrackerType.YES_NO, listOf(entry(today.minusDays(1))), daily)))
    }

    @Test
    fun `skips a daily count whose target is met and notifies when it is not`() {
        val daily = target(TargetPeriod.DAILY, 5.0)
        assertFalse(notify(bundle(TrackerType.COUNT, listOf(entry(today, 3.0), entry(today, 2.0)), daily)))
        assertTrue(notify(bundle(TrackerType.COUNT, listOf(entry(today, 3.0)), daily)))
    }

    @Test
    fun `skips a weekly target already met this week and notifies otherwise`() {
        val weekly = target(TargetPeriod.WEEKLY, 3.0)
        // Met earlier in the week, nothing logged today: still skipped.
        val met = listOf(entry(today.minusDays(2), 2.0), entry(today.minusDays(1), 1.0))
        assertFalse(notify(bundle(TrackerType.COUNT, met, weekly)))
        // Logged today but the week's target is not reached: still notifies.
        assertTrue(notify(bundle(TrackerType.COUNT, listOf(entry(today, 1.0)), weekly)))
        // Entries from the previous week do not count.
        assertTrue(notify(bundle(TrackerType.COUNT, listOf(entry(today.minusDays(3), 5.0)), weekly)))
        // YES_NO weekly counts distinct days.
        val yesNoWeekly = target(TargetPeriod.WEEKLY, 2.0)
        assertFalse(notify(bundle(TrackerType.YES_NO, listOf(entry(today), entry(today.minusDays(1))), yesNoWeekly)))
        assertTrue(notify(bundle(TrackerType.YES_NO, listOf(entry(today), entry(today)), yesNoWeekly)))
    }

    @Test
    fun `notifies a tracker without a target only when nothing is logged today`() {
        for (type in listOf(TrackerType.YES_NO, TrackerType.COUNT)) {
            assertTrue(type.name, notify(bundle(type, listOf(entry(today.minusDays(1), 2.0)))))
            assertFalse(type.name, notify(bundle(type, listOf(entry(today, 2.0)))))
        }
    }

    @Test
    fun `measure tracker notifies until a value is logged today`() {
        assertTrue(notify(bundle(TrackerType.MEASURE, listOf(entry(today.minusDays(1), 80.0)))))
        assertFalse(notify(bundle(TrackerType.MEASURE, listOf(entry(today, 80.0)))))
    }

    @Test
    fun `skips archived trackers and trackers without an enabled reminder`() {
        assertFalse(notify(bundle(TrackerType.YES_NO, archived = true)))
        assertFalse(notify(bundle(TrackerType.YES_NO, hasReminder = false)))
        assertFalse(notify(bundle(TrackerType.YES_NO, reminderEnabled = false)))
        assertTrue(notify(bundle(TrackerType.YES_NO)))
    }
}
