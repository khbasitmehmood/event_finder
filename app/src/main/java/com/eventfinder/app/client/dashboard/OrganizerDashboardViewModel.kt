package com.eventfinder.app.client.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.model.TicketStatus
import com.eventfinder.app.domain.model.User
import com.eventfinder.app.domain.repository.TicketRepository
import com.eventfinder.app.domain.usecase.GetUserEventsUseCase
import com.eventfinder.app.domain.usecase.auth.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrganizerDashboardViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getUserEventsUseCase: GetUserEventsUseCase,
    private val ticketRepository: TicketRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrganizerDashboardUiState())
    val uiState: StateFlow<OrganizerDashboardUiState> = _uiState.asStateFlow()

    private var selectedEventStatsJob: Job? = null

    data class OrganizerDashboardUiState(
        val user: User? = null,
        val userEvents: List<Event> = emptyList(),
        val upcomingEvents: List<Event> = emptyList(),
        val selectedEventId: String? = null,
        val selectedEventStats: EventDashboardStats = EventDashboardStats(),
        val isLoading: Boolean = false,
        val isLoadingSelectedStats: Boolean = false,
        val error: String? = null
    )

    data class EventDashboardStats(
        val ticketsSold: Int = 0,
        val peopleAttending: Int = 0
    )

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Load current user
            getCurrentUserUseCase().fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(user = user)

                    // Load organizer's events
                    user?.let {
                        loadUserEvents(it.uid)
                    }
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load user data"
                    )
                }
            )
        }
    }

    private suspend fun loadUserEvents(userId: String) {
        getUserEventsUseCase(userId).fold(
            onSuccess = { events ->
                val organizerEvents = events.filter { it.organizerId == userId }
                val upcomingEvents = organizerEvents
                    .filter { it.isUpcoming() }
                    .sortedBy { it.startTime }
                val validSelectedEventId = _uiState.value.selectedEventId
                    ?.takeIf { selectedId ->
                        upcomingEvents.any { event -> event.dashboardEventId() == selectedId }
                    }
                val selectedEventId = validSelectedEventId ?: upcomingEvents.firstOrNull()?.dashboardEventId()

                _uiState.value = _uiState.value.copy(
                    userEvents = organizerEvents,
                    upcomingEvents = upcomingEvents,
                    selectedEventId = selectedEventId,
                    selectedEventStats = if (selectedEventId == null) {
                        EventDashboardStats()
                    } else {
                        _uiState.value.selectedEventStats
                    },
                    isLoading = false,
                    isLoadingSelectedStats = selectedEventId != null
                )

                if (selectedEventId != null) {
                    observeSelectedEventStats(selectedEventId)
                } else {
                    selectedEventStatsJob?.cancel()
                }
            },
            onFailure = { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = exception.message ?: "Failed to load events"
                )
            }
        )
    }

    fun selectEvent(eventId: String) {
        if (eventId == _uiState.value.selectedEventId) return

        _uiState.value = _uiState.value.copy(
            selectedEventId = eventId,
            selectedEventStats = EventDashboardStats(),
            isLoadingSelectedStats = true
        )
        observeSelectedEventStats(eventId)
    }

    private fun observeSelectedEventStats(eventId: String) {
        selectedEventStatsJob?.cancel()
        selectedEventStatsJob = viewModelScope.launch {
            ticketRepository.observeEventAttendees(eventId).collect { result ->
                result.fold(
                    onSuccess = { tickets ->
                        val ticketsSold = tickets.count { it.status != TicketStatus.CANCELLED }
                        val peopleAttending = tickets.count { it.status == TicketStatus.CHECKED_IN }

                        _uiState.value = _uiState.value.copy(
                            selectedEventStats = EventDashboardStats(
                                ticketsSold = ticketsSold,
                                peopleAttending = peopleAttending
                            ),
                            isLoadingSelectedStats = false
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            selectedEventStats = EventDashboardStats(),
                            isLoadingSelectedStats = false,
                            error = exception.message ?: "Failed to load event stats"
                        )
                    }
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun Event.dashboardEventId(): String {
        return eventId.ifBlank { id }
    }
}
