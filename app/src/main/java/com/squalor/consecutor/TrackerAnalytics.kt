package com.squalor.consecutor

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

object TrackerAnalytics {
    fun toDetail(
        bundle: TrackerBundle,
        today: LocalDate = LocalDate.now(),
        weekFields: WeekFields = WeekFields.of(Locale.getDefault())
    ): TrackerDetail {
        val summary = toSummary(bundle, today, weekFields)
        val target = bundle.target.firstOrNull()
        val trend = buildTrend(bundle, target, today)
        val reminder = bundle.reminder.firstOrNull()?.toConfig()
        val entries = bundle.entries
            .filterNot { it.isDeleted }
            .sortedWith(compareByDescending<EntryEntity> { LocalDate.parse(it.effectiveDate) }.thenByDescending { it.occurredAtEpochMs })
            .map {
                EntryItem(
                    id = it.id,
                    effectiveDate = LocalDate.parse(it.effectiveDate),
                    occurredAtEpochMs = it.occurredAtEpochMs,
                    value = it.value,
                    note = it.note
                )
            }

        return TrackerDetail(
            tracker = bundle.tracker,
            target = target,
            reminder = reminder,
            summary = summary,
            entries = entries,
            trend = trend
        )
    }

    fun toSummary(
        bundle: TrackerBundle,
        today: LocalDate = LocalDate.now(),
        weekFields: WeekFields = WeekFields.of(Locale.getDefault())
    ): TrackerSummary {
        val tracker = bundle.tracker
        val target = bundle.target.firstOrNull()
        val reminder = bundle.reminder.firstOrNull()
        val activeEntries = bundle.entries.filter {
            !it.isDeleted && !LocalDate.parse(it.effectiveDate).isAfter(today)
        }
        val aggregated = aggregateEntries(activeEntries, tracker.type)
        val streak = if (target == null || tracker.type == TrackerType.MEASURE) {
            StreakStats(0, 0, 0f)
        } else {
            computeStreakStats(aggregated, target, today, weekFields)
        }
        val lastEntryDate = activeEntries
            .maxByOrNull { LocalDate.parse(it.effectiveDate) }
            ?.effectiveDate
            ?.let(LocalDate::parse)
        val progress = computeProgress(activeEntries, aggregated, tracker.type, target, today, weekFields)

        return TrackerSummary(
            id = tracker.id,
            name = tracker.name,
            emoji = tracker.emoji,
            description = tracker.description,
            type = tracker.type,
            unit = tracker.unit,
            currentStreak = streak.current,
            longestStreak = streak.longest,
            totalValue = if (tracker.type == TrackerType.YES_NO) {
                aggregated.size.toDouble()
            } else {
                activeEntries.sumOf { entryValue(tracker.type, it) }
            },
            completionRate = streak.completionRate.takeIf { progress.hasTarget },
            todayValue = progress.todayValue,
            doneToday = progress.doneToday,
            todayEntryIds = progress.todayEntryIds,
            periodValue = progress.periodValue,
            periodTarget = progress.periodTarget,
            periodMet = progress.periodMet,
            targetPeriod = progress.targetPeriod,
            lastEntryDate = lastEntryDate,
            targetLabel = target?.let { formatTarget(it, tracker.type, tracker.unit) },
            reminderLabel = reminder?.takeIf { it.enabled }?.let(::formatReminder),
            isArchived = tracker.isArchived
        )
    }

    private fun computeProgress(
        activeEntries: List<EntryEntity>,
        aggregated: Map<LocalDate, Double>,
        type: TrackerType,
        target: TargetEntity?,
        today: LocalDate,
        weekFields: WeekFields
    ): TodayProgress {
        val todayEntries = activeEntries.filter { LocalDate.parse(it.effectiveDate) == today }
        val todayValue = aggregated[today] ?: 0.0
        // MEASURE trackers have no target; ignore any stale target row.
        val effectiveTarget = target.takeIf { type != TrackerType.MEASURE }
        val doneToday = when {
            type != TrackerType.COUNT -> todayEntries.isNotEmpty()
            effectiveTarget?.period == TargetPeriod.DAILY -> targetMet(todayValue, effectiveTarget)
            effectiveTarget != null -> todayEntries.isNotEmpty()
            else -> todayValue > 0.0
        }
        val periodValue = if (effectiveTarget?.period == TargetPeriod.WEEKLY) {
            val weekStart = startOfWeek(today, weekFields)
            aggregated.filterKeys { it >= weekStart }.values.sum()
        } else {
            todayValue
        }
        val periodTarget = effectiveTarget?.targetValue
        return TodayProgress(
            hasTarget = effectiveTarget != null,
            todayValue = todayValue,
            doneToday = doneToday,
            todayEntryIds = todayEntries.map { it.id },
            periodValue = periodValue,
            periodTarget = periodTarget,
            periodMet = periodTarget != null && periodValue >= periodTarget,
            targetPeriod = effectiveTarget?.period
        )
    }

