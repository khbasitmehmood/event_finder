package com.eventfinder.app.domain.usecase.ticket

import com.eventfinder.app.domain.model.TicketStatus
import com.eventfinder.app.domain.repository.TicketRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for cancelling a ticket
 */
@Singleton
class CancelTicketUseCase @Inject constructor(
    private val ticketRepository: TicketRepository
) {

    suspend operator fun invoke(
        ticketId: String,
        userId: String
    ): Result<Unit> {
        // First verify the ticket belongs to the user
        val ticketResult = ticketRepository.getTicketById(ticketId)

        if (ticketResult.isFailure) {
            return Result.failure(ticketResult.exceptionOrNull() ?: Exception("Unknown error"))
        }

        val ticket = ticketResult.getOrNull()
            ?: return Result.failure(Exception("Ticket not found"))

        // Verify ownership
        if (ticket.userId != userId) {
            return Result.failure(Exception("Unauthorized: This ticket does not belong to you"))
        }

        // Check if ticket is already cancelled
        if (ticket.status == TicketStatus.CANCELLED) {
            return Result.failure(Exception("Ticket is already cancelled"))
        }

        // Check if ticket is checked in (can't cancel after check-in)
        if (ticket.status == TicketStatus.CHECKED_IN) {
            return Result.failure(Exception("Cannot cancel ticket after check-in"))
        }

        // Cancel the ticket
        return ticketRepository.cancelTicket(ticketId, userId)
    }
}
