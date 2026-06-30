package com.eventfinder.app.data.service

import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.model.EventNotification
import com.eventfinder.app.domain.model.NotificationRecipientType
import com.eventfinder.app.domain.model.NotificationType
import com.eventfinder.app.domain.repository.EventRepository
import com.eventfinder.app.domain.service.NotificationService
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Basic implementation of NotificationService
 * Currently stores notifications in-memory
 * Can be extended to use Firebase Cloud Messaging (FCM) and Firestore
 */
@Singleton
class NotificationServiceImpl @Inject constructor(
    private val eventRepository: EventRepository
) : NotificationService {

    // In-memory storage (will be replaced with Firestore)
    private val notifications = mutableListOf<EventNotification>()

    private val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())

    override suspend fun sendNotification(notification: EventNotification): Result<EventNotification> {
        return try {
            delay(100) // Simulate network delay

            val notificationWithId = notification.copy(
                id = UUID.randomUUID().toString(),
                notificationId = UUID.randomUUID().toString()
            ).markAsDelivered()

            notifications.add(0, notificationWithId)

            android.util.Log.d("NotificationService", "Notification sent: ${notification.type} to ${notification.recipientUserId}")

            Result.success(notificationWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendBulkNotifications(notifications: List<EventNotification>): Result<Int> {
        return try {
            var successCount = 0
            notifications.forEach { notification ->
                sendNotification(notification).onSuccess {
                    successCount++
                }
            }
            Result.success(successCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun notifyEventAttendees(
        eventId: String,
        type: NotificationType,
        title: String,
        message: String,
        metadata: Map<String, String>
    ): Result<Int> {
        return try {
            // Get event details
            val event = eventRepository.getEventById(eventId).getOrNull()
                ?: return Result.failure(Exception("Event not found"))

            // In a real implementation, get list of attendees from ticket repository
            // For now, send to a placeholder
            val notification = EventNotification(
                type = type,
                title = title,
                message = message,
                recipientUserId = "attendee_placeholder",
                recipientUserType = NotificationRecipientType.ATTENDEE,
                eventId = eventId,
                eventTitle = event.title,
                eventImageUrl = event.mainImageUrl,
                organizerId = event.organizerId,
                organizerName = event.organizerName,
                metadata = metadata
            )

            sendNotification(notification)
            Result.success(1)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun notifyEventOrganizer(
        eventId: String,
        organizerId: String,
        type: NotificationType,
        title: String,
        message: String,
        metadata: Map<String, String>
    ): Result<EventNotification> {
        return try {
            val event = eventRepository.getEventById(eventId).getOrNull()
                ?: return Result.failure(Exception("Event not found"))

            val notification = EventNotification(
                type = type,
                title = title,
                message = message,
                recipientUserId = organizerId,
                recipientUserType = NotificationRecipientType.ORGANIZER,
                eventId = eventId,
                eventTitle = event.title,
                eventImageUrl = event.mainImageUrl,
                organizerId = organizerId,
                organizerName = event.organizerName,
                metadata = metadata
            )

            sendNotification(notification)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun notifyPublicEventDiscovery(event: Event): Result<Int> {
        return Result.success(0)
    }

    override suspend fun notifyTicketCreated(
        event: Event,
        buyerName: String,
        ticketType: String,
        amount: Double,
        currency: String
    ): Result<EventNotification> {
        val eventIdToUse = event.eventId.ifEmpty { event.id }
        val isPaidTicket = amount > 0.0
        return notifyEventOrganizer(
            eventId = eventIdToUse,
            organizerId = event.organizerId,
            type = if (isPaidTicket) NotificationType.TICKET_SOLD else NotificationType.NEW_ATTENDEE,
            title = if (isPaidTicket) "Ticket sold" else "New attendee",
            message = if (isPaidTicket) {
                "$buyerName bought a ticket for ${event.title}."
            } else {
                "$buyerName reserved a ticket for ${event.title}."
            },
            metadata = mapOf(
                "buyerName" to buyerName,
                "ticketType" to ticketType,
                "amount" to amount.toString(),
                "currency" to currency
            )
        )
    }

    override suspend fun notifyEventPostponed(
        event: Event,
        reason: String,
        newStartTime: Long?,
        newEndTime: Long?
    ): Result<Int> {
        val message = if (newStartTime != null) {
            "The event has been postponed to ${dateTimeFormat.format(newStartTime)}. Reason: $reason"
        } else {
            "The event has been postponed to a date TBD. Reason: $reason"
        }

        return notifyEventAttendees(
            eventId = event.id,
            type = NotificationType.EVENT_POSTPONED,
            title = "${event.title} - Postponed",
            message = message,
            metadata = mapOf(
                "reason" to reason,
                "newStartTime" to (newStartTime?.toString() ?: "TBD")
            )
        )
    }

    override suspend fun notifyEventRescheduled(
        event: Event,
        reason: String,
        changedFields: List<String>
    ): Result<Int> {
        val changes = changedFields.joinToString(", ")
        val message = "The event has been rescheduled. Changes: $changes. Reason: $reason"

        return notifyEventAttendees(
            eventId = event.id,
            type = NotificationType.EVENT_RESCHEDULED,
            title = "${event.title} - Rescheduled",
            message = message,
            metadata = mapOf(
                "reason" to reason,
                "changedFields" to changes
            )
        )
    }

    override suspend fun notifyEventCancelled(
        event: Event,
        reason: String,
        refundStatus: String
    ): Result<Int> {
        val message = buildString {
            append("The event has been cancelled. Reason: $reason")
            if (event.hasPaidTicket()) {
                append("\nRefunds will be processed automatically. Status: $refundStatus")
            }
        }

        return notifyEventAttendees(
            eventId = event.id,
            type = NotificationType.EVENT_CANCELLED,
            title = "${event.title} - Cancelled",
            message = message,
            metadata = mapOf(
                "reason" to reason,
                "refundStatus" to refundStatus
            )
        )
    }

    override suspend fun scheduleEventReminders(event: Event): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()

            // Schedule 24h reminder
            val reminder24h = event.startTime - (24 * 60 * 60 * 1000)
            if (reminder24h > now) {
                val notification = EventNotification(
                    type = NotificationType.EVENT_STARTING_SOON_24H,
                    title = "Tomorrow: ${event.title}",
                    message = "Your event starts tomorrow at ${dateTimeFormat.format(event.startTime)}",
                    recipientUserId = "attendee_placeholder",
                    recipientUserType = NotificationRecipientType.ATTENDEE,
                    eventId = event.id,
                    eventTitle = event.title,
                    eventImageUrl = event.mainImageUrl,
                    scheduledFor = reminder24h
                )
                sendNotification(notification)
            }

            // Schedule 1h reminder
            val reminder1h = event.startTime - (60 * 60 * 1000)
            if (reminder1h > now) {
                val notification = EventNotification(
                    type = NotificationType.EVENT_STARTING_SOON_1H,
                    title = "Starting Soon: ${event.title}",
                    message = "Your event starts in 1 hour!",
                    recipientUserId = "attendee_placeholder",
                    recipientUserType = NotificationRecipientType.ATTENDEE,
                    eventId = event.id,
                    eventTitle = event.title,
                    eventImageUrl = event.mainImageUrl,
                    scheduledFor = reminder1h
                )
                sendNotification(notification)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelEventNotifications(eventId: String): Result<Unit> {
        return try {
            notifications.removeAll { it.eventId == eventId && !it.isDelivered }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUnreadNotifications(userId: String): Result<List<EventNotification>> {
        return try {
            delay(100)
            val unread = notifications.filter {
                it.recipientUserId == userId && !it.isRead && it.isActive()
            }
            Result.success(unread)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserNotifications(userId: String, limit: Int): Result<List<EventNotification>> {
        return try {
            delay(100)
            val userNotifications = notifications
                .filter { it.recipientUserId == userId && it.isActive() }
                .take(limit)
            Result.success(userNotifications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            val index = notifications.indexOfFirst { it.notificationId == notificationId }
            if (index != -1) {
                notifications[index] = notifications[index].markAsRead()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAllAsRead(userId: String): Result<Unit> {
        return try {
            notifications.replaceAll { notification ->
                if (notification.recipientUserId == userId && !notification.isRead) {
                    notification.markAsRead()
                } else {
                    notification
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            notifications.removeAll { it.notificationId == notificationId }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUnreadCount(userId: String): Result<Int> {
        return try {
            delay(50)
            val count = notifications.count {
                it.recipientUserId == userId && !it.isRead && it.isActive()
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
