package com.squalor.consecutor

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
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
    fun yesNoAddEntryTwiceOnOneDateReturnsTheSameIdWithoutChangingEntryOrTracker() = runBlocking {
        val trackerId = dao.insertTracker(tracker().copy(type = TrackerType.YES_NO))
        val firstId = repository.addEntry(trackerId, TrackerType.YES_NO, draft().copy(note = "First"))
        val firstEntry = dao.getEntryById(firstId)
        assertNotNull(firstEntry)
        assertEquals(1.0, firstEntry!!.value!!, 0.0)
        // A sentinel makes an accidental bump detectable without relying on clock resolution.
        val trackerAfterFirst = dao.getTracker(trackerId)!!.copy(updatedAtEpochMs = 123)
        dao.updateTracker(trackerAfterFirst)

        val secondId = repository.addEntry(trackerId, TrackerType.YES_NO, draft().copy(note = "Second"))

        assertEquals(firstId, secondId)
        assertEquals(listOf(firstEntry), dao.getTrackerBundles().single().entries)
        assertEquals(trackerAfterFirst, dao.getTracker(trackerId))
    }

    @Test
    fun countAddEntryTwiceOnOneDateInsertsTwoRowsAndReturnsTheirIds() = runBlocking {
        val trackerId = dao.insertTracker(tracker())
        val firstId = repository.addEntry(trackerId, TrackerType.COUNT, draft())
        val secondId = repository.addEntry(trackerId, TrackerType.COUNT, draft())

        assertNotEquals(firstId, secondId)
        val entries = dao.getTrackerBundles().single().entries
        assertEquals(setOf(firstId, secondId), entries.map { it.id }.toSet())
        assertEquals(2, entries.size)
        entries.forEach { assertEquals(2.0, it.value!!, 0.0) }
    }

    @Test
    fun measureAddEntryTwiceOnOneDateInsertsTwoRowsAndReturnsTheirIds() = runBlocking {
        val trackerId = dao.insertTracker(tracker().copy(type = TrackerType.MEASURE))
        val firstId = repository.addEntry(trackerId, TrackerType.MEASURE, draft())
        val secondId = repository.addEntry(trackerId, TrackerType.MEASURE, draft().copy(value = 3.0))

        assertNotEquals(firstId, secondId)
        val entries = dao.getTrackerBundles().single().entries
        assertEquals(setOf(firstId, secondId), entries.map { it.id }.toSet())
        assertEquals(2, entries.size)
        assertEquals(2.0, dao.getEntryById(firstId)!!.value!!, 0.0)
        assertEquals(3.0, dao.getEntryById(secondId)!!.value!!, 0.0)
    }

    @Test
    fun yesNoAddEntryAfterSoftDeletionInsertsANewActiveRow() = runBlocking {
        val trackerId = dao.insertTracker(tracker().copy(type = TrackerType.YES_NO))
        val deletedId = repository.addEntry(trackerId, TrackerType.YES_NO, draft())
        repository.deleteEntry(deletedId, trackerId)

        assertNull(dao.findActiveEntryId(trackerId, draft().effectiveDate.toString()))
        val newId = repository.addEntry(trackerId, TrackerType.YES_NO, draft())

        assertNotEquals(deletedId, newId)
        val entries = dao.getTrackerBundles().single().entries
        assertEquals(2, entries.size)
        assertTrue(dao.getEntryById(deletedId)!!.isDeleted)
        assertEquals(newId, entries.single { !it.isDeleted }.id)
        assertEquals(newId, dao.findActiveEntryId(trackerId, draft().effectiveDate.toString()))
    }

    @Test
    fun yesNoAddEntryKeepsDatesAndTrackersIndependent() = runBlocking {
        val firstTracker = dao.insertTracker(tracker("A").copy(type = TrackerType.YES_NO))
        val secondTracker = dao.insertTracker(tracker("B").copy(type = TrackerType.YES_NO))
        val nextDay = draft().copy(effectiveDate = draft().effectiveDate.plusDays(1))

        val firstId = repository.addEntry(firstTracker, TrackerType.YES_NO, draft())
        val nextDayId = repository.addEntry(firstTracker, TrackerType.YES_NO, nextDay)
        val secondTrackerId = repository.addEntry(secondTracker, TrackerType.YES_NO, draft())

        assertEquals(3, setOf(firstId, nextDayId, secondTrackerId).size)
        assertEquals(firstId, dao.findActiveEntryId(firstTracker, draft().effectiveDate.toString()))
        assertEquals(nextDayId, dao.findActiveEntryId(firstTracker, nextDay.effectiveDate.toString()))
        assertEquals(secondTrackerId, dao.findActiveEntryId(secondTracker, draft().effectiveDate.toString()))
        assertEquals(3, dao.getTrackerBundles().sumOf { it.entries.size })
    }

    @Test
    fun concurrentYesNoAdditionsReturnOneIdAndKeepOneActiveRow() = runBlocking {
        val trackerId = dao.insertTracker(tracker().copy(type = TrackerType.YES_NO))
        val start = CompletableDeferred<Unit>()
        val additions = List(8) {
            async(Dispatchers.Default) {
                start.await()
                repository.addEntry(trackerId, TrackerType.YES_NO, draft())
            }
        }
        start.complete(Unit)
        val ids = additions.awaitAll()

        assertEquals(1, ids.toSet().size)
        val entries = dao.getTrackerBundles().single().entries
        assertEquals(1, entries.size)
        assertEquals(ids.first(), entries.single().id)
        assertEquals(false, entries.single().isDeleted)
    }

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
