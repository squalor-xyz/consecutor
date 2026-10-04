package com.squalor.consecutor

import android.content.Context
import androidx.annotation.StringRes
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
    @StringRes val savedRes: Int,
    @StringRes val saveFailedRes: Int,
    @StringRes val exportFailedRes: Int,
    private val filePrefix: String,
    private val extension: String,
    val chooserTitle: String
) {
    CSV(
        "text/csv", R.string.event_csv_saved, R.string.event_csv_save_failed, R.string.event_csv_export_failed,
        "consecutor-entries", "csv", "Share CSV"
    ),
    BACKUP(
        "application/json", R.string.event_backup_saved, R.string.event_backup_save_failed,
        R.string.event_backup_export_failed, "consecutor-backup", "json", "Share backup"
    );

    fun fileName(date: LocalDate): String = "$filePrefix-$date.$extension"
}
