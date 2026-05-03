package com.eventfinder.app.domain.usecase

import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.model.EventPostponement
import com.eventfinder.app.domain.model.EventState
import com.eventfinder.app.domain.repository.EventRepository
import javax.inject.Inject

/**
 * Use case for postponing an event
 */
class PostponeEventUseCase @Inject constructor(
    private val eventRepository: EventRepository,
    private val updateEventStateUseCase: UpdateEventStateUseCase
) {
    /**
     * Postpone an event with validation
     *
     * @param eventId ID of the event to postpone
     * @param newStartTime New start time (null for TBD)
     * @param newEndTime New end time (null for TBD or no end time)
     * @param reason Reason for postponement (required)
     * @param userId Organizer's user ID
     * @return Result with updated event or error
     */
    suspend operator fun invoke(
        eventId: String,
        newStartTime: Long?,
        newEndTime: Long?,
        reason: String,
        userId: String
    ): Result<Event> {
        return try {
            // Validate inputs
            if (reason.isBlank()) {
                return Result.failure(Exception("Reason for postponement is required"))
            }

            if (reason.length < 10) {
                return Result.failure(Exception("Reason must be at least 10 characters"))
            }

            if (reason.length > 500) {
                return Result.failure(Exception("Reason must not exceed 500 characters"))
            }

            // Get current event
            val currentEventResult = eventRepository.getEventById(eventId)
            val currentEvent = currentEventResult.getOrNull()
                ?: return Result.failure(Exception("Event not found"))

            // Log event state for debugging
            android.util.Log.d("PostponeEventUseCase", "Event state: ${currentEvent.state}, allowPostponement: ${currentEvent.allowPostponement}, postponementCount: ${currentEvent.postponementCount}, hasEnded: ${currentEvent.hasEnded()}")

            // Validate event can be postponed
            if (!currentEvent.canPostpone()) {
                return Result.failure(
                    Exception(
                        when {
                            !currentEvent.allowPostponement -> "Postponement is not allowed for this event"
                            !currentEvent.state.canPostpone() -> "Event in ${currentEvent.state.getDisplayName()} state cannot be postponed"
                            currentEvent.postponementCount >= currentEvent.maxPostponements ->
                                "Maximum postponements (${currentEvent.maxPostponements}) reached"
                            currentEvent.hasEnded() -> "Event has already ended"
                            else -> "Event cannot be postponed"
                        }
                    )
                )
            }

            // Validate new times if provided
            if (newStartTime != null) {
                val now = System.currentTimeMillis()
                val oneHourFromNow = now + (60 * 60 * 1000)

                if (newStartTime < oneHourFromNow) {
                    return Result.failure(
                        Exception("New start time must be at least 1 hour from now")
                    )
                }

                // Validate end time if provided
                if (newEndTime != null && newEndTime <= newStartTime) {
                    return Result.failure(
                        Exception("End time must be after start time")
                    )
                }
            }

            // Create postponement record
            val postponement = EventPostponement(
                originalStartTime = currentEvent.startTime,
                originalEndTime = currentEvent.endTime,
                newStartTime = newStartTime,
                newEndTime = newEndTime,
                reason = reason.trim(),
                postponedAt = System.currentTimeMillis(),
                postponedBy = userId,
                notificationSent = false
            )

            // Postpone the event
            eventRepository.postponeEvent(
                eventId = eventId,
                postponement = postponement
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validate postponement is allowed
     */
    fun canPostpone(event: Event): Boolean = event.canPostpone()

    /**
     * Get validation errors for postponement
     */
    fun getPostponeValidationErrors(
        event: Event,
        newStartTime: Long?,
        reason: String
    ): List<String> {
        val errors = mutableListOf<String>()

        if (!event.allowPostponement) {
            errors.add("Postponement is not allowed for this event")
        }

        if (!event.state.canPostpone()) {
            errors.add("Event in ${event.state.getDisplayName()} state cannot be postponed")
        }

        if (event.postponementCount >= event.maxPostponements) {
            errors.add("Maximum postponements (${event.maxPostponements}) reached")
        }

        if (event.hasEnded()) {
            errors.add("Event has already ended")
        }

        if (reason.isBlank()) {
            errors.add("Reason is required")
        } else if (reason.length < 10) {
            errors.add("Reason must be at least 10 characters")
        } else if (reason.length > 500) {
            errors.add("Reason must not exceed 500 characters")
        }

        if (newStartTime != null) {
            val now = System.currentTimeMillis()
            val oneHourFromNow = now + (60 * 60 * 1000)

            if (newStartTime < oneHourFromNow) {
                errors.add("New start time must be at least 1 hour from now")
            }
        }

        return errors
    }
}
