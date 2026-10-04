package com.squalor.consecutor

import java.io.File
import java.io.IOException

object BackupFiles {
    fun prune(dir: File, keep: Int = 3) {
        require(keep >= 0)
        if (!dir.exists()) return
        val files = dir.listFiles() ?: throw IOException("Unable to list safety backups.")
        files.filter { it.isFile && it.name.startsWith("pre-import-") && it.name.endsWith(".json") }
            .sortedByDescending { it.name }
            .drop(keep)
            .forEach { if (!it.delete()) throw IOException("Unable to remove old safety backup.") }
    }
}
