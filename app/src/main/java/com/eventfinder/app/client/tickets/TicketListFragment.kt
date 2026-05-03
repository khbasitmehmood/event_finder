package com.eventfinder.app.client.tickets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.eventfinder.app.R
import com.eventfinder.app.databinding.ItemTicketCardBinding
import com.eventfinder.app.domain.model.Ticket
import com.eventfinder.app.domain.model.TicketStatus
import com.eventfinder.app.domain.model.TicketType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fragment to display a list of tickets
 * Used as a child fragment in ViewPager2
 */
class TicketListFragment : Fragment() {

    private var tickets: List<Ticket> = emptyList()
    private lateinit var adapter: TicketAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_ticket_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val emptyView = view.findViewById<View>(R.id.layoutEmpty)

        adapter = TicketAdapter { ticket ->
            // Navigate to ticket detail
            val bundle = bundleOf("TICKET_ID" to ticket.ticketId)
            findNavController().navigate(R.id.ticketDetailFragment, bundle)
        }

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        // Get tickets from arguments
        @Suppress("DEPRECATION")
        tickets = arguments?.getSerializable(ARG_TICKETS) as? List<Ticket> ?: emptyList()

        if (tickets.isEmpty()) {
            recyclerView.isVisible = false
            emptyView.isVisible = true
        } else {
            recyclerView.isVisible = true
            emptyView.isVisible = false
            adapter.submitList(tickets)
        }
    }

    companion object {
        private const val ARG_TICKETS = "tickets"

        fun newInstance(tickets: List<Ticket>): TicketListFragment {
            return TicketListFragment().apply {
                arguments = bundleOf(ARG_TICKETS to ArrayList(tickets))
            }
        }
    }
}

/**
 * RecyclerView Adapter for tickets
 */
class TicketAdapter(
    private val onTicketClick: (Ticket) -> Unit
) : RecyclerView.Adapter<TicketAdapter.TicketViewHolder>() {

    private var tickets: List<Ticket> = emptyList()

    fun submitList(newTickets: List<Ticket>) {
        tickets = newTickets
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val binding = ItemTicketCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TicketViewHolder(binding, onTicketClick)
    }

    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        holder.bind(tickets[position])
    }

    override fun getItemCount(): Int = tickets.size

    class TicketViewHolder(
        private val binding: ItemTicketCardBinding,
        private val onTicketClick: (Ticket) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(ticket: Ticket) {
            binding.root.setOnClickListener { onTicketClick(ticket) }

            // Event Image
            binding.ivEventImage.load(R.drawable.event_img) {
                crossfade(true)
                placeholder(R.drawable.event_img)
            }

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

            // Event Title
            binding.tvEventTitle.text = ticket.eventTitle

            // Event Date
            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            binding.tvEventDate.text = dateFormat.format(Date(ticket.eventStartTime))

            // Ticket Info
            val typeText = when (ticket.ticketType) {
                TicketType.PUBLIC_RESERVATION -> "Public Event"
                TicketType.FREE_PRIVATE -> "Free Ticket"
                TicketType.PAID -> "Paid Ticket"
            }

            binding.tvTicketInfo.text = if (ticket.purchasePrice > 0) {
                "$typeText • ${ticket.currency} ${ticket.purchasePrice}"
            } else {
                typeText
            }
        }
    }
}
