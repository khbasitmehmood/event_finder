package com.eventfinder.app.client.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.eventfinder.app.R
import com.eventfinder.app.databinding.ItemEventBinding
import com.eventfinder.app.domain.model.Event
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter for displaying events in a compact/small card format
 * Used for horizontal lists with smaller cards
 */
class SmallEventAdapter(
    private val onClick: (Event) -> Unit
) : ListAdapter<Event, SmallEventAdapter.SmallEventViewHolder>(EventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmallEventViewHolder {
        val binding = ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SmallEventViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: SmallEventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SmallEventViewHolder(
        private val binding: ItemEventBinding,
        private val onClick: (Event) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        fun bind(event: Event) {
            binding.tvTitle.text = event.title
            binding.tvLocation.text = event.address ?: "Location TBD"
            binding.tvDate.text = dateFormat.format(Date(event.startTime))

            // Load image using Coil
            binding.imgCover.load(event.mainImageUrl ?: event.imageUrls.firstOrNull()) {
                crossfade(true)
                placeholder(R.drawable.ic_event_placeholder)
                error(R.drawable.ic_event_placeholder)
            }

            binding.root.setOnClickListener { onClick(event) }
        }
    }

    private class EventDiffCallback : DiffUtil.ItemCallback<Event>() {
        override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean {
            return oldItem == newItem
        }
    }
}
