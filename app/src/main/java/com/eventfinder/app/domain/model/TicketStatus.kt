package com.eventfinder.app.domain.model

/**
 * Status of a ticket throughout its lifecycle
 */
enum class TicketStatus {
    /**
     * User has reserved a spot (public events)
     */
    RESERVED,

    /**
     * Ticket has been purchased (free or paid private events)
     */
    PURCHASED,

    /**
     * User has been checked in at the event
     */
    CHECKED_IN,

    /**
     * Ticket has been cancelled by the user
     */
    CANCELLED,

    /**
     * Event has passed and ticket is no longer valid
     */
    EXPIRED
}
