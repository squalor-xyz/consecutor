package com.squalor.consecutor

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.roundToInt

object TrackerAnalytics {
    fun toDetail(bundle: TrackerBundle, today: LocalDate = LocalDate.now()): TrackerDetail {
        val summary = toSummary(bundle, today)
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

    fun toSummary(bundle: TrackerBundle, today: LocalDate = LocalDate.now()): TrackerSummary {
        val tracker = bundle.tracker
        val target = bundle.target.firstOrNull()
        val reminder = bundle.reminder.firstOrNull()
        val activeEntries = bundle.entries.filterNot { it.isDeleted }
        val aggregated = aggregateEntries(activeEntries)
        val streak = if (target == null || tracker.type == TrackerType.MEASURE) {
            StreakStats(0, 0, 0f)
        } else {
            computeStreakStats(aggregated, target, today)
        }
        val lastEntryDate = activeEntries
            .maxByOrNull { LocalDate.parse(it.effectiveDate) }
            ?.effectiveDate
            ?.let(LocalDate::parse)

        return TrackerSummary(
            id = tracker.id,
            name = tracker.name,
            emoji = tracker.emoji,
            description = tracker.description,
            type = tracker.type,
            unit = tracker.unit,
            currentStreak = streak.current,
            longestStreak = streak.longest,
            totalValue = activeEntries.sumOf { entryValue(tracker.type, it) },
            completionRate = streak.completionRate,
            lastEntryDate = lastEntryDate,
            targetLabel = target?.let { formatTarget(it, tracker.type, tracker.unit) },
            reminderLabel = reminder?.takeIf { it.enabled }?.let(::formatReminder),
            isArchived = tracker.isArchived
        )
    }

    private fun buildTrend(bundle: TrackerBundle, target: TargetEntity?, today: LocalDate): List<TrendPoint> {
        val aggregated = aggregateEntries(bundle.entries.filterNot { it.isDeleted })
        val last14Days = (13L downTo 0L).map { today.minusDays(it) }
        return last14Days.map { date ->
            val value = aggregated[date] ?: 0.0
            TrendPoint(
                date = date,
                value = value,
                metTarget = target?.let { targetMet(value, it) } ?: value > 0.0
            )
        }
    }

    private fun aggregateEntries(entries: List<EntryEntity>): Map<LocalDate, Double> {
        return entries.groupBy { LocalDate.parse(it.effectiveDate) }
            .mapValues { (_, dayEntries) ->
                dayEntries.sumOf { entry ->
                    when {
                        entry.value != null -> entry.value
                        else -> 1.0
                    }
                }
            }
    }

    private fun computeStreakStats(
        aggregatedByDate: Map<LocalDate, Double>,
        target: TargetEntity,
        today: LocalDate
    ): StreakStats {
        val periodHits: Map<PeriodKey, Boolean> = when (target.period) {
            TargetPeriod.DAILY -> aggregatedByDate
                .mapKeys { PeriodKey.Day(it.key) }
                .mapValues { targetMet(it.value, target) }
            TargetPeriod.WEEKLY -> aggregatedByDate
                .entries
                .groupBy { PeriodKey.Week(startOfWeek(it.key)) }
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
            TargetPeriod.WEEKLY -> PeriodKey.Week(startOfWeek(today))
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
        val valueLabel = if (type == TrackerType.YES_NO) "1" else formatNumber(target.targetValue)
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

    private fun startOfWeek(date: LocalDate): LocalDate {
        val fields = WeekFields.of(Locale.getDefault())
        return date.minusDays((date.get(fields.dayOfWeek()) - 1).toLong())
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

    private fun formatNumber(value: Double): String {
        val rounded = value.roundToInt().toDouble()
        return if (rounded == value) rounded.toInt().toString() else value.toString()
    }

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
