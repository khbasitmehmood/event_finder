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
    val promotedOrganizers: List<User> = emptyList(), // TODO: Implement promoted organizers
    val interestBasedOrganizers: List<User> = emptyList(), // TODO: Implement interest filtering
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
     * TODO: Add interest-based filtering and promoted organizers
     */
    private fun extractOrganizers(events: List<Event>): List<User> {
        if (events.isEmpty()) {
            android.util.Log.d("ExploreViewModel", "No events to extract organizers from")
            return emptyList()
        }

        // Group events by organizer ID and create User objects
        val organizerMap = mutableMapOf<String, User>()

        events.forEach { event ->
            // Skip if organizer ID is empty
            if (event.organizerId.isBlank()) {
                android.util.Log.d("ExploreViewModel", "Event ${event.title} has empty organizerId")
                return@forEach
            }

            if (!organizerMap.containsKey(event.organizerId)) {
                // Create a minimal User object from event data
                organizerMap[event.organizerId] = User(
                    uid = event.organizerId,
                    email = "",
                    userType = UserType.ORGANIZER,
                    organizerProfile = com.eventfinder.app.domain.model.OrganizerProfile(
                        organizationName = event.organizerName.ifBlank { "Unknown Organizer" },
                        contactPerson = event.organizerName.ifBlank { "Unknown" },
                        phoneNumber = "",
                        logoUrl = event.organizerPhotoUrl,
                        city = null // TODO: Extract from event address
                    ),
                    isProfileComplete = true
                )
                android.util.Log.d("ExploreViewModel", "Added organizer: ${event.organizerName} (${event.organizerId})")
            }
        }

        val organizers = organizerMap.values.toList()
        android.util.Log.d("ExploreViewModel", "Extracted ${organizers.size} unique organizers from ${events.size} events")

        // TODO: Future enhancement - sort by:
        // 1. Promoted organizers (premium feature)
        // 2. Organizers matching user interests
        // 3. Organizers with most upcoming events
        // 4. Organizers with highest rated events

        return organizers
    }

    /**
     * TODO: Filter organizers by user interests
     * This will show organizers who host events in categories the user is interested in
     */
    private fun filterOrganizersByInterests(
        organizers: List<User>,
        userInterests: List<String>,
        events: List<Event>
    ): List<User> {
        // Count events per organizer per interest category
        // Return organizers sorted by relevance to user interests
        return organizers // Placeholder for now
    }

    /**
     * TODO: Get promoted organizers
     * This will return organizers who have paid for promotion
     */
    private fun getPromotedOrganizers(organizers: List<User>): List<User> {
        // Check organizer profile for promotion status
        // Return promoted organizers first
        return organizers // Placeholder for now
    }
}