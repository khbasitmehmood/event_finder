package com.eventfinder.app.client.explore

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.eventfinder.app.databinding.ItemEventCardBinding
import com.eventfinder.app.client.home.EventItem

class ExploreUpcomingAdapter(
    private var eventList: List<EventItem>,
    private val onItemClick: (EventItem) -> Unit,
    private val activity: AppCompatActivity
) : RecyclerView.Adapter<ExploreUpcomingAdapter.ExploreViewHolder>() {

    inner class ExploreViewHolder(val binding: ItemEventCardBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExploreViewHolder {
        val binding = ItemEventCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExploreViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExploreViewHolder, position: Int) {
        val item = eventList[position]

        holder.binding.apply {
            eventTitle.text = item.title

            root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun getItemCount(): Int = eventList.size

    /**
     * Update the events list with new data using DiffUtil for efficient updates
     */
    fun updateEvents(newEvents: List<EventItem>) {
        val diffCallback = EventDiffCallback(eventList, newEvents)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        eventList = newEvents
        diffResult.dispatchUpdatesTo(this)
    }

    /**
     * DiffUtil callback for efficient RecyclerView updates
     */
    private class EventDiffCallback(
        private val oldList: List<EventItem>,
        private val newList: List<EventItem>
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