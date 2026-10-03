package com.squalor.consecutor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TrackerAnalyticsTest {
    @Test
    fun `streak is kept the day after the last satisfied day and lost two days after`() {
        val day = LocalDate.of(2026, 4, 21)
        val bundle = TrackerBundle(
            tracker = TrackerEntity(
                id = 1,
                name = "Read",
                type = TrackerType.YES_NO,
                createdAtEpochMs = 0,
                updatedAtEpochMs = 0
            ),
            entries = listOf(entry(1, day.toString())),
            target = listOf(TargetEntity(trackerId = 1, period = TargetPeriod.DAILY, targetValue = 1.0)),
            reminder = emptyList()
        )

        assertEquals(1, TrackerAnalytics.toSummary(bundle, day).currentStreak)
        assertEquals(1, TrackerAnalytics.toSummary(bundle, day.plusDays(1)).currentStreak)
        assertEquals(0, TrackerAnalytics.toSummary(bundle, day.plusDays(2)).currentStreak)
    }

    @Test
    fun `daily streak counts contiguous satisfied days`() {
        val tracker = TrackerEntity(
            id = 1,
            name = "Read",
            type = TrackerType.YES_NO,
            createdAtEpochMs = 0,
            updatedAtEpochMs = 0
        )
        val bundle = TrackerBundle(
            tracker = tracker,
            entries = listOf(
                entry(1, "2026-04-19"),
                entry(1, "2026-04-20"),
                entry(1, "2026-04-21")
            ),
            target = listOf(TargetEntity(trackerId = 1, period = TargetPeriod.DAILY, targetValue = 1.0)),
            reminder = emptyList()
        )

        val summary = TrackerAnalytics.toSummary(bundle, LocalDate.parse("2026-04-21"))
        assertEquals(3, summary.currentStreak)
        assertEquals(3, summary.longestStreak)
        assertTrue(summary.completionRate > 0f)
    }

    @Test
    fun `weekly streak spans year boundary`() {
        val tracker = TrackerEntity(
            id = 1,
            name = "Workout",
            type = TrackerType.COUNT,
            createdAtEpochMs = 0,
            updatedAtEpochMs = 0
        )
        val bundle = TrackerBundle(
            tracker = tracker,
            entries = listOf(
                entry(1, "2025-12-29", 3.0),
                entry(1, "2026-01-05", 3.0)
            ),
            target = listOf(TargetEntity(trackerId = 1, period = TargetPeriod.WEEKLY, targetValue = 3.0)),
            reminder = emptyList()
        )

        val summary = TrackerAnalytics.toSummary(bundle, LocalDate.parse("2026-01-05"))
        assertEquals(2, summary.currentStreak)
        assertEquals(2, summary.longestStreak)
    }

    @Test
    fun `measure tracker skips streak calculations`() {
        val tracker = TrackerEntity(
            id = 1,
            name = "Weight",
            type = TrackerType.MEASURE,
            createdAtEpochMs = 0,
            updatedAtEpochMs = 0
        )
        val bundle = TrackerBundle(
            tracker = tracker,
            entries = listOf(entry(1, "2026-04-21", 182.4)),
            target = emptyList(),
            reminder = emptyList()
        )

        val summary = TrackerAnalytics.toSummary(bundle, LocalDate.parse("2026-04-21"))
        assertEquals(0, summary.currentStreak)
        assertEquals(182.4, summary.totalValue, 0.0)
    }

    private fun entry(trackerId: Long, effectiveDate: String, value: Double? = null): EntryEntity {
        return EntryEntity(
            trackerId = trackerId,
            effectiveDate = effectiveDate,
            occurredAtEpochMs = 0,
            value = value,
            createdAtEpochMs = 0,
            updatedAtEpochMs = 0
        )
    }

    @Test
    fun `deleted entries are ignored for streaks totals and trends`() {
        val tracker = TrackerEntity(id = 1, name = "Meditate", type = TrackerType.YES_NO, createdAtEpochMs = 0, updatedAtEpochMs = 0)
        val bundle = TrackerBundle(
            tracker = tracker,
            entries = listOf(
                entry(1, "2026-04-20"),
                entry(1, "2026-04-21").copy(isDeleted = true),
                entry(1, "2026-04-22")
            ),
            target = listOf(TargetEntity(trackerId = 1, period = TargetPeriod.DAILY, targetValue = 1.0)),
            reminder = emptyList()
        )
        val summary = TrackerAnalytics.toSummary(bundle, LocalDate.parse("2026-04-22"))
        assertEquals(1, summary.currentStreak) // 21 deleted, so 20 and 22 are not contiguous
        assertEquals(2.0, summary.totalValue, 0.0) // 20 and 22 count as 1.0 each for YES_NO; 21 is deleted
    }

    @Test
    fun `multiple entries on same day are aggregated for target and total`() {
        val tracker = TrackerEntity(id = 1, name = "Water", type = TrackerType.COUNT, createdAtEpochMs = 0, updatedAtEpochMs = 0)
        val bundle = TrackerBundle(
            tracker = tracker,
            entries = listOf(
                entry(1, "2026-04-21", 4.0),
                entry(1, "2026-04-21", 3.0)  // same day, should sum to 7
            ),
            target = listOf(TargetEntity(trackerId = 1, period = TargetPeriod.DAILY, targetValue = 6.0)),
            reminder = emptyList()
        )
        val summary = TrackerAnalytics.toSummary(bundle, LocalDate.parse("2026-04-21"))
        assertEquals(1, summary.currentStreak)
        assertEquals(7.0, summary.totalValue, 0.0)
    }

    @Test
    fun `streak is zero when no recent satisfied periods`() {
        val tracker = TrackerEntity(id = 1, name = "Run", type = TrackerType.YES_NO, createdAtEpochMs = 0, updatedAtEpochMs = 0)
        val bundle = TrackerBundle(
            tracker = tracker,
            entries = listOf(entry(1, "2026-04-01")),
            target = listOf(TargetEntity(trackerId = 1, period = TargetPeriod.DAILY, targetValue = 1.0)),
            reminder = emptyList()
        )
        val summary = TrackerAnalytics.toSummary(bundle, LocalDate.parse("2026-04-21"))
        assertEquals(0, summary.currentStreak)
        assertEquals(1, summary.longestStreak)
    }
}
