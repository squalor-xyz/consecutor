package com.squalor.consecutor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class BackupFilesTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `prune keeps the newest three safety backups by file name`() {
        val dir = folder.newFolder("backups")
        val names = (1..4).map { "pre-import-20261003-12000$it.json" }
        names.reversed().forEachIndexed { index, name ->
            File(dir, name).apply {
                writeText("{}")
                setLastModified(index.toLong() * 1000)
            }
        }
        val unrelated = File(dir, "export.json").apply { writeText("{}") }
        val otherExtension = File(dir, "pre-import-20261003-120005.txt").apply { writeText("text") }
        val subdirectory = File(dir, "pre-import-20261003-120006.json").apply { mkdir() }

        BackupFiles.prune(dir)

        assertFalse(File(dir, names.first()).exists())
        names.drop(1).forEach { assertTrue(File(dir, it).exists()) }
        assertTrue(unrelated.exists())
        assertTrue(otherExtension.exists())
        assertTrue(subdirectory.isDirectory)
    }

    @Test
    fun `prune supports custom retention and zero`() {
        val dir = folder.newFolder("backups")
        File(dir, "pre-import-20261003-120001.json").writeText("{}")
        File(dir, "pre-import-20261003-120002.json").writeText("{}")
        BackupFiles.prune(dir, keep = 1)
        assertEquals(listOf("pre-import-20261003-120002.json"), dir.list()?.toList())
        BackupFiles.prune(dir, keep = 0)
        assertEquals(0, dir.list()?.size)
    }

    @Test
    fun `prune on missing directory does nothing`() {
        val dir = File(folder.root, "missing")
        BackupFiles.prune(dir)
        assertFalse(dir.exists())
    }

    @Test(expected = IOException::class)
    fun `prune reports an unreadable directory`() {
        BackupFiles.prune(folder.newFile("not-a-directory"))
    }
}
