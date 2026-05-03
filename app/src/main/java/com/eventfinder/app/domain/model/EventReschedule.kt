package com.eventfinder.app.domain.model

/**
 * Represents an event reschedule record
 * Tracks all changes made during rescheduling
 */
data class EventReschedule(
    val originalStartTime: Long,
    val originalEndTime: Long?,
    val originalLocation: EventLocation?,
    val originalAddress: String?,

    val newStartTime: Long,
    val newEndTime: Long?,
    val newLocation: EventLocation?,
    val newAddress: String?,

    val reason: String,
    val rescheduledAt: Long = System.currentTimeMillis(),
    val rescheduledBy: String,
    val notificationSent: Boolean = false
) {
    /**
     * Check if start time was changed
     */
    fun isStartTimeChanged(): Boolean = originalStartTime != newStartTime

    /**
     * Check if end time was changed
     */
    fun isEndTimeChanged(): Boolean = originalEndTime != newEndTime

    /**
     * Check if location was changed
     */
    fun isLocationChanged(): Boolean {
        return originalLocation?.latitude != newLocation?.latitude ||
               originalLocation?.longitude != newLocation?.longitude
    }

    /**
     * Check if address was changed
     */
    fun isAddressChanged(): Boolean = originalAddress != newAddress

    /**
     * Get list of changed fields
     */
    fun getChangedFields(): List<RescheduleField> {
        val fields = mutableListOf<RescheduleField>()
        if (isStartTimeChanged()) fields.add(RescheduleField.START_TIME)
        if (isEndTimeChanged()) fields.add(RescheduleField.END_TIME)
        if (isLocationChanged()) fields.add(RescheduleField.LOCATION)
        if (isAddressChanged()) fields.add(RescheduleField.ADDRESS)
        return fields
    }

    /**
     * Get summary of changes for display
     */
    fun getChangesSummary(): String {
        val changes = getChangedFields()
        return when {
            changes.isEmpty() -> "No changes"
            changes.size == 1 -> changes.first().getDisplayName()
            else -> "${changes.size} changes"
        }
    }
}
