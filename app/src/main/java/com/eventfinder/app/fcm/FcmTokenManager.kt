package com.eventfinder.app.fcm

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages FCM token retrieval and updates
 */
@Singleton
class FcmTokenManager @Inject constructor() {

    /**
     * Get current FCM token
     */
    suspend fun getToken(): Result<String> {
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            android.util.Log.d(TAG, "FCM Token: $token")
            Result.success(token)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to get FCM token", e)
            Result.failure(e)
        }
    }

    /**
     * Subscribe to a topic
     * Useful for broadcasting notifications to all users or specific groups
     */
    suspend fun subscribeToTopic(topic: String): Result<Unit> {
        return try {
            FirebaseMessaging.getInstance()
                .subscribeToTopic(topic)
                .await()
            android.util.Log.d(TAG, "Subscribed to topic: $topic")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to subscribe to topic: $topic", e)
            Result.failure(e)
        }
    }

    /**
     * Unsubscribe from a topic
     */
    suspend fun unsubscribeFromTopic(topic: String): Result<Unit> {
        return try {
            FirebaseMessaging.getInstance()
                .unsubscribeFromTopic(topic)
                .await()
            android.util.Log.d(TAG, "Unsubscribed from topic: $topic")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to unsubscribe from topic: $topic", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "FcmTokenManager"

        // Common topics
        const val TOPIC_ALL_USERS = "all_users"
        const val TOPIC_ORGANIZERS = "organizers"
        const val TOPIC_ATTENDEES = "attendees"
    }
}
