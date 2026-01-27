package com.eventfinder.app.client.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
// Ensure these binding imports match your project's generated classes
import com.eventfinder.app.databinding.ItemEventCardBinding
import com.eventfinder.app.databinding.ItemEventUpcomingBinding

class EventAdapter(
    private val items: List<EventItem>,
    private val isFeatured: Boolean,
    private val onClick: (EventItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    inner class FeaturedVH(val b: ItemEventCardBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(e: EventItem) {
            b.eventTitle.text = e.title
            b.eventLocation.text = e.location
            b.root.setOnClickListener { onClick(e) }
        }
    }

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