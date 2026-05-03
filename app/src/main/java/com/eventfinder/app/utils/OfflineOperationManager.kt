package com.eventfinder.app.utils

import android.content.Context
import android.content.SharedPreferences
import com.eventfinder.app.domain.model.PendingCheckIn
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages offline operations queue
 */
@Singleton
class OfflineOperationManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("offline_operations", Context.MODE_PRIVATE)

    private val gson = Gson()

    private companion object {
        const val KEY_PENDING_CHECK_INS = "pending_check_ins"
    }

    /**
     * Queue a check-in operation for later
     */
    fun queueCheckIn(pendingCheckIn: PendingCheckIn) {
        val queue = getPendingCheckIns().toMutableList()
        queue.add(pendingCheckIn)
        savePendingCheckIns(queue)
    }

    /**
     * Get all pending check-ins
     */
    fun getPendingCheckIns(): List<PendingCheckIn> {
        val json = prefs.getString(KEY_PENDING_CHECK_INS, null) ?: return emptyList()
        val type = object : TypeToken<List<PendingCheckIn>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Remove a check-in from queue after successful sync
     */
    fun removeCheckIn(ticketId: String) {
        val queue = getPendingCheckIns().toMutableList()
        queue.removeAll { it.ticketId == ticketId }
        savePendingCheckIns(queue)
    }

    /**
     * Clear all pending check-ins
     */
    fun clearAllPendingCheckIns() {
        prefs.edit().remove(KEY_PENDING_CHECK_INS).apply()
    }

    /**
     * Check if there are pending operations
     */
    fun hasPendingOperations(): Boolean {
        return getPendingCheckIns().isNotEmpty()
    }

    private fun savePendingCheckIns(checkIns: List<PendingCheckIn>) {
        val json = gson.toJson(checkIns)
        prefs.edit().putString(KEY_PENDING_CHECK_INS, json).apply()
    }
}
