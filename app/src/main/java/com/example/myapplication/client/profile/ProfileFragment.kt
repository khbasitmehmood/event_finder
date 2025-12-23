package com.example.myapplication.client.profile

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentProfileBinding

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
        // This MUST match the ID in your XML exactly
        binding.btnSwitchAdmin.setOnClickListener {
            // Show a toast to verify the click is actually happening
            Toast.makeText(requireContext(), "Switching...", Toast.LENGTH_SHORT).show()

            (requireActivity() as? MainActivity)?.switchDashboard(toAdmin = true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clean up binding to avoid memory leaks
    }
}