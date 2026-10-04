package com.squalor.consecutor

import android.content.Context
import java.io.File
import java.time.LocalDate

object ExportCache {
    fun exportsDir(context: Context): File = File(context.cacheDir, "exports").apply { mkdirs() }

    fun clear(dir: File) {
        dir.listFiles()?.forEach { it.deleteRecursively() }
    }
}

enum class ExportKind(
    val mimeType: String,
    val label: String,
    private val filePrefix: String,
    private val extension: String,
    val chooserTitle: String
) {
    CSV("text/csv", "CSV", "consecutor-entries", "csv", "Share CSV"),
    BACKUP("application/json", "backup", "consecutor-backup", "json", "Share backup");

    fun fileName(date: LocalDate): String = "$filePrefix-$date.$extension"
}
