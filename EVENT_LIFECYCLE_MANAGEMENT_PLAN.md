# Event Lifecycle Management - Implementation Plan

## Overview
Add comprehensive event state management, delay/reschedule capabilities, and notification system for event updates.

---

## 1. Event State Management

### Current Problem
- Events only have boolean flags (published/not published)
- No clear lifecycle states
- Can't distinguish between upcoming, ongoing, completed events
- No cancelled/postponed states

### Solution: Event State Enum

```kotlin
enum class EventState {
    DRAFT,          // Event created but not published
    SCHEDULED,      // Event published, waiting to start
    LIVE,           // Event is currently happening
    COMPLETED,      // Event finished successfully
    CANCELLED,      // Event cancelled by organizer
    POSTPONED,      // Event delayed/rescheduled
    EXPIRED         // Event passed without being marked complete
}
```

### State Transitions

```
DRAFT → SCHEDULED (when published)
SCHEDULED → LIVE (when start time reached)
SCHEDULED → POSTPONED (when rescheduled before start)
SCHEDULED → CANCELLED (when cancelled before start)
LIVE → COMPLETED (when manually marked complete or end time reached)
LIVE → CANCELLED (when cancelled during event - rare)
POSTPONED → SCHEDULED (when new date confirmed)
Any state → EXPIRED (if not managed properly)
```

### Auto-State Updates
- Background job checks events every hour
- Updates SCHEDULED → LIVE when startTime reached
- Updates LIVE → COMPLETED when endTime reached
- Updates to EXPIRED if event not managed

---

## 2. Event Delay/Postpone Functionality

### Use Cases
1. **Minor Delay**: Event delayed by hours (same day)
2. **Major Postponement**: Event moved to different date
3. **TBD Postponement**: Date not yet decided

### Data Model

```kotlin
data class EventPostponement(
    val originalStartTime: Long,
    val originalEndTime: Long?,
    val newStartTime: Long?,        // null if TBD
    val newEndTime: Long?,
    val reason: String,
    val postponedAt: Long,
    val postponedBy: String,        // organizer userId
    val notificationSent: Boolean
)

// Add to Event model
data class Event(
    // ... existing fields ...
    val state: EventState = EventState.DRAFT,
    val postponementHistory: List<EventPostponement> = emptyList(),
    val currentPostponement: EventPostponement? = null
)
```

### Business Rules
- Can only postpone events in SCHEDULED or POSTPONED state
- Cannot postpone after event starts (LIVE state)
- Cannot postpone cancelled events
- Must provide reason for postponement
- Minimum delay: 1 hour from current time
- Maximum postponements: 3 times per event

### UI Flow - Organizer

```
ManageEventFragment
└─ More Menu → "Postpone Event"
   └─ PostponeEventBottomSheet
      ├─ Current Date/Time display
      ├─ New Date picker
      ├─ New Time picker
      ├─ "Date TBD" checkbox
      ├─ Reason input (required)
      └─ Confirm button
         └─ Show confirmation dialog
            ├─ "X attendees will be notified"
            └─ Confirm/Cancel
               └─ Update event
                  └─ Send notifications
```

---

## 3. Event Reschedule Functionality

### Difference from Postpone
- **Postpone**: Implies delay, usually same details
- **Reschedule**: Can change date, time, and potentially location

### Data Model

```kotlin
data class EventReschedule(
    val originalStartTime: Long,
    val originalEndTime: Long?,
    val originalLocationName: String?,
    val originalAddress: String?,
    val newStartTime: Long,
    val newEndTime: Long?,
    val newLocationName: String?,
    val newAddress: String?,
    val newLatitude: Double?,
    val newLongitude: Double?,
    val reason: String,
    val rescheduledAt: Long,
    val rescheduledBy: String,
    val notificationSent: Boolean,
    val changedFields: List<RescheduleField>
)

enum class RescheduleField {
    START_TIME,
    END_TIME,
    LOCATION,
    ADDRESS
}

// Add to Event model
data class Event(
    // ... existing fields ...
    val rescheduleHistory: List<EventReschedule> = emptyList(),
    val currentReschedule: EventReschedule? = null
)
```

