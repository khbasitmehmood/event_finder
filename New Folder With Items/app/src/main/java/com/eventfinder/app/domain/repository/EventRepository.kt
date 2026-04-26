package com.eventfinder.app.domain.repository

import com.eventfinder.app.domain.model.Event

/**
 * Repository interface for Event operations - defined in domain layer
 */
interface EventRepository {
    suspend fun getExploreEvents(): Result<List<Event>>
    suspend fun getEventById(id: Int): Result<Event?>
    suspend fun searchEvents(query: String): Result<List<Event>>
    suspend fun getEventsByCategory(category: String): Result<List<Event>>
}
