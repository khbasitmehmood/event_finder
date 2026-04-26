package com.eventfinder.app.domain.usecase

import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.repository.EventRepository
import javax.inject.Inject

/**
 * Use case for getting events created by the current user
 */
class GetUserEventsUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(userId: String): Result<List<Event>> {
        return eventRepository.getUserEvents(userId)
    }
}
