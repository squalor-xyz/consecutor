package com.squalor.consecutor

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream

class BackupInputTest {
    @Test
    fun `read preserves UTF-8 backup text`() {
        val raw = "{\"name\":\"📓 café\"}"
        assertEquals(raw, readBackupText(ByteArrayInputStream(raw.toByteArray(Charsets.UTF_8))))
    }

    @Test
    fun `read accepts exactly twenty MiB`() {
        val bytes = ByteArray(20 * 1024 * 1024) { ' '.code.toByte() }
        assertEquals(bytes.size, readBackupText(ByteArrayInputStream(bytes)).length)
    }

    @Test
    fun `read refuses more than twenty MiB`() {
        val bytes = ByteArray(20 * 1024 * 1024 + 1)
        try {
            readBackupText(ByteArrayInputStream(bytes))
        } catch (error: BackupFormatException) {
            assertEquals("That file is too large to be a backup.", error.message)
            return
        }
        throw AssertionError("Expected size rejection")
    }
}
