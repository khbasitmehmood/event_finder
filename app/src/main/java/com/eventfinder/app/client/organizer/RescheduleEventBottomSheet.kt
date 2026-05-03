package com.eventfinder.app.client.organizer

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.eventfinder.app.databinding.BottomSheetRescheduleEventBinding
import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.model.EventLocation
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Bottom sheet for rescheduling an event
 * Allows organizer to change date, time, and optionally location
 */
class RescheduleEventBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetRescheduleEventBinding? = null
    private val binding get() = _binding!!

    private var event: Event? = null
    private var newStartTime: Long? = null
    private var newEndTime: Long? = null
    private var newLocation: EventLocation? = null
    private var newAddress: String? = null

    private var onRescheduleClick: ((newStartTime: Long, newEndTime: Long?, newLocation: EventLocation?, newAddress: String?, reason: String) -> Unit)? = null

    private val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetRescheduleEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        event?.let { bindCurrentDetails(it) }

        setupDatePickers()
        setupLocationToggle()
        setupReasonInput()
        setupButtons()
    }

    private fun bindCurrentDetails(event: Event) {
        // Show current date/time
        val startDate = dateTimeFormat.format(event.startTime)
        val endDate = event.endTime?.let { dateTimeFormat.format(it) } ?: "Not set"
        binding.tvCurrentDateTime.text = "$startDate - $endDate"

        // Show current location
        binding.tvCurrentLocation.text = event.address ?: "Location not set"
    }

    private fun setupDatePickers() {
        // Start Date Picker
        binding.etStartDate.setOnClickListener {
            showDateTimePicker { timestamp ->
                newStartTime = timestamp
                binding.etStartDate.setText(dateTimeFormat.format(timestamp))
                binding.startDateLayout.error = null
                updateChangesSummary()
                updateRescheduleButtonState()
            }
        }

        binding.startDateLayout.setEndIconOnClickListener {
            binding.etStartDate.performClick()
        }

        // End Date Picker
        binding.etEndDate.setOnClickListener {
            showDateTimePicker { timestamp ->
                // Validate end time is after start time
                if (newStartTime != null && timestamp < newStartTime!!) {
                    binding.endDateLayout.error = "End time must be after start time"
                } else {
                    newEndTime = timestamp
                    binding.etEndDate.setText(dateTimeFormat.format(timestamp))
                    binding.endDateLayout.error = null
                    updateChangesSummary()
                    updateRescheduleButtonState()
                }
            }
        }

        binding.endDateLayout.setEndIconOnClickListener {
            binding.etEndDate.performClick()
        }
    }

    private fun setupLocationToggle() {
        binding.switchChangeLocation.setOnCheckedChangeListener { _, isChecked ->
            binding.addressLayout.isVisible = isChecked
            if (!isChecked) {
                newLocation = null
                newAddress = null
                binding.etAddress.setText("")
            }
            updateChangesSummary()
            updateRescheduleButtonState()
        }

        binding.etAddress.doAfterTextChanged {
            val address = it?.toString()?.trim()
            if (binding.switchChangeLocation.isChecked && !address.isNullOrBlank()) {
                newAddress = address
                // For now, keep the same location coordinates
                // In a real app, you'd use a map picker here
                newLocation = event?.location
                updateChangesSummary()
            }
        }
    }

    private fun setupReasonInput() {
        binding.etReason.doAfterTextChanged {
            val length = it?.length ?: 0
            if (length > 0 && length < 10) {
                binding.reasonLayout.error = "Reason must be at least 10 characters"
            } else {
                binding.reasonLayout.error = null
            }
            updateRescheduleButtonState()
        }
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnReschedule.setOnClickListener {
            val reason = binding.etReason.text.toString().trim()

            if (!validateInput(reason)) {
                return@setOnClickListener
            }

            onRescheduleClick?.invoke(
                newStartTime!!,
                newEndTime,
                newLocation,
                newAddress,
                reason
            )
            dismiss()
        }
    }

    private fun showDateTimePicker(onDateTimeSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()

        // Date Picker
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)

                // Time Picker
                TimePickerDialog(
                    requireContext(),
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        onDateTimeSelected(calendar.timeInMillis)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set minimum date to tomorrow
        datePickerDialog.datePicker.minDate = System.currentTimeMillis() + (24 * 60 * 60 * 1000)
        datePickerDialog.show()
    }

    private fun validateInput(reason: String): Boolean {
        var isValid = true

        // Validate reason
        if (reason.length < 10) {
            binding.reasonLayout.error = "Reason must be at least 10 characters"
            isValid = false
        } else if (reason.length > 500) {
            binding.reasonLayout.error = "Reason must not exceed 500 characters"
            isValid = false
        }

        // Validate new start time is set
        if (newStartTime == null) {
            binding.startDateLayout.error = "Please select a new start date"
            isValid = false
        }

        // Validate new date is in the future
        if (newStartTime != null) {
            val minTime = System.currentTimeMillis() + (60 * 60 * 1000) // 1 hour from now
            if (newStartTime!! < minTime) {
                binding.startDateLayout.error = "New date must be at least 1 hour from now"
                isValid = false
            }
        }

        // Validate end time is after start time
        if (newEndTime != null && newStartTime != null && newEndTime!! < newStartTime!!) {
            binding.endDateLayout.error = "End time must be after start time"
            isValid = false
        }

        // Check if anything actually changed
        val currentEvent = event
        if (currentEvent != null) {
            val hasChanges = newStartTime != currentEvent.startTime ||
                    newEndTime != currentEvent.endTime ||
                    (binding.switchChangeLocation.isChecked && newAddress != null)

            if (!hasChanges) {
                binding.startDateLayout.error = "No changes detected"
                isValid = false
            }
        }

        return isValid
    }

    private fun updateChangesSummary() {
        val changes = mutableListOf<String>()

        val currentEvent = event ?: return

        if (newStartTime != null && newStartTime != currentEvent.startTime) {
            changes.add("Start time changed")
        }

        if (newEndTime != null && newEndTime != currentEvent.endTime) {
            changes.add("End time changed")
        }

        if (binding.switchChangeLocation.isChecked && newAddress != null) {
            changes.add("Location/Address changed")
        }

        if (changes.isNotEmpty()) {
            binding.cardChangesSummary.isVisible = true
            binding.tvChangesSummary.text = changes.joinToString("\n• ", "• ")
        } else {
            binding.cardChangesSummary.isVisible = false
        }
    }

    private fun updateRescheduleButtonState() {
        val reason = binding.etReason.text.toString().trim()
        val isReasonValid = reason.length >= 10 && reason.length <= 500
        val hasNewStartTime = newStartTime != null

        val currentEvent = event
        val hasChanges = if (currentEvent != null) {
            newStartTime != currentEvent.startTime ||
                    newEndTime != currentEvent.endTime ||
                    (binding.switchChangeLocation.isChecked && newAddress != null)
        } else {
            false
        }

        binding.btnReschedule.isEnabled = isReasonValid && hasNewStartTime && hasChanges
    }

    fun setEvent(event: Event) {
        this.event = event
    }

    fun setOnRescheduleClickListener(listener: (newStartTime: Long, newEndTime: Long?, newLocation: EventLocation?, newAddress: String?, reason: String) -> Unit) {
        onRescheduleClick = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "RescheduleEventBottomSheet"

        fun newInstance(
            event: Event,
            onRescheduleClick: (newStartTime: Long, newEndTime: Long?, newLocation: EventLocation?, newAddress: String?, reason: String) -> Unit
        ): RescheduleEventBottomSheet {
            return RescheduleEventBottomSheet().apply {
                setEvent(event)
                setOnRescheduleClickListener(onRescheduleClick)
            }
        }
    }
}
