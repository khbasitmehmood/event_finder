package com.eventfinder.app.data.source

import com.eventfinder.app.data.mapper.EventStatsMapper
import com.eventfinder.app.data.mapper.TicketMapper
import com.eventfinder.app.data.model.EventStatsDto
import com.eventfinder.app.data.model.TicketDto
import com.eventfinder.app.domain.model.EventStats
import com.eventfinder.app.domain.model.Ticket
import com.eventfinder.app.domain.model.TicketStatus
import com.eventfinder.app.domain.model.TicketType
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore implementation of TicketDataSource
 */
@Singleton
class FirestoreTicketDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) : TicketDataSource {

    private val ticketsCollection = firestore.collection("tickets")
    private val statsCollection = firestore.collection("event_stats")
    private val eventsCollection = firestore.collection("events")

    override suspend fun createTicket(ticket: Ticket): Ticket {
        val ticketDto = TicketMapper.toDto(ticket)
        val docRef = ticketsCollection.document()
        val ticketWithId = ticketDto.copy(id = docRef.id, ticketId = docRef.id)

        // Use transaction to ensure atomicity
        firestore.runTransaction { transaction ->
            // Create ticket
            transaction.set(docRef, ticketWithId)

            // Update event's currentParticipantCount
            val eventRef = eventsCollection.document(ticket.eventId)
            transaction.update(eventRef, "currentParticipantCount", FieldValue.increment(1))
        }.await()

        return TicketMapper.toDomain(ticketWithId)
    }

    override suspend fun getTicketById(ticketId: String): Ticket? {
        return try {
            val snapshot = ticketsCollection.document(ticketId).get().await()
            val dto = snapshot.toTicketDtoSafely()
            dto?.let { TicketMapper.toDomain(it) }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreTicketDataSource", "Failed to fetch ticket by id=$ticketId", e)
            null
        }
    }

