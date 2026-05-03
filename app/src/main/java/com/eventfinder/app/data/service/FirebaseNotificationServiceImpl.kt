package com.eventfinder.app.data.service

import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.model.EventNotification
import com.eventfinder.app.domain.model.NotificationRecipientType
import com.eventfinder.app.domain.model.NotificationType
import com.eventfinder.app.domain.repository.EventRepository
import com.eventfinder.app.domain.service.NotificationService
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase-based implementation of NotificationService
 * Stores notifications in Firestore and supports FCM push notifications
 */
@Singleton
class FirebaseNotificationServiceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val eventRepository: EventRepository
) : NotificationService {

    private val notificationsCollection = firestore.collection("notifications")
    private val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())

    override suspend fun sendNotification(notification: EventNotification): Result<EventNotification> {
        return try {
            val notificationWithId = notification.copy(
                id = UUID.randomUUID().toString(),
                notificationId = UUID.randomUUID().toString()
            ).markAsDelivered()

            // Save to Firestore
            notificationsCollection
                .document(notificationWithId.notificationId)
                .set(notificationWithId.toMap())
                .await()

            // TODO: Send FCM push notification
            // sendFcmNotification(notificationWithId)

            android.util.Log.d(TAG, "Notification saved: ${notification.type} to ${notification.recipientUserId}")

            Result.success(notificationWithId)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to send notification", e)
            Result.failure(e)
        }
    }

    override suspend fun sendBulkNotifications(notifications: List<EventNotification>): Result<Int> {
        return try {
            var successCount = 0

            // Use batch write for better performance
            val batch = firestore.batch()

            notifications.forEach { notification ->
                val notificationWithId = notification.copy(
                    id = UUID.randomUUID().toString(),
                    notificationId = UUID.randomUUID().toString()
                ).markAsDelivered()

                val docRef = notificationsCollection.document(notificationWithId.notificationId)
                batch.set(docRef, notificationWithId.toMap())
                successCount++
            }

            batch.commit().await()

            android.util.Log.d(TAG, "Bulk notifications sent: $successCount")
            Result.success(successCount)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to send bulk notifications", e)
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
            val event = eventRepository.getEventById(eventId).getOrNull()
                ?: return Result.failure(Exception("Event not found"))

            // Get actual attendees from tickets collection
            val attendeeIds = getEventAttendees(eventId)

            if (attendeeIds.isEmpty()) {
                android.util.Log.d(TAG, "No attendees found for event: $eventId")
                return Result.success(0)
            }

            // Create notification for each attendee
            val notifications = attendeeIds.map { userId ->
                EventNotification(
                    type = type,
                    title = title,
                    message = message,
                    recipientUserId = userId,
                    recipientUserType = NotificationRecipientType.ATTENDEE,
                    eventId = eventId,
                    eventTitle = event.title,
                    eventImageUrl = event.mainImageUrl,
                    organizerId = event.organizerId,
                    organizerName = event.organizerName,
                    metadata = metadata
                )
            }

            // Send all notifications in bulk
            sendBulkNotifications(notifications)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to notify attendees", e)
            Result.failure(e)
        }
    }

    /**
     * Get list of unique user IDs who have tickets for this event
     */
    private suspend fun getEventAttendees(eventId: String): List<String> {
        return try {
            val ticketsSnapshot = firestore.collection("tickets")
                .whereEqualTo("eventId", eventId)
                .get()
                .await()

            val userIds = ticketsSnapshot.documents
                .mapNotNull { it.getString("userId") }
                .distinct()

            android.util.Log.d(TAG, "Found ${userIds.size} attendees for event: $eventId")
            userIds
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to get attendees for event: $eventId", e)
            emptyList()
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
            if (!event.isFree) {
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

            // Get attendees for this event
            val attendeeIds = getEventAttendees(event.id)

            if (attendeeIds.isEmpty()) {
                android.util.Log.d(TAG, "No attendees to send reminders for event: ${event.id}")
                return Result.success(Unit)
            }

            val notifications24h = mutableListOf<EventNotification>()
            val notifications1h = mutableListOf<EventNotification>()

            // Schedule 24h reminder for each attendee
            val reminder24h = event.startTime - (24 * 60 * 60 * 1000)
            if (reminder24h > now) {
                attendeeIds.forEach { userId ->
                    notifications24h.add(
                        EventNotification(
                            type = NotificationType.EVENT_STARTING_SOON_24H,
                            title = "Tomorrow: ${event.title}",
                            message = "Your event starts tomorrow at ${dateTimeFormat.format(event.startTime)}",
                            recipientUserId = userId,
                            recipientUserType = NotificationRecipientType.ATTENDEE,
                            eventId = event.id,
                            eventTitle = event.title,
                            eventImageUrl = event.mainImageUrl,
                            scheduledFor = reminder24h
                        )
                    )
                }
            }

            // Schedule 1h reminder for each attendee
            val reminder1h = event.startTime - (60 * 60 * 1000)
            if (reminder1h > now) {
                attendeeIds.forEach { userId ->
                    notifications1h.add(
                        EventNotification(
                            type = NotificationType.EVENT_STARTING_SOON_1H,
                            title = "Starting Soon: ${event.title}",
                            message = "Your event starts in 1 hour!",
                            recipientUserId = userId,
                            recipientUserType = NotificationRecipientType.ATTENDEE,
                            eventId = event.id,
                            eventTitle = event.title,
                            eventImageUrl = event.mainImageUrl,
                            scheduledFor = reminder1h
                        )
                    )
                }
            }

            // Send all reminders in bulk
            if (notifications24h.isNotEmpty()) {
                sendBulkNotifications(notifications24h)
            }
            if (notifications1h.isNotEmpty()) {
                sendBulkNotifications(notifications1h)
            }

            android.util.Log.d(TAG, "Scheduled reminders for ${attendeeIds.size} attendees")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelEventNotifications(eventId: String): Result<Unit> {
        return try {
            notificationsCollection
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("isDelivered", false)
                .get()
                .await()
                .documents
                .forEach { doc ->
                    doc.reference.delete().await()
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUnreadNotifications(userId: String): Result<List<EventNotification>> {
        return try {
            val documents = notificationsCollection
                .whereEqualTo("recipientUserId", userId)
                .whereEqualTo("isRead", false)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val notifications = documents.mapNotNull { doc ->
                doc.toEventNotification()
            }.filter { it.isActive() }

            Result.success(notifications)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to get unread notifications", e)
            Result.failure(e)
        }
    }

    override suspend fun getUserNotifications(userId: String, limit: Int): Result<List<EventNotification>> {
        return try {
            val documents = notificationsCollection
                .whereEqualTo("recipientUserId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val notifications = documents.mapNotNull { doc ->
                doc.toEventNotification()
            }.filter { it.isActive() }

            Result.success(notifications)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to get user notifications", e)
            Result.failure(e)
        }
    }

    override suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            notificationsCollection
                .document(notificationId)
                .update(
                    mapOf(
                        "isRead" to true,
                        "readAt" to System.currentTimeMillis()
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAllAsRead(userId: String): Result<Unit> {
        return try {
            val batch = firestore.batch()

            val documents = notificationsCollection
                .whereEqualTo("recipientUserId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .await()

            documents.forEach { doc ->
                batch.update(
                    doc.reference,
                    mapOf(
                        "isRead" to true,
                        "readAt" to System.currentTimeMillis()
                    )
                )
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            notificationsCollection
                .document(notificationId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUnreadCount(userId: String): Result<Int> {
        return try {
            val documents = notificationsCollection
                .whereEqualTo("recipientUserId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .await()

            val count = documents.mapNotNull { doc ->
                doc.toEventNotification()
            }.count { it.isActive() }

            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "FirebaseNotificationService"
    }
}

/**
 * Extension function to convert EventNotification to Map for Firestore
 */
private fun EventNotification.toMap(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "notificationId" to notificationId,
        "type" to type.name,
        "title" to title,
        "message" to message,
        "priority" to priority.name,
        "recipientUserId" to recipientUserId,
        "recipientUserType" to recipientUserType.name,
        "eventId" to eventId,
        "eventTitle" to eventTitle,
        "eventImageUrl" to eventImageUrl,
        "organizerId" to organizerId,
        "organizerName" to organizerName,
        "metadata" to metadata,
        "actionUrl" to actionUrl,
        "actionLabel" to actionLabel,
        "isRead" to isRead,
        "isDelivered" to isDelivered,
        "deliveredAt" to deliveredAt,
        "readAt" to readAt,
        "createdAt" to createdAt,
        "scheduledFor" to scheduledFor,
        "expiresAt" to expiresAt
    )
}

/**
 * Extension function to convert Firestore document to EventNotification
 */
private fun com.google.firebase.firestore.DocumentSnapshot.toEventNotification(): EventNotification? {
    return try {
        EventNotification(
            id = getString("id") ?: "",
            notificationId = getString("notificationId") ?: "",
            type = NotificationType.valueOf(getString("type") ?: "SYSTEM_ANNOUNCEMENT"),
            title = getString("title") ?: "",
            message = getString("message") ?: "",
            priority = com.eventfinder.app.domain.model.NotificationPriority.valueOf(
                getString("priority") ?: "NORMAL"
            ),
            recipientUserId = getString("recipientUserId") ?: "",
            recipientUserType = NotificationRecipientType.valueOf(
                getString("recipientUserType") ?: "ATTENDEE"
            ),
            eventId = getString("eventId") ?: "",
            eventTitle = getString("eventTitle") ?: "",
            eventImageUrl = getString("eventImageUrl"),
            organizerId = getString("organizerId"),
            organizerName = getString("organizerName"),
            metadata = get("metadata") as? Map<String, String> ?: emptyMap(),
            actionUrl = getString("actionUrl"),
            actionLabel = getString("actionLabel"),
            isRead = getBoolean("isRead") ?: false,
            isDelivered = getBoolean("isDelivered") ?: false,
            deliveredAt = getLong("deliveredAt"),
            readAt = getLong("readAt"),
            createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
            scheduledFor = getLong("scheduledFor"),
            expiresAt = getLong("expiresAt")
        )
    } catch (e: Exception) {
        android.util.Log.e("FirebaseNotificationService", "Failed to parse notification", e)
        null
    }
}
