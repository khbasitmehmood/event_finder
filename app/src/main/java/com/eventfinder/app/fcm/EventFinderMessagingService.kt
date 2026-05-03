package com.eventfinder.app.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.eventfinder.app.MainActivity
import com.eventfinder.app.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Service to handle Firebase Cloud Messaging push notifications
 */
class EventFinderMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        android.util.Log.d(TAG, "New FCM token: $token")

        // TODO: Send token to your server to enable push notifications for this device
        // For now, just log it
        // In production: sendTokenToServer(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        android.util.Log.d(TAG, "Message received from: ${message.from}")

        // Check if message contains data payload
        if (message.data.isNotEmpty()) {
            android.util.Log.d(TAG, "Message data: ${message.data}")
            handleDataMessage(message.data)
        }

        // Check if message contains notification payload
        message.notification?.let {
            android.util.Log.d(TAG, "Message notification: ${it.title} - ${it.body}")
            showNotification(
                title = it.title ?: "Event Finder",
                message = it.body ?: "",
                data = message.data
            )
        }
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val notificationType = data["type"] ?: return
        val title = data["title"] ?: "Event Finder"
        val message = data["message"] ?: ""
        val eventId = data["eventId"]
        val priority = data["priority"] ?: "NORMAL"

        // Show notification
        showNotification(
            title = title,
            message = message,
            data = data,
            channelId = getChannelIdForPriority(priority)
        )
    }

    private fun showNotification(
        title: String,
        message: String,
        data: Map<String, String>,
        channelId: String = CHANNEL_ID_DEFAULT
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel (Android 8.0+)
        createNotificationChannels(notificationManager)

        // Create intent to open app
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            // Add event ID if available for deep linking
            data["eventId"]?.let { putExtra("eventId", it) }
            data["notificationId"]?.let { putExtra("notificationId", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(getNotificationPriority(data["priority"]))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Show notification
        val notificationId = data["notificationId"]?.hashCode() ?: System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)
    }

    private fun createNotificationChannels(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Default channel
            val defaultChannel = NotificationChannel(
                CHANNEL_ID_DEFAULT,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General event notifications"
            }

            // High priority channel
            val highPriorityChannel = NotificationChannel(
                CHANNEL_ID_HIGH_PRIORITY,
                "Important Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important event updates (cancellations, rescheduling)"
            }

            // Urgent channel
            val urgentChannel = NotificationChannel(
                CHANNEL_ID_URGENT,
                "Urgent Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent notifications requiring immediate attention"
                enableVibration(true)
            }

            // Reminders channel
            val remindersChannel = NotificationChannel(
                CHANNEL_ID_REMINDERS,
                "Event Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for upcoming events"
            }

            notificationManager.createNotificationChannels(
                listOf(defaultChannel, highPriorityChannel, urgentChannel, remindersChannel)
            )
        }
    }

    private fun getChannelIdForPriority(priority: String): String {
        return when (priority) {
            "URGENT" -> CHANNEL_ID_URGENT
            "HIGH" -> CHANNEL_ID_HIGH_PRIORITY
            "LOW", "NORMAL" -> CHANNEL_ID_DEFAULT
            else -> CHANNEL_ID_DEFAULT
        }
    }

    private fun getNotificationPriority(priority: String?): Int {
        return when (priority) {
            "URGENT", "HIGH" -> NotificationCompat.PRIORITY_HIGH
            "NORMAL" -> NotificationCompat.PRIORITY_DEFAULT
            "LOW" -> NotificationCompat.PRIORITY_LOW
            else -> NotificationCompat.PRIORITY_DEFAULT
        }
    }

    companion object {
        private const val TAG = "FCMService"

        // Notification channels
        const val CHANNEL_ID_DEFAULT = "event_notifications"
        const val CHANNEL_ID_HIGH_PRIORITY = "event_notifications_high"
        const val CHANNEL_ID_URGENT = "event_notifications_urgent"
        const val CHANNEL_ID_REMINDERS = "event_reminders"
    }
}
