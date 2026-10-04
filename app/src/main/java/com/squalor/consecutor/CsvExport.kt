package com.squalor.consecutor

import java.math.BigDecimal

/** Flat, spreadsheet-friendly CSV of all active entries. Pure, so it can be unit tested without Android. */
object CsvExport {
    private const val HEADER = "date,tracker,type,value,unit,note"
    private const val LINE_END = "\r\n"
    private val FORMULA_STARTS = setOf('=', '+', '-', '@', '\t', '\r')

    fun build(bundles: List<TrackerBundle>): String {
        val out = StringBuilder(HEADER).append(LINE_END)
        bundles.sortedBy { it.tracker.id }.forEach { bundle ->
            val tracker = bundle.tracker
            bundle.entries
                .filter { !it.isDeleted }
                .sortedBy { it.effectiveDate }
                .forEach { entry ->
                    out.append(
                        listOf(
                            entry.effectiveDate,
                            textCell(tracker.name),
                            tracker.type.name,
                            valueCell(tracker.type, entry.value),
                            textCell(tracker.unit),
                            textCell(entry.note)
                        ).joinToString(",")
                    ).append(LINE_END)
                }
        }
        return out.toString()
    }

    private fun valueCell(type: TrackerType, value: Double?): String = when {
        type == TrackerType.YES_NO -> "1"
        value == null -> ""
        else -> BigDecimal(value.toString()).stripTrailingZeros().toPlainString()
    }

    // Prefix a quote so spreadsheets read formula-like text as text, then apply RFC 4180 quoting.
    private fun textCell(text: String?): String {
        if (text.isNullOrEmpty()) return ""
        val safe = if (text.first() in FORMULA_STARTS) "'$text" else text
        return if (safe.any { it == ',' || it == '"' || it == '\r' || it == '\n' }) {
            "\"" + safe.replace("\"", "\"\"") + "\""
        } else {
            safe
        }
    }
}
