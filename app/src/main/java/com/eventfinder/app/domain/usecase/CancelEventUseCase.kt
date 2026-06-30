package com.eventfinder.app.domain.usecase

import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.model.EventCancellation
import com.eventfinder.app.domain.model.RefundStatus
import com.eventfinder.app.domain.repository.EventRepository
import com.eventfinder.app.domain.service.NotificationService
import javax.inject.Inject

/**
 * Use case for cancelling an event
 * Handles cancellation logic and initiates refunds for paid events
 */
class CancelEventUseCase @Inject constructor(
    private val eventRepository: EventRepository,
    private val notificationService: NotificationService
) {
    /**
     * Cancel an event with validation
     *
     * @param eventId ID of the event to cancel
     * @param reason Reason for cancellation (required)
     * @param userId Organizer's user ID
     * @return Result with updated event or error
     */
    suspend operator fun invoke(
        eventId: String,
        reason: String,
        userId: String
    ): Result<Event> {
        return try {
            // Validate reason
            if (reason.isBlank()) {
                return Result.failure(Exception("Reason for cancellation is required"))
            }

            if (reason.length < 10) {
                return Result.failure(Exception("Reason must be at least 10 characters"))
            }

            if (reason.length > 500) {
                return Result.failure(Exception("Reason must not exceed 500 characters"))
            }

            // Get current event
            val currentEventResult = eventRepository.getEventById(eventId)
            val currentEvent = currentEventResult.getOrNull()
                ?: return Result.failure(Exception("Event not found"))

            // Validate event can be cancelled
            if (!currentEvent.canCancel()) {
                return Result.failure(
                    Exception(
                        when {
                            !currentEvent.state.canCancel() -> "Event in ${currentEvent.state.getDisplayName()} state cannot be cancelled"
                            currentEvent.hasEnded() -> "Event has already ended"
                            currentEvent.isCancelled() -> "Event is already cancelled"
                            else -> "Event cannot be cancelled"
                        }
                    )
                )
            }

            // Determine refund status
            val refundStatus = if (!currentEvent.hasPaidTicket()) {
                RefundStatus.NOT_APPLICABLE
            } else {
                // For paid events, initiate refund process
                RefundStatus.PENDING
            }

            // Create cancellation record
            val cancellation = EventCancellation(
                cancelledAt = System.currentTimeMillis(),
                cancelledBy = userId,
                reason = reason.trim(),
                refundStatus = refundStatus,
                notificationSent = false,
                attendeeCount = currentEvent.currentParticipantCount,
                refundAmount = if (currentEvent.hasPaidTicket()) currentEvent.price else null,
                refundCurrency = if (currentEvent.hasPaidTicket()) currentEvent.currency else null
            )

            // Cancel the event
            val result = eventRepository.cancelEvent(
                eventId = eventId,
                cancellation = cancellation
            )

            // Send notifications to attendees
            result.onSuccess { updatedEvent ->
                notificationService.notifyEventCancelled(
                    event = updatedEvent,
                    reason = reason,
                    refundStatus = refundStatus.getDisplayName()
                )
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validate cancellation is allowed
     */
    fun canCancel(event: Event): Boolean = event.canCancel()

    /**
     * Get validation errors for cancellation
     */
    fun getCancelValidationErrors(
        event: Event,
        reason: String
    ): List<String> {
        val errors = mutableListOf<String>()

        if (!event.state.canCancel()) {
            errors.add("Event in ${event.state.getDisplayName()} state cannot be cancelled")
        }

        if (event.hasEnded()) {
            errors.add("Event has already ended")
        }

        if (event.isCancelled()) {
            errors.add("Event is already cancelled")
        }

        if (reason.isBlank()) {
            errors.add("Reason is required")
        } else if (reason.length < 10) {
            errors.add("Reason must be at least 10 characters")
        } else if (reason.length > 500) {
            errors.add("Reason must not exceed 500 characters")
        }

        return errors
    }

    /**
     * Get cancellation impact summary
     */
    fun getCancellationImpact(event: Event): CancellationImpact {
        return CancellationImpact(
            attendeeCount = event.currentParticipantCount,
            requiresRefund = event.hasPaidTicket(),
            refundAmount = if (event.hasPaidTicket()) event.price else null,
            refundCurrency = if (event.hasPaidTicket()) event.currency else null
        )
    }
}

/**
 * Data class representing the impact of cancelling an event
 */
data class CancellationImpact(
    val attendeeCount: Int,
    val requiresRefund: Boolean,
    val refundAmount: Double?,
    val refundCurrency: String?
) {
    fun getImpactSummary(): String {
        return buildString {
            append("$attendeeCount attendee(s) will be notified")
            if (requiresRefund && refundAmount != null) {
                append("\nRefunds of $refundCurrency ${String.format("%.2f", refundAmount)} per ticket will be initiated")
            }
        }
    }
}
