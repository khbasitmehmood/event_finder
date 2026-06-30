package com.eventfinder.app.client.createevent

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.eventfinder.app.R
import com.eventfinder.app.databinding.FragmentCreateEventNewBinding
import com.eventfinder.app.domain.model.EventCategory
import com.eventfinder.app.domain.model.EventVisibility
import com.eventfinder.app.domain.model.UserType
import com.eventfinder.app.utils.AuthNavArgs
import com.eventfinder.app.utils.UserPreferences
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class CreateEventFragment : Fragment(R.layout.fragment_create_event_new) {

    private var _binding: FragmentCreateEventNewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreateEventViewModel by viewModels()

    @Inject
    lateinit var userPreferences: UserPreferences

    private var selectedDate: Calendar? = null
    private var selectedStartTime: Calendar? = null
    private var selectedEndTime: Calendar? = null

    private val selectedCategories = mutableSetOf<EventCategory>()

    // Selected Map Location
    private var selectedLocationAddress: String? = null
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateEventNewBinding.bind(view)

        setupToolbar()
        setupDateTimePickers()
        setupPricingToggle()
        setupImageUpload()
        setupActionButtons()
        observeViewModel()
        setupFragmentResultListeners()
    }

    override fun onResume() {
        super.onResume()
        // Reload categories in case user returned from ChooseInterestsFragment
        viewModel.loadCategories()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        handleUiState(state)
                    }
                }

                launch {
                    viewModel.draftState.collect { state ->
                        handleDraftState(state)
                    }
                }
                
                launch {
                    viewModel.isCategoriesLoading.collect { isLoading ->
                        binding.progressCategories.isVisible = isLoading
                        binding.scrollCategories.isVisible = !isLoading
                        binding.btnAddMoreCategories.isEnabled = !isLoading
                    }
                }
                
                launch {
                    viewModel.categories.collect { categories ->
                        setupCategoryChips(categories)
                    }
                }
            }
        }
    }

    private fun setupFragmentResultListeners() {
        setFragmentResultListener("location_request") { _, bundle ->
            val address = bundle.getString("address")
            val lat = bundle.getDouble("lat")
            val lng = bundle.getDouble("lng")

            if (address != null) {
                selectedLocationAddress = address
                selectedLatitude = lat
                selectedLongitude = lng
                binding.etLocation.setText(address)
            }
        }
    }

    private fun handleUiState(state: CreateEventUiState) {
        when (state) {
            is CreateEventUiState.Idle -> {
                setLoadingState(false)
            }
            is CreateEventUiState.Loading -> {
                setLoadingState(true)
            }
            is CreateEventUiState.Success -> {
                setLoadingState(false)
                showSuccessDialog(state.event.title)
            }
            is CreateEventUiState.Error -> {
                setLoadingState(false)
                showErrorDialog(state.message)
            }
        }
    }

    private fun handleDraftState(state: DraftState) {
        when (state) {
            is DraftState.Idle -> {
                // No action needed
            }
            is DraftState.Saving -> {
                Toast.makeText(context, "Saving draft...", Toast.LENGTH_SHORT).show()
            }
            is DraftState.Saved -> {
                Toast.makeText(context, "Draft saved successfully", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
            is DraftState.Error -> {
                Toast.makeText(context, "Failed to save draft", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.btnPublish.isEnabled = !isLoading
        binding.btnSaveDraft.isEnabled = !isLoading

        if (isLoading) {
            binding.btnPublish.text = "Creating..."
        } else {
            binding.btnPublish.text = "Publish Event"
        }
    }

    private fun showSuccessDialog(eventTitle: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Event Created! 🎉")
            .setMessage("\"$eventTitle\" has been published successfully.")
            .setPositiveButton("View Events") { _, _ ->
                // Navigate back to home
                findNavController().navigateUp()
            }
            .setCancelable(false)
            .show()
    }

    private fun showErrorDialog(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupCategoryChips(categories: List<EventCategory>) {
        binding.cgCategories.removeAllViews()
        
        // Retain previously selected categories that are still in the list
        val validSelectedIds = categories.map { it.id }.toSet()
        selectedCategories.removeAll { it.id !in validSelectedIds }

        categories.forEach { category ->
            val chip = Chip(requireContext()).apply {
                text = category.name
                isCheckable = true
                isChecked = selectedCategories.any { it.id == category.id }
                
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedCategories.add(category)
                    } else {
                        selectedCategories.removeIf { it.id == category.id }
                    }
                }
            }
            binding.cgCategories.addView(chip)
        }
    }

    private fun setupDateTimePickers() {
        // Date Picker
        binding.cardSelectDate.setOnClickListener {
            val calendar = selectedDate ?: Calendar.getInstance()

            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    selectedDate = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    }
                    binding.tvSelectedDate.text = dateFormat.format(selectedDate!!.time)
                    binding.tvSelectedDate.setTextColor(
                        resources.getColor(R.color.text_primary, null)
                    )
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).apply {
                datePicker.minDate = System.currentTimeMillis()
                show()
            }
        }

        // Start Time Picker
        binding.cardSelectStartTime.setOnClickListener {
            val calendar = selectedStartTime ?: Calendar.getInstance()

            TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    selectedStartTime = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hourOfDay)
                        set(Calendar.MINUTE, minute)
                    }
                    binding.tvSelectedStartTime.text = timeFormat.format(selectedStartTime!!.time)
                    binding.tvSelectedStartTime.setTextColor(
                        resources.getColor(R.color.text_primary, null)
                    )
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false // 12-hour format
            ).show()
        }
        
        // End Time Picker
        binding.cardSelectEndTime.setOnClickListener {
            val calendar = selectedEndTime ?: Calendar.getInstance()

            TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    selectedEndTime = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hourOfDay)
                        set(Calendar.MINUTE, minute)
                    }
                    binding.tvSelectedEndTime.text = timeFormat.format(selectedEndTime!!.time)
                    binding.tvSelectedEndTime.setTextColor(
                        resources.getColor(R.color.text_primary, null)
                    )
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false // 12-hour format
            ).show()
        }
    }

    private fun setupPricingToggle() {
        binding.toggleGroupPricing.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val isFree = checkedId == R.id.btnFree
                binding.layoutPrice.isVisible = !isFree
                if (isFree) {
                    binding.etTicketPrice.text?.clear()
                }
            }
        }

        // Setup visibility toggle
        binding.toggleGroupVisibility.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val isPrivate = checkedId == R.id.btnPrivate
                binding.cardRequiresTicket.isVisible = isPrivate

                // Update info text
                if (isPrivate) {
                    binding.tvVisibilityInfo.text = "Private events require invitation and can have tickets with QR codes"
                } else {
                    binding.tvVisibilityInfo.text = "Public: Anyone can see and join\nPrivate: Requires ticket for entry"
                    binding.switchRequiresTicket.isChecked = false
                }
            }
        }
    }

    private fun setupImageUpload() {
        binding.cardEventImage.setOnClickListener {
            // TODO: Open image picker
            Toast.makeText(context, "Image picker coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupActionButtons() {
        binding.btnAddMoreCategories.setOnClickListener {
            val bundle = Bundle().apply {
                putString(AuthNavArgs.USER_TYPE, UserType.ORGANIZER.name)
                putBoolean("FROM_CREATE_EVENT", true)
            }
            findNavController().navigate(R.id.chooseInterestsFragment, bundle)
        }

        binding.btnSelectLocationOnMap.setOnClickListener {
            findNavController().navigate(R.id.mapLocationPickerFragment)
        }

        // Hook up the layout wrapper for end icon click
        binding.layoutLocation.setEndIconOnClickListener {
            findNavController().navigate(R.id.mapLocationPickerFragment)
        }

        // Save Draft
        binding.btnSaveDraft.setOnClickListener {
            if (validateBasicInfo()) {
                saveDraft()
            }
        }

        // Publish Event
        binding.btnPublish.setOnClickListener {
            if (validateAllFields()) {
                publishEvent(saveAsDraft = false)
            }
        }

        binding.btnSaveDraft.setOnClickListener {
            if (validateBasicInfo()) {
                publishEvent(saveAsDraft = true)
            }
        }
    }

    private fun validateBasicInfo(): Boolean {
        val title = binding.etEventTitle.text?.toString()?.trim()

        if (title.isNullOrEmpty()) {
            binding.etEventTitle.error = "Title is required"
            binding.etEventTitle.requestFocus()
            return false
        }

        return true
    }

    private fun validateAllFields(): Boolean {
        val title = binding.etEventTitle.text?.toString()?.trim()
        val description = binding.etDescription.text?.toString()?.trim()
        val location = binding.etLocation.text?.toString()?.trim()

        when {
            title.isNullOrEmpty() -> {
                binding.etEventTitle.error = "Title is required"
                binding.etEventTitle.requestFocus()
                return false
            }
            description.isNullOrEmpty() -> {
                binding.etDescription.error = "Description is required"
                binding.etDescription.requestFocus()
                return false
            }
            selectedCategories.isEmpty() -> {
                Toast.makeText(context, "Please select at least one category", Toast.LENGTH_SHORT).show()
                return false
            }
            selectedDate == null -> {
                Toast.makeText(context, "Please select a date", Toast.LENGTH_SHORT).show()
                return false
            }
            selectedStartTime == null -> {
                Toast.makeText(context, "Please select a start time", Toast.LENGTH_SHORT).show()
                return false
            }
            location.isNullOrEmpty() || selectedLatitude == null || selectedLongitude == null -> {
                Toast.makeText(context, "Please select location from the map", Toast.LENGTH_SHORT).show()
                binding.etLocation.error = "Location is required"
                binding.etLocation.requestFocus()
                return false
            }
        }

        return true
    }

    private fun saveDraft() {
        val title = binding.etEventTitle.text?.toString()?.trim() ?: ""
        val description = binding.etDescription.text?.toString()?.trim()
        val location = binding.etLocation.text?.toString()?.trim()

        // Combine date and time if available
        var startTimeMillis: Long? = null
        if (selectedDate != null && selectedStartTime != null) {
            val startDateTime = Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedDate!!.get(Calendar.YEAR))
                set(Calendar.MONTH, selectedDate!!.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, selectedDate!!.get(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, selectedStartTime!!.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, selectedStartTime!!.get(Calendar.MINUTE))
            }
            startTimeMillis = startDateTime.timeInMillis
        }

        var endTimeMillis: Long? = null
        if (selectedDate != null && selectedEndTime != null) {
            val endDateTime = Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedDate!!.get(Calendar.YEAR))
                set(Calendar.MONTH, selectedDate!!.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, selectedDate!!.get(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, selectedEndTime!!.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, selectedEndTime!!.get(Calendar.MINUTE))
            }
            endTimeMillis = endDateTime.timeInMillis
        }

        val isFree = binding.toggleGroupPricing.checkedButtonId == R.id.btnFree
        val price = if (!isFree) {
            binding.etTicketPrice.text?.toString()?.toDoubleOrNull()
        } else null

        val currency = if (!isFree) {
            "PKR"
        } else null

        val maxParticipants = binding.etMaxAttendees.text?.toString()?.toIntOrNull()

        val isPrivate = binding.toggleGroupVisibility.checkedButtonId == R.id.btnPrivate
        val visibility = if (isPrivate) EventVisibility.PRIVATE else EventVisibility.PUBLIC
        val requiresTicket = !isFree || binding.switchRequiresTicket.isChecked

        viewModel.saveDraft(
            title = title,
            description = description,
            selectedCategories = selectedCategories.toList(),
            startTimeMillis = startTimeMillis,
            endTimeMillis = endTimeMillis,
            locationName = location,
            latitude = selectedLatitude,
            longitude = selectedLongitude,
            address = selectedLocationAddress,
            maxParticipants = maxParticipants,
            isFree = isFree,
            price = price,
            currency = currency,
            organizerId = userPreferences.getUserId() ?: "",
            tags = emptyList(),
            visibility = visibility,
            requiresTicket = requiresTicket
        )
    }

    private fun publishEvent(saveAsDraft: Boolean = false) {
        // Combine date and start time
        val eventStartDateTime = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedDate!!.get(Calendar.YEAR))
            set(Calendar.MONTH, selectedDate!!.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, selectedDate!!.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, selectedStartTime!!.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, selectedStartTime!!.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
        }
        
        // Combine date and end time if selected
        val eventEndDateTime = selectedEndTime?.let { endTime ->
            Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedDate!!.get(Calendar.YEAR))
                set(Calendar.MONTH, selectedDate!!.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, selectedDate!!.get(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, endTime.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, endTime.get(Calendar.MINUTE))
                set(Calendar.SECOND, 0)
            }
        }

        // Get all form data
        val title = binding.etEventTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        
        // Set first category as main, and rest (or all) as tags
        val primaryCategory = selectedCategories.first()
        val extraTags = selectedCategories.map { it.name }

        val locationName = selectedLocationAddress ?: binding.etLocation.text.toString().trim()
        val address = locationName // Use location name as address for now

        val maxParticipants = binding.etMaxAttendees.text?.toString()?.toIntOrNull()
        val isFree = binding.toggleGroupPricing.checkedButtonId == R.id.btnFree
        val price = if (!isFree) binding.etTicketPrice.text?.toString()?.toDoubleOrNull() else null
        val currency = if (!isFree) "PKR" else null

        // Get actual map selected coordinates
        val latitude = selectedLatitude ?: 31.5497
        val longitude = selectedLongitude ?: 74.3436

        // Get user ID and name from preferences
        val userId = userPreferences.getUserId()
        val userName = userPreferences.getUserName()

        // Determine event visibility and ticket requirement
        val isPrivate = binding.toggleGroupVisibility.checkedButtonId == R.id.btnPrivate
        val visibility = if (isPrivate) EventVisibility.PRIVATE else EventVisibility.PUBLIC
        val requiresTicket = !isFree || binding.switchRequiresTicket.isChecked

        // Create the event
        viewModel.createEvent(
            title = title,
            description = description,
            category = primaryCategory,
            startTimeMillis = eventStartDateTime.timeInMillis,
            endTimeMillis = eventEndDateTime?.timeInMillis,
            locationName = locationName,
            latitude = latitude,
            longitude = longitude,
            address = address,
            maxParticipants = maxParticipants,
            isFree = isFree,
            price = price,
            currency = currency,
            organizerId = userId,
            organizerName = userName,
            tags = extraTags,
            visibility = visibility,
            requiresTicket = requiresTicket,
            saveAsDraft = saveAsDraft
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
