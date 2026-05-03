package com.eventfinder.app.data.repository

import com.eventfinder.app.data.source.TicketDataSource
import com.eventfinder.app.domain.model.EventStats
import com.eventfinder.app.domain.model.Ticket
import com.eventfinder.app.domain.model.TicketStatus
import com.eventfinder.app.domain.model.TicketType
import com.eventfinder.app.domain.repository.TicketRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of TicketRepository
 */
@Singleton
class TicketRepositoryImpl @Inject constructor(
    private val ticketDataSource: TicketDataSource
) : TicketRepository {

    override suspend fun createTicket(ticket: Ticket): Result<Ticket> {
        return try {
            val createdTicket = ticketDataSource.createTicket(ticket)
            Result.success(createdTicket)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTicketById(ticketId: String): Result<Ticket?> {
        return try {
            val ticket = ticketDataSource.getTicketById(ticketId)
            Result.success(ticket)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserTickets(userId: String): Result<List<Ticket>> {
        return try {
            val tickets = ticketDataSource.getUserTickets(userId)
            Result.success(tickets)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getEventAttendees(eventId: String): Result<List<Ticket>> {
        return try {
            val attendees = ticketDataSource.getEventAttendees(eventId)
            Result.success(attendees)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun validateTicketByQR(qrCodeData: String): Result<Ticket?> {
        return try {
            val ticket = ticketDataSource.validateTicketByQR(qrCodeData)
            Result.success(ticket)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkInTicket(ticketId: String, organizerId: String): Result<Ticket> {
        return try {
            val updatedTicket = ticketDataSource.updateTicketStatus(
                ticketId = ticketId,
                status = TicketStatus.CHECKED_IN,
                checkedInBy = organizerId
            )
            Result.success(updatedTicket)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelTicket(ticketId: String, userId: String): Result<Unit> {
        return try {
            ticketDataSource.cancelTicket(ticketId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getEventStats(eventId: String): Result<EventStats> {
        return try {
            val stats = ticketDataSource.getEventStats(eventId)
            if (stats != null) {
                Result.success(stats)
            } else {
                // Return empty stats if not found
                Result.success(
                    EventStats(
                        eventId = eventId,
                        totalTickets = 0,
                        checkedInCount = 0,
                        reservedCount = 0,
                        cancelledCount = 0,
                        totalRevenue = 0.0
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun incrementEventStats(
        eventId: String,
        ticketType: TicketType,
        amount: Double
    ): Result<Unit> {
        return try {
            ticketDataSource.incrementEventStats(eventId, ticketType, amount)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeEventAttendees(eventId: String): Flow<Result<List<Ticket>>> {
        return ticketDataSource.observeEventAttendees(eventId)
            .map { tickets -> Result.success(tickets) }
            .catch { e -> emit(Result.failure(e)) }
    }

    override fun observeEventStats(eventId: String): Flow<Result<EventStats?>> {
        return ticketDataSource.observeEventStats(eventId)
            .map { stats ->
                Result.success(stats ?: EventStats(
                    eventId = eventId,
                    totalTickets = 0,
                    checkedInCount = 0,
                    reservedCount = 0,
                    cancelledCount = 0,
                    totalRevenue = 0.0
                ))
            }
            .catch { e -> emit(Result.failure(e)) }
    }

    override suspend fun getOrganizerBookings(organizerId: String): Result<List<Ticket>> {
        return try {
            val tickets = ticketDataSource.getOrganizerBookings(organizerId)
            Result.success(tickets)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
