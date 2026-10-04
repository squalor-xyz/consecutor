package com.squalor.consecutor

import java.text.BreakIterator
import java.time.LocalDate
import java.util.Locale

enum class FormError {
    REQUIRED,
    TOO_LONG,
    NOT_A_NUMBER,
    MUST_BE_POSITIVE,
    OUT_OF_RANGE,
    FUTURE_DATE,
    INVALID_DATE,
    ONE_CHARACTER_ONLY,
    ALREADY_LOGGED
}

enum class TrackerField { NAME, EMOJI, DESCRIPTION, UNIT, TARGET }

enum class EntryField { DATE, VALUE, NOTE }

object EditorLimits {
    const val NAME = 60
    const val DESCRIPTION = 280
    const val UNIT = 20
    const val NOTE = 500
    const val MAX_WEEKLY_YES_NO_TARGET = 7
}

/** A YES_NO tracker counts once per day, so its target is 1 per day or a whole number of days per week. */
fun isValidYesNoTarget(period: TargetPeriod, value: Double): Boolean = when (period) {
    TargetPeriod.DAILY -> value == 1.0
    TargetPeriod.WEEKLY -> value == Math.floor(value) && value >= 1.0 && value <= EditorLimits.MAX_WEEKLY_YES_NO_TARGET
}

fun validateTrackerForm(
    name: String,
    emoji: String,
    description: String,
    unit: String,
    type: TrackerType,
    targetEnabled: Boolean,
    targetPeriod: TargetPeriod,
    targetText: String,
    locale: Locale
): Map<TrackerField, FormError> {
    val errors = linkedMapOf<TrackerField, FormError>()
    val trimmedName = name.trim()
    when {
        trimmedName.isEmpty() -> errors[TrackerField.NAME] = FormError.REQUIRED
        trimmedName.length > EditorLimits.NAME -> errors[TrackerField.NAME] = FormError.TOO_LONG
    }
    if (emoji.isNotBlank() && characterCount(emoji.trim()) != 1) {
        errors[TrackerField.EMOJI] = FormError.ONE_CHARACTER_ONLY
    }
    if (description.trim().length > EditorLimits.DESCRIPTION) errors[TrackerField.DESCRIPTION] = FormError.TOO_LONG
    if (type != TrackerType.YES_NO && unit.trim().length > EditorLimits.UNIT) {
        errors[TrackerField.UNIT] = FormError.TOO_LONG
    }
    if (targetEnabled && type != TrackerType.MEASURE) {
        targetError(type, targetPeriod, targetText, locale)?.let { errors[TrackerField.TARGET] = it }
    }
    return errors
}

fun validateEntryForm(
    type: TrackerType,
    date: LocalDate?,
    valueText: String,
    note: String,
    today: LocalDate,
    otherYesNoDates: Set<LocalDate>,
    locale: Locale
): Map<EntryField, FormError> {
    val errors = linkedMapOf<EntryField, FormError>()
    when {
        date == null -> errors[EntryField.DATE] = FormError.INVALID_DATE
        date.isAfter(today) -> errors[EntryField.DATE] = FormError.FUTURE_DATE
        type == TrackerType.YES_NO && date in otherYesNoDates -> errors[EntryField.DATE] = FormError.ALREADY_LOGGED
    }
    if (type != TrackerType.YES_NO) {
        entryValueError(type, valueText, locale)?.let { errors[EntryField.VALUE] = it }
    }
    if (note.trim().length > EditorLimits.NOTE) errors[EntryField.NOTE] = FormError.TOO_LONG
    return errors
}

/** The target value to save, or null when the form has no target. YES_NO daily targets are always 1. */
fun resolveTargetValue(
    type: TrackerType,
    targetEnabled: Boolean,
    targetPeriod: TargetPeriod,
    targetText: String,
    locale: Locale
): Double? = when {
    !targetEnabled || type == TrackerType.MEASURE -> null
    type == TrackerType.YES_NO && targetPeriod == TargetPeriod.DAILY -> 1.0
    else -> NumberRules.parseDecimal(targetText, locale)
}

private fun targetError(type: TrackerType, period: TargetPeriod, text: String, locale: Locale): FormError? {
    if (type == TrackerType.YES_NO && period == TargetPeriod.DAILY) return null
    if (text.isBlank()) return FormError.REQUIRED
    val value = NumberRules.parseDecimal(text, locale) ?: return FormError.NOT_A_NUMBER
    return if (type == TrackerType.YES_NO) {
        FormError.OUT_OF_RANGE.takeUnless { isValidYesNoTarget(period, value) }
    } else {
        FormError.MUST_BE_POSITIVE.takeIf { value <= 0.0 }
    }
}

private fun entryValueError(type: TrackerType, text: String, locale: Locale): FormError? {
    if (text.isBlank()) return FormError.REQUIRED
    val value = NumberRules.parseDecimal(text, locale) ?: return FormError.NOT_A_NUMBER
    return FormError.MUST_BE_POSITIVE.takeIf { type == TrackerType.COUNT && value <= 0.0 }
}

private fun characterCount(text: String): Int {
    val iterator = BreakIterator.getCharacterInstance()
    iterator.setText(text)
    var count = 0
    while (iterator.next() != BreakIterator.DONE) count++
    return count
}
