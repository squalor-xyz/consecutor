package com.squalor.consecutor

import androidx.annotation.StringRes

sealed interface UiEvent {
    data class Archived(@StringRes val textRes: Int, val trackerId: Long, val args: List<Any> = emptyList()) : UiEvent
    data class Deleted(@StringRes val textRes: Int, val trackerId: Long, val args: List<Any> = emptyList()) : UiEvent
    data class Message(@StringRes val textRes: Int, val args: List<Any> = emptyList()) : UiEvent
    data class Logged(
        @StringRes val textRes: Int,
        val trackerId: Long,
        val entryId: Long,
        val args: List<Any> = emptyList()
    ) : UiEvent
    data class Cleared(
        @StringRes val textRes: Int,
        val trackerId: Long,
        val entryIds: List<Long>,
        val args: List<Any> = emptyList()
    ) : UiEvent
}
