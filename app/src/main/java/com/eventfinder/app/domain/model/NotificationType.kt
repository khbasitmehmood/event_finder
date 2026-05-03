package com.eventfinder.app.domain.model

/**
 * Types of notifications in the system
 */
enum class NotificationType {
    // Event Lifecycle - For Attendees
    EVENT_PUBLISHED,
    EVENT_POSTPONED,
    EVENT_RESCHEDULED,
    EVENT_CANCELLED,
    EVENT_STARTING_SOON_24H,
    EVENT_STARTING_SOON_1H,
    EVENT_STARTED,
    EVENT_COMPLETED,
    EVENT_EXPIRED,
    EVENT_DETAILS_CHANGED,

    // Tickets - For Attendees
    TICKET_PURCHASED,
    TICKET_CONFIRMED,
    TICKET_CANCELLED,
    REFUND_INITIATED,
    REFUND_COMPLETED,
    REFUND_FAILED,

    // Event Management - For Organizers
    EVENT_PUBLISHED_SUCCESS,
    EVENT_STATE_CHANGED,
    EVENT_ABOUT_TO_START,
    EVENT_ENDED_MARK_COMPLETE,
    EVENT_AUTO_EXPIRED,

    // Attendee Activity - For Organizers
    NEW_ATTENDEE,
    TICKET_SCANNED,
    CAPACITY_MILESTONE_50,
    CAPACITY_MILESTONE_75,
    CAPACITY_MILESTONE_90,
    CAPACITY_FULL,
    LOW_ATTENDANCE_ALERT,

    // Financial - For Organizers
    REFUNDS_INITIATED,
    REFUNDS_COMPLETED,
    REFUND_ACTION_NEEDED,

    // General
    ORGANIZER_MESSAGE,
    SYSTEM_ANNOUNCEMENT;

    fun getDisplayName(): String = when (this) {
        EVENT_PUBLISHED -> "Event Published"
        EVENT_POSTPONED -> "Event Postponed"
        EVENT_RESCHEDULED -> "Event Rescheduled"
        EVENT_CANCELLED -> "Event Cancelled"
        EVENT_STARTING_SOON_24H -> "Event Tomorrow"
        EVENT_STARTING_SOON_1H -> "Event Starting Soon"
        EVENT_STARTED -> "Event Started"
        EVENT_COMPLETED -> "Event Completed"
        EVENT_EXPIRED -> "Event Expired"
        EVENT_DETAILS_CHANGED -> "Event Updated"

        TICKET_PURCHASED -> "Ticket Purchased"
        TICKET_CONFIRMED -> "Booking Confirmed"
        TICKET_CANCELLED -> "Ticket Cancelled"
        REFUND_INITIATED -> "Refund Processing"
        REFUND_COMPLETED -> "Refund Completed"
        REFUND_FAILED -> "Refund Failed"

        EVENT_PUBLISHED_SUCCESS -> "Event Published"
        EVENT_STATE_CHANGED -> "Event State Changed"
        EVENT_ABOUT_TO_START -> "Event About to Start"
        EVENT_ENDED_MARK_COMPLETE -> "Mark Event Complete"
        EVENT_AUTO_EXPIRED -> "Event Expired"

        NEW_ATTENDEE -> "New Attendee"
        TICKET_SCANNED -> "Ticket Scanned"
        CAPACITY_MILESTONE_50 -> "50% Capacity"
        CAPACITY_MILESTONE_75 -> "75% Capacity"
        CAPACITY_MILESTONE_90 -> "90% Capacity"
        CAPACITY_FULL -> "Event Full"
        LOW_ATTENDANCE_ALERT -> "Low Attendance"

        REFUNDS_INITIATED -> "Refunds Initiated"
        REFUNDS_COMPLETED -> "Refunds Completed"
        REFUND_ACTION_NEEDED -> "Refund Action Needed"

        ORGANIZER_MESSAGE -> "Message from Organizer"
        SYSTEM_ANNOUNCEMENT -> "System Announcement"
    }

