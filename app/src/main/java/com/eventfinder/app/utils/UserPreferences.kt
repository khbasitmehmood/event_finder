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
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_TYPE = "user_type"
        private const val KEY_PENDING_AUTH_STEP = "pending_auth_step"
        private const val KEY_PENDING_PAYMENT_CHECKOUT_ID = "pending_payment_checkout_id"
        private const val KEY_PENDING_PAYMENT_EVENT_ID = "pending_payment_event_id"
        private const val KEY_PENDING_PAYMENT_USER_ID = "pending_payment_user_id"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_USER_LOCATION_ADDRESS = "user_location_address"
        private const val KEY_USER_LOCATION_LAT = "user_location_lat"
        private const val KEY_USER_LOCATION_LNG = "user_location_lng"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_SYSTEM = "system"
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
     * Get user email
     */
    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    /**
     * Set user email
     */
    fun setUserEmail(email: String) {
        prefs.edit().putString(KEY_USER_EMAIL, email).apply()
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
     * Get pending auth/onboarding step, if any.
     */
    fun getPendingAuthStep(): String? {
        return prefs.getString(KEY_PENDING_AUTH_STEP, null)
    }

    /**
     * Set pending auth/onboarding step.
     */
    fun setPendingAuthStep(step: String) {
        prefs.edit().putString(KEY_PENDING_AUTH_STEP, step).apply()
    }

    /**
     * Clear pending auth/onboarding step.
     */
    fun clearPendingAuthStep() {
        prefs.edit().remove(KEY_PENDING_AUTH_STEP).apply()
    }

    fun setPendingPayment(checkoutId: String, eventId: String, userId: String) {
        prefs.edit()
            .putString(KEY_PENDING_PAYMENT_CHECKOUT_ID, checkoutId)
            .putString(KEY_PENDING_PAYMENT_EVENT_ID, eventId)
            .putString(KEY_PENDING_PAYMENT_USER_ID, userId)
            .apply()
    }

    fun getPendingPaymentCheckoutId(): String? {
        return prefs.getString(KEY_PENDING_PAYMENT_CHECKOUT_ID, null)
    }

    fun getPendingPaymentEventId(): String? {
        return prefs.getString(KEY_PENDING_PAYMENT_EVENT_ID, null)
    }

    fun getPendingPaymentUserId(): String? {
        return prefs.getString(KEY_PENDING_PAYMENT_USER_ID, null)
    }

    fun clearPendingPayment() {
        prefs.edit()
            .remove(KEY_PENDING_PAYMENT_CHECKOUT_ID)
            .remove(KEY_PENDING_PAYMENT_EVENT_ID)
            .remove(KEY_PENDING_PAYMENT_USER_ID)
            .apply()
    }

    fun areNotificationsEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun getThemeMode(): String {
        return prefs.getString(KEY_THEME_MODE, THEME_SYSTEM) ?: THEME_SYSTEM
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
    }

    fun setUserLocation(address: String, latitude: Double, longitude: Double) {
        prefs.edit()
            .putString(KEY_USER_LOCATION_ADDRESS, address)
            .putLong(KEY_USER_LOCATION_LAT, latitude.toRawBits())
            .putLong(KEY_USER_LOCATION_LNG, longitude.toRawBits())
            .apply()
    }

    fun getUserLocationAddress(): String? {
        return prefs.getString(KEY_USER_LOCATION_ADDRESS, null)
    }

    fun getUserLocationLatitude(): Double? {
        return if (prefs.contains(KEY_USER_LOCATION_LAT)) {
            Double.fromBits(prefs.getLong(KEY_USER_LOCATION_LAT, 0L))
        } else {
            null
        }
    }

    fun getUserLocationLongitude(): Double? {
        return if (prefs.contains(KEY_USER_LOCATION_LNG)) {
            Double.fromBits(prefs.getLong(KEY_USER_LOCATION_LNG, 0L))
        } else {
            null
        }
    }

    /**
     * Clear all preferences (for logout)
     */
    fun clear() {
        prefs.edit().clear().apply()
    }

}
