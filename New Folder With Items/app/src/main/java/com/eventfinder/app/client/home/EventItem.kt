package com.eventfinder.app.client.home

import com.eventfinder.app.R

data class EventItem(
    val id: Int,
    val title: String,
    val location: String? = null,
    val date: String? = null,
    val imageRes: Int? = R.drawable.ic_event_placeholder // default placeholder
)