### Business Rules
- Can reschedule SCHEDULED or POSTPONED events
- Cannot reschedule after event starts
- Cannot reschedule cancelled events
- Must provide reason
- Can change: Date, Time, Location
- Cannot change: Title, Category, Price, Max Participants
- Attendees must be notified of all changes

### UI Flow - Organizer

```
ManageEventFragment
└─ More Menu → "Reschedule Event"
   └─ RescheduleEventBottomSheet
      ├─ Date Section
      │  ├─ Current: [Date] → New: [Date Picker]
      │  └─ Changed indicator
      ├─ Time Section
      │  ├─ Start: [Time] → New: [Time Picker]
      │  └─ End: [Time] → New: [Time Picker]
      ├─ Location Section (optional)
      │  ├─ Current: [Location]
      │  └─ New: [Map Picker]
      ├─ Reason input (required)
      └─ Confirm button
         └─ Show summary of changes
            └─ Notify attendees
```

---

## 4. Event Cancellation

### Data Model

```kotlin
data class EventCancellation(
    val cancelledAt: Long,
    val cancelledBy: String,
    val reason: String,
    val refundStatus: RefundStatus,
    val notificationSent: Boolean
)

enum class RefundStatus {
    NOT_APPLICABLE,     // Free event
    PENDING,            // Refunds being processed
    PROCESSING,         // In progress
    COMPLETED,          // All refunds done
    PARTIAL             // Some refunds failed
}

// Add to Event model
data class Event(
    // ... existing fields ...
    val cancellation: EventCancellation? = null
)
```

### Business Rules
- Can cancel events in SCHEDULED, POSTPONED, or LIVE state
- Cannot cancel COMPLETED events
- Must provide reason
- Automatic refund initiation for paid events
- All attendees notified immediately

---

## 5. Notification System

### Notification Types

```kotlin
enum class EventNotificationType {
    EVENT_PUBLISHED,        // Event goes live
    EVENT_REMINDER_24H,     // 24 hours before
    EVENT_REMINDER_1H,      // 1 hour before
    EVENT_STARTING_NOW,     // Event starting
    EVENT_POSTPONED,        // Event delayed
    EVENT_RESCHEDULED,      // Event time/location changed
    EVENT_CANCELLED,        // Event cancelled
    EVENT_COMPLETED,        // Event finished (feedback request)
    EVENT_UPDATED           // Other updates
}

data class EventNotification(
    val id: String,
    val userId: String,
    val eventId: String,
    val type: EventNotificationType,
    val title: String,
    val message: String,
    val data: Map<String, String>,
    val createdAt: Long,
    val read: Boolean = false,
    val actionRequired: Boolean = false
)
```

### Notification Messages

**Postponed:**
```
Title: "Event Postponed: [Event Name]"
Message: "[Event Name] has been postponed. New date: [New Date] or TBD. Reason: [Reason]"
Action: "View Details"
```

**Rescheduled:**
```
Title: "Event Rescheduled: [Event Name]"
Message: "[Event Name] has been rescheduled to [New Date] at [New Time]. Location: [New Location]. Reason: [Reason]"
Action: "View Changes"
```

**Cancelled:**
```
Title: "Event Cancelled: [Event Name]"
Message: "[Event Name] has been cancelled. Reason: [Reason]. Your refund is being processed."
Action: "View Refund Status"
```

### Notification Channels
1. **In-App Notifications** (NotificationFragment)
2. **Firebase Cloud Messaging** (Push Notifications)
3. **Email Notifications** (Optional)
4. **SMS Notifications** (Optional, premium)

---

## 6. Data Model Updates

### Event.kt - Complete Structure

