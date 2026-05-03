package com.eventfinder.app.data.source

import com.eventfinder.app.domain.model.EventStats
import com.eventfinder.app.domain.model.Ticket
import com.eventfinder.app.domain.model.TicketStatus
import com.eventfinder.app.domain.model.TicketType
import kotlinx.coroutines.flow.Flow

/**
 * Data source interface for Ticket operations
 */
interface TicketDataSource {

    /**
     * Create a new ticket in Firestore
     */
    suspend fun createTicket(ticket: Ticket): Ticket

    /**
     * Get a ticket by its ID
     */
    suspend fun getTicketById(ticketId: String): Ticket?

    /**
     * Get all tickets for a specific user
     */
    suspend fun getUserTickets(userId: String): List<Ticket>

    /**
     * Get all attendees (tickets) for a specific event
     */
    suspend fun getEventAttendees(eventId: String): List<Ticket>

    /**
     * Validate a ticket using QR code data
     */
    suspend fun validateTicketByQR(qrCodeData: String): Ticket?

    /**
     * Update ticket status
     */
    suspend fun updateTicketStatus(
        ticketId: String,
        status: TicketStatus,
        checkedInBy: String? = null
    ): Ticket

    /**
     * Cancel a ticket
     */
    suspend fun cancelTicket(ticketId: String): Unit

    /**
     * Get event statistics
     */
    suspend fun getEventStats(eventId: String): EventStats?

    /**
     * Update event statistics
     */
    suspend fun updateEventStats(eventId: String, stats: EventStats): Unit

    /**
     * Increment event statistics atomically
     */
    suspend fun incrementEventStats(
        eventId: String,
        ticketType: TicketType,
        amount: Double
    ): Unit

    /**
     * Observe event attendees in real-time
     */
    fun observeEventAttendees(eventId: String): Flow<List<Ticket>>

    /**
     * Observe event statistics in real-time
     */
    fun observeEventStats(eventId: String): Flow<EventStats?>

    /**
     * Get all bookings (tickets) for an organizer's events
     */
    suspend fun getOrganizerBookings(organizerId: String): List<Ticket>
}
