package com.eventfinder.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.eventfinder.app.databinding.ActivityMainBinding
import com.eventfinder.app.domain.model.UserType
import com.eventfinder.app.domain.usecase.SeedCategoriesUseCase
import com.eventfinder.app.fcm.FcmTokenManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var seedCategoriesUseCase: SeedCategoriesUseCase

    @Inject
    lateinit var fcmTokenManager: FcmTokenManager

    private var currentUserType: UserType? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.util.Log.d("MainActivity", "Notification permission granted")
            // Get and log FCM token
            lifecycleScope.launch {
                fcmTokenManager.getToken().onSuccess { token ->
                    android.util.Log.d("FCM_TOKEN", "========================================")
                    android.util.Log.d("FCM_TOKEN", "YOUR FCM TOKEN:")
                    android.util.Log.d("FCM_TOKEN", token)
                    android.util.Log.d("FCM_TOKEN", "========================================")
                }
            }
        } else {
            android.util.Log.d("MainActivity", "Notification permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Run the seeding logic once in the background
        lifecycleScope.launch {
            seedCategoriesUseCase()
        }

        navController = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController

        updateBottomNavForUserType() // Setup dynamic bottom nav based on user type
        setupNavigation()
        observeViewModel()

        // Request notification permission
        requestNotificationPermission()
    }

    private fun observeViewModel() {
        // Admin mode observation - COMMENTED OUT (preserved for future admin features)
        /*
        viewModel.adminHeaderData.observe(this) { headerData ->
            updateAdminHeader(headerData)
        }

        viewModel.switchModeEvent.observe(this) { event ->
            event?.let {
                handleSwitchModeEvent(it)
                viewModel.onSwitchModeEventHandled()
            }
        }
        */
    }

    private fun setupNavigation() {
        // Setup role-based bottom navigation handling
        var isUpdatingSelection = false

        binding.bottomNavigation.setOnItemReselectedListener {
            // Keep current tab state; do not push duplicate destinations.
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (isUpdatingSelection) return@setOnItemSelectedListener true

            navigateToTopLevelDestination(item.itemId)
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Ensure the correct menu is loaded for the current user type (e.g. after login)
            updateBottomNavForUserType()

            val uiState = viewModel.onDestinationChanged(destination.id)
            applyNavigationUIState(uiState, destination.label?.toString())

            // Update selected item based on current destination (prevent loops)
            isUpdatingSelection = true
            if (binding.bottomNavigation.menu.findItem(destination.id) != null) {
                binding.bottomNavigation.selectedItemId = destination.id
            }
            isUpdatingSelection = false
        }
    }

    private fun navigateToTopLevelDestination(destinationId: Int): Boolean {
        val currentDestinationId = navController.currentDestination?.id
        if (currentDestinationId == destinationId) return true

        return try {
            val navOptions = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(navController.graph.findStartDestination().id, false, true)
                .build()

            navController.navigate(destinationId, null, navOptions)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Loads role-specific bottom navigation tabs.
     */
    fun updateBottomNavForUserType() {
        val userType = viewModel.getUserType()

        if (currentUserType == userType) return
        currentUserType = userType

        // Clear any old admin mode preference to prevent confusion
        @Suppress("DEPRECATION")
        com.eventfinder.app.utils.ModeManager.setAdminMode(this, false)

        binding.bottomNavigation.menu.clear()
        binding.bottomNavigation.inflateMenu(
            if (userType == UserType.ORGANIZER) R.menu.bottom_nav_organizer else R.menu.bottom_nav_user
        )
    }

    private fun applyNavigationUIState(uiState: MainViewModel.NavigationUIState, destinationLabel: String?) {
        with(binding) {
            bottomNavigation.visibility = if (uiState.showBottomNav) View.VISIBLE else View.GONE
            adminTopBar.visibility = if (uiState.showAdminTopBar) View.VISIBLE else View.GONE
            adminNavView.visibility = if (uiState.showAdminDrawer) View.VISIBLE else View.GONE

            drawerLayout.setDrawerLockMode(
                if (uiState.drawerLocked) DrawerLayout.LOCK_MODE_LOCKED_CLOSED
                else DrawerLayout.LOCK_MODE_UNLOCKED
            )

            // Update admin title if in admin mode
            if (uiState.showAdminTopBar) {
                tvAdminTitle.text = destinationLabel ?: getString(R.string.admin_dashboard)
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // For older Android versions, get FCM token directly
            lifecycleScope.launch {
                fcmTokenManager.getToken().onSuccess { token ->
                    android.util.Log.d("FCM_TOKEN", "========================================")
                    android.util.Log.d("FCM_TOKEN", "YOUR FCM TOKEN:")
                    android.util.Log.d("FCM_TOKEN", token)
                    android.util.Log.d("FCM_TOKEN", "========================================")
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(binding.adminNavView)) {
            binding.drawerLayout.closeDrawer(binding.adminNavView)
        } else {
            super.onBackPressed()
        }
    }
}