package com.eventfinder.app.data.source

import com.eventfinder.app.domain.model.Event
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dummy/Local data source that provides mock event data
 * This can be replaced with RemoteDataSource or LocalDataSource later
 */
@Singleton
class DummyEventDataSource @Inject constructor() : EventDataSource {

    // Simulated dummy events data
    private val dummyEvents = listOf(
        Event(
            id = 1,
            title = "Tech Conference 2024",
            location = "Lahore Expo Center",
            date = "12 Dec 2024",
            category = "Business",
            description = "Annual technology conference featuring the latest innovations."
        ),
        Event(
            id = 2,
            title = "Music Fiesta",
            location = "Karachi Arena",
            date = "20 Dec 2024",
            category = "Music",
            description = "A night of live music performances from top artists."
        ),
        Event(
            id = 3,
            title = "Business Meetup",
            location = "Islamabad Club",
            date = "28 Dec 2024",
            category = "Business",
            description = "Networking event for entrepreneurs and business professionals."
        ),
        Event(
            id = 4,
            title = "Sports Carnival",
            location = "Gaddafi Stadium",
            date = "5 Jan 2025",
            category = "Sports",
            description = "Multi-sport event featuring cricket, football, and more."
        ),
        Event(
            id = 5,
            title = "Art Exhibition",
            location = "National Art Gallery",
            date = "15 Jan 2025",
            category = "Education",
            description = "Showcasing contemporary art from local and international artists."
        ),
        Event(
            id = 6,
            title = "Food Festival",
            location = "Lahore Food Street",
            date = "22 Jan 2025",
            category = "Music",
            description = "Experience culinary delights from around the world."
        ),
        Event(
            id = 7,
            title = "Startup Summit",
            location = "Arfa Tower Lahore",
            date = "1 Feb 2025",
            category = "Business",
            description = "Connect with investors and fellow entrepreneurs."
        ),
        Event(
            id = 8,
            title = "Classical Music Night",
            location = "Alhamra Arts Center",
            date = "10 Feb 2025",
            category = "Music",
            description = "An evening of classical music performances."
        )
    )

    override suspend fun getEvents(): List<Event> {
        // Simulate network delay
        delay(1000)
        return dummyEvents
    }

    override suspend fun getEventById(id: Int): Event? {
        delay(300)
        return dummyEvents.find { it.id == id }
    }

    override suspend fun searchEvents(query: String): List<Event> {
        delay(300)
        if (query.isBlank()) return dummyEvents

        return dummyEvents.filter { event ->
            event.title.contains(query, ignoreCase = true) ||
            event.location.contains(query, ignoreCase = true) ||
            event.category?.contains(query, ignoreCase = true) == true
        }
    }

    override suspend fun getEventsByCategory(category: String): List<Event> {
        delay(300)
        return dummyEvents.filter { it.category.equals(category, ignoreCase = true) }
    }
}
