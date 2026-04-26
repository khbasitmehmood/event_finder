package com.eventfinder.app.client.explore

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.eventfinder.app.R
import com.eventfinder.app.databinding.ItemEventCardBinding
import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.utils.DateFormatter
import com.eventfinder.app.utils.LocationUtils
import java.text.NumberFormat
import java.util.Locale

/**
 * Adapter for displaying events in the Explore screen
 */
class ExploreUpcomingAdapter(
    private var eventList: List<Event>,
    private val onItemClick: (Event) -> Unit
) : RecyclerView.Adapter<ExploreUpcomingAdapter.ExploreViewHolder>() {

    inner class ExploreViewHolder(val binding: ItemEventCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExploreViewHolder {
        val binding = ItemEventCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExploreViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExploreViewHolder, position: Int) {
        val event = eventList[position]

        holder.binding.apply {
            // Load event image with Coil
            eventImage.load(event.mainImageUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_event_placeholder)
                error(R.drawable.ic_event_placeholder)
            }

            // Set date
            eventDay.text = DateFormatter.getDay(event.startTime)
            eventMonth.text = DateFormatter.getMonth(event.startTime)

            // Set category
            eventCategory.text = event.category?.name ?: "EVENT"
            eventCategory.isVisible = event.category != null

            // Set title
            eventTitle.text = event.title

            // Set location
            eventLocation.text = LocationUtils.getShortAddress(event.address)

            // Set distance if available
            if (event.distanceKm != null) {
                eventDistance.text = LocationUtils.formatDistance(event.distanceKm)
                eventDistance.isVisible = true
            } else {
                eventDistance.isVisible = false
            }

            // Set time only (more compact)
            eventDateTime.text = DateFormatter.formatTime(event.startTime)

            // Set price
            if (!event.isFree && event.price != null) {
                val formattedPrice = NumberFormat.getCurrencyInstance(Locale("en", "PK"))
                    .apply { currency = java.util.Currency.getInstance(event.currency ?: "PKR") }
                    .format(event.price)
                eventPrice.text = formattedPrice
                eventPrice.isVisible = true
            } else if (event.isFree) {
                eventPrice.text = "FREE"
                eventPrice.isVisible = true
            } else {
                eventPrice.isVisible = false
            }

            // Click listeners
            root.setOnClickListener {
                onItemClick(event)
            }

            btnAddToFavourites.setOnClickListener {
                // TODO: Implement favorite functionality
            }
        }
    }

    override fun getItemCount(): Int = eventList.size

    /**
     * Update the events list with new data using DiffUtil for efficient updates
     */
    fun updateEvents(newEvents: List<Event>) {
        val diffCallback = EventDiffCallback(eventList, newEvents)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        eventList = newEvents
        diffResult.dispatchUpdatesTo(this)
    }

    /**
     * DiffUtil callback for efficient RecyclerView updates
     */
    private class EventDiffCallback(
        private val oldList: List<Event>,
        private val newList: List<Event>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}