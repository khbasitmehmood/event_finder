package com.eventfinder.app.domain.usecase.ticket

import com.eventfinder.app.domain.model.EventStats
import com.eventfinder.app.domain.repository.TicketRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for retrieving event statistics
 */
@Singleton
class GetEventStatsUseCase @Inject constructor(
    private val ticketRepository: TicketRepository
) {

    suspend operator fun invoke(eventId: String): Result<EventStats> {
        return ticketRepository.getEventStats(eventId)
    }
}
