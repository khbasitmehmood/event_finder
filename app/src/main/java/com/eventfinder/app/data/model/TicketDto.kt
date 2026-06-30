package com.eventfinder.app.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Data Transfer Object for Firestore Ticket documents
 * Represents the exact structure stored in Firestore
 */
data class TicketDto(
    @DocumentId
    val id: String = "",
    val ticketId: String = "",
    val eventId: String = "",
    val eventTitle: String = "",
    val eventStartTime: Timestamp? = null,
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val ticketType: String = "PUBLIC_RESERVATION",
    val status: String = "RESERVED",
    val qrCodeData: String = "",
    val purchasePrice: Number = 0.0,
    val currency: String = "PKR",
    val paymentStatus: String = "NOT_REQUIRED",
    val paymentProvider: String? = null,
    val paymentTransactionId: String? = null,
    val paidAt: Timestamp? = null,
    val purchasedAt: Timestamp? = null,
    val checkedInAt: Timestamp? = null,
    val checkedInBy: String? = null,
    val eventLocation: String? = null,
    val organizerId: String = "",
    val organizerName: String = ""
)
