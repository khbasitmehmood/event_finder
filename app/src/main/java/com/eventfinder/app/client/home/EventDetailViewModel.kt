package com.eventfinder.app.client.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.model.PaymentCheckout
import com.eventfinder.app.domain.model.PaymentRequest
import com.eventfinder.app.domain.model.PaymentStatus
import com.eventfinder.app.domain.model.Ticket
import com.eventfinder.app.domain.repository.PaymentGateway
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
    val checkout: PaymentCheckout? = null,
    val checkoutInProgress: Boolean = false,
    val checkoutStatusMessage: String? = null,
    val error: String? = null,
    val purchaseSuccess: Boolean = false,
    val completedCheckoutId: String? = null,
    val failedCheckoutId: String? = null,
    val confirmedTicketId: String? = null
)

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val getEventByIdUseCase: GetEventByIdUseCase,
    private val getUserTicketsUseCase: GetUserTicketsUseCase,
    private val purchaseTicketUseCase: PurchaseTicketUseCase,
    private val paymentGateway: PaymentGateway
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventDetailUiState())
    val uiState: StateFlow<EventDetailUiState> = _uiState.asStateFlow()
    private val tag = "EventDetailPayment"
    private var currentUserId: String? = null

    fun loadEvent(eventId: String, userId: String) {
        currentUserId = userId
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

    fun refreshUserTicket(eventId: String, userId: String = currentUserId.orEmpty()) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            checkUserTicket(eventId, userId)
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
                            purchaseSuccess = true,
                            confirmedTicketId = ticket.ticketId
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

    fun startPaidCheckout(
        event: Event,
        userId: String,
        userName: String,
        userEmail: String
    ) {
        viewModelScope.launch {
            val amount = event.price ?: 0.0
            val currency = event.currency ?: "PKR"

            Log.d(
                tag,
                "startPaidCheckout eventId=${event.eventId} title=${event.title} " +
                    "visibility=${event.visibility} requiresTicket=${event.requiresTicket} " +
                    "isFree=${event.isFree} price=$amount currency=$currency " +
                    "requiresPaidCheckout=${event.requiresPaidCheckout()}"
            )

            if (!event.requiresPaidCheckout()) {
                Log.d(tag, "Bypassing Safepay and creating ticket directly for eventId=${event.eventId}")
                purchaseTicket(event, userId, userName, userEmail)
                return@launch
            }

            _uiState.update {
                it.copy(
                    isPurchasing = true,
                    checkout = null,
                    checkoutInProgress = false,
                    checkoutStatusMessage = null,
                    error = null,
                    purchaseSuccess = false,
                    completedCheckoutId = null,
                    failedCheckoutId = null,
                    confirmedTicketId = null
                )
            }

            paymentGateway.createCheckout(
                PaymentRequest(
                    eventId = event.eventId,
                    eventTitle = event.title,
                    userId = userId,
                    userName = userName,
                    userEmail = userEmail,
                    amount = amount,
                    currency = currency,
                    description = "Ticket for ${event.title}"
                )
            ).fold(
                onSuccess = { checkout ->
                    Log.d(
                        tag,
                        "Checkout created checkoutId=${checkout.checkoutId} transactionId=${checkout.transactionId}"
                    )
                    _uiState.update {
                        it.copy(
                            checkout = checkout,
                            checkoutInProgress = true,
                            checkoutStatusMessage = "Safepay checkout opened. Complete payment to receive your ticket.",
                            isPurchasing = false
                        )
                    }
                },
                onFailure = { exception ->
                    Log.e(tag, "Failed to create checkout for eventId=${event.eventId}", exception)
                    _uiState.update {
                        it.copy(
                            isPurchasing = false,
                            error = exception.message ?: "Failed to start checkout"
                        )
                    }
                }
            )
        }
    }

    fun completePaidCheckout(
        event: Event,
        userId: String,
        userName: String,
        userEmail: String,
        checkoutId: String? = null
    ) {
        val resolvedCheckoutId = checkoutId ?: _uiState.value.checkout?.checkoutId ?: return
        Log.d(tag, "completePaidCheckout checkoutId=$resolvedCheckoutId eventId=${event.eventId}")

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPurchasing = true,
                    error = null,
                    checkoutStatusMessage = "Verifying payment..."
                )
            }

            paymentGateway.getCheckoutReceipt(resolvedCheckoutId).fold(
                onSuccess = { receipt ->
                    Log.d(
                        tag,
                        "Checkout receipt checkoutId=${receipt.checkoutId} status=${receipt.status} " +
                            "ticketId=${receipt.ticketId} transactionId=${receipt.transactionId}"
                    )
                    if (receipt.status != PaymentStatus.PAID) {
                        _uiState.update {
                            it.copy(
                                isPurchasing = false,
                                checkout = null,
                                checkoutInProgress = false,
                                error = "Payment is not complete yet. You can try again.",
                                failedCheckoutId = resolvedCheckoutId
                            )
                        }
                        return@fold
                    }

                    checkUserTicket(event.eventId, userId)
                    _uiState.update {
                        it.copy(
                            checkout = null,
                            checkoutInProgress = false,
                            checkoutStatusMessage = "Payment confirmed. Your ticket is ready.",
                            isPurchasing = false,
                            purchaseSuccess = true,
                            completedCheckoutId = resolvedCheckoutId,
                            failedCheckoutId = null,
                            confirmedTicketId = receipt.ticketId
                        )
                    }
                },
                onFailure = { exception ->
                    Log.e(tag, "Failed to verify checkout checkoutId=$resolvedCheckoutId", exception)
                    _uiState.update {
                        it.copy(
                            isPurchasing = false,
                            checkout = null,
                            checkoutInProgress = false,
                            error = exception.message ?: "Failed to verify payment",
                            failedCheckoutId = resolvedCheckoutId
                        )
                    }
                }
            )
        }
    }

    fun clearCheckoutStatusMessage() {
        _uiState.update { it.copy(checkoutStatusMessage = null) }
    }

    fun resetPurchaseSuccess() {
        _uiState.update { it.copy(purchaseSuccess = false, checkoutStatusMessage = null, completedCheckoutId = null) }
    }

    fun clearFailedCheckout() {
        _uiState.update { it.copy(failedCheckoutId = null) }
    }
}