```kotlin
data class Event(
    // Existing fields
    val id: String = "",
    val eventId: String = "",
    val title: String,
    val description: String? = null,
    val category: EventCategory? = null,
    val organizerId: String,
    val organizerName: String,
    val startTime: Long,
    val endTime: Long?,
    val location: EventLocation,
    val address: String?,
    val maxParticipants: Int?,
    val currentParticipantCount: Int = 0,
    val isFree: Boolean,
    val price: Double?,
    val currency: String?,
    val imageUrls: List<String> = emptyList(),
    val mainImageUrl: String?,
    val tags: List<String> = emptyList(),
    val visibility: EventVisibility = EventVisibility.PUBLIC,
    val requiresTicket: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long?,
    
    // NEW FIELDS
    val state: EventState = EventState.DRAFT,
    val publishedAt: Long? = null,
    val completedAt: Long? = null,
    
    // Lifecycle tracking
    val stateHistory: List<StateChange> = emptyList(),
    
    // Postponement
    val postponementHistory: List<EventPostponement> = emptyList(),
    val currentPostponement: EventPostponement? = null,
    
    // Rescheduling
    val rescheduleHistory: List<EventReschedule> = emptyList(),
    val currentReschedule: EventReschedule? = null,
    
    // Cancellation
    val cancellation: EventCancellation? = null,
    
    // Notification tracking
    val notificationsSent: List<NotificationLog> = emptyList(),
    
    // Metadata
    val allowPostponement: Boolean = true,
    val allowRescheduling: Boolean = true,
    val maxPostponements: Int = 3,
    val postponementCount: Int = 0
)

data class StateChange(
    val fromState: EventState,
    val toState: EventState,
    val changedAt: Long,
    val changedBy: String?,
    val reason: String?,
    val automatic: Boolean = false
)

data class NotificationLog(
    val type: EventNotificationType,
    val sentAt: Long,
    val recipientCount: Int,
    val successful: Int,
    val failed: Int
)
```

---

## 7. Repository Methods

### EventRepository.kt - New Methods

```kotlin
interface EventRepository {
    // Existing methods...
    
    // State management
    suspend fun updateEventState(
        eventId: String,
        newState: EventState,
        reason: String? = null,
        automatic: Boolean = false
    ): Result<Event>
    
    suspend fun getEventsByState(state: EventState): Result<List<Event>>
    
    // Postponement
    suspend fun postponeEvent(
        eventId: String,
        newStartTime: Long?,
        newEndTime: Long?,
        reason: String,
        organizerId: String
    ): Result<Event>
    
    // Rescheduling
    suspend fun rescheduleEvent(
        eventId: String,
        reschedule: EventReschedule
    ): Result<Event>
    
    // Cancellation
    suspend fun cancelEvent(
        eventId: String,
        reason: String,
        organizerId: String
    ): Result<Event>
    
    // Completion
    suspend fun markEventComplete(
        eventId: String,
        organizerId: String
    ): Result<Event>
    
    // Batch operations
    suspend fun updateExpiredEvents(): Result<Int>
    suspend fun updateLiveEvents(): Result<Int>
    suspend fun updateCompletedEvents(): Result<Int>
}
```

---

## 8. Use Cases

### New Use Cases to Create

