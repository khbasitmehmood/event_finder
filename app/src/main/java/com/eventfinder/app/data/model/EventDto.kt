package com.eventfinder.app.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint

/**
 * Data Transfer Object for Firestore Event documents.
 * This represents the exact structure stored in Firestore.
 * Separate from domain Event model for clean architecture.
 */
data class EventDto(
    @DocumentId
    val id: String = "",
    val eventId: String = "",
    val title: String = "",
    val description: String? = null,
    val category: String? = null,

    val organizerId: String = "",
    val organizerName: String = "",
    val organizerPhotoUrl: String? = null,
    val organizerSocialLinks: Map<String, String>? = null,

    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,

    val location: GeoPoint? = null,
    val geohash: String? = null,
    val address: String? = null,

    val maxParticipants: Int? = null,
    val currentParticipantCount: Int = 0,

    val isFree: Boolean = true,
    val price: Double? = null,
    val currency: String? = "PKR",

    val imageUrls: List<String> = emptyList(),
    val mainImageUrl: String? = null,

    val tags: List<String> = emptyList(),
    val visibility: String = "PUBLIC",
    val requiresTicket: Boolean = false,

    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,

    // Phase 1: State Management
    val state: String = "DRAFT",
    val publishedAt: Timestamp? = null,
    val completedAt: Timestamp? = null,
    val stateHistory: List<Map<String, Any>> = emptyList(),

    // Phase 2: Postponement
    val postponementHistory: List<Map<String, Any>> = emptyList(),
    val currentPostponement: Map<String, Any>? = null,
    val postponementCount: Int = 0,
    val maxPostponements: Int = 3,
    val allowPostponement: Boolean = true,

    // Phase 3: Rescheduling
    val rescheduleHistory: List<Map<String, Any>> = emptyList(),
    val currentReschedule: Map<String, Any>? = null,
    val rescheduleCount: Int = 0,
    val maxReschedules: Int = 5,
    val allowReschedule: Boolean = true,

    // Phase 4: Cancellation
    val cancellation: Map<String, Any>? = null,
    val cancelledAt: Timestamp? = null
)
