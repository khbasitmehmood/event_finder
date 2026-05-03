package com.eventfinder.app.domain.usecase.ticket

import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.model.EventVisibility
import com.eventfinder.app.domain.model.Ticket
import com.eventfinder.app.domain.model.TicketStatus
import com.eventfinder.app.domain.model.TicketType
import com.eventfinder.app.domain.repository.TicketRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for purchasing or reserving a ticket for an event
 */
@Singleton
class PurchaseTicketUseCase @Inject constructor(
    private val ticketRepository: TicketRepository
) {

    suspend operator fun invoke(
        event: Event,
        userId: String,
        userName: String,
        userEmail: String
    ): Result<Ticket> {
        // Generate unique QR code
        val qrCodeData = generateUniqueQRCode(event.eventId, userId)

        // Determine ticket type based on event visibility and pricing
        val ticketType = when {
            event.visibility == EventVisibility.PUBLIC && !event.requiresTicket -> {
                TicketType.PUBLIC_RESERVATION
            }
            event.isFree -> TicketType.FREE_PRIVATE
            else -> TicketType.PAID
        }

        // Determine status
        val status = when (ticketType) {
            TicketType.PUBLIC_RESERVATION -> TicketStatus.RESERVED
            else -> TicketStatus.PURCHASED
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
            purchasePrice = event.price ?: 0.0,
            currency = event.currency ?: "PKR",
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
