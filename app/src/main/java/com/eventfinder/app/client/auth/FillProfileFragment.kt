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
import com.google.android.material.chip.Chip
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
    private var isEditMode = false

    private val allCategories = listOf("Music", "Education", "Sports", "Business", "Movies", "Politics")

    // For picking image from gallery
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                binding.ivProfileImage.setImageURI(uri)
            }
        }
    }

    // For capturing image from camera
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            selectedImageUri = cameraImageUri
            binding.ivProfileImage.setImageURI(cameraImageUri)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFillProfileBinding.bind(view)

        isEditMode = arguments?.getBoolean("IS_EDIT_MODE", false) ?: false

        setupUI()
        setupClickListeners()
        observeViewModel()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (isEditMode) {
                findNavController().popBackStack()
            } else {
                requireActivity().finish()
            }
        }
    }

    private fun setupUI() {
        val userType = userPreferences.getUserType()
        
        if (isEditMode) {
            binding.btnBack.isVisible = true
            binding.tvTitle.text = getString(R.string.action_settings).replace("Settings", "Edit Profile")
            binding.tvSubtitle.text = "Update your details below"
            binding.btnSaveProfile.text = "Save Changes"
        }

        if (userType == UserType.ORGANIZER.name) {
            binding.tilName.hint = "Organization Name"
            binding.llOrganizerFields.isVisible = true
            binding.tvCategoriesLabel.text = "Events we offer"
        } else {
            binding.tilName.hint = "Full Name"
            binding.llOrganizerFields.isVisible = false
            binding.tvCategoriesLabel.text = "Add Interests"
        }

        setupChips(emptyList())
    }

    private fun setupChips(selectedInterests: List<String>) {
        binding.chipGroupCategories.removeAllViews()
        for (category in allCategories) {
            val chip = layoutInflater.inflate(R.layout.item_chip_choice, binding.chipGroupCategories, false) as Chip
            chip.text = category
            chip.isChecked = selectedInterests.contains(category)
            binding.chipGroupCategories.addView(chip)
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.flProfileImage.setOnClickListener {
            showImagePickerDialog()
        }

        binding.btnSaveProfile.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val contact = binding.etContact.text.toString().trim()
            val contactPerson = binding.etContactPerson.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()

            // Gather selected interests/categories
            val selectedInterests = mutableListOf<String>()
            for (i in 0 until binding.chipGroupCategories.childCount) {
                val chip = binding.chipGroupCategories.getChildAt(i) as Chip
                if (chip.isChecked) {
                    selectedInterests.add(chip.text.toString())
                }
            }

            // We now check if an image is provided OR if the user already has one loaded via Firebase
            val existingPhotoUrl = viewModel.currentUser.value?.profile?.photoUrl 
                ?: viewModel.currentUser.value?.organizerProfile?.logoUrl
                
            if (selectedImageUri == null && existingPhotoUrl.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Please select a profile photo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (name.isEmpty()) {
                binding.tilName.error = "This field is required"
                return@setOnClickListener
            }

            val userType = userPreferences.getUserType()
            if (userType == UserType.ORGANIZER.name) {
                if (contactPerson.isEmpty()) {
                    binding.tilContactPerson.error = "Contact person is required"
                    return@setOnClickListener
                }
                if (contact.isEmpty()) {
                    binding.tilContact.error = "Contact number is required"
                    return@setOnClickListener
                }
            }

            binding.tilName.error = null
            binding.tilContact.error = null
            binding.tilContactPerson.error = null

            viewModel.updateProfile(
                name = name,
                contactNumber = contact,
                contactPerson = contactPerson,
                description = description,
                interests = selectedInterests,
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
                    viewModel.currentUser.collect { user ->
                        user?.let {
                            if (it.userType == UserType.ORGANIZER) {
                                binding.etName.setText(it.organizerProfile?.organizationName)
                                binding.etContact.setText(it.organizerProfile?.phoneNumber)
                                binding.etContactPerson.setText(it.organizerProfile?.contactPerson)
                                binding.etDescription.setText(it.organizerProfile?.description)
                                
                                setupChips(it.organizerProfile?.offeredEvents ?: emptyList())
                                
                                it.organizerProfile?.logoUrl?.let { url ->
                                    if (url.isNotBlank() && selectedImageUri == null) {
                                        binding.ivProfileImage.load(url) {
                                            crossfade(true)
                                            transformations(CircleCropTransformation())
                                        }
                                    }
                                }
                            } else {
                                binding.etName.setText(it.profile?.fullName)
                                binding.etContact.setText(it.profile?.phoneNumber)
                                
                                setupChips(it.profile?.interests ?: emptyList())
                                
                                it.profile?.photoUrl?.let { url ->
                                    if (url.isNotBlank() && selectedImageUri == null) {
                                        binding.ivProfileImage.load(url) {
                                            crossfade(true)
                                            transformations(CircleCropTransformation())
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is FillProfileUiState.Idle -> setLoadingState(false)
                            is FillProfileUiState.Loading -> setLoadingState(true)
                            is FillProfileUiState.Success -> {
                                setLoadingState(false)
                                Toast.makeText(context, "Profile saved successfully!", Toast.LENGTH_SHORT).show()
                                
                                if (isEditMode) {
                                    findNavController().popBackStack()
                                } else {
                                    val bundle = Bundle().apply {
                                        putString("TARGET", "USER")
                                    }
                                    findNavController().navigate(R.id.transitionFragment, bundle)
                                }
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
        binding.etContact.isEnabled = !isLoading
        binding.etContactPerson.isEnabled = !isLoading
        binding.etDescription.isEnabled = !isLoading
        binding.flProfileImage.isEnabled = !isLoading
        binding.chipGroupCategories.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}