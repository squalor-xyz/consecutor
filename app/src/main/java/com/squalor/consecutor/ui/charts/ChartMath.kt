package com.squalor.consecutor.ui.charts

import com.squalor.consecutor.TrendPoint

data class ChartSummary(
    val daysMet: Int,
    val daysWithData: Int,
    val totalDays: Int,
    val average: Double?
)

internal object ChartMath {
    fun summarize(points: List<TrendPoint>): ChartSummary {
        val values = points.mapNotNull { it.value }
        return ChartSummary(
            daysMet = points.count { it.metTarget },
            daysWithData = values.size,
            totalDays = points.size,
            average = values.takeIf { it.isNotEmpty() }?.average()
        )
    }

    /** The value range to plot: padded by 1 when flat, otherwise by 10% of the span. */
    fun yRange(values: List<Double>): ClosedFloatingPointRange<Double> {
        if (values.isEmpty()) return 0.0..1.0
        val min = values.min()
        val max = values.max()
        val pad = if (min == max) 1.0 else (max - min) * 0.1
        return (min - pad)..(max + pad)
    }

    fun barHeight(value: Double, maxY: Double, chartHeightPx: Float): Float {
        if (maxY <= 0.0) return 0f
        return (value / maxY * chartHeightPx).toFloat().coerceIn(0f, chartHeightPx)
    }
}
