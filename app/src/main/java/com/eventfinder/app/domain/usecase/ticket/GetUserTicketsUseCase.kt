package com.eventfinder.app.domain.usecase.ticket

import com.eventfinder.app.domain.model.Ticket
import com.eventfinder.app.domain.repository.TicketRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for retrieving all tickets for a specific user
 */
@Singleton
class GetUserTicketsUseCase @Inject constructor(
    private val ticketRepository: TicketRepository
) {

    suspend operator fun invoke(userId: String): Result<List<Ticket>> {
        return ticketRepository.getUserTickets(userId)
    }
}
