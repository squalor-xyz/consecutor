package com.squalor.consecutor

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.squalor.consecutor.ui.theme.ConsecutorTheme

class MainActivity : ComponentActivity() {
    private val viewModel: TrackerViewModel by viewModels {
        viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ConsecutorApp
                TrackerViewModel(createSavedStateHandle(), app.repository, app.reminderScheduler)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            ConsecutorTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MainScreen(viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val trackerId = intent?.getLongExtra(ReminderScheduler.EXTRA_TRACKER_ID, -1L) ?: -1L
        if (trackerId != -1L) viewModel.select(trackerId)
    }
}
