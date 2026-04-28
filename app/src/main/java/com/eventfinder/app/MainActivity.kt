package com.eventfinder.app

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.eventfinder.app.databinding.ActivityMainBinding
import com.eventfinder.app.domain.usecase.SeedCategoriesUseCase
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Run the seeding logic once in the background
        lifecycleScope.launch {
            seedCategoriesUseCase()
        }

        navController = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController

        setupNavigation()
        observeViewModel()
        updateBottomNavForUserType() // Setup dynamic bottom nav based on user type
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
        // Setup navigation with custom handling for organizers
        var isUpdatingSelection = false

        binding.bottomNavigation.setOnItemReselectedListener {
            // Keep current tab state; do not push duplicate destinations.
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (isUpdatingSelection) return@setOnItemSelectedListener true

            val destinationId = when (item.itemId) {
                R.id.homeFragment -> {
                    if (viewModel.getUserType() == com.eventfinder.app.domain.model.UserType.ORGANIZER) {
                        R.id.organizerDashboardFragment
                    } else {
                        R.id.homeFragment
                    }
                }
                else -> item.itemId
            }

            navigateToTopLevelDestination(destinationId)
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val uiState = viewModel.onDestinationChanged(destination.id)
            applyNavigationUIState(uiState, destination.label?.toString())

            // Update bottom nav labels dynamically based on current destination
            val homeItem = binding.bottomNavigation.menu.findItem(R.id.homeFragment)
            if (destination.id == R.id.organizerDashboardFragment) {
                homeItem?.title = getString(R.string.dashboard)
                homeItem?.setIcon(R.drawable.ic_dashboard)
            } else if (destination.id == R.id.homeFragment) {
                homeItem?.title = getString(R.string.home)
                homeItem?.setIcon(R.drawable.ic_home)
            }

            // Update selected item based on current destination (prevent loops)
            isUpdatingSelection = true
            when (destination.id) {
                R.id.homeFragment, R.id.organizerDashboardFragment -> {
                    binding.bottomNavigation.selectedItemId = R.id.homeFragment
                }
                R.id.exploreFragment -> {
                    binding.bottomNavigation.selectedItemId = R.id.exploreFragment
                }
                R.id.favouritesFragment -> {
                    binding.bottomNavigation.selectedItemId = R.id.favouritesFragment
                }
                R.id.profileFragment -> {
                    binding.bottomNavigation.selectedItemId = R.id.profileFragment
                }
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
     * Updates the first item in bottom navigation based on user type.
     * If ORGANIZER: shows "Dashboard" with dashboard icon, points to organizerDashboardFragment
     * If USER: shows "Home" with home icon, points to homeFragment
     */
    private fun updateBottomNavForUserType() {
        val userType = viewModel.getUserType()
        val menu = binding.bottomNavigation.menu
        val firstItem = menu.findItem(R.id.homeFragment)

        // Clear any old admin mode preference to prevent confusion
        @Suppress("DEPRECATION")
        com.eventfinder.app.utils.ModeManager.setAdminMode(this, false)

        if (userType == com.eventfinder.app.domain.model.UserType.ORGANIZER) {
            // Change to Dashboard for organizers
            firstItem?.let {
                it.title = getString(R.string.dashboard)
                it.setIcon(R.drawable.ic_dashboard)
            }
        } else {
            // Keep as Home for regular users
            firstItem?.let {
                it.title = getString(R.string.home)
                it.setIcon(R.drawable.ic_home)
            }
        }
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