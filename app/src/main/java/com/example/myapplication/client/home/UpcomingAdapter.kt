package com.example.myapplication.client.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemEventUpcomingBinding

class UpcomingAdapter(
    private val items: List<EventItem>,
    private val onClick: (EventItem) -> Unit
) : RecyclerView.Adapter<UpcomingAdapter.VH>() {

    inner class VH(val b: ItemEventUpcomingBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(e: EventItem) {
            b.tvEventTitle.text = e.title
            b.tvEventDate.text = e.date
            b.imgThumb.setOnClickListener { onClick(e) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemEventUpcomingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size
}
