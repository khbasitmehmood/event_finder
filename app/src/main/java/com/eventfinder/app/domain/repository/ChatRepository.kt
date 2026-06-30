package com.eventfinder.app.domain.repository

import com.eventfinder.app.domain.model.Event

/**
 * Repository interface for AI chatbot operations - defined in domain layer
 */
interface ChatRepository {
    /**
     * Sends a user message to the AI model, grounded in the given list of events.
     * Each call is independent — no conversation history is sent.
     */
    suspend fun sendMessage(userMessage: String, availableEvents: List<Event>): Result<String>
}
