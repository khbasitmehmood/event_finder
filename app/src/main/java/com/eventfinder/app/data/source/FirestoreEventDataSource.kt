package com.eventfinder.app.data.source

import com.eventfinder.app.data.mapper.EventMapper
import com.eventfinder.app.data.model.EventDto
import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.model.EventCategory
import com.eventfinder.app.domain.model.EventPostponement
import com.eventfinder.app.domain.model.EventState
import com.eventfinder.app.domain.model.StateChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore implementation of EventDataSource
 * Handles all Firestore operations for Events
 */
@Singleton
class FirestoreEventDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) : EventDataSource {

    companion object {
        private const val EVENTS_COLLECTION = "events"
        private const val FIELD_START_TIME = "startTime"
        private const val FIELD_CATEGORY = "category"
        private const val FIELD_VISIBILITY = "visibility"
        private const val FIELD_GEOHASH = "geohash"
        private const val FIELD_STATE = "state"
        private const val FIELD_ORGANIZER_ID = "organizerId"
    }

    override suspend fun getEvents(): List<Event> {
        return try {
            // Note: Removed orderBy to avoid requiring a composite index in Firestore
            // We will sort client-side instead
            val snapshot = firestore.collection(EVENTS_COLLECTION)
//                .whereEqualTo(FIELD_VISIBILITY, "PUBLIC")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(EventDto::class.java)?.let { dto ->
                    EventMapper.toDomain(dto.copy(id = doc.id))
                }
            }.sortedBy { it.startTime }.take(50)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreEventDataSource", "Failed to fetch events", e)
            throw Exception("Failed to fetch events: ${e.message}", e)
        }
    }

    override suspend fun getEventById(id: String): Event? {
        return try {
            val snapshot = firestore.collection(EVENTS_COLLECTION)
                .document(id)
                .get()
                .await()

            snapshot.toObject(EventDto::class.java)?.let { dto ->
                EventMapper.toDomain(dto.copy(id = snapshot.id))
            }
        } catch (e: Exception) {
            throw Exception("Failed to fetch event by id: ${e.message}", e)
        }
    }

    override suspend fun searchEvents(query: String): List<Event> {
        return try {
            // Fallback to client-side filtering completely to avoid missing index errors
            val allEvents = getEvents()
            allEvents.filter { event ->
                event.title.contains(query, ignoreCase = true) ||
                event.description?.contains(query, ignoreCase = true) == true ||
                event.address?.contains(query, ignoreCase = true) == true
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreEventDataSource", "Failed to search events", e)
            emptyList()
        }
    }

    override suspend fun getEventsByCategory(category: EventCategory): List<Event> {
        return try {
            // Note: Removed orderBy to avoid requiring a composite index in Firestore
            val snapshot = firestore.collection(EVENTS_COLLECTION)
                .whereEqualTo(FIELD_CATEGORY, category.name)
                .whereEqualTo(FIELD_VISIBILITY, "PUBLIC")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(EventDto::class.java)?.let { dto ->
                    EventMapper.toDomain(dto.copy(id = doc.id))
                }
            }.sortedBy { it.startTime }.take(50)
        } catch (e: Exception) {
            throw Exception("Failed to fetch events by category: ${e.message}", e)
        }
    }

    override suspend fun getNearbyEvents(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): List<Event> {
        return try {
            val events = getEvents()

            events.filter { event ->
                val distance = calculateDistance(
                    latitude,
                    longitude,
                    event.location.latitude,
                    event.location.longitude
                )
                distance <= radiusKm
            }.sortedBy { event ->
                calculateDistance(
                    latitude,
                    longitude,
                    event.location.latitude,
                    event.location.longitude
                )
            }.map { event ->
                event.copy(
                    distanceKm = calculateDistance(
                        latitude,
                        longitude,
                        event.location.latitude,
                        event.location.longitude
                    )
                )
            }
        } catch (e: Exception) {
            throw Exception("Failed to fetch nearby events: ${e.message}", e)
        }
    }

    override suspend fun createEvent(event: Event): Event {
        return try {
            val eventDto = EventMapper.toDto(event)
            val docRef = firestore.collection(EVENTS_COLLECTION).document()

            val eventWithId = eventDto.copy(
                id = docRef.id,
                eventId = docRef.id
            )

            docRef.set(eventWithId).await()

            EventMapper.toDomain(eventWithId)
        } catch (e: Exception) {
            throw Exception("Failed to create event: ${e.message}", e)
        }
    }

    override suspend fun getUserEvents(userId: String): List<Event> {
        return try {
            android.util.Log.d("FirestoreEventDataSource", "Fetching events for userId: $userId")

            // Note: Removed orderBy to avoid requiring a composite index in Firestore
            val snapshot = firestore.collection(EVENTS_COLLECTION)
//                .whereEqualTo("organizerId", userId)
                .get()
                .await()

            android.util.Log.d("FirestoreEventDataSource", "Found ${snapshot.size()} documents")

            val events = snapshot.documents.mapNotNull { doc ->
                android.util.Log.d("FirestoreEventDataSource", "Document: ${doc.id}, data: ${doc.data}")
                doc.toObject(EventDto::class.java)?.let { dto ->
                    EventMapper.toDomain(dto.copy(id = doc.id))
                }
            }.filter { event ->
                // Filter out DRAFT events - they shouldn't appear in manage events
                event.state != com.eventfinder.app.domain.model.EventState.DRAFT
            }

            android.util.Log.d("FirestoreEventDataSource", "Mapped to ${events.size} non-draft events")
            events.sortedByDescending { it.startTime }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreEventDataSource", "Failed to fetch user events", e)
            throw Exception("Failed to fetch user events: ${e.message}", e)
        }
    }

    override suspend fun updateEvent(event: Event): Event {
        return try {
            val eventDto = EventMapper.toDto(event)

            firestore.collection(EVENTS_COLLECTION)
                .document(event.id)
                .set(eventDto)
                .await()

            event
        } catch (e: Exception) {
            throw Exception("Failed to update event: ${e.message}", e)
        }
    }

    override suspend fun deleteEvent(eventId: String) {
        try {
            firestore.collection(EVENTS_COLLECTION)
                .document(eventId)
                .delete()
                .await()
        } catch (e: Exception) {
            throw Exception("Failed to delete event: ${e.message}", e)
        }
    }

    /**
     * Calculate distance between two coordinates using Haversine formula
     * Returns distance in kilometers
     */
    private fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadiusKm = 6371.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return earthRadiusKm * c
    }

    // Phase 1: State Management Implementation
    override suspend fun updateEventState(
        eventId: String,
        newState: EventState,
        reason: String?,
        changedBy: String?,
        automatic: Boolean
    ): Event {
        return try {
            // Get current event first
            val currentEvent = getEventById(eventId)
                ?: throw Exception("Event not found")

            // Create state change record
            val stateChange = StateChange(
                fromState = currentEvent.state,
                toState = newState,
                changedAt = System.currentTimeMillis(),
                changedBy = changedBy,
                reason = reason,
                automatic = automatic
            )

            // Prepare update data
            val updates = mutableMapOf<String, Any>(
                FIELD_STATE to newState.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            // Add timestamp for specific states
            when (newState) {
                EventState.SCHEDULED -> {
                    if (currentEvent.publishedAt == null) {
                        updates["publishedAt"] = FieldValue.serverTimestamp()
                    }
                }
                EventState.COMPLETED -> {
                    updates["completedAt"] = FieldValue.serverTimestamp()
                }
                else -> {}
            }

            // Add state change to history
            updates["stateHistory"] = FieldValue.arrayUnion(
                mapOf(
                    "fromState" to stateChange.fromState.name,
                    "toState" to stateChange.toState.name,
                    "changedAt" to stateChange.changedAt,
                    "changedBy" to stateChange.changedBy,
                    "reason" to stateChange.reason,
                    "automatic" to stateChange.automatic
                )
            )

            // Update Firestore
            firestore.collection(EVENTS_COLLECTION)
                .document(eventId)
                .update(updates)
                .await()

            // Return updated event
            getEventById(eventId) ?: throw Exception("Failed to fetch updated event")
        } catch (e: Exception) {
            android.util.Log.e("FirestoreEventDataSource", "Failed to update event state", e)
            throw Exception("Failed to update event state: ${e.message}", e)
        }
    }

    override suspend fun getEventsByState(state: EventState): List<Event> {
        return try {
            val snapshot = firestore.collection(EVENTS_COLLECTION)
                .whereEqualTo(FIELD_STATE, state.name)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(EventDto::class.java)?.let { dto ->
                    EventMapper.toDomain(dto.copy(id = doc.id))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreEventDataSource", "Failed to fetch events by state", e)
            throw Exception("Failed to fetch events by state: ${e.message}", e)
        }
    }

    override suspend fun getEventsByStates(states: List<EventState>): List<Event> {
        return try {
            if (states.isEmpty()) return emptyList()

            // Firestore 'in' query supports up to 10 values
            val stateNames = states.map { it.name }
            val snapshot = firestore.collection(EVENTS_COLLECTION)
                .whereIn(FIELD_STATE, stateNames)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(EventDto::class.java)?.let { dto ->
                    EventMapper.toDomain(dto.copy(id = doc.id))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreEventDataSource", "Failed to fetch events by states", e)
            throw Exception("Failed to fetch events by states: ${e.message}", e)
        }
    }

    override suspend fun getOrganizerEventsByState(
        organizerId: String,
        state: EventState
    ): List<Event> {
        return try {
            val snapshot = firestore.collection(EVENTS_COLLECTION)
                .whereEqualTo(FIELD_ORGANIZER_ID, organizerId)
                .whereEqualTo(FIELD_STATE, state.name)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(EventDto::class.java)?.let { dto ->
                    EventMapper.toDomain(dto.copy(id = doc.id))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreEventDataSource", "Failed to fetch organizer events by state", e)
            throw Exception("Failed to fetch organizer events by state: ${e.message}", e)
        }
    }

    // Phase 2: Postponement Implementation
    override suspend fun postponeEvent(
        eventId: String,
        postponement: EventPostponement
    ): Event {
        return try {
            // Get current event
            val currentEvent = getEventById(eventId)
                ?: throw Exception("Event not found")

            // Prepare postponement data
            val postponementMap = mapOf(
                "originalStartTime" to postponement.originalStartTime,
                "originalEndTime" to postponement.originalEndTime,
                "newStartTime" to postponement.newStartTime,
                "newEndTime" to postponement.newEndTime,
                "reason" to postponement.reason,
                "postponedAt" to postponement.postponedAt,
                "postponedBy" to postponement.postponedBy,
                "notificationSent" to postponement.notificationSent
            )

            // Prepare update data
            val updates = mutableMapOf<String, Any>(
                "state" to EventState.POSTPONED.name,
                "currentPostponement" to postponementMap,
                "postponementCount" to FieldValue.increment(1),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            // Update start and end times if new times are provided
            if (postponement.newStartTime != null) {
                updates["startTime"] = com.google.firebase.Timestamp(
                    postponement.newStartTime / 1000,
                    ((postponement.newStartTime % 1000) * 1000000).toInt()
                )
            }

            if (postponement.newEndTime != null) {
                updates["endTime"] = com.google.firebase.Timestamp(
                    postponement.newEndTime / 1000,
                    ((postponement.newEndTime % 1000) * 1000000).toInt()
                )
            }

            // Add to postponement history
            updates["postponementHistory"] = FieldValue.arrayUnion(postponementMap)

            // Add state change to history
            val stateChange = mapOf(
                "fromState" to currentEvent.state.name,
                "toState" to EventState.POSTPONED.name,
                "changedAt" to System.currentTimeMillis(),
                "changedBy" to postponement.postponedBy,
                "reason" to "Event postponed: ${postponement.reason}",
                "automatic" to false
            )
            updates["stateHistory"] = FieldValue.arrayUnion(stateChange)

            // Update Firestore
            firestore.collection(EVENTS_COLLECTION)
                .document(eventId)
                .update(updates)
                .await()

            android.util.Log.d("FirestoreEventDataSource", "Event $eventId postponed successfully")

            // Return updated event
            getEventById(eventId) ?: throw Exception("Failed to fetch updated event")
        } catch (e: Exception) {
            android.util.Log.e("FirestoreEventDataSource", "Failed to postpone event", e)
            throw Exception("Failed to postpone event: ${e.message}", e)
        }
    }

    // Phase 3: Rescheduling Implementation
    override suspend fun rescheduleEvent(
        eventId: String,
        reschedule: com.eventfinder.app.domain.model.EventReschedule
    ): Event {
        return try {
            // Get current event
            val currentEvent = getEventById(eventId)
                ?: throw Exception("Event not found")

            // Prepare reschedule data
            val rescheduleMap = mapOf(
                "originalStartTime" to reschedule.originalStartTime,
                "originalEndTime" to reschedule.originalEndTime,
                "originalLatitude" to reschedule.originalLocation?.latitude,
                "originalLongitude" to reschedule.originalLocation?.longitude,
                "originalAddress" to reschedule.originalAddress,
                "newStartTime" to reschedule.newStartTime,
                "newEndTime" to reschedule.newEndTime,
                "newLatitude" to reschedule.newLocation?.latitude,
                "newLongitude" to reschedule.newLocation?.longitude,
                "newAddress" to reschedule.newAddress,
                "reason" to reschedule.reason,
                "rescheduledAt" to reschedule.rescheduledAt,
                "rescheduledBy" to reschedule.rescheduledBy,
                "notificationSent" to reschedule.notificationSent,
                "changedFields" to reschedule.getChangedFields().map { it.name }
            )

            // Prepare update data
            val updates = mutableMapOf<String, Any>(
                "currentReschedule" to rescheduleMap,
                "rescheduleCount" to FieldValue.increment(1),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            // Update start time
            updates["startTime"] = com.google.firebase.Timestamp(
                reschedule.newStartTime / 1000,
                ((reschedule.newStartTime % 1000) * 1000000).toInt()
            )

            // Update end time if provided
            if (reschedule.newEndTime != null) {
                updates["endTime"] = com.google.firebase.Timestamp(
                    reschedule.newEndTime / 1000,
                    ((reschedule.newEndTime % 1000) * 1000000).toInt()
                )
            }

            // Update location if changed
            if (reschedule.newLocation != null) {
                updates["location"] = com.google.firebase.firestore.GeoPoint(
                    reschedule.newLocation.latitude,
                    reschedule.newLocation.longitude
                )
            }

            // Update address if changed
            if (reschedule.newAddress != null) {
                updates["address"] = reschedule.newAddress
            }

            // Add to reschedule history
            updates["rescheduleHistory"] = FieldValue.arrayUnion(rescheduleMap)

            // Add state change to history
            val stateChange = mapOf(
                "fromState" to currentEvent.state.name,
                "toState" to EventState.SCHEDULED.name,
                "changedAt" to System.currentTimeMillis(),
                "changedBy" to reschedule.rescheduledBy,
                "reason" to "Event rescheduled: ${reschedule.reason}",
                "automatic" to false
            )
            updates["stateHistory"] = FieldValue.arrayUnion(stateChange)

            // If event was postponed, change back to scheduled
            if (currentEvent.state == EventState.POSTPONED) {
                updates["state"] = EventState.SCHEDULED.name
                updates["currentPostponement"] = FieldValue.delete()
            }

            // Update Firestore
            firestore.collection(EVENTS_COLLECTION)
                .document(eventId)
                .update(updates)
                .await()

            android.util.Log.d("FirestoreEventDataSource", "Event $eventId rescheduled successfully")

            // Return updated event
            getEventById(eventId) ?: throw Exception("Failed to fetch updated event")
        } catch (e: Exception) {
            android.util.Log.e("FirestoreEventDataSource", "Failed to reschedule event", e)
            throw Exception("Failed to reschedule event: ${e.message}", e)
        }
    }

    // Phase 4: Cancellation Implementation
    override suspend fun cancelEvent(
        eventId: String,
        cancellation: com.eventfinder.app.domain.model.EventCancellation
    ): Event {
        return try {
            // Get current event
            val currentEvent = getEventById(eventId)
                ?: throw Exception("Event not found")

            // Prepare cancellation data
            val cancellationMap = mapOf(
                "cancelledAt" to cancellation.cancelledAt,
                "cancelledBy" to cancellation.cancelledBy,
                "reason" to cancellation.reason,
                "refundStatus" to cancellation.refundStatus.name,
                "notificationSent" to cancellation.notificationSent,
                "attendeeCount" to cancellation.attendeeCount,
                "refundAmount" to cancellation.refundAmount,
                "refundCurrency" to cancellation.refundCurrency
            )

            // Prepare update data
            val updates = mutableMapOf<String, Any>(
                "state" to EventState.CANCELLED.name,
                "cancellation" to cancellationMap,
                "cancelledAt" to com.google.firebase.Timestamp(
                    cancellation.cancelledAt / 1000,
                    ((cancellation.cancelledAt % 1000) * 1000000).toInt()
                ),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            // Add state change to history
            val stateChange = mapOf(
                "fromState" to currentEvent.state.name,
                "toState" to EventState.CANCELLED.name,
                "changedAt" to System.currentTimeMillis(),
                "changedBy" to cancellation.cancelledBy,
                "reason" to "Event cancelled: ${cancellation.reason}",
                "automatic" to false
            )
            updates["stateHistory"] = FieldValue.arrayUnion(stateChange)

            // Update Firestore
            firestore.collection(EVENTS_COLLECTION)
                .document(eventId)
                .update(updates)
                .await()

            android.util.Log.d("FirestoreEventDataSource", "Event $eventId cancelled successfully")

            // Return updated event
            getEventById(eventId) ?: throw Exception("Failed to fetch updated event")
        } catch (e: Exception) {
            android.util.Log.e("FirestoreEventDataSource", "Failed to cancel event", e)
            throw Exception("Failed to cancel event: ${e.message}", e)
        }
    }
}
