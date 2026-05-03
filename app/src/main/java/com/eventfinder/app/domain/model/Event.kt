package com.eventfinder.app.domain.model

/**
 * Domain model for Event - used across the app
 * This is the comprehensive model that matches Firestore structure
 */
data class Event(
    val id: String = "",
    val eventId: String = "",                 // Firestore document ID
    val title: String,
    val description: String? = null,
    val category: EventCategory? = null,

    val organizerId: String,
    val organizerName: String,
    val organizerPhotoUrl: String? = null,
    val organizerSocialLinks: OrganizerSocialLinks? = null,

    val startTime: Long,
    val endTime: Long? = null,

    val location: EventLocation,
    val address: String? = null,              // human-readable address

    val maxParticipants: Int? = null,
    val currentParticipantCount: Int = 0,

    val isFree: Boolean = true,
    val price: Double? = null,
    val currency: String? = "PKR",

    val imageUrls: List<String> = emptyList(),
    val mainImageUrl: String? = imageUrls.firstOrNull(),

    val tags: List<String> = emptyList(),
    val visibility: EventVisibility = EventVisibility.PUBLIC,
    val requiresTicket: Boolean = false,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null,

    // Client-side / transient fields (not persisted)
    val distanceKm: Double? = null,           // calculated from user location
    val isUserParticipating: Boolean = false,
    val isUserOrganizer: Boolean = false
)