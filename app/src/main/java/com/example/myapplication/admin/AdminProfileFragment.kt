package com.example.myapplication.admin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R

class AdminProfileFragment : Fragment(R.layout.admin_fragment_profile) {

    private lateinit var ivAdminProfile: ImageView
    private lateinit var tvAdminName: TextView
    private lateinit var tvAdminEmail: TextView
    private lateinit var tvAdminContact: TextView
    private lateinit var tvAdminOrganization: TextView
    private lateinit var tvTotalEvents: TextView
    private lateinit var tvAdminRating: TextView
    private lateinit var tvAdminIntro: TextView
    private lateinit var btnSave: Button

    private var selectedImageUri: Uri? = null

    // Image Picker with persistent permission
    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                // Persist permission across app restarts
                requireContext().contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                selectedImageUri = it
                ivAdminProfile.setImageURI(it)

                // Save URI permanently
                val pref = requireActivity().getSharedPreferences("admin_profile", 0)
                pref.edit().putString("profile_image", it.toString()).apply()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // INIT VIEWS
        ivAdminProfile = view.findViewById(R.id.ivAdminProfile)
        tvAdminName = view.findViewById(R.id.tvAdminName)
        tvAdminEmail = view.findViewById(R.id.tvAdminEmail)
        tvAdminContact = view.findViewById(R.id.tvAdminContact)
        tvAdminOrganization = view.findViewById(R.id.tvAdminOrganization)
        tvTotalEvents = view.findViewById(R.id.tvTotalEvents)
        tvAdminRating = view.findViewById(R.id.tvAdminRating)
        tvAdminIntro = view.findViewById(R.id.tvAdminIntro)
        btnSave = view.findViewById(R.id.btnSaveProfile)

        // SET CLICK LISTENERS TO OPEN EDIT SCREEN
        ivAdminProfile.setOnClickListener {
            imagePickerLauncher.launch(arrayOf("image/*"))
        }

        tvAdminName.setOnClickListener { openEditScreen("name", tvAdminName.text.toString()) }
        tvAdminEmail.setOnClickListener { openEditScreen("email", tvAdminEmail.text.toString()) }
        tvAdminContact.setOnClickListener { openEditScreen("contact", tvAdminContact.text.toString()) }
        tvAdminOrganization.setOnClickListener { openEditScreen("organization", tvAdminOrganization.text.toString()) }
        tvTotalEvents.setOnClickListener { openEditScreen("events", tvTotalEvents.text.toString()) }
        tvAdminIntro.setOnClickListener { openEditScreen("intro", tvAdminIntro.text.toString()) }

        // Rating is read-only
        tvAdminRating.isClickable = false
        tvAdminRating.isFocusable = false

        // Save button
        btnSave.setOnClickListener { saveAdminProfile() }

        // Observe edited results
        observeEditResults()

        // Load admin data
        loadAdminData()
    }

    private fun openEditScreen(field: String, value: String) {
        val bundle = Bundle().apply {
            putString("field", field)
            putString("current_value", value)
        }
        findNavController().navigate(
            R.id.action_adminProfileFragment_to_editAdminProfileFragment,
            bundle
        )
    }

    private fun observeEditResults() {
        val navBackStackEntry = findNavController().currentBackStackEntry
        val savedStateHandle = navBackStackEntry?.savedStateHandle

        savedStateHandle?.getLiveData<String>("name")?.observe(viewLifecycleOwner) { tvAdminName.text = it }
        savedStateHandle?.getLiveData<String>("email")?.observe(viewLifecycleOwner) { tvAdminEmail.text = it }
        savedStateHandle?.getLiveData<String>("contact")?.observe(viewLifecycleOwner) { tvAdminContact.text = it }
        savedStateHandle?.getLiveData<String>("organization")?.observe(viewLifecycleOwner) { tvAdminOrganization.text = it }
        savedStateHandle?.getLiveData<String>("events")?.observe(viewLifecycleOwner) { tvTotalEvents.text = it }
        savedStateHandle?.getLiveData<String>("intro")?.observe(viewLifecycleOwner) { tvAdminIntro.text = it }
    }

    private fun loadAdminData() {
        val pref = requireActivity().getSharedPreferences("admin_profile", 0)

        // Load image safely
        pref.getString("profile_image", null)?.let {
            try {
                val uri = Uri.parse(it)
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                ivAdminProfile.setImageURI(uri)
            } catch (_: Exception) {
                ivAdminProfile.setImageResource(R.drawable.ic_profile)
            }
        } ?: ivAdminProfile.setImageResource(R.drawable.ic_profile)

        tvAdminName.text = pref.getString("name", "John Doe")
        tvAdminEmail.text = pref.getString("email", "admin@example.com")
        tvAdminContact.text = pref.getString("contact", "+92-300-1234567")
        tvAdminOrganization.text = pref.getString("organization", "EventX (Owner)")
        tvTotalEvents.text = pref.getString("totalEvents", "5")
        tvAdminRating.text = "4.8 / 5"
        tvAdminIntro.text = pref.getString("intro", "Passionate event organizer with 5+ years experience.")
    }

    private fun saveAdminProfile() {
        val pref = requireActivity().getSharedPreferences("admin_profile", 0)
        with(pref.edit()) {
            putString("name", tvAdminName.text.toString())
            putString("email", tvAdminEmail.text.toString())
            putString("contact", tvAdminContact.text.toString())
            putString("organization", tvAdminOrganization.text.toString())
            putString("totalEvents", tvTotalEvents.text.toString())
            putString("intro", tvAdminIntro.text.toString())
            apply()
        }
        Toast.makeText(requireContext(), "Profile saved successfully", Toast.LENGTH_SHORT).show()
    }
}
