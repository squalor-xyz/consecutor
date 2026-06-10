package com.squalor.consecutor

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.DayOfWeek
import java.time.LocalDate

enum class TrackerType {
    YES_NO,
    COUNT,
    MEASURE
}

enum class TargetPeriod {
    DAILY,
    WEEKLY
}

@Entity(tableName = "trackers")
data class TrackerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String? = null,
    val description: String? = null,
    val type: TrackerType,
    val unit: String? = null,
    val colorHex: String = "#1F6FEB",
    val isArchived: Boolean = false,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

@Entity(
    tableName = "entries",
    indices = [Index("trackerId")]
)
data class EntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackerId: Long,
    val effectiveDate: String,
    val occurredAtEpochMs: Long,
    val value: Double? = null,
    val note: String? = null,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "targets",
    indices = [Index("trackerId")],
)
data class TargetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackerId: Long,
    val period: TargetPeriod,
    val targetValue: Double
)

@Entity(
    tableName = "reminders",
    indices = [Index("trackerId")],
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackerId: Long,
    val enabled: Boolean,
    val hourOfDay: Int,
    val minuteOfHour: Int,
    val daysOfWeekCsv: String? = null
)

data class TrackerBundle(
    @Embedded val tracker: TrackerEntity,
    @Relation(parentColumn = "id", entityColumn = "trackerId")
    val entries: List<EntryEntity>,
    @Relation(parentColumn = "id", entityColumn = "trackerId")
    val target: List<TargetEntity>,
    @Relation(parentColumn = "id", entityColumn = "trackerId")
    val reminder: List<ReminderEntity>
)

data class TrackerDraft(
    val name: String,
    val emoji: String?,
    val description: String?,
    val type: TrackerType,
    val unit: String?,
    val colorHex: String,
    val targetPeriod: TargetPeriod?,
    val targetValue: Double?,
    val reminderEnabled: Boolean,
    val reminderHour: Int,
    val reminderMinute: Int,
    val reminderDays: Set<DayOfWeek>
)

data class EntryDraft(
    val effectiveDate: LocalDate,
    val value: Double?,
    val note: String?
)

data class ReminderConfig(
    val enabled: Boolean,
    val hourOfDay: Int,
    val minuteOfHour: Int,
    val daysOfWeek: Set<DayOfWeek>
)

data class TrackerSummary(
    val id: Long,
    val name: String,
    val emoji: String?,
    val description: String?,
    val type: TrackerType,
    val unit: String?,
    val currentStreak: Int,
    val longestStreak: Int,
    val totalValue: Double,
    val completionRate: Float,
    val lastEntryDate: LocalDate?,
    val targetLabel: String?,
    val reminderLabel: String?,
    val isArchived: Boolean
)

data class TrendPoint(
    val date: LocalDate,
    val value: Double,
    val metTarget: Boolean
)

data class EntryItem(
    val id: Long,
    val effectiveDate: LocalDate,
    val value: Double?,
    val note: String?
)

data class TrackerDetail(
    val tracker: TrackerEntity,
    val target: TargetEntity?,
    val reminder: ReminderConfig?,
    val summary: TrackerSummary,
    val entries: List<EntryItem>,
    val trend: List<TrendPoint>
)
