package com.eventfinder.app.client.organizer

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import com.eventfinder.app.R
import com.eventfinder.app.databinding.DialogCancelEventBinding
import com.eventfinder.app.domain.model.Event
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Dialog for cancelling an event
 * Shows impact summary and requires cancellation reason
 */
class CancelEventDialog : DialogFragment() {

    private var _binding: DialogCancelEventBinding? = null
    private val binding get() = _binding!!

    private var event: Event? = null
    private var onCancelConfirmed: ((reason: String) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCancelEventBinding.inflate(layoutInflater)

        event?.let { bindEventImpact(it) }
        setupReasonInput()
        setupButtons()

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    private fun bindEventImpact(event: Event) {
        val impactLines = mutableListOf<String>()

        // Attendee count
        if (event.currentParticipantCount > 0) {
            impactLines.add("• ${event.currentParticipantCount} attendee(s) will be notified")
        } else {
            impactLines.add("• No attendees registered yet")
        }

        // Refund information
        if (!event.isFree && event.currentParticipantCount > 0) {
            val refundPerTicket = event.price ?: 0.0
            impactLines.add("• Refunds of ${event.currency} ${String.format("%.2f", refundPerTicket)} per ticket will be initiated")

            val totalRefund = refundPerTicket * event.currentParticipantCount
            impactLines.add("• Total refund amount: ${event.currency} ${String.format("%.2f", totalRefund)}")
        } else if (event.isFree) {
            impactLines.add("• This is a free event, no refunds needed")
        }

        binding.tvImpactSummary.text = impactLines.joinToString("\n")
    }

    private fun setupReasonInput() {
        binding.etReason.doAfterTextChanged {
            val length = it?.length ?: 0
            if (length > 0 && length < 10) {
                binding.reasonLayout.error = "Reason must be at least 10 characters"
            } else {
                binding.reasonLayout.error = null
            }
            updateCancelButtonState()
        }
    }

    private fun setupButtons() {
        binding.btnKeepEvent.setOnClickListener {
            dismiss()
        }

        binding.btnCancelEvent.setOnClickListener {
            val reason = binding.etReason.text.toString().trim()

            if (!validateInput(reason)) {
                return@setOnClickListener
            }

            onCancelConfirmed?.invoke(reason)
            dismiss()
        }
    }

    private fun validateInput(reason: String): Boolean {
        var isValid = true

        if (reason.length < 10) {
            binding.reasonLayout.error = "Reason must be at least 10 characters"
            isValid = false
        } else if (reason.length > 500) {
            binding.reasonLayout.error = "Reason must not exceed 500 characters"
            isValid = false
        }

        return isValid
    }

    private fun updateCancelButtonState() {
        val reason = binding.etReason.text.toString().trim()
        val isReasonValid = reason.length >= 10 && reason.length <= 500

        binding.btnCancelEvent.isEnabled = isReasonValid
    }

    fun setEvent(event: Event) {
        this.event = event
    }

    fun setOnCancelConfirmedListener(listener: (reason: String) -> Unit) {
        onCancelConfirmed = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "CancelEventDialog"

        fun newInstance(
            event: Event,
            onCancelConfirmed: (reason: String) -> Unit
        ): CancelEventDialog {
            return CancelEventDialog().apply {
                setEvent(event)
                setOnCancelConfirmedListener(onCancelConfirmed)
            }
        }
    }
}
