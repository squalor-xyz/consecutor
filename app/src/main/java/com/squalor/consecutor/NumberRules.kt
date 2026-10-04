package com.squalor.consecutor

import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.ParsePosition
import java.util.Locale
import kotlin.math.abs

/** Shared rules for user-entered and displayed numbers. Pure JVM so it can be unit tested without Android. */
object NumberRules {
    const val MAX_VALUE = 1_000_000_000.0

    fun isValidValue(value: Double): Boolean = value.isFinite() && abs(value) <= MAX_VALUE

    /**
     * Parses [text] in [locale] (`2,5` in de-DE, `2.5` in en-US). Returns null for blank text, trailing
     * junk, non-finite or out-of-range numbers. Grouping is off, so `1.5` in de-DE is rejected, not read as 15.
     */
    fun parseDecimal(text: String, locale: Locale = Locale.getDefault()): Double? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        val format = NumberFormat.getNumberInstance(locale).apply { isGroupingUsed = false }
        val position = ParsePosition(0)
        val number = format.parse(trimmed, position) ?: return null
        if (position.index != trimmed.length) return null
        return number.toDouble().takeIf(::isValidValue)
    }

    /** For display only: at most two decimals, no scientific notation, no grouping. */
    fun formatNumber(value: Double, locale: Locale = Locale.getDefault()): String =
        NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
            isGroupingUsed = false
        }.format(value)

    /** For prefilling text fields: full precision, no scientific notation or grouping. */
    fun formatForInput(value: Double, locale: Locale = Locale.getDefault()): String =
        BigDecimal(value.toString()).stripTrailingZeros().toPlainString()
            .replace('.', DecimalFormatSymbols.getInstance(locale).decimalSeparator)
}
