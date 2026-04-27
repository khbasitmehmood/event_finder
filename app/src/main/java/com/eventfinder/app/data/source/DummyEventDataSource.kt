package com.eventfinder.app.data.source

import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.model.EventCategory
import com.eventfinder.app.domain.model.EventLocation
import com.eventfinder.app.domain.model.EventVisibility
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Dummy/Local data source that provides mock event data
 * Useful for testing without Firestore connection
 */
@Singleton
class DummyEventDataSource @Inject constructor() : EventDataSource {

    // Simulated dummy events data with proper structure
    private val dummyEvents = listOf(
        Event(
            id = "1",
            eventId = "event_1",
            title = "Tech Conference 2024",
            description = "Annual technology conference featuring the latest innovations in AI, blockchain, and cloud computing.",
            category = EventCategory("cat_business", "Business & Expos"),
            organizerId = "org_1",
            organizerName = "Tech Events PK",
            startTime = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L), // 7 days from now
            location = EventLocation(31.5497, 74.3436), // Lahore
            address = "Lahore Expo Center, Lahore, Pakistan",
            isFree = false,
            price = 5000.0,
            currency = "PKR",
            imageUrls = listOf("https://images.unsplash.com/photo-1540575467063-178a50c2df87"),
            mainImageUrl = "https://images.unsplash.com/photo-1540575467063-178a50c2df87",
            tags = listOf("technology", "conference", "networking"),
            visibility = EventVisibility.PUBLIC,
            createdAt = System.currentTimeMillis()
        ),
        Event(
            id = "2",
            eventId = "event_2",
            title = "Music Festival 2024",
            description = "A spectacular night featuring Pakistan's top musicians and bands.",
            category = EventCategory("cat_music", "Music & Concerts"),
            organizerId = "org_2",
            organizerName = "Sound Events",
            startTime = System.currentTimeMillis() + (14 * 24 * 60 * 60 * 1000L),
            location = EventLocation(24.8607, 67.0011), // Karachi
            address = "Karachi Expo Centre, Karachi, Pakistan",
            isFree = false,
            price = 3000.0,
            currency = "PKR",
            imageUrls = listOf("https://images.unsplash.com/photo-1470229722913-7c0e2dbbafd3"),
            mainImageUrl = "https://images.unsplash.com/photo-1470229722913-7c0e2dbbafd3",
            tags = listOf("music", "concert", "entertainment"),
            visibility = EventVisibility.PUBLIC,
            createdAt = System.currentTimeMillis()
        ),
        Event(
            id = "3",
            eventId = "event_3",
            title = "Food Festival",
            description = "Celebrate culinary diversity with food from around the world.",
            category = EventCategory("cat_food", "Food Festivals & Galas"),
            organizerId = "org_3",
            organizerName = "Foodie Events",
            startTime = System.currentTimeMillis() + (21 * 24 * 60 * 60 * 1000L),
            location = EventLocation(33.6844, 73.0479), // Islamabad
            address = "F-9 Park, Islamabad, Pakistan",
            isFree = true,
            imageUrls = listOf("https://images.unsplash.com/photo-1555939594-58d7cb561ad1"),
            mainImageUrl = "https://images.unsplash.com/photo-1555939594-58d7cb561ad1",
            tags = listOf("food", "festival", "family"),
            visibility = EventVisibility.PUBLIC,
            createdAt = System.currentTimeMillis()
        ),
        Event(
            id = "4",
            eventId = "event_4",
            title = "Cricket Tournament",
            description = "Inter-city cricket championship featuring top teams.",
            category = EventCategory("cat_sports", "Sports & Cricket Screenings"),
            organizerId = "org_4",
            organizerName = "Sports League PK",
            startTime = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L),
            location = EventLocation(31.5204, 74.3587), // Lahore
            address = "Gaddafi Stadium, Lahore, Pakistan",
            isFree = false,
            price = 1500.0,
            currency = "PKR",
            maxParticipants = 20000,
            currentParticipantCount = 5430,
            imageUrls = listOf("https://images.unsplash.com/photo-1531415074968-036ba1b575da"),
            mainImageUrl = "https://images.unsplash.com/photo-1531415074968-036ba1b575da",
            tags = listOf("sports", "cricket", "tournament"),
            visibility = EventVisibility.PUBLIC,
            createdAt = System.currentTimeMillis()
        ),
        Event(
            id = "5",
            eventId = "event_5",
            title = "Digital Marketing Workshop",
            description = "Learn the latest digital marketing strategies from industry experts.",
            category = EventCategory("cat_workshops", "Workshops & Training"),
            organizerId = "org_5",
            organizerName = "Learn & Grow Academy",
            startTime = System.currentTimeMillis() + (10 * 24 * 60 * 60 * 1000L),
            location = EventLocation(31.4697, 74.2728),
            address = "Arfa Software Technology Park, Lahore, Pakistan",
            isFree = false,
            price = 2500.0,
            currency = "PKR",
            maxParticipants = 50,
            currentParticipantCount = 32,
            imageUrls = listOf("https://images.unsplash.com/photo-1552664730-d307ca884978"),
            mainImageUrl = "https://images.unsplash.com/photo-1552664730-d307ca884978",
            tags = listOf("workshop", "marketing", "business"),
            visibility = EventVisibility.PUBLIC,
            createdAt = System.currentTimeMillis()
        )
    )

    override suspend fun getEvents(): List<Event> {
        delay(1000) // Simulate network delay
        return dummyEvents
    }

    override suspend fun getEventById(id: String): Event? {
        delay(300)
        return dummyEvents.find { it.id == id }
    }

    override suspend fun searchEvents(query: String): List<Event> {
        delay(300)
        if (query.isBlank()) return dummyEvents

        return dummyEvents.filter { event ->
            event.title.contains(query, ignoreCase = true) ||
            event.description?.contains(query, ignoreCase = true) == true ||
            event.address?.contains(query, ignoreCase = true) == true
        }
    }

    override suspend fun getEventsByCategory(category: EventCategory): List<Event> {
        delay(300)
        return dummyEvents.filter { it.category?.id == category.id }
    }

    override suspend fun getNearbyEvents(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): List<Event> {
        delay(500)
        return dummyEvents.filter { event ->
            val distance = calculateDistance(
                latitude,
                longitude,
                event.location.latitude,
                event.location.longitude
            )
            distance <= radiusKm
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
    }

    // Mutable list to simulate database operations
    private val userCreatedEvents = mutableListOf<Event>()

    override suspend fun createEvent(event: Event): Event {
        delay(500)
        val eventWithId = event.copy(
            id = "user_event_${System.currentTimeMillis()}",
            eventId = "user_event_${System.currentTimeMillis()}",
            createdAt = System.currentTimeMillis()
        )
        userCreatedEvents.add(0, eventWithId)
        return eventWithId
    }

    override suspend fun getUserEvents(userId: String): List<Event> {
        delay(500)
        return userCreatedEvents.filter { it.organizerId == userId }
    }

    override suspend fun updateEvent(event: Event): Event {
        delay(500)
        val index = userCreatedEvents.indexOfFirst { it.id == event.id }
        if (index != -1) {
            userCreatedEvents[index] = event.copy(updatedAt = System.currentTimeMillis())
            return userCreatedEvents[index]
        }
        throw Exception("Event not found")
    }

    override suspend fun deleteEvent(eventId: String) {
        delay(300)
        userCreatedEvents.removeAll { it.id == eventId }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }
}