    private fun buildTrend(bundle: TrackerBundle, target: TargetEntity?, today: LocalDate): List<TrendPoint> {
        val activeEntries = bundle.entries.filter {
            !it.isDeleted && !LocalDate.parse(it.effectiveDate).isAfter(today)
        }
        val type = bundle.tracker.type
        val dailyValues = dailyValues(
            activeEntries.map { DayEntry(LocalDate.parse(it.effectiveDate), it.occurredAtEpochMs, it.value) },
            type
        )
        val last30Days = (29L downTo 0L).map { today.minusDays(it) }
        return last30Days.map { date ->
            val value = if (type == TrackerType.MEASURE) dailyValues[date] else dailyValues[date] ?: 0.0
            TrendPoint(
                date = date,
                value = value,
                metTarget = value != null && (target?.let { targetMet(value, it) } ?: (value > 0.0))
            )
        }
    }

    /**
     * One month of day cells for the calendar view. Entries dated after [today] are ignored and
     * those days are [DayState.FUTURE]. Only a DAILY target on a COUNT or YES_NO tracker can give
     * [DayState.PARTIAL]; every other tracker is judged on whether the day has an entry.
     */
    fun buildMonth(
        type: TrackerType,
        target: TargetEntity?,
        entries: List<EntryItem>,
        month: YearMonth,
        today: LocalDate,
        weekFields: WeekFields
    ): MonthGrid {
        val dailyValues = dailyValues(
            entries.filter { !it.effectiveDate.isAfter(today) }
                .map { DayEntry(it.effectiveDate, it.occurredAtEpochMs, it.value) },
            type
        )
        val dailyTarget = target?.takeIf { type != TrackerType.MEASURE && it.period == TargetPeriod.DAILY }
        val cells = (1..month.lengthOfMonth()).map { dayOfMonth ->
            val date = month.atDay(dayOfMonth)
            val hasEntry = date in dailyValues
            val value = dailyValues[date] ?: 0.0
            val state = when {
                date.isAfter(today) -> DayState.FUTURE
                dailyTarget != null && targetMet(value, dailyTarget) -> DayState.MET
                dailyTarget != null && value > 0.0 -> DayState.PARTIAL
                dailyTarget == null && hasEntry -> DayState.MET
                date == today -> DayState.OPEN
                else -> DayState.MISSED
            }
            DayCell(date = date, value = value, state = state)
        }
        return MonthGrid(
            month = month,
            leadingBlanks = month.atDay(1).get(weekFields.dayOfWeek()) - 1,
            cells = cells
        )
    }

    private fun aggregateEntries(entries: List<EntryEntity>, type: TrackerType): Map<LocalDate, Double> {
        return aggregateDayValues(
            entries.map { LocalDate.parse(it.effectiveDate) to it.value },
            type
        )
    }

    private fun aggregateDayValues(entries: List<Pair<LocalDate, Double?>>, type: TrackerType): Map<LocalDate, Double> {
        return entries.groupBy({ it.first }, { it.second })
            .mapValues { (_, values) ->
                if (type == TrackerType.YES_NO) {
                    1.0
                } else {
                    values.sumOf { it ?: 1.0 }
                }
            }
    }

    /**
     * Daily values for trends and months. COUNT and YES_NO use the streak aggregation. MEASURE uses
     * the latest entry of the day by occurredAtEpochMs, and a null value stays null.
     */
    private fun dailyValues(entries: List<DayEntry>, type: TrackerType): Map<LocalDate, Double?> {
        if (type != TrackerType.MEASURE) {
            return aggregateDayValues(entries.map { it.date to it.value }, type)
        }
        return entries.groupBy { it.date }
            .mapValues { (_, dayEntries) -> dayEntries.maxByOrNull { it.occurredAtEpochMs }?.value }
    }

    private class DayEntry(val date: LocalDate, val occurredAtEpochMs: Long, val value: Double?)

