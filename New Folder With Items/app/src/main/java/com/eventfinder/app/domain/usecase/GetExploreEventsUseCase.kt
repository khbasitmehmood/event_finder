package com.eventfinder.app.domain.usecase

import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.repository.EventRepository
import javax.inject.Inject

/**
 * Use case for getting explore events
 */
class GetExploreEventsUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(): Result<List<Event>> = eventRepository.getExploreEvents()
}
