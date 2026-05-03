package com.eventfinder.app.domain.model

/**
 * Represents a single state transition in event lifecycle
 */
data class StateChange(
    /**
     * Previous state
     */
    val fromState: EventState,

    /**
     * New state
     */
    val toState: EventState,

    /**
     * Timestamp when state changed
     */
    val changedAt: Long = System.currentTimeMillis(),

    /**
     * User ID who triggered the change (null for automatic transitions)
     */
    val changedBy: String? = null,

    /**
     * Reason for state change (optional)
     */
    val reason: String? = null,

    /**
     * Whether this was an automatic transition (e.g., time-based)
     */
    val automatic: Boolean = false
) {
    /**
     * Get human-readable description of this state change
     */
    fun getDescription(): String {
        val transition = "${fromState.getDisplayName()} → ${toState.getDisplayName()}"
        return when {
            automatic -> "$transition (Automatic)"
            reason != null -> "$transition: $reason"
            else -> transition
        }
    }

    /**
     * Check if this is a valid state transition
     */
    fun isValid(): Boolean {
        return when (fromState) {
            EventState.DRAFT -> toState == EventState.SCHEDULED
            EventState.SCHEDULED -> toState in listOf(
                EventState.LIVE,
                EventState.POSTPONED,
                EventState.CANCELLED,
                EventState.EXPIRED
            )
            EventState.LIVE -> toState in listOf(
                EventState.COMPLETED,
                EventState.CANCELLED
            )
            EventState.POSTPONED -> toState in listOf(
                EventState.SCHEDULED,
                EventState.CANCELLED,
                EventState.EXPIRED
            )
            EventState.COMPLETED, EventState.CANCELLED, EventState.EXPIRED -> false
        }
    }
}
