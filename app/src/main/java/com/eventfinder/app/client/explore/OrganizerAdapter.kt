package com.eventfinder.app.client.explore

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.eventfinder.app.R
import com.eventfinder.app.databinding.ItemOrganizerBinding
import com.eventfinder.app.domain.model.User

/**
 * Adapter for displaying organizers in Explore screen
 */
class OrganizerAdapter(
    private val onClick: (User) -> Unit
) : ListAdapter<User, OrganizerAdapter.OrganizerViewHolder>(OrganizerDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrganizerViewHolder {
        val binding = ItemOrganizerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrganizerViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: OrganizerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class OrganizerViewHolder(
        private val binding: ItemOrganizerBinding,
        private val onClick: (User) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(organizer: User) {
            val profile = organizer.organizerProfile ?: return

            binding.tvOrganizerName.text = profile.organizationName
            binding.tvOrganizerCity.text = profile.city ?: "Location not specified"

            // Load organizer logo/photo
            binding.imgOrganizer.load(profile.logoUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_profile)
                error(R.drawable.ic_profile)
                transformations(CircleCropTransformation())
            }

            binding.root.setOnClickListener { onClick(organizer) }
        }
    }

    private class OrganizerDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}
