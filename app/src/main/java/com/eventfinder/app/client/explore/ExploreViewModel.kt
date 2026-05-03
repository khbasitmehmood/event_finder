package com.eventfinder.app.client.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.model.EventCategory
import com.eventfinder.app.domain.model.User
import com.eventfinder.app.domain.model.UserType
import com.eventfinder.app.domain.usecase.GetEventCategoriesUseCase
import com.eventfinder.app.domain.usecase.GetExploreEventsUseCase
import com.eventfinder.app.domain.usecase.SearchEventsUseCase
import com.eventfinder.app.domain.usecase.auth.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Filter options for explore screen
 */
data class ExploreFilters(
    val selectedCategories: Set<String> = emptySet(),
    val priceFilter: PriceFilter = PriceFilter.ALL,
    val onlyUserInterests: Boolean = false
)

enum class PriceFilter {
    ALL, FREE, PAID
}

/**
 * Enhanced UI State for Explore
 */
data class ExploreState(
    val allEvents: List<Event> = emptyList(),
    val filteredEvents: List<Event> = emptyList(),
    val organizers: List<User> = emptyList(),
    val allCategories: List<EventCategory> = emptyList(),
    val userInterests: List<String> = emptyList(),
    val filters: ExploreFilters = ExploreFilters(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for ExploreFragment
 * Handles business logic and communicates with use cases
 */
@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val getExploreEventsUseCase: GetExploreEventsUseCase,
    private val searchEventsUseCase: SearchEventsUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getEventCategoriesUseCase: GetEventCategoriesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreState())
    val uiState: StateFlow<ExploreState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadData()
    }

    /**
     * Load all data including events, categories, user info, and organizers
     */
    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Load user data for interests
            val user = getCurrentUserUseCase().getOrNull()
            val userInterests = user?.profile?.interests ?: emptyList()

            // Load all categories
            val categories = getEventCategoriesUseCase().getOrNull() ?: emptyList()

            // Load all events
            getExploreEventsUseCase()
                .onSuccess { events ->
                    // Extract unique organizers from events
                    val organizers = extractOrganizers(events)

                    _uiState.update { state ->
                        val filteredEvents = applyFilters(events, state.filters, userInterests)
                        state.copy(
                            allEvents = events,
                            filteredEvents = filteredEvents,
                            organizers = organizers,
                            allCategories = categories,
                            userInterests = userInterests,
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load events"
                        )
                    }
                }
        }
    }

    /**
     * Search events by query
     */
    fun searchEvents(query: String) {
        _searchQuery.value = query

        if (query.isBlank()) {
            // Reset to all events with current filters
            _uiState.update { state ->
                state.copy(
                    filteredEvents = applyFilters(state.allEvents, state.filters, state.userInterests)
                )
            }
            return
        }

        viewModelScope.launch {
            searchEventsUseCase(query)
                .onSuccess { events ->
                    _uiState.update { state ->
                        state.copy(
                            filteredEvents = applyFilters(events, state.filters, state.userInterests)
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(error = error.message ?: "Search failed")
                    }
                }
        }
    }

    /**
     * Apply filters to events
     */
    fun applyFilters(filters: ExploreFilters) {
        _uiState.update { state ->
            val baseEvents = if (_searchQuery.value.isBlank()) {
                state.allEvents
            } else {
                state.filteredEvents
            }

            state.copy(
                filters = filters,
                filteredEvents = applyFilters(baseEvents, filters, state.userInterests)
            )
        }
    }

    /**
     * Toggle user interests filter
     */
    fun toggleUserInterestsFilter() {
        val currentFilters = _uiState.value.filters
        applyFilters(currentFilters.copy(onlyUserInterests = !currentFilters.onlyUserInterests))
    }

    /**
     * Clear all filters
     */
    fun clearFilters() {
        applyFilters(ExploreFilters())
    }

    /**
     * Refresh data
     */
    fun refresh() {
        loadData()
    }

    /**
     * Clear error
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Apply filters to event list
     */
    private fun applyFilters(
        events: List<Event>,
        filters: ExploreFilters,
        userInterests: List<String>
    ): List<Event> {
        var filtered = events

        // Filter by user interests
        if (filters.onlyUserInterests && userInterests.isNotEmpty()) {
            filtered = filtered.filter { event ->
                event.category?.id?.let { userInterests.contains(it) } ?: false
            }
        }

        // Filter by selected categories
        if (filters.selectedCategories.isNotEmpty()) {
            filtered = filtered.filter { event ->
                event.category?.id?.let { filters.selectedCategories.contains(it) } ?: false
            }
        }

        // Filter by price
        filtered = when (filters.priceFilter) {
            PriceFilter.FREE -> filtered.filter { it.isFree }
            PriceFilter.PAID -> filtered.filter { !it.isFree }
            PriceFilter.ALL -> filtered
        }

        return filtered
    }

    /**
     * Extract unique organizers from events
     */
    private fun extractOrganizers(events: List<Event>): List<User> {
        // Group events by organizer ID and create User objects
        val organizerMap = mutableMapOf<String, User>()

        events.forEach { event ->
            if (!organizerMap.containsKey(event.organizerId)) {
                // Create a minimal User object from event data
                organizerMap[event.organizerId] = User(
                    uid = event.organizerId,
                    email = "",
                    userType = UserType.ORGANIZER,
                    organizerProfile = com.eventfinder.app.domain.model.OrganizerProfile(
                        organizationName = event.organizerName,
                        contactPerson = event.organizerName,
                        phoneNumber = "",
                        logoUrl = event.organizerPhotoUrl
                    ),
                    isProfileComplete = true
                )
            }
        }

        return organizerMap.values.toList()
    }
}