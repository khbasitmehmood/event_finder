package com.eventfinder.app.domain.repository

import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.model.EventCategory
import com.eventfinder.app.domain.model.EventState

/**
 * Repository interface for Event operations - defined in domain layer
 */
interface EventRepository {
    suspend fun getExploreEvents(): Result<List<Event>>
    suspend fun getEventById(id: String): Result<Event?>
    suspend fun searchEvents(query: String): Result<List<Event>>
    suspend fun getEventsByCategory(category: EventCategory): Result<List<Event>>
    suspend fun getNearbyEvents(latitude: Double, longitude: Double, radiusKm: Double): Result<List<Event>>
    suspend fun createEvent(event: Event): Result<Event>
    suspend fun getUserEvents(userId: String): Result<List<Event>>
    suspend fun updateEvent(event: Event): Result<Event>
    suspend fun deleteEvent(eventId: String): Result<Unit>

    // Phase 1: State Management
    suspend fun updateEventState(
        eventId: String,
        newState: EventState,
        reason: String? = null,
        changedBy: String? = null,
        automatic: Boolean = false
    ): Result<Event>

    suspend fun getEventsByState(state: EventState): Result<List<Event>>
    suspend fun getEventsByStates(states: List<EventState>): Result<List<Event>>
    suspend fun getOrganizerEventsByState(organizerId: String, state: EventState): Result<List<Event>>

    // Phase 2: Postponement
    suspend fun postponeEvent(
        eventId: String,
        postponement: com.eventfinder.app.domain.model.EventPostponement
    ): Result<Event>

    // Phase 3: Rescheduling
    suspend fun rescheduleEvent(
        eventId: String,
        reschedule: com.eventfinder.app.domain.model.EventReschedule
    ): Result<Event>

    // Phase 4: Cancellation
    suspend fun cancelEvent(
        eventId: String,
        cancellation: com.eventfinder.app.domain.model.EventCancellation
    ): Result<Event>
}
