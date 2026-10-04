package com.squalor.consecutor

import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.performScrollToNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.time.LocalDate
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun seedTracker() {
        // These tests clear the app database. Never run them against personal device data.
        check(Build.HARDWARE in setOf("goldfish", "ranchu")) {
            "NavigationTest requires an Android emulator; it clears the app database."
        }
        val app = composeRule.activity.application as ConsecutorApp
        runBlocking(Dispatchers.IO) {
            app.database.clearAllTables()
            app.repository.createTracker(
                TrackerDraft(
                    name = "Navigation tracker",
                    emoji = null,
                    description = null,
                    type = TrackerType.COUNT,
                    unit = null,
                    colorHex = "#1F6FEB",
                    targetPeriod = null,
                    targetValue = null,
                    reminderEnabled = false,
                    reminderHour = 20,
                    reminderMinute = 0,
                    reminderDays = emptySet()
                )
            )
        }
        waitForNode(hasText("Navigation tracker"))
    }

    @Test
    fun backFromDetailReturnsToDashboard() {
        openTracker()
        pressBack()
        composeRule.onNodeWithContentDescription("Add tracker").assertIsDisplayed()
    }

    @Test
    fun backFromSettingsReturnsToDashboard() {
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("Privacy-first defaults").assertIsDisplayed()
        pressBack()
        composeRule.onNodeWithContentDescription("Add tracker").assertIsDisplayed()
    }

    @Test
    fun typedTrackerNameSurvivesRecreation() {
        composeRule.onNodeWithContentDescription("Add tracker").performClick()
        composeRule.onNodeWithText("Name").performTextInput("Unfinished tracker")
        composeRule.activityRule.scenario.recreate()
        waitForNode(hasText("Name"))
        composeRule.onNodeWithText("Name").assertTextContains("Unfinished tracker")
    }

    @Test
    fun editingTrackerNameSurvivesRecreation() {
        openTracker()
        composeRule.onNodeWithContentDescription("Edit tracker").performClick()
        composeRule.onNodeWithText("Name").performTextReplacement("Navigation tracker edited")
        composeRule.onNodeWithText("Name").assertTextContains("Navigation tracker edited")
        composeRule.activityRule.scenario.recreate()
        waitForNode(hasText("Name"))
        composeRule.onNodeWithText("Name").assertTextContains("Navigation tracker edited")
    }

    @Test
    fun entryFieldsSurviveRecreation() {
        openTracker()
        composeRule.onNodeWithContentDescription("Add entry").performClick()
        composeRule.onNodeWithText("Value").performTextReplacement("42")
        composeRule.onNodeWithText("Value").assertTextContains("42")
        composeRule.onNodeWithText("Note").performTextInput("Unfinished entry")
        composeRule.activityRule.scenario.recreate()
        waitForNode(hasText("Value"))
        composeRule.onNodeWithText("Value").assertTextContains("42")
        composeRule.onNodeWithText("Note").assertTextContains("Unfinished entry")
    }

    @Test
    fun selectedTrackerSurvivesRecreation() {
        openTracker()
        composeRule.activityRule.scenario.recreate()
        waitForNode(hasContentDescription("Add entry"))
        composeRule.onNodeWithContentDescription("Add entry").assertIsDisplayed()
        composeRule.onAllNodesWithText("Navigation tracker")[0].assertIsDisplayed()
    }

    @Test
    fun countOneTapLoggingAndUndoUpdateDashboard() {
        val app = composeRule.activity.application as ConsecutorApp
        runBlocking(Dispatchers.IO) {
            val dao = app.database.trackerDao()
            val id = dao.getTrackerBundles().single().tracker.id
            dao.insertTarget(TargetEntity(trackerId = id, period = TargetPeriod.DAILY, targetValue = 3.0))
        }
        waitForNode(hasText("0 / 3 today"))
        composeRule.onNodeWithText("+1").performClick()
        waitForNode(hasText("1 / 3 today"))
        waitForNode(hasText("Logged Navigation tracker (+1)"))
        composeRule.onNodeWithText("Undo").performClick()
        waitForNode(hasText("0 / 3 today"))
        assertTrue(activeEntries().isEmpty())
    }

    @Test
    fun countLongPressOpensCustomEntryForToday() {
        composeRule.onNodeWithText("+1").performTouchInput { longClick() }
        waitForNode(hasText("Log entry"))
        composeRule.onNodeWithText("Date (YYYY-MM-DD)").assertTextContains(LocalDate.now().toString())
        composeRule.onNodeWithText("Value").performTextReplacement("7")
        composeRule.onNodeWithText("Save").performClick()
        waitForNode(hasText("7 today"))
        assertEquals(7.0, activeEntries().single().value!!, 0.0)
    }

    @Test
    fun measureLogOpensTodayEditorWithoutNavigating() {
        changeTrackerType(TrackerType.MEASURE)
        waitForNode(hasText("Log"))
        composeRule.onNodeWithText("Log").performClick()
        waitForNode(hasText("Log entry"))
        composeRule.onNodeWithText("Date (YYYY-MM-DD)").assertTextContains(LocalDate.now().toString())
        composeRule.onNodeWithText("Value").performTextInput("12.5")
        composeRule.onNodeWithText("Save").performClick()
        waitForNode(hasText("Entry added."))
        composeRule.onNodeWithContentDescription("Add tracker").assertIsDisplayed()
        assertEquals(12.5, activeEntries().single().value!!, 0.0)
    }

    @Test
    fun yesNoTwoTapsClearAndReplaceTheSnackbarAndUndoRestores() {
        changeTrackerType(TrackerType.YES_NO)
        waitForNode(isToggleable())
        composeRule.onNode(isToggleable()).performClick()
        composeRule.onNode(isToggleable()).performClick()
        waitForNode(hasText("Cleared Navigation tracker"))
        waitForNode(hasText("Not logged today"))
        assertTrue(activeEntries().isEmpty())
        composeRule.onNodeWithText("Undo").performClick()
        waitForNode(hasText("Done today"))
        assertEquals(1, activeEntries().size)
    }

    @Test
    fun editorDeletionUndoRestoresValueAndNote() {
        openTracker()
        composeRule.onNodeWithContentDescription("Add entry").performClick()
        composeRule.onNodeWithText("Value").performTextReplacement("42")
        composeRule.onNodeWithText("Note").performTextInput("Keep this note")
        composeRule.onNodeWithText("Save").performClick()
        waitForNode(hasText("Entry added."))
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Keep this note"))
        composeRule.onNodeWithText("Keep this note").performClick()
        composeRule.onNodeWithText("Delete").performClick()
        waitForNode(hasText("Entry deleted"))
        assertTrue(activeEntries().isEmpty())
        composeRule.onNodeWithText("Undo").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { activeEntries().size == 1 }
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Keep this note"))
        composeRule.onNodeWithText("Keep this note").assertIsDisplayed()
        val entry = activeEntries().single()
        assertEquals(42.0, entry.value!!, 0.0)
        assertEquals("Keep this note", entry.note)
    }

    @Test
    fun archiveUndoReturnsTrackerToDashboard() {
        archiveTracker()
        composeRule.onNodeWithText("No trackers yet.").assertIsDisplayed()
        composeRule.onNodeWithText("Undo").performClick()
        waitForNode(hasText("Navigation tracker"))
        composeRule.onNodeWithText("Archived (1)").assertDoesNotExist()
        assertEquals(false, trackerBundle()!!.tracker.isArchived)
    }

    @Test
    fun archivedListIsReachableWithoutActiveTrackersAndRestoreKeepsReminder() {
        val app = composeRule.activity.application as ConsecutorApp
        val before = runBlocking(Dispatchers.IO) {
            val id = app.database.trackerDao().getTrackerBundles().single().tracker.id
            app.database.trackerDao().insertReminder(
                ReminderEntity(trackerId = id, enabled = true, hourOfDay = 7, minuteOfHour = 35, daysOfWeekCsv = "1,3,5")
            )
            app.repository.getTrackerBundle(id)!!.reminder
        }
        archiveTracker()
        assertEquals(before, trackerBundle()!!.reminder)
        composeRule.onNodeWithText("Archived (1)").performClick()
        waitForNode(hasText("Restore"))
        composeRule.activityRule.scenario.recreate()
        waitForNode(hasText("Restore"))
        composeRule.onNodeWithText("Restore").performClick()
        waitForNode(hasText("No archived trackers."))
        assertEquals(before, trackerBundle()!!.reminder)
        assertEquals(false, trackerBundle()!!.tracker.isArchived)
        composeRule.onNodeWithContentDescription("Back").performClick()
        waitForNode(hasText("Navigation tracker"))
        composeRule.onNodeWithText("Archived (1)").assertDoesNotExist()
    }

    @Test
    fun backFromArchivedReturnsToDashboard() {
        archiveTracker()
        composeRule.onNodeWithText("Archived (1)").performClick()
        waitForNode(hasText("Restore"))
        pressBack()
        composeRule.onNodeWithContentDescription("Add tracker").assertIsDisplayed()
    }

    @Test
    fun detailDeleteNamesTrackerCountsActiveEntriesAndCanBeCancelled() {
        seedEntriesForDelete()
        openTracker()
        composeRule.onNodeWithContentDescription("Tracker options").performClick()
        composeRule.onNodeWithText("Delete permanently").performClick()
        waitForNode(hasText("Delete tracker permanently?"))
        composeRule.onNodeWithText("Delete \"Navigation tracker\" and its 1 entry? This cannot be undone.").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithContentDescription("Add entry").assertIsDisplayed()
        assertTrue(trackerBundle() != null)
        composeRule.onNodeWithContentDescription("Tracker options").performClick()
        composeRule.onNodeWithText("Delete permanently").performClick()
        waitForNode(hasText("Delete tracker permanently?"))
        composeRule.onNodeWithText("Delete permanently").performClick()
        waitForNode(hasText("Tracker deleted."))
        composeRule.onNodeWithContentDescription("Add tracker").assertIsDisplayed()
        assertEquals(null, trackerBundle())
    }

    @Test
    fun archivedDeleteCountsActiveEntriesCanBeCancelledAndReturnsToDashboard() {
        seedEntriesForDelete()
        archiveTracker()
        composeRule.onNodeWithText("Archived (1)").performClick()
        waitForNode(hasText("Delete"))
        composeRule.onNodeWithText("Delete").performClick()
        waitForNode(hasText("Delete tracker permanently?"))
        composeRule.onNodeWithText("Delete \"Navigation tracker\" and its 1 entry? This cannot be undone.").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()
        assertTrue(trackerBundle()!!.tracker.isArchived)
        composeRule.onNodeWithText("Delete").performClick()
        waitForNode(hasText("Delete tracker permanently?"))
        composeRule.onNodeWithText("Delete permanently").performClick()
        waitForNode(hasText("Tracker deleted."))
        composeRule.onNodeWithContentDescription("Add tracker").assertIsDisplayed()
        composeRule.onNodeWithText("Archived (1)").assertDoesNotExist()
        assertEquals(null, trackerBundle())
    }

    private fun trackerBundle(): TrackerBundle? {
        val app = composeRule.activity.application as ConsecutorApp
        return runBlocking(Dispatchers.IO) { app.repository.getReminderBundles().singleOrNull() }
    }

    private fun seedEntriesForDelete() {
        val app = composeRule.activity.application as ConsecutorApp
        runBlocking(Dispatchers.IO) {
            val id = app.database.trackerDao().getTrackerBundles().single().tracker.id
            val draft = EntryDraft(LocalDate.now(), 1.0, null)
            app.repository.addEntry(id, TrackerType.COUNT, draft)
            val deleted = app.repository.addEntry(id, TrackerType.COUNT, draft)
            app.repository.deleteEntry(deleted, id)
        }
    }

    private fun archiveTracker() {
        openTracker()
        composeRule.onNodeWithContentDescription("Tracker options").performClick()
        composeRule.onNodeWithText("Archive tracker").performClick()
        waitForNode(hasText("Archived (1)"))
        waitForNode(hasText("Tracker archived."))
        composeRule.onNodeWithContentDescription("Add tracker").assertIsDisplayed()
    }

    private fun changeTrackerType(type: TrackerType) {
        val app = composeRule.activity.application as ConsecutorApp
        runBlocking(Dispatchers.IO) {
            val dao = app.database.trackerDao()
            val tracker = dao.getTrackerBundles().single().tracker
            dao.updateTracker(tracker.copy(type = type))
        }
    }

    private fun activeEntries(): List<EntryEntity> {
        val app = composeRule.activity.application as ConsecutorApp
        return runBlocking(Dispatchers.IO) {
            app.repository.getReminderBundles().single().entries.filterNot { it.isDeleted }
        }
    }

    // Espresso.pressBack needs the app window to have focus, which aosp-atd's system ANR dialog takes away.
    // This drives the same back dispatcher without a key event.
    private fun pressBack() {
        composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    private fun waitForNode(matcher: SemanticsMatcher) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openTracker() {
        composeRule.onNodeWithText("Navigation tracker").performClick()
        waitForNode(hasContentDescription("Add entry"))
        composeRule.onNodeWithContentDescription("Add entry").assertIsDisplayed()
    }
}
