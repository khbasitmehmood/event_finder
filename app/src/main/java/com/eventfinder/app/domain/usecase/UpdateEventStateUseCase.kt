package com.eventfinder.app.domain.usecase

import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.model.EventState
import com.eventfinder.app.domain.model.StateChange
import com.eventfinder.app.domain.repository.EventRepository
import javax.inject.Inject

/**
 * Use case for updating event state with validation
 */
class UpdateEventStateUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    /**
     * Update event state with validation
     *
     * @param eventId ID of the event to update
     * @param newState New state to transition to
     * @param reason Optional reason for state change
     * @param userId User ID triggering the change (null for automatic)
     * @param automatic Whether this is an automatic transition
     * @return Result with updated event or error
     */
    suspend operator fun invoke(
        eventId: String,
        newState: EventState,
        reason: String? = null,
        userId: String? = null,
        automatic: Boolean = false
    ): Result<Event> {
        return try {
            // Get current event
            val currentEventResult = eventRepository.getEventById(eventId)
            val currentEvent = currentEventResult.getOrNull()
                ?: return Result.failure(Exception("Event not found"))

            // Validate state transition
            val stateChange = StateChange(
                fromState = currentEvent.state,
                toState = newState,
                changedAt = System.currentTimeMillis(),
                changedBy = userId,
                reason = reason,
                automatic = automatic
            )

            if (!stateChange.isValid()) {
                return Result.failure(
                    Exception("Invalid state transition: ${currentEvent.state} → $newState")
                )
            }

            // Perform state update
            eventRepository.updateEventState(
                eventId = eventId,
                newState = newState,
                reason = reason,
                changedBy = userId,
                automatic = automatic
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validate if a state transition is allowed
     */
    fun canTransition(fromState: EventState, toState: EventState): Boolean {
        val stateChange = StateChange(
            fromState = fromState,
            toState = toState
        )
        return stateChange.isValid()
    }

    /**
     * Get allowed next states for an event
     */
    fun getAllowedNextStates(currentState: EventState): List<EventState> {
        return EventState.values().filter { nextState ->
            canTransition(currentState, nextState)
        }
    }
}
