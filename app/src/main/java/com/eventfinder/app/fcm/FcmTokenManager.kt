package com.eventfinder.app.fcm

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages FCM token retrieval and updates
 */
@Singleton
class FcmTokenManager @Inject constructor(
    private val firestore: FirebaseFirestore
) {

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

    suspend fun saveCurrentTokenForUser(
        userId: String,
        notificationsEnabled: Boolean = true
    ): Result<String> {
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            saveTokenForUser(userId, token, notificationsEnabled).getOrThrow()
            Result.success(token)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to save current FCM token for user: $userId", e)
            Result.failure(e)
        }
    }

    suspend fun saveTokenForUser(
        userId: String,
        token: String,
        notificationsEnabled: Boolean = true
    ): Result<Unit> {
        return try {
            if (userId.isBlank() || token.isBlank()) {
                return Result.failure(IllegalArgumentException("User ID and FCM token are required"))
            }

            firestore
                .collection("users")
                .document(userId)
                .collection(TOKEN_COLLECTION)
                .document(token.sha256())
                .set(
                    mapOf(
                        "token" to token,
                        "platform" to "android",
                        "notificationsEnabled" to notificationsEnabled,
                        "updatedAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )
                .await()

            android.util.Log.d(TAG, "Saved FCM token for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to save FCM token for user: $userId", e)
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
        private const val TOKEN_COLLECTION = "fcmTokens"

        // Common topics
        const val TOPIC_ALL_USERS = "all_users"
        const val TOPIC_ORGANIZERS = "organizers"
        const val TOPIC_ATTENDEES = "attendees"
    }
}

fun saveFcmTokenForUser(
    userId: String,
    token: String,
    notificationsEnabled: Boolean
) {
    if (userId.isBlank() || token.isBlank()) return

    Firebase.firestore
        .collection("users")
        .document(userId)
        .collection("fcmTokens")
        .document(token.sha256())
        .set(
            mapOf(
                "token" to token,
                "platform" to "android",
                "notificationsEnabled" to notificationsEnabled,
                "updatedAt" to System.currentTimeMillis()
            ),
            SetOptions.merge()
        )
        .addOnSuccessListener {
            android.util.Log.d("FcmTokenManager", "Saved refreshed FCM token for user: $userId")
        }
        .addOnFailureListener { error ->
            android.util.Log.e("FcmTokenManager", "Failed to save refreshed FCM token", error)
        }
}

private fun String.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}
