package com.eventfinder.app.domain.model

import java.util.UUID

/**
 * Domain model for Event Draft
 * Represents a partially completed event that can be saved and resumed later
 */
data class EventDraft(
    val draftId: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String? = null,
    val selectedCategories: List<EventCategory> = emptyList(),
    val startTimeMillis: Long? = null,
    val endTimeMillis: Long? = null,
    val locationName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val maxParticipants: Int? = null,
    val isFree: Boolean = true,
    val price: Double? = null,
    val currency: String? = null,
    val tags: List<String> = emptyList(),
    val visibility: EventVisibility = EventVisibility.PUBLIC,
    val requiresTicket: Boolean = false,
    val organizerId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
