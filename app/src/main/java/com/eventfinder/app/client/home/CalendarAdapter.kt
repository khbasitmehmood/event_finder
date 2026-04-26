package com.eventfinder.app.client.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.eventfinder.app.R
import com.eventfinder.app.databinding.ItemCalendarDayBinding

/**
 * Adapter for displaying week calendar
 */
class CalendarAdapter(
    private var days: List<CalendarDay>,
    private val onDayClick: (CalendarDay) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    private var selectedPosition = -1

    inner class CalendarViewHolder(val binding: ItemCalendarDayBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val previousPosition = selectedPosition
                    selectedPosition = position
                    notifyItemChanged(previousPosition)
                    notifyItemChanged(selectedPosition)
                    onDayClick(days[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val binding = ItemCalendarDayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CalendarViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        val day = days[position]
        val context = holder.binding.root.context
        val isSelected = position == selectedPosition || day.isSelected

        holder.binding.apply {
            tvDayName.text = day.dayName
            tvDayNumber.text = day.dayNumber
            eventIndicator.isVisible = day.hasEvents

            // Update card appearance based on selection
            if (isSelected) {
                cardCalendarDay.strokeWidth = 0
                cardCalendarDay.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.md_primary)
                )
                tvDayName.setTextColor(
                    ContextCompat.getColor(context, R.color.md_on_primary)
                )
                tvDayNumber.setTextColor(
                    ContextCompat.getColor(context, R.color.md_on_primary)
                )
            } else if (day.isToday) {
                cardCalendarDay.strokeWidth = 2
                cardCalendarDay.strokeColor =
                    ContextCompat.getColor(context, R.color.md_primary)
                cardCalendarDay.setCardBackgroundColor(
                    ContextCompat.getColor(context, android.R.color.white)
                )
                tvDayName.setTextColor(
                    ContextCompat.getColor(context, R.color.md_primary)
                )
                tvDayNumber.setTextColor(
                    ContextCompat.getColor(context, R.color.md_primary)
                )
            } else {
                cardCalendarDay.strokeWidth = 1
                cardCalendarDay.strokeColor = 0xFFE0E0E0.toInt()
                cardCalendarDay.setCardBackgroundColor(
                    ContextCompat.getColor(context, android.R.color.white)
                )
                tvDayName.setTextColor(0xFF999999.toInt())
                tvDayNumber.setTextColor(
                    ContextCompat.getColor(context, R.color.md_primary)
                )
            }
        }
    }

    override fun getItemCount(): Int = days.size

    fun updateDays(newDays: List<CalendarDay>) {
        days = newDays
        // Find today and auto-select it
        selectedPosition = days.indexOfFirst { it.isToday }
        notifyDataSetChanged()
    }
}