    fun getIconResource(): String = when (this) {
        EVENT_PUBLISHED, EVENT_PUBLISHED_SUCCESS -> "ic_event"
        EVENT_POSTPONED, EVENT_RESCHEDULED -> "ic_select_date"
        EVENT_CANCELLED -> "ic_delete_outline"
        EVENT_STARTING_SOON_24H, EVENT_STARTING_SOON_1H, EVENT_ABOUT_TO_START -> "ic_notification"
        EVENT_STARTED -> "ic_play"
        EVENT_COMPLETED -> "ic_check"
        EVENT_EXPIRED, EVENT_AUTO_EXPIRED -> "ic_warning"

        TICKET_PURCHASED, TICKET_CONFIRMED, NEW_ATTENDEE -> "ic_ticket"
        TICKET_CANCELLED, TICKET_SCANNED -> "ic_qr_code"
        REFUND_INITIATED, REFUND_COMPLETED, REFUNDS_INITIATED, REFUNDS_COMPLETED -> "ic_payment"
        REFUND_FAILED, REFUND_ACTION_NEEDED -> "ic_error"

        CAPACITY_MILESTONE_50, CAPACITY_MILESTONE_75, CAPACITY_MILESTONE_90, CAPACITY_FULL -> "ic_people"
        LOW_ATTENDANCE_ALERT -> "ic_warning"

        EVENT_STATE_CHANGED, EVENT_DETAILS_CHANGED -> "ic_info"
        EVENT_ENDED_MARK_COMPLETE -> "ic_check"

        ORGANIZER_MESSAGE -> "ic_message"
        SYSTEM_ANNOUNCEMENT -> "ic_notification"
    }

    fun getPriority(): NotificationPriority = when (this) {
        EVENT_CANCELLED, REFUND_FAILED, REFUND_ACTION_NEEDED,
        EVENT_AUTO_EXPIRED, LOW_ATTENDANCE_ALERT -> NotificationPriority.URGENT

        EVENT_POSTPONED, EVENT_RESCHEDULED, EVENT_STARTING_SOON_1H,
        CAPACITY_FULL, EVENT_ABOUT_TO_START -> NotificationPriority.HIGH

        EVENT_STARTING_SOON_24H, NEW_ATTENDEE, TICKET_SCANNED,
        CAPACITY_MILESTONE_90, EVENT_ENDED_MARK_COMPLETE -> NotificationPriority.NORMAL

        else -> NotificationPriority.LOW
    }

    fun isForAttendee(): Boolean = this in listOf(
        EVENT_PUBLISHED, EVENT_POSTPONED, EVENT_RESCHEDULED, EVENT_CANCELLED,
        EVENT_STARTING_SOON_24H, EVENT_STARTING_SOON_1H, EVENT_STARTED,
        EVENT_COMPLETED, EVENT_EXPIRED, EVENT_DETAILS_CHANGED,
        TICKET_PURCHASED, TICKET_CONFIRMED, TICKET_CANCELLED,
        REFUND_INITIATED, REFUND_COMPLETED, REFUND_FAILED,
        ORGANIZER_MESSAGE
    )

    fun isForOrganizer(): Boolean = this in listOf(
        EVENT_PUBLISHED_SUCCESS, EVENT_STATE_CHANGED, EVENT_ABOUT_TO_START,
        EVENT_ENDED_MARK_COMPLETE, EVENT_AUTO_EXPIRED,
        NEW_ATTENDEE, TICKET_SCANNED,
        CAPACITY_MILESTONE_50, CAPACITY_MILESTONE_75, CAPACITY_MILESTONE_90,
        CAPACITY_FULL, LOW_ATTENDANCE_ALERT,
        REFUNDS_INITIATED, REFUNDS_COMPLETED, REFUND_ACTION_NEEDED
    )
}

/**
 * Priority levels for notifications
 */
enum class NotificationPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT;

    fun getDisplayName(): String = when (this) {
        LOW -> "Low"
        NORMAL -> "Normal"
        HIGH -> "High"
        URGENT -> "Urgent"
    }
}
