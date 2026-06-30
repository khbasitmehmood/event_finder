package com.eventfinder.app.domain.model

data class PaymentRequest(
    val eventId: String,
    val eventTitle: String,
    val userId: String,
    val userName: String,
    val userEmail: String,
    val amount: Double,
    val currency: String,
    val description: String
)
