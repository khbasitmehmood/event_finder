package com.eventfinder.app.client.organizer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eventfinder.app.databinding.ItemAttendeeBinding
import com.eventfinder.app.databinding.ItemBookingGroupHeaderBinding
import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.model.Ticket
import com.eventfinder.app.domain.model.TicketStatus
import com.eventfinder.app.domain.model.TicketType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BookingGroupAdapter(
    private val onEventClick: (Event) -> Unit,
    private val onToggleExpand: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var bookingGroups: List<OrganizerBookingsViewModel.BookingGroup> = emptyList()

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_TICKET = 1
    }

    fun submitList(groups: List<OrganizerBookingsViewModel.BookingGroup>) {
        bookingGroups = groups
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return bookingGroups.sumOf { group ->
            1 + if (group.isExpanded) group.tickets.size else 0
        }
    }

    override fun getItemViewType(position: Int): Int {
        var currentPos = 0
        for (group in bookingGroups) {
            if (currentPos == position) {
                return VIEW_TYPE_HEADER
            }
            currentPos++

            if (group.isExpanded) {
                if (position < currentPos + group.tickets.size) {
                    return VIEW_TYPE_TICKET
                }
                currentPos += group.tickets.size
            }
        }
        return VIEW_TYPE_HEADER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val binding = ItemBookingGroupHeaderBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            HeaderViewHolder(binding)
        } else {
            val binding = ItemAttendeeBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            TicketViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                val group = getGroupAtPosition(position)
                if (group != null) {
                    holder.bind(group, onEventClick, onToggleExpand)
                }
            }
            is TicketViewHolder -> {
                val ticket = getTicketAtPosition(position)
                if (ticket != null) {
                    holder.bind(ticket)
                }
            }
        }
    }

    private fun getGroupAtPosition(position: Int): OrganizerBookingsViewModel.BookingGroup? {
        var currentPos = 0
        for (group in bookingGroups) {
            if (currentPos == position) {
                return group
            }
            currentPos += 1 + if (group.isExpanded) group.tickets.size else 0
        }
        return null
    }

    private fun getTicketAtPosition(position: Int): Ticket? {
        var currentPos = 0
        for (group in bookingGroups) {
            currentPos++ // Skip header
            if (group.isExpanded) {
                val ticketIndex = position - currentPos
                if (ticketIndex >= 0 && ticketIndex < group.tickets.size) {
                    return group.tickets[ticketIndex]
                }
                currentPos += group.tickets.size
            }
        }
        return null
    }

    class HeaderViewHolder(
        private val binding: ItemBookingGroupHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            group: OrganizerBookingsViewModel.BookingGroup,
            onEventClick: (Event) -> Unit,
            onToggleExpand: (String) -> Unit
        ) {
            binding.tvEventTitle.text = group.event.title
            binding.tvEventDate.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                .format(Date(group.event.startTime))

            binding.tvTotalCount.text = "${group.totalCount} bookings"
            binding.tvCheckedIn.text = "${group.checkInCount} checked in"

            binding.ivExpandIcon.rotation = if (group.isExpanded) 180f else 0f

            binding.root.setOnClickListener {
                onToggleExpand(group.event.eventId)
            }

            binding.btnViewEvent.setOnClickListener {
                onEventClick(group.event)
            }
        }
    }

    class TicketViewHolder(
        private val binding: ItemAttendeeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(ticket: Ticket) {
            binding.tvName.text = ticket.userName
            binding.tvInitials.text = getInitials(ticket.userName)

            val ticketTypeText = when (ticket.ticketType) {
                TicketType.PUBLIC_RESERVATION -> "Public Event"
                TicketType.FREE_PRIVATE -> "Free Ticket"
                TicketType.PAID -> "Paid Ticket"
            }
            binding.tvTicketInfo.text = ticketTypeText

            binding.tvBookingId.text = "ID: ${ticket.ticketId.take(8).uppercase()}"

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
            binding.tvStatus.text = statusText

            val statusColor = when (ticket.status) {
                TicketStatus.CHECKED_IN -> android.graphics.Color.parseColor("#4CAF50")
                TicketStatus.RESERVED, TicketStatus.PURCHASED -> android.graphics.Color.parseColor("#FFA26B")
                TicketStatus.CANCELLED, TicketStatus.EXPIRED -> android.graphics.Color.parseColor("#F44336")
            }
            binding.tvStatus.setTextColor(statusColor)
        }

        private fun getInitials(name: String): String {
            val parts = name.trim().split(" ")
            return when {
                parts.size >= 2 -> "${parts[0].firstOrNull() ?: ""}${parts[1].firstOrNull() ?: ""}".uppercase()
                parts.size == 1 -> parts[0].take(2).uppercase()
                else -> "NA"
            }
        }
    }
}
