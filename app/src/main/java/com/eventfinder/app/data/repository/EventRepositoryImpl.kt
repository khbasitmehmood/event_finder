package com.eventfinder.app.data.repository

import com.eventfinder.app.data.source.EventDataSource
import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.model.EventCategory
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
}
