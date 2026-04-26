package com.eventfinder.app.utils

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages user preferences using SharedPreferences
 * Stores user ID consistently across the app
 */
@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "event_finder_prefs"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_TYPE = "user_type"
    }

    /**
     * Get or generate user ID
     * If no user ID exists, creates a new one
     */
    fun getUserId(): String {
        var userId = prefs.getString(KEY_USER_ID, null)
        if (userId == null) {
            // Generate new user ID
            userId = "user_${System.currentTimeMillis()}"
            prefs.edit().putString(KEY_USER_ID, userId).apply()
        }
        return userId
    }

    /**
     * Set user ID (for Firebase Auth integration)
     */
    fun setUserId(userId: String) {
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }

    /**
     * Get user name
     */
    fun getUserName(): String {
        return prefs.getString(KEY_USER_NAME, "Guest User") ?: "Guest User"
    }

    /**
     * Set user name
     */
    fun setUserName(name: String) {
        prefs.edit().putString(KEY_USER_NAME, name).apply()
    }

    /**
     * Get user type (default is USER)
     */
    fun getUserType(): String {
        return prefs.getString(KEY_USER_TYPE, "USER") ?: "USER"
    }

    /**
     * Set user type (USER or ORGANIZER)
     */
    fun setUserType(type: String) {
        prefs.edit().putString(KEY_USER_TYPE, type).apply()
    }

    /**
     * Clear all preferences (for logout)
     */
    fun clear() {
        prefs.edit().clear().apply()
    }
}
