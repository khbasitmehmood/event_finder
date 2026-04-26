package com.eventfinder.app.data.source

import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.model.EventCategory

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
}