```kotlin
// 1. PostponeEventUseCase.kt
class PostponeEventUseCase @Inject constructor(
    private val eventRepository: EventRepository,
    private val notificationService: NotificationService,
    private val ticketRepository: TicketRepository
) {
    suspend operator fun invoke(
        eventId: String,
        newStartTime: Long?,
        newEndTime: Long?,
        reason: String,
        organizerId: String
    ): Result<Event>
}

// 2. RescheduleEventUseCase.kt
class RescheduleEventUseCase @Inject constructor(
    private val eventRepository: EventRepository,
    private val notificationService: NotificationService,
    private val ticketRepository: TicketRepository
) {
    suspend operator fun invoke(
        eventId: String,
        reschedule: EventReschedule
    ): Result<Event>
}

// 3. CancelEventUseCase.kt
class CancelEventUseCase @Inject constructor(
    private val eventRepository: EventRepository,
    private val notificationService: NotificationService,
    private val ticketRepository: TicketRepository,
    private val refundService: RefundService
) {
    suspend operator fun invoke(
        eventId: String,
        reason: String,
        organizerId: String
    ): Result<CancellationResult>
}

// 4. UpdateEventStateUseCase.kt
class UpdateEventStateUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(
        eventId: String,
        newState: EventState,
        reason: String? = null
    ): Result<Event>
}

// 5. UpdateEventLifecycleUseCase.kt
class UpdateEventLifecycleUseCase @Inject constructor(
    private val eventRepository: EventRepository,
    private val notificationService: NotificationService
) {
    // Background job to update event states
    suspend fun updateAllEvents(): Result<UpdateSummary>
}
```

---

## 9. UI Components

### A. Organizer UI

#### ManageEventFragment - New Actions

```xml
<!-- Add to options menu -->
<menu>
    <item android:id="@+id/action_postpone"
          android:title="Postpone Event"
          android:visible="@{viewModel.canPostpone}" />
          
    <item android:id="@+id/action_reschedule"
          android:title="Reschedule Event"
          android:visible="@{viewModel.canReschedule}" />
          
    <item android:id="@+id/action_cancel"
          android:title="Cancel Event"
          android:visible="@{viewModel.canCancel}" />
          
    <item android:id="@+id/action_mark_complete"
          android:title="Mark as Completed"
          android:visible="@{viewModel.canComplete}" />
</menu>
```

#### PostponeEventBottomSheet.kt

```kotlin
class PostponeEventBottomSheet(
    private val event: Event,
    private val onPostpone: (newStartTime: Long?, newEndTime: Long?, reason: String) -> Unit
) : BottomSheetDialogFragment() {
    
    // UI Components:
    // - Current date/time display
    // - New date picker
    // - New time pickers
    // - "Date TBD" checkbox
    // - Reason input (required, max 500 chars)
    // - Attendee count display
    // - Postpone button
}
```

#### RescheduleEventBottomSheet.kt

```kotlin
class RescheduleEventBottomSheet(
    private val event: Event,
    private val onReschedule: (reschedule: EventReschedule) -> Unit
) : BottomSheetDialogFragment() {
    
    // UI Components:
    // - Current details section
    // - New date/time section
    // - New location section (optional)
    // - Changes summary
    // - Reason input
    // - Attendee count display
    // - Reschedule button
}
```

#### Event State Badge

```xml
<!-- Show in event cards and detail screen -->
<com.google.android.material.chip.Chip
    android:id="@+id/chipEventState"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:chipBackgroundColor="@{viewModel.stateColor}"
    android:text="@{viewModel.stateText}" />
```

### B. User/Attendee UI

#### Event Detail Screen - State Indicators

```xml
<!-- Show postponement notice -->
<MaterialCardView
    android:visibility="@{event.state == EventState.POSTPONED}"
    app:cardBackgroundColor="@color/warning_container">
    
    <LinearLayout>
        <ImageView android:src="@drawable/ic_schedule" />
        <TextView android:text="This event has been postponed" />
        <TextView android:text="@{event.currentPostponement.reason}" />
        <TextView android:text="New date: TBD or [date]" />
    </LinearLayout>
</MaterialCardView>

<!-- Show cancellation notice -->
<MaterialCardView
    android:visibility="@{event.state == EventState.CANCELLED}"
    app:cardBackgroundColor="@color/error_container">
    
    <LinearLayout>
        <ImageView android:src="@drawable/ic_cancel" />
        <TextView android:text="This event has been cancelled" />
        <TextView android:text="@{event.cancellation.reason}" />
        <TextView android:text="Refund status: [status]" />
    </LinearLayout>
</MaterialCardView>
```

