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
