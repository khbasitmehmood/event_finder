package com.eventfinder.app.client.createevent

import com.eventfinder.app.domain.model.Event

/**
 * UI State for Create Event screen
 */
sealed class CreateEventUiState {
    object Idle : CreateEventUiState()
    object Loading : CreateEventUiState()
    data class Success(val event: Event) : CreateEventUiState()
    data class Error(val message: String) : CreateEventUiState()
}

/**
 * Draft state for saving/loading drafts
 */
sealed class DraftState {
    object Idle : DraftState()
    object Saving : DraftState()
    object Saved : DraftState()
    data class Error(val message: String) : DraftState()
}
