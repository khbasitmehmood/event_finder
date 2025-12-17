package com.example.myapplication.utils

import android.content.Context

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
