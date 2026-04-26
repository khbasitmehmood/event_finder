package com.eventfinder.app.domain.usecase

import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.repository.EventRepository
import javax.inject.Inject

/**
 * Use case for creating a new event
 */
class CreateEventUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(event: Event): Result<Event> {
        return try {
            // Validate event data
            validateEvent(event)

            // Create event in repository
            eventRepository.createEvent(event)
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to create event: ${e.message}", e))
        }
    }

    private fun validateEvent(event: Event) {
        require(event.title.isNotBlank()) { "Event title cannot be empty" }
        require(event.organizerId.isNotBlank()) { "Organizer ID is required" }
        require(event.organizerName.isNotBlank()) { "Organizer name is required" }
        require(event.startTime > System.currentTimeMillis()) { "Event start time must be in the future" }
        require(event.location.latitude != 0.0 || event.location.longitude != 0.0) {
            "Valid location is required"
        }
    }
}
