package com.eventfinder.app.client.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventfinder.app.domain.usecase.GetExploreEventsUseCase
import com.eventfinder.app.domain.usecase.SearchEventsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for ExploreFragment
 * Handles business logic and communicates with use cases
 */
@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val getExploreEventsUseCase: GetExploreEventsUseCase,
    private val searchEventsUseCase: SearchEventsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ExploreUiState>(ExploreUiState.Loading)
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadEvents()
    }

    /**
     * Load all explore events
     */
    fun loadEvents() {
        viewModelScope.launch {
            _uiState.value = ExploreUiState.Loading

            getExploreEventsUseCase()
                .onSuccess { events ->
                    _uiState.value = if (events.isEmpty()) {
                        ExploreUiState.Empty
                    } else {
                        ExploreUiState.Success(events)
                    }
                }
                .onFailure { error ->
                    _uiState.value = ExploreUiState.Error(
                        error.message ?: "An unknown error occurred"
                    )
                }
        }
    }

    /**
     * Search events by query
     */
    fun searchEvents(query: String) {
        _searchQuery.value = query

        viewModelScope.launch {
            _uiState.value = ExploreUiState.Loading

            searchEventsUseCase(query)
                .onSuccess { events ->
                    _uiState.value = if (events.isEmpty()) {
                        ExploreUiState.Empty
                    } else {
                        ExploreUiState.Success(events)
                    }
                }
                .onFailure { error ->
                    _uiState.value = ExploreUiState.Error(
                        error.message ?: "Search failed"
                    )
                }
        }
    }

    /**
     * Refresh events
     */
    fun refresh() {
        if (_searchQuery.value.isBlank()) {
            loadEvents()
        } else {
            searchEvents(_searchQuery.value)
        }
    }
}