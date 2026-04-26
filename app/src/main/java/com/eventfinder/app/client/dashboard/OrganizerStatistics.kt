package com.eventfinder.app.client.dashboard

/**
 * Data class representing organizer dashboard statistics.
 * Currently uses mock data. Will be replaced with real Firebase analytics in future.
 */
data class OrganizerStatistics(
    val totalEvents: Int = 0,
    val totalEarnings: String = "$0",
    val activeEvents: Int = 0,
    val totalParticipants: Int = 0
)
