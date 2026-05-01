package com.eventfinder.app.client.organizer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eventfinder.app.databinding.FragmentManageEventAttendeesBinding
import com.eventfinder.app.databinding.ItemAttendeeBinding

class ManageEventAttendeesFragment : Fragment() {

    private var _binding: FragmentManageEventAttendeesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageEventAttendeesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.rvAttendees.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAttendees.adapter = AttendeeAdapter(getDummyAttendees())
    }

    private fun getDummyAttendees() = listOf(
        Attendee("Jane Smith", "JS", "2 x General Admission", "Booking ID: BK-1001", "Paid"),
        Attendee("Michael Chen", "MC", "1 x VIP", "Booking ID: BK-1002", "Paid"),
        Attendee("Aisha Rahman", "AR", "2 x General Admission", "Booking ID: BK-1003", "Pending"),
        Attendee("David Lee", "DL", "1 x General Admission", "Booking ID: BK-1004", "Paid")
    )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class Attendee(
    val name: String,
    val initials: String,
    val ticketInfo: String,
    val bookingId: String,
    val status: String
)

class AttendeeAdapter(private val attendees: List<Attendee>) :
    RecyclerView.Adapter<AttendeeAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemAttendeeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAttendeeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val attendee = attendees[position]
        holder.binding.tvName.text = attendee.name
        holder.binding.tvInitials.text = attendee.initials
        holder.binding.tvTicketInfo.text = attendee.ticketInfo
        holder.binding.tvBookingId.text = attendee.bookingId
        holder.binding.tvStatus.text = attendee.status
        
        if (attendee.status == "Pending") {
            holder.binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#FFA26B")) // md_secondary
            // Ideally should change background drawable as well
        }
    }

    override fun getItemCount() = attendees.size
}
