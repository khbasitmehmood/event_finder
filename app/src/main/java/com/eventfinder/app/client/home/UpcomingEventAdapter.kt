package com.eventfinder.app.client.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.eventfinder.app.R
import com.eventfinder.app.databinding.ItemEventUpcomingBinding
import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.utils.DateFormatter

class UpcomingEventAdapter(
    private val onClick: (Event) -> Unit
) : ListAdapter<Event, UpcomingEventAdapter.ViewHolder>(EventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEventUpcomingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemEventUpcomingBinding,
        private val onClick: (Event) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) {
            binding.apply {
                tvEventTitle.text = event.title
                tvEventDate.text = DateFormatter.formatDate(event.startTime)

                event.mainImageUrl?.let { url ->
                    imgThumb.load(url) {
                        crossfade(true)
                        placeholder(R.drawable.ic_image_placeholder)
                        error(R.drawable.ic_image_placeholder)
                    }
                } ?: run {
                    imgThumb.setImageResource(R.drawable.ic_image_placeholder)
                }

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
            return oldItem.createdAt == newItem.createdAt
        }
    }
}