package com.squalor.consecutor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
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
            assertEquals(30, detail.trend.size)
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
            val emptyValue = if (type == TrackerType.MEASURE) null else 0.0
            assertTrue(type.name, detail.trend.all { it.value == emptyValue && !it.metTarget })
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
                    entry(1, today.minusDays(1).toString(), 3.0).copy(occurredAtEpochMs = 1),
                    entry(1, today.toString()),
                    entry(1, today.toString(), 99.0).copy(isDeleted = true)
                )
            )

            val detail = TrackerAnalytics.toDetail(bundle, today)
            val expectedTotal = if (type == TrackerType.COUNT) 6.0 else 5.0
            assertEquals(type.name, expectedTotal, detail.summary.totalValue, 0.0)
            // COUNT trends sum a day and use 1.0 for a null entry. MEASURE trends take the latest entry of a day, and a null value stays null.
            val expectedTrend = if (type == TrackerType.COUNT) listOf(5.0, 1.0) else listOf(3.0, null)
            assertEquals(type.name, expectedTrend, detail.trend.takeLast(2).map { it.value })
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

    @Test
    fun `trend covers the 30 days ending today`() {
        val today = LocalDate.of(2026, 4, 21)
        val bundle = analyticsBundle(TrackerType.COUNT, listOf(entry(1, today.toString(), 2.0)))

        val trend = TrackerAnalytics.toDetail(bundle, today).trend
        assertEquals(30, trend.size)
        assertEquals(today.minusDays(29), trend.first().date)
        assertEquals(today, trend.last().date)
        assertEquals((29L downTo 0L).map { today.minusDays(it) }, trend.map { it.date })
    }

    @Test
    fun `measure trend has null for days without an entry and uses the latest entry of a day`() {
        val today = LocalDate.of(2026, 4, 21)
        val bundle = noTargetBundle(
            TrackerType.MEASURE,
            listOf(
                entry(1, today.minusDays(3).toString(), 90.0).copy(isDeleted = true),
                entry(1, today.minusDays(2).toString(), 70.0).copy(occurredAtEpochMs = 1),
                entry(1, today.minusDays(2).toString(), 72.0).copy(occurredAtEpochMs = 5),
                entry(1, today.minusDays(2).toString(), 71.0).copy(occurredAtEpochMs = 3),
                entry(1, today.toString(), 80.5)
            )
        )

        val trend = TrackerAnalytics.toDetail(bundle, today).trend
        assertEquals(listOf(null, 72.0, null, 80.5), trend.takeLast(4).map { it.value })
        assertEquals(listOf(false, true, false, true), trend.takeLast(4).map { it.metTarget })
        assertTrue(trend.take(26).all { it.value == null })
    }

    @Test
    fun `count and yes_no trend default missing days to zero`() {
        val today = LocalDate.of(2026, 4, 21)
        for (type in listOf(TrackerType.COUNT, TrackerType.YES_NO)) {
            val bundle = analyticsBundle(type, listOf(entry(1, today.toString(), 2.0)))

            val trend = TrackerAnalytics.toDetail(bundle, today).trend
            assertTrue(type.name, trend.take(29).all { it.value == 0.0 && !it.metTarget })
            assertEquals(type.name, if (type == TrackerType.COUNT) 2.0 else 1.0, trend.last().value!!, 0.0)
        }
    }

    @Test
    fun `buildMonth pads leading blanks for Monday-start and for Sunday-start weeks`() {
        val today = LocalDate.of(2026, 4, 21)
        val april = YearMonth.of(2026, 4)

        val monday = TrackerAnalytics.buildMonth(TrackerType.COUNT, null, emptyList(), april, today, WeekFields.ISO)
        val sunday = TrackerAnalytics.buildMonth(TrackerType.COUNT, null, emptyList(), april, today, WeekFields.SUNDAY_START)
        // 1 April 2026 is a Wednesday.
        assertEquals(2, monday.leadingBlanks)
        assertEquals(3, sunday.leadingBlanks)
        assertEquals(april, monday.month)
        assertEquals(30, monday.cells.size)
        assertEquals(april.atDay(1), monday.cells.first().date)
        assertEquals(april.atDay(30), monday.cells.last().date)
    }

    @Test
    fun `buildMonth marks MET PARTIAL MISSED OPEN and FUTURE days for a daily count target`() {
        val today = LocalDate.of(2026, 4, 21)
        val april = YearMonth.of(2026, 4)
        val target = TargetEntity(trackerId = 1, period = TargetPeriod.DAILY, targetValue = 3.0)
        val entries = listOf(
            item(april.atDay(2), 3.0),
            item(april.atDay(3), 1.0),
            item(april.atDay(5), 2.0),
            item(april.atDay(5), 1.0)
        )

        val grid = TrackerAnalytics.buildMonth(TrackerType.COUNT, target, entries, april, today, WeekFields.ISO)
        fun cell(day: Int) = grid.cells[day - 1]
        assertEquals(DayState.MET, cell(2).state)
        assertEquals(3.0, cell(2).value, 0.0)
        assertEquals(DayState.PARTIAL, cell(3).state)
        assertEquals(1.0, cell(3).value, 0.0)
        assertEquals(DayState.MISSED, cell(4).state)
        assertEquals(DayState.MET, cell(5).state)
        assertEquals(DayState.OPEN, cell(21).state)
        assertEquals(DayState.FUTURE, cell(22).state)
        assertEquals(DayState.FUTURE, cell(30).state)

        val partialToday = TrackerAnalytics.buildMonth(
            TrackerType.COUNT, target, entries + item(today, 1.0), april, today, WeekFields.ISO
        )
        assertEquals(DayState.PARTIAL, partialToday.cells[20].state)
    }

    @Test
    fun `buildMonth for a tracker without a target marks days with an entry as MET`() {
        val today = LocalDate.of(2026, 4, 21)
        val april = YearMonth.of(2026, 4)
        val entries = listOf(item(april.atDay(2), 1.0), item(april.atDay(3), null))

        val grid = TrackerAnalytics.buildMonth(TrackerType.COUNT, null, entries, april, today, WeekFields.ISO)
        assertEquals(DayState.MET, grid.cells[1].state)
        assertEquals(DayState.MET, grid.cells[2].state)
        assertEquals(DayState.MISSED, grid.cells[3].state)
        assertEquals(DayState.OPEN, grid.cells[20].state)
        assertEquals(DayState.FUTURE, grid.cells[21].state)

        val measure = TrackerAnalytics.buildMonth(
            TrackerType.MEASURE,
            TargetEntity(trackerId = 1, period = TargetPeriod.DAILY, targetValue = 100.0),
            listOf(item(april.atDay(2), 72.0, occurredAt = 1), item(april.atDay(2), 71.0, occurredAt = 2)),
            april, today, WeekFields.ISO
        )
        assertEquals(DayState.MET, measure.cells[1].state)
        assertEquals(71.0, measure.cells[1].value, 0.0)
        assertEquals(DayState.MISSED, measure.cells[2].state)
    }

    @Test
    fun `buildMonth ignores entries dated after today`() {
        val today = LocalDate.of(2026, 4, 21)
        val april = YearMonth.of(2026, 4)
        val entries = listOf(item(today.plusDays(1), 5.0), item(today.plusDays(4), 5.0))

        for (type in TrackerType.entries) {
            val grid = TrackerAnalytics.buildMonth(type, null, entries, april, today, WeekFields.ISO)
            assertEquals(type.name, DayState.FUTURE, grid.cells[21].state)
            assertEquals(type.name, DayState.FUTURE, grid.cells[24].state)
            assertEquals(type.name, 0.0, grid.cells[21].value, 0.0)
            assertEquals(type.name, DayState.OPEN, grid.cells[20].state)
        }
    }

    @Test
    fun `summary exposes target period and value and the enabled reminder`() {
        val bundle = analyticsBundle(TrackerType.COUNT, emptyList(), TargetPeriod.WEEKLY, 3.0).copy(
            reminder = listOf(
                ReminderEntity(trackerId = 1, enabled = true, hourOfDay = 7, minuteOfHour = 30, daysOfWeekCsv = "1,5")
            )
        )

        val summary = TrackerAnalytics.toSummary(bundle, LocalDate.of(2026, 4, 21))

        assertEquals(TargetPeriod.WEEKLY, summary.targetPeriod)
        assertEquals(3.0, summary.targetValue!!, 0.0)
        val reminder = summary.reminder!!
        assertEquals(7, reminder.hourOfDay)
        assertEquals(30, reminder.minuteOfHour)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), reminder.daysOfWeek)
    }

    @Test
    fun `summary has a null reminder when the reminder is disabled`() {
        val bundle = analyticsBundle(TrackerType.COUNT, emptyList()).copy(
            reminder = listOf(
                ReminderEntity(trackerId = 1, enabled = false, hourOfDay = 7, minuteOfHour = 30, daysOfWeekCsv = "1,5")
            )
        )

        assertNull(TrackerAnalytics.toSummary(bundle, LocalDate.of(2026, 4, 21)).reminder)
    }

    private fun item(date: LocalDate, value: Double?, occurredAt: Long = 0): EntryItem =
        EntryItem(id = 0, effectiveDate = date, occurredAtEpochMs = occurredAt, value = value, note = null)

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
