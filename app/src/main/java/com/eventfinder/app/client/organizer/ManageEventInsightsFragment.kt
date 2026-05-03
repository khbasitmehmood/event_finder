package com.eventfinder.app.client.organizer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.eventfinder.app.databinding.FragmentManageEventInsightsBinding
import com.eventfinder.app.domain.model.EventStats
import com.eventfinder.app.domain.model.TicketStatus
import com.eventfinder.app.domain.model.TicketType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManageEventInsightsFragment : Fragment() {

    private var _binding: FragmentManageEventInsightsBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: ManageEventSharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageEventInsightsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSwipeRefresh()
        observeViewModel()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            sharedViewModel.refreshData()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    sharedViewModel.attendees.collect { attendees ->
                        updateStatistics(attendees, sharedViewModel.eventStats.value)
                    }
                }

                launch {
                    sharedViewModel.eventStats.collect { stats ->
                        updateStatistics(sharedViewModel.attendees.value, stats)
                    }
                }

                launch {
                    sharedViewModel.isLoading.collect { isLoading ->
                        binding.swipeRefresh.isRefreshing = isLoading
                    }
                }

                launch {
                    sharedViewModel.error.collect { error ->
                        if (error != null) {
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            sharedViewModel.clearError()
                        }
                    }
                }
            }
        }
    }

    private fun updateStatistics(attendees: List<com.eventfinder.app.domain.model.Ticket>, stats: EventStats?) {
        // Show empty state if no data
        val isEmpty = attendees.isEmpty()
        binding.layoutEmpty.isVisible = isEmpty

        if (isEmpty) {
            return
        }

        // Calculate ticket counts by type and status
        val totalTickets = attendees.filter { it.status != TicketStatus.CANCELLED }.size
        val checkedInCount = attendees.count { it.status == TicketStatus.CHECKED_IN }
        val paidTickets = attendees.count {
            it.ticketType == TicketType.PAID && it.status != TicketStatus.CANCELLED
        }
        val freeTickets = attendees.count {
            (it.ticketType == TicketType.FREE_PRIVATE || it.ticketType == TicketType.PUBLIC_RESERVATION)
                && it.status != TicketStatus.CANCELLED
        }
        val pendingTickets = attendees.count {
            it.status == TicketStatus.RESERVED
        }

        // Calculate revenue
        val totalRevenue = attendees
            .filter { it.status != TicketStatus.CANCELLED }
            .sumOf { it.purchasePrice }
        val paidRevenue = attendees
            .filter { it.ticketType == TicketType.PAID && it.status != TicketStatus.CANCELLED }
            .sumOf { it.purchasePrice }
        val pendingRevenue = attendees
            .filter { it.status == TicketStatus.RESERVED && it.ticketType == TicketType.PAID }
            .sumOf { it.purchasePrice }

        val currency = attendees.firstOrNull()?.currency ?: stats?.currency ?: "PKR"

        // Update top summary stats
        binding.tvTotalBookings.text = totalTickets.toString()
        binding.tvPaidBookings.text = paidTickets.toString()
        binding.tvFreeBookings.text = freeTickets.toString()
        binding.tvPendingBookings.text = pendingTickets.toString()

        // Update capacity (assuming max capacity is in event stats or default to 200)
        val maxCapacity = 200 // TODO: Get this from Event model
        val capacityPercentage = if (maxCapacity > 0) {
            ((totalTickets.toFloat() / maxCapacity) * 100).toInt()
        } else 0

        binding.tvCapacityFraction.text = "$totalTickets / $maxCapacity"
        binding.progressCapacity.progress = capacityPercentage
        binding.tvTicketsRemaining.text = "${ maxCapacity - totalTickets} tickets remaining"

        // Update revenue
        binding.tvTotalRevenue.text = "$currency ${String.format("%.2f", totalRevenue)}"
        binding.tvPaidRevenue.text = "$currency ${String.format("%.2f", paidRevenue)}"
        binding.tvPendingRevenue.text = "$currency ${String.format("%.2f", pendingRevenue)}"

        // Update ticket type split
        val publicTickets = attendees.count {
            it.ticketType == TicketType.PUBLIC_RESERVATION && it.status != TicketStatus.CANCELLED
        }
        val privateTickets = attendees.count {
            (it.ticketType == TicketType.FREE_PRIVATE || it.ticketType == TicketType.PAID)
                && it.status != TicketStatus.CANCELLED
        }

        binding.tvGeneralAdmission.text = "$publicTickets tickets sold"
        val publicPercentage = if (totalTickets > 0) {
            ((publicTickets.toFloat() / totalTickets) * 100).toInt()
        } else 0
        binding.progressGeneralAdmission.progress = publicPercentage

        binding.tvVip.text = "$privateTickets tickets sold"
        val privatePercentage = if (totalTickets > 0) {
            ((privateTickets.toFloat() / totalTickets) * 100).toInt()
        } else 0
        binding.progressVip.progress = privatePercentage

        // Update checked-in stats
        binding.tvCheckedIn.text = "$checkedInCount checked in"
        binding.tvCheckedInPercentage.text = if (totalTickets > 0) {
            "${((checkedInCount.toFloat() / totalTickets) * 100).toInt()}%"
        } else "0%"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
