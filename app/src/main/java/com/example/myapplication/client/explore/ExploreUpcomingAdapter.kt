package com.example.myapplication.client.explore

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
// REMOVED: import android.R
// ADDED: The import for your specific binding class
import com.example.myapplication.databinding.ItemEventCardBinding
import com.example.myapplication.client.favourites.FavouritesViewModel
import com.example.myapplication.client.home.EventItem

class ExploreUpcomingAdapter(
    private val eventList: List<EventItem>,
    private val onItemClick: (EventItem) -> Unit,
    private val activity: AppCompatActivity
) : RecyclerView.Adapter<ExploreUpcomingAdapter.ExploreViewHolder>() {

    // Use the Binding class generated from item_event_card.xml
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
            // Updated to match the IDs in our new XML
            eventTitle.text = item.title
            // Since our new design has separate Date and Month,
            // you might need to split your item.date string or just set one:
            // tvDate.text = item.date

            // Note: If you kept the IDs tvLocation/tvDate in the XML,
            // use those here. If you used my code exactly, use:
            // eventTitle, etc.

            // Open details
            root.setOnClickListener {
                onItemClick(item)
            }

            // Favorite button (The black circle in our design)
            // Ensure this ID exists in your item_event_card.xml
            // btnAddFavourite.setOnClickListener { ... }
        }
    }

    override fun getItemCount(): Int = eventList.size
}