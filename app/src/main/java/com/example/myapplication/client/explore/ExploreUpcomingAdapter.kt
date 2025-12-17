package com.example.myapplication.client.explore

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemExploreEventBinding
import com.example.myapplication.client.favourites.FavouritesViewModel
import com.example.myapplication.client.home.EventItem

class ExploreUpcomingAdapter(
    private val eventList: List<EventItem>,
    private val onItemClick: (EventItem) -> Unit,
    private val activity: AppCompatActivity
) : RecyclerView.Adapter<ExploreUpcomingAdapter.ExploreViewHolder>() {

    inner class ExploreViewHolder(val binding: ItemExploreEventBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExploreViewHolder {
        val binding = ItemExploreEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExploreViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExploreViewHolder, position: Int) {
        val item = eventList[position]

        holder.binding.apply {
            tvTitle.text = item.title
            tvLocation.text = item.location
            tvDate.text = item.date
            item.imageRes?.let { ivEventImage.setImageResource(it) }

            // Open details
            root.setOnClickListener {
                onItemClick(item)
            }

            // Add to favourites
            btnAddFavourite.setOnClickListener {
                val viewModel = ViewModelProvider(activity)
                    .get(FavouritesViewModel::class.java)

                viewModel.addFavourite(item.title)
            }
        }
    }

    override fun getItemCount(): Int = eventList.size
}
