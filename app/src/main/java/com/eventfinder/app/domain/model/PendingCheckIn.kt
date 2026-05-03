package com.eventfinder.app.domain.model

/**
 * Represents a check-in operation that's pending due to offline mode
 */
data class PendingCheckIn(
    val ticketId: String,
    val organizerId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val eventId: String
)
