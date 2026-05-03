package com.eventfinder.app.domain.model

/**
 * Type of ticket issued for an event
 */
enum class TicketType {
    /**
     * Free reservation for public events - "I am going"
     * No QR ticket needed, just counts attendance
     */
    PUBLIC_RESERVATION,

    /**
     * Free ticket for private events
     * Requires QR code for entry
     */
    FREE_PRIVATE,

    /**
     * Paid ticket for private events
     * Requires payment and QR code for entry
     */
    PAID
}
