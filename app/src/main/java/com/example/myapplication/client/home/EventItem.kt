package com.example.myapplication.client.home

import com.example.myapplication.R

data class EventItem(
    val id: Int,
    val title: String,
    val location: String? = null,
    val date: String? = null,
    val imageRes: Int? = R.drawable.ic_event_placeholder // default placeholder
)
