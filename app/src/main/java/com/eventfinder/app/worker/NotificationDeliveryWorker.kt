package com.eventfinder.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await

/**
 * Worker to check and deliver scheduled notifications
 * Runs periodically to send notifications that are scheduled for a specific time
 */
@HiltWorker
class NotificationDeliveryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val now = System.currentTimeMillis()

            // Query for scheduled notifications that are due
            val dueNotifications = firestore.collection("notifications")
                .whereEqualTo("isDelivered", false)
                .whereLessThanOrEqualTo("scheduledFor", now)
                .get()
                .await()

            android.util.Log.d(TAG, "Found ${dueNotifications.size()} notifications to deliver")

            // Deliver each notification
            dueNotifications.forEach { doc ->
                try {
                    // Mark as delivered
                    doc.reference.update(
                        mapOf(
                            "isDelivered" to true,
                            "deliveredAt" to now
                        )
                    ).await()

                    // TODO: Send FCM push notification here
                    // For now, just mark as delivered in Firestore
                    android.util.Log.d(TAG, "Delivered notification: ${doc.id}")
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Failed to deliver notification: ${doc.id}", e)
                }
            }

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error in notification delivery worker", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "NotificationDeliveryWorker"
        const val WORK_NAME = "notification_delivery_work"
    }
}
