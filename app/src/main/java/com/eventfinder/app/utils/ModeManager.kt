package com.eventfinder.app.utils

import android.content.Context

/**
 * @deprecated ModeManager is deprecated and preserved for future actual admin features.
 * Organizers should not use admin mode - they use OrganizerDashboardFragment directly via user type routing.
 * TODO: Admin mode system preserved for future actual admin panel features
 */
@Deprecated("Preserved for future admin features. Organizers use user type-based routing instead.")
object ModeManager {

    private const val PREF_NAME = "app_mode_pref"
    private const val KEY_ADMIN = "is_admin"

    fun setAdminMode(context: Context, isAdmin: Boolean) {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        pref.edit().putBoolean(KEY_ADMIN, isAdmin).apply()
    }

    fun isAdminMode(context: Context): Boolean {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return pref.getBoolean(KEY_ADMIN, false)
    }
}
