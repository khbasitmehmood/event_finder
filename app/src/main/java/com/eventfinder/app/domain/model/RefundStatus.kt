package com.eventfinder.app.domain.model

/**
 * Status of refund processing for cancelled events
 */
enum class RefundStatus {
    /**
     * Event is free, no refunds needed
     */
    NOT_APPLICABLE,

    /**
     * Refunds are queued for processing
     */
    PENDING,

    /**
     * Refunds are being processed
     */
    PROCESSING,

    /**
     * All refunds completed successfully
     */
    COMPLETED,

    /**
     * Some refunds failed, manual intervention needed
     */
    FAILED;

    fun getDisplayName(): String = when (this) {
        NOT_APPLICABLE -> "Not Applicable"
        PENDING -> "Pending"
        PROCESSING -> "Processing"
        COMPLETED -> "Completed"
        FAILED -> "Failed"
    }

    fun isActive(): Boolean = this in listOf(PENDING, PROCESSING)

    fun isComplete(): Boolean = this == COMPLETED

    fun requiresAction(): Boolean = this == FAILED
}
