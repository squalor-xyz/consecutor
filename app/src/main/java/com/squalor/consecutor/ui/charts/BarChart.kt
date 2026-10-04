package com.squalor.consecutor.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.squalor.consecutor.TrendPoint
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** One bar per day, for COUNT and YES_NO trackers. Days that meet the target are filled. */
@Composable
internal fun BarChart(
    points: List<TrendPoint>,
    targetValue: Double?,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val textMeasurer = rememberTextMeasurer()
    val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)

    Canvas(modifier = modifier.fillMaxWidth().height(160.dp)) {
        if (points.isEmpty()) return@Canvas
        val gap = 2.dp.toPx()
        val hairline = 1.dp.toPx()
        val labelGap = 4.dp.toPx()
        val labels = listOf(points.first(), points[points.size / 2], points.last())
            .map { textMeasurer.measure(it.date.format(dateFormat), labelStyle) }
        val chartHeight = size.height - labels.maxOf { it.size.height } - labelGap
        val barWidth = (size.width - gap * (points.size - 1)) / points.size
        val maxY = maxOf(points.maxOf { it.value ?: 0.0 }, targetValue ?: 0.0, 1.0)

        points.forEachIndexed { i, point ->
            val x = i * (barWidth + gap)
            val value = point.value ?: 0.0
            val height = ChartMath.barHeight(value, maxY, chartHeight)
            when {
                point.metTarget -> drawRect(primary, Offset(x, chartHeight - height), Size(barWidth, height))
                value <= 0.0 -> drawLine(outline, Offset(x, chartHeight), Offset(x + barWidth, chartHeight), hairline)
                else -> drawRect(
                    outline,
                    Offset(x, chartHeight - height),
                    Size(barWidth, height),
                    style = Stroke(hairline)
                )
            }
        }

        if (targetValue != null) {
            val y = chartHeight - ChartMath.barHeight(targetValue, maxY, chartHeight)
            drawLine(
                outline,
                Offset(0f, y),
                Offset(size.width, y),
                hairline,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            )
        }

        val labelTop = chartHeight + labelGap
        val xs = listOf(0f, (size.width - labels[1].size.width) / 2, size.width - labels[2].size.width)
        labels.forEachIndexed { i, layout -> drawText(layout, topLeft = Offset(xs[i], labelTop)) }
    }
}
