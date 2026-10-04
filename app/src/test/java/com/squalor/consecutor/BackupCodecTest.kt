package com.squalor.consecutor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {

    @Test
    fun `exportedAt round-trips through encode and decode`() {
        val timestamp = 1_800_000_000_000L
        val json = BackupCodec.encode(emptyList(), timestamp)
        assertEquals(timestamp, BackupCodec.decode(json).exportedAtEpochMs)
        assertEquals(1, org.json.JSONObject(json).getInt("version"))
    }

    @Test
    fun `decode of a backup without exportedAt gives null`() {
        assertNull(BackupCodec.decode("""{"version":1,"trackers":[]}""").exportedAtEpochMs)
        assertTrue(!org.json.JSONObject(BackupCodec.encode(emptyList())).has("exportedAtEpochMs"))
    }

    @Test
    fun `decode of a backup with null exportedAt gives null`() {
        assertNull(BackupCodec.decode("""{"version":1,"trackers":[],"exportedAtEpochMs":null}""").exportedAtEpochMs)
    }

    @Test
    fun `invalid exportedAt is a format error`() {
        assertRejects("""{"version":1,"trackers":[],"exportedAtEpochMs":"invalid"}""", "missing required data")
    }

    @Test
    fun `encode then decode roundtrips tracker with target reminder and mixed entries`() {
        val now = System.currentTimeMillis()
        val bundle = TrackerBundle(
            tracker = TrackerEntity(
                id = 42,
                name = "Journal",
                emoji = "📓",
                description = "Daily writing",
                type = TrackerType.YES_NO,
                unit = null,
                colorHex = "#1F6FEB",
                isArchived = false,
                createdAtEpochMs = now - 100000,
                updatedAtEpochMs = now
            ),
            entries = listOf(
                EntryEntity(
                    id = 1,
                    trackerId = 42,
                    effectiveDate = "2026-04-20",
                    occurredAtEpochMs = now - 200000,
                    value = null,
                    note = "Good day",
                    createdAtEpochMs = now - 200000,
                    updatedAtEpochMs = now - 200000,
                    isDeleted = false
                ),
                EntryEntity(
                    id = 2,
                    trackerId = 42,
                    effectiveDate = "2026-04-21",
                    occurredAtEpochMs = now - 100000,
                    value = 1.0,
                    note = null,
                    createdAtEpochMs = now - 100000,
                    updatedAtEpochMs = now - 100000,
                    isDeleted = false
                ),
                EntryEntity(
                    id = 3,
                    trackerId = 42,
                    effectiveDate = "2026-04-22",
                    occurredAtEpochMs = now,
                    value = null,
                    note = "Missed",
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now,
                    isDeleted = true   // should survive roundtrip
                )
            ),
            target = listOf(
                TargetEntity(trackerId = 42, period = TargetPeriod.DAILY, targetValue = 1.0)
            ),
            reminder = listOf(
                ReminderEntity(
                    trackerId = 42,
                    enabled = true,
                    hourOfDay = 21,
                    minuteOfHour = 30,
                    daysOfWeekCsv = "1,2,3,4,5"
                )
            )
        )

        val json = BackupCodec.encode(listOf(bundle))
        val payload = BackupCodec.decode(json)

        assertEquals(1, payload.trackers.size)
        val imported = payload.trackers.first()

        assertEquals("Journal", imported.tracker.name)
        assertEquals("📓", imported.tracker.emoji)
        assertEquals(TrackerType.YES_NO, imported.tracker.type)
        assertEquals("#1F6FEB", imported.tracker.colorHex)
        assertEquals(false, imported.tracker.isArchived)

        assertNotNull(imported.target)
        assertEquals(TargetPeriod.DAILY, imported.target!!.period)
        assertEquals(1.0, imported.target!!.targetValue, 0.0)

        assertNotNull(imported.reminder)
        assertTrue(imported.reminder!!.enabled)
        assertEquals(21, imported.reminder!!.hourOfDay)
        assertEquals("1,2,3,4,5", imported.reminder!!.daysOfWeekCsv)

        assertEquals(3, imported.entries.size)
        val deleted = imported.entries.first { it.isDeleted }
        assertEquals("2026-04-22", deleted.effectiveDate)
        assertEquals("Missed", deleted.note)
        assertTrue(deleted.isDeleted)

        val active = imported.entries.filterNot { it.isDeleted }
        assertEquals(2, active.size)
    }

    @Test
    fun `decode rejects unsupported version`() {
        val badJson = """{"version":99,"trackers":[]}"""
        var threw = false
        try {
            BackupCodec.decode(badJson)
        } catch (e: IllegalArgumentException) {
            threw = true
            assertTrue(e.message?.contains("Unsupported backup version") == true)
        }
        assertTrue("Expected require failure for bad version", threw)
    }

    @Test
    fun `roundtrip handles measure tracker with no target or reminder and null values`() {
        val now = System.currentTimeMillis()
        val bundle = TrackerBundle(
            tracker = TrackerEntity(
                id = 7,
                name = "Weight",
                emoji = null,
                description = null,
                type = TrackerType.MEASURE,
                unit = "kg",
                colorHex = "#1F6FEB",
                isArchived = true,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            ),
            entries = listOf(
                EntryEntity(
                    id = 10,
                    trackerId = 7,
                    effectiveDate = "2026-04-01",
                    occurredAtEpochMs = now,
                    value = 82.5,
                    note = null,
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now,
                    isDeleted = false
                )
            ),
            target = emptyList(),
            reminder = emptyList()
        )

        val json = BackupCodec.encode(listOf(bundle))
        val payload = BackupCodec.decode(json)
        val t = payload.trackers.first()

        assertEquals(TrackerType.MEASURE, t.tracker.type)
        assertEquals("kg", t.tracker.unit)
        assertTrue(t.tracker.isArchived)
        assertNull(t.target)
        assertNull(t.reminder)
        assertEquals(1, t.entries.size)
        assertEquals(82.5, t.entries.first().value)
    }

    @Test
    fun `encode produces pretty json with version and trackers array`() {
        val bundle = TrackerBundle(
            tracker = TrackerEntity(
                id = 1, name = "Test", type = TrackerType.COUNT,
                createdAtEpochMs = 0, updatedAtEpochMs = 0
            ),
            entries = emptyList(),
            target = emptyList(),
            reminder = emptyList()
        )
        val json = BackupCodec.encode(listOf(bundle))
        assertTrue(json.contains("\"version\": 1"))
        assertTrue(json.contains("\"trackers\""))
        assertTrue(json.contains("Test"))
    }

    private fun backupJson(
        name: String = "Water",
        type: String = "COUNT",
        effectiveDate: String = "2026-04-21",
        hourOfDay: Int = 20,
        minuteOfHour: Int = 0,
        daysOfWeekCsv: String = "1,3,5",
        targetValue: Double = 8.0,
        entryValue: String = "2"
    ): String = """
        {"version":1,"trackers":[{
          "tracker":{"name":"$name","type":"$type","createdAtEpochMs":0,"updatedAtEpochMs":0},
          "target":{"period":"DAILY","targetValue":$targetValue},
          "reminder":{"enabled":true,"hourOfDay":$hourOfDay,"minuteOfHour":$minuteOfHour,"daysOfWeekCsv":"$daysOfWeekCsv"},
          "entries":[{"effectiveDate":"$effectiveDate","occurredAtEpochMs":0,"value":$entryValue,"createdAtEpochMs":0,"updatedAtEpochMs":0}]
        }]}
    """.trimIndent()

    private fun assertRejects(json: String, vararg messageFragments: String) {
        try {
            BackupCodec.decode(json)
        } catch (e: BackupFormatException) {
            messageFragments.forEach { fragment ->
                assertTrue("'${e.message}' should contain '$fragment'", e.message?.contains(fragment) == true)
            }
            return
        }
        throw AssertionError("Expected BackupFormatException")
    }

    @Test
    fun `valid hand-written backup decodes`() {
        val payload = BackupCodec.decode(backupJson())
        assertEquals("Water", payload.trackers.single().tracker.name)
    }

    @Test
    fun `decode rejects invalid effectiveDate`() {
        assertRejects(backupJson(effectiveDate = "2026-13-40"), "Water", "2026-13-40")
    }

    @Test
    fun `decode rejects reminder hour out of range`() {
        assertRejects(backupJson(hourOfDay = 99), "Water", "hour")
    }

    @Test
    fun `decode rejects reminder minute out of range`() {
        assertRejects(backupJson(minuteOfHour = 60), "Water", "minute")
    }

    @Test
    fun `decode rejects invalid reminder weekday`() {
        assertRejects(backupJson(daysOfWeekCsv = "1,8"), "Water", "8")
    }

    @Test
    fun `decode rejects non-positive target`() {
        assertRejects(backupJson(targetValue = 0.0), "Water", "target")
    }

    @Test
    fun `decode rejects an entry value that is not finite`() {
        // getDouble converts a JSON string to a double, so "1e999" becomes Infinity.
        assertRejects(backupJson(entryValue = "\"1e999\""), "Water", "out of range")
    }

    @Test
    fun `decode rejects a yes_no daily target other than 1`() {
        assertRejects(backupJson(type = "YES_NO", targetValue = 2.0), "Water", "yes/no")
    }

    @Test
    fun `decode rejects a target value above the maximum`() {
        assertRejects(backupJson(targetValue = 2.0E9), "Water", "out of range")
    }

    @Test
    fun `encode fails loudly on non-finite values`() {
        val bundle = TrackerBundle(
            tracker = TrackerEntity(
                id = 1, name = "T", type = TrackerType.MEASURE,
                createdAtEpochMs = 0, updatedAtEpochMs = 0
            ),
            entries = listOf(
                EntryEntity(
                    id = 1, trackerId = 1, effectiveDate = "2026-04-01", occurredAtEpochMs = 0,
                    value = Double.POSITIVE_INFINITY, createdAtEpochMs = 0, updatedAtEpochMs = 0
                )
            ),
            target = emptyList(),
            reminder = emptyList()
        )
        try {
            BackupCodec.encode(listOf(bundle))
        } catch (e: org.json.JSONException) {
            return
        }
        throw AssertionError("Expected JSONException")
    }

    @Test
    fun `decode rejects blank tracker name`() {
        assertRejects(backupJson(name = "  "), "name")
    }

    @Test
    fun `decode rejects unknown tracker type with readable message`() {
        assertRejects(backupJson(type = "STEPS"), "Water", "STEPS")
    }

    @Test
    fun `decode rejects malformed json as BackupFormatException`() {
        assertRejects("not json")
    }

    @Test
    fun `unsupported version is a BackupFormatException`() {
        assertRejects("""{"version":99,"trackers":[]}""", "Unsupported backup version")
    }

    @Test
    fun `absent optional fields decode to null`() {
        val json = """
            {"version":1,"trackers":[{
              "tracker":{"name":"Run","type":"YES_NO","createdAtEpochMs":0,"updatedAtEpochMs":0},
              "entries":[{"effectiveDate":"2026-04-21","occurredAtEpochMs":0,"createdAtEpochMs":0,"updatedAtEpochMs":0}]
            }]}
        """.trimIndent()
        val tracker = BackupCodec.decode(json).trackers.single()
        assertNull(tracker.tracker.emoji)
        assertNull(tracker.tracker.description)
        assertNull(tracker.tracker.unit)
        assertNull(tracker.target)
        assertNull(tracker.reminder)
        assertNull(tracker.entries.single().value)
        assertNull(tracker.entries.single().note)
    }

    @Test
    fun `literal string null survives roundtrip`() {
        val bundle = TrackerBundle(
            tracker = TrackerEntity(
                id = 1, name = "null", emoji = "null", description = "null", unit = "null",
                type = TrackerType.COUNT, createdAtEpochMs = 0, updatedAtEpochMs = 0
            ),
            entries = listOf(
                EntryEntity(
                    trackerId = 1, effectiveDate = "2026-04-21", occurredAtEpochMs = 0, value = 1.0,
                    note = "null", createdAtEpochMs = 0, updatedAtEpochMs = 0
                )
            ),
            target = emptyList(),
            reminder = emptyList()
        )
        val tracker = BackupCodec.decode(BackupCodec.encode(listOf(bundle))).trackers.single()
        assertEquals("null", tracker.tracker.name)
        assertEquals("null", tracker.tracker.emoji)
        assertEquals("null", tracker.tracker.description)
        assertEquals("null", tracker.tracker.unit)
        assertEquals("null", tracker.entries.single().note)
    }
}
