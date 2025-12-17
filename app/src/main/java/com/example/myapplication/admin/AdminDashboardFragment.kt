package com.example.myapplication.admin

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.client.auth.TransitionFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView

class AdminDashboardFragment : Fragment(R.layout.fragment_admin_dashboard) {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var btnCreateEvent: Button
    private lateinit var ivMenu: ImageView
    private lateinit var btnAdminProfile: ImageView

    // 🔹 Header Views
    private lateinit var tvAdminName: TextView
    private lateinit var tvAdminEmail: TextView
    private lateinit var ivAdminPhoto: ImageView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🔹 INITIALIZE VIEWS
        drawerLayout = view.findViewById(R.id.drawerLayout)
        navView = view.findViewById(R.id.navView)
        btnCreateEvent = view.findViewById(R.id.btnCreateEvent)
        ivMenu = view.findViewById(R.id.ivMenu)
        btnAdminProfile = view.findViewById(R.id.btnAdminProfile)

        // 🔹 CONNECT NAV HEADER MAIN
        // Navigation headers are not part of the fragment's direct view hierarchy,
        // so we access them through the NavigationView.
        val headerView = navView.getHeaderView(0)
        tvAdminName = headerView.findViewById(R.id.tvAdminName)
        tvAdminEmail = headerView.findViewById(R.id.tvAdminEmail)
        ivAdminPhoto = headerView.findViewById(R.id.ivAdminPhoto)

        // 🔹 SET HEADER DATA (Example)
        tvAdminName.text = "Epic Admin"
        tvAdminEmail.text = "admin@epicevents.com"

        // 🔹 TRIGGER CONFIRMATION DIALOG
        btnAdminProfile.setOnClickListener {
            showSwitchConfirmation()
        }

        // 🔹 CREATE EVENT BUTTON
        btnCreateEvent.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboardFragment_to_createEventFragment)
        }

        // 🔹 MENU CLICK → OPEN DRAWER
        ivMenu.setOnClickListener {
            drawerLayout.openDrawer(navView)
        }

        // 🔹 SIDEBAR NAVIGATION ITEM CLICK
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_admin_profile -> {
                    findNavController().navigate(R.id.adminProfileFragment)
                    drawerLayout.closeDrawer(navView)
                    true
                }
                R.id.nav_admin_settings -> {
                    findNavController().navigate(R.id.adminSettingsFragment)
                    drawerLayout.closeDrawer(navView)
                    true
                }
                else -> false
            }
        }
    }

    private fun showSwitchConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Switch to User View?")
            .setMessage("Are you sure you want to exit the Admin Dashboard and return to the Event Finder?")
            .setCancelable(true)
            .setNegativeButton("Stay here") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Switch Now") { _, _ ->
                performUserSwitch()
            }
            .show()
    }

    private fun performUserSwitch() {
        val bundle = Bundle().apply {
            putString("TARGET", "USER")
        }

        // Clear the backstack so the Admin screen is forgotten
        val navOptions = androidx.navigation.NavOptions.Builder()
            .setPopUpTo(R.id.main_nav_graph, true)
            .build()

        findNavController().navigate(R.id.transitionFragment, bundle, navOptions)
    }
}