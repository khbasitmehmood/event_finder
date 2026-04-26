package com.eventfinder.app.client.profile

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.eventfinder.app.MainActivity
import com.eventfinder.app.R
import com.eventfinder.app.databinding.FragmentProfileBinding
import com.eventfinder.app.domain.model.UserType
import com.eventfinder.app.utils.ModeManager
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        // Edit Profile
        binding.btnEditProfile.setOnClickListener {
            val bundle = Bundle().apply {
                putBoolean("IS_EDIT_MODE", true)
            }
            findNavController().navigate(R.id.fillProfileFragment, bundle)
        }

        // Watchlist
        binding.btnWatchlist.setOnClickListener {
            try {
                findNavController().navigate(R.id.watchlistFragment)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Navigation failed", Toast.LENGTH_SHORT).show()
            }
        }

        // Logout
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        // Switch to Admin
        binding.btnSwitchAdmin.setOnClickListener {
            showSwitchToAdminConfirmation()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.logoutState.collect { state ->
                        handleLogoutState(state)
                    }
                }
                
                launch {
                    viewModel.currentUser.collect { user ->
                        user?.let {
                            val displayName = it.profile?.fullName 
                                ?: it.organizerProfile?.organizationName 
                                ?: "Guest User"
                            
                            binding.profileName.text = displayName
                            binding.profileEmail.text = it.email

                            val photoUrl = if (it.userType == UserType.ORGANIZER) {
                                binding.tvInterestsTitle.text = "Events We Offer"
                                val offered = it.organizerProfile?.offeredEvents ?: emptyList()
                                setupChips(offered)
                                
                                it.organizerProfile?.logoUrl
                            } else {
                                binding.tvInterestsTitle.text = "Add Interests"
                                val interests = it.profile?.interests ?: emptyList()
                                setupChips(interests)
                                
                                it.profile?.photoUrl
                            }

                            if (!photoUrl.isNullOrBlank()) {
                                binding.profileImage.load(photoUrl) {
                                    crossfade(true)
                                    transformations(CircleCropTransformation())
                                    placeholder(R.drawable.ic_profile)
                                    error(R.drawable.ic_profile)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupChips(items: List<String>) {
        binding.chipGroupInterests.removeAllViews()
        for (item in items) {
            val chip = layoutInflater.inflate(R.layout.item_chip_choice, binding.chipGroupInterests, false) as Chip
            chip.text = item
            chip.isChecked = true
            chip.isClickable = false
            chip.isCheckable = false
            binding.chipGroupInterests.addView(chip)
        }
    }

    private fun handleLogoutState(state: LogoutState) {
        when (state) {
            is LogoutState.Idle -> {
                // Do nothing
            }
            is LogoutState.Loading -> {
                // Show loading (optional - logout is usually fast)
                binding.btnLogout.isEnabled = false
            }
            is LogoutState.Success -> {
                binding.btnLogout.isEnabled = true
                Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
                navigateToLogin()
            }
            is LogoutState.Error -> {
                binding.btnLogout.isEnabled = true
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                viewModel.resetLogoutState()
            }
        }
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setCancelable(true)
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Logout") { _, _ ->
                viewModel.logout()
            }
            .show()
    }

    private fun navigateToLogin() {
        // Clear back stack and navigate to login
        findNavController().navigate(R.id.action_profile_to_login)
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
        ModeManager.setAdminMode(requireContext(), true)
        Toast.makeText(requireContext(), "Switching to Admin Mode...", Toast.LENGTH_SHORT).show()
        (requireActivity() as? MainActivity)?.switchDashboard(toAdmin = true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}