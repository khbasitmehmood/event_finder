package com.eventfinder.app.domain.repository

import com.eventfinder.app.domain.model.EventStats
import com.eventfinder.app.domain.model.Ticket
import com.eventfinder.app.domain.model.TicketType
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Ticket operations - defined in domain layer
 */
interface TicketRepository {

    /**
     * Create a new ticket
     */
    suspend fun createTicket(ticket: Ticket): Result<Ticket>

    /**
     * Get a ticket by its ID
     */
    suspend fun getTicketById(ticketId: String): Result<Ticket?>

    /**
     * Get all tickets for a specific user
     */
    suspend fun getUserTickets(userId: String): Result<List<Ticket>>

    /**
     * Get all attendees (tickets) for a specific event
     */
    suspend fun getEventAttendees(eventId: String): Result<List<Ticket>>

    /**
     * Validate a ticket using QR code data
     */
    suspend fun validateTicketByQR(qrCodeData: String): Result<Ticket?>

    /**
     * Check in a ticket (mark attendee as present)
     */
    suspend fun checkInTicket(ticketId: String, organizerId: String): Result<Ticket>

    /**
     * Cancel a ticket
     */
    suspend fun cancelTicket(ticketId: String, userId: String): Result<Unit>

    /**
     * Get event statistics
     */
    suspend fun getEventStats(eventId: String): Result<EventStats>

    /**
     * Increment event statistics when a ticket is created
     */
    suspend fun incrementEventStats(
        eventId: String,
        ticketType: TicketType,
        amount: Double
    ): Result<Unit>

    /**
     * Observe event attendees in real-time
     */
    fun observeEventAttendees(eventId: String): Flow<Result<List<Ticket>>>

    /**
     * Observe event statistics in real-time
     */
    fun observeEventStats(eventId: String): Flow<Result<EventStats?>>

    /**
     * Get all bookings (tickets) for an organizer's events
     */
    suspend fun getOrganizerBookings(organizerId: String): Result<List<Ticket>>
}
