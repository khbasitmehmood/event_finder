package com.eventfinder.app.data.repository

import com.eventfinder.app.data.source.EventDataSource
import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.model.EventCategory
import com.eventfinder.app.domain.model.EventState
import com.eventfinder.app.domain.repository.EventRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of EventRepository
 * Talks to the data source and handles data operations
 */
@Singleton
class EventRepositoryImpl @Inject constructor(
    private val eventDataSource: EventDataSource
) : EventRepository {

    override suspend fun getExploreEvents(): Result<List<Event>> {
        return try {
            val events = eventDataSource.getEvents()
            Result.success(events)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getEventById(id: String): Result<Event?> {
        return try {
            val event = eventDataSource.getEventById(id)
            Result.success(event)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchEvents(query: String): Result<List<Event>> {
        return try {
            val events = eventDataSource.searchEvents(query)
            Result.success(events)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getEventsByCategory(category: EventCategory): Result<List<Event>> {
        return try {
            val events = eventDataSource.getEventsByCategory(category)
            Result.success(events)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getNearbyEvents(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): Result<List<Event>> {
        return try {
            val events = eventDataSource.getNearbyEvents(latitude, longitude, radiusKm)
            Result.success(events)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createEvent(event: Event): Result<Event> {
        return try {
            val createdEvent = eventDataSource.createEvent(event)
            Result.success(createdEvent)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserEvents(userId: String): Result<List<Event>> {
        return try {
            val events = eventDataSource.getUserEvents(userId)
            Result.success(events)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateEvent(event: Event): Result<Event> {
        return try {
            val updatedEvent = eventDataSource.updateEvent(event)
            Result.success(updatedEvent)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteEvent(eventId: String): Result<Unit> {
        return try {
            eventDataSource.deleteEvent(eventId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Phase 1: State Management
    override suspend fun updateEventState(
        eventId: String,
        newState: EventState,
        reason: String?,
        changedBy: String?,
        automatic: Boolean
    ): Result<Event> {
        return try {
            val event = eventDataSource.updateEventState(
                eventId = eventId,
                newState = newState,
                reason = reason,
                changedBy = changedBy,
                automatic = automatic
            )
            Result.success(event)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getEventsByState(state: EventState): Result<List<Event>> {
        return try {
            val events = eventDataSource.getEventsByState(state)
            Result.success(events)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getEventsByStates(states: List<EventState>): Result<List<Event>> {
        return try {
            val events = eventDataSource.getEventsByStates(states)
            Result.success(events)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getOrganizerEventsByState(
        organizerId: String,
        state: EventState
    ): Result<List<Event>> {
        return try {
            val events = eventDataSource.getOrganizerEventsByState(organizerId, state)
            Result.success(events)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Phase 2: Postponement
    override suspend fun postponeEvent(
        eventId: String,
        postponement: com.eventfinder.app.domain.model.EventPostponement
    ): Result<Event> {
        return try {
            val event = eventDataSource.postponeEvent(eventId, postponement)
            Result.success(event)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Phase 3: Rescheduling
    override suspend fun rescheduleEvent(
        eventId: String,
        reschedule: com.eventfinder.app.domain.model.EventReschedule
    ): Result<Event> {
        return try {
            val event = eventDataSource.rescheduleEvent(eventId, reschedule)
            Result.success(event)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Phase 4: Cancellation
    override suspend fun cancelEvent(
        eventId: String,
        cancellation: com.eventfinder.app.domain.model.EventCancellation
    ): Result<Event> {
        return try {
            val event = eventDataSource.cancelEvent(eventId, cancellation)
            Result.success(event)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Draft/Publish Management
    override suspend fun getDraftEvents(organizerId: String): Result<List<Event>> {
        return try {
            val events = eventDataSource.getOrganizerEventsByState(organizerId, EventState.DRAFT)
            Result.success(events)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun publishEvent(eventId: String, organizerId: String): Result<Event> {
        return try {
            val event = eventDataSource.publishEvent(eventId, organizerId)
            Result.success(event)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
