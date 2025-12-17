package com.example.myapplication

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.myapplication.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Use ViewBinding for cleaner code
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Properly find the NavHostFragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
            ?: return // Safety exit if navigation host is missing

        val navController = navHostFragment.navController

        // 2. Link BottomNav using binding
        binding.bottomNavigation.setupWithNavController(navController)

        // 3. Precise Visibility Logic for your sequence
        // Inside MainActivity.kt onCreate
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                // 🔹 ADD ALL 4 DASHBOARD IDs HERE
                R.id.homeFragment,
                R.id.exploreFragment,
                R.id.favouritesFragment,
                R.id.profileFragment -> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                }

                // Hide for Auth, Splash, and Admin screens
                else -> {
                    binding.bottomNavigation.visibility = View.GONE
                }
            }
        }
    }
}