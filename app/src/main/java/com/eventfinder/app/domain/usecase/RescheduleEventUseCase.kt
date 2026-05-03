package com.eventfinder.app.domain.usecase

import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.model.EventLocation
import com.eventfinder.app.domain.model.EventReschedule
import com.eventfinder.app.domain.repository.EventRepository
import javax.inject.Inject

/**
 * Use case for rescheduling an event
 * Allows changing date, time, and optionally location
 */
class RescheduleEventUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    /**
     * Reschedule an event with validation
     *
     * @param eventId ID of the event to reschedule
     * @param newStartTime New start time (required)
     * @param newEndTime New end time (optional)
     * @param newLocation New location (optional)
     * @param newAddress New address (optional)
     * @param reason Reason for rescheduling (required)
     * @param userId Organizer's user ID
     * @return Result with updated event or error
     */
    suspend operator fun invoke(
        eventId: String,
        newStartTime: Long,
        newEndTime: Long?,
        newLocation: EventLocation?,
        newAddress: String?,
        reason: String,
        userId: String
    ): Result<Event> {
        return try {
            // Validate reason
            if (reason.isBlank()) {
                return Result.failure(Exception("Reason for rescheduling is required"))
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

            // Validate event can be rescheduled
            if (!currentEvent.canReschedule()) {
                return Result.failure(
                    Exception(
                        when {
                            !currentEvent.allowReschedule -> "Rescheduling is not allowed for this event"
                            !currentEvent.state.canReschedule() -> "Event in ${currentEvent.state.getDisplayName()} state cannot be rescheduled"
                            currentEvent.rescheduleCount >= currentEvent.maxReschedules ->
                                "Maximum reschedules (${currentEvent.maxReschedules}) reached"
                            currentEvent.hasEnded() -> "Event has already ended"
                            else -> "Event cannot be rescheduled"
                        }
                    )
                )
            }

            // Validate new times
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

            // Check if anything actually changed
            val hasChanges = newStartTime != currentEvent.startTime ||
                    newEndTime != currentEvent.endTime ||
                    newLocation != currentEvent.location ||
                    newAddress != currentEvent.address

            if (!hasChanges) {
                return Result.failure(
                    Exception("No changes detected. Please modify at least one field.")
                )
            }

            // Create reschedule record
            val reschedule = EventReschedule(
                originalStartTime = currentEvent.startTime,
                originalEndTime = currentEvent.endTime,
                originalLocation = currentEvent.location,
                originalAddress = currentEvent.address,
                newStartTime = newStartTime,
                newEndTime = newEndTime,
                newLocation = newLocation,
                newAddress = newAddress,
                reason = reason.trim(),
                rescheduledAt = System.currentTimeMillis(),
                rescheduledBy = userId,
                notificationSent = false
            )

            // Reschedule the event
            eventRepository.rescheduleEvent(
                eventId = eventId,
                reschedule = reschedule
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validate rescheduling is allowed
     */
    fun canReschedule(event: Event): Boolean = event.canReschedule()

    /**
     * Get validation errors for rescheduling
     */
    fun getRescheduleValidationErrors(
        event: Event,
        newStartTime: Long,
        newEndTime: Long?,
        reason: String
    ): List<String> {
        val errors = mutableListOf<String>()

        if (!event.allowReschedule) {
            errors.add("Rescheduling is not allowed for this event")
        }

        if (!event.state.canReschedule()) {
            errors.add("Event in ${event.state.getDisplayName()} state cannot be rescheduled")
        }

        if (event.rescheduleCount >= event.maxReschedules) {
            errors.add("Maximum reschedules (${event.maxReschedules}) reached")
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

        val now = System.currentTimeMillis()
        val oneHourFromNow = now + (60 * 60 * 1000)

        if (newStartTime < oneHourFromNow) {
            errors.add("New start time must be at least 1 hour from now")
        }

        if (newEndTime != null && newEndTime <= newStartTime) {
            errors.add("End time must be after start time")
        }

        return errors
    }
}
