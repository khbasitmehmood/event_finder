package com.eventfinder.app.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Utility object for formatting dates and times
 */
object DateFormatter {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("dd", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
    private val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
    private val fullDateTimeFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    /**
     * Format timestamp to readable date
     * Example: "25 Apr 2024"
     */
    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    /**
     * Format timestamp to time
     * Example: "02:30 PM"
     */
    fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }

    /**
     * Format timestamp to full date and time
     * Example: "25 Apr 2024, 02:30 PM"
     */
    fun formatFullDateTime(timestamp: Long): String {
        return fullDateTimeFormat.format(Date(timestamp))
    }

    /**
     * Get day from timestamp
     * Example: "25"
     */
    fun getDay(timestamp: Long): String {
        return dayFormat.format(Date(timestamp))
    }

    /**
     * Get month from timestamp
     * Example: "APR"
     */
    fun getMonth(timestamp: Long): String {
        return monthFormat.format(Date(timestamp)).uppercase()
    }

    /**
     * Get year from timestamp
     * Example: "2024"
     */
    fun getYear(timestamp: Long): String {
        return yearFormat.format(Date(timestamp))
    }

    /**
     * Get relative time string
     * Example: "In 2 days", "Tomorrow", "Today"
     */
    fun getRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = timestamp - now

        return when {
            diff < 0 -> "Past event"
            diff < TimeUnit.HOURS.toMillis(1) -> "In ${TimeUnit.MILLISECONDS.toMinutes(diff)} minutes"
            diff < TimeUnit.HOURS.toMillis(24) -> "In ${TimeUnit.MILLISECONDS.toHours(diff)} hours"
            diff < TimeUnit.HOURS.toMillis(48) -> "Tomorrow"
            diff < TimeUnit.DAYS.toMillis(7) -> "In ${TimeUnit.MILLISECONDS.toDays(diff)} days"
            diff < TimeUnit.DAYS.toMillis(30) -> "In ${TimeUnit.MILLISECONDS.toDays(diff) / 7} weeks"
            else -> formatDate(timestamp)
        }
    }

    /**
     * Format date range
     * Example: "25 Apr - 27 Apr 2024"
     */
    fun formatDateRange(startTime: Long, endTime: Long?): String {
        if (endTime == null) return formatDate(startTime)

        val startDate = Date(startTime)
        val endDate = Date(endTime)

        return if (isSameDay(startDate, endDate)) {
            formatDate(startTime)
        } else {
            "${formatDate(startTime)} - ${formatDate(endTime)}"
        }
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = java.util.Calendar.getInstance().apply { time = date1 }
        val cal2 = java.util.Calendar.getInstance().apply { time = date2 }
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
    }
}
