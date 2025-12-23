package com.example.myapplication.client.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
// Ensure these binding imports match your project's generated classes
import com.example.myapplication.databinding.ItemEventCardBinding
import com.example.myapplication.databinding.ItemEventUpcomingBinding

class EventAdapter(
    private val items: List<EventItem>,
    private val isFeatured: Boolean,
    private val onClick: (EventItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // 🔹 ViewHolder for Featured Items (Using the Explore-style card)
    inner class FeaturedVH(val b: ItemEventCardBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(e: EventItem) {
            // Mapping IDs to the item_event_card.xml structure
            b.eventTitle.text = e.title
            b.eventLocation.text = e.location

            // If your EventItem has separate day/month, use them here.
            // Otherwise, set a default or split the date string.
            // b.tvDateDay.text = "12"

            b.root.setOnClickListener { onClick(e) }
        }
    }

    // 🔹 ViewHolder for Upcoming Items (Keeping your existing logic)
    inner class UpcomingVH(val b: ItemEventUpcomingBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(e: EventItem) {
            b.tvEventTitle.text = e.title
            b.tvEventDate.text = e.date
            b.root.setOnClickListener { onClick(e) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (isFeatured) {
            // Inflating the new premium card layout
            FeaturedVH(ItemEventCardBinding.inflate(inflater, parent, false))
        } else {
            UpcomingVH(ItemEventUpcomingBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is FeaturedVH -> holder.bind(item)
            is UpcomingVH -> holder.bind(item)
        }
    }

    override fun getItemCount() = items.size
}