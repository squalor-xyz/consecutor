package com.squalor.consecutor

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class TrackerDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: TrackerDao
    private lateinit var repository: TrackerRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.trackerDao()
        repository = TrackerRepository(db, dao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun tracker(name: String = "Water") = TrackerEntity(
        name = name,
        type = TrackerType.COUNT,
        createdAtEpochMs = 1,
        updatedAtEpochMs = 1
    )

    private fun entry(trackerId: Long) = EntryEntity(
        trackerId = trackerId,
        effectiveDate = "2026-01-01",
        occurredAtEpochMs = 1,
        value = 1.0,
        createdAtEpochMs = 1,
        updatedAtEpochMs = 1
    )

    private fun draft() = EntryDraft(effectiveDate = LocalDate.of(2026, 1, 2), value = 2.0, note = null)

    @Test
    fun deletingATrackerCascadesToEntriesTargetsAndReminders() = runBlocking {
        val id = dao.insertTracker(tracker())
        dao.insertEntry(entry(id))
        dao.insertTarget(TargetEntity(trackerId = id, period = TargetPeriod.DAILY, targetValue = 3.0))
        dao.insertReminder(ReminderEntity(trackerId = id, enabled = true, hourOfDay = 8, minuteOfHour = 0))

        dao.deleteTracker(id)

        assertTrue(dao.getTrackerBundles().isEmpty())
        val counts = db.openHelper.readableDatabase
        for (table in listOf("entries", "targets", "reminders")) {
            counts.query("SELECT COUNT(*) FROM $table").use {
                it.moveToFirst()
                assertEquals(table, 0, it.getInt(0))
            }
        }
    }

    @Test
    fun aSecondTargetForTheSameTrackerReplacesTheFirst() = runBlocking {
        val id = dao.insertTracker(tracker())
        dao.insertTarget(TargetEntity(trackerId = id, period = TargetPeriod.DAILY, targetValue = 3.0))
        dao.insertTarget(TargetEntity(trackerId = id, period = TargetPeriod.WEEKLY, targetValue = 5.0))

        val targets = dao.getTrackerBundles().single().target
        assertEquals(1, targets.size)
        assertEquals(TargetPeriod.WEEKLY, targets.single().period)
        assertEquals(5.0, targets.single().targetValue, 0.0)
    }

    @Test
    fun aSecondReminderForTheSameTrackerReplacesTheFirst() = runBlocking {
        val id = dao.insertTracker(tracker())
        dao.insertReminder(ReminderEntity(trackerId = id, enabled = true, hourOfDay = 8, minuteOfHour = 0))
        dao.insertReminder(ReminderEntity(trackerId = id, enabled = true, hourOfDay = 20, minuteOfHour = 30))

        val reminders = dao.getTrackerBundles().single().reminder
        assertEquals(1, reminders.size)
        assertEquals(20, reminders.single().hourOfDay)
        assertEquals(30, reminders.single().minuteOfHour)
    }

    @Test
    fun getTrackerReturnsASingleRowOrNull() = runBlocking {
        val id = dao.insertTracker(tracker("Run"))
        dao.insertTracker(tracker("Read"))

        assertEquals("Run", dao.getTracker(id)?.name)
        assertNull(dao.getTracker(id + 100))
    }

    @Test
    fun updateEntryRejectsAnEntryThatBelongsToAnotherTracker() = runBlocking {
        val first = dao.insertTracker(tracker("A"))
        val second = dao.insertTracker(tracker("B"))
        val entryId = dao.insertEntry(entry(first))

        try {
            repository.updateEntry(entryId, second, TrackerType.COUNT, draft())
            throw AssertionError("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            assertNotNull(expected.message)
        }
        assertEquals("2026-01-01", dao.getEntryById(entryId)?.effectiveDate)
    }

    @Test
    fun updateTrackerAndArchiveTrackerForAnUnknownIdThrowIllegalArgumentException() = runBlocking {
        val trackerDraft = TrackerDraft(
            name = "Ghost",
            emoji = null,
            description = null,
            type = TrackerType.YES_NO,
            unit = null,
            colorHex = "#1F6FEB",
            targetPeriod = null,
            targetValue = null,
            reminderEnabled = false,
            reminderHour = 8,
            reminderMinute = 0,
            reminderDays = emptySet()
        )
        try {
            repository.updateTracker(999, trackerDraft)
            throw AssertionError("expected IllegalArgumentException from updateTracker")
        } catch (expected: IllegalArgumentException) {
        }
        try {
            repository.archiveTracker(999)
            throw AssertionError("expected IllegalArgumentException from archiveTracker")
        } catch (expected: IllegalArgumentException) {
        }
    }
}
