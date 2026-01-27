package com.eventfinder.app.client.explore

import com.eventfinder.app.domain.model.Event

/**
 * UI State for Explore screen
 */
sealed class ExploreUiState {
    object Loading : ExploreUiState()
    data class Success(val events: List<Event>) : ExploreUiState()
    data class Error(val message: String) : ExploreUiState()
    object Empty : ExploreUiState()
}
