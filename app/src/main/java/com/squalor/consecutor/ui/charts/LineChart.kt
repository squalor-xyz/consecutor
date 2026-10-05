package com.squalor.consecutor.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.squalor.consecutor.R
import com.squalor.consecutor.TrendPoint
import com.squalor.consecutor.ui.formatValue

/** A line through the measured days, for MEASURE trackers. Days without a value break the line. */
@Composable
internal fun LineChart(
    points: List<TrendPoint>,
    unit: String?,
    modifier: Modifier = Modifier
) {
    val values = points.mapNotNull { it.value }
    if (values.isEmpty()) {
        Text(
            pluralStringResource(R.plurals.chart_line_empty, points.size, points.size),
            modifier = modifier,
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }

    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val textMeasurer = rememberTextMeasurer()
    val minLabel = formatValue(values.min(), unit)
    val maxLabel = formatValue(values.max(), unit)

    Canvas(modifier = modifier.fillMaxWidth().height(160.dp)) {
        val min = textMeasurer.measure(minLabel, labelStyle)
        val max = textMeasurer.measure(maxLabel, labelStyle)
        val gutter = maxOf(min.size.width, max.size.width) + 8.dp.toPx()
        val inset = 4.dp.toPx()
        val plotLeft = gutter + inset
        val plotWidth = size.width - plotLeft - inset
        val range = ChartMath.yRange(values)
        val span = range.endInclusive - range.start

        fun xAt(i: Int) = if (points.size == 1) plotLeft + plotWidth / 2 else plotLeft + plotWidth * i / (points.size - 1)
        fun yAt(v: Double) = (size.height - ((v - range.start) / span * size.height)).toFloat()

        drawLine(outline, Offset(gutter, 0f), Offset(gutter, size.height), 1.dp.toPx())

        val path = Path()
        var previousHadValue = false
        points.forEachIndexed { i, point ->
            val v = point.value
            if (v == null) {
                previousHadValue = false
            } else {
                val at = Offset(xAt(i), yAt(v))
                if (previousHadValue) path.lineTo(at.x, at.y) else path.moveTo(at.x, at.y)
                previousHadValue = true
            }
        }
        if (values.size >= 2) {
            drawPath(path, primary, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        points.forEachIndexed { i, point ->
            point.value?.let { drawCircle(primary, 3.dp.toPx(), Offset(xAt(i), yAt(it))) }
        }

        drawText(max, topLeft = Offset(0f, 0f))
        drawText(min, topLeft = Offset(0f, size.height - min.size.height))
    }
}
