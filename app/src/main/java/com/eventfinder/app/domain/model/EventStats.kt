package com.eventfinder.app.domain.model

/**
 * Domain model for Event Statistics
 * Tracks attendance and revenue metrics for an event
 */
data class EventStats(
    val eventId: String,
    val totalTickets: Int = 0,
    val checkedInCount: Int = 0,
    val reservedCount: Int = 0,
    val cancelledCount: Int = 0,
    val totalRevenue: Double = 0.0,
    val currency: String = "PKR",
    val lastUpdated: Long = System.currentTimeMillis()
)
