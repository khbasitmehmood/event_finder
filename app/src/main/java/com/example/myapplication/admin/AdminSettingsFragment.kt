package com.example.myapplication.admin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AlertDialog
import com.example.myapplication.R

class AdminSettingsFragment : Fragment(R.layout.admin_fragment_settings) {

    private lateinit var switchEventNotifications: Switch
    private lateinit var switchBookingAlerts: Switch
    private lateinit var switchReviewAlerts: Switch
    private lateinit var tvThemeValue: TextView
    private lateinit var tvLanguageValue: TextView
    private lateinit var tvAppVersion: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🔹 Initialize Views
        switchEventNotifications = view.findViewById(R.id.switchEventNotifications)
        switchBookingAlerts = view.findViewById(R.id.switchBookingAlerts)
        switchReviewAlerts = view.findViewById(R.id.switchReviewAlerts)
        tvThemeValue = view.findViewById(R.id.tvThemeValue)
        tvLanguageValue = view.findViewById(R.id.tvLanguageValue)
        tvAppVersion = view.findViewById(R.id.tvAppVersion)

        // 🔹 Load saved preferences
        val pref = requireActivity().getSharedPreferences("admin_settings", 0)
        switchEventNotifications.isChecked = pref.getBoolean("event_notifications", true)
        switchBookingAlerts.isChecked = pref.getBoolean("booking_alerts", true)
        switchReviewAlerts.isChecked = pref.getBoolean("review_alerts", true)
        tvThemeValue.text = pref.getString("theme", "Light")
        tvLanguageValue.text = pref.getString("language", "English")
        tvAppVersion.text = getAppVersion()

        // 🔹 Switch listeners to save changes immediately
        switchEventNotifications.setOnCheckedChangeListener { _, isChecked ->
            pref.edit().putBoolean("event_notifications", isChecked).apply()
        }
        switchBookingAlerts.setOnCheckedChangeListener { _, isChecked ->
            pref.edit().putBoolean("booking_alerts", isChecked).apply()
        }
        switchReviewAlerts.setOnCheckedChangeListener { _, isChecked ->
            pref.edit().putBoolean("review_alerts", isChecked).apply()
        }

        // 🔹 Theme selection (front-end only)
        view.findViewById<View>(R.id.llTheme).setOnClickListener {
            val themes = arrayOf("Light", "Dark", "System Default")
            val currentTheme = tvThemeValue.text.toString()
            val checkedItem = themes.indexOf(currentTheme)

            AlertDialog.Builder(requireContext())
                .setTitle("Select Theme")
                .setSingleChoiceItems(themes, checkedItem) { dialog, which ->
                    tvThemeValue.text = themes[which]
                    pref.edit().putString("theme", themes[which]).apply()

                    // Apply theme instantly
                    when (themes[which]) {
                        "Light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        "Dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        "System Default" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    }

                    dialog.dismiss()
                    Toast.makeText(requireContext(), "Theme set to ${themes[which]}", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // 🔹 Language selection (front-end only)
        view.findViewById<View>(R.id.llLanguage).setOnClickListener {
            val languages = arrayOf("English", "Spanish", "French", "German", "Chinese")
            val currentLang = tvLanguageValue.text.toString()
            val checkedItem = languages.indexOf(currentLang)

            AlertDialog.Builder(requireContext())
                .setTitle("Select Language")
                .setSingleChoiceItems(languages, checkedItem) { dialog, which ->
                    tvLanguageValue.text = languages[which]
                    pref.edit().putString("language", languages[which]).apply()
                    dialog.dismiss()
                    Toast.makeText(requireContext(), "Language set to ${languages[which]}", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // 🔹 Other clickable actions
        view.findViewById<View>(R.id.llViewStats).setOnClickListener {
            Toast.makeText(requireContext(), "View Stats coming soon", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.llExportData).setOnClickListener {
            Toast.makeText(requireContext(), "Export Data coming soon", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.llClearCache).setOnClickListener {
            Toast.makeText(requireContext(), "Cache cleared", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.llPrivacyPolicy).setOnClickListener {
            openUrl("https://yourapp.com/privacy")
        }

        view.findViewById<View>(R.id.llTerms).setOnClickListener {
            openUrl("https://yourapp.com/terms")
        }

        view.findViewById<View>(R.id.llDeleteAccount).setOnClickListener {
            Toast.makeText(requireContext(), "Delete Account functionality coming soon", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.llContactSupport).setOnClickListener {
            openUrl("mailto:support@yourapp.com")
        }

        view.findViewById<View>(R.id.llFeedback).setOnClickListener {
            openUrl("mailto:feedback@yourapp.com")
        }
    }

    // 🔹 Helper: Get current app version
    private fun getAppVersion(): String {
        return try {
            val pInfo = requireActivity().packageManager.getPackageInfo(requireActivity().packageName, 0)
            pInfo.versionName
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    // 🔹 Helper: Open URL or email
    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Unable to open link", Toast.LENGTH_SHORT).show()
        }
    }
}
