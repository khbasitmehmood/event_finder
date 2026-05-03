package com.eventfinder.app.domain.model

/**
 * Represents a postponement/delay of an event
 */
data class EventPostponement(
    /**
     * Original start time before postponement
     */
    val originalStartTime: Long,

    /**
     * Original end time before postponement
     */
    val originalEndTime: Long?,

    /**
     * New start time (null if TBD - To Be Determined)
     */
    val newStartTime: Long? = null,

    /**
     * New end time (null if TBD or no end time)
     */
    val newEndTime: Long? = null,

    /**
     * Reason for postponement (required)
     */
    val reason: String,

    /**
     * Timestamp when event was postponed
     */
    val postponedAt: Long = System.currentTimeMillis(),

    /**
     * User ID who postponed the event (organizer)
     */
    val postponedBy: String,

    /**
     * Whether notification has been sent to attendees
     */
    val notificationSent: Boolean = false
) {
    /**
     * Check if new date is determined
     */
    fun isDateDetermined(): Boolean = newStartTime != null

    /**
     * Check if this is TBD (To Be Determined)
     */
    fun isTBD(): Boolean = newStartTime == null

    /**
     * Get duration of postponement in days (if dates known)
     */
    fun getPostponementDurationDays(): Int? {
        return if (newStartTime != null) {
            val durationMillis = newStartTime - originalStartTime
            (durationMillis / (24 * 60 * 60 * 1000)).toInt()
        } else {
            null
        }
    }

    /**
     * Get human-readable postponement description
     */
    fun getDescription(): String {
        return when {
            isTBD() -> "Event postponed - New date TBD. Reason: $reason"
            else -> {
                val days = getPostponementDurationDays() ?: 0
                when {
                    days == 0 -> "Event postponed to same day. Reason: $reason"
                    days == 1 -> "Event postponed by 1 day. Reason: $reason"
                    days > 1 -> "Event postponed by $days days. Reason: $reason"
                    else -> "Event rescheduled to earlier date. Reason: $reason"
                }
            }
        }
    }
}
