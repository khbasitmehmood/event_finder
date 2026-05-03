package com.eventfinder.app.domain.usecase.ticket

import com.eventfinder.app.domain.model.Ticket
import com.eventfinder.app.domain.repository.TicketRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for retrieving all attendees (tickets) for a specific event
 */
@Singleton
class GetEventAttendeesUseCase @Inject constructor(
    private val ticketRepository: TicketRepository
) {

    suspend operator fun invoke(eventId: String): Result<List<Ticket>> {
        return ticketRepository.getEventAttendees(eventId)
    }
}
