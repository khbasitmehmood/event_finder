package com.eventfinder.app.client.tickets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventfinder.app.domain.model.Ticket
import com.eventfinder.app.domain.model.TicketStatus
import com.eventfinder.app.domain.usecase.ticket.GetUserTicketsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TicketsUiState(
    val allTickets: List<Ticket> = emptyList(),
    val upcomingTickets: List<Ticket> = emptyList(),
    val pastTickets: List<Ticket> = emptyList(),
    val cancelledTickets: List<Ticket> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TicketsViewModel @Inject constructor(
    private val getUserTicketsUseCase: GetUserTicketsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TicketsUiState())
    val uiState: StateFlow<TicketsUiState> = _uiState.asStateFlow()

    fun loadUserTickets(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            getUserTicketsUseCase(userId).fold(
                onSuccess = { tickets ->
                    val currentTime = System.currentTimeMillis()

                    val upcoming = tickets.filter {
                        it.status != TicketStatus.CANCELLED &&
                                it.eventStartTime > currentTime
                    }

                    val past = tickets.filter {
                        it.status != TicketStatus.CANCELLED &&
                                it.eventStartTime <= currentTime
                    }

                    val cancelled = tickets.filter {
                        it.status == TicketStatus.CANCELLED
                    }

                    _uiState.update {
                        it.copy(
                            allTickets = tickets,
                            upcomingTickets = upcoming,
                            pastTickets = past,
                            cancelledTickets = cancelled,
                            isLoading = false
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            error = exception.message ?: "Failed to load tickets",
                            isLoading = false
                        )
                    }
                }
            )
        }
    }

    fun refreshTickets(userId: String) {
        loadUserTickets(userId)
    }
}
