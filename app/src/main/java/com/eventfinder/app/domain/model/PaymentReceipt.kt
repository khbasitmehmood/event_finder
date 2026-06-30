package com.eventfinder.app.domain.model

data class PaymentReceipt(
    val provider: String,
    val transactionId: String,
    val checkoutId: String? = null,
    val ticketId: String? = null,
    val amount: Double,
    val currency: String,
    val paidAt: Long,
    val status: PaymentStatus
)
