package com.squalor.consecutor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.squalor.consecutor.ui.theme.ConsecutorTheme

/**
 * Main entry point of the app, hosting the Jetpack Compose UI.
 */
class MainActivity : ComponentActivity() {
    private val viewModel: EventViewModel by viewModels {
        EventViewModelFactory((application as ConsecutorApp).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ConsecutorTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    EventListScreen(viewModel)
                }
            }
        }
    }
}