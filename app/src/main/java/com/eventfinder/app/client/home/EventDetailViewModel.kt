package com.eventfinder.app.client.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.model.Ticket
import com.eventfinder.app.domain.usecase.GetEventByIdUseCase
import com.eventfinder.app.domain.usecase.ticket.GetUserTicketsUseCase
import com.eventfinder.app.domain.usecase.ticket.PurchaseTicketUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventDetailUiState(
    val event: Event? = null,
    val userTicket: Ticket? = null,
    val isLoading: Boolean = false,
    val isPurchasing: Boolean = false,
    val error: String? = null,
    val purchaseSuccess: Boolean = false
)

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val getEventByIdUseCase: GetEventByIdUseCase,
    private val getUserTicketsUseCase: GetUserTicketsUseCase,
    private val purchaseTicketUseCase: PurchaseTicketUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventDetailUiState())
    val uiState: StateFlow<EventDetailUiState> = _uiState.asStateFlow()

    fun loadEvent(eventId: String, userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            getEventByIdUseCase(eventId).fold(
                onSuccess = { event ->
                    if (event != null) {
                        _uiState.update { it.copy(event = event, isLoading = false) }
                        // Check if user already has a ticket for this event
                        checkUserTicket(eventId, userId)
                    } else {
                        _uiState.update { it.copy(error = "Event not found", isLoading = false) }
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(error = exception.message ?: "Failed to load event", isLoading = false)
                    }
                }
            )
        }
    }

    private suspend fun checkUserTicket(eventId: String, userId: String) {
        getUserTicketsUseCase(userId).fold(
            onSuccess = { tickets ->
                // Find ticket for this event
                val ticket = tickets.firstOrNull { it.eventId == eventId }
                _uiState.update { it.copy(userTicket = ticket) }
            },
            onFailure = {
                // Ignore error, just means no ticket found
                _uiState.update { it.copy(userTicket = null) }
            }
        )
    }

    fun purchaseTicket(
        event: Event,
        userId: String,
        userName: String,
        userEmail: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPurchasing = true, error = null, purchaseSuccess = false) }

            purchaseTicketUseCase(
                event = event,
                userId = userId,
                userName = userName,
                userEmail = userEmail
            ).fold(
                onSuccess = { ticket ->
                    _uiState.update {
                        it.copy(
                            userTicket = ticket,
                            isPurchasing = false,
                            purchaseSuccess = true
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isPurchasing = false,
                            error = exception.message ?: "Failed to purchase ticket"
                        )
                    }
                }
            )
        }
    }

    fun resetPurchaseSuccess() {
        _uiState.update { it.copy(purchaseSuccess = false) }
    }
}
