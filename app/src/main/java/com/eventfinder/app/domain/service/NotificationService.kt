package com.eventfinder.app.domain.service

import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.model.EventNotification
import com.eventfinder.app.domain.model.NotificationType

/**
 * Service interface for managing event notifications
 */
interface NotificationService {
    /**
     * Send notification to specific user
     */
    suspend fun sendNotification(notification: EventNotification): Result<EventNotification>

    /**
     * Send notification to multiple users
     */
    suspend fun sendBulkNotifications(notifications: List<EventNotification>): Result<Int>

    /**
     * Notify all event attendees
     */
    suspend fun notifyEventAttendees(
        eventId: String,
        type: NotificationType,
        title: String,
        message: String,
        metadata: Map<String, String> = emptyMap()
    ): Result<Int>

    /**
     * Notify event organizer
     */
    suspend fun notifyEventOrganizer(
        eventId: String,
        organizerId: String,
        type: NotificationType,
        title: String,
        message: String,
        metadata: Map<String, String> = emptyMap()
    ): Result<EventNotification>

    /**
     * Notify users whose interests or saved location match a newly published public event.
     */
    suspend fun notifyPublicEventDiscovery(event: Event): Result<Int>

    /**
     * Notify organizer when a ticket is sold or reserved.
     */
    suspend fun notifyTicketCreated(
        event: Event,
        buyerName: String,
        ticketType: String,
        amount: Double,
        currency: String
    ): Result<EventNotification>

    /**
     * Create notification for event postponement
     */
    suspend fun notifyEventPostponed(
        event: Event,
        reason: String,
        newStartTime: Long?,
        newEndTime: Long?
    ): Result<Int>

    /**
     * Create notification for event rescheduling
     */
    suspend fun notifyEventRescheduled(
        event: Event,
        reason: String,
        changedFields: List<String>
    ): Result<Int>

    /**
     * Create notification for event cancellation
     */
    suspend fun notifyEventCancelled(
        event: Event,
        reason: String,
        refundStatus: String
    ): Result<Int>

    /**
     * Schedule reminder notifications for event
     */
    suspend fun scheduleEventReminders(event: Event): Result<Unit>

    /**
     * Cancel scheduled notifications for event
     */
    suspend fun cancelEventNotifications(eventId: String): Result<Unit>

    /**
     * Get unread notifications for user
     */
    suspend fun getUnreadNotifications(userId: String): Result<List<EventNotification>>

    /**
     * Get all notifications for user
     */
    suspend fun getUserNotifications(
        userId: String,
        limit: Int = 50
    ): Result<List<EventNotification>>

    /**
     * Mark notification as read
     */
    suspend fun markAsRead(notificationId: String): Result<Unit>

    /**
     * Mark all notifications as read for user
     */
    suspend fun markAllAsRead(userId: String): Result<Unit>

    /**
     * Delete notification
     */
    suspend fun deleteNotification(notificationId: String): Result<Unit>

    /**
     * Get unread count for user
     */
    suspend fun getUnreadCount(userId: String): Result<Int>
}
