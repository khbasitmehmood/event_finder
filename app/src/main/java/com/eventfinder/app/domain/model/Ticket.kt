package com.eventfinder.app.domain.model

import java.io.Serializable

/**
 * Domain model for Ticket
 * Represents a user's registration/purchase for an event
 */
data class Ticket(
    val id: String = "",
    val ticketId: String = "",
    val eventId: String,
    val eventTitle: String,
    val eventStartTime: Long,
    val userId: String,
    val userName: String,
    val userEmail: String,
    val ticketType: TicketType,
    val status: TicketStatus,
    val qrCodeData: String,
    val purchasePrice: Double = 0.0,
    val currency: String = "PKR",
    val paymentStatus: PaymentStatus = PaymentStatus.NOT_REQUIRED,
    val paymentProvider: String? = null,
    val paymentTransactionId: String? = null,
    val paidAt: Long? = null,
    val purchasedAt: Long = System.currentTimeMillis(),
    val checkedInAt: Long? = null,
    val checkedInBy: String? = null,
    val eventLocation: String? = null,
    val organizerId: String,
    val organizerName: String
) : Serializable
