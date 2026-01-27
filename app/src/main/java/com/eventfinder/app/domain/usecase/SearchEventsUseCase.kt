package com.eventfinder.app.domain.usecase

import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.repository.EventRepository
import javax.inject.Inject

/**
 * Use case for searching events
 */
class SearchEventsUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(query: String): Result<List<Event>> {
        return eventRepository.searchEvents(query)
    }
}
