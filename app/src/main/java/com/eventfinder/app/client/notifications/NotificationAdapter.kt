package com.eventfinder.app.client.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.eventfinder.app.R
import com.eventfinder.app.databinding.ItemNotificationBinding
import com.eventfinder.app.domain.model.EventNotification
import com.eventfinder.app.domain.model.NotificationPriority

/**
 * Adapter for displaying notifications
 */
class NotificationAdapter(
    private val onNotificationClick: (EventNotification) -> Unit
) : ListAdapter<EventNotification, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding, onNotificationClick)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NotificationViewHolder(
        private val binding: ItemNotificationBinding,
        private val onNotificationClick: (EventNotification) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: EventNotification) {
            with(binding) {
                // Title and message
                textTitle.text = notification.title
                textMessage.text = notification.message

                // Time
                textTime.text = notification.getDisplayTime()

                // Unread indicator
                unreadIndicator.isVisible = !notification.isRead

                // Background color for unread
                cardNotification.setCardBackgroundColor(
                    if (notification.isRead) {
                        itemView.context.getColor(android.R.color.transparent)
                    } else {
                        itemView.context.getColor(R.color.notification_unread_bg)
                    }
                )

                // Event image
                if (!notification.eventImageUrl.isNullOrEmpty()) {
                    imageEvent.isVisible = true
                    iconNotification.isVisible = false
                    imageEvent.load(notification.eventImageUrl) {
                        crossfade(true)
                        placeholder(R.drawable.ic_calendar)
                        error(R.drawable.ic_calendar)
                    }
                } else {
                    imageEvent.isVisible = false
                    iconNotification.isVisible = true
                    iconNotification.setImageResource(
                        getNotificationIcon(notification.type.name)
                    )
                }

                // Event title
                if (notification.eventTitle.isNotEmpty()) {
                    textEventTitle.isVisible = true
                    textEventTitle.text = notification.eventTitle
                } else {
                    textEventTitle.isVisible = false
                }

                // Priority badge (only show for high/urgent)
                chipPriority.isVisible = notification.priority in listOf(
                    NotificationPriority.HIGH,
                    NotificationPriority.URGENT
                )
                if (chipPriority.isVisible) {
                    chipPriority.text = notification.priority.name.lowercase()
                        .replaceFirstChar { it.uppercase() }
                    chipPriority.setChipBackgroundColorResource(
                        when (notification.priority) {
                            NotificationPriority.URGENT -> R.color.priority_urgent_bg
                            NotificationPriority.HIGH -> R.color.priority_high_bg
                            else -> android.R.color.transparent
                        }
                    )
                }

                // Click listener
                root.setOnClickListener {
                    onNotificationClick(notification)
                }
            }
        }

        private fun getNotificationIcon(typeName: String): Int {
            return when {
                typeName.contains("CANCEL") -> R.drawable.ic_cancel
                typeName.contains("POSTPONE") -> R.drawable.ic_calendar
                typeName.contains("RESCHEDULE") -> R.drawable.ic_calendar
                typeName.contains("TICKET") -> R.drawable.ic_check_circle
                typeName.contains("REFUND") -> R.drawable.ic_info
                typeName.contains("ATTENDEE") || typeName.contains("CAPACITY") -> R.drawable.ic_profile
                else -> R.drawable.ic_notifications
            }
        }
    }

    class NotificationDiffCallback : DiffUtil.ItemCallback<EventNotification>() {
        override fun areItemsTheSame(
            oldItem: EventNotification,
            newItem: EventNotification
        ): Boolean {
            return oldItem.notificationId == newItem.notificationId
        }

        override fun areContentsTheSame(
            oldItem: EventNotification,
            newItem: EventNotification
        ): Boolean {
            return oldItem == newItem
        }
    }
}
