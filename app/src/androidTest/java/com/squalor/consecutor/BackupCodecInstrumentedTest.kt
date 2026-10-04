package com.squalor.consecutor

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Round-trips a backup through Android's own org.json implementation. JVM unit tests use the
 * Maven org.json artifact, which differs from Android's in edge cases such as null handling.
 */
@RunWith(AndroidJUnit4::class)
class BackupCodecInstrumentedTest {
    @Test
    fun encodeOmitsDeletedEntriesOnAndroid() {
        val active = EntryEntity(trackerId = 1, effectiveDate = "2026-01-01", occurredAtEpochMs = 1,
            value = 1.0, note = "active", createdAtEpochMs = 1, updatedAtEpochMs = 1)
        val bundle = TrackerBundle(
            tracker = TrackerEntity(id = 1, name = "Water", type = TrackerType.COUNT,
                createdAtEpochMs = 1, updatedAtEpochMs = 1),
            entries = listOf(active, active.copy(note = "secret", isDeleted = true)),
            target = emptyList(), reminder = emptyList()
        )
        val json = BackupCodec.encode(listOf(bundle))
        assertTrue(!json.contains("secret"))
        val entries = org.json.JSONObject(json).getJSONArray("trackers")
            .getJSONObject(0).getJSONArray("entries")
        assertEquals(1, entries.length())
        assertEquals(false, entries.getJSONObject(0).getBoolean("isDeleted"))
        assertEquals("active", BackupCodec.decode(json).trackers.single().entries.single().note)
    }

    @Test
    fun roundtripPreservesNullsLiteralNullStringsAndValues() {
        val bundle = TrackerBundle(
            tracker = TrackerEntity(
                id = 3,
                name = "null",
                emoji = null,
                description = "null",
                type = TrackerType.MEASURE,
                unit = null,
                colorHex = "#1F6FEB",
                isArchived = true,
                createdAtEpochMs = 1,
                updatedAtEpochMs = 2
            ),
            entries = listOf(
                EntryEntity(
                    trackerId = 3, effectiveDate = "2026-04-21", occurredAtEpochMs = 3, value = 82.5,
                    note = null, createdAtEpochMs = 3, updatedAtEpochMs = 3
                ),
                EntryEntity(
                    trackerId = 3, effectiveDate = "2026-04-22", occurredAtEpochMs = 4, value = null,
                    note = "null", createdAtEpochMs = 4, updatedAtEpochMs = 4
                )
            ),
            target = emptyList(),
            reminder = listOf(
                ReminderEntity(trackerId = 3, enabled = true, hourOfDay = 7, minuteOfHour = 5, daysOfWeekCsv = null)
            )
        )

        val imported = BackupCodec.decode(BackupCodec.encode(listOf(bundle))).trackers.single()

        assertEquals("null", imported.tracker.name)
        assertNull(imported.tracker.emoji)
        assertEquals("null", imported.tracker.description)
        assertNull(imported.tracker.unit)
        assertTrue(imported.tracker.isArchived)
        assertNull(imported.target)
        assertEquals(7, imported.reminder!!.hourOfDay)
        assertNull(imported.reminder!!.daysOfWeekCsv)

        val (first, second) = imported.entries
        assertEquals(82.5, first.value!!, 0.0)
        assertNull(first.note)
        assertNull(second.value)
        assertEquals("null", second.note)
        assertEquals(false, second.isDeleted)
    }
}
