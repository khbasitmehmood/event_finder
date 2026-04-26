package com.eventfinder.app.client.createevent

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.eventfinder.app.R
import com.eventfinder.app.databinding.FragmentCreateEventNewBinding
import com.eventfinder.app.domain.model.EventCategory
import com.eventfinder.app.utils.UserPreferences
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

/**
 * Modern Material 3 Create Event Fragment
 */
@AndroidEntryPoint
class CreateEventFragment : Fragment(R.layout.fragment_create_event_new) {

    private var _binding: FragmentCreateEventNewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreateEventViewModel by viewModels()

    @Inject
    lateinit var userPreferences: UserPreferences

    private var selectedDate: Calendar? = null
    private var selectedTime: Calendar? = null

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateEventNewBinding.bind(view)

        setupToolbar()
        setupCategoryDropdown()
        setupDateTimePickers()
        setupPricingToggle()
        setupImageUpload()
        setupActionButtons()
        observeViewModel()
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

    private fun setupCategoryDropdown() {
        val categories = EventCategory.values().map { it.name.capitalize() }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categories
        )
        binding.actvCategory.setAdapter(adapter)
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
                        resources.getColor(R.color.md_primary, null)
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

        // Time Picker
        binding.cardSelectTime.setOnClickListener {
            val calendar = selectedTime ?: Calendar.getInstance()

            TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    selectedTime = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hourOfDay)
                        set(Calendar.MINUTE, minute)
                    }
                    binding.tvSelectedTime.text = timeFormat.format(selectedTime!!.time)
                    binding.tvSelectedTime.setTextColor(
                        resources.getColor(R.color.md_primary, null)
                    )
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false // 12-hour format
            ).show()
        }
    }

    private fun setupPricingToggle() {
        binding.switchFreeEvent.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutPrice.isVisible = !isChecked
            if (isChecked) {
                binding.etTicketPrice.text?.clear()
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
        // Save Draft
        binding.btnSaveDraft.setOnClickListener {
            if (validateBasicInfo()) {
                val title = binding.etEventTitle.text?.toString()?.trim() ?: ""
                val description = binding.etDescription.text?.toString()?.trim()
                val category = binding.actvCategory.text?.toString()?.trim()

                viewModel.saveDraft(title, description, category)
            }
        }

        // Publish Event
        binding.btnPublish.setOnClickListener {
            if (validateAllFields()) {
                publishEvent()
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
        val category = binding.actvCategory.text?.toString()?.trim()
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
            category.isNullOrEmpty() -> {
                binding.actvCategory.error = "Category is required"
                binding.actvCategory.requestFocus()
                return false
            }
            selectedDate == null -> {
                Toast.makeText(context, "Please select a date", Toast.LENGTH_SHORT).show()
                return false
            }
            selectedTime == null -> {
                Toast.makeText(context, "Please select a time", Toast.LENGTH_SHORT).show()
                return false
            }
            location.isNullOrEmpty() -> {
                binding.etLocation.error = "Location is required"
                binding.etLocation.requestFocus()
                return false
            }
        }

        return true
    }

    private fun publishEvent() {
        // Combine date and time
        val eventDateTime = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedDate!!.get(Calendar.YEAR))
            set(Calendar.MONTH, selectedDate!!.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, selectedDate!!.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, selectedTime!!.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, selectedTime!!.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
        }

        // Get all form data
        val title = binding.etEventTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val categoryString = binding.actvCategory.text.toString().trim().uppercase()
        val category = try {
            EventCategory.valueOf(categoryString)
        } catch (e: Exception) {
            EventCategory.OTHER
        }

        val locationName = binding.etLocation.text.toString().trim()
        val address = locationName // Use location name as address for now

        val maxParticipants = binding.etMaxAttendees.text?.toString()?.toIntOrNull()
        val isFree = binding.switchFreeEvent.isChecked
        val price = if (!isFree) binding.etTicketPrice.text?.toString()?.toDoubleOrNull() else null
        val currency = if (!isFree) "PKR" else null

        // For now, use dummy coordinates (Lahore city center)
        // TODO: Implement proper location picker with geocoding
        val latitude = 31.5497
        val longitude = 74.3436

        // Get user ID and name from preferences
        val userId = userPreferences.getUserId()
        val userName = userPreferences.getUserName()

        // Create the event
        viewModel.createEvent(
            title = title,
            description = description,
            category = category,
            startTimeMillis = eventDateTime.timeInMillis,
            endTimeMillis = null, // TODO: Add end time picker
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
            tags = emptyList() // TODO: Add tags input
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun String.capitalize(): String {
        return this.lowercase()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
}
