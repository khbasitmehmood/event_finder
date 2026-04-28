package com.eventfinder.app

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.eventfinder.app.domain.model.UserType
import com.eventfinder.app.utils.ModeManager
import com.eventfinder.app.utils.UserPreferences
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val application: Application,
    private val firestore: FirebaseFirestore,
    private val userPreferences: UserPreferences
) : ViewModel() {

    // UI State for navigation mode
    private val _isAdminMode = MutableLiveData<Boolean>()
    val isAdminMode: LiveData<Boolean> = _isAdminMode

    // Event to trigger mode switch
    private val _switchModeEvent = MutableLiveData<SwitchModeEvent?>()
    val switchModeEvent: LiveData<SwitchModeEvent?> = _switchModeEvent

    // Admin header data
    private val _adminHeaderData = MutableLiveData<AdminHeaderData>()
    val adminHeaderData: LiveData<AdminHeaderData> = _adminHeaderData

    // Client main screen IDs for bottom nav visibility
    val clientMainScreenIds = setOf(
        R.id.homeFragment,
        R.id.organizerDashboardFragment,
        R.id.organizerEventsFragment,
        R.id.organizerBookingsFragment,
        R.id.exploreFragment,
        R.id.ticketsFragment,
        R.id.favouritesFragment,
        R.id.profileFragment
    )

    init {
        // Load saved mode preference
        _isAdminMode.value = ModeManager.isAdminMode(application)
        loadAdminHeaderData()
    }

    /**
     * Get the current user type from preferences
     */
    fun getUserType(): UserType {
        val userTypeString = userPreferences.getUserType()
        return try {
            UserType.valueOf(userTypeString)
        } catch (e: Exception) {
            UserType.USER // Default to USER if invalid
        }
    }

    /**
     * Check if app should start in admin mode (on fresh start)
     * @deprecated Admin mode system preserved for future actual admin features
     */
    @Deprecated("Admin mode system preserved for future actual admin features")
    fun shouldStartInAdminMode(): Boolean {
        return ModeManager.isAdminMode(application)
    }

    /**
     * Handle destination change and determine UI visibility
     */
    fun onDestinationChanged(destinationId: Int): NavigationUIState {
        val isAdmin = _isAdminMode.value ?: false

        return if (isAdmin) {
            NavigationUIState(
                showBottomNav = false,
                showAdminTopBar = true,
                showAdminDrawer = true,
                drawerLocked = false
            )
        } else {
            NavigationUIState(
                showBottomNav = clientMainScreenIds.contains(destinationId),
                showAdminTopBar = false,
                showAdminDrawer = false,
                drawerLocked = true
            )
        }
    }

    /**
     * Switch between admin and client mode
     * @deprecated Admin mode system preserved for future actual admin features
     */
    @Deprecated("Admin mode system preserved for future actual admin features")
    fun switchDashboard(toAdmin: Boolean) {
        // Save mode preference
        ModeManager.setAdminMode(application, toAdmin)
        _isAdminMode.value = toAdmin

        // Trigger switch event
        _switchModeEvent.value = if (toAdmin) {
            SwitchModeEvent.ToAdmin
        } else {
            SwitchModeEvent.ToClient
        }
    }

    /**
     * Clear switch mode event after handling
     */
    fun onSwitchModeEventHandled() {
        _switchModeEvent.value = null
    }

    /**
     * Load admin header data from preferences
     */
    private fun loadAdminHeaderData() {
        val pref = application.getSharedPreferences("admin_profile", 0)
        _adminHeaderData.value = AdminHeaderData(
            name = pref.getString("name", "Admin User") ?: "Admin User",
            email = pref.getString("email", "admin@epicevents.com") ?: "admin@epicevents.com"
        )
    }

    /**
     * Refresh admin header data (call after profile update)
     */
    fun refreshAdminHeaderData() {
        loadAdminHeaderData()
    }

    // Data classes for UI state
    data class NavigationUIState(
        val showBottomNav: Boolean,
        val showAdminTopBar: Boolean,
        val showAdminDrawer: Boolean,
        val drawerLocked: Boolean
    )

    data class AdminHeaderData(
        val name: String,
        val email: String
    )

    sealed class SwitchModeEvent {
        object ToAdmin : SwitchModeEvent()
        object ToClient : SwitchModeEvent()
    }
}
