package com.squalor.consecutor

sealed interface UiEvent {
    data class Archived(val text: String, val trackerId: Long) : UiEvent
    data class Deleted(val text: String, val trackerId: Long) : UiEvent
    data class Message(val text: String) : UiEvent
    data class Logged(val text: String, val trackerId: Long, val entryId: Long) : UiEvent
    data class Cleared(val text: String, val trackerId: Long, val entryIds: List<Long>) : UiEvent
}
