package com.eventfinder.app.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Data Transfer Object for Firestore EventStats documents
 * Stores aggregated statistics for an event
 */
data class EventStatsDto(
    @DocumentId
    val eventId: String = "",
    val totalTickets: Int = 0,
    val checkedInCount: Int = 0,
    val reservedCount: Int = 0,
    val cancelledCount: Int = 0,
    val totalRevenue: Double = 0.0,
    val currency: String = "PKR",
    val lastUpdated: Timestamp? = null
)
