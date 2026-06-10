package com.squalor.consecutor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.squalor.consecutor.ui.theme.ConsecutorTheme

class MainActivity : ComponentActivity() {
    private val viewModel: TrackerViewModel by viewModels {
        val app = application as ConsecutorApp
        TrackerViewModelFactory(app.repository, app.reminderScheduler)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ConsecutorTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MainScreen(viewModel)
                }
            }
        }
    }
}
