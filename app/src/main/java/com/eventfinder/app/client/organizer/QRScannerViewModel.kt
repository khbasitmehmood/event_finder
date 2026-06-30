package com.eventfinder.app.client.organizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventfinder.app.domain.model.PendingCheckIn
import com.eventfinder.app.domain.model.Ticket
import com.eventfinder.app.domain.model.TicketStatus
import com.eventfinder.app.domain.usecase.ticket.CheckInAttendeeUseCase
import com.eventfinder.app.domain.usecase.ticket.ValidateTicketQRUseCase
import com.eventfinder.app.utils.NetworkConnectivityObserver
import com.eventfinder.app.utils.OfflineOperationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QRScannerUiState(
    val scannedTicket: Ticket? = null,
    val isValidating: Boolean = false,
    val isCheckingIn: Boolean = false,
    val error: String? = null,
    val checkInSuccess: Boolean = false,
    val scannerActive: Boolean = true
)

@HiltViewModel
class QRScannerViewModel @Inject constructor(
    private val validateTicketQRUseCase: ValidateTicketQRUseCase,
    private val checkInAttendeeUseCase: CheckInAttendeeUseCase,
    private val networkObserver: NetworkConnectivityObserver,
    private val offlineManager: OfflineOperationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(QRScannerUiState())
    val uiState: StateFlow<QRScannerUiState> = _uiState.asStateFlow()

    fun validateQRCode(qrCodeData: String, organizerId: String, expectedEventId: String? = null) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isValidating = true,
                    error = null,
                    scannerActive = false,
                    checkInSuccess = false
                )
            }

            validateTicketQRUseCase(qrCodeData).fold(
                onSuccess = { ticket ->
                    if (ticket == null) {
                        _uiState.update {
                            it.copy(
                                isValidating = false,
                                error = "Invalid QR code",
                                scannerActive = true
                            )
                        }
                        return@fold
                    }

                    // Validate ticket belongs to this organizer
                    if (ticket.organizerId != organizerId) {
                        _uiState.update {
                            it.copy(
                                isValidating = false,
                                error = "This ticket is for a different event",
                                scannerActive = true
                            )
                        }
                        return@fold
                    }

                    // Validate ticket belongs to the selected event when scanner is event-scoped
                    if (!expectedEventId.isNullOrBlank() && ticket.eventId != expectedEventId) {
                        _uiState.update {
                            it.copy(
                                isValidating = false,
                                error = "This ticket is for a different event",
                                scannerActive = true
                            )
                        }
                        return@fold
                    }

                    // Check ticket status
                    when (ticket.status) {
                        TicketStatus.CHECKED_IN -> {
                            _uiState.update {
                                it.copy(
                                    scannedTicket = ticket,
                                    isValidating = false,
                                    error = "Already checked in",
                                    scannerActive = false
                                )
                            }
                        }
                        TicketStatus.CANCELLED -> {
                            _uiState.update {
                                it.copy(
                                    isValidating = false,
                                    error = "Ticket has been cancelled",
                                    scannerActive = true
                                )
                            }
                        }
                        TicketStatus.EXPIRED -> {
                            _uiState.update {
                                it.copy(
                                    isValidating = false,
                                    error = "Ticket has expired",
                                    scannerActive = true
                                )
                            }
                        }
                        else -> {
                            // Valid ticket
                            _uiState.update {
                                it.copy(
                                    scannedTicket = ticket,
                                    isValidating = false,
                                    scannerActive = false
                                )
                            }
                        }
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isValidating = false,
                            error = exception.message ?: "Failed to validate ticket",
                            scannerActive = true
                        )
                    }
                }
            )
        }
    }

    fun checkInAttendee(ticketId: String, organizerId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingIn = true, error = null) }

            val isOnline = networkObserver.isCurrentlyConnected()
            val currentTicket = _uiState.value.scannedTicket

            if (!isOnline && currentTicket != null) {
                // Queue for later when online
                offlineManager.queueCheckIn(
                    PendingCheckIn(
                        ticketId = ticketId,
                        organizerId = organizerId,
                        eventId = currentTicket.eventId
                    )
                )

                // Update UI optimistically
                _uiState.update {
                    it.copy(
                        scannedTicket = currentTicket.copy(status = TicketStatus.CHECKED_IN),
                        isCheckingIn = false,
                        checkInSuccess = true,
                        scannerActive = false,
                        error = "Queued for sync when online"
                    )
                }
            } else {
                // Online - perform check-in immediately
                checkInAttendeeUseCase(ticketId, organizerId).fold(
                    onSuccess = { updatedTicket ->
                        _uiState.update {
                            it.copy(
                                scannedTicket = updatedTicket,
                                isCheckingIn = false,
                                checkInSuccess = true,
                                scannerActive = false
                            )
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update {
                            it.copy(
                                isCheckingIn = false,
                                error = exception.message ?: "Failed to check in attendee"
                            )
                        }
                    }
                )
            }
        }
    }

    fun resetScanner() {
        _uiState.update {
            QRScannerUiState(scannerActive = true)
        }
    }

    fun dismissResult() {
        _uiState.update {
            it.copy(
                scannedTicket = null,
                error = null,
                checkInSuccess = false,
                scannerActive = true
            )
        }
    }
}
