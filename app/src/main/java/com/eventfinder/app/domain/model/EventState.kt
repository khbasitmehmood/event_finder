package com.eventfinder.app.domain.model

/**
 * Represents the lifecycle state of an event
 */
enum class EventState {
    /**
     * Event created but not published yet
     */
    DRAFT,

    /**
     * Event published and scheduled, waiting to start
     */
    SCHEDULED,

    /**
     * Event is currently happening (between start and end time)
     */
    LIVE,

    /**
     * Event finished successfully
     */
    COMPLETED,

    /**
     * Event cancelled by organizer
     */
    CANCELLED,

    /**
     * Event has been postponed/delayed
     */
    POSTPONED,

    /**
     * Event passed without being properly managed
     */
    EXPIRED;

    /**
     * Check if event can be postponed from this state
     */
    fun canPostpone(): Boolean = this in listOf(SCHEDULED, POSTPONED)

    /**
     * Check if event can be rescheduled from this state
     */
    fun canReschedule(): Boolean = this in listOf(SCHEDULED, POSTPONED)

    /**
     * Check if event can be cancelled from this state
     */
    fun canCancel(): Boolean = this in listOf(SCHEDULED, POSTPONED, LIVE)

    /**
     * Check if event can be marked as completed from this state
     */
    fun canComplete(): Boolean = this == LIVE

    /**
     * Check if event is in an active state (not finished)
     */
    fun isActive(): Boolean = this in listOf(DRAFT, SCHEDULED, LIVE, POSTPONED)

    /**
     * Check if event is in a final state (cannot be changed)
     */
    fun isFinal(): Boolean = this in listOf(COMPLETED, CANCELLED, EXPIRED)

    /**
     * Get display name for UI
     */
    fun getDisplayName(): String = when (this) {
        DRAFT -> "Draft"
        SCHEDULED -> "Scheduled"
        LIVE -> "Live"
        COMPLETED -> "Completed"
        CANCELLED -> "Cancelled"
        POSTPONED -> "Postponed"
        EXPIRED -> "Expired"
    }

    /**
     * Get color resource for state badge
     */
    fun getColorType(): StateColorType = when (this) {
        DRAFT -> StateColorType.NEUTRAL
        SCHEDULED -> StateColorType.PRIMARY
        LIVE -> StateColorType.SUCCESS
        COMPLETED -> StateColorType.SUCCESS
        CANCELLED -> StateColorType.ERROR
        POSTPONED -> StateColorType.WARNING
        EXPIRED -> StateColorType.ERROR
    }
}

/**
 * Color types for state badges
 */
enum class StateColorType {
    PRIMARY,
    SUCCESS,
    WARNING,
    ERROR,
    NEUTRAL
}
