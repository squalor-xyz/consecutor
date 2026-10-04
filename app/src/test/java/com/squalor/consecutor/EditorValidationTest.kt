package com.squalor.consecutor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class EditorValidationTest {
    private val today = LocalDate.of(2026, 4, 21)

    private fun tracker(
        name: String = "Read",
        emoji: String = "",
        description: String = "",
        unit: String = "",
        type: TrackerType = TrackerType.COUNT,
        targetEnabled: Boolean = false,
        targetPeriod: TargetPeriod = TargetPeriod.DAILY,
        targetText: String = "",
        locale: Locale = Locale.US
    ): Map<TrackerField, FormError> = validateTrackerForm(
        name, emoji, description, unit, type, targetEnabled, targetPeriod, targetText, locale
    )

    private fun entry(
        type: TrackerType = TrackerType.COUNT,
        date: LocalDate? = today,
        valueText: String = "1",
        note: String = "",
        otherYesNoDates: Set<LocalDate> = emptySet(),
        locale: Locale = Locale.US
    ): Map<EntryField, FormError> = validateEntryForm(type, date, valueText, note, today, otherYesNoDates, locale)

    @Test
    fun `name is required, trimmed, and at most 60 characters`() {
        assertEquals(FormError.REQUIRED, tracker(name = "").get(TrackerField.NAME))
        assertEquals(FormError.REQUIRED, tracker(name = "   ").get(TrackerField.NAME))
        assertEquals(null, tracker(name = "a".repeat(60)).get(TrackerField.NAME))
        assertEquals(null, tracker(name = "  " + "a".repeat(60) + "  ").get(TrackerField.NAME))
        assertEquals(FormError.TOO_LONG, tracker(name = "a".repeat(61)).get(TrackerField.NAME))
        assertTrue(tracker().isEmpty())
    }

    @Test
    fun `description is at most 280 characters and unit at most 20`() {
        assertEquals(null, tracker(description = "d".repeat(280)).get(TrackerField.DESCRIPTION))
        assertEquals(FormError.TOO_LONG, tracker(description = "d".repeat(281)).get(TrackerField.DESCRIPTION))
        assertEquals(null, tracker(unit = "u".repeat(20)).get(TrackerField.UNIT))
        assertEquals(FormError.TOO_LONG, tracker(unit = "u".repeat(21)).get(TrackerField.UNIT))
        // The unit field is hidden for YES_NO, so it is not validated there.
        assertEquals(null, tracker(type = TrackerType.YES_NO, unit = "u".repeat(21)).get(TrackerField.UNIT))
    }

    @Test
    fun `emoji must be a single character`() {
        assertEquals(null, tracker(emoji = "").get(TrackerField.EMOJI))
        assertEquals(null, tracker(emoji = "😀").get(TrackerField.EMOJI))
        assertEquals(FormError.ONE_CHARACTER_ONLY, tracker(emoji = "ab").get(TrackerField.EMOJI))
    }

    @Test
    fun `target must be a positive number when enabled and is ignored when disabled or for measure`() {
        assertEquals(FormError.REQUIRED, tracker(targetEnabled = true, targetText = "").get(TrackerField.TARGET))
        assertEquals(FormError.NOT_A_NUMBER, tracker(targetEnabled = true, targetText = "abc").get(TrackerField.TARGET))
        assertEquals(FormError.NOT_A_NUMBER, tracker(targetEnabled = true, targetText = "Infinity").get(TrackerField.TARGET))
        assertEquals(FormError.MUST_BE_POSITIVE, tracker(targetEnabled = true, targetText = "0").get(TrackerField.TARGET))
        assertEquals(FormError.MUST_BE_POSITIVE, tracker(targetEnabled = true, targetText = "-2").get(TrackerField.TARGET))
        assertEquals(null, tracker(targetEnabled = true, targetText = "2.5").get(TrackerField.TARGET))
        assertEquals(null, tracker(targetEnabled = false, targetText = "abc").get(TrackerField.TARGET))
        assertEquals(
            null,
            tracker(type = TrackerType.MEASURE, targetEnabled = true, targetText = "abc").get(TrackerField.TARGET)
        )
    }

    @Test
    fun `count entry value is required and must be positive`() {
        assertEquals(FormError.REQUIRED, entry(valueText = "").get(EntryField.VALUE))
        assertEquals(FormError.NOT_A_NUMBER, entry(valueText = "abc").get(EntryField.VALUE))
        assertEquals(FormError.MUST_BE_POSITIVE, entry(valueText = "0").get(EntryField.VALUE))
        assertEquals(FormError.MUST_BE_POSITIVE, entry(valueText = "-1").get(EntryField.VALUE))
        assertEquals(null, entry(valueText = "2.5").get(EntryField.VALUE))
    }

    @Test
    fun `measure entry value is required and may be negative or zero`() {
        assertEquals(FormError.REQUIRED, entry(TrackerType.MEASURE, valueText = " ").get(EntryField.VALUE))
        assertEquals(null, entry(TrackerType.MEASURE, valueText = "0").get(EntryField.VALUE))
        assertEquals(null, entry(TrackerType.MEASURE, valueText = "-3.5").get(EntryField.VALUE))
    }

    @Test
    fun `entry value rejects NaN, Infinity and values above the maximum`() {
        for (type in listOf(TrackerType.COUNT, TrackerType.MEASURE)) {
            for (text in listOf("NaN", "Infinity", "-Infinity", "1e999", "2000000000")) {
                assertEquals("$type $text", FormError.NOT_A_NUMBER, entry(type, valueText = text).get(EntryField.VALUE))
            }
        }
    }

    @Test
    fun `entry date after today is rejected`() {
        assertEquals(null, entry(date = today).get(EntryField.DATE))
        assertEquals(FormError.FUTURE_DATE, entry(date = today.plusDays(1)).get(EntryField.DATE))
        assertEquals(FormError.INVALID_DATE, entry(date = null).get(EntryField.DATE))
    }

    @Test
    fun `yes_no entry on a date that already has another entry is rejected, and editing the same entry is allowed`() {
        val taken = setOf(today)
        assertEquals(
            FormError.ALREADY_LOGGED,
            entry(TrackerType.YES_NO, otherYesNoDates = taken).get(EntryField.DATE)
        )
        // Editing the same entry: its own date is not in the "other" set.
        assertEquals(null, entry(TrackerType.YES_NO, otherYesNoDates = emptySet()).get(EntryField.DATE))
        assertEquals(null, entry(TrackerType.YES_NO, date = today.minusDays(1), otherYesNoDates = taken).get(EntryField.DATE))
        // COUNT may log several entries on one date.
        assertEquals(null, entry(TrackerType.COUNT, otherYesNoDates = taken).get(EntryField.DATE))
        // YES_NO has no value field to validate.
        assertEquals(null, entry(TrackerType.YES_NO, valueText = "").get(EntryField.VALUE))
    }

    @Test
    fun `yes_no daily target is fixed at 1 and ignores the target text`() {
        for (text in listOf("", "abc", "0", "5")) {
            assertEquals(
                text,
                null,
                tracker(type = TrackerType.YES_NO, targetEnabled = true, targetPeriod = TargetPeriod.DAILY, targetText = text)
                    .get(TrackerField.TARGET)
            )
        }
        assertTrue(isValidYesNoTarget(TargetPeriod.DAILY, 1.0))
        assertTrue(!isValidYesNoTarget(TargetPeriod.DAILY, 2.0))
    }

    @Test
    fun `yes_no weekly target must be a whole number from 1 to 7`() {
        fun weekly(text: String) = tracker(
            type = TrackerType.YES_NO,
            targetEnabled = true,
            targetPeriod = TargetPeriod.WEEKLY,
            targetText = text
        ).get(TrackerField.TARGET)

        assertEquals(FormError.OUT_OF_RANGE, weekly("0"))
        assertEquals(FormError.OUT_OF_RANGE, weekly("8"))
        assertEquals(FormError.OUT_OF_RANGE, weekly("2.5"))
        assertEquals(null, weekly("1"))
        assertEquals(null, weekly("3"))
        assertEquals(null, weekly("7"))
        assertEquals(FormError.REQUIRED, weekly(""))
        assertEquals(FormError.NOT_A_NUMBER, weekly("abc"))
        assertTrue(isValidYesNoTarget(TargetPeriod.WEEKLY, 7.0))
        assertTrue(!isValidYesNoTarget(TargetPeriod.WEEKLY, 8.0))
        assertTrue(!isValidYesNoTarget(TargetPeriod.WEEKLY, 2.5))
    }

    @Test
    fun `note is at most 500 characters`() {
        assertEquals(null, entry(note = "n".repeat(500)).get(EntryField.NOTE))
        assertEquals(FormError.TOO_LONG, entry(note = "n".repeat(501)).get(EntryField.NOTE))
    }

    @Test
    fun `a value prefilled with formatForInput validates in de-DE`() {
        val prefilled = NumberRules.formatForInput(2.5, Locale.GERMANY)
        assertEquals("2,5", prefilled)
        assertEquals(null, entry(TrackerType.MEASURE, valueText = prefilled, locale = Locale.GERMANY).get(EntryField.VALUE))
        assertEquals(null, entry(TrackerType.COUNT, valueText = prefilled, locale = Locale.GERMANY).get(EntryField.VALUE))
        assertEquals(
            null,
            tracker(targetEnabled = true, targetText = prefilled, locale = Locale.GERMANY).get(TrackerField.TARGET)
        )
    }
}
