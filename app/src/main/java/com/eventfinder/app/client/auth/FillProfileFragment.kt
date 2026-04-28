package com.eventfinder.app.client.auth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
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
import com.eventfinder.app.databinding.FragmentFillProfileBinding
import com.eventfinder.app.domain.model.UserType
import com.eventfinder.app.utils.AuthNavArgs
import com.eventfinder.app.utils.AuthFlowSource
import com.eventfinder.app.utils.AuthPendingStep
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
    private var hasUserTypeArg: Boolean = false
    private var flowSource: String = AuthFlowSource.REGISTER
    private var isEditMode: Boolean = false
    private var hasPrefilledData: Boolean = false

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

        isEditMode = arguments?.getBoolean(AuthNavArgs.IS_EDIT_MODE, false) == true
        hasUserTypeArg = arguments?.containsKey(AuthNavArgs.USER_TYPE) == true
        userType = UserType.valueOf(arguments?.getString(AuthNavArgs.USER_TYPE) ?: UserType.USER.name)
        flowSource = arguments?.getString(AuthNavArgs.FLOW_SOURCE) ?: AuthFlowSource.REGISTER

        if (!isEditMode) {
            val pending = if (flowSource == AuthFlowSource.LOGIN) {
                AuthPendingStep.FILL_PROFILE_LOGIN
            } else {
                AuthPendingStep.FILL_PROFILE_REGISTER
            }
            userPreferences.setPendingAuthStep(pending)
        }

        setupUI()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupUI() {
        if (userType == UserType.ORGANIZER) {
            binding.tvTitle.text = getString(R.string.fill_profile_organizer_title)
            binding.tvSubtitle.text = getString(R.string.fill_profile_organizer_subtitle)
            binding.tvNameLabel.text = getString(R.string.fill_profile_hint_organizer_name)
            binding.tilName.hint = getString(R.string.fill_profile_hint_organizer_name)

            binding.tvContactLabel.isVisible = true
            binding.tilContact.isVisible = true
            binding.tvDescriptionLabel.isVisible = true
            binding.tilDescription.isVisible = true
            binding.cardUseLocation.isVisible = false
            binding.tvManualLocation.isVisible = false
        } else {
            binding.tvTitle.text = getString(R.string.fill_profile_user_title)
            binding.tvSubtitle.text = getString(R.string.fill_profile_user_subtitle)
            binding.tvNameLabel.text = getString(R.string.hint_full_name)
            binding.tilName.hint = getString(R.string.hint_full_name)

            binding.tvContactLabel.isVisible = false
            binding.tilContact.isVisible = false
            binding.tvDescriptionLabel.isVisible = false
            binding.tilDescription.isVisible = false
            binding.cardUseLocation.isVisible = true
            binding.tvManualLocation.isVisible = true
        }

        if (isEditMode) {
            binding.tvTitle.text = getString(R.string.fill_profile_edit_title)
            binding.tvSubtitle.text = getString(R.string.fill_profile_edit_subtitle)
            binding.btnSaveProfile.text = getString(R.string.save)
        } else {
            binding.btnSaveProfile.text = getString(R.string.btn_continue)
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
            Toast.makeText(requireContext(), getString(R.string.fill_profile_manual_location_soon), Toast.LENGTH_SHORT).show()
        }

        binding.btnSaveProfile.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val city = binding.etCity.text.toString().trim()
            val phone = binding.etContact.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()
            
            // Validate required fields
            if (name.isEmpty()) {
                binding.tilName.error = getString(R.string.fill_profile_error_name_required)
                return@setOnClickListener
            }
            binding.tilName.error = null

            // Update profile with viewModel
            viewModel.updateProfile(
                userType = userType,
                name = name,
                city = city,
                contactNumber = phone,
                contactPerson = name, // Use as fallback
                description = description,
                interests = emptyList(), // Interests moved to next screen
                imageUri = selectedImageUri
            )
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf(
            getString(R.string.fill_profile_photo_capture),
            getString(R.string.fill_profile_photo_gallery)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.fill_profile_photo_title))
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
                        if (user != null) {
                            if (!hasUserTypeArg) {
                                userType = user.userType
                            }
                            setupUI()
                            prefillUserData(user)
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
                                if (isEditMode) {
                                    findNavController().navigateUp()
                                } else {
                                    if (flowSource == AuthFlowSource.LOGIN) {
                                        userPreferences.clearPendingAuthStep()

                                        val destination = if (userType == UserType.ORGANIZER) {
                                            R.id.organizer_main_graph
                                        } else {
                                            R.id.user_main_graph
                                        }

                                        val navOptions = NavOptions.Builder()
                                            .setLaunchSingleTop(true)
                                            .setPopUpTo(R.id.main_nav_graph, true)
                                            .build()
                                        findNavController().navigate(destination, null, navOptions)
                                    } else {
                                        userPreferences.setPendingAuthStep(AuthPendingStep.CHOOSE_INTERESTS)
                                        // Register onboarding continues to interests selection.
                                        val bundle = Bundle().apply {
                                            putString(AuthNavArgs.USER_TYPE, userType.name)
                                        }
                                        val navOptions = NavOptions.Builder()
                                            .setLaunchSingleTop(true)
                                            .build()
                                        findNavController().navigate(R.id.action_fillProfile_to_chooseInterests, bundle, navOptions)
                                    }
                                }
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
        binding.etContact.isEnabled = !isLoading
        binding.etDescription.isEnabled = !isLoading
    }

    private fun prefillUserData(user: com.eventfinder.app.domain.model.User) {
        if (hasPrefilledData) return

        val defaultName = userPreferences.getUserName().takeIf { it.isNotBlank() && it != "Guest User" }
        if (user.userType == UserType.ORGANIZER) {
            val profile = user.organizerProfile
            binding.etName.setText(profile?.organizationName ?: defaultName.orEmpty())
            binding.etContact.setText(profile?.phoneNumber.orEmpty())
            binding.etCity.setText(profile?.city.orEmpty())
            binding.etDescription.setText(profile?.description.orEmpty())

            val logoUrl = profile?.logoUrl
            if (!logoUrl.isNullOrBlank()) {
                binding.ivProfileImage.load(logoUrl) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                    placeholder(R.drawable.ic_profile)
                    error(R.drawable.ic_profile)
                }
            }
        } else {
            val profile = user.profile
            binding.etName.setText(profile?.fullName ?: defaultName.orEmpty())
            binding.etCity.setText(profile?.city.orEmpty())

            val photoUrl = profile?.photoUrl
            if (!photoUrl.isNullOrBlank()) {
                binding.ivProfileImage.load(photoUrl) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                    placeholder(R.drawable.ic_profile)
                    error(R.drawable.ic_profile)
                }
            }
        }

        hasPrefilledData = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}