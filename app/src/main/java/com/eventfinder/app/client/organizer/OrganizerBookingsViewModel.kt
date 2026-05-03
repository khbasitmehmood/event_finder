package com.eventfinder.app.client.organizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.model.Ticket
import com.eventfinder.app.domain.model.TicketStatus
import com.eventfinder.app.domain.model.TicketType
import com.eventfinder.app.domain.repository.EventRepository
import com.eventfinder.app.domain.repository.TicketRepository
import com.eventfinder.app.utils.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrganizerBookingsViewModel @Inject constructor(
    private val ticketRepository: TicketRepository,
    private val eventRepository: EventRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingsUiState())
    val uiState: StateFlow<BookingsUiState> = _uiState.asStateFlow()

    private var allBookings: List<BookingGroup> = emptyList()
    private var currentFilter: FilterType = FilterType.ALL
    private var currentSearchQuery: String = ""

    data class BookingsUiState(
        val bookingGroups: List<BookingGroup> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val isEmpty: Boolean = false
    )

    data class BookingGroup(
        val event: Event,
        val tickets: List<Ticket>,
        val totalCount: Int,
        val checkInCount: Int,
        val paidCount: Int,
        val freeCount: Int,
        val cancelledCount: Int,
        val isExpanded: Boolean = false
    )

    enum class FilterType {
        ALL, PAID, FREE, PENDING, CHECKED_IN, CANCELLED
    }

    init {
        loadBookings()
    }

    fun loadBookings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val organizerId = userPreferences.getUserId()
            if (organizerId == null) {
                _uiState.update {
                    it.copy(isLoading = false, error = "User not logged in")
                }
                return@launch
            }

            // Get all tickets for organizer's events
            ticketRepository.getOrganizerBookings(organizerId).fold(
                onSuccess = { tickets ->
                    // Get unique event IDs
                    val eventIds = tickets.map { it.eventId }.distinct()

                    // Fetch event details for each
                    val events = mutableMapOf<String, Event>()
                    eventIds.forEach { eventId ->
                        eventRepository.getEventById(eventId).fold(
                            onSuccess = { event ->
                                if (event != null) {
                                    events[eventId] = event
                                }
                            },
                            onFailure = { /* Skip if event not found */ }
                        )
                    }

                    // Group tickets by event
                    val grouped = tickets.groupBy { it.eventId }.mapNotNull { (eventId, eventTickets) ->
                        val event = events[eventId] ?: return@mapNotNull null

                        BookingGroup(
                            event = event,
                            tickets = eventTickets,
                            totalCount = eventTickets.count { it.status != TicketStatus.CANCELLED },
                            checkInCount = eventTickets.count { it.status == TicketStatus.CHECKED_IN },
                            paidCount = eventTickets.count {
                                it.ticketType == TicketType.PAID && it.status != TicketStatus.CANCELLED
                            },
                            freeCount = eventTickets.count {
                                (it.ticketType == TicketType.FREE_PRIVATE ||
                                 it.ticketType == TicketType.PUBLIC_RESERVATION) &&
                                it.status != TicketStatus.CANCELLED
                            },
                            cancelledCount = eventTickets.count { it.status == TicketStatus.CANCELLED }
                        )
                    }.sortedByDescending { it.event.startTime }

                    allBookings = grouped
                    applyFiltersAndSearch()
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load bookings"
                        )
                    }
                }
            )
        }
    }

    fun setFilter(filter: FilterType) {
        currentFilter = filter
        applyFiltersAndSearch()
    }

    fun setSearchQuery(query: String) {
        currentSearchQuery = query.lowercase()
        applyFiltersAndSearch()
    }

    fun toggleGroupExpanded(eventId: String) {
        allBookings = allBookings.map { group ->
            if (group.event.eventId == eventId) {
                group.copy(isExpanded = !group.isExpanded)
            } else {
                group
            }
        }
        applyFiltersAndSearch()
    }

    private fun applyFiltersAndSearch() {
        var filtered = allBookings

        // Apply filter
        if (currentFilter != FilterType.ALL) {
            filtered = filtered.map { group ->
                val filteredTickets = when (currentFilter) {
                    FilterType.PAID -> group.tickets.filter {
                        it.ticketType == TicketType.PAID && it.status != TicketStatus.CANCELLED
                    }
                    FilterType.FREE -> group.tickets.filter {
                        (it.ticketType == TicketType.FREE_PRIVATE ||
                         it.ticketType == TicketType.PUBLIC_RESERVATION) &&
                        it.status != TicketStatus.CANCELLED
                    }
                    FilterType.PENDING -> group.tickets.filter {
                        it.status == TicketStatus.RESERVED
                    }
                    FilterType.CHECKED_IN -> group.tickets.filter {
                        it.status == TicketStatus.CHECKED_IN
                    }
                    FilterType.CANCELLED -> group.tickets.filter {
                        it.status == TicketStatus.CANCELLED
                    }
                    else -> group.tickets
                }
                group.copy(tickets = filteredTickets)
            }.filter { it.tickets.isNotEmpty() }
        }

        // Apply search
        if (currentSearchQuery.isNotEmpty()) {
            filtered = filtered.map { group ->
                val searchedTickets = group.tickets.filter { ticket ->
                    ticket.userName.lowercase().contains(currentSearchQuery) ||
                    ticket.userEmail.lowercase().contains(currentSearchQuery) ||
                    ticket.ticketId.lowercase().contains(currentSearchQuery)
                }
                group.copy(tickets = searchedTickets)
            }.filter { it.tickets.isNotEmpty() }
        }

        _uiState.update {
            it.copy(
                bookingGroups = filtered,
                isLoading = false,
                isEmpty = filtered.isEmpty()
            )
        }
    }

    fun refreshBookings() {
        loadBookings()
    }
}
