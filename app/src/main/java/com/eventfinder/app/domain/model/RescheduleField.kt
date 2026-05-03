package com.eventfinder.app.domain.model

/**
 * Fields that can be changed during event rescheduling
 */
enum class RescheduleField {
    START_TIME,
    END_TIME,
    LOCATION,
    ADDRESS;

    fun getDisplayName(): String = when (this) {
        START_TIME -> "Start Time"
        END_TIME -> "End Time"
        LOCATION -> "Location"
        ADDRESS -> "Address"
    }
}
