package com.eventfinder.app.client.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eventfinder.app.R
import com.eventfinder.app.databinding.ItemCategoryBinding
import com.eventfinder.app.domain.model.EventCategory

class CategoryAdapter : ListAdapter<EventCategory, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    inner class CategoryViewHolder(val binding: ItemCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(category: EventCategory) {
            binding.tvCategoryLabel.text = category.name
            // We use a generic icon for now since we removed icons from the data model
            binding.ivCategoryIcon.setImageResource(R.drawable.ic_explore)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class CategoryDiffCallback : DiffUtil.ItemCallback<EventCategory>() {
        override fun areItemsTheSame(oldItem: EventCategory, newItem: EventCategory): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: EventCategory, newItem: EventCategory): Boolean {
            return oldItem == newItem
        }
    }
}