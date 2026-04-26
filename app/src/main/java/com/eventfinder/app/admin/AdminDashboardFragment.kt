package com.eventfinder.app.admin

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.eventfinder.app.R

/**
 * @deprecated This AdminDashboardFragment is obsolete and preserved for future actual admin features.
 * Organizers now use OrganizerDashboardFragment instead.
 * TODO: Admin mode system preserved for future actual admin panel features
 */
@Deprecated("Obsolete - Use OrganizerDashboardFragment for organizers. Preserved for future admin features.")
class AdminDashboardFragment : Fragment(R.layout.fragment_admin_dashboard) {

    private lateinit var btnCreateEvent: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        btnCreateEvent = view.findViewById(R.id.btnCreateEvent)

        // Create Event button
        btnCreateEvent.setOnClickListener {
            findNavController().navigate(R.id.action_adminDashboardFragment_to_createEventFragment)
        }

        // Note: The drawer and top bar are now handled by MainActivity
        // Navigation is centralized in MainActivity for admin mode
    }
}