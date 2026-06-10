package com.squalor.consecutor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TrackerAnalyticsTest {
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
}
