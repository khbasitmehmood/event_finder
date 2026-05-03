package com.eventfinder.app.data.source

import com.eventfinder.app.data.mapper.EventMapper
import com.eventfinder.app.data.model.EventDto
import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.model.EventCategory
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
            }

            android.util.Log.d("FirestoreEventDataSource", "Mapped to ${events.size} events")
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
}
