package com.squalor.consecutor

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.time.LocalDate

class TrackerRepository(
    private val database: AppDatabase,
    private val trackerDao: TrackerDao
) {
    fun observeDashboard(): Flow<List<TrackerSummary>> {
        return trackerDao.observeTrackerBundles().map { bundles ->
            bundles.filterNot { it.tracker.isArchived }
                .map { TrackerAnalytics.toSummary(it) }
        }
    }

    fun observeTrackerDetail(trackerId: Long): Flow<TrackerDetail?> {
        return trackerDao.observeTrackerBundle(trackerId).map { bundle ->
            bundle?.let { TrackerAnalytics.toDetail(it) }
        }
    }

    suspend fun createTracker(draft: TrackerDraft): Long {
        val now = System.currentTimeMillis()
        return database.withTransaction {
            val trackerId = trackerDao.insertTracker(
                TrackerEntity(
                    name = draft.name.trim(),
                    emoji = draft.emoji?.trim()?.ifBlank { null },
                    description = draft.description?.trim()?.ifBlank { null },
                    type = draft.type,
                    unit = draft.unit?.trim()?.ifBlank { null },
                    colorHex = draft.colorHex,
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now
                )
            )
            upsertTarget(trackerId, draft)
            upsertReminder(trackerId, draft)
            trackerId
        }
    }

    suspend fun updateTracker(trackerId: Long, draft: TrackerDraft) {
        val existing = trackerDao.getTrackerBundles().firstOrNull { it.tracker.id == trackerId }?.tracker ?: return
        val now = System.currentTimeMillis()
        database.withTransaction {
            trackerDao.updateTracker(
                existing.copy(
                    name = draft.name.trim(),
                    emoji = draft.emoji?.trim()?.ifBlank { null },
                    description = draft.description?.trim()?.ifBlank { null },
                    type = draft.type,
                    unit = draft.unit?.trim()?.ifBlank { null },
                    colorHex = draft.colorHex,
                    updatedAtEpochMs = now
                )
            )
            upsertTarget(trackerId, draft)
            upsertReminder(trackerId, draft)
        }
    }

    suspend fun archiveTracker(trackerId: Long) {
        val existing = trackerDao.getTrackerBundles().firstOrNull { it.tracker.id == trackerId }?.tracker ?: return
        trackerDao.updateTracker(existing.copy(isArchived = true, updatedAtEpochMs = System.currentTimeMillis()))
        trackerDao.deleteReminderForTracker(trackerId)
    }

    suspend fun addEntry(trackerId: Long, trackerType: TrackerType, draft: EntryDraft) {
        val now = System.currentTimeMillis()
        trackerDao.insertEntry(
            EntryEntity(
                trackerId = trackerId,
                effectiveDate = draft.effectiveDate.toString(),
                occurredAtEpochMs = now,
                value = normalizeValue(trackerType, draft.value),
                note = draft.note?.trim()?.ifBlank { null },
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
        )
        bumpTracker(trackerId, now)
    }

    suspend fun updateEntry(entryId: Long, trackerId: Long, trackerType: TrackerType, draft: EntryDraft) {
        val existing = trackerDao.getEntryById(entryId) ?: return
        val now = System.currentTimeMillis()
        trackerDao.updateEntry(
            existing.copy(
                effectiveDate = draft.effectiveDate.toString(),
                value = normalizeValue(trackerType, draft.value),
                note = draft.note?.trim()?.ifBlank { null },
                updatedAtEpochMs = now
            )
        )
        bumpTracker(trackerId, now)
    }

    suspend fun deleteEntry(entryId: Long, trackerId: Long) {
        val existing = trackerDao.getEntryById(entryId) ?: return
        val now = System.currentTimeMillis()
        trackerDao.updateEntry(existing.copy(isDeleted = true, updatedAtEpochMs = now))
        bumpTracker(trackerId, now)
    }

    suspend fun quickLog(summary: TrackerSummary) {
        if (summary.type == TrackerType.MEASURE) return
        addEntry(
            trackerId = summary.id,
            trackerType = summary.type,
            draft = EntryDraft(
                effectiveDate = LocalDate.now(),
                value = 1.0,
                note = null
            )
        )
    }

    suspend fun exportCsv(context: Context): File {
        val bundles = trackerDao.getTrackerBundles()
        val file = File(context.cacheDir, "consecutor-trackers.csv")
        file.writeText(buildCsv(bundles))
        return file
    }

    suspend fun exportBackup(context: Context): File {
        val bundles = trackerDao.getTrackerBundles()
        val file = File(context.cacheDir, "consecutor-backup.json")
        file.writeText(BackupCodec.encode(bundles))
        return file
    }

    suspend fun importBackup(rawBackup: String) {
        val payload = BackupCodec.decode(rawBackup)
        database.withTransaction {
            trackerDao.deleteAllEntries()
            trackerDao.deleteAllTargets()
            trackerDao.deleteAllReminders()
            trackerDao.deleteAllTrackers()

            payload.trackers.forEach { imported ->
                val trackerId = trackerDao.insertTracker(imported.tracker.copy(id = 0))
                imported.target?.let { target ->
                    trackerDao.insertTarget(
                        TargetEntity(
                            trackerId = trackerId,
                            period = target.period,
                            targetValue = target.targetValue
                        )
                    )
                }
                imported.reminder?.let { reminder ->
                    trackerDao.insertReminder(
                        ReminderEntity(
                            trackerId = trackerId,
                            enabled = reminder.enabled,
                            hourOfDay = reminder.hourOfDay,
                            minuteOfHour = reminder.minuteOfHour,
                            daysOfWeekCsv = reminder.daysOfWeekCsv
                        )
                    )
                }
                imported.entries.forEach { entry ->
                    trackerDao.insertEntry(
                        EntryEntity(
                            trackerId = trackerId,
                            effectiveDate = entry.effectiveDate,
                            occurredAtEpochMs = entry.occurredAtEpochMs,
                            value = entry.value,
                            note = entry.note,
                            createdAtEpochMs = entry.createdAtEpochMs,
                            updatedAtEpochMs = entry.updatedAtEpochMs,
                            isDeleted = entry.isDeleted
                        )
                    )
                }
            }
        }
    }

    suspend fun getReminderBundles(): List<TrackerBundle> = trackerDao.getTrackerBundles()

    private suspend fun upsertTarget(trackerId: Long, draft: TrackerDraft) {
        trackerDao.deleteTargetForTracker(trackerId)
        if (draft.type != TrackerType.MEASURE && draft.targetPeriod != null && draft.targetValue != null) {
            trackerDao.insertTarget(
                TargetEntity(
                    trackerId = trackerId,
                    period = draft.targetPeriod,
                    targetValue = draft.targetValue
                )
            )
        }
    }

    private suspend fun upsertReminder(trackerId: Long, draft: TrackerDraft) {
        trackerDao.deleteReminderForTracker(trackerId)
        if (draft.reminderEnabled) {
            trackerDao.insertReminder(
                ReminderEntity(
                    trackerId = trackerId,
                    enabled = true,
                    hourOfDay = draft.reminderHour,
                    minuteOfHour = draft.reminderMinute,
                    daysOfWeekCsv = draft.reminderDays.sortedBy { it.value }.joinToString(",") { it.value.toString() }
                        .ifBlank { null }
                )
            )
        }
    }

    private suspend fun bumpTracker(trackerId: Long, now: Long) {
        val tracker = trackerDao.getTrackerBundles().firstOrNull { it.tracker.id == trackerId }?.tracker ?: return
        trackerDao.updateTracker(tracker.copy(updatedAtEpochMs = now))
    }

    private fun normalizeValue(type: TrackerType, rawValue: Double?): Double? {
        return when (type) {
            TrackerType.YES_NO -> 1.0
            TrackerType.COUNT -> rawValue ?: 1.0
            TrackerType.MEASURE -> rawValue
        }
    }

    private fun buildCsv(bundles: List<TrackerBundle>): String {
        val lines = mutableListOf<String>()
        lines += "record_type,tracker_id,entry_id,name,emoji,description,type,unit,is_archived,target_period,target_value,reminder_enabled,reminder_time,reminder_days,effective_date,value,note,is_deleted"
        bundles.forEach { bundle ->
            val target = bundle.target.firstOrNull()
            val reminder = bundle.reminder.firstOrNull()
            lines += listOf(
                "tracker",
                bundle.tracker.id.toString(),
                "",
                csv(bundle.tracker.name),
                csv(bundle.tracker.emoji),
                csv(bundle.tracker.description),
                bundle.tracker.type.name,
                csv(bundle.tracker.unit),
                bundle.tracker.isArchived.toString(),
                target?.period?.name ?: "",
                target?.targetValue?.toString() ?: "",
                reminder?.enabled?.toString() ?: "false",
                reminder?.let { "%02d:%02d".format(it.hourOfDay, it.minuteOfHour) } ?: "",
                csv(reminder?.daysOfWeekCsv),
                "",
                "",
                "",
                ""
            ).joinToString(",")
            bundle.entries.forEach { entry ->
                lines += listOf(
                    "entry",
                    entry.trackerId.toString(),
                    entry.id.toString(),
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    entry.effectiveDate,
                    entry.value?.toString() ?: "",
                    csv(entry.note),
                    entry.isDeleted.toString()
                ).joinToString(",")
            }
            lines += ""
        }
        return lines.joinToString("\n")
    }

    private fun csv(value: String?): String {
        if (value == null) {
            return ""
        }
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}