    override suspend fun getUserTickets(userId: String): List<Ticket> {
        return try {
            val snapshot = ticketsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            android.util.Log.d(
                "FirestoreTicketDataSource",
                "Fetched ${snapshot.size()} ticket document(s) for userId=$userId"
            )

            snapshot.documents.mapNotNull { doc ->
                doc.toTicketDtoSafely()?.let { TicketMapper.toDomain(it) }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreTicketDataSource", "Failed to fetch user tickets for userId=$userId", e)
            emptyList()
        }
    }

    override suspend fun getEventAttendees(eventId: String): List<Ticket> {
        return try {
            val snapshot = ticketsCollection
                .whereEqualTo("eventId", eventId)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toTicketDtoSafely()?.let { TicketMapper.toDomain(it) }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun validateTicketByQR(qrCodeData: String): Ticket? {
        return try {
            val snapshot = ticketsCollection
                .whereEqualTo("qrCodeData", qrCodeData)
                .limit(1)
                .get()
                .await()

            val dto = snapshot.documents.firstOrNull()?.toTicketDtoSafely()
            dto?.let { TicketMapper.toDomain(it) }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun updateTicketStatus(
        ticketId: String,
        status: TicketStatus,
        checkedInBy: String?
    ): Ticket {
        val docRef = ticketsCollection.document(ticketId)

        firestore.runTransaction { transaction ->
            val updates = mutableMapOf<String, Any>(
                "status" to status.name
            )

            if (status == TicketStatus.CHECKED_IN) {
                updates["checkedInAt"] = Timestamp.now()
                checkedInBy?.let { updates["checkedInBy"] = it }
            }

            transaction.update(docRef, updates)
        }.await()

        // Fetch and return updated ticket
        val updatedSnapshot = docRef.get().await()
        val dto = updatedSnapshot.toTicketDtoSafely()
            ?: throw Exception("Failed to fetch updated ticket")

        return TicketMapper.toDomain(dto)
    }

    override suspend fun cancelTicket(ticketId: String) {
        val docRef = ticketsCollection.document(ticketId)

        // Get ticket first to know the eventId
        val ticket = getTicketById(ticketId)
            ?: throw Exception("Ticket not found")

        firestore.runTransaction { transaction ->
            // Update ticket status
            transaction.update(
                docRef,
                mapOf(
                    "status" to TicketStatus.CANCELLED.name
                )
            )

            // Decrement event's currentParticipantCount
            val eventRef = eventsCollection.document(ticket.eventId)
            transaction.update(eventRef, "currentParticipantCount", FieldValue.increment(-1))
        }.await()
    }

    override suspend fun getEventStats(eventId: String): EventStats? {
        return try {
            val snapshot = statsCollection.document(eventId).get().await()
            val dto = snapshot.toObject(EventStatsDto::class.java)
            dto?.let { EventStatsMapper.toDomain(it) }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun updateEventStats(eventId: String, stats: EventStats) {
        val dto = EventStatsMapper.toDto(stats)
        statsCollection.document(eventId).set(dto).await()
    }

    override suspend fun incrementEventStats(
        eventId: String,
        ticketType: TicketType,
        amount: Double
    ) {
        val docRef = statsCollection.document(eventId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)

            val updates = mutableMapOf<String, Any>(
                "totalTickets" to FieldValue.increment(1),
                "lastUpdated" to Timestamp.now()
            )

            when (ticketType) {
                TicketType.PUBLIC_RESERVATION -> {
                    updates["reservedCount"] = FieldValue.increment(1)
                }
                TicketType.FREE_PRIVATE, TicketType.PAID -> {
                    if (amount > 0) {
                        updates["totalRevenue"] = FieldValue.increment(amount)
                    }
                }
            }

            if (snapshot.exists()) {
                transaction.update(docRef, updates)
            } else {
                // Initialize stats if doesn't exist
                val initialStats = EventStatsDto(
                    eventId = eventId,
                    totalTickets = 1,
                    checkedInCount = 0,
                    reservedCount = if (ticketType == TicketType.PUBLIC_RESERVATION) 1 else 0,
                    cancelledCount = 0,
                    totalRevenue = amount,
                    currency = "PKR",
                    lastUpdated = Timestamp.now()
                )
                transaction.set(docRef, initialStats)
            }
        }.await()
    }

    override fun observeEventAttendees(eventId: String): Flow<List<Ticket>> = callbackFlow {
        val listener = ticketsCollection
            .whereEqualTo("eventId", eventId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val tickets = snapshot.documents.mapNotNull { doc ->
                        try {
                            val dto = doc.toTicketDtoSafely()
                            dto?.let { TicketMapper.toDomain(it) }
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(tickets)
                }
            }

        awaitClose { listener.remove() }
    }

    override fun observeEventStats(eventId: String): Flow<EventStats?> = callbackFlow {
        val listener = statsCollection
            .document(eventId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    try {
                        val dto = snapshot.toObject(EventStatsDto::class.java)
                        val stats = dto?.let { EventStatsMapper.toDomain(it) }
                        trySend(stats)
                    } catch (e: Exception) {
                        trySend(null)
                    }
                } else {
                    trySend(null)
                }
            }

        awaitClose { listener.remove() }
    }

    override suspend fun getOrganizerBookings(organizerId: String): List<Ticket> {
        return try {
            val snapshot = ticketsCollection
                .whereEqualTo("organizerId", organizerId)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toTicketDtoSafely()?.let { TicketMapper.toDomain(it) }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun DocumentSnapshot.toTicketDtoSafely(): TicketDto? {
        if (!exists()) return null

        return runCatching {
            toObject(TicketDto::class.java)
        }.getOrElse { error ->
            android.util.Log.w(
                "FirestoreTicketDataSource",
                "Falling back to manual ticket mapping for id=$id: ${error.message}"
            )
            TicketDto(
                id = id,
                ticketId = getString("ticketId").orEmpty().ifBlank { id },
                eventId = getString("eventId").orEmpty(),
                eventTitle = getString("eventTitle").orEmpty(),
                eventStartTime = getTimestamp("eventStartTime"),
                userId = getString("userId").orEmpty(),
                userName = getString("userName").orEmpty(),
                userEmail = getString("userEmail").orEmpty(),
                ticketType = getString("ticketType") ?: TicketType.PUBLIC_RESERVATION.name,
                status = getString("status") ?: TicketStatus.RESERVED.name,
                qrCodeData = getString("qrCodeData").orEmpty(),
                purchasePrice = getDouble("purchasePrice")
                    ?: getLong("purchasePrice")
                    ?: 0.0,
                currency = getString("currency") ?: "PKR",
                paymentStatus = getString("paymentStatus") ?: "NOT_REQUIRED",
                paymentProvider = getString("paymentProvider"),
                paymentTransactionId = getString("paymentTransactionId"),
                paidAt = getTimestamp("paidAt"),
                purchasedAt = getTimestamp("purchasedAt"),
                checkedInAt = getTimestamp("checkedInAt"),
                checkedInBy = getString("checkedInBy"),
                eventLocation = getString("eventLocation"),
                organizerId = getString("organizerId").orEmpty(),
                organizerName = getString("organizerName").orEmpty()
            )
        }
    }
}
