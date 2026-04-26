package com.eventfinder.app.client.profile

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.eventfinder.app.MainActivity
import com.eventfinder.app.R
import com.eventfinder.app.databinding.FragmentProfileBinding
import com.eventfinder.app.utils.ModeManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Link the binding to the view
        _binding = FragmentProfileBinding.bind(view)

        // 1. WATCHLIST CLICK
        binding.btnWatchlist.setOnClickListener {
            try {
                findNavController().navigate(R.id.watchlistFragment)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Navigation failed", Toast.LENGTH_SHORT).show()
            }
        }

        // 2. SWITCH TO ADMIN CLICK
        binding.btnSwitchAdmin.setOnClickListener {
            showSwitchToAdminConfirmation()
        }
    }

    private fun showSwitchToAdminConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Switch to Admin Mode?")
            .setMessage("Are you sure you want to switch to the Admin Dashboard?")
            .setCancelable(true)
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Switch Now") { _, _ ->
                performAdminSwitch()
            }
            .show()
    }

    private fun performAdminSwitch() {
        // Save admin mode preference
        ModeManager.setAdminMode(requireContext(), true)

        // Show a toast to verify the click is actually happening
        Toast.makeText(requireContext(), "Switching to Admin Mode...", Toast.LENGTH_SHORT).show()

        (requireActivity() as? MainActivity)?.switchDashboard(toAdmin = true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clean up binding to avoid memory leaks
    }
}