#### NotificationFragment - Event Updates

```kotlin
// Show event update notifications prominently
class NotificationAdapter : ListAdapter<Notification, ViewHolder> {
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = getItem(position)
        
        when (notification.type) {
            POSTPONED, RESCHEDULED, CANCELLED -> {
                // Show with warning/error styling
                // Add action button
                // Mark as important
            }
        }
    }
}
```

---

## 10. Background Jobs

### EventLifecycleWorker.kt

```kotlin
@HiltWorker
class EventLifecycleWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val updateEventLifecycleUseCase: UpdateEventLifecycleUseCase
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val summary = updateEventLifecycleUseCase.updateAllEvents()
            
            // Log results
            Log.d("EventLifecycle", "Updated ${summary.totalUpdated} events")
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
    
    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<EventLifecycleWorker>(
                1, TimeUnit.HOURS // Run every hour
            ).setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            ).build()
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "event_lifecycle_update",
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }
    }
}
```

---

## 11. Implementation Order

### Phase 1: Core State Management (Priority: CRITICAL)
**Files to Create:**
1. ✅ EventState.kt (enum)
2. ✅ StateChange.kt (data class)
3. ✅ Update Event.kt model

**Files to Modify:**
4. ✅ EventRepository.kt (add state methods)
5. ✅ FirestoreEventDataSource.kt (implement state updates)
6. ✅ UpdateEventStateUseCase.kt (create)

**Testing:**
- Unit tests for state transitions
- Repository tests

**Time Estimate:** 2-3 hours

---

### Phase 2: Postponement (Priority: HIGH) ✅ COMPLETED
**Files Created:**
1. ✅ EventPostponement.kt (data class) - DONE
2. ✅ PostponeEventUseCase.kt - DONE
3. ✅ PostponeEventBottomSheet.kt - DONE
4. ✅ bottom_sheet_postpone_event.xml - DONE
5. ✅ ic_calendar.xml (drawable) - DONE
6. ✅ ic_info.xml (drawable) - DONE

**Files Modified:**
7. ✅ Event.kt (add postponement fields) - DONE
8. ✅ EventRepository.kt (add postpone method) - DONE
9. ✅ EventRepositoryImpl.kt - DONE
10. ✅ EventDataSource.kt - DONE
11. ✅ FirestoreEventDataSource.kt (implement postponeEvent) - DONE
12. ✅ DummyEventDataSource.kt (dummy implementation) - DONE
13. ✅ EventDto.kt (add postponement fields) - DONE
14. ✅ EventMapper.kt (map postponement fields) - DONE
15. ✅ ManageEventSharedViewModel.kt (add postponeEvent method) - DONE
16. ✅ ManageEventFragment.kt (add postpone action) - DONE
17. ✅ bottom_sheet_event_actions.xml (add postpone button) - DONE

**Testing:**
- ✅ Postpone validations (reason length, state check, max postponements)
- ✅ Date picker functionality (TBD or specific date)
- ✅ Minimum 1 hour from now validation
- ✅ End time after start time validation
- ⏳ Notification triggering (Phase 5)

**Implementation Details:**
- Supports both specific date/time and TBD postponements
- Stores full postponement history
- Increments postponement count with max limit of 3
- Updates event state to POSTPONED
- Adds state change to history
- Comprehensive validation in use case layer
- User-friendly bottom sheet with toggle for TBD/specific date
- Date and time pickers with minimum date validation
- Real-time button state updates based on validation

**Time Taken:** ~3 hours

---

### Phase 3: Rescheduling (Priority: HIGH) ✅ COMPLETED
**Files Created:**
1. ✅ RescheduleField.kt (enum) - DONE
2. ✅ EventReschedule.kt (data class with helper methods) - DONE
3. ✅ RescheduleEventUseCase.kt (with validation) - DONE
4. ✅ RescheduleEventBottomSheet.kt - DONE
5. ✅ bottom_sheet_reschedule_event.xml - DONE

