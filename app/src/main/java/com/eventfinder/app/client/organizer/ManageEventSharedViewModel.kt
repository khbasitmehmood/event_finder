package com.eventfinder.app.client.organizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.model.EventLocation
import com.eventfinder.app.domain.model.EventStats
import com.eventfinder.app.domain.model.Ticket
import com.eventfinder.app.domain.repository.TicketRepository
import com.eventfinder.app.domain.usecase.CancelEventUseCase
import com.eventfinder.app.domain.usecase.PostponeEventUseCase
import com.eventfinder.app.domain.usecase.RescheduleEventUseCase
import com.eventfinder.app.utils.NetworkConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageEventSharedViewModel @Inject constructor(
    private val ticketRepository: TicketRepository,
    private val networkObserver: NetworkConnectivityObserver,
    private val postponeEventUseCase: PostponeEventUseCase,
    private val rescheduleEventUseCase: RescheduleEventUseCase,
    private val cancelEventUseCase: CancelEventUseCase
) : ViewModel() {

    private val _attendees = MutableStateFlow<List<Ticket>>(emptyList())
    val attendees: StateFlow<List<Ticket>> = _attendees.asStateFlow()

    private val _eventStats = MutableStateFlow<EventStats?>(null)
    val eventStats: StateFlow<EventStats?> = _eventStats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var currentEventId: String? = null

    init {
        // Observe network connectivity
        viewModelScope.launch {
            networkObserver.observe().collect { isConnected ->
                _isOnline.value = isConnected
            }
        }
    }

    fun loadEventData(eventId: String) {
        if (currentEventId == eventId) {
            return // Already observing this event
        }
        currentEventId = eventId

        _isLoading.value = true
        _error.value = null

        // Start observing attendees in real-time
        viewModelScope.launch {
            ticketRepository.observeEventAttendees(eventId).collect { result ->
                _isLoading.value = false
                result.fold(
                    onSuccess = { attendees ->
                        _attendees.value = attendees
                    },
                    onFailure = { exception ->
                        _error.value = exception.message ?: "Failed to load attendees"
                    }
                )
            }
        }

        // Start observing stats in real-time
        viewModelScope.launch {
            ticketRepository.observeEventStats(eventId).collect { result ->
                result.fold(
                    onSuccess = { stats ->
                        _eventStats.value = stats
                    },
                    onFailure = { exception ->
                        if (_error.value == null) {
                            _error.value = exception.message ?: "Failed to load stats"
                        }
                    }
                )
            }
        }
    }

    fun refreshData() {
        currentEventId?.let { eventId ->
            viewModelScope.launch {
                _isLoading.value = true
                _error.value = null

                ticketRepository.getEventAttendees(eventId).fold(
                    onSuccess = { attendees ->
                        _attendees.value = attendees
                    },
                    onFailure = { exception ->
                        _error.value = exception.message ?: "Failed to load attendees"
                    }
                )

                ticketRepository.getEventStats(eventId).fold(
                    onSuccess = { stats ->
                        _eventStats.value = stats
                    },
                    onFailure = { exception ->
                        if (_error.value == null) {
                            _error.value = exception.message ?: "Failed to load stats"
                        }
                    }
                )

                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun postponeEvent(
        eventId: String,
        newStartTime: Long?,
        newEndTime: Long?,
        reason: String,
        userId: String,
        onSuccess: (Event) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            postponeEventUseCase(
                eventId = eventId,
                newStartTime = newStartTime,
                newEndTime = newEndTime,
                reason = reason,
                userId = userId
            ).fold(
                onSuccess = { event ->
                    _isLoading.value = false
                    onSuccess(event)
                },
                onFailure = { exception ->
                    _isLoading.value = false
                    val errorMessage = exception.message ?: "Failed to postpone event"
                    _error.value = errorMessage
                    onError(errorMessage)
                }
            )
        }
    }

    fun rescheduleEvent(
        eventId: String,
        newStartTime: Long,
        newEndTime: Long?,
        newLocation: EventLocation?,
        newAddress: String?,
        reason: String,
        userId: String,
        onSuccess: (Event) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            rescheduleEventUseCase(
                eventId = eventId,
                newStartTime = newStartTime,
                newEndTime = newEndTime,
                newLocation = newLocation,
                newAddress = newAddress,
                reason = reason,
                userId = userId
            ).fold(
                onSuccess = { event ->
                    _isLoading.value = false
                    onSuccess(event)
                },
                onFailure = { exception ->
                    _isLoading.value = false
                    val errorMessage = exception.message ?: "Failed to reschedule event"
                    _error.value = errorMessage
                    onError(errorMessage)
                }
            )
        }
    }

    fun cancelEvent(
        eventId: String,
        reason: String,
        userId: String,
        onSuccess: (Event) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            cancelEventUseCase(
                eventId = eventId,
                reason = reason,
                userId = userId
            ).fold(
                onSuccess = { event ->
                    _isLoading.value = false
                    onSuccess(event)
                },
                onFailure = { exception ->
                    _isLoading.value = false
                    val errorMessage = exception.message ?: "Failed to cancel event"
                    _error.value = errorMessage
                    onError(errorMessage)
                }
            )
        }
    }
}
