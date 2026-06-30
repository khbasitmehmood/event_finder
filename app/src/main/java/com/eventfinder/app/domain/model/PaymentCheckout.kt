package com.eventfinder.app.domain.model

data class PaymentCheckout(
    val checkoutId: String,
    val checkoutUrl: String,
    val provider: String,
    val transactionId: String,
    val amount: Double,
    val currency: String
)
