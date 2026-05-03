package com.eventfinder.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.eventfinder.app.domain.model.EventState
import com.eventfinder.app.domain.repository.EventRepository
import com.eventfinder.app.domain.service.NotificationService
import com.eventfinder.app.domain.model.NotificationType
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background worker that checks for events needing automatic state updates
 * - SCHEDULED → LIVE (when event starts)
 * - LIVE → COMPLETED (when event ends)
 *
 * Runs periodically via WorkManager
 */
@HiltWorker
class EventStateUpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val eventRepository: EventRepository,
    private val notificationService: NotificationService
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val now = System.currentTimeMillis()

            // Update scheduled events that have started
            updateScheduledToLive(now)

            // Update live events that have ended
            updateLiveToCompleted(now)

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error updating event states", e)
            Result.retry()
        }
    }

    private suspend fun updateScheduledToLive(now: Long) {
        val scheduledEvents = eventRepository.getEventsByState(EventState.SCHEDULED)
            .getOrNull() ?: return

        scheduledEvents.forEach { event ->
            // Check if event has started (within 5 min grace period to account for worker timing)
            if (now >= event.startTime - GRACE_PERIOD_MS) {
                eventRepository.updateEventState(
                    eventId = event.id,
                    newState = EventState.LIVE,
                    reason = "Event has started",
                    changedBy = null, // Automatic system update
                    automatic = true
                ).onSuccess {
                    android.util.Log.d(TAG, "Event ${event.id} transitioned to LIVE")

                    // Notify organizer
                    notificationService.notifyEventOrganizer(
                        eventId = event.id,
                        organizerId = event.organizerId,
                        type = NotificationType.EVENT_STARTED,
                        title = "Your event is now live!",
                        message = "${event.title} has started. Good luck!",
                        metadata = emptyMap()
                    )
                }
            }
        }
    }

    private suspend fun updateLiveToCompleted(now: Long) {
        val liveEvents = eventRepository.getEventsByState(EventState.LIVE)
            .getOrNull() ?: return

        liveEvents.forEach { event ->
            val endTime = event.endTime ?: (event.startTime + DEFAULT_EVENT_DURATION_MS)

            // Check if event has ended (with 5 min grace period)
            if (now >= endTime + GRACE_PERIOD_MS) {
                eventRepository.updateEventState(
                    eventId = event.id,
                    newState = EventState.COMPLETED,
                    reason = "Event has ended",
                    changedBy = null,
                    automatic = true
                ).onSuccess {
                    android.util.Log.d(TAG, "Event ${event.id} transitioned to COMPLETED")

                    // Notify organizer
                    notificationService.notifyEventOrganizer(
                        eventId = event.id,
                        organizerId = event.organizerId,
                        type = NotificationType.EVENT_ENDED_MARK_COMPLETE,
                        title = "Event completed!",
                        message = "${event.title} has ended. View attendance summary in your dashboard.",
                        metadata = mapOf(
                            "attendeeCount" to event.currentParticipantCount.toString()
                        )
                    )
                }
            }
        }
    }

    companion object {
        private const val TAG = "EventStateUpdateWorker"
        const val WORK_NAME = "event_state_update_work"

        // Grace period to account for worker timing variations
        private const val GRACE_PERIOD_MS = 5 * 60 * 1000L // 5 minutes

        // Default duration if endTime is not set (3 hours)
        private const val DEFAULT_EVENT_DURATION_MS = 3 * 60 * 60 * 1000L
    }
}
