package com.eventfinder.app.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Helper object to manually trigger event state update worker
 * Useful for testing or immediate execution
 */
object EventStateUpdateTrigger {

    /**
     * Trigger an immediate one-time event state update check
     * Useful for testing or when you need immediate state sync
     */
    fun triggerImmediateUpdate(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<EventStateUpdateWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)

        android.util.Log.d("EventStateUpdateTrigger", "Immediate state update triggered")
    }
}
