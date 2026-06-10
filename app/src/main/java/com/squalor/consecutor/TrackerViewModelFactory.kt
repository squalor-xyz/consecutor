package com.squalor.consecutor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class TrackerViewModelFactory(
    private val repository: TrackerRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrackerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TrackerViewModel(repository, reminderScheduler) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
