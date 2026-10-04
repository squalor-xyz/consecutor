package com.squalor.consecutor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.temporal.WeekFields

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
        assertTrue(summary.completionRate!! > 0f)
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
    @Test
    fun `yes_no weekly target of 3 is not met by three entries on one day`() {
        val today = LocalDate.of(2026, 4, 20)
        val bundle = analyticsBundle(
            TrackerType.YES_NO,
            List(3) { entry(1, today.toString()) },
            TargetPeriod.WEEKLY,
            3.0
        )

        val summary = TrackerAnalytics.toSummary(bundle, today)
        assertEquals(0, summary.currentStreak)
        assertEquals(0, summary.longestStreak)
        assertEquals(0f, summary.completionRate!!, 0f)
        assertEquals(1.0, summary.totalValue, 0.0)
    }

    @Test
    fun `yes_no two entries on one day count once in total and in the daily streak`() {
        val today = LocalDate.of(2026, 4, 21)
        val bundle = analyticsBundle(
            TrackerType.YES_NO,
            listOf(entry(1, today.toString()), entry(1, today.toString(), 8.0))
        )

        val summary = TrackerAnalytics.toSummary(bundle, today)
        assertEquals(1.0, summary.totalValue, 0.0)
        assertEquals(1, summary.currentStreak)
        assertEquals(1, summary.longestStreak)
        assertEquals(1f / 14f, summary.completionRate!!, 0f)
        // A second entry cannot satisfy a daily threshold of two either.
        val higherTarget = bundle.copy(target = listOf(bundle.target.single().copy(targetValue = 2.0)))
        assertEquals(0, TrackerAnalytics.toSummary(higherTarget, today).currentStreak)
    }

    @Test
    fun `yes_no trends count distinct active dates regardless of stored values`() {
        val today = LocalDate.of(2026, 4, 21)
        val bundle = analyticsBundle(
            TrackerType.YES_NO,
            listOf(
                entry(1, today.minusDays(2).toString()).copy(isDeleted = true),
                entry(1, today.minusDays(1).toString(), 0.0),
                entry(1, today.toString()),
                entry(1, today.toString(), 8.0)
            )
        )

        val detail = TrackerAnalytics.toDetail(bundle, today)
        assertEquals(2.0, detail.summary.totalValue, 0.0)
        assertEquals(2, detail.summary.currentStreak)
        assertEquals(listOf(0.0, 1.0, 1.0), detail.trend.takeLast(3).map { it.value })
        assertEquals(listOf(false, true, true), detail.trend.takeLast(3).map { it.metTarget })
        assertEquals(3, detail.entries.size)
    }

    @Test
    fun `entry dated after today does not extend the streak or total or last entry date`() {
        val today = LocalDate.of(2026, 4, 21)
        for (type in TrackerType.entries) {
            val bundle = analyticsBundle(
                type,
                listOf(
                    entry(1, today.minusDays(1).toString(), 1.0),
                    entry(1, today.toString(), 1.0),
                    entry(1, today.plusDays(1).toString(), 100.0)
                )
            )

            val summary = TrackerAnalytics.toSummary(bundle, today)
            assertEquals(type.name, 2.0, summary.totalValue, 0.0)
            assertEquals(type.name, today, summary.lastEntryDate)
            val expectedStreak = if (type == TrackerType.MEASURE) 0 else 2
            assertEquals(type.name, expectedStreak, summary.currentStreak)
            assertEquals(type.name, expectedStreak, summary.longestStreak)
            val detail = TrackerAnalytics.toDetail(bundle, today)
            assertEquals(summary, detail.summary)
            assertEquals(today.plusDays(1), detail.entries.first().effectiveDate)
            assertEquals(3, detail.entries.size)
            assertEquals(14, detail.trend.size)
            assertEquals(today, detail.trend.last().date)
            assertEquals(listOf(1.0, 1.0), detail.trend.takeLast(2).map { it.value })
        }
    }

    @Test
    fun `future-only entries produce empty analytics but remain in detail history`() {
        val today = LocalDate.of(2026, 4, 21)
        for (type in TrackerType.entries) {
            val future = entry(1, today.plusDays(1).toString(), 5.0)
            val bundle = analyticsBundle(type, listOf(future, future.copy(isDeleted = true)))
            val detail = TrackerAnalytics.toDetail(bundle, today)

            assertEquals(type.name, 0.0, detail.summary.totalValue, 0.0)
            assertEquals(type.name, 0, detail.summary.currentStreak)
            assertEquals(type.name, 0, detail.summary.longestStreak)
            if (type == TrackerType.MEASURE) {
                assertNull(type.name, detail.summary.completionRate)
            } else {
                assertEquals(type.name, 0f, detail.summary.completionRate!!, 0f)
            }
            assertNull(type.name, detail.summary.lastEntryDate)
            assertTrue(type.name, detail.trend.all { it.value == 0.0 && !it.metTarget })
            assertEquals(1, detail.entries.size)
            assertEquals(today.plusDays(1), detail.entries.single().effectiveDate)
        }
    }

    @Test
    fun `future entries cannot satisfy a weekly target in the current week`() {
        val today = LocalDate.of(2026, 4, 21)
        val bundle = analyticsBundle(
            TrackerType.COUNT,
            listOf(entry(1, today.toString(), 1.0), entry(1, today.plusDays(1).toString(), 1.0)),
            TargetPeriod.WEEKLY,
            2.0
        )

        val summary = TrackerAnalytics.toSummary(bundle, today)
        assertEquals(0, summary.currentStreak)
        assertEquals(0, summary.longestStreak)
        assertEquals(0f, summary.completionRate!!, 0f)
        assertEquals(1.0, summary.totalValue, 0.0)
    }

    @Test
    fun `count and measure keep existing sums and null value handling`() {
        val today = LocalDate.of(2026, 4, 21)
        for (type in listOf(TrackerType.COUNT, TrackerType.MEASURE)) {
            val bundle = analyticsBundle(
                type,
                listOf(
                    entry(1, today.minusDays(1).toString(), 2.0),
                    entry(1, today.minusDays(1).toString(), 3.0),
                    entry(1, today.toString()),
                    entry(1, today.toString(), 99.0).copy(isDeleted = true)
                )
            )

            val detail = TrackerAnalytics.toDetail(bundle, today)
            val expectedTotal = if (type == TrackerType.COUNT) 6.0 else 5.0
            assertEquals(type.name, expectedTotal, detail.summary.totalValue, 0.0)
            // Existing trends use 1.0 for a null entry, including MEASURE.
            assertEquals(listOf(5.0, 1.0), detail.trend.takeLast(2).map { it.value })
        }
    }

    @Test
    fun `weekly target uses Monday-start weeks when given WeekFields ISO`() {
        val today = LocalDate.of(2026, 4, 20)
        val bundle = analyticsBundle(
            TrackerType.COUNT,
            listOf(entry(1, "2026-04-19", 1.0), entry(1, "2026-04-20", 1.0)),
            TargetPeriod.WEEKLY,
            2.0
        )

        val summary = TrackerAnalytics.toSummary(bundle, today, weekFields = WeekFields.ISO)
        assertEquals(0, summary.currentStreak)
        assertEquals(0, summary.longestStreak)
        assertEquals(0f, summary.completionRate!!, 0f)
        assertEquals(2.0, summary.totalValue, 0.0)
        assertEquals(summary, TrackerAnalytics.toDetail(bundle, today, weekFields = WeekFields.ISO).summary)
    }

    @Test
    fun `weekly target uses Sunday-start weeks when given WeekFields SUNDAY_START`() {
        val today = LocalDate.of(2026, 4, 20)
        val bundle = analyticsBundle(
            TrackerType.COUNT,
            listOf(entry(1, "2026-04-19", 1.0), entry(1, "2026-04-20", 1.0)),
            TargetPeriod.WEEKLY,
            2.0
        )

        val summary = TrackerAnalytics.toSummary(bundle, today, weekFields = WeekFields.SUNDAY_START)
        assertEquals(1, summary.currentStreak)
        assertEquals(1, summary.longestStreak)
        assertEquals(1f / 8f, summary.completionRate!!, 0f)
        assertEquals(2.0, summary.totalValue, 0.0)
        assertEquals(summary, TrackerAnalytics.toDetail(bundle, today, weekFields = WeekFields.SUNDAY_START).summary)
    }

    @Test
    fun `todayValue sums today's active entries and ignores deleted ones`() {
        val today = LocalDate.of(2026, 4, 22)
        val bundle = analyticsBundle(
            TrackerType.COUNT,
            listOf(
                entry(1, today.toString(), 2.0),
                entry(1, today.toString(), 3.0),
                entry(1, today.toString(), 50.0).copy(isDeleted = true),
                entry(1, today.minusDays(1).toString(), 7.0)
            )
        )

        val summary = TrackerAnalytics.toSummary(bundle, today, WeekFields.ISO)
        assertEquals(5.0, summary.todayValue, 0.0)
    }

    @Test
    fun `yes_no doneToday is true with an entry today and false otherwise`() {
        val today = LocalDate.of(2026, 4, 22)
        val withToday = analyticsBundle(TrackerType.YES_NO, listOf(entry(1, today.toString())))
        val onlyYesterday = analyticsBundle(TrackerType.YES_NO, listOf(entry(1, today.minusDays(1).toString())))

        val done = TrackerAnalytics.toSummary(withToday, today, WeekFields.ISO)
        assertTrue(done.doneToday)
        assertEquals(1.0, done.todayValue, 0.0)
        assertEquals(1.0, done.periodValue, 0.0)
        assertEquals(1.0, done.periodTarget!!, 0.0)
        assertTrue(done.periodMet)

        val notDone = TrackerAnalytics.toSummary(onlyYesterday, today, WeekFields.ISO)
        assertFalse(notDone.doneToday)
        assertEquals(0.0, notDone.todayValue, 0.0)
        assertFalse(notDone.periodMet)
    }

    @Test
    fun `count with a daily target is done today only when todayValue reaches the target`() {
        val today = LocalDate.of(2026, 4, 22)
        val partial = analyticsBundle(
            TrackerType.COUNT,
            listOf(entry(1, today.toString(), 3.0)),
            TargetPeriod.DAILY,
            5.0
        )
        val partialSummary = TrackerAnalytics.toSummary(partial, today, WeekFields.ISO)
        assertFalse(partialSummary.doneToday)
        assertEquals(3.0, partialSummary.periodValue, 0.0)
        assertEquals(5.0, partialSummary.periodTarget!!, 0.0)
        assertFalse(partialSummary.periodMet)

        val full = partial.copy(entries = partial.entries + entry(1, today.toString(), 2.0))
        val fullSummary = TrackerAnalytics.toSummary(full, today, WeekFields.ISO)
        assertTrue(fullSummary.doneToday)
        assertEquals(5.0, fullSummary.periodValue, 0.0)
        assertTrue(fullSummary.periodMet)
    }

    @Test
    fun `count with a weekly target reports week sum and periodMet`() {
        val today = LocalDate.of(2026, 4, 22) // Wednesday; ISO week starts Monday 2026-04-20
        val bundle = analyticsBundle(
            TrackerType.COUNT,
            listOf(
                entry(1, "2026-04-19", 100.0), // previous week
                entry(1, "2026-04-20", 4.0),
                entry(1, "2026-04-22", 3.0)
            ),
            TargetPeriod.WEEKLY,
            10.0
        )

        val summary = TrackerAnalytics.toSummary(bundle, today, WeekFields.ISO)
        assertEquals(7.0, summary.periodValue, 0.0)
        assertEquals(10.0, summary.periodTarget!!, 0.0)
        assertFalse(summary.periodMet)
        assertEquals(3.0, summary.todayValue, 0.0)
        assertTrue(summary.doneToday) // logged today, even though the weekly target is not reached

        val met = bundle.copy(entries = bundle.entries + entry(1, "2026-04-21", 3.0))
        val metSummary = TrackerAnalytics.toSummary(met, today, WeekFields.ISO)
        assertEquals(10.0, metSummary.periodValue, 0.0)
        assertTrue(metSummary.periodMet)
    }

    @Test
    fun `yes_no with a weekly target counts distinct days in periodValue`() {
        val today = LocalDate.of(2026, 4, 22)
        val bundle = analyticsBundle(
            TrackerType.YES_NO,
            listOf(
                entry(1, "2026-04-19"), // previous week
                entry(1, "2026-04-20"),
                entry(1, "2026-04-20"), // same day twice counts once
                entry(1, "2026-04-22")
            ),
            TargetPeriod.WEEKLY,
            3.0
        )

        val summary = TrackerAnalytics.toSummary(bundle, today, WeekFields.ISO)
        assertEquals(2.0, summary.periodValue, 0.0)
        assertEquals(3.0, summary.periodTarget!!, 0.0)
        assertFalse(summary.periodMet)
        assertTrue(summary.doneToday)
        assertEquals(1.0, summary.todayValue, 0.0)
    }

    @Test
    fun `tracker without a target has null periodTarget and null completionRate`() {
        val today = LocalDate.of(2026, 4, 22)
        for (type in listOf(TrackerType.YES_NO, TrackerType.COUNT)) {
            val withEntry = noTargetBundle(type, listOf(entry(1, today.toString(), 2.0)))
            val summary = TrackerAnalytics.toSummary(withEntry, today, WeekFields.ISO)
            assertNull(type.name, summary.periodTarget)
            assertNull(type.name, summary.completionRate)
            assertFalse(type.name, summary.periodMet)
            assertTrue(type.name, summary.doneToday)
            assertEquals(type.name, summary.todayValue, summary.periodValue, 0.0)

            val empty = TrackerAnalytics.toSummary(noTargetBundle(type, emptyList()), today, WeekFields.ISO)
            assertFalse(type.name, empty.doneToday)
            assertEquals(type.name, 0.0, empty.periodValue, 0.0)
        }
    }

    @Test
    fun `measure tracker has null periodTarget and null completionRate and is done today with an entry`() {
        val today = LocalDate.of(2026, 4, 22)
        val bundle = noTargetBundle(TrackerType.MEASURE, listOf(entry(1, today.toString(), 82.5)))

        val summary = TrackerAnalytics.toSummary(bundle, today, WeekFields.ISO)
        assertNull(summary.periodTarget)
        assertNull(summary.completionRate)
        assertFalse(summary.periodMet)
        assertTrue(summary.doneToday)
        assertEquals(82.5, summary.todayValue, 0.0)

        // A MEASURE tracker never has a target in the app; stale target rows must not produce one here.
        val withStaleTarget = analyticsBundle(TrackerType.MEASURE, listOf(entry(1, today.toString(), 82.5)))
        val stale = TrackerAnalytics.toSummary(withStaleTarget, today, WeekFields.ISO)
        assertNull(stale.periodTarget)
        assertNull(stale.completionRate)
    }

    @Test
    fun `todayEntryIds lists only active entries dated today`() {
        val today = LocalDate.of(2026, 4, 22)
        val bundle = analyticsBundle(
            TrackerType.COUNT,
            listOf(
                entry(1, today.toString(), 1.0).copy(id = 11),
                entry(1, today.toString(), 1.0).copy(id = 12, isDeleted = true),
                entry(1, today.minusDays(1).toString(), 1.0).copy(id = 13),
                entry(1, today.plusDays(1).toString(), 1.0).copy(id = 14),
                entry(1, today.toString(), 1.0).copy(id = 15)
            )
        )

        val summary = TrackerAnalytics.toSummary(bundle, today, WeekFields.ISO)
        assertEquals(setOf(11L, 15L), summary.todayEntryIds.toSet())
        assertEquals(2, summary.todayEntryIds.size)
    }

    private fun noTargetBundle(type: TrackerType, entries: List<EntryEntity>): TrackerBundle =
        analyticsBundle(type, entries).copy(target = emptyList())

    private fun analyticsBundle(
        type: TrackerType,
        entries: List<EntryEntity>,
        period: TargetPeriod = TargetPeriod.DAILY,
        targetValue: Double = 1.0
    ): TrackerBundle = TrackerBundle(
        tracker = TrackerEntity(id = 1, name = "Analytics", type = type, createdAtEpochMs = 0, updatedAtEpochMs = 0),
        entries = entries,
        target = listOf(TargetEntity(trackerId = 1, period = period, targetValue = targetValue)),
        reminder = emptyList()
    )

}
