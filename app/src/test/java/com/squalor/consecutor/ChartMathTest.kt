package com.squalor.consecutor

import com.squalor.consecutor.ui.charts.ChartMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ChartMathTest {
    private val start = LocalDate.of(2026, 4, 1)

    private fun points(vararg values: Double?, met: (Double?) -> Boolean = { it != null && it >= 5.0 }) =
        values.mapIndexed { i, v -> TrendPoint(start.plusDays(i.toLong()), v, met(v)) }

    @Test
    fun `summarize counts met days and averages only non-null values`() {
        val summary = ChartMath.summarize(points(6.0, null, 2.0, 7.0, null))
        assertEquals(2, summary.daysMet)
        assertEquals(3, summary.daysWithData)
        assertEquals(5, summary.totalDays)
        assertEquals(5.0, summary.average!!, 0.0)
    }

    @Test
    fun `summarize has no average when every value is null`() {
        val summary = ChartMath.summarize(points(null, null))
        assertEquals(0, summary.daysWithData)
        assertNull(summary.average)
    }

    @Test
    fun `yRange pads the range when all values are equal`() {
        val range = ChartMath.yRange(listOf(4.0, 4.0))
        assertEquals(3.0, range.start, 0.0)
        assertEquals(5.0, range.endInclusive, 0.0)
    }

    @Test
    fun `yRange pads by ten percent of the span otherwise`() {
        val range = ChartMath.yRange(listOf(60.0, 80.0, 70.0))
        assertEquals(58.0, range.start, 1e-9)
        assertEquals(82.0, range.endInclusive, 1e-9)
    }

    @Test
    fun `barHeight is clamped to the chart height`() {
        assertEquals(50f, ChartMath.barHeight(5.0, 10.0, 100f), 0f)
        assertEquals(100f, ChartMath.barHeight(25.0, 10.0, 100f), 0f)
        assertEquals(0f, ChartMath.barHeight(-3.0, 10.0, 100f), 0f)
        assertEquals(0f, ChartMath.barHeight(3.0, 0.0, 100f), 0f)
    }
}
