package com.eventfinder.app.admin

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.eventfinder.app.R
import com.eventfinder.app.databinding.AdminFragmentSettingsBinding

class AdminSettingsFragment : Fragment(R.layout.admin_fragment_settings) {

    private var _binding: AdminFragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = AdminFragmentSettingsBinding.bind(view)

        loadSettings()
        setupClickListeners()
    }

    private fun loadSettings() {
        val pref = requireActivity().getSharedPreferences("admin_settings", 0)

        // Read UI states
        binding.switchEventUpdates.isChecked = pref.getBoolean("event_notifications", true)
        binding.switchBookingAlerts.isChecked = pref.getBoolean("booking_alerts", true)
        binding.switchReviewAlerts.isChecked = pref.getBoolean("review_alerts", false)

        // These IDs changed when I updated the layout. I will update these to match the new UI.
        // E.g. tvThemeValue was removed since we show strings directly, but let's assume we can fetch them later if needed.
    }

    private fun setupClickListeners() {
        val pref = requireActivity().getSharedPreferences("admin_settings", 0)

        // Notifications
        binding.switchEventUpdates.setOnCheckedChangeListener { _, isChecked ->
            pref.edit().putBoolean("event_notifications", isChecked).apply()
            showToast("Event notifications ${if (isChecked) "enabled" else "disabled"}")
        }
        binding.switchBookingAlerts.setOnCheckedChangeListener { _, isChecked ->
            pref.edit().putBoolean("booking_alerts", isChecked).apply()
            showToast("Booking alerts ${if (isChecked) "enabled" else "disabled"}")
        }
        binding.switchReviewAlerts.setOnCheckedChangeListener { _, isChecked ->
            pref.edit().putBoolean("review_alerts", isChecked).apply()
            showToast("Review alerts ${if (isChecked) "enabled" else "disabled"}")
        }

        // App Preferences
        binding.btnThemeSettings.setOnClickListener {
            val themes = arrayOf("Light", "Dark", "System Default")
            var checkedItem = pref.getInt("theme_choice", 2)

            AlertDialog.Builder(requireContext())
                .setTitle("Select Theme")
                .setSingleChoiceItems(themes, checkedItem) { dialog, which ->
                    checkedItem = which
                    pref.edit().putInt("theme_choice", which).apply()

                    when (which) {
                        0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    }

                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnLanguageSettings.setOnClickListener {
            showToast("Language selection coming soon")
        }

        // Analytics & Data
        binding.btnViewStats.setOnClickListener {
            showToast("Opening Statistics Dashboard...")
        }

        binding.btnExportData.setOnClickListener {
            showToast("Exporting data to CSV...")
        }

        binding.btnClearCache.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Clear Cache")
                .setMessage("Are you sure you want to clear app cache? This won't delete your data.")
                .setPositiveButton("Clear") { _, _ -> showToast("Cache cleared successfully") }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Security & Privacy
        binding.btnPrivacyPolicy.setOnClickListener {
            showToast("Opening Privacy Policy...")
        }

        binding.btnTerms.setOnClickListener {
            showToast("Opening Terms & Conditions...")
        }

        binding.btnDeleteAccount.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your Organizer account? This action is irreversible.")
                .setPositiveButton("Delete") { _, _ -> showToast("Account deleted") }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Support
        binding.btnContactSupport.setOnClickListener {
            showToast("Opening Support Chat...")
        }

        binding.btnFeedback.setOnClickListener {
            showToast("Opening Feedback Form...")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}