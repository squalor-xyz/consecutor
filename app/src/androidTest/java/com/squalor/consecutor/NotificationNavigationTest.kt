package com.squalor.consecutor

import android.content.Intent
import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationNavigationTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private lateinit var app: ConsecutorApp
    private var firstId = 0L
    private var secondId = 0L

    @Before
    fun seedTrackers() {
        check(Build.HARDWARE in setOf("goldfish", "ranchu")) {
            "NotificationNavigationTest requires an Android emulator; it clears the app database."
        }
        app = ApplicationProvider.getApplicationContext()
        runBlocking(Dispatchers.IO) {
            app.database.clearAllTables()
            firstId = createTracker("First notification tracker")
            secondId = createTracker("Second notification tracker")
        }
    }

    @Test
    fun initialIntentOpensDetailAndBackReturnsToDashboard() {
        ActivityScenario.launch<MainActivity>(trackerIntent(firstId)).use { scenario ->
            assertDetail("First notification tracker")
            back(scenario)
            composeRule.onNodeWithContentDescription("Add tracker").assertIsDisplayed()
        }
    }

    @Test
    fun newIntentOpensDetailFromDashboardAndSwitchesTrackerInSameActivity() {
        ActivityScenario.launch<MainActivity>(trackerIntent(null)).use { scenario ->
            waitForDashboard()
            sendIntent(scenario, firstId)
            assertDetail("First notification tracker")
            sendIntent(scenario, secondId)
            assertDetail("Second notification tracker")
            scenario.onActivity {
                assertEquals(secondId, it.intent.getLongExtra(ReminderScheduler.EXTRA_TRACKER_ID, -1L))
            }
            back(scenario)
            composeRule.onNodeWithContentDescription("Add tracker").assertIsDisplayed()
        }
    }

    @Test
    fun recreationPreservesSelectionAndDoesNotReplayIntentAfterBack() {
        ActivityScenario.launch<MainActivity>(trackerIntent(firstId)).use { scenario ->
            assertDetail("First notification tracker")
            // Change selection through the UI, leaving the launch intent pointing at the first tracker.
            back(scenario)
            composeRule.onNodeWithText("Second notification tracker").performClick()
            assertDetail("Second notification tracker")
            scenario.recreate()
            assertDetail("Second notification tracker")
            back(scenario)
            scenario.recreate()
            waitForDashboard()
            composeRule.onNodeWithContentDescription("Add entry").assertDoesNotExist()
        }
    }

    @Test
    fun missingTrackerShowsNotFoundAndBackReturnsToDashboard() {
        ActivityScenario.launch<MainActivity>(trackerIntent(Long.MAX_VALUE)).use { scenario ->
            composeRule.waitUntil(5_000) {
                composeRule.onAllNodes(hasText("Tracker not found.")).fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithText("Tracker not found.").assertIsDisplayed()
            back(scenario)
            composeRule.onNodeWithContentDescription("Add tracker").assertIsDisplayed()
        }
    }

    @Test
    fun intentWithoutTrackerExtraLeavesCurrentDetailSelected() {
        ActivityScenario.launch<MainActivity>(trackerIntent(firstId)).use { scenario ->
            assertDetail("First notification tracker")
            sendIntent(scenario, null)
            assertDetail("First notification tracker")
        }
    }

    @Test
    fun notificationContentIntentsKeepTrackersDistinctAndReuseActivity() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
            ParcelFileDescriptor.AutoCloseInputStream(
                automation.executeShellCommand("pm grant ${app.packageName} ${Manifest.permission.POST_NOTIFICATIONS}")
            ).use { it.readBytes() }
        }
        val scheduler = ReminderScheduler(app)
        scheduler.ensureNotificationChannel()
        val manager = app.getSystemService(NotificationManager::class.java)
        try {
            scheduler.showNotification(app, firstId, "First notification tracker", null, TrackerType.COUNT)
            scheduler.showNotification(app, secondId, "Second notification tracker", null, TrackerType.COUNT)
            lateinit var firstIntent: PendingIntent
            lateinit var secondIntent: PendingIntent
            composeRule.waitUntil(5_000) {
                val notifications = manager.activeNotifications.associateBy { it.id }
                val first = notifications[firstId.toInt()]?.notification?.contentIntent
                val second = notifications[secondId.toInt()]?.notification?.contentIntent
                if (first != null && second != null) {
                    firstIntent = first
                    secondIntent = second
                    true
                } else false
            }
            assertNotEquals(firstIntent, secondIntent)
            ActivityScenario.launch<MainActivity>(trackerIntent(null)).use { scenario ->
                waitForDashboard()
                firstIntent.send()
                assertDetail("First notification tracker")
                secondIntent.send()
                assertDetail("Second notification tracker")
                scenario.onActivity {
                    assertEquals(secondId, it.intent.getLongExtra(ReminderScheduler.EXTRA_TRACKER_ID, -1L))
                }
                back(scenario)
            }
        } finally {
            manager.cancel(firstId.toInt())
            manager.cancel(secondId.toInt())
        }
    }

    @Test
    fun countActionAddsOneEntryAndDismissesNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
            ParcelFileDescriptor.AutoCloseInputStream(
                automation.executeShellCommand("pm grant ${app.packageName} ${Manifest.permission.POST_NOTIFICATIONS}")
            ).use { it.readBytes() }
        }
        val scheduler = ReminderScheduler(app)
        scheduler.ensureNotificationChannel()
        val manager = app.getSystemService(NotificationManager::class.java)
        try {
            scheduler.showNotification(app, firstId, "First notification tracker", null, TrackerType.COUNT)
            lateinit var action: android.app.Notification.Action
            composeRule.waitUntil(5_000) {
                val found = manager.activeNotifications.firstOrNull { it.id == firstId.toInt() }
                    ?.notification?.actions?.singleOrNull()
                if (found != null) action = found
                found != null
            }
            assertEquals("+1", action.title.toString())
            action.actionIntent.send()
            composeRule.waitUntil(5_000) {
                val entries = runBlocking(Dispatchers.IO) { app.repository.countActiveEntries(firstId) }
                entries == 1 && manager.activeNotifications.none { it.id == firstId.toInt() }
            }
        } finally {
            manager.cancel(firstId.toInt())
        }
    }

    @Test
    fun markDoneTwiceLogsYesNoOnce() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
            ParcelFileDescriptor.AutoCloseInputStream(
                automation.executeShellCommand("pm grant ${app.packageName} ${Manifest.permission.POST_NOTIFICATIONS}")
            ).use { it.readBytes() }
        }
        val yesNoId = runBlocking(Dispatchers.IO) { createTracker("Yes/no notification tracker", TrackerType.YES_NO) }
        val scheduler = ReminderScheduler(app)
        scheduler.ensureNotificationChannel()
        val manager = app.getSystemService(NotificationManager::class.java)
        try {
            scheduler.showNotification(app, yesNoId, "Yes/no notification tracker", null, TrackerType.YES_NO)
            lateinit var action: android.app.Notification.Action
            composeRule.waitUntil(5_000) {
                val found = manager.activeNotifications.firstOrNull { it.id == yesNoId.toInt() }
                    ?.notification?.actions?.singleOrNull()
                if (found != null) action = found
                found != null
            }
            assertEquals("Mark done", action.title.toString())
            action.actionIntent.send()
            composeRule.waitUntil(5_000) {
                runBlocking(Dispatchers.IO) { app.repository.countActiveEntries(yesNoId) } == 1
            }
            action.actionIntent.send()
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            Thread.sleep(1_000)
            assertEquals(1, runBlocking(Dispatchers.IO) { app.repository.countActiveEntries(yesNoId) })
        } finally {
            manager.cancel(yesNoId.toInt())
        }
    }

    private suspend fun createTracker(name: String, type: TrackerType = TrackerType.COUNT): Long = app.repository.createTracker(
        TrackerDraft(
            name = name, emoji = null, description = null, type = type,
            unit = null, colorHex = "#1F6FEB", targetPeriod = null, targetValue = null,
            reminderEnabled = false, reminderHour = 20, reminderMinute = 0, reminderDays = emptySet()
        )
    )

    private fun trackerIntent(id: Long?): Intent = Intent(app, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (id != null) putExtra(ReminderScheduler.EXTRA_TRACKER_ID, id)
    }

    private fun sendIntent(scenario: ActivityScenario<MainActivity>, id: Long?) {
        scenario.onActivity {
            it.startActivity(trackerIntent(id))
        }
        composeRule.waitForIdle()
    }

    private fun assertDetail(name: String) {
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasContentDescription("Add entry")).fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodes(hasText(name)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Add entry").assertIsDisplayed()
        composeRule.onAllNodesWithText(name)[0].assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Add tracker").assertDoesNotExist()
    }

    private fun waitForDashboard() {
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasContentDescription("Add tracker")).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun back(scenario: ActivityScenario<MainActivity>) {
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
        waitForDashboard()
    }
}
