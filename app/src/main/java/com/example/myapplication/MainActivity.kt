package com.example.myapplication

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.myapplication.databinding.ActivityMainBinding
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    // Track current mode to prevent accidental resets
    private var isAdminMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Initialize visibility and setup
        setupNavigation()
    }

    private fun setupNavigation() {
        binding.bottomNavigation.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Use IDs from BOTH graphs here
            val dashboardIds = setOf(
                R.id.homeFragment, R.id.exploreFragment, R.id.favouritesFragment, R.id.profileFragment,
                R.id.adminDashboardFragment, R.id.createEventFragment // Add your admin home ID
            )

            if (dashboardIds.contains(destination.id)) {
                binding.bottomNavigation.visibility = View.VISIBLE
            } else {
                binding.bottomNavigation.visibility = View.GONE
            }
        }
    }

    // Inside MainActivity.kt
    fun switchDashboard(toAdmin: Boolean) {
        try {
            binding.bottomNavigation.setOnItemSelectedListener(null)
            binding.bottomNavigation.menu.clear()

            if (toAdmin) {
                binding.bottomNavigation.inflateMenu(R.menu.admin_sidebar_menu)
            } else {
                binding.bottomNavigation.inflateMenu(R.menu.bottom_nav_menu)
            }

            val graphResId = if (toAdmin) R.navigation.admin_nav_graph else R.navigation.nav_graph
            navController.setGraph(graphResId)

            // 🔹 FORCE THE UI TO SWITCH IMMEDIATELY
            val startId = if (toAdmin) R.id.adminDashboardFragment else R.id.homeFragment
            navController.navigate(startId)

            binding.bottomNavigation.setupWithNavController(navController)
            binding.bottomNavigation.visibility = View.VISIBLE

        } catch (e: Exception) {
            android.util.Log.e("NAV_ERROR", "Switch failed: ${e.message}")
            recreate()
        }
    }
}