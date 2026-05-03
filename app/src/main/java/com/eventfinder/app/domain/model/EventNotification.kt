package com.eventfinder.app.domain.model

/**
 * Represents a notification about an event
 */
data class EventNotification(
    val id: String = "",
    val notificationId: String = "",

    // Core fields
    val type: NotificationType,
    val title: String,
    val message: String,
    val priority: NotificationPriority = type.getPriority(),

    // Recipients
    val recipientUserId: String,
    val recipientUserType: NotificationRecipientType, // ATTENDEE or ORGANIZER

    // Related entities
    val eventId: String,
    val eventTitle: String,
    val eventImageUrl: String? = null,
    val organizerId: String? = null,
    val organizerName: String? = null,

    // Additional context
    val metadata: Map<String, String> = emptyMap(),
    val actionUrl: String? = null, // Deep link to relevant screen
    val actionLabel: String? = null,

    // Status
    val isRead: Boolean = false,
    val isDelivered: Boolean = false,
    val deliveredAt: Long? = null,
    val readAt: Long? = null,

    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val scheduledFor: Long? = null, // For scheduled notifications
    val expiresAt: Long? = null
) {
    /**
     * Check if notification is expired
     */
    fun isExpired(): Boolean {
        return expiresAt != null && System.currentTimeMillis() > expiresAt
    }

    /**
     * Check if notification should be shown now
     */
    fun isScheduledForFuture(): Boolean {
        return scheduledFor != null && System.currentTimeMillis() < scheduledFor
    }

    /**
     * Check if notification is active
     */
    fun isActive(): Boolean = !isExpired() && !isScheduledForFuture()

    /**
     * Get display time (relative)
     */
    fun getDisplayTime(): String {
        val now = System.currentTimeMillis()
        val diff = now - createdAt

        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            diff < 604800_000 -> "${diff / 86400_000}d ago"
            else -> "${diff / 604800_000}w ago"
        }
    }

    /**
     * Mark notification as read
     */
    fun markAsRead(): EventNotification {
        return copy(
            isRead = true,
            readAt = System.currentTimeMillis()
        )
    }

    /**
     * Mark notification as delivered
     */
    fun markAsDelivered(): EventNotification {
        return copy(
            isDelivered = true,
            deliveredAt = System.currentTimeMillis()
        )
    }
}

/**
 * Recipient type for notification targeting
 */
enum class NotificationRecipientType {
    ATTENDEE,
    ORGANIZER,
    BOTH;

    fun getDisplayName(): String = when (this) {
        ATTENDEE -> "Attendee"
        ORGANIZER -> "Organizer"
        BOTH -> "All Users"
    }
}
