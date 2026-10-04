package com.squalor.consecutor

import com.squalor.consecutor.ui.fromPickerMillis
import com.squalor.consecutor.ui.isSelectable
import com.squalor.consecutor.ui.toPickerMillis
import java.time.LocalDate
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PickerConversionsTest {
    private val dates = listOf(
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 2, 28),
        LocalDate.of(2024, 2, 29),
        LocalDate.of(2026, 12, 31),
        LocalDate.of(1999, 6, 15)
    )

    @Test
    fun `localDate to utc millis and back is identity in any default timezone`() {
        val original = TimeZone.getDefault()
        try {
            listOf("Pacific/Kiritimati", "Pacific/Pago_Pago").forEach { zone ->
                TimeZone.setDefault(TimeZone.getTimeZone(zone))
                dates.forEach { date ->
                    assertEquals("$date in $zone", date, date.toPickerMillis().fromPickerMillis())
                }
            }
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test
    fun `isSelectable allows today and earlier and rejects tomorrow`() {
        val today = LocalDate.of(2026, 10, 4)
        assertTrue(isSelectable(today.toPickerMillis(), today))
        assertTrue(isSelectable(today.minusDays(1).toPickerMillis(), today))
        assertFalse(isSelectable(today.plusDays(1).toPickerMillis(), today))
    }
}
