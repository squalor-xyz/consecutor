package com.squalor.consecutor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ExportCacheTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `clear deletes every file in the exports directory and keeps the directory`() {
        val dir = folder.newFolder("exports")
        File(dir, "consecutor-backup-2026-04-21.json").writeText("{}")
        File(dir, "consecutor-entries-2026-04-21.csv").writeText("date\r\n")

        ExportCache.clear(dir)

        assertTrue(dir.isDirectory)
        assertEquals(0, dir.listFiles()?.size)
    }

    @Test
    fun `clear on a missing directory does nothing`() {
        val dir = File(folder.root, "missing")

        ExportCache.clear(dir)

        assertFalse(dir.exists())
    }
}