**Files Modified:**
6. ✅ Event.kt (add reschedule fields and methods) - DONE
7. ✅ EventRepository.kt (add reschedule method) - DONE
8. ✅ EventRepositoryImpl.kt - DONE
9. ✅ EventDataSource.kt - DONE
10. ✅ FirestoreEventDataSource.kt (implement rescheduleEvent) - DONE
11. ✅ DummyEventDataSource.kt (dummy implementation) - DONE
12. ✅ EventDto.kt (add reschedule fields) - DONE
13. ✅ EventMapper.kt (map reschedule fields) - DONE
14. ✅ ManageEventSharedViewModel.kt (add rescheduleEvent method) - DONE
15. ✅ ManageEventFragment.kt (add reschedule action) - DONE
16. ✅ bottom_sheet_event_actions.xml (add reschedule button) - DONE

**Testing:**
- ✅ Multiple field changes (date, time, location, address)
- ✅ Change summary display with live updates
- ✅ Location toggle with address input
- ⏳ Location picker integration (placeholder - uses current location for now)
- ✅ Validation: reason length, min 1 hour from now, end after start
- ✅ Changes detection (prevents reschedule with no changes)

**Implementation Details:**
- Supports changing start time, end time, location, and address
- Stores full reschedule history
- Increments reschedule count with max limit of 5
- Updates event state back to SCHEDULED if it was POSTPONED
- Clears postponement data when rescheduled
- Adds state change to history
- Comprehensive validation in use case layer
- User-friendly bottom sheet with current details display
- Real-time changes summary
- Location toggle for optional location changes
- Date and time pickers with minimum date validation
- Real-time button state updates based on validation

**Time Taken:** ~2 hours

---

### Phase 4: Cancellation (Priority: HIGH) ✅ COMPLETED
**Files Created:**
1. ✅ RefundStatus.kt (enum with 5 states) - DONE
2. ✅ EventCancellation.kt (data class with helper methods) - DONE
3. ✅ CancelEventUseCase.kt (with validation and impact calculation) - DONE
4. ✅ CancelEventDialog.kt - DONE
5. ✅ dialog_cancel_event.xml - DONE

**Files Modified:**
6. ✅ Event.kt (add cancellation fields and methods) - DONE
7. ✅ EventRepository.kt (add cancelEvent method) - DONE
8. ✅ EventRepositoryImpl.kt - DONE
9. ✅ EventDataSource.kt - DONE
10. ✅ FirestoreEventDataSource.kt (implement cancelEvent) - DONE
11. ✅ DummyEventDataSource.kt (dummy implementation) - DONE
12. ✅ EventDto.kt (add cancellation fields) - DONE
13. ✅ EventMapper.kt (map cancellation fields) - DONE
14. ✅ ManageEventSharedViewModel.kt (add cancelEvent method) - DONE
15. ✅ ManageEventFragment.kt (add cancel action) - DONE
16. ✅ bottom_sheet_event_actions.xml (make cancel button clickable) - DONE

**Testing:**
- ✅ Cancellation confirmation dialog with impact summary
- ✅ Refund status tracking (NOT_APPLICABLE for free, PENDING for paid)
- ✅ State transitions to CANCELLED
- ✅ Validation: reason length, can't cancel ended/cancelled events
- ✅ Impact calculation: attendee count, refund amounts

**Implementation Details:**
- Supports cancelling SCHEDULED, POSTPONED, and LIVE events
- Cannot cancel DRAFT, COMPLETED, CANCELLED, or EXPIRED events
- Tracks refund status with 5 states: NOT_APPLICABLE, PENDING, PROCESSING, COMPLETED, FAILED
- Stores cancellation details including attendee count and refund amounts
- Updates event state to CANCELLED
- Adds state change to history
- Dialog shows impact summary (attendees affected, refund amounts)
- Comprehensive validation in use case layer
- Material 3 dialog with error color scheme
- Real-time button state based on validation

