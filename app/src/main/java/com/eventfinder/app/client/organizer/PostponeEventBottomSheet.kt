package com.eventfinder.app.client.organizer

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import com.eventfinder.app.R
import com.eventfinder.app.databinding.BottomSheetPostponeEventBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Bottom sheet for postponing an event.
 * Allows organizer to set a new date/time or mark as TBD.
 */
class PostponeEventBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetPostponeEventBinding? = null
    private val binding get() = _binding!!

    private var newStartTime: Long? = null
    private var newEndTime: Long? = null
    private var isTBD: Boolean = true

    private var onPostponeClick: ((newStartTime: Long?, newEndTime: Long?, reason: String) -> Unit)? = null

    private val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetPostponeEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDateTypeToggle()
        setupDatePickers()
        setupReasonInput()
        setupButtons()
    }

    private fun setupDateTypeToggle() {
        binding.dateTypeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            when (checkedId) {
                R.id.btnSpecificDate -> {
                    isTBD = false
                    binding.datePickerContainer.visibility = View.VISIBLE
                    updatePostponeButtonState()
                }
                R.id.btnTBD -> {
                    isTBD = true
                    binding.datePickerContainer.visibility = View.GONE
                    newStartTime = null
                    newEndTime = null
                    binding.etStartDate.setText("")
                    binding.etEndDate.setText("")
                    updatePostponeButtonState()
                }
            }
        }
    }

    private fun setupDatePickers() {
        // Start Date Picker
        binding.etStartDate.setOnClickListener {
            showDateTimePicker { timestamp ->
                newStartTime = timestamp
                binding.etStartDate.setText(dateTimeFormat.format(timestamp))
                binding.startDateLayout.error = null
                updatePostponeButtonState()
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
                    updatePostponeButtonState()
                }
            }
        }

        binding.endDateLayout.setEndIconOnClickListener {
            binding.etEndDate.performClick()
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
            updatePostponeButtonState()
        }
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnPostpone.setOnClickListener {
            val reason = binding.etReason.text.toString().trim()

            if (!validateInput(reason)) {
                return@setOnClickListener
            }

            onPostponeClick?.invoke(newStartTime, newEndTime, reason)
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

        // Validate date if not TBD
        if (!isTBD && newStartTime == null) {
            binding.startDateLayout.error = "Please select a new start date"
            isValid = false
        }

        // Validate new date is in the future
        if (!isTBD && newStartTime != null) {
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

        return isValid
    }

    private fun updatePostponeButtonState() {
        val reason = binding.etReason.text.toString().trim()
        val isReasonValid = reason.length >= 10 && reason.length <= 500

        val isDateValid = if (isTBD) {
            true // TBD doesn't require date
        } else {
            newStartTime != null
        }

        binding.btnPostpone.isEnabled = isReasonValid && isDateValid
    }

    fun setOnPostponeClickListener(listener: (newStartTime: Long?, newEndTime: Long?, reason: String) -> Unit) {
        onPostponeClick = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "PostponeEventBottomSheet"

        fun newInstance(
            onPostponeClick: (newStartTime: Long?, newEndTime: Long?, reason: String) -> Unit
        ): PostponeEventBottomSheet {
            return PostponeEventBottomSheet().apply {
                setOnPostponeClickListener(onPostponeClick)
            }
        }
    }
}
