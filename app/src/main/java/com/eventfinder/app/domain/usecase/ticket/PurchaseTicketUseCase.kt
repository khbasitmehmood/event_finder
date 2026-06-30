package com.eventfinder.app.domain.usecase.ticket

import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.model.EventVisibility
import com.eventfinder.app.domain.model.PaymentReceipt
import com.eventfinder.app.domain.model.PaymentStatus
import com.eventfinder.app.domain.model.Ticket
import com.eventfinder.app.domain.model.TicketStatus
import com.eventfinder.app.domain.model.TicketType
import com.eventfinder.app.domain.repository.TicketRepository
import com.eventfinder.app.domain.service.NotificationService
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for purchasing or reserving a ticket for an event
 */
@Singleton
class PurchaseTicketUseCase @Inject constructor(
    private val ticketRepository: TicketRepository,
    private val notificationService: NotificationService
) {

    suspend operator fun invoke(
        event: Event,
        userId: String,
        userName: String,
        userEmail: String,
        paymentReceipt: PaymentReceipt? = null
    ): Result<Ticket> {
        // Generate unique QR code
        val qrCodeData = generateUniqueQRCode(event.eventId, userId)

        // Determine ticket type based on event visibility and pricing
        val ticketType = when {
            event.visibility == EventVisibility.PUBLIC && !event.requiresTicket -> {
                TicketType.PUBLIC_RESERVATION
            }
            event.requiresPaidCheckout() -> TicketType.PAID
            else -> TicketType.FREE_PRIVATE
        }

        // Determine status
        val status = when (ticketType) {
            TicketType.PUBLIC_RESERVATION -> TicketStatus.RESERVED
            else -> TicketStatus.PURCHASED
        }

        val price = event.price ?: 0.0
        val currency = event.currency ?: "PKR"
        if (ticketType == TicketType.PAID && paymentReceipt?.status != PaymentStatus.PAID) {
            return Result.failure(IllegalStateException("Payment must be completed before ticket purchase"))
        }

        // Create ticket
        val ticket = Ticket(
            ticketId = UUID.randomUUID().toString(),
            eventId = event.eventId,
            eventTitle = event.title,
            eventStartTime = event.startTime,
            userId = userId,
            userName = userName,
            userEmail = userEmail,
            ticketType = ticketType,
            status = status,
            qrCodeData = qrCodeData,
            purchasePrice = price,
            currency = currency,
            paymentStatus = paymentReceipt?.status ?: PaymentStatus.NOT_REQUIRED,
            paymentProvider = paymentReceipt?.provider,
            paymentTransactionId = paymentReceipt?.transactionId,
            paidAt = paymentReceipt?.paidAt,
            purchasedAt = System.currentTimeMillis(),
            eventLocation = event.address,
            organizerId = event.organizerId,
            organizerName = event.organizerName
        )

        // Save ticket
        val result = ticketRepository.createTicket(ticket)

        // Update event stats if successful
        if (result.isSuccess) {
            ticketRepository.incrementEventStats(
                eventId = event.eventId,
                ticketType = ticketType,
                amount = ticket.purchasePrice
            )
            notificationService.notifyTicketCreated(
                event = event,
                buyerName = userName,
                ticketType = ticketType.name,
                amount = ticket.purchasePrice,
                currency = ticket.currency
            ).onFailure { error ->
                android.util.Log.e("PurchaseTicketUseCase", "Failed to notify organizer about ticket", error)
            }
        }

        return result
    }

    /**
     * Generate a unique QR code data string
     * Format: eventId_userId_randomUUID_timestamp
     */
    private fun generateUniqueQRCode(eventId: String, userId: String): String {
        return "${eventId}_${userId}_${UUID.randomUUID()}_${System.currentTimeMillis()}"
    }
}
