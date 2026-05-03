package com.eventfinder.app.domain.usecase.ticket

import com.eventfinder.app.domain.model.Ticket
import com.eventfinder.app.domain.model.TicketStatus
import com.eventfinder.app.domain.repository.TicketRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for checking in an attendee at an event
 */
@Singleton
class CheckInAttendeeUseCase @Inject constructor(
    private val ticketRepository: TicketRepository
) {

    suspend operator fun invoke(
        ticketId: String,
        organizerId: String
    ): Result<Ticket> {
        // First validate the ticket
        val ticketResult = ticketRepository.getTicketById(ticketId)

        if (ticketResult.isFailure) {
            return Result.failure(ticketResult.exceptionOrNull() ?: Exception("Unknown error"))
        }

        val ticket = ticketResult.getOrNull()
            ?: return Result.failure(Exception("Ticket not found"))

        // Check if ticket is already checked in
        if (ticket.status == TicketStatus.CHECKED_IN) {
            return Result.failure(Exception("Ticket already checked in"))
        }

        // Check if ticket is cancelled
        if (ticket.status == TicketStatus.CANCELLED) {
            return Result.failure(Exception("Ticket has been cancelled"))
        }

        // Check if ticket is expired
        if (ticket.status == TicketStatus.EXPIRED) {
            return Result.failure(Exception("Ticket has expired"))
        }

        // Verify organizer owns this event
        if (ticket.organizerId != organizerId) {
            return Result.failure(Exception("Unauthorized: You are not the organizer of this event"))
        }

        // Check in the ticket
        return ticketRepository.checkInTicket(ticketId, organizerId)
    }
}
