package com.eventfinder.app.domain.repository

import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.model.EventCategory

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
}
