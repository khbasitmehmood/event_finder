package com.eventfinder.app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.eventfinder.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        setupNavigation()
    }

    private fun setupNavigation() {
        binding.bottomNavigation.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val dashboardIds = setOf(
                R.id.homeFragment,
                R.id.exploreFragment,
                R.id.favouritesFragment,
                R.id.profileFragment,
                R.id.adminDashboardFragment,
                R.id.createEventFragment // Add your admin home ID
            )

            if (dashboardIds.contains(destination.id)) {
                binding.bottomNavigation.visibility = View.VISIBLE
            } else {
                binding.bottomNavigation.visibility = View.GONE
            }
        }
    }

    fun switchDashboard(toAdmin: Boolean) {
        try {
            binding.bottomNavigation.setOnItemSelectedListener(null)
            binding.bottomNavigation.menu.clear()


            if (toAdmin.not()){
                binding.bottomNavigation.inflateMenu(R.menu.bottom_nav_menu)
                navController.setGraph(R.navigation.nav_graph)
                navController.navigate(R.id.homeFragment)
                binding.bottomNavigation.setupWithNavController(navController)
                binding.bottomNavigation.visibility = View.VISIBLE
            }else{
                navController.setGraph(R.navigation.admin_nav_graph)
                navController.navigate(R.id.adminDashboardFragment)
                binding.bottomNavigation.setupWithNavController(navController)
                binding.bottomNavigation.visibility = View.GONE
            }


        } catch (e: Exception) {
            android.util.Log.e("NAV_ERROR", "Switch failed: ${e.message}")
            recreate()
        }
    }
}