package com.eventfinder.app.domain.model

/**
 * User's notification preferences
 */
data class NotificationPreferences(
    val userId: String,

    // Global settings
    val notificationsEnabled: Boolean = true,
    val pushNotificationsEnabled: Boolean = true,
    val inAppNotificationsEnabled: Boolean = true,
    val emailNotificationsEnabled: Boolean = false,

    // Event lifecycle notifications (for attendees)
    val eventPostponedEnabled: Boolean = true,
    val eventRescheduledEnabled: Boolean = true,
    val eventCancelledEnabled: Boolean = true,
    val eventStartingSoonEnabled: Boolean = true,
    val eventStartedEnabled: Boolean = true,
    val eventCompletedEnabled: Boolean = false,
    val eventDetailsChangedEnabled: Boolean = true,

    // Ticket notifications (for attendees)
    val ticketPurchasedEnabled: Boolean = true,
    val refundNotificationsEnabled: Boolean = true,

    // Organizer notifications
    val newAttendeeEnabled: Boolean = true,
    val capacityMilestonesEnabled: Boolean = true,
    val eventStateChangesEnabled: Boolean = true,
    val refundAlertsEnabled: Boolean = true,

    // Timing preferences
    val reminder24hEnabled: Boolean = true,
    val reminder1hEnabled: Boolean = true,

    // Quiet hours
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: Int = 22, // Hour (0-23)
    val quietHoursEnd: Int = 8,    // Hour (0-23)

    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Check if notification type is enabled
     */
    fun isNotificationTypeEnabled(type: NotificationType): Boolean {
        if (!notificationsEnabled) return false

        return when (type) {
            NotificationType.EVENT_POSTPONED -> eventPostponedEnabled
            NotificationType.EVENT_RESCHEDULED -> eventRescheduledEnabled
            NotificationType.EVENT_CANCELLED -> eventCancelledEnabled
            NotificationType.EVENT_STARTING_SOON_24H -> eventStartingSoonEnabled && reminder24hEnabled
            NotificationType.EVENT_STARTING_SOON_1H -> eventStartingSoonEnabled && reminder1hEnabled
            NotificationType.EVENT_STARTED -> eventStartedEnabled
            NotificationType.EVENT_COMPLETED -> eventCompletedEnabled
            NotificationType.EVENT_DETAILS_CHANGED -> eventDetailsChangedEnabled

            NotificationType.TICKET_PURCHASED, NotificationType.TICKET_CONFIRMED -> ticketPurchasedEnabled
            NotificationType.REFUND_INITIATED, NotificationType.REFUND_COMPLETED,
            NotificationType.REFUND_FAILED -> refundNotificationsEnabled

            NotificationType.NEW_ATTENDEE -> newAttendeeEnabled
            NotificationType.CAPACITY_MILESTONE_50, NotificationType.CAPACITY_MILESTONE_75,
            NotificationType.CAPACITY_MILESTONE_90, NotificationType.CAPACITY_FULL -> capacityMilestonesEnabled
            NotificationType.EVENT_STATE_CHANGED, NotificationType.EVENT_ABOUT_TO_START,
            NotificationType.EVENT_ENDED_MARK_COMPLETE -> eventStateChangesEnabled
            NotificationType.REFUNDS_INITIATED, NotificationType.REFUNDS_COMPLETED,
            NotificationType.REFUND_ACTION_NEEDED -> refundAlertsEnabled

            else -> true // Enable by default for types not explicitly configured
        }
    }

    /**
     * Check if currently in quiet hours
     */
    fun isInQuietHours(): Boolean {
        if (!quietHoursEnabled) return false

        val now = java.util.Calendar.getInstance()
        val currentHour = now.get(java.util.Calendar.HOUR_OF_DAY)

        return if (quietHoursStart < quietHoursEnd) {
            currentHour in quietHoursStart until quietHoursEnd
        } else {
            // Quiet hours span midnight
            currentHour >= quietHoursStart || currentHour < quietHoursEnd
        }
    }

    /**
     * Check if notification should be delivered now
     */
    fun shouldDeliverNotification(type: NotificationType): Boolean {
        if (!isNotificationTypeEnabled(type)) return false

        // Always deliver urgent notifications, even during quiet hours
        if (type.getPriority() == NotificationPriority.URGENT) return true

        return !isInQuietHours()
    }
}
