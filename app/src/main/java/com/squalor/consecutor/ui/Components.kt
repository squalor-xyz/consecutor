package com.squalor.consecutor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.squalor.consecutor.NumberRules
import com.squalor.consecutor.TrackerType

@Composable
internal fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, modifier = Modifier.padding(24.dp))
    }
}

internal fun formatValue(value: Double, unit: String?): String {
    return listOf(NumberRules.formatNumber(value), unit).filterNotNull().joinToString(" ")
}

internal fun formatEntryValue(type: TrackerType, value: Double?, unit: String?): String {
    return when (type) {
        TrackerType.YES_NO -> formatValue(1.0, unit)
        TrackerType.COUNT -> formatValue(value ?: 1.0, unit)
        TrackerType.MEASURE -> value?.let { formatValue(it, unit) } ?: "No value"
    }
}
