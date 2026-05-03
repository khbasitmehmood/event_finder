package com.eventfinder.app.utils

import android.view.MenuItem
import androidx.core.view.isVisible
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Utility functions for managing notification badges
 */
object BadgeUtils {

    /**
     * Show or update badge on a toolbar menu item
     * Note: Toolbar badges require a custom approach
     * This is a placeholder for future implementation
     */
    fun setMenuItemBadge(menuItem: MenuItem?, count: Int) {
        // Material3 toolbar doesn't support badges directly
        // This would require a custom BadgeDrawable implementation
        // For now, badges are shown on bottom navigation only
    }

    /**
     * Show or update badge on bottom navigation item
     */
    fun setBottomNavBadge(
        bottomNav: BottomNavigationView,
        itemId: Int,
        count: Int
    ) {
        val badge = bottomNav.getOrCreateBadge(itemId)

        if (count > 0) {
            badge.isVisible = true
            badge.number = count
            badge.maxCharacterCount = 3 // Show "99+" for large numbers
        } else {
            badge.isVisible = false
        }
    }

    /**
     * Clear badge from bottom navigation item
     */
    fun clearBottomNavBadge(
        bottomNav: BottomNavigationView,
        itemId: Int
    ) {
        bottomNav.removeBadge(itemId)
    }
}
