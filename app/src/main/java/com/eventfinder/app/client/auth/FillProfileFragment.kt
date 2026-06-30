package com.eventfinder.app.client.auth

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Geocoder
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class FillProfileFragment : Fragment(R.layout.fragment_fill_profile) {

    private companion object {
        private const val SERVICE_CITY = "Lahore"
    }

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
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var selectedLocationAddress: String? = null
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                binding.ivProfileImage.setImageURI(uri)
            }
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission is required to capture a photo", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            selectedImageUri = cameraImageUri
            binding.ivProfileImage.setImageURI(cameraImageUri)
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (granted) {
            useCurrentLocation()
        } else {
            binding.switchLocation.isChecked = false
            Toast.makeText(requireContext(), "Location permission is required to use current location", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFillProfileBinding.bind(view)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

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
        setupFragmentResultListeners()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupUI() {
        binding.etCity.setText(SERVICE_CITY)
        binding.etCity.isEnabled = false
        binding.etCity.isFocusable = false

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

        binding.cardUseLocation.setOnClickListener {
            if (binding.switchLocation.isEnabled) {
                binding.switchLocation.isChecked = !binding.switchLocation.isChecked
            }
        }

        binding.switchLocation.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requestCurrentLocation()
            }
        }
        
        binding.tvManualLocation.setOnClickListener {
            findNavController().navigate(R.id.mapLocationPickerFragment)
        }

        binding.btnSaveProfile.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val city = SERVICE_CITY
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
                locationAddress = selectedLocationAddress.takeIf { userType == UserType.USER },
                latitude = selectedLatitude.takeIf { userType == UserType.USER },
                longitude = selectedLongitude.takeIf { userType == UserType.USER },
                imageUri = selectedImageUri
            )
        }
    }

    private fun setupFragmentResultListeners() {
        setFragmentResultListener("location_request") { _, bundle ->
            val address = bundle.getString("address")
            val lat = bundle.getDouble("lat")
            val lng = bundle.getDouble("lng")

            if (!address.isNullOrBlank()) {
                selectedLocationAddress = address
                selectedLatitude = lat
                selectedLongitude = lng
                userPreferences.setUserLocation(address, lat, lng)
                showSelectedLocation(
                    address = address,
                    switchChecked = false,
                    switchEnabled = false
                )
                Toast.makeText(requireContext(), "Nearby-events location selected", Toast.LENGTH_SHORT).show()
            }
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
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }

        val photoFile = File(requireContext().cacheDir, "profile_pic_${System.currentTimeMillis()}.jpg")
        cameraImageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
        cameraLauncher.launch(cameraImageUri)
    }

    private fun requestCurrentLocation() {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFineLocation || hasCoarseLocation) {
            useCurrentLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun useCurrentLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            binding.switchLocation.isChecked = false
            return
        }

        setLocationLoading(true)
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    applyLocation(location)
                } else {
                    requestFreshLocation()
                }
            }
            .addOnFailureListener {
                setLocationLoading(false)
                binding.switchLocation.isChecked = false
                Toast.makeText(requireContext(), "Unable to get current location", Toast.LENGTH_SHORT).show()
            }
    }

    private fun requestFreshLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            setLocationLoading(false)
            binding.switchLocation.isChecked = false
            return
        }

        val cancellationTokenSource = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location ->
            if (location != null) {
                applyLocation(location)
            } else {
                setLocationLoading(false)
                binding.switchLocation.isChecked = false
                Toast.makeText(requireContext(), "Unable to get current location", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            setLocationLoading(false)
            binding.switchLocation.isChecked = false
            Toast.makeText(requireContext(), "Unable to get current location", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyLocation(location: Location) {
        val address = resolveAddress(location)
        setLocationLoading(false)
        if (address.isNullOrBlank()) {
            binding.switchLocation.isChecked = false
            Toast.makeText(requireContext(), "Unable to resolve current location", Toast.LENGTH_SHORT).show()
            return
        }

        selectedLocationAddress = address
        selectedLatitude = location.latitude
        selectedLongitude = location.longitude
        userPreferences.setUserLocation(address, location.latitude, location.longitude)
        showSelectedLocation(
            address = address,
            switchChecked = true,
            switchEnabled = true
        )
        Toast.makeText(requireContext(), "Nearby-events location set", Toast.LENGTH_SHORT).show()
    }

    private fun resolveAddress(location: Location): String? {
        return try {
            val addresses = Geocoder(requireContext(), Locale.getDefault())
                .getFromLocation(location.latitude, location.longitude, 1)
            val address = addresses?.firstOrNull()
            address?.getAddressLine(0)
                ?: address?.locality
                ?: address?.subAdminArea
                ?: address?.adminArea
                ?: address?.countryName
        } catch (_: Exception) {
            null
        }
    }

    private fun setLocationLoading(isLoading: Boolean) {
        binding.switchLocation.isEnabled = !isLoading
        binding.cardUseLocation.isEnabled = !isLoading
        binding.tvUseLocationTitle.text = if (isLoading) {
            "Getting location..."
        } else {
            getString(R.string.fill_profile_use_location)
        }
    }

    private fun showSelectedLocation(
        address: String,
        switchChecked: Boolean,
        switchEnabled: Boolean
    ) {
        binding.switchLocation.setOnCheckedChangeListener(null)
        binding.switchLocation.isChecked = switchChecked
        binding.switchLocation.isEnabled = switchEnabled
        binding.switchLocation.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requestCurrentLocation()
            } else {
                selectedLocationAddress = null
                selectedLatitude = null
                selectedLongitude = null
                binding.tvSelectedLocation.isVisible = false
            }
        }
        binding.tvSelectedLocation.text = address
        binding.tvSelectedLocation.isVisible = true
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
        binding.etCity.isEnabled = false
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
            binding.etCity.setText(SERVICE_CITY)
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
            binding.etCity.setText(SERVICE_CITY)
            selectedLocationAddress = profile?.locationAddress ?: userPreferences.getUserLocationAddress()
            selectedLatitude = profile?.latitude ?: userPreferences.getUserLocationLatitude()
            selectedLongitude = profile?.longitude ?: userPreferences.getUserLocationLongitude()
            selectedLocationAddress?.let {
                showSelectedLocation(
                    address = it,
                    switchChecked = true,
                    switchEnabled = true
                )
            }

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
