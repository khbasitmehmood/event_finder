package com.example.myapplication.client.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemEventFeaturedBinding
import com.example.myapplication.databinding.ItemEventUpcomingBinding

class EventAdapter(
    private val items: List<EventItem>,
    private val isFeatured: Boolean, // Flag to decide which layout to use
    private val onClick: (EventItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // 🔹 ViewHolder for Featured Items
    inner class FeaturedVH(val b: ItemEventFeaturedBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(e: EventItem) {
            // Check your XML ID: if it's @+id/tv_title, use b.tvTitle
            b.tvTitle.text = e.title
            b.root.setOnClickListener { onClick(e) }
        }
    }

    // 🔹 ViewHolder for Upcoming Items
    inner class UpcomingVH(val b: ItemEventUpcomingBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(e: EventItem) {
            b.tvEventTitle.text = e.title
            b.root.setOnClickListener { onClick(e) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (isFeatured) {
            FeaturedVH(ItemEventFeaturedBinding.inflate(inflater, parent, false))
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