    private fun computeStreakStats(
        aggregatedByDate: Map<LocalDate, Double>,
        target: TargetEntity,
        today: LocalDate,
        weekFields: WeekFields
    ): StreakStats {
        val periodHits: Map<PeriodKey, Boolean> = when (target.period) {
            TargetPeriod.DAILY -> aggregatedByDate
                .mapKeys { PeriodKey.Day(it.key) }
                .mapValues { targetMet(it.value, target) }
            TargetPeriod.WEEKLY -> aggregatedByDate
                .entries
                .groupBy { PeriodKey.Week(startOfWeek(it.key, weekFields)) }
                .mapValues { (_, entries) -> targetMet(entries.sumOf { it.value }, target) }
        }

        val satisfiedPeriods = periodHits
            .filterValues { it }
            .keys
            .sorted()

        if (satisfiedPeriods.isEmpty()) {
            return StreakStats(0, 0, 0f)
        }

        val currentPeriod = when (target.period) {
            TargetPeriod.DAILY -> PeriodKey.Day(today)
            TargetPeriod.WEEKLY -> PeriodKey.Week(startOfWeek(today, weekFields))
        }
        val lastSatisfied = satisfiedPeriods.last()
        val current = if (currentPeriod.distanceFrom(lastSatisfied) > 1) {
            0
        } else {
            countBackwardRun(satisfiedPeriods)
        }

        var longest = 1
        var running = 1
        for (index in 1 until satisfiedPeriods.size) {
            if (satisfiedPeriods[index].distanceFrom(satisfiedPeriods[index - 1]) == 1L) {
                running += 1
                longest = maxOf(longest, running)
            } else {
                running = 1
            }
        }

        val window = when (target.period) {
            TargetPeriod.DAILY -> 14
            TargetPeriod.WEEKLY -> 8
        }
        val recentCompleted = (0 until window).count { offset ->
            val key = currentPeriod.minus(offset.toLong())
            periodHits[key] == true
        }
        return StreakStats(
            current = current,
            longest = longest,
            completionRate = recentCompleted.toFloat() / window.toFloat()
        )
    }

    private fun countBackwardRun(sortedPeriods: List<PeriodKey>): Int {
        var count = 1
        for (index in sortedPeriods.lastIndex downTo 1) {
            if (sortedPeriods[index].distanceFrom(sortedPeriods[index - 1]) == 1L) {
                count += 1
            } else {
                break
            }
        }
        return count
    }

    private fun targetMet(value: Double, target: TargetEntity): Boolean = value >= target.targetValue

    private fun formatTarget(target: TargetEntity, type: TrackerType, unit: String?): String {
        val valueLabel = if (type == TrackerType.YES_NO) "1" else NumberRules.formatNumber(target.targetValue)
        val suffix = when (target.period) {
            TargetPeriod.DAILY -> "day"
            TargetPeriod.WEEKLY -> "week"
        }
        val unitLabel = unit?.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""
        return "$valueLabel$unitLabel per $suffix"
    }

    private fun formatReminder(reminder: ReminderEntity): String {
        val timeLabel = "%02d:%02d".format(reminder.hourOfDay, reminder.minuteOfHour)
        val days = reminder.toConfig().daysOfWeek
        if (days.isEmpty() || days.size == 7) {
            return "Daily at $timeLabel"
        }
        val label = days.sortedBy { it.value }.joinToString(", ") { it.name.take(3).lowercase().replaceFirstChar(Char::titlecase) }
        return "$label at $timeLabel"
    }

    fun entryValue(type: TrackerType, entry: EntryEntity): Double {
        return when (type) {
            TrackerType.YES_NO -> 1.0
            TrackerType.COUNT -> entry.value ?: 1.0
            TrackerType.MEASURE -> entry.value ?: 0.0
        }
    }

    private fun startOfWeek(date: LocalDate, weekFields: WeekFields): LocalDate {
        return date.minusDays((date.get(weekFields.dayOfWeek()) - 1).toLong())
    }

    fun ReminderEntity.toConfig(): ReminderConfig {
        val days = daysOfWeekCsv
            ?.split(",")
            ?.mapNotNull { token -> token.toIntOrNull()?.let(DayOfWeek::of) }
            ?.toSet()
            ?: emptySet()
        return ReminderConfig(
            enabled = enabled,
            hourOfDay = hourOfDay,
            minuteOfHour = minuteOfHour,
            daysOfWeek = days
        )
    }

    private data class TodayProgress(
        val hasTarget: Boolean,
        val todayValue: Double,
        val doneToday: Boolean,
        val todayEntryIds: List<Long>,
        val periodValue: Double,
        val periodTarget: Double?,
        val periodMet: Boolean,
        val targetPeriod: TargetPeriod?
    )

    data class StreakStats(
        val current: Int,
        val longest: Int,
        val completionRate: Float
    )

    sealed interface PeriodKey : Comparable<PeriodKey> {
        fun distanceFrom(other: PeriodKey): Long
        fun minus(offset: Long): PeriodKey

        data class Day(val date: LocalDate) : PeriodKey {
            override fun distanceFrom(other: PeriodKey): Long {
                other as Day
                return ChronoUnit.DAYS.between(other.date, date)
            }

            override fun minus(offset: Long): PeriodKey = Day(date.minusDays(offset))

            override fun compareTo(other: PeriodKey): Int {
                other as Day
                return date.compareTo(other.date)
            }
        }

        data class Week(val startDate: LocalDate) : PeriodKey {
            override fun distanceFrom(other: PeriodKey): Long {
                other as Week
                return ChronoUnit.WEEKS.between(other.startDate, startDate)
            }

            override fun minus(offset: Long): PeriodKey = Week(startDate.minusWeeks(offset))

            override fun compareTo(other: PeriodKey): Int {
                other as Week
                return startDate.compareTo(other.startDate)
            }
        }
    }
}
