package com.eventfinder.app.domain.model

/**
 * Domain model for Event - used across the app
 */
data class Event(
    val id: Int,
    val title: String,
    val location: String,
    val date: String,
    val imageUrl: String? = null,
    val category: String? = null,
    val description: String? = null
)
