package com.squalor.consecutor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class NumberRulesTest {
    private val enUs = Locale.US
    private val deDe = Locale.GERMANY

    @Test
    fun `parseDecimal accepts 2_5 in en-US and 2,5 in de-DE`() {
        assertEquals(2.5, NumberRules.parseDecimal("2.5", enUs)!!, 0.0)
        assertEquals(2.5, NumberRules.parseDecimal("2,5", deDe)!!, 0.0)
        assertEquals(-5.0, NumberRules.parseDecimal(" -5 ", enUs)!!, 0.0)
    }

    @Test
    fun `parseDecimal rejects a grouping-style 1_5 in de-DE instead of reading 15`() {
        assertNull(NumberRules.parseDecimal("1.5", deDe))
    }

    @Test
    fun `parseDecimal rejects NaN Infinity 1e999 and trailing junk like 12abc`() {
        listOf("NaN", "Infinity", "-Infinity", "1e999", "12abc", "1.2.3", "abc").forEach {
            assertNull("'$it' should be rejected", NumberRules.parseDecimal(it, enUs))
        }
    }

    @Test
    fun `parseDecimal rejects blank`() {
        assertNull(NumberRules.parseDecimal("", enUs))
        assertNull(NumberRules.parseDecimal("   ", enUs))
    }

    @Test
    fun `parseDecimal rejects values beyond the maximum`() {
        assertEquals(1_000_000_000.0, NumberRules.parseDecimal("1000000000", enUs)!!, 0.0)
        assertNull(NumberRules.parseDecimal("1000000001", enUs))
    }

    @Test
    fun `isValidValue rejects values beyond 1e9 in magnitude and non-finite values`() {
        assertTrue(NumberRules.isValidValue(0.0))
        assertTrue(NumberRules.isValidValue(1e9))
        assertTrue(NumberRules.isValidValue(-1e9))
        assertFalse(NumberRules.isValidValue(1e9 + 1))
        assertFalse(NumberRules.isValidValue(-1e9 - 1))
        assertFalse(NumberRules.isValidValue(1e300))
        assertFalse(NumberRules.isValidValue(Double.NaN))
        assertFalse(NumberRules.isValidValue(Double.POSITIVE_INFINITY))
        assertFalse(NumberRules.isValidValue(Double.NEGATIVE_INFINITY))
    }

    @Test
    fun `formatNumber shows at most two decimals and never scientific notation`() {
        assertEquals("3000000000", NumberRules.formatNumber(3000000000.0, enUs))
        assertEquals("82.5", NumberRules.formatNumber(82.5, enUs))
        assertEquals("0.33", NumberRules.formatNumber(0.333333, enUs))
        assertEquals("3", NumberRules.formatNumber(3.0, enUs))
        assertEquals("82.5", NumberRules.formatNumber(82.50000001, enUs))
        assertEquals("1234567.5", NumberRules.formatNumber(1234567.5, enUs))
    }

    @Test
    fun `formatNumber uses the locale decimal separator`() {
        assertEquals("2,5", NumberRules.formatNumber(2.5, deDe))
    }

    @Test
    fun `formatForInput keeps full precision and round-trips through parseDecimal in en-US and de-DE`() {
        listOf(3.0, 2.5, 82.555, 1000000000.0, -5.25).forEach { value ->
            listOf(enUs, deDe).forEach { locale ->
                val text = NumberRules.formatForInput(value, locale)
                val parsed = NumberRules.parseDecimal(text, locale)
                assertNotNull("'$text' should parse in $locale", parsed)
                assertEquals("round trip of $value in $locale", value, parsed!!, 0.0)
            }
        }
        assertEquals("3", NumberRules.formatForInput(3.0, enUs))
        assertEquals("82.555", NumberRules.formatForInput(82.555, enUs))
        assertEquals("82,555", NumberRules.formatForInput(82.555, deDe))
        assertEquals("1000000000", NumberRules.formatForInput(1000000000.0, enUs))
    }
}
