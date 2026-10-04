package com.squalor.consecutor

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvExportTest {
    private fun tracker(
        id: Long,
        name: String = "Tracker $id",
        type: TrackerType = TrackerType.COUNT,
        unit: String? = null,
        isArchived: Boolean = false
    ) = TrackerEntity(
        id = id,
        name = name,
        type = type,
        unit = unit,
        isArchived = isArchived,
        createdAtEpochMs = 0,
        updatedAtEpochMs = 0
    )

    private fun entry(
        trackerId: Long,
        date: String = "2026-04-20",
        value: Double? = null,
        note: String? = null,
        isDeleted: Boolean = false
    ) = EntryEntity(
        trackerId = trackerId,
        effectiveDate = date,
        occurredAtEpochMs = 0,
        value = value,
        note = note,
        createdAtEpochMs = 0,
        updatedAtEpochMs = 0,
        isDeleted = isDeleted
    )

    private fun bundle(tracker: TrackerEntity, vararg entries: EntryEntity) =
        TrackerBundle(tracker, entries.toList(), emptyList(), emptyList())

    private fun rows(csv: String): List<String> = csv.split("\r\n").dropLast(1)

    @Test
    fun `header is date,tracker,type,value,unit,note`() {
        val csv = CsvExport.build(listOf(bundle(tracker(1), entry(1, value = 1.0))))
        assertEquals("date,tracker,type,value,unit,note", rows(csv).first())
    }

    @Test
    fun `one row per active entry with tracker name, type and unit`() {
        val csv = CsvExport.build(
            listOf(
                bundle(
                    tracker(1, name = "Water", unit = "ml", isArchived = true),
                    entry(1, "2026-04-20", 250.0),
                    entry(1, "2026-04-21", 500.0, isDeleted = true),
                    entry(1, "2026-04-22", 300.0, note = "ok")
                )
            )
        )
        assertEquals(
            listOf(
                "date,tracker,type,value,unit,note",
                "2026-04-20,Water,COUNT,250,ml,",
                "2026-04-22,Water,COUNT,300,ml,ok"
            ),
            rows(csv)
        )
    }

    @Test
    fun `yes_no value is 1 and count values have no trailing zeros`() {
        val csv = CsvExport.build(
            listOf(
                bundle(tracker(1, type = TrackerType.YES_NO), entry(1, value = null), entry(1, "2026-04-21", 0.0)),
                bundle(
                    tracker(2),
                    entry(2, "2026-04-20", 3.0),
                    entry(2, "2026-04-21", 2.5),
                    entry(2, "2026-04-22", 1.0E10)
                )
            )
        )
        val values = rows(csv).drop(1).map { it.split(",")[3] }
        assertEquals(listOf("1", "1", "3", "2.5", "10000000000"), values)
    }

    @Test
    fun `measure entry without a value gives an empty value cell`() {
        val csv = CsvExport.build(
            listOf(bundle(tracker(1, type = TrackerType.MEASURE, unit = "kg"), entry(1, value = null)))
        )
        assertEquals("2026-04-20,Tracker 1,MEASURE,,kg,", rows(csv)[1])
    }

    @Test
    fun `quotes cells with commas, quotes and line breaks, doubling inner quotes`() {
        val csv = CsvExport.build(
            listOf(
                bundle(
                    tracker(1, name = "A, B"),
                    entry(1, "2026-04-20", 1.0, note = "say \"hi\""),
                    entry(1, "2026-04-21", 1.0, note = "line1\nline2"),
                    entry(1, "2026-04-22", 1.0, note = "a\rb")
                )
            )
        )
        assertEquals(
            "date,tracker,type,value,unit,note\r\n" +
                "2026-04-20,\"A, B\",COUNT,1,,\"say \"\"hi\"\"\"\r\n" +
                "2026-04-21,\"A, B\",COUNT,1,,\"line1\nline2\"\r\n" +
                "2026-04-22,\"A, B\",COUNT,1,,\"a\rb\"\r\n",
            csv
        )
    }

    @Test
    fun `prefixes text cells that start with = + - @ tab or carriage return with a single quote`() {
        val csv = CsvExport.build(
            listOf(
                bundle(
                    tracker(1, name = "=1+1", unit = "+u"),
                    entry(1, "2026-04-20", 1.0, note = "-note"),
                    entry(1, "2026-04-21", 1.0, note = "@home"),
                    entry(1, "2026-04-22", 1.0, note = "\tTab"),
                    entry(1, "2026-04-23", 1.0, note = "\rCr"),
                    entry(1, "2026-04-24", 1.0, note = "=HYPERLINK(\"x\",\"y\")")
                )
            )
        )
        assertEquals(
            "date,tracker,type,value,unit,note\r\n" +
                "2026-04-20,'=1+1,COUNT,1,'+u,'-note\r\n" +
                "2026-04-21,'=1+1,COUNT,1,'+u,'@home\r\n" +
                "2026-04-22,'=1+1,COUNT,1,'+u,'\tTab\r\n" +
                "2026-04-23,'=1+1,COUNT,1,'+u,\"'\rCr\"\r\n" +
                "2026-04-24,'=1+1,COUNT,1,'+u,\"'=HYPERLINK(\"\"x\"\",\"\"y\"\")\"\r\n",
            csv
        )
    }

    @Test
    fun `does not prefix numeric value cells, so a negative measure stays -5`() {
        val csv = CsvExport.build(
            listOf(bundle(tracker(1, type = TrackerType.MEASURE), entry(1, value = -5.0)))
        )
        assertEquals("2026-04-20,Tracker 1,MEASURE,-5,,", rows(csv)[1])
    }

    @Test
    fun `rows are ordered by tracker id then date`() {
        val csv = CsvExport.build(
            listOf(
                bundle(tracker(2), entry(2, "2026-04-19", 1.0)),
                bundle(tracker(1), entry(1, "2026-04-22", 1.0), entry(1, "2026-04-20", 1.0))
            )
        )
        assertEquals(
            listOf(
                "2026-04-20,Tracker 1,COUNT,1,,",
                "2026-04-22,Tracker 1,COUNT,1,,",
                "2026-04-19,Tracker 2,COUNT,1,,"
            ),
            rows(csv).drop(1)
        )
    }

    @Test
    fun `no trackers gives the header only`() {
        assertEquals("date,tracker,type,value,unit,note\r\n", CsvExport.build(emptyList()))
    }
}
