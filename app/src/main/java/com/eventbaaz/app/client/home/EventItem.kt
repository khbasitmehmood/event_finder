package com.eventbaaz.app.client.home

import com.eventbaaz.app.R

data class EventItem(
    val id: Int,
    val title: String,
    val location: String? = null,
    val date: String? = null,
    val imageRes: Int? = R.drawable.ic_event_placeholder // default placeholder
)
