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
        setupAdminDrawer()
        observeViewModel()

        // Check saved mode and apply it on fresh start
        if (savedInstanceState == null && viewModel.shouldStartInAdminMode()) {
            switchToAdminMode()
        }
    }

    private fun observeViewModel() {
        // Observe admin header data changes
        viewModel.adminHeaderData.observe(this) { headerData ->
            updateAdminHeader(headerData)
        }

        // Observe switch mode events
        viewModel.switchModeEvent.observe(this) { event ->
            event?.let {
                handleSwitchModeEvent(it)
                viewModel.onSwitchModeEventHandled()
            }
        }
    }

    private fun setupNavigation() {
        binding.bottomNavigation.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val uiState = viewModel.onDestinationChanged(destination.id)
            applyNavigationUIState(uiState, destination.label?.toString())
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

    private fun setupAdminDrawer() {
        with(binding) {
            // Menu button opens drawer
            ivMenuDrawer.setOnClickListener {
                drawerLayout.openDrawer(adminNavView)
            }

            // Switch to user button
            ivSwitchToUser.setOnClickListener {
                showSwitchToUserConfirmation()
            }

            // Navigation drawer item clicks
            adminNavView.setNavigationItemSelectedListener { menuItem ->
                handleAdminDrawerNavigation(menuItem.itemId)
            }
        }
    }

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

    private fun switchToAdminMode() {
        with(binding) {
            // Hide client navigation
            bottomNavigation.visibility = View.GONE

            // Show admin navigation
            adminTopBar.visibility = View.VISIBLE
            adminNavView.visibility = View.VISIBLE
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        }

        // Switch to admin navigation graph
        navController.setGraph(R.navigation.admin_nav_graph)

        // Navigate to admin dashboard
        navController.navigate(
            R.id.adminDashboardFragment,
            null,
            androidx.navigation.NavOptions.Builder()
                .setPopUpTo(navController.graph.startDestinationId, true)
                .build()
        )
    }

    private fun switchToClientMode() {
        with(binding) {
            // Hide admin navigation
            adminTopBar.visibility = View.GONE
            adminNavView.visibility = View.GONE
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

            // Show client navigation
            bottomNavigation.visibility = View.VISIBLE
            bottomNavigation.inflateMenu(R.menu.bottom_nav_menu)
        }

        // Switch to client navigation graph
        navController.setGraph(R.navigation.nav_graph)

        // Navigate to home
        navController.navigate(
            R.id.homeFragment,
            null,
            androidx.navigation.NavOptions.Builder()
                .setPopUpTo(navController.graph.startDestinationId, true)
                .build()
        )

        // Setup bottom navigation
        binding.bottomNavigation.setupWithNavController(navController)
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