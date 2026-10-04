package com.squalor.consecutor.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// The Material3 date picker reports and accepts UTC midnight millis, so never use the default zone here.
internal fun LocalDate.toPickerMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

internal fun Long.fromPickerMillis(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

internal fun isSelectable(utcMillis: Long, today: LocalDate): Boolean = !utcMillis.fromPickerMillis().isAfter(today)