**Time Taken:** ~1.5 hours

---

### Phase 5: Notification System (Priority: MEDIUM)
**Files to Create:**
1. ✅ EventNotificationType.kt (enum)
2. ✅ EventNotification.kt (data class)
3. ✅ NotificationLog.kt (data class)
4. ✅ EventNotificationService.kt
5. ✅ NotificationRepository.kt

**Files to Modify:**
6. ✅ All use cases (trigger notifications)
7. ✅ EventDetailFragment.kt (show notices)

**Testing:**
- Notification creation
- In-app display
- FCM integration (future)

**Time Estimate:** 4-5 hours

---

### Phase 6: Background Jobs (Priority: MEDIUM) ✅ COMPLETED
**Status:** ✅ Fully Implemented

**Files Created:**
1. ✅ EventStateUpdateWorker.kt - Background worker for automatic state updates
2. ✅ WorkManagerInitializer.kt - Schedules periodic workers
3. ✅ HiltWorkerConfiguration.kt - Hilt + WorkManager integration
4. ✅ EventStateUpdateTrigger.kt - Manual trigger for testing

**Files Modified:**
5. ✅ EventFinderApplication.kt - Initialize WorkManager on app startup
6. ✅ build.gradle.kts - Added WorkManager dependencies
7. ✅ libs.versions.toml - Added WorkManager versions

**Features Implemented:**
- ✅ Automatic SCHEDULED → LIVE transition when event starts
- ✅ Automatic LIVE → COMPLETED transition when event ends
- ✅ Periodic worker runs every 15 minutes
- ✅ Notifies organizers when events start and end
- ✅ Uses Hilt for dependency injection in workers
- ✅ Network connectivity constraint (requires internet)
- ✅ 5-minute grace period to account for timing variations
- ✅ Manual trigger available for testing

**Implementation Details:**
- WorkManager PeriodicWorkRequest with 15-minute interval
- HiltWorker annotation enables @Inject in Worker classes
- Configuration.Provider interface in Application class for Hilt integration
- Fetches events by state (SCHEDULED, LIVE) and checks timestamps
- Updates event state via repository
- Sends notifications via NotificationService
- Logs all state transitions for debugging
- ExistingPeriodicWorkPolicy.KEEP prevents duplicate schedules

**Notification Integration:**
- EVENT_STARTED notification sent to organizer when SCHEDULED → LIVE
- EVENT_ENDED_MARK_COMPLETE notification sent when LIVE → COMPLETED
- Metadata includes attendee count for completed events

**Testing:**
- Worker execution via WorkManager periodic schedule
- Manual trigger via EventStateUpdateTrigger.triggerImmediateUpdate()
- View logs with tag "EventStateUpdateWorker"

**Time Taken:** ~1.5 hours

---

### Phase 7: Notification UI & Badge Display (Priority: MEDIUM) ✅ COMPLETED
**Status:** ✅ Fully Implemented

**Files Created:**
1. ✅ NotificationsFragment.kt - Main notifications screen
2. ✅ NotificationsViewModel.kt - ViewModel with StateFlow
3. ✅ NotificationAdapter.kt - RecyclerView adapter with DiffUtil
4. ✅ fragment_notifications.xml - Layout with sections and states
5. ✅ item_notification.xml - Notification item card layout
6. ✅ menu_notifications.xml - Menu with "Mark all as read"
7. ✅ BadgeUtils.kt - Utility for managing badges

**Files Modified:**
8. ✅ user_main_graph.xml - Added notificationsFragment destination
9. ✅ organizer_main_graph.xml - Added notificationsFragment destination
10. ✅ fragment_home.xml - Added notification icon button
11. ✅ HomeFragment.kt - Added navigation to notifications
12. ✅ colors.xml - Added notification colors

