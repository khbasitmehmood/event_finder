package com.eventfinder.app.domain.model

/**
 * Represents an event cancellation record
 * Tracks cancellation details and refund status
 */
data class EventCancellation(
    val cancelledAt: Long = System.currentTimeMillis(),
    val cancelledBy: String,
    val reason: String,
    val refundStatus: RefundStatus = RefundStatus.NOT_APPLICABLE,
    val notificationSent: Boolean = false,
    val attendeeCount: Int = 0,
    val refundAmount: Double? = null,
    val refundCurrency: String? = null
) {
    /**
     * Check if this was a paid event requiring refunds
     */
    fun requiresRefund(): Boolean = refundStatus != RefundStatus.NOT_APPLICABLE

    /**
     * Check if refunds are still being processed
     */
    fun isRefundPending(): Boolean = refundStatus.isActive()

    /**
     * Check if refunds completed successfully
     */
    fun isRefundComplete(): Boolean = refundStatus.isComplete()

    /**
     * Check if manual intervention is needed
     */
    fun requiresManualAction(): Boolean = refundStatus.requiresAction()

    /**
     * Get summary of cancellation for display
     */
    fun getSummary(): String {
        return buildString {
            append("Cancelled on ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(cancelledAt)}")
            if (attendeeCount > 0) {
                append(" • $attendeeCount attendees")
            }
            if (requiresRefund()) {
                append(" • Refunds: ${refundStatus.getDisplayName()}")
            }
        }
    }
}
