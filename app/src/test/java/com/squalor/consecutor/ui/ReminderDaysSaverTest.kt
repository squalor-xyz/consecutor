package com.squalor.consecutor.ui

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek

class ReminderDaysSaverTest {
    private val scope = SaverScope { true }

    @Test
    fun emptySelectionRoundTripsInsteadOfResettingToInitialDays() {
        assertRoundTrip(emptySet())
    }

    @Test
    fun selectedDaysRoundTrip() {
        assertRoundTrip(setOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY))
    }

    private fun assertRoundTrip(days: Set<DayOfWeek>) {
        val saved = requireNotNull(with(ReminderDaysSaver) { scope.save(days) })
        assertEquals(days, ReminderDaysSaver.restore(saved))
    }
}