**Features Implemented:**
- ✅ Full notification list UI with unread/read sections
- ✅ Pull-to-refresh support
- ✅ Mark single notification as read on tap
- ✅ Mark all as read menu action
- ✅ Unread indicator (blue dot) on cards
- ✅ Priority badges for HIGH/URGENT notifications
- ✅ Event image/icon display
- ✅ Relative time display (2h ago, 1d ago)
- ✅ Empty state with helpful message
- ✅ Loading state with progress indicator
- ✅ Error state handling
- ✅ Notification icon in HomeFragment header
- ✅ Navigation from home to notifications screen

**UI/UX Details:**
- Unread notifications have light blue background (#F5F7FF)
- Two-section layout: "Unread" and "Earlier"
- Material 3 card design with 12dp corner radius
- Priority chips with color coding:
  - URGENT: Red background (#FEE2E2)
  - HIGH: Yellow background (#FEF3C7)
- Dynamic icon based on notification type
- Event title and image shown when available
- Smooth scrolling with NestedScrollView
- SwipeRefreshLayout integration

**State Management:**
- Uses sealed class NotificationsUiState (Loading, Empty, Success, Error)
- StateFlow for reactive updates
- Separate lists for unread vs all notifications
- Unread count tracking in ViewModel

**Future Enhancements (TODO):**
- Deep linking to event detail from notification
- Delete notification with swipe gesture
- Filter notifications by type
- Notification settings screen
- Toolbar badge with unread count

**Time Taken:** ~2 hours

---

## 12. Total Estimates

**Total Implementation Time:** 20-27 hours

**Phases Breakdown:**
- Phase 1 (State): 2-3 hours
- Phase 2 (Postpone): 3-4 hours
- Phase 3 (Reschedule): 4-5 hours
- Phase 4 (Cancel): 2-3 hours
- Phase 5 (Notifications): 4-5 hours
- Phase 6 (Background): 2-3 hours
- Phase 7 (Polish): 3-4 hours

**Recommended Approach:**
- Day 1: Phase 1 + Phase 2 (5-7 hours)
- Day 2: Phase 3 + Phase 4 (6-8 hours)
- Day 3: Phase 5 + Phase 6 (6-8 hours)
- Day 4: Phase 7 + Testing (3-4 hours)

---

## 13. Success Criteria

### Functional
- ✅ Events automatically transition states
- ✅ Organizers can postpone events
- ✅ Organizers can reschedule events
- ✅ Organizers can cancel events
- ✅ Attendees receive notifications
- ✅ State history tracked
- ✅ Refunds initiated on cancellation

### Non-Functional
- ✅ State updates happen within 1 hour
- ✅ Notifications sent within 5 minutes
- ✅ UI reflects state changes immediately
- ✅ No duplicate notifications
- ✅ All operations logged

### User Experience
- ✅ Clear state indicators
- ✅ Easy postpone/reschedule flow
- ✅ Informative error messages
- ✅ Confirmation dialogs for destructive actions
- ✅ History of changes visible

---

## 14. Risks & Mitigations

### Risk 1: Notification Spam
**Mitigation:**
- Rate limiting (max 1 notification per event per hour)
- Batch notifications for multiple changes
- User preferences for notification types

### Risk 2: State Conflicts
**Mitigation:**
- Transaction-based state updates
- Optimistic locking with version field
- Retry logic for conflicts

### Risk 3: Refund Failures
**Mitigation:**
- Async refund processing
- Manual refund fallback
- Admin dashboard for refund management

### Risk 4: Performance
**Mitigation:**
- Index on event state field
- Batch background updates
- Pagination for large event lists

---

## Ready to Start?

**Next Steps:**
1. ✅ Review and approve this plan
2. ✅ Start with Phase 1: Core State Management
3. ✅ Create EventState enum
4. ✅ Update Event model
5. ✅ Implement repository methods
6. ✅ Add UI indicators

Let me know if you want me to:
- Proceed with Phase 1 implementation
- Modify any part of the plan
- Add more details to specific phases
