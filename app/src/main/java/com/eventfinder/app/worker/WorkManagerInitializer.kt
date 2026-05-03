package com.eventfinder.app.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Initializer for WorkManager background jobs
 * Schedules periodic work for event state updates
 */
object WorkManagerInitializer {

    /**
     * Schedule all background workers
     * Should be called from Application.onCreate()
     */
    fun initialize(context: Context) {
        scheduleEventStateUpdateWorker(context)
        scheduleNotificationDeliveryWorker(context)
    }

    /**
     * Schedule periodic event state update worker
     * Runs every 15 minutes to check for events needing state updates
     */
    private fun scheduleEventStateUpdateWorker(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Need network to update Firestore
            .build()

        val workRequest = PeriodicWorkRequestBuilder<EventStateUpdateWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            EventStateUpdateWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Don't replace if already scheduled
            workRequest
        )

        android.util.Log.d("WorkManagerInitializer", "Event state update worker scheduled")
    }

    /**
     * Schedule periodic notification delivery worker
     * Runs every 15 minutes to check for scheduled notifications
     */
    private fun scheduleNotificationDeliveryWorker(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<NotificationDeliveryWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            NotificationDeliveryWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        android.util.Log.d("WorkManagerInitializer", "Notification delivery worker scheduled")
    }

    /**
     * Cancel all scheduled workers
     * Useful for testing or manual cleanup
     */
    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelAllWork()
    }
}
