package com.eventfinder.app.domain.model

data class EventLocation(
    val latitude: Double,
    val longitude: Double,
    val geohash: String? = null               // optional – compute on save if needed
)