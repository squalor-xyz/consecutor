package com.squalor.consecutor.ui.charts

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import com.squalor.consecutor.R
import com.squalor.consecutor.ui.formatValue

/** The spoken summary of a trend chart, for its content description. */
@Composable
internal fun chartSummaryText(summary: ChartSummary, hasTarget: Boolean, unit: String?): String {
    val total = summary.totalDays
    val average = summary.average ?: return pluralStringResource(R.plurals.chart_summary_empty, total, total)
    val plural = if (hasTarget) R.plurals.chart_summary_target else R.plurals.chart_summary_entries
    return pluralStringResource(plural, total, total, summary.daysMet, formatValue(average, unit))
}
