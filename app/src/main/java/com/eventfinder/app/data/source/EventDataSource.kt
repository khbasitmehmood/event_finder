package com.eventfinder.app.data.source

import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.model.EventCategory
import com.eventfinder.app.domain.model.EventState

/**
 * Data source interface for events
 */
interface EventDataSource {
    suspend fun getEvents(): List<Event>
    suspend fun getEventById(id: String): Event?
    suspend fun searchEvents(query: String): List<Event>
    suspend fun getEventsByCategory(category: EventCategory): List<Event>
    suspend fun getNearbyEvents(latitude: Double, longitude: Double, radiusKm: Double): List<Event>
    suspend fun createEvent(event: Event): Event
    suspend fun getUserEvents(userId: String): List<Event>
    suspend fun updateEvent(event: Event): Event
    suspend fun deleteEvent(eventId: String)

    // Phase 1: State Management
    suspend fun updateEventState(
        eventId: String,
        newState: EventState,
        reason: String?,
        changedBy: String?,
        automatic: Boolean
    ): Event

    suspend fun getEventsByState(state: EventState): List<Event>
    suspend fun getEventsByStates(states: List<EventState>): List<Event>
    suspend fun getOrganizerEventsByState(organizerId: String, state: EventState): List<Event>

    // Phase 2: Postponement
    suspend fun postponeEvent(
        eventId: String,
        postponement: com.eventfinder.app.domain.model.EventPostponement
    ): Event

    // Phase 3: Rescheduling
    suspend fun rescheduleEvent(
        eventId: String,
        reschedule: com.eventfinder.app.domain.model.EventReschedule
    ): Event

    // Phase 4: Cancellation
    suspend fun cancelEvent(
        eventId: String,
        cancellation: com.eventfinder.app.domain.model.EventCancellation
    ): Event

    // Draft/Publish Management
    suspend fun publishEvent(eventId: String, organizerId: String): Event
}
