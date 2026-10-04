package com.squalor.consecutor

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDate

class TrackerRepository(
    private val database: AppDatabase,
    private val trackerDao: TrackerDao
) {
    companion object {
        const val PURGE_AFTER_MS = 24 * 60 * 60 * 1000L
    }

    suspend fun purgeDeletedEntries(now: Long = System.currentTimeMillis()): Int =
        trackerDao.purgeDeletedEntries(now - PURGE_AFTER_MS)

    fun observeDashboard(today: Flow<LocalDate>): Flow<List<TrackerSummary>> {
        return combine(trackerDao.observeTrackerBundles(), today) { bundles, date ->
            bundles.filterNot { it.tracker.isArchived }
                .map { TrackerAnalytics.toSummary(it, date) }
        }.flowOn(Dispatchers.Default)
    }

    fun observeTrackerDetail(trackerId: Long, today: Flow<LocalDate>): Flow<TrackerDetail?> {
        return combine(trackerDao.observeTrackerBundle(trackerId), today) { bundle, date ->
            bundle?.let { TrackerAnalytics.toDetail(it, date) }
        }.flowOn(Dispatchers.Default)
    }

    suspend fun createTracker(draft: TrackerDraft): Long {
        requireValidTarget(draft)
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
        requireValidTarget(draft)
        val existing = trackerDao.getTracker(trackerId) ?: throw IllegalArgumentException("Unknown tracker $trackerId")
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
        val existing = trackerDao.getTracker(trackerId) ?: throw IllegalArgumentException("Unknown tracker $trackerId")
        trackerDao.updateTracker(existing.copy(isArchived = true, updatedAtEpochMs = System.currentTimeMillis()))
        trackerDao.deleteReminderForTracker(trackerId)
    }

    suspend fun addEntry(trackerId: Long, trackerType: TrackerType, draft: EntryDraft): Long {
        requireValidValue(draft)
        val now = System.currentTimeMillis()
        return database.withTransaction {
            if (trackerType == TrackerType.YES_NO) {
                trackerDao.findActiveEntryId(trackerId, draft.effectiveDate.toString())?.let {
                    return@withTransaction it
                }
            }
            val entryId = trackerDao.insertEntry(
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
            entryId
        }
    }

    suspend fun updateEntry(entryId: Long, trackerId: Long, trackerType: TrackerType, draft: EntryDraft) {
        requireValidValue(draft)
        val now = System.currentTimeMillis()
        database.withTransaction {
            val existing = trackerDao.getEntryById(entryId) ?: return@withTransaction
            require(existing.trackerId == trackerId) { "Entry $entryId does not belong to tracker $trackerId" }
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
    }

    suspend fun deleteEntry(entryId: Long, trackerId: Long) {
        softDeleteEntries(trackerId, listOf(entryId))
    }

    suspend fun softDeleteEntries(trackerId: Long, ids: List<Long>) =
        setEntriesDeleted(trackerId, ids, true)

    suspend fun restoreEntries(trackerId: Long, ids: List<Long>) =
        setEntriesDeleted(trackerId, ids, false)

    private suspend fun setEntriesDeleted(trackerId: Long, ids: List<Long>, deleted: Boolean) {
        if (ids.isEmpty()) return
        database.withTransaction {
            val now = System.currentTimeMillis()
            trackerDao.setEntriesDeleted(trackerId, ids, deleted, now)
            bumpTracker(trackerId, now)
        }
    }

    sealed interface TodayToggleResult {
        data class Logged(val entryId: Long) : TodayToggleResult
        data class Cleared(val entryIds: List<Long>) : TodayToggleResult
    }

    /** Read and toggle within one transaction, independent of dashboard refresh timing. */
    suspend fun toggleToday(trackerId: Long, today: LocalDate): TodayToggleResult = database.withTransaction {
        val ids = trackerDao.findActiveEntryIds(trackerId, today.toString())
        if (ids.isEmpty()) {
            TodayToggleResult.Logged(addEntry(trackerId, TrackerType.YES_NO, EntryDraft(today, 1.0, null)))
        } else {
            softDeleteEntries(trackerId, ids)
            TodayToggleResult.Cleared(ids)
        }
    }

    suspend fun entriesCsv(): String = CsvExport.build(trackerDao.getTrackerBundles())

    suspend fun backupJson(): String = BackupCodec.encode(trackerDao.getTrackerBundles(), System.currentTimeMillis())

    suspend fun getTrackerCount(): Int = trackerDao.getTrackerCount()

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
                imported.entries.filterNot { it.isDeleted }.forEach { entry ->
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

    suspend fun getTrackerBundle(id: Long): TrackerBundle? = trackerDao.getTrackerBundle(id)

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
        val tracker = trackerDao.getTracker(trackerId) ?: return
        trackerDao.updateTracker(tracker.copy(updatedAtEpochMs = now))
    }

    private fun requireValidValue(draft: EntryDraft) {
        draft.value?.let { require(NumberRules.isValidValue(it)) { "Entry value is out of range." } }
    }

    private fun requireValidTarget(draft: TrackerDraft) {
        draft.targetValue?.let {
            require(NumberRules.isValidValue(it) && it > 0.0) { "Target must be greater than zero and within range." }
        }
        val period = draft.targetPeriod
        val value = draft.targetValue
        if (draft.type == TrackerType.YES_NO && period != null && value != null) {
            require(isValidYesNoTarget(period, value)) {
                "A yes/no target must be 1 per day or 1 to 7 per week."
            }
        }
    }

    private fun normalizeValue(type: TrackerType, rawValue: Double?): Double? {
        return when (type) {
            TrackerType.YES_NO -> 1.0
            TrackerType.COUNT -> rawValue ?: 1.0
            TrackerType.MEASURE -> rawValue
        }
    }
}
