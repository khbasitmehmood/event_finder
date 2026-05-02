package com.eventfinder.app.client.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.eventfinder.app.R
import com.eventfinder.app.databinding.ItemEventCardBinding
import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.utils.DateFormatter
import com.eventfinder.app.utils.LocationUtils

/**
 * Adapter for displaying events on Home screen
 * Works with Event domain model
 */
class HomeEventAdapter(
    private val isHorizontal: Boolean = false,
    private val onClick: (Event) -> Unit
) : ListAdapter<Event, HomeEventAdapter.EventViewHolder>(EventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        
        if (isHorizontal) {
            val layoutParams = binding.root.layoutParams
            layoutParams.width = (parent.context.resources.displayMetrics.widthPixels * 0.85).toInt()
            binding.root.layoutParams = layoutParams
        } else {
            val layoutParams = binding.root.layoutParams
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            binding.root.layoutParams = layoutParams
        }
        
        return EventViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class EventViewHolder(
        private val binding: ItemEventCardBinding,
        private val onClick: (Event) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) {
            binding.apply {
                // Title
                eventTitle.text = event.title

                // Location
                eventLocation.text = event.address ?: event.location.let {
                    "${it.latitude}, ${it.longitude}"
                }

                // Date and Time
                eventDateTime.text = DateFormatter.formatDate(event.startTime)

                // Date badge
                eventDay.text = DateFormatter.getDay(event.startTime)
                eventMonth.text = DateFormatter.getMonth(event.startTime).uppercase()

                // Category
                event.category?.let {
                    eventCategory.text = it.name.lowercase()
                        .replaceFirstChar { char -> char.uppercase() }
                }

                // Price
                if (event.isFree) {
                    eventPrice.text = "Free"
                    eventPrice.visibility = android.view.View.GONE
                } else {
                    eventPrice.text = "${event.currency ?: "PKR"} ${event.price?.toInt() ?: 0}"
                    eventPrice.visibility = android.view.View.VISIBLE
                }

                // Image
                event.mainImageUrl?.let { url ->
                    eventImage.load(url) {
                        crossfade(true)
                        placeholder(R.drawable.ic_image_placeholder)
                        error(R.drawable.ic_image_placeholder)
                    }
                } ?: run {
                    eventImage.setImageResource(R.drawable.ic_image_placeholder)
                }

                // Click listener
                root.setOnClickListener {
                    onClick(event)
                }
            }
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