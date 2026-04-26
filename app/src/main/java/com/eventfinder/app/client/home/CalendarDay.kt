package com.eventfinder.app.client.home

import java.util.Date

/**
 * Data class representing a single day in the calendar view
 */
data class CalendarDay(
    val date: Date,
    val dayName: String,
    val dayNumber: String,
    val isToday: Boolean = false,
    val isSelected: Boolean = false,
    val hasEvents: Boolean = false
)
