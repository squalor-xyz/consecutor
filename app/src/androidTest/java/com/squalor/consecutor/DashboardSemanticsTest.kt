package com.squalor.consecutor

import android.os.Build
import android.text.format.DateFormat
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RunWith(AndroidJUnit4::class)
class DashboardSemanticsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()
    private val today = LocalDate.now()
    private val app get() = composeRule.activity.application as ConsecutorApp
    private val resources get() = composeRule.activity.resources

    @Before
    fun seedTracker() {
        check(Build.HARDWARE in setOf("goldfish", "ranchu")) {
            "DashboardSemanticsTest requires an emulator; it clears the app database."
        }
        runBlocking(Dispatchers.IO) {
            app.database.clearAllTables()
            val id = app.repository.createTracker(
                TrackerDraft(
                    name = "Water", emoji = null, description = null, type = TrackerType.COUNT,
                    unit = "glasses", colorHex = "#1F6FEB", targetPeriod = TargetPeriod.DAILY,
                    targetValue = 8.0, reminderEnabled = false, reminderHour = 20,
                    reminderMinute = 0, reminderDays = emptySet()
                )
            )
            (1L..5L).forEach { daysAgo ->
                app.repository.addEntry(id, TrackerType.COUNT, EntryDraft(today.minusDays(daysAgo), 8.0, null))
            }
            app.repository.addEntry(id, TrackerType.COUNT, EntryDraft(today, 2.0, null))
        }
        waitFor(hasContentDescription(expectedCountLabel("2")))
    }

    @Test
    fun dashboardCardExposesOneMergedLabelAndNavigates() {
        val label = expectedCountLabel("2")
        composeRule.onAllNodes(hasContentDescription(label)).assertCountEquals(1)
        composeRule.onNodeWithContentDescription(label).assertHasClickAction().performClick()
        waitFor(hasContentDescription(resources.getString(R.string.cd_edit_tracker)))
        composeRule.onNodeWithContentDescription(resources.getString(R.string.cd_edit_tracker)).assertIsDisplayed()
    }

    @Test
    fun incrementIsNamedIndependentAndDoesNotNavigate() {
        composeRule.onNodeWithContentDescription(resources.getString(R.string.dashboard_increment_description, "Water"))
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        waitFor(hasContentDescription(expectedCountLabel("3")))
        composeRule.onNodeWithContentDescription(resources.getString(R.string.cd_add_tracker)).assertIsDisplayed()
    }

    @Test
    fun yesNoControlNamesTrackerAndUpdatesItsState() {
        changeType(TrackerType.YES_NO)
        val clear = resources.getString(R.string.dashboard_clear_today_description, "Water")
        waitFor(hasContentDescription(clear))
        composeRule.onNodeWithContentDescription(clear).assertIsOn().performClick()
        val log = resources.getString(R.string.dashboard_log_today_description, "Water")
        waitFor(hasContentDescription(log))
        composeRule.onNodeWithContentDescription(log).assertIsOff()
        composeRule.onNodeWithContentDescription(resources.getString(R.string.cd_add_tracker)).assertIsDisplayed()
    }

    @Test
    fun measureLoggingNamesTrackerAndOpensEditor() {
        changeType(TrackerType.MEASURE)
        val log = resources.getString(R.string.dashboard_log_today_description, "Water")
        waitFor(hasContentDescription(log))
        composeRule.onNodeWithContentDescription(log).performClick()
        waitFor(hasText(resources.getString(R.string.entry_title_log)))
        composeRule.onNodeWithText(resources.getString(R.string.entry_title_log)).assertIsDisplayed()
    }

    @Test
    fun editorLabelsToggleOneSwitchPerRow() {
        composeRule.onNodeWithContentDescription(resources.getString(R.string.cd_add_tracker)).performClick()
        for (labelRes in listOf(R.string.editor_track_target, R.string.editor_reminder)) {
            val label = resources.getString(labelRes)
            val matcher = hasText(label) and isToggleable() and
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)
            scrollTo(matcher, inDialog = true)
            composeRule.onNode(matcher).assertIsOff().assertHeightIsAtLeast(48.dp)
            composeRule.onAllNodes(isToggleable() and hasAnyDescendant(hasText(label)), useUnmergedTree = true)
                .assertCountEquals(1)
            // Tap the label side, away from the switch, to exercise the actual row touch target.
            composeRule.onNode(matcher).performTouchInput { click(Offset(12f, center.y)) }
            composeRule.onNode(matcher).assertIsOn()
            if (labelRes == R.string.editor_reminder) {
                val day = DayOfWeek.FRIDAY.getDisplayName(TextStyle.SHORT, resources.configuration.locales[0])
                scrollTo(hasText(day), inDialog = true)
                composeRule.onNodeWithText(day).assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
                scrollTo(matcher, inDialog = true)
            }
            composeRule.onNode(matcher).performTouchInput { click(Offset(12f, center.y)) }
            composeRule.onNode(matcher).assertIsOff()
        }
    }

    @Test
    fun calendarDayHas48DpBoundsAndOpensEditor() {
        composeRule.onNodeWithContentDescription(expectedCountLabel("2")).performClick()
        val locale = resources.configuration.locales[0]
        val date = today.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMMd"), locale))
        val label = resources.getString(R.string.calendar_day_description, date, resources.getString(R.string.calendar_state_partial))
        val day = composeRule.onNodeWithContentDescription(label)
        scrollTo(hasContentDescription(label))
        day.assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).assertHasClickAction()
        day.performClick()
        waitFor(hasText(resources.getString(R.string.entry_title_edit)))
        composeRule.onNodeWithText(resources.getString(R.string.entry_title_edit)).assertIsDisplayed()
    }

    @Test
    fun settingsBackupActionsRemainReachable() {
        composeRule.onNodeWithContentDescription(resources.getString(R.string.cd_settings)).performClick()
        val importAction = hasText(resources.getString(R.string.settings_import_backup))
        scrollTo(importAction)
        composeRule.onNode(importAction).assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        val notificationsAction = hasText(resources.getString(R.string.settings_allow_notifications)) or
            hasText(resources.getString(R.string.action_open_settings))
        scrollTo(notificationsAction)
        composeRule.onNode(notificationsAction).assertIsDisplayed()
    }

    @Test
    fun createTrackerFieldsAndSaveRemainReachable() {
        composeRule.onNodeWithContentDescription(resources.getString(R.string.cd_add_tracker)).performClick()
        scrollTo(hasText(resources.getString(R.string.editor_name)), inDialog = true)
        composeRule.onNodeWithText(resources.getString(R.string.editor_name)).performTextInput("A tracker with a longer name")
        scrollTo(hasText(resources.getString(R.string.editor_type_count)), inDialog = true)
        composeRule.onNodeWithText(resources.getString(R.string.editor_type_count)).performClick()
        scrollTo(hasText(resources.getString(R.string.editor_unit)), inDialog = true)
        composeRule.onNodeWithText(resources.getString(R.string.editor_unit)).performTextInput("glasses")
        composeRule.onNodeWithText(resources.getString(R.string.action_save)).assertIsDisplayed().performClick()
        waitFor(hasText("A tracker with a longer name"))
        composeRule.onNodeWithText("A tracker with a longer name").assertIsDisplayed()
    }

    private fun expectedCountLabel(value: String): String = listOf(
        "Water",
        resources.getString(R.string.status_today, resources.getString(R.string.status_progress_spoken, value, "8") + " glasses"),
        resources.getQuantityString(R.plurals.streak_days, 5, 5, 5),
        resources.getQuantityString(R.plurals.dashboard_completion_days, 14, 14, 35)
    ).joinToString(resources.getString(R.string.accessibility_separator))

    private fun changeType(type: TrackerType) = runBlocking(Dispatchers.IO) {
        val dao = app.database.trackerDao()
        val tracker = dao.getTrackerBundles().single().tracker
        dao.updateTracker(tracker.copy(type = type))
        dao.deleteTargetForTracker(tracker.id)
        if (type == TrackerType.YES_NO) {
            dao.insertTarget(TargetEntity(trackerId = tracker.id, period = TargetPeriod.DAILY, targetValue = 1.0))
        }
    }

    private fun scrollTo(matcher: SemanticsMatcher, inDialog: Boolean = false) {
        val scroll = if (inDialog) hasScrollAction() and hasAnyAncestor(isDialog()) else hasScrollAction()
        composeRule.onNode(scroll).performScrollToNode(matcher)
    }

    private fun waitFor(matcher: SemanticsMatcher) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
