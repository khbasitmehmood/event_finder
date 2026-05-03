package com.eventfinder.app.client.tickets

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.eventfinder.app.R
import com.eventfinder.app.databinding.FragmentTicketDetailBinding
import com.eventfinder.app.domain.model.Ticket
import com.eventfinder.app.domain.model.TicketStatus
import com.eventfinder.app.domain.model.TicketType
import com.eventfinder.app.utils.QRCodeGenerator
import com.eventfinder.app.utils.UserPreferences
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class TicketDetailFragment : Fragment(R.layout.fragment_ticket_detail) {

    private var _binding: FragmentTicketDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TicketDetailViewModel by viewModels()

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTicketDetailBinding.bind(view)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnCancelTicket.setOnClickListener {
            showCancelConfirmation()
        }

        val ticketId = arguments?.getString("TICKET_ID")
        if (ticketId != null) {
            viewModel.loadTicket(ticketId)
        } else {
            Toast.makeText(context, "Ticket not found", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.isLoading) {
                        // Show loading state
                    } else if (state.error != null) {
                        Toast.makeText(context, state.error, Toast.LENGTH_SHORT).show()
                    } else if (state.ticket != null) {
                        bindTicketData(state.ticket)
                    }

                    // Handle cancel success
                    if (state.cancelSuccess) {
                        Toast.makeText(context, "Ticket cancelled successfully", Toast.LENGTH_SHORT).show()
                        viewModel.resetCancelSuccess()
                    }

                    // Update button state
                    binding.btnCancelTicket.isEnabled = !state.isCancelling
                    if (state.isCancelling) {
                        binding.btnCancelTicket.text = "Cancelling..."
                    } else {
                        binding.btnCancelTicket.text = "Cancel Ticket"
                    }
                }
            }
        }
    }

    private fun bindTicketData(ticket: Ticket) {
        // Status Chip
        binding.chipStatus.text = ticket.status.name
        binding.chipStatus.setChipBackgroundColorResource(
            when (ticket.status) {
                TicketStatus.PURCHASED, TicketStatus.RESERVED -> R.color.md_tertiary_container
                TicketStatus.CHECKED_IN -> R.color.md_secondary_container
                TicketStatus.CANCELLED -> R.color.md_error_container
                TicketStatus.EXPIRED -> R.color.md_surface_variant
            }
        )

        // Generate and display QR Code
        val qrBitmap = QRCodeGenerator.generateQRCode(ticket.qrCodeData, 512, 512)
        if (qrBitmap != null) {
            binding.ivQRCode.setImageBitmap(qrBitmap)
        } else {
            binding.ivQRCode.setImageResource(R.drawable.ic_profile) // Fallback
        }

        // Ticket ID
        binding.tvTicketId.text = "Ticket ID: ${ticket.ticketId.take(8).uppercase()}"

        // Event Details
        binding.tvEventTitle.text = ticket.eventTitle

        val dateFormat = SimpleDateFormat("EEEE, dd MMM yyyy - hh:mm a", Locale.getDefault())
        binding.tvEventDate.text = dateFormat.format(Date(ticket.eventStartTime))

        binding.tvEventLocation.text = ticket.eventLocation ?: "Location TBD"

        // Ticket Info
        binding.tvUserName.text = ticket.userName

        binding.tvTicketType.text = when (ticket.ticketType) {
            TicketType.PUBLIC_RESERVATION -> "Public Event Reservation"
            TicketType.FREE_PRIVATE -> "Free Ticket"
            TicketType.PAID -> "Paid Ticket"
        }

        // Price
        if (ticket.purchasePrice > 0) {
            binding.layoutPrice.isVisible = true
            binding.tvPrice.text = "${ticket.currency} ${ticket.purchasePrice}"
        } else {
            binding.layoutPrice.isVisible = false
        }

        // Purchase Date
        val purchaseDateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        binding.tvPurchaseDate.text = purchaseDateFormat.format(Date(ticket.purchasedAt))

        // Check-in Info
        if (ticket.status == TicketStatus.CHECKED_IN && ticket.checkedInAt != null) {
            binding.cardCheckInInfo.isVisible = true
            binding.tvCheckInTime.text = "Checked in on ${purchaseDateFormat.format(Date(ticket.checkedInAt))}"
        } else {
            binding.cardCheckInInfo.isVisible = false
        }

        // Cancel button visibility
        binding.btnCancelTicket.isVisible = ticket.status != TicketStatus.CANCELLED &&
                ticket.status != TicketStatus.CHECKED_IN &&
                ticket.status != TicketStatus.EXPIRED
    }

    private fun showCancelConfirmation() {
        val ticket = viewModel.uiState.value.ticket ?: return

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cancel Ticket")
            .setMessage("Are you sure you want to cancel your ticket for \"${ticket.eventTitle}\"? This action cannot be undone.")
            .setPositiveButton("Yes, Cancel") { _, _ ->
                val userId = userPreferences.getUserId()
                viewModel.cancelTicket(ticket.ticketId, userId)
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
