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

    // State Management (Phase 1)
    val state: EventState = EventState.DRAFT,
    val publishedAt: Long? = null,
    val completedAt: Long? = null,
    val stateHistory: List<StateChange> = emptyList(),

    // Postponement (Phase 2)
    val postponementHistory: List<EventPostponement> = emptyList(),
    val currentPostponement: EventPostponement? = null,
    val postponementCount: Int = 0,
    val maxPostponements: Int = 3,
    val allowPostponement: Boolean = true,

    // Rescheduling (Phase 3)
    val rescheduleHistory: List<EventReschedule> = emptyList(),
    val currentReschedule: EventReschedule? = null,
    val rescheduleCount: Int = 0,
    val maxReschedules: Int = 5,
    val allowReschedule: Boolean = true,

    // Cancellation (Phase 4)
    val cancellation: EventCancellation? = null,
    val cancelledAt: Long? = null,

    // Client-side / transient fields (not persisted)
    val distanceKm: Double? = null,           // calculated from user location
    val isUserParticipating: Boolean = false,
    val isUserOrganizer: Boolean = false
) {
    /**
     * Check if event is currently live (happening now)
     */
    fun isLive(): Boolean {
        val now = System.currentTimeMillis()
        return state == EventState.LIVE || (
            state == EventState.SCHEDULED &&
            startTime <= now &&
            (endTime == null || endTime >= now)
        )
    }

    /**
     * Check if event has ended
     */
    fun hasEnded(): Boolean {
        val now = System.currentTimeMillis()
        return state in listOf(EventState.COMPLETED, EventState.EXPIRED) ||
            (endTime != null && endTime < now)
    }

    /**
     * Check if event is upcoming
     */
    fun isUpcoming(): Boolean {
        val now = System.currentTimeMillis()
        return state in listOf(EventState.SCHEDULED, EventState.POSTPONED) &&
            startTime > now
    }

    /**
     * Get the current effective state based on time
     */
    fun getEffectiveState(): EventState {
        if (state.isFinal()) return state

        val now = System.currentTimeMillis()
        return when {
            state == EventState.SCHEDULED && startTime <= now && (endTime == null || endTime >= now) -> EventState.LIVE
            state == EventState.LIVE && endTime != null && endTime < now -> EventState.COMPLETED
            else -> state
        }
    }

    /**
     * Check if event can be postponed
     */
    fun canPostpone(): Boolean {
        return allowPostponement &&
                state.canPostpone() &&
                postponementCount < maxPostponements &&
                !hasEnded()
    }

    /**
     * Check if event is currently postponed
     */
    fun isPostponed(): Boolean {
        return state == EventState.POSTPONED && currentPostponement != null
    }

    /**
     * Get remaining postponement attempts
     */
    fun getRemainingPostponements(): Int {
        return maxOf(0, maxPostponements - postponementCount)
    }

    /**
     * Check if event can be rescheduled
     */
    fun canReschedule(): Boolean {
        return allowReschedule &&
                state.canReschedule() &&
                rescheduleCount < maxReschedules &&
                !hasEnded()
    }

    /**
     * Check if event is currently rescheduled
     */
    fun isRescheduled(): Boolean {
        return currentReschedule != null
    }

    /**
     * Get remaining reschedule attempts
     */
    fun getRemainingReschedules(): Int {
        return maxOf(0, maxReschedules - rescheduleCount)
    }

    /**
     * Check if event can be cancelled
     */
    fun canCancel(): Boolean {
        return state.canCancel() && !hasEnded()
    }

    /**
     * Check if event is cancelled
     */
    fun isCancelled(): Boolean {
        return state == EventState.CANCELLED && cancellation != null
    }

    /**
     * Returns true when the event has a paid ticket.
     */
    fun hasPaidTicket(): Boolean {
        return (price ?: 0.0) > 0.0
    }

    fun requiresPaidCheckout(): Boolean {
        return requiresTicket && hasPaidTicket()
    }
}
