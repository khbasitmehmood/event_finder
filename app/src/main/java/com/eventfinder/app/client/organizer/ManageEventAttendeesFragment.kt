package com.eventfinder.app.client.organizer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eventfinder.app.R
import com.eventfinder.app.databinding.FragmentManageEventAttendeesBinding
import com.eventfinder.app.databinding.ItemAttendeeBinding
import com.eventfinder.app.domain.model.Ticket
import com.eventfinder.app.domain.model.TicketStatus
import com.eventfinder.app.domain.model.TicketType
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class ManageEventAttendeesFragment : Fragment() {

    private var _binding: FragmentManageEventAttendeesBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: ManageEventSharedViewModel by activityViewModels()
    private lateinit var attendeeAdapter: AttendeeAdapter

    private var allAttendees: List<Ticket> = emptyList()
    private var currentFilter: FilterType = FilterType.ALL
    private var searchQuery: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageEventAttendeesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFilters()
        setupSearch()
        setupSwipeRefresh()
        setupErrorState()
        observeViewModel()

        loadEventId()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            sharedViewModel.refreshData()
        }
    }

    private fun setupErrorState() {
        binding.errorState.btnRetry.setOnClickListener {
            sharedViewModel.refreshData()
        }
    }

    private fun loadEventId() {
        val eventId = arguments?.getString("EVENT_ID")

        if (eventId == null) {
            Toast.makeText(context, "Event not found", Toast.LENGTH_SHORT).show()
        }
        // Note: sharedViewModel.loadEventData() is already called in ManageEventFragment
        // The shared ViewModel ensures data is loaded once and shared across all tabs
    }

    private fun setupRecyclerView() {
        attendeeAdapter = AttendeeAdapter(emptyList())
        binding.rvAttendees.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = attendeeAdapter
        }
    }

    private fun setupFilters() {
        val chipGroup = binding.root.findViewById<ViewGroup>(R.id.chipGroup) ?: return

        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip ?: continue
            chip.setOnClickListener {
                // Reset all chips
                for (j in 0 until chipGroup.childCount) {
                    val otherChip = chipGroup.getChildAt(j) as? Chip
                    otherChip?.isChecked = false
                }
                chip.isChecked = true

                currentFilter = when (chip.text.toString().lowercase()) {
                    "all" -> FilterType.ALL
                    "paid" -> FilterType.PAID
                    "pending", "reserved" -> FilterType.PENDING
                    "free" -> FilterType.FREE
                    "checked in" -> FilterType.CHECKED_IN
                    else -> FilterType.ALL
                }
                applyFiltersAndSearch()
            }
        }
    }

    private fun setupSearch() {
        val searchInput = binding.root.findViewById<View>(R.id.searchInput)
        if (searchInput is com.google.android.material.textfield.TextInputEditText) {
            searchInput.addTextChangedListener { text ->
                searchQuery = text?.toString()?.lowercase() ?: ""
                applyFiltersAndSearch()
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    sharedViewModel.attendees.collect { attendees ->
                        allAttendees = attendees
                        applyFiltersAndSearch()
                    }
                }

                launch {
                    sharedViewModel.isLoading.collect { isLoading ->
                        binding.swipeRefresh.isRefreshing = isLoading
                        binding.progressLoading.isVisible = isLoading && allAttendees.isEmpty()
                    }
                }

                launch {
                    sharedViewModel.error.collect { error ->
                        if (error != null) {
                            // Show error state only if no cached data
                            if (allAttendees.isEmpty()) {
                                binding.errorState.root.isVisible = true
                                binding.errorState.tvErrorMessage.text = error
                                binding.rvAttendees.isVisible = false
                                binding.layoutEmpty.isVisible = false
                            } else {
                                // Show toast if we have cached data
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                            sharedViewModel.clearError()
                        } else {
                            binding.errorState.root.isVisible = false
                        }
                    }
                }
            }
        }
    }

    private fun applyFiltersAndSearch() {
        var filtered = allAttendees

        // Apply status filter
        filtered = when (currentFilter) {
            FilterType.ALL -> filtered
            FilterType.PAID -> filtered.filter {
                it.ticketType == TicketType.PAID && it.status != TicketStatus.CANCELLED
            }
            FilterType.FREE -> filtered.filter {
                (it.ticketType == TicketType.FREE_PRIVATE || it.ticketType == TicketType.PUBLIC_RESERVATION)
                    && it.status != TicketStatus.CANCELLED
            }
            FilterType.PENDING -> filtered.filter {
                it.status == TicketStatus.RESERVED && it.status != TicketStatus.CANCELLED
            }
            FilterType.CHECKED_IN -> filtered.filter {
                it.status == TicketStatus.CHECKED_IN
            }
        }

        // Apply search query
        if (searchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.userName.lowercase().contains(searchQuery) ||
                it.userEmail.lowercase().contains(searchQuery) ||
                it.ticketId.lowercase().contains(searchQuery)
            }
        }

        attendeeAdapter.updateAttendees(filtered)

        // Show/hide empty state
        val isEmpty = filtered.isEmpty()
        binding.rvAttendees.isVisible = !isEmpty
        binding.layoutEmpty.isVisible = isEmpty && !sharedViewModel.isLoading.value

        // Update empty state message based on context
        if (isEmpty) {
            if (searchQuery.isNotEmpty() || currentFilter != FilterType.ALL) {
                binding.tvEmptyTitle.text = "No Results Found"
                binding.tvEmptyMessage.text = "Try adjusting your search or filters"
            } else {
                binding.tvEmptyTitle.text = "No Attendees Yet"
                binding.tvEmptyMessage.text = "Attendees will appear here when\npeople purchase tickets"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    enum class FilterType {
        ALL, PAID, FREE, PENDING, CHECKED_IN
    }
}

class AttendeeAdapter(private var attendees: List<Ticket>) :
    RecyclerView.Adapter<AttendeeAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemAttendeeBinding) : RecyclerView.ViewHolder(binding.root)

    fun updateAttendees(newAttendees: List<Ticket>) {
        attendees = newAttendees
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAttendeeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ticket = attendees[position]
        val context = holder.itemView.context

        holder.binding.tvName.text = ticket.userName
        holder.binding.tvInitials.text = getInitials(ticket.userName)

        // Ticket info
        val ticketTypeText = when (ticket.ticketType) {
            TicketType.PUBLIC_RESERVATION -> "Public Event"
            TicketType.FREE_PRIVATE -> "Free Ticket"
            TicketType.PAID -> "Paid - ${ticket.currency} ${ticket.purchasePrice}"
        }
        holder.binding.tvTicketInfo.text = ticketTypeText

        // Booking ID
        holder.binding.tvBookingId.text = "ID: ${ticket.ticketId.take(8).uppercase()}"

        // Status
        val statusText = when (ticket.status) {
            TicketStatus.RESERVED -> "Reserved"
            TicketStatus.PURCHASED -> "Purchased"
            TicketStatus.CHECKED_IN -> {
                if (ticket.checkedInAt != null) {
                    val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
                    "Checked In - ${sdf.format(Date(ticket.checkedInAt))}"
                } else {
                    "Checked In"
                }
            }
            TicketStatus.CANCELLED -> "Cancelled"
            TicketStatus.EXPIRED -> "Expired"
        }
        holder.binding.tvStatus.text = statusText

        // Status color
        val statusColor = when (ticket.status) {
            TicketStatus.CHECKED_IN -> android.graphics.Color.parseColor("#4CAF50") // Green
            TicketStatus.RESERVED, TicketStatus.PURCHASED -> android.graphics.Color.parseColor("#FFA26B") // Orange
            TicketStatus.CANCELLED, TicketStatus.EXPIRED -> android.graphics.Color.parseColor("#F44336") // Red
        }
        holder.binding.tvStatus.setTextColor(statusColor)
    }

    override fun getItemCount() = attendees.size

    private fun getInitials(name: String): String {
        val parts = name.trim().split(" ")
        return when {
            parts.size >= 2 -> "${parts[0].firstOrNull() ?: ""}${parts[1].firstOrNull() ?: ""}".uppercase()
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> "NA"
        }
    }
}
