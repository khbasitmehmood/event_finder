package com.eventfinder.app

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.eventfinder.app.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navController = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController

        setupNavigation()
        observeViewModel()
        updateBottomNavForUserType() // Setup dynamic bottom nav based on user type

        // Note: Admin mode switching removed - organizers now use user type-based routing
        // Old admin system is preserved in commented code for future actual admin features
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
        val userType = viewModel.getUserType()
        var isUpdatingSelection = false

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (isUpdatingSelection) return@setOnItemSelectedListener true

            when (item.itemId) {
                R.id.homeFragment -> {
                    // Navigate to appropriate home based on user type
                    val destinationId = if (viewModel.getUserType() == com.eventfinder.app.domain.model.UserType.ORGANIZER) {
                        R.id.organizerDashboardFragment
                    } else {
                        R.id.homeFragment
                    }
                    try {
                        navController.navigate(destinationId)
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
                else -> {
                    // Let NavController handle other items normally
                    try {
                        navController.navigate(item.itemId)
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
            }
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

    /*
     * Admin drawer setup - COMMENTED OUT
     * Preserved for future actual admin features. Not used by organizers.
     *
    @Deprecated("Preserved for future admin features. Organizers use bottom navigation.")
    private fun setupAdminDrawer() {
        with(binding) {
            ivMenuDrawer.setOnClickListener {
                drawerLayout.openDrawer(adminNavView)
            }

            ivSwitchToUser.setOnClickListener {
                showSwitchToUserConfirmation()
            }

            adminNavView.setNavigationItemSelectedListener { menuItem ->
                handleAdminDrawerNavigation(menuItem.itemId)
            }
        }
    }
    */

    /*
     * Admin navigation methods - COMMENTED OUT
     * Preserved for future actual admin features.
     *
    private fun handleAdminDrawerNavigation(menuItemId: Int): Boolean {
        val destinationId = when (menuItemId) {
            R.id.nav_admin_dashboard -> R.id.adminDashboardFragment
            R.id.nav_admin_profile -> R.id.adminProfileFragment
            R.id.nav_admin_settings -> R.id.adminSettingsFragment
            else -> return false
        }
        navController.navigate(destinationId)
        binding.drawerLayout.closeDrawer(binding.adminNavView)
        return true
    }

    private fun updateAdminHeader(headerData: MainViewModel.AdminHeaderData) {
        binding.adminNavView.getHeaderView(0)?.let { headerView ->
            headerView.findViewById<TextView>(R.id.tvAdminName)?.text = headerData.name
            headerView.findViewById<TextView>(R.id.tvAdminEmail)?.text = headerData.email
        }
    }

    private fun showSwitchToUserConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.switch_to_user_title)
            .setMessage(R.string.switch_to_user_message)
            .setCancelable(true)
            .setNegativeButton(R.string.stay_here) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(R.string.switch_now) { _, _ -> viewModel.switchDashboard(toAdmin = false) }
            .show()
    }

    fun switchDashboard(toAdmin: Boolean) = viewModel.switchDashboard(toAdmin)
    */

    /*
     * Admin mode switching - COMMENTED OUT
     * Preserved for future actual admin features.
     *
    private fun handleSwitchModeEvent(event: MainViewModel.SwitchModeEvent) {
        try {
            binding.bottomNavigation.apply {
                setOnItemSelectedListener(null)
                menu.clear()
            }

            when (event) {
                is MainViewModel.SwitchModeEvent.ToAdmin -> switchToAdminMode()
                is MainViewModel.SwitchModeEvent.ToClient -> switchToClientMode()
            }
        } catch (e: Exception) {
            android.util.Log.e("NAV_ERROR", "Switch failed: ${e.message}")
            recreate()
        }
    }

    @Deprecated("Preserved for future admin features")
    private fun switchToAdminMode() {
        with(binding) {
            bottomNavigation.visibility = View.GONE
            adminTopBar.visibility = View.VISIBLE
            adminNavView.visibility = View.VISIBLE
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        }
        navController.setGraph(R.navigation.admin_nav_graph)
        navController.navigate(
            R.id.adminDashboardFragment,
            null,
            androidx.navigation.NavOptions.Builder()
                .setPopUpTo(navController.graph.startDestinationId, true)
                .build()
        )
    }

    @Deprecated("Preserved for future admin features")
    private fun switchToClientMode() {
        with(binding) {
            adminTopBar.visibility = View.GONE
            adminNavView.visibility = View.GONE
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            bottomNavigation.visibility = View.VISIBLE
            bottomNavigation.inflateMenu(R.menu.bottom_nav_menu)
        }
        navController.setGraph(R.navigation.nav_graph)
        navController.navigate(
            R.id.homeFragment,
            null,
            androidx.navigation.NavOptions.Builder()
                .setPopUpTo(navController.graph.startDestinationId, true)
                .build()
        )
        binding.bottomNavigation.setupWithNavController(navController)
    }
    */

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(binding.adminNavView)) {
            binding.drawerLayout.closeDrawer(binding.adminNavView)
        } else {
            super.onBackPressed()
        }
    }
}