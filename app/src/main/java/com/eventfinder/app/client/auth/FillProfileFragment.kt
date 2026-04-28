package com.eventfinder.app.client.auth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.eventfinder.app.R
import com.eventfinder.app.databinding.FragmentFillProfileBinding
import com.eventfinder.app.domain.model.UserType
import com.eventfinder.app.utils.UserPreferences
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class FillProfileFragment : Fragment(R.layout.fragment_fill_profile) {

    private var _binding: FragmentFillProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FillProfileViewModel by viewModels()

    @Inject
    lateinit var userPreferences: UserPreferences

    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null
    private var userType: UserType = UserType.USER

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                binding.ivProfileImage.setImageURI(uri)
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            selectedImageUri = cameraImageUri
            binding.ivProfileImage.setImageURI(cameraImageUri)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFillProfileBinding.bind(view)

        userType = UserType.valueOf(arguments?.getString("USER_TYPE") ?: UserType.USER.name)

        setupUI()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupUI() {
        if (userType == UserType.ORGANIZER) {
            binding.tvTitle.text = "Set up your organizer profile"
            binding.tvSubtitle.text = "Tell people who is hosting the events."
            binding.tilName.hint = "Organizer Name"
            
            binding.tilEmail.isVisible = true
            binding.tilContact.isVisible = true
            binding.tilDescription.isVisible = true
            binding.cardUseLocation.isVisible = false
        } else {
            binding.tvTitle.text = "Let's set up your profile"
            binding.tvSubtitle.text = "This helps us personalize events for you."
            binding.tilName.hint = "Full Name"
            
            binding.tilEmail.isVisible = false
            binding.tilContact.isVisible = false
            binding.tilDescription.isVisible = false
            binding.cardUseLocation.isVisible = true
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.flProfileImage.setOnClickListener {
            showImagePickerDialog()
        }
        
        binding.tvManualLocation.setOnClickListener {
            // Future location picker integration
            Toast.makeText(requireContext(), "Manual location selection coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnSaveProfile.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val city = binding.etCity.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val phone = binding.etContact.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()
            
            // Validate required fields
            if (name.isEmpty()) {
                binding.tilName.error = "Name is required"
                return@setOnClickListener
            }
            binding.tilName.error = null

            // Update profile with viewModel
            // For now, updating what FillProfileViewModel supports
            // In a complete implementation, FillProfileViewModel should be updated to take these fields.
            viewModel.updateProfile(
                name = name,
                contactNumber = phone,
                contactPerson = name, // Use as fallback
                description = description,
                interests = emptyList(), // Interests moved to next screen
                imageUri = selectedImageUri
            )
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Capture Photo", "Choose from Gallery")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Profile Photo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }
            .show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun openCamera() {
        val photoFile = File(requireContext().cacheDir, "profile_pic_${System.currentTimeMillis()}.jpg")
        cameraImageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
        cameraLauncher.launch(cameraImageUri)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is FillProfileUiState.Idle -> setLoadingState(false)
                            is FillProfileUiState.Loading -> setLoadingState(true)
                            is FillProfileUiState.Success -> {
                                setLoadingState(false)
                                // Move to Choose Interests
                                val bundle = Bundle().apply {
                                    putString("USER_TYPE", userType.name)
                                }
                                findNavController().navigate(R.id.action_fillProfile_to_chooseInterests, bundle)
                                viewModel.resetState()
                            }
                            is FillProfileUiState.Error -> {
                                setLoadingState(false)
                                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                                viewModel.resetState()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.btnSaveProfile.isEnabled = !isLoading
        binding.etName.isEnabled = !isLoading
        binding.etCity.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etContact.isEnabled = !isLoading
        binding.etDescription.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}