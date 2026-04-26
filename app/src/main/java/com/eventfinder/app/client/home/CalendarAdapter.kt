package com.eventfinder.app.client.home

import android.content.res.ColorStateList
import android.graphics.Color
import android.util.TypedValue
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

        val greenColor = Color.parseColor("#00A96E")

        holder.binding.apply {
            tvDayName.text = day.dayName
            tvDayNumber.text = day.dayNumber
            tvDayNumber.setTextColor(Color.parseColor("#1A1A1A")) // Black text for all days
            
            // In the mock, dots are green for all days
            eventIndicator.isVisible = true
            eventIndicator.backgroundTintList = ColorStateList.valueOf(greenColor)

            if (isSelected || day.isToday) {
                // Set stroke width (in pixels)
                val strokePx = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 
                    1.5f, 
                    context.resources.displayMetrics
                ).toInt()
                cardCalendarDay.strokeWidth = strokePx
                cardCalendarDay.strokeColor = greenColor
            } else {
                cardCalendarDay.strokeWidth = 0
            }
        }
    }

    override fun getItemCount(): Int = days.size

    fun updateDays(newDays: List<CalendarDay>) {
        days = newDays
        // Find today and auto-select it if nothing is selected
        if (selectedPosition == -1) {
            selectedPosition = days.indexOfFirst { it.isToday }
        }
        notifyDataSetChanged()
    }
}