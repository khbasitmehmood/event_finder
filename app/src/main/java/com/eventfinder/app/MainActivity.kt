package com.eventfinder.app

import android.os.Bundle
import android.view.View
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

    private var currentUserType: UserType? = null

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

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(binding.adminNavView)) {
            binding.drawerLayout.closeDrawer(binding.adminNavView)
        } else {
            super.onBackPressed()
        }
    }
}