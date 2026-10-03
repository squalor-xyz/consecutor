package com.squalor.consecutor.ui

import androidx.compose.runtime.saveable.SaverScope
import com.squalor.consecutor.TrackerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class EntryEditorStateSaverTest {
    private val scope = SaverScope { true }

    @Test
    fun newEntryWithNullableFieldsRoundTrips() {
        val state = EntryEditorState(
            trackerId = 7L,
            trackerType = TrackerType.COUNT,
            unit = null,
            entryId = null,
            existingDate = LocalDate.of(2026, 10, 3),
            existingValue = null,
            existingNote = null
        )
        assertRoundTrip(state)
    }

    @Test
    fun existingEntryRoundTrips() {
        val state = EntryEditorState(
            trackerId = 9L,
            trackerType = TrackerType.MEASURE,
            unit = "kg",
            entryId = 15L,
            existingDate = LocalDate.of(2026, 10, 2),
            existingValue = 72.5,
            existingNote = "Previous measurement"
        )
        assertRoundTrip(state)
    }

    @Test
    fun closedEditorDoesNotSaveState() {
        val saved = with(EntryEditorStateSaver) { scope.save(null) }
        assertNull(saved)
    }

    private fun assertRoundTrip(state: EntryEditorState) {
        val saved = requireNotNull(with(EntryEditorStateSaver) { scope.save(state) })
        assertEquals(state, EntryEditorStateSaver.restore(saved))
    }
}
