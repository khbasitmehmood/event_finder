package com.example.myapplication.client.watchlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemWatchlistBinding

// Cleaned: Reference WatchlistItem by its simple name.
class WatchlistAdapter(
    private val items: List<WatchlistItem>
) : RecyclerView.Adapter<WatchlistAdapter.WatchlistViewHolder>() {

    inner class WatchlistViewHolder(val binding: ItemWatchlistBinding)
        : RecyclerView.ViewHolder(binding.root)

    // Cleaned: Removed the illegal prefix from the return type.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WatchlistViewHolder {
        val binding = ItemWatchlistBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WatchlistViewHolder(binding)
    }

    // Cleaned: Removed the illegal prefix from the holder parameter type.
    override fun onBindViewHolder(holder: WatchlistViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvTitle.text = item.title
        holder.binding.tvGenre.text = item.genre
        holder.binding.tvYear.text = item.year
    }

    override fun getItemCount() = items.size
}