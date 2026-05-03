package com.eventfinder.app.client.tickets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventfinder.app.domain.model.Ticket
import com.eventfinder.app.domain.usecase.ticket.CancelTicketUseCase
import com.eventfinder.app.domain.repository.TicketRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TicketDetailUiState(
    val ticket: Ticket? = null,
    val isLoading: Boolean = false,
    val isCancelling: Boolean = false,
    val error: String? = null,
    val cancelSuccess: Boolean = false
)

@HiltViewModel
class TicketDetailViewModel @Inject constructor(
    private val ticketRepository: TicketRepository,
    private val cancelTicketUseCase: CancelTicketUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TicketDetailUiState())
    val uiState: StateFlow<TicketDetailUiState> = _uiState.asStateFlow()

    fun loadTicket(ticketId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            ticketRepository.getTicketById(ticketId).fold(
                onSuccess = { ticket ->
                    if (ticket != null) {
                        _uiState.update { it.copy(ticket = ticket, isLoading = false) }
                    } else {
                        _uiState.update { it.copy(error = "Ticket not found", isLoading = false) }
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(error = exception.message ?: "Failed to load ticket", isLoading = false)
                    }
                }
            )
        }
    }

    fun cancelTicket(ticketId: String, userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCancelling = true, error = null, cancelSuccess = false) }

            cancelTicketUseCase(ticketId, userId).fold(
                onSuccess = {
                    // Reload ticket to show updated status
                    loadTicket(ticketId)
                    _uiState.update { it.copy(isCancelling = false, cancelSuccess = true) }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isCancelling = false,
                            error = exception.message ?: "Failed to cancel ticket"
                        )
                    }
                }
            )
        }
    }

    fun resetCancelSuccess() {
        _uiState.update { it.copy(cancelSuccess = false) }
    }
}
