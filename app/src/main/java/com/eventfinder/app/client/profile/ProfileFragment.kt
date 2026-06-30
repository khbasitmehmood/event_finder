package com.eventfinder.app.client.profile

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.eventfinder.app.R
import com.eventfinder.app.databinding.FragmentProfileBinding
import com.eventfinder.app.domain.model.UserType
import com.eventfinder.app.fcm.FcmTokenManager
import com.eventfinder.app.utils.AuthNavArgs
import com.eventfinder.app.utils.UserPreferences
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var fcmTokenManager: FcmTokenManager

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        userPreferences.setNotificationsEnabled(isGranted)
        saveNotificationPreferenceToToken(isGranted)
        binding.switchNotifications.isChecked = isGranted
        Toast.makeText(
            requireContext(),
            if (isGranted) "Notifications enabled" else "Notifications disabled",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        setupSettingsState()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        // Edit Profile
        binding.btnEditProfile.setOnClickListener {
            val currentType = viewModel.currentUser.value?.userType ?: UserType.USER
            val bundle = Bundle().apply {
                putBoolean(AuthNavArgs.IS_EDIT_MODE, true)
                putString(AuthNavArgs.USER_TYPE, currentType.name)
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

        binding.btnNotifications.setOnClickListener {
            binding.switchNotifications.isChecked = !binding.switchNotifications.isChecked
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                userPreferences.setNotificationsEnabled(isChecked)
                saveNotificationPreferenceToToken(isChecked)
                Toast.makeText(
                    requireContext(),
                    if (isChecked) "Notifications enabled" else "Notifications disabled",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.btnTheme.setOnClickListener {
            showThemeDialog()
        }

        // Logout
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
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
                    // Combine flow so we setup chips only when both user and categories are ready
                    combine(
                        viewModel.currentUser,
                        viewModel.categories
                    ) { user, categories ->
                        Pair(user, categories)
                    }.collect { (user, categories) ->
                        if (user != null) {
                            val displayName = user.profile?.fullName 
                                ?: user.organizerProfile?.organizationName 
                                ?: "Guest User"
                            
                            binding.profileName.text = displayName
                            binding.profileEmail.text = user.email

                            val photoUrl = if (user.userType == UserType.ORGANIZER) {
                                binding.tvRoleBadge.text = "Organizer"
                                binding.tvInterestsTitle.text = "Events We Offer"
                                val offeredIds = user.organizerProfile?.offeredEvents ?: emptyList()
                                // Map IDs back to names
                                val names = offeredIds.map { id ->
                                    categories.find { it.id == id }?.name ?: id
                                }
                                setupChips(names)
                                
                                user.organizerProfile?.logoUrl
                            } else {
                                binding.tvRoleBadge.text = "Standard Member"
                                binding.tvInterestsTitle.text = "Add Interests"
                                val interestIds = user.profile?.interests ?: emptyList()
                                // Map IDs back to names
                                val names = interestIds.map { id ->
                                    categories.find { it.id == id }?.name ?: id
                                }
                                setupChips(names)
                                
                                user.profile?.photoUrl
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

    private fun setupSettingsState() {
        binding.switchNotifications.setOnCheckedChangeListener(null)
        val notificationsAllowedBySystem = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val notificationsEnabled = userPreferences.areNotificationsEnabled() && notificationsAllowedBySystem
        if (!notificationsEnabled) {
            userPreferences.setNotificationsEnabled(false)
        }
        binding.switchNotifications.isChecked = notificationsEnabled
        updateThemeLabel(userPreferences.getThemeMode())
    }

    private fun saveNotificationPreferenceToToken(enabled: Boolean) {
        val userId = userPreferences.getStoredUserId() ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            fcmTokenManager.saveCurrentTokenForUser(
                userId = userId,
                notificationsEnabled = enabled
            )
        }
    }

    private fun showThemeDialog() {
        val options = arrayOf("Light", "Dark", "System default")
        val currentIndex = when (userPreferences.getThemeMode()) {
            UserPreferences.THEME_LIGHT -> 0
            UserPreferences.THEME_DARK -> 1
            else -> 2
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Theme")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                val mode = when (which) {
                    0 -> UserPreferences.THEME_LIGHT
                    1 -> UserPreferences.THEME_DARK
                    else -> UserPreferences.THEME_SYSTEM
                }
                userPreferences.setThemeMode(mode)
                applyTheme(mode)
                updateThemeLabel(mode)
                dialog.dismiss()
            }
            .show()
    }

    private fun applyTheme(mode: String) {
        val nightMode = when (mode) {
            UserPreferences.THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            UserPreferences.THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    private fun updateThemeLabel(mode: String) {
        binding.tvThemeValue.text = when (mode) {
            UserPreferences.THEME_LIGHT -> "Light"
            UserPreferences.THEME_DARK -> "Dark"
            else -> "System"
        }
    }

    private fun setupChips(items: List<String>) {
        binding.chipGroupInterests.removeAllViews()
        for (item in items) {
            val chip = Chip(requireContext())
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
                binding.btnLogout.isEnabled = false
            }
            is LogoutState.Success -> {
                binding.btnLogout.isEnabled = true
                Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
                viewModel.resetLogoutState()
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
        val navController = findNavController()
        val navOptions = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setPopUpTo(R.id.main_nav_graph, true)
            .build()

        try {
            // Route to auth root graph to avoid cross-graph destination issues.
            navController.navigate(R.id.auth_nav_graph, null, navOptions)
        } catch (e: Exception) {
            // Fallback: welcome screen if graph route fails for any reason.
            navController.navigate(R.id.welcomeFragment, null, navOptions)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadCurrentUser()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
