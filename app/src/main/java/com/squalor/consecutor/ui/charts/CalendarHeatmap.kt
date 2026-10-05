package com.squalor.consecutor.ui.charts

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick as semanticsClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.squalor.consecutor.DayCell
import com.squalor.consecutor.DayState
import com.squalor.consecutor.EntryItem
import com.squalor.consecutor.R
import com.squalor.consecutor.TargetEntity
import com.squalor.consecutor.TrackerAnalytics
import com.squalor.consecutor.TrackerType
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields

private const val DAYS_IN_WEEK = 7

/** One month of hits and misses. Tapping a day that is not in the future calls [onDayClick]. */
@Composable
internal fun CalendarHeatmap(
    type: TrackerType,
    target: TargetEntity?,
    entries: List<EntryItem>,
    today: LocalDate,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    val weekFields = remember(locale) { WeekFields.of(locale) }
    val currentMonth = YearMonth.from(today)
    val oldestMonth = entries.minOfOrNull { it.effectiveDate }
        ?.let { YearMonth.from(it) }
        ?.coerceAtMost(currentMonth)
        ?: currentMonth
    var monthText by rememberSaveable { mutableStateOf(currentMonth.toString()) }
    val month = YearMonth.parse(monthText).coerceIn(oldestMonth, currentMonth)
    val grid = TrackerAnalytics.buildMonth(type, target, entries, month, today, weekFields)
    val monthFormat = remember(locale) {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "LLLLyyyy"), locale)
    }
    val dayFormat = remember(locale) {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMMd"), locale)
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { monthText = month.minusMonths(1).toString() },
                enabled = month > oldestMonth
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.calendar_previous_month))
            }
            Text(
                month.format(monthFormat),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            IconButton(
                onClick = { monthText = month.plusMonths(1).toString() },
                enabled = month < currentMonth
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.calendar_next_month))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            (0 until DAYS_IN_WEEK).forEach { offset ->
                Text(
                    weekFields.firstDayOfWeek.plus(offset.toLong()).getDisplayName(TextStyle.NARROW, locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        val slots = List(grid.leadingBlanks) { null } + grid.cells
        slots.chunked(DAYS_IN_WEEK).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                (0 until DAYS_IN_WEEK).forEach { column ->
                    val cell = week.getOrNull(column)
                    if (cell == null) {
                        Box(modifier = Modifier.weight(1f).heightIn(min = 48.dp))
                    } else {
                        DayCellView(
                            cell = cell,
                            description = stringResource(
                                R.string.calendar_day_description,
                                cell.date.format(dayFormat),
                                stringResource(cell.state.descriptionRes())
                            ),
                            onClick = { onDayClick(cell.date) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCellView(
    cell: DayCell,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(8.dp)
    val (fill, border, textColor) = when (cell.state) {
        DayState.MET -> Triple(colors.primary, null, colors.onPrimary)
        DayState.PARTIAL -> Triple(colors.primary.copy(alpha = 0.4f), null, colors.onSurface)
        DayState.MISSED -> Triple(Color.Transparent, BorderStroke(1.dp, colors.outline), colors.onSurface)
        DayState.OPEN -> Triple(Color.Transparent, BorderStroke(2.dp, colors.primary), colors.onSurface)
        DayState.FUTURE -> Triple(Color.Transparent, null, colors.onSurface.copy(alpha = 0.38f))
    }
    val tappable = cell.state != DayState.FUTURE
    val activate = onClick
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clearAndSetSemantics {
                contentDescription = description
                if (tappable) {
                    role = Role.Button
                    semanticsClick {
                        activate()
                        true
                    }
                }
            }
            .heightIn(min = 48.dp)
            .background(fill, shape)
            .let { if (border != null) it.border(border, shape) else it }
            .let { if (tappable) it.clickable(onClick = onClick) else it }
    ) {
        Text(cell.date.dayOfMonth.toString(), color = textColor, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 8.dp))
    }
}

private fun DayState.descriptionRes(): Int = when (this) {
    DayState.MET -> R.string.calendar_state_met
    DayState.PARTIAL -> R.string.calendar_state_partial
    DayState.MISSED -> R.string.calendar_state_missed
    DayState.OPEN -> R.string.calendar_state_open
    DayState.FUTURE -> R.string.calendar_state_future
}
