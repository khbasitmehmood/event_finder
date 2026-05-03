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
    val updatedAt: Timestamp? = null
)
