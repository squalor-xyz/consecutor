package com.squalor.consecutor

import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
import androidx.test.espresso.Espresso
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
        Espresso.pressBack()
        composeRule.onNodeWithContentDescription("Add tracker").assertIsDisplayed()
    }

    @Test
    fun backFromSettingsReturnsToDashboard() {
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("Privacy-first defaults").assertIsDisplayed()
        Espresso.pressBack()
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
        composeRule.onNodeWithText("Name").performTextInput(" edited")
        composeRule.activityRule.scenario.recreate()
        waitForNode(hasText("Name"))
        composeRule.onNodeWithText("Name").assertTextContains(" edited")
    }

    @Test
    fun entryFieldsSurviveRecreation() {
        openTracker()
        composeRule.onNodeWithContentDescription("Add entry").performClick()
        composeRule.onNodeWithText("Value").performTextInput("42")
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
        composeRule.onNodeWithText("Navigation tracker").assertIsDisplayed()
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
