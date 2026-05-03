package com.eventfinder.app.client.organizer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.eventfinder.app.databinding.FragmentManageEventOverviewBinding
import com.eventfinder.app.domain.model.EventStats
import com.eventfinder.app.domain.model.TicketStatus
import com.eventfinder.app.domain.model.TicketType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManageEventOverviewFragment : Fragment() {

    private var _binding: FragmentManageEventOverviewBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: ManageEventSharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageEventOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    sharedViewModel.attendees.collect { attendees ->
                        updateOverviewStats(attendees, sharedViewModel.eventStats.value)
                    }
                }

                launch {
                    sharedViewModel.eventStats.collect { stats ->
                        updateOverviewStats(sharedViewModel.attendees.value, stats)
                    }
                }
            }
        }
    }

    private fun updateOverviewStats(attendees: List<com.eventfinder.app.domain.model.Ticket>, stats: EventStats?) {
        if (attendees.isEmpty()) {
            // Keep placeholder strings for empty state
            return
        }

        // Calculate statistics
        val totalTickets = attendees.filter { it.status != TicketStatus.CANCELLED }.size
        val checkedInCount = attendees.count { it.status == TicketStatus.CHECKED_IN }
        val paidTickets = attendees.count {
            it.ticketType == TicketType.PAID && it.status != TicketStatus.CANCELLED
        }
        val freeTickets = attendees.count {
            (it.ticketType == TicketType.FREE_PRIVATE || it.ticketType == TicketType.PUBLIC_RESERVATION)
                && it.status != TicketStatus.CANCELLED
        }

        // Calculate revenue
        val totalRevenue = attendees
            .filter { it.status != TicketStatus.CANCELLED }
            .sumOf { it.purchasePrice }

        val currency = attendees.firstOrNull()?.currency ?: stats?.currency ?: "PKR"

        // Ticket type counts
        val publicTickets = attendees.count {
            it.ticketType == TicketType.PUBLIC_RESERVATION && it.status != TicketStatus.CANCELLED
        }
        val privateTickets = attendees.count {
            (it.ticketType == TicketType.FREE_PRIVATE || it.ticketType == TicketType.PAID)
                && it.status != TicketStatus.CANCELLED
        }

        // Update UI with real data
        binding.tvTicketsSold.text = totalTickets.toString()
        binding.tvAttendeesCount.text = checkedInCount.toString()
        binding.tvRevenue.text = "$currency ${String.format("%.2f", totalRevenue)}"

        // Calculate remaining (assuming max capacity of 200 for now)
        val maxCapacity = 200 // TODO: Get from Event model
        val remaining = maxCapacity - totalTickets
        binding.tvRemaining.text = remaining.toString()

        // Update ticket type counts
        binding.tvGeneralAdmissionTickets.text = "$publicTickets tickets"
        binding.tvVipTickets.text = "$privateTickets tickets"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
