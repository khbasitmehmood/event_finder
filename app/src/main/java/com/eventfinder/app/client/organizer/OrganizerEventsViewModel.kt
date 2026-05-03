package com.eventfinder.app.client.organizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.repository.EventRepository
import com.eventfinder.app.utils.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrganizerEventsViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventsUiState())
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

    data class EventsUiState(
        val upcomingEvents: List<Event> = emptyList(),
        val happeningNowEvents: List<Event> = emptyList(),
        val pastEvents: List<Event> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null
    )

    init {
        loadEvents()
    }

    fun loadEvents() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val organizerId = userPreferences.getUserId()
            if (organizerId == null) {
                _uiState.update {
                    it.copy(isLoading = false, error = "User not logged in")
                }
                return@launch
            }

            eventRepository.getUserEvents(organizerId).fold(
                onSuccess = { events ->
                    val (upcoming, happeningNow, past) = categorizeEvents(events)
                    _uiState.update {
                        it.copy(
                            upcomingEvents = upcoming,
                            happeningNowEvents = happeningNow,
                            pastEvents = past,
                            isLoading = false
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load events"
                        )
                    }
                }
            )
        }
    }

    private fun categorizeEvents(events: List<Event>): Triple<List<Event>, List<Event>, List<Event>> {
        val now = System.currentTimeMillis()

        val upcoming = events
            .filter { it.startTime > now }
            .sortedBy { it.startTime }

        val happeningNow = events
            .filter { it.startTime <= now && (it.endTime ?: Long.MAX_VALUE) >= now }
            .sortedBy { it.startTime }

        val past = events
            .filter { (it.endTime ?: it.startTime) < now }
            .sortedByDescending { it.endTime ?: it.startTime }

        return Triple(upcoming, happeningNow, past)
    }

    fun refreshEvents() {
        loadEvents()
    }
}
