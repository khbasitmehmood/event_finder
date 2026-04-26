package com.eventfinder.app.data.source

import com.eventfinder.app.domain.model.Event

/**
 * Data source interface for events
 */
interface EventDataSource {
    suspend fun getEvents(): List<Event>
    suspend fun getEventById(id: Int): Event?
    suspend fun searchEvents(query: String): List<Event>
    suspend fun getEventsByCategory(category: String): List<Event>
}
