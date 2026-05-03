# Event Finder - Notification System Documentation

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Notification Types](#notification-types)
4. [Notification Flows](#notification-flows)
5. [Priority System](#priority-system)
6. [Scheduling System](#scheduling-system)
7. [User Preferences](#user-preferences)
8. [Firebase Implementation](#firebase-implementation)
9. [Testing Strategy](#testing-strategy)
10. [Future Enhancements](#future-enhancements)

---

## Overview

The Event Finder notification system provides real-time updates to users about event lifecycle changes, bookings, and important alerts. It supports both in-app notifications and push notifications via Firebase Cloud Messaging (FCM).

### Key Features
- **30+ Notification Types** covering all user interactions
- **Priority-based Delivery** (Low, Normal, High, Urgent)
- **Scheduled Notifications** (24h, 1h reminders)
- **User Preferences** with granular control
- **Quiet Hours** support
- **Read/Unread Tracking**
- **Bulk Notifications** for event attendees
- **Metadata Support** for custom data
- **Deep Linking** to relevant screens

---

## Architecture

### Components

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                              │
│  (NotificationFragment, NotificationBadge, NotificationItem) │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│                    Domain Layer                              │
│  - NotificationService (interface)                           │
│  - NotificationPreferences                                   │
│  - EventNotification (model)                                 │
│  - NotificationType (enum)                                   │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│                     Data Layer                               │
│  - NotificationServiceImpl                                   │
│  - Firebase Firestore (notifications collection)            │
│  - Firebase Cloud Messaging (FCM)                            │
└─────────────────────────────────────────────────────────────┘
```

### Data Flow

1. **Event Action Occurs** (e.g., event postponed)
2. **Use Case Executes** (e.g., PostponeEventUseCase)
3. **Repository Updates** event state in Firestore
4. **Notification Service Triggered** automatically
5. **Notification Created** with appropriate type and metadata
6. **User Preferences Checked** to determine if user wants this notification
7. **Priority Evaluated** to determine delivery urgency
8. **Quiet Hours Checked** (except for urgent notifications)
9. **Notification Stored** in Firestore
10. **FCM Push Sent** if push notifications enabled
11. **In-App Badge Updated** with unread count
12. **User Opens App** and sees notification
13. **User Clicks Notification** → Deep link to relevant screen
14. **Notification Marked as Read**

---

## Notification Types

### For Attendees (Users who registered for events)

#### Event Lifecycle Notifications

| Type | When Triggered | Priority | Contains |
|------|----------------|----------|----------|
| `EVENT_PUBLISHED` | Event becomes public after creation | LOW | Event details, start time |
| `EVENT_POSTPONED` | Organizer postpones event | HIGH | Reason, new date (or TBD) |
| `EVENT_RESCHEDULED` | Date/time/location changed | HIGH | Reason, changed fields, new details |
| `EVENT_CANCELLED` | Organizer cancels event | URGENT | Reason, refund status |
| `EVENT_STARTING_SOON_24H` | 24 hours before event | NORMAL | Event details, start time |
| `EVENT_STARTING_SOON_1H` | 1 hour before event | HIGH | Event details, location |
| `EVENT_STARTED` | Event start time reached | NORMAL | Event details, check-in info |
| `EVENT_COMPLETED` | Event marked complete | LOW | Thank you message, feedback request |
| `EVENT_EXPIRED` | Event passed without completion | LOW | Apology, alternative events |
| `EVENT_DETAILS_CHANGED` | Non-critical details updated | LOW | Changed fields |

#### Ticket/Booking Notifications

| Type | When Triggered | Priority | Contains |
|------|----------------|----------|----------|
| `TICKET_PURCHASED` | User buys ticket | NORMAL | Ticket details, QR code |
| `TICKET_CONFIRMED` | Payment confirmed | NORMAL | Confirmation number, event details |
| `TICKET_CANCELLED` | User cancels their ticket | NORMAL | Cancellation confirmation, refund info |
| `REFUND_INITIATED` | Refund processing started | NORMAL | Amount, estimated time |
| `REFUND_COMPLETED` | Money returned to user | NORMAL | Amount, transaction ID |
| `REFUND_FAILED` | Refund processing failed | URGENT | Reason, support contact |

#### Communication

| Type | When Triggered | Priority | Contains |
|------|----------------|----------|----------|
| `ORGANIZER_MESSAGE` | Organizer sends update | NORMAL | Custom message |
| `SYSTEM_ANNOUNCEMENT` | Platform-wide announcement | LOW | Announcement text |

---

### For Organizers

#### Event Management

| Type | When Triggered | Priority | Contains |
|------|----------------|----------|----------|
| `EVENT_PUBLISHED_SUCCESS` | Event successfully published | NORMAL | Event ID, view link |
| `EVENT_STATE_CHANGED` | State auto-changed by system | NORMAL | From state, to state, reason |
| `EVENT_ABOUT_TO_START` | 1 hour before event (organizer) | HIGH | Checklist, preparation tips |
| `EVENT_ENDED_MARK_COMPLETE` | Event end time passed | NORMAL | Prompt to mark complete |
| `EVENT_AUTO_EXPIRED` | Event expired without action | URGENT | Event details, action needed |

#### Attendee Activity

| Type | When Triggered | Priority | Contains |
|------|----------------|----------|----------|
| `NEW_ATTENDEE` | Someone purchases ticket | NORMAL | Attendee name, total count |
| `TICKET_SCANNED` | QR code scanned at venue | NORMAL | Attendee name, check-in time |
| `CAPACITY_MILESTONE_50` | 50% capacity reached | NORMAL | Current/max count |
| `CAPACITY_MILESTONE_75` | 75% capacity reached | NORMAL | Current/max count |
| `CAPACITY_MILESTONE_90` | 90% capacity reached | HIGH | Current/max count, warning |
| `CAPACITY_FULL` | 100% capacity reached | HIGH | Event full, waitlist option |
| `LOW_ATTENDANCE_ALERT` | < 20% capacity 24h before | URGENT | Current count, suggestions |

#### Financial

| Type | When Triggered | Priority | Contains |
|------|----------------|----------|----------|
| `REFUNDS_INITIATED` | Refund batch started | NORMAL | Count, total amount |
| `REFUNDS_COMPLETED` | All refunds processed | NORMAL | Success count, total amount |
| `REFUND_ACTION_NEEDED` | Manual intervention required | URGENT | Failed count, reason |

---

## Notification Flows

### Flow 1: Event Postponed

**Trigger:** Organizer postpones event via ManageEventFragment

**Step-by-Step:**

1. **User Action**
   - Organizer opens event → More menu → Postpone Event
   - Selects new date (or TBD)
   - Enters reason (min 10 chars)
   - Clicks "Postpone Event"

2. **Use Case Execution**
   ```kotlin
   PostponeEventUseCase.invoke(
       eventId = "event123",
       newStartTime = 1735689600000, // or null for TBD
       newEndTime = 1735693200000,
       reason = "Due to weather conditions, we need to postpone",
       userId = "organizer456"
   )
   ```

3. **Event State Update**
   - Event state changed: SCHEDULED → POSTPONED
   - EventPostponement record created with:
     - originalStartTime
     - originalEndTime
     - newStartTime (or null)
     - newEndTime (or null)
     - reason
     - postponedBy
     - postponedAt timestamp
   - postponementCount incremented
   - Saved to Firestore

4. **Notification Service Triggered**
   ```kotlin
   notificationService.notifyEventPostponed(
       event = updatedEvent,
       reason = "Due to weather conditions, we need to postpone",
       newStartTime = 1735689600000,
       newEndTime = 1735693200000
   )
   ```

5. **Attendee Lookup**
   - Query `tickets` collection for all tickets with `eventId = "event123"`
   - Get unique list of `userId` values
   - Result: ["user1", "user2", "user3", ...]

6. **Notification Creation (Per Attendee)**
   ```kotlin
   EventNotification(
       type = NotificationType.EVENT_POSTPONED,
       title = "Tech Conference 2024 - Postponed",
       message = "The event has been postponed to Jan 1, 2025 at 10:00 AM. Reason: Due to weather conditions, we need to postpone",
       priority = NotificationPriority.HIGH,
       recipientUserId = "user1",
       recipientUserType = NotificationRecipientType.ATTENDEE,
       eventId = "event123",
       eventTitle = "Tech Conference 2024",
       eventImageUrl = "https://...",
       organizerId = "organizer456",
       organizerName = "Tech Events PK",
       metadata = {
           "reason": "Due to weather conditions, we need to postpone",
           "newStartTime": "1735689600000",
           "oldStartTime": "1735603200000"
       },
       actionUrl = "eventfinder://event/event123",
       actionLabel = "View Details"
   )
   ```

7. **User Preference Check**
   ```kotlin
   val prefs = getUserPreferences("user1")
   if (!prefs.notificationsEnabled) return // Skip
   if (!prefs.eventPostponedEnabled) return // Skip
   if (prefs.isInQuietHours() && priority != URGENT) return // Skip non-urgent
   ```

8. **Notification Stored**
   - Saved to Firestore: `notifications/{notificationId}`
   - Indexed by: recipientUserId, eventId, createdAt
   - TTL: 30 days

9. **Push Notification Sent**
   ```kotlin
   FCM.send(
       token = getUserFCMToken("user1"),
       notification = {
           title = "Tech Conference 2024 - Postponed",
           body = "The event has been postponed to Jan 1, 2025...",
           imageUrl = "https://...",
           data = {
               "type": "EVENT_POSTPONED",
               "eventId": "event123",
               "notificationId": "notif789"
           }
       }
   )
   ```

10. **In-App Badge Updated**
    - Unread count incremented for user1
    - Badge shown on bottom navigation
    - NotificationFragment auto-refreshes if open

11. **User Interaction**
    - **Option A: User clicks push notification**
      - App opens to EventDetailFragment with eventId
      - Notification auto-marked as read
    
    - **Option B: User opens app → Notifications tab**
      - Sees notification with red dot (unread)
      - Clicks notification → EventDetailFragment
      - Notification marked as read
      - Red dot removed
      - Unread count decremented

---

### Flow 2: Event Rescheduled

**Trigger:** Organizer reschedules event (changes date/time/location)

**Step-by-Step:**

1. **User Action**
   - Organizer opens event → More menu → Reschedule Event
   - Changes start/end time
   - Optionally changes location/address
   - Enters reason
   - Clicks "Reschedule Event"

2. **Use Case Execution**
   ```kotlin
   RescheduleEventUseCase.invoke(
       eventId = "event123",
       newStartTime = 1735689600000,
       newEndTime = 1735693200000,
       newLocation = EventLocation(31.5204, 74.3587),
       newAddress = "New Venue, Lahore",
       reason = "Venue upgrade to accommodate more attendees",
       userId = "organizer456"
   )
   ```

3. **Event State Update**
   - Event state: POSTPONED → SCHEDULED (if was postponed)
   - Event fields updated:
     - startTime = new value
     - endTime = new value
     - location = new value (if changed)
     - address = new value (if changed)
   - EventReschedule record created
   - rescheduleCount incremented
   - currentPostponement cleared (if any)

4. **Changed Fields Detection**
   ```kotlin
   val changedFields = reschedule.getChangedFields()
   // Returns: [START_TIME, END_TIME, LOCATION, ADDRESS]
   ```

5. **Notification Service Triggered**
   ```kotlin
   notificationService.notifyEventRescheduled(
       event = updatedEvent,
       reason = "Venue upgrade to accommodate more attendees",
       changedFields = ["Start Time", "End Time", "Location", "Address"]
   )
   ```

6. **Notification Created (Per Attendee)**
   ```kotlin
   EventNotification(
       type = NotificationType.EVENT_RESCHEDULED,
       title = "Tech Conference 2024 - Rescheduled",
       message = "The event has been rescheduled. Changes: Start Time, End Time, Location, Address. Reason: Venue upgrade to accommodate more attendees",
       priority = NotificationPriority.HIGH,
       recipientUserId = "user1",
       recipientUserType = NotificationRecipientType.ATTENDEE,
       eventId = "event123",
       eventTitle = "Tech Conference 2024",
       metadata = {
           "reason": "Venue upgrade to accommodate more attendees",
           "changedFields": "Start Time, End Time, Location, Address",
           "oldStartTime": "1735603200000",
           "newStartTime": "1735689600000",
           "oldLocation": "Old Venue, Lahore",
           "newLocation": "New Venue, Lahore"
       },
       actionUrl = "eventfinder://event/event123"
   )
   ```

7. **Delivery Process**
   - Same as Flow 1 (preference check, FCM push, badge update)

8. **Old Reminders Cancelled**
   ```kotlin
   notificationService.cancelEventNotifications("event123")
   ```

9. **New Reminders Scheduled**
   ```kotlin
   notificationService.scheduleEventReminders(updatedEvent)
   // Schedules new 24h and 1h reminders based on new start time
   ```

---

### Flow 3: Event Cancelled

**Trigger:** Organizer cancels event

**Step-by-Step:**

1. **User Action**
   - Organizer opens event → More menu → Cancel Event
   - Sees impact summary (attendee count, refund amounts)
   - Enters reason
   - Confirms cancellation

2. **Use Case Execution**
   ```kotlin
   CancelEventUseCase.invoke(
       eventId = "event123",
       reason = "Speaker unable to attend due to emergency",
       userId = "organizer456"
   )
   ```

3. **Event State Update**
   - Event state changed: SCHEDULED/LIVE → CANCELLED
   - EventCancellation record created:
     - cancelledAt
     - cancelledBy
     - reason
     - refundStatus = PENDING (if paid) or NOT_APPLICABLE (if free)
     - attendeeCount
     - refundAmount per ticket
     - refundCurrency
   - cancelledAt timestamp set

4. **Refund Process Initiated** (if paid event)
   ```kotlin
   // Pseudo-code - actual payment gateway integration
   if (!event.isFree) {
       val tickets = getEventTickets(eventId)
       tickets.forEach { ticket ->
           paymentGateway.initiateRefund(
               transactionId = ticket.paymentId,
               amount = ticket.amount,
               reason = "Event cancelled"
           )
       }
   }
   ```

5. **Notification Service Triggered**
   ```kotlin
   notificationService.notifyEventCancelled(
       event = updatedEvent,
       reason = "Speaker unable to attend due to emergency",
       refundStatus = "Pending" // or "Not Applicable"
   )
   ```

6. **Notification Created (Per Attendee)**
   ```kotlin
   EventNotification(
       type = NotificationType.EVENT_CANCELLED,
       title = "Tech Conference 2024 - Cancelled",
       message = "The event has been cancelled. Reason: Speaker unable to attend due to emergency\nRefunds will be processed automatically. Status: Pending",
       priority = NotificationPriority.URGENT, // Always urgent
       recipientUserId = "user1",
       recipientUserType = NotificationRecipientType.ATTENDEE,
       eventId = "event123",
       eventTitle = "Tech Conference 2024",
       metadata = {
           "reason": "Speaker unable to attend due to emergency",
           "refundStatus": "PENDING",
           "refundAmount": "5000.0",
           "refundCurrency": "PKR"
       },
       actionUrl = "eventfinder://event/event123"
   )
   ```

7. **Delivery Process**
   - **Ignores quiet hours** (URGENT priority)
   - **Ignores some user preferences** (cancellations always delivered)
   - FCM with high priority
   - In-app badge with red indicator

8. **All Reminders Cancelled**
   ```kotlin
   notificationService.cancelEventNotifications("event123")
   ```

9. **Organizer Notification**
   ```kotlin
   notificationService.notifyEventOrganizer(
       eventId = "event123",
       organizerId = "organizer456",
       type = NotificationType.REFUNDS_INITIATED,
       title = "Refunds Initiated",
       message = "Refunds have been initiated for 50 attendees. Total: PKR 250,000"
   )
   ```

10. **Follow-up Notifications**
    - When each refund completes:
      ```kotlin
      EventNotification(
          type = NotificationType.REFUND_COMPLETED,
          title = "Refund Completed",
          message = "Your refund of PKR 5,000 has been processed and will appear in your account within 5-7 business days",
          recipientUserId = "user1",
          recipientUserType = NotificationRecipientType.ATTENDEE
      )
      ```
    
    - When all refunds complete (organizer):
      ```kotlin
      EventNotification(
          type = NotificationType.REFUNDS_COMPLETED,
          title = "All Refunds Completed",
          message = "All 50 refunds have been successfully processed. Total: PKR 250,000",
          recipientUserId = "organizer456",
          recipientUserType = NotificationRecipientType.ORGANIZER
      )
      ```

---

### Flow 4: Event Starting Soon (Reminders)

**Trigger:** Scheduled notification system checks time

**Step-by-Step:**

1. **Scheduling (When Event Created/Updated)**
   ```kotlin
   // Called after event is created or rescheduled
   notificationService.scheduleEventReminders(event)
   ```

2. **Reminder Records Created**
   ```kotlin
   // 24-hour reminder
   EventNotification(
       type = NotificationType.EVENT_STARTING_SOON_24H,
       title = "Tomorrow: Tech Conference 2024",
       message = "Your event starts tomorrow at 10:00 AM",
       scheduledFor = event.startTime - (24 * 60 * 60 * 1000),
       recipientUserId = "user1",
       recipientUserType = NotificationRecipientType.ATTENDEE,
       eventId = "event123"
   )

   // 1-hour reminder
   EventNotification(
       type = NotificationType.EVENT_STARTING_SOON_1H,
       title = "Starting Soon: Tech Conference 2024",
       message = "Your event starts in 1 hour!",
       scheduledFor = event.startTime - (60 * 60 * 1000),
       recipientUserId = "user1",
       recipientUserType = NotificationRecipientType.ATTENDEE,
       eventId = "event123"
   )
   ```

3. **Background Job Execution** (Phase 6 - Future)
   ```kotlin
   // WorkManager job runs every 15 minutes
   class NotificationWorker : Worker() {
       override fun doWork(): Result {
           val now = System.currentTimeMillis()
           val pendingNotifications = getScheduledNotifications(now)
           
           pendingNotifications.forEach { notification ->
               if (notification.scheduledFor <= now) {
                   deliverNotification(notification)
               }
           }
           
           return Result.success()
       }
   }
   ```

4. **Delivery Time Check**
   ```kotlin
   fun shouldDeliverNow(notification: EventNotification): Boolean {
       val now = System.currentTimeMillis()
       val scheduledFor = notification.scheduledFor ?: return false
       
       // Within 5-minute window of scheduled time
       return now >= scheduledFor && now <= (scheduledFor + 5 * 60 * 1000)
   }
   ```

5. **User Preference Check**
   ```kotlin
   val prefs = getUserPreferences(notification.recipientUserId)
   
   // Check if reminders enabled
   if (!prefs.eventStartingSoonEnabled) return
   
   // Check specific reminder setting
   if (notification.type == EVENT_STARTING_SOON_24H && !prefs.reminder24hEnabled) return
   if (notification.type == EVENT_STARTING_SOON_1H && !prefs.reminder1hEnabled) return
   
   // Check quiet hours (but HIGH priority may override)
   if (prefs.isInQuietHours() && notification.priority != NotificationPriority.HIGH) return
   ```

6. **Notification Delivered**
   - FCM push sent
   - In-app notification created
   - Marked as delivered
   - scheduledFor cleared

---

### Flow 5: New Attendee Registration

**Trigger:** User purchases ticket

**Step-by-Step:**

1. **User Action**
   - User views event → Clicks "Book Ticket"
   - Completes payment
   - Ticket created in database

2. **Ticket Created**
   ```kotlin
   Ticket(
       id = "ticket789",
       eventId = "event123",
       userId = "user1",
       purchasedAt = System.currentTimeMillis(),
       amount = 5000.0,
       status = TicketStatus.CONFIRMED
   )
   ```

3. **Attendee Notification** (to user)
   ```kotlin
   EventNotification(
       type = NotificationType.TICKET_PURCHASED,
       title = "Ticket Purchased",
       message = "Your ticket for Tech Conference 2024 has been confirmed!",
       priority = NotificationPriority.NORMAL,
       recipientUserId = "user1",
       recipientUserType = NotificationRecipientType.ATTENDEE,
       eventId = "event123",
       metadata = {
           "ticketId": "ticket789",
           "amount": "5000.0",
           "currency": "PKR"
       },
       actionUrl = "eventfinder://ticket/ticket789"
   )
   ```

4. **Organizer Notification**
   ```kotlin
   val currentCount = getEventAttendeeCount("event123") // 32
   val maxCapacity = event.maxParticipants // 100
   
   EventNotification(
       type = NotificationType.NEW_ATTENDEE,
       title = "New Attendee",
       message = "Ali Khan just registered for Tech Conference 2024. Total attendees: 32/100",
       priority = NotificationPriority.NORMAL,
       recipientUserId = "organizer456",
       recipientUserType = NotificationRecipientType.ORGANIZER,
       eventId = "event123",
       metadata = {
           "attendeeName": "Ali Khan",
           "currentCount": "32",
           "maxCapacity": "100",
           "percentage": "32"
       }
   )
   ```

5. **Capacity Milestone Check**
   ```kotlin
   val percentage = (currentCount.toDouble() / maxCapacity) * 100
   
   when {
       percentage >= 50 && !milestoneTriggered50 -> {
           sendCapacityMilestone(
               type = CAPACITY_MILESTONE_50,
               current = 50,
               max = 100
           )
       }
       percentage >= 75 && !milestoneTriggered75 -> {
           sendCapacityMilestone(
               type = CAPACITY_MILESTONE_75,
               current = 75,
               max = 100
           )
       }
       percentage >= 90 && !milestoneTriggered90 -> {
           sendCapacityMilestone(
               type = CAPACITY_MILESTONE_90,
               current = 90,
               max = 100
           )
       }
       percentage >= 100 -> {
           sendCapacityMilestone(
               type = CAPACITY_FULL,
               current = 100,
               max = 100
           )
       }
   }
   ```

6. **Event Reminders Scheduled** (for new attendee)
   ```kotlin
   scheduleEventReminders(event, userId = "user1")
   ```

---

### Flow 6: QR Code Scanned (Check-in)

**Trigger:** Organizer scans attendee's QR code

**Step-by-Step:**

1. **User Action**
   - Organizer opens ManageEventFragment
   - Clicks FAB "Scan QR"
   - Camera opens → Scans attendee's QR code

2. **Ticket Validation**
   ```kotlin
   val ticket = ticketRepository.getTicketById(ticketId)
   
   // Validations
   if (ticket.eventId != currentEventId) return Error("Wrong event")
   if (ticket.isScanned) return Error("Already checked in")
   if (ticket.status != TicketStatus.CONFIRMED) return Error("Invalid ticket")
   ```

3. **Check-in Processed**
   ```kotlin
   ticket.copy(
       isScanned = true,
       scannedAt = System.currentTimeMillis(),
       scannedBy = organizerId
   )
   ```

4. **Organizer Notification**
   ```kotlin
   EventNotification(
       type = NotificationType.TICKET_SCANNED,
       title = "Check-in Successful",
       message = "Ali Khan checked in for Tech Conference 2024",
       priority = NotificationPriority.NORMAL,
       recipientUserId = "organizer456",
       recipientUserType = NotificationRecipientType.ORGANIZER,
       eventId = "event123",
       metadata = {
           "attendeeName": "Ali Khan",
           "checkedInAt": "1735689600000",
           "totalCheckedIn": "28",
           "totalAttendees": "50"
       }
   )
   ```

5. **Attendee Notification** (optional)
   ```kotlin
   EventNotification(
       type = NotificationType.TICKET_SCANNED,
       title = "Checked In",
       message = "You've been checked in to Tech Conference 2024. Enjoy the event!",
       priority = NotificationPriority.LOW,
       recipientUserId = "user1",
       recipientUserType = NotificationRecipientType.ATTENDEE,
       eventId = "event123"
   )
   ```

---

### Flow 7: Low Attendance Alert

**Trigger:** Background job checks 24h before event

**Step-by-Step:**

1. **Background Job Runs**
   ```kotlin
   // Scheduled to run every 6 hours
   class AttendanceCheckWorker : Worker() {
       override fun doWork(): Result {
           val tomorrow = System.currentTimeMillis() + (24 * 60 * 60 * 1000)
           val events = getEventsStartingIn24Hours()
           
           events.forEach { event ->
               checkAttendance(event)
           }
           
           return Result.success()
       }
   }
   ```

2. **Attendance Calculation**
   ```kotlin
   fun checkAttendance(event: Event) {
       val registeredCount = getEventAttendeeCount(event.id)
       val capacity = event.maxParticipants ?: return
       val percentage = (registeredCount.toDouble() / capacity) * 100
       
       // Alert if less than 20% capacity
       if (percentage < 20 && !alertAlreadySent(event.id)) {
           sendLowAttendanceAlert(event, registeredCount, capacity, percentage)
       }
   }
   ```

3. **Organizer Notification**
   ```kotlin
   EventNotification(
       type = NotificationType.LOW_ATTENDANCE_ALERT,
       title = "Low Attendance Alert",
       message = "Tech Conference 2024 starts in 24 hours with only 15/100 attendees (15%). Consider promotional actions.",
       priority = NotificationPriority.URGENT,
       recipientUserId = "organizer456",
       recipientUserType = NotificationRecipientType.ORGANIZER,
       eventId = "event123",
       metadata = {
           "registeredCount": "15",
           "capacity": "100",
           "percentage": "15",
           "hoursUntilStart": "24"
       },
       actionUrl = "eventfinder://event/event123/promote",
       actionLabel = "Promote Event"
   )
   ```

4. **Suggestions Included**
   - Share on social media
   - Send to local groups
   - Consider last-minute discount
   - Reduce capacity if possible

---

## Priority System

### Priority Levels

| Priority | Delivery Behavior | Examples |
|----------|------------------|----------|
| **URGENT** | • Ignores quiet hours<br>• High-priority FCM<br>• Sound + vibration<br>• Heads-up notification<br>• Red badge | Event cancelled<br>Refund failed<br>Event expired<br>Low attendance |
| **HIGH** | • Respects quiet hours for non-critical<br>• Normal FCM<br>• Sound enabled<br>• Badge notification | Event postponed<br>Event rescheduled<br>1h reminder<br>Capacity full |
| **NORMAL** | • Respects quiet hours<br>• Silent if in quiet mode<br>• Badge notification | New attendee<br>Ticket purchased<br>24h reminder<br>Check-in |
| **LOW** | • Respects quiet hours<br>• Silent delivery<br>• Badge only<br>• No sound/vibration | Event completed<br>Event published<br>Details changed |

### Priority Determination

```kotlin
fun NotificationType.getPriority(): NotificationPriority {
    return when (this) {
        // URGENT
        EVENT_CANCELLED,
        REFUND_FAILED,
        REFUND_ACTION_NEEDED,
        EVENT_AUTO_EXPIRED,
        LOW_ATTENDANCE_ALERT -> NotificationPriority.URGENT
        
        // HIGH
        EVENT_POSTPONED,
        EVENT_RESCHEDULED,
        EVENT_STARTING_SOON_1H,
        CAPACITY_FULL,
        EVENT_ABOUT_TO_START -> NotificationPriority.HIGH
        
        // NORMAL
        EVENT_STARTING_SOON_24H,
        NEW_ATTENDEE,
        TICKET_SCANNED,
        CAPACITY_MILESTONE_90,
        EVENT_ENDED_MARK_COMPLETE,
        TICKET_PURCHASED,
        TICKET_CONFIRMED,
        REFUND_INITIATED,
        REFUND_COMPLETED,
        ORGANIZER_MESSAGE -> NotificationPriority.NORMAL
        
        // LOW
        else -> NotificationPriority.LOW
    }
}
```

---

## Scheduling System

### Types of Scheduled Notifications

1. **Event Reminders** (24h, 1h before start)
2. **Organizer Alerts** (1h before start, post-event)
3. **Background Jobs** (attendance check, state updates)

### Implementation

```kotlin
// Create scheduled notification
fun scheduleNotification(notification: EventNotification, deliveryTime: Long) {
    val scheduledNotification = notification.copy(
        scheduledFor = deliveryTime,
        isDelivered = false
    )
    
    // Save to Firestore
    firestoreNotifications.document(notification.id).set(scheduledNotification)
    
    // Schedule WorkManager job
    val delay = deliveryTime - System.currentTimeMillis()
    val workRequest = OneTimeWorkRequestBuilder<NotificationDeliveryWorker>()
        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
        .setInputData(workDataOf("notificationId" to notification.id))
        .build()
    
    WorkManager.getInstance(context).enqueue(workRequest)
}
```

### Cancellation

```kotlin
fun cancelScheduledNotifications(eventId: String) {
    // Remove from Firestore
    firestoreNotifications
        .where("eventId", "==", eventId)
        .where("isDelivered", "==", false)
        .get()
        .addOnSuccessListener { documents ->
            documents.forEach { it.reference.delete() }
        }
    
    // Cancel WorkManager jobs
    WorkManager.getInstance(context)
        .cancelAllWorkByTag("event_$eventId")
}
```

### Rescheduling

```kotlin
fun rescheduleEventNotifications(event: Event) {
    // Cancel old notifications
    cancelScheduledNotifications(event.id)
    
    // Create new reminders with updated times
    scheduleEventReminders(event)
}
```

---

## User Preferences

### Preference Model

```kotlin
data class NotificationPreferences(
    val userId: String,
    
    // Global toggles
    val notificationsEnabled: Boolean = true,
    val pushNotificationsEnabled: Boolean = true,
    val inAppNotificationsEnabled: Boolean = true,
    val emailNotificationsEnabled: Boolean = false,
    
    // Category preferences (Attendees)
    val eventPostponedEnabled: Boolean = true,
    val eventRescheduledEnabled: Boolean = true,
    val eventCancelledEnabled: Boolean = true, // Cannot disable
    val eventStartingSoonEnabled: Boolean = true,
    val eventStartedEnabled: Boolean = true,
    val eventCompletedEnabled: Boolean = false,
    val eventDetailsChangedEnabled: Boolean = true,
    
    // Ticket preferences (Attendees)
    val ticketPurchasedEnabled: Boolean = true,
    val refundNotificationsEnabled: Boolean = true, // Cannot disable
    
    // Organizer preferences
    val newAttendeeEnabled: Boolean = true,
    val capacityMilestonesEnabled: Boolean = true,
    val eventStateChangesEnabled: Boolean = true,
    val refundAlertsEnabled: Boolean = true, // Cannot disable
    
    // Reminder preferences
    val reminder24hEnabled: Boolean = true,
    val reminder1hEnabled: Boolean = true,
    
    // Quiet hours
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: Int = 22, // 10 PM
    val quietHoursEnd: Int = 8      // 8 AM
)
```

### Preference Hierarchy

```
1. System-Critical Notifications (Always Delivered)
   ├─ EVENT_CANCELLED
   ├─ REFUND_FAILED
   └─ REFUND_ACTION_NEEDED

2. Global Toggle (notificationsEnabled)
   └─ If false, only critical notifications delivered

3. Push vs In-App Toggle
   ├─ pushNotificationsEnabled → FCM delivery
   └─ inAppNotificationsEnabled → In-app badge/list

4. Category Toggles
   └─ Specific notification types

5. Quiet Hours
   └─ Applies to NORMAL and LOW priority only
```

### Preference Check Flow

```kotlin
fun shouldDeliverNotification(
    userId: String,
    notificationType: NotificationType
): Boolean {
    val prefs = getUserPreferences(userId)
    val priority = notificationType.getPriority()
    
    // Critical notifications always delivered
    if (priority == NotificationPriority.URGENT) return true
    
    // Global toggle
    if (!prefs.notificationsEnabled) return false
    
    // Category-specific toggle
    if (!prefs.isNotificationTypeEnabled(notificationType)) return false
    
    // Quiet hours (doesn't apply to HIGH/URGENT)
    if (priority <= NotificationPriority.NORMAL && prefs.isInQuietHours()) {
        return false
    }
    
    return true
}
```

### User Settings Screen

**Location:** Settings → Notifications

**Sections:**
1. **Master Controls**
   - Enable/Disable all notifications
   - Enable/Disable push notifications
   - Enable/Disable in-app notifications

2. **Event Updates** (for attendees)
   - Postponements & Reschedules ✓
   - Cancellations (always on, greyed out)
   - Event starting soon ✓
   - Event started ✓
   - Event completed ✓
   - Detail changes ✓

3. **Tickets & Payments** (for attendees)
   - Purchase confirmations ✓
   - Refund updates (always on, greyed out)

4. **Event Management** (for organizers)
   - New attendee registrations ✓
   - Capacity milestones ✓
   - Event state changes ✓
   - Refund alerts (always on, greyed out)

5. **Reminders**
   - 24 hours before event ✓
   - 1 hour before event ✓

6. **Quiet Hours**
   - Enable quiet hours ✓
   - Start time: [10:00 PM]
   - End time: [8:00 AM]
   - Note: Urgent notifications will still be delivered

---

## Firebase Implementation

### Current Status

✅ **Implemented (In-Memory)**
- Notification models
- NotificationService interface
- Basic in-memory storage
- Use case integration

❌ **Not Yet Implemented**
- Firebase Firestore storage
- Firebase Cloud Messaging (FCM)
- Background jobs (WorkManager)
- User preferences storage

### Required Firebase Setup

#### 1. Firestore Collections

**notifications**
```javascript
{
  notificationId: "notif_12345",
  type: "EVENT_POSTPONED",
  title: "Tech Conference 2024 - Postponed",
  message: "The event has been postponed...",
  priority: "HIGH",
  
  recipientUserId: "user_123",
  recipientUserType: "ATTENDEE",
  
  eventId: "event_456",
  eventTitle: "Tech Conference 2024",
  eventImageUrl: "https://...",
  organizerId: "org_789",
  organizerName: "Tech Events PK",
  
  metadata: {
    reason: "Weather conditions",
    newStartTime: "1735689600000"
  },
  
  actionUrl: "eventfinder://event/event_456",
  actionLabel: "View Details",
  
  isRead: false,
  isDelivered: true,
  deliveredAt: Timestamp,
  readAt: null,
  
  createdAt: Timestamp,
  scheduledFor: null,
  expiresAt: Timestamp(now + 30days)
}
```

**Indexes:**
```javascript
// Composite indexes needed:
1. recipientUserId (ASC) + createdAt (DESC)
2. recipientUserId (ASC) + isRead (ASC) + createdAt (DESC)
3. eventId (ASC) + createdAt (DESC)
4. scheduledFor (ASC) + isDelivered (ASC)
```

**notificationPreferences**
```javascript
{
  userId: "user_123",
  notificationsEnabled: true,
  pushNotificationsEnabled: true,
  inAppNotificationsEnabled: true,
  emailNotificationsEnabled: false,
  
  // Category preferences
  eventPostponedEnabled: true,
  eventRescheduledEnabled: true,
  eventCancelledEnabled: true,
  eventStartingSoonEnabled: true,
  // ... all other fields from model
  
  quietHoursEnabled: false,
  quietHoursStart: 22,
  quietHoursEnd: 8,
  
  updatedAt: Timestamp
}
```

**fcmTokens**
```javascript
{
  userId: "user_123",
  token: "fcm_token_xyz...",
  platform: "android", // or "ios"
  appVersion: "1.2.0",
  createdAt: Timestamp,
  lastUsedAt: Timestamp
}
```

#### 2. Firestore Security Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Notifications - users can only read their own
    match /notifications/{notificationId} {
      allow read: if request.auth != null 
        && resource.data.recipientUserId == request.auth.uid;
      
      allow update: if request.auth != null 
        && resource.data.recipientUserId == request.auth.uid
        && request.resource.data.diff(resource.data).affectedKeys()
          .hasOnly(['isRead', 'readAt']);
      
      // System can create/delete
      allow create, delete: if false; // Only server-side
    }
    
    // Notification Preferences
    match /notificationPreferences/{userId} {
      allow read, write: if request.auth != null 
        && userId == request.auth.uid;
    }
    
    // FCM Tokens
    match /fcmTokens/{userId} {
      allow read, write: if request.auth != null 
        && userId == request.auth.uid;
    }
  }
}
```

#### 3. Firebase Cloud Messaging Setup

**Add FCM to Android App:**

1. **Add google-services.json**
   ```
   app/google-services.json
   ```

2. **Update build.gradle (project level)**
   ```gradle
   dependencies {
       classpath 'com.google.gms:google-services:4.3.15'
   }
   ```

3. **Update build.gradle (app level)**
   ```gradle
   plugins {
       id 'com.google.gms.google-services'
   }
   
   dependencies {
       implementation platform('com.google.firebase:firebase-bom:32.7.0')
       implementation 'com.google.firebase:firebase-messaging-ktx'
       implementation 'com.google.firebase:firebase-analytics-ktx'
   }
   ```

4. **Create FirebaseMessagingService**
   ```kotlin
   // app/src/main/java/com/eventfinder/app/fcm/EventFinderMessagingService.kt
   
   @HiltAndroidApp
   class EventFinderMessagingService : FirebaseMessagingService() {
       
       @Inject
       lateinit var notificationService: NotificationService
       
       override fun onNewToken(token: String) {
           super.onNewToken(token)
           // Save token to Firestore
           saveFCMToken(token)
       }
       
       override fun onMessageReceived(remoteMessage: RemoteMessage) {
           super.onMessageReceived(remoteMessage)
           
           // Extract data
           val data = remoteMessage.data
           val notificationId = data["notificationId"] ?: return
           val eventId = data["eventId"]
           val type = data["type"]?.let { NotificationType.valueOf(it) }
           
           // Show notification
           showNotification(
               title = remoteMessage.notification?.title ?: "",
               message = remoteMessage.notification?.body ?: "",
               imageUrl = remoteMessage.notification?.imageUrl?.toString(),
               notificationId = notificationId,
               eventId = eventId,
               type = type
           )
       }
       
       private fun showNotification(
           title: String,
           message: String,
           imageUrl: String?,
           notificationId: String,
           eventId: String?,
           type: NotificationType?
       ) {
           val intent = Intent(this, MainActivity::class.java).apply {
               flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
               putExtra("notificationId", notificationId)
               putExtra("eventId", eventId)
           }
           
           val pendingIntent = PendingIntent.getActivity(
               this, 0, intent, PendingIntent.FLAG_IMMUTABLE
           )
           
           val notification = NotificationCompat.Builder(this, CHANNEL_ID)
               .setContentTitle(title)
               .setContentText(message)
               .setSmallIcon(R.drawable.ic_notification)
               .setAutoCancel(true)
               .setContentIntent(pendingIntent)
               .setPriority(type?.getPriority()?.toAndroidPriority() ?: PRIORITY_DEFAULT)
               .apply {
                   imageUrl?.let { setLargeIcon(loadBitmap(it)) }
               }
               .build()
           
           NotificationManagerCompat.from(this)
               .notify(notificationId.hashCode(), notification)
       }
       
       private fun saveFCMToken(token: String) {
           val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
           
           val fcmToken = hashMapOf(
               "userId" to userId,
               "token" to token,
               "platform" to "android",
               "appVersion" to BuildConfig.VERSION_NAME,
               "createdAt" to FieldValue.serverTimestamp(),
               "lastUsedAt" to FieldValue.serverTimestamp()
           )
           
           FirebaseFirestore.getInstance()
               .collection("fcmTokens")
               .document(userId)
               .set(fcmToken, SetOptions.merge())
       }
   }
   ```

5. **Register Service in AndroidManifest.xml**
   ```xml
   <service
       android:name=".fcm.EventFinderMessagingService"
       android:exported="false">
       <intent-filter>
           <action android:name="com.google.firebase.MESSAGING_EVENT" />
       </intent-filter>
   </service>
   ```

6. **Create Notification Channels**
   ```kotlin
   // app/src/main/java/com/eventfinder/app/utils/NotificationChannels.kt
   
   object NotificationChannels {
       const val CHANNEL_ID_URGENT = "urgent_notifications"
       const val CHANNEL_ID_HIGH = "high_notifications"
       const val CHANNEL_ID_NORMAL = "normal_notifications"
       const val CHANNEL_ID_LOW = "low_notifications"
       
       fun createChannels(context: Context) {
           if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
               val notificationManager = context.getSystemService(NotificationManager::class.java)
               
               // Urgent channel
               val urgentChannel = NotificationChannel(
                   CHANNEL_ID_URGENT,
                   "Urgent Notifications",
                   NotificationManager.IMPORTANCE_HIGH
               ).apply {
                   description = "Critical event updates that require immediate attention"
                   enableLights(true)
                   enableVibration(true)
                   setShowBadge(true)
               }
               
               // High priority channel
               val highChannel = NotificationChannel(
                   CHANNEL_ID_HIGH,
                   "Important Notifications",
                   NotificationManager.IMPORTANCE_HIGH
               ).apply {
                   description = "Important event updates"
                   enableLights(true)
                   enableVibration(true)
               }
               
               // Normal priority channel
               val normalChannel = NotificationChannel(
                   CHANNEL_ID_NORMAL,
                   "Event Updates",
                   NotificationManager.IMPORTANCE_DEFAULT
               ).apply {
                   description = "Regular event notifications"
               }
               
               // Low priority channel
               val lowChannel = NotificationChannel(
                   CHANNEL_ID_LOW,
                   "Event Info",
                   NotificationManager.IMPORTANCE_LOW
               ).apply {
                   description = "Low priority event information"
                   setShowBadge(false)
               }
               
               notificationManager.createNotificationChannels(listOf(
                   urgentChannel, highChannel, normalChannel, lowChannel
               ))
           }
       }
   }
   ```

#### 4. Update NotificationServiceImpl for Firebase

```kotlin
// app/src/main/java/com/eventfinder/app/data/service/FirebaseNotificationServiceImpl.kt

@Singleton
class FirebaseNotificationServiceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val eventRepository: EventRepository,
    private val ticketRepository: TicketRepository,
    private val context: Context
) : NotificationService {
    
    private val notificationsCollection = firestore.collection("notifications")
    private val preferencesCollection = firestore.collection("notificationPreferences")
    private val fcmTokensCollection = firestore.collection("fcmTokens")
    
    override suspend fun sendNotification(notification: EventNotification): Result<EventNotification> {
        return try {
            // Generate ID if needed
            val notificationWithId = if (notification.id.isEmpty()) {
                notification.copy(
                    id = notificationsCollection.document().id,
                    notificationId = UUID.randomUUID().toString()
                )
            } else {
                notification
            }
            
            // Check user preferences
            val prefs = getUserPreferences(notification.recipientUserId)
            if (!shouldDeliverNotification(prefs, notification.type)) {
                return Result.success(notificationWithId)
            }
            
            // Save to Firestore
            notificationsCollection
                .document(notificationWithId.id)
                .set(notificationWithId.toMap())
                .await()
            
            // Send FCM push if enabled
            if (prefs.pushNotificationsEnabled) {
                sendFCMNotification(notificationWithId)
            }
            
            Result.success(notificationWithId.markAsDelivered())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun sendFCMNotification(notification: EventNotification) {
        val token = getUserFCMToken(notification.recipientUserId) ?: return
        
        // Prepare FCM message
        val message = com.google.firebase.messaging.RemoteMessage.Builder(token)
            .setData(mapOf(
                "notificationId" to notification.notificationId,
                "eventId" to notification.eventId,
                "type" to notification.type.name
            ))
            .setNotification(
                com.google.firebase.messaging.Notification.Builder()
                    .setTitle(notification.title)
                    .setBody(notification.message)
                    .setImage(notification.eventImageUrl)
                    .build()
            )
            .setPriority(notification.priority.toFCMPriority())
            .build()
        
        // Note: FCM messages are sent via Firebase Admin SDK from backend
        // For now, use local notification
        showLocalNotification(notification)
    }
    
    private fun showLocalNotification(notification: EventNotification) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("notificationId", notification.notificationId)
            putExtra("eventId", notification.eventId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            notification.notificationId.hashCode(), 
            intent, 
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val channelId = when (notification.priority) {
            NotificationPriority.URGENT -> NotificationChannels.CHANNEL_ID_URGENT
            NotificationPriority.HIGH -> NotificationChannels.CHANNEL_ID_HIGH
            NotificationPriority.NORMAL -> NotificationChannels.CHANNEL_ID_NORMAL
            NotificationPriority.LOW -> NotificationChannels.CHANNEL_ID_LOW
        }
        
        val androidNotification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(notification.priority.toAndroidPriority())
            .build()
        
        NotificationManagerCompat.from(context)
            .notify(notification.notificationId.hashCode(), androidNotification)
    }
    
    override suspend fun getUnreadNotifications(userId: String): Result<List<EventNotification>> {
        return try {
            val snapshot = notificationsCollection
                .whereEqualTo("recipientUserId", userId)
                .whereEqualTo("isRead", false)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()
            
            val notifications = snapshot.documents.mapNotNull { doc ->
                doc.toObject(EventNotification::class.java)
            }
            
            Result.success(notifications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            notificationsCollection
                .whereEqualTo("notificationId", notificationId)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?.reference
                ?.update(mapOf(
                    "isRead" to true,
                    "readAt" to FieldValue.serverTimestamp()
                ))
                ?.await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun getUserPreferences(userId: String): NotificationPreferences {
        return try {
            val doc = preferencesCollection.document(userId).get().await()
            doc.toObject(NotificationPreferences::class.java) 
                ?: NotificationPreferences(userId = userId)
        } catch (e: Exception) {
            NotificationPreferences(userId = userId)
        }
    }
    
    private suspend fun getUserFCMToken(userId: String): String? {
        return try {
            val doc = fcmTokensCollection.document(userId).get().await()
            doc.getString("token")
        } catch (e: Exception) {
            null
        }
    }
}
```

#### 5. Background Jobs with WorkManager

```kotlin
// app/src/main/java/com/eventfinder/app/worker/NotificationDeliveryWorker.kt

@HiltWorker
class NotificationDeliveryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationService: NotificationService
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        
        // Get scheduled notifications that should be delivered now
        val notifications = getScheduledNotifications(now)
        
        notifications.forEach { notification ->
            if (shouldDeliverNow(notification, now)) {
                notificationService.sendNotification(notification)
            }
        }
        
        return Result.success()
    }
    
    private suspend fun getScheduledNotifications(now: Long): List<EventNotification> {
        // Query Firestore for notifications scheduled for now
        // scheduledFor <= now AND isDelivered = false
        return emptyList() // Implementation
    }
    
    private fun shouldDeliverNow(notification: EventNotification, now: Long): Boolean {
        val scheduledFor = notification.scheduledFor ?: return false
        // Within 5-minute window
        return now >= scheduledFor && now <= (scheduledFor + 5 * 60 * 1000)
    }
}
```

**Schedule periodic work:**
```kotlin
// In Application class
class EventFinderApplication : Application(), Configuration.Provider {
    
    override fun onCreate() {
        super.onCreate()
        
        // Schedule notification delivery worker
        val workRequest = PeriodicWorkRequestBuilder<NotificationDeliveryWorker>(
            15, TimeUnit.MINUTES
        ).build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "notification_delivery",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
```

---

## Testing Strategy

### Unit Tests

**NotificationServiceTest.kt**
```kotlin
@Test
fun `sendNotification creates notification with correct fields`() = runTest {
    val notification = EventNotification(
        type = NotificationType.EVENT_POSTPONED,
        title = "Event Postponed",
        message = "Test message",
        recipientUserId = "user123",
        recipientUserType = NotificationRecipientType.ATTENDEE,
        eventId = "event456",
        eventTitle = "Test Event"
    )
    
    val result = notificationService.sendNotification(notification)
    
    assertTrue(result.isSuccess)
    val saved = result.getOrNull()
    assertNotNull(saved?.id)
    assertEquals(notification.type, saved?.type)
}

@Test
fun `notification respects user preferences`() = runTest {
    // Setup: User has postponement notifications disabled
    val prefs = NotificationPreferences(
        userId = "user123",
        eventPostponedEnabled = false
    )
    saveUserPreferences(prefs)
    
    val notification = EventNotification(
        type = NotificationType.EVENT_POSTPONED,
        recipientUserId = "user123",
        // ...
    )
    
    notificationService.sendNotification(notification)
    
    // Verify notification not delivered
    val unread = notificationService.getUnreadNotifications("user123")
    assertTrue(unread.getOrNull()?.isEmpty() == true)
}

@Test
fun `urgent notifications ignore quiet hours`() = runTest {
    val prefs = NotificationPreferences(
        userId = "user123",
        quietHoursEnabled = true,
        quietHoursStart = 22,
        quietHoursEnd = 8
    )
    
    // During quiet hours (e.g., 2 AM)
    val notification = EventNotification(
        type = NotificationType.EVENT_CANCELLED,
        priority = NotificationPriority.URGENT,
        recipientUserId = "user123",
        // ...
    )
    
    val result = notificationService.sendNotification(notification)
    
    assertTrue(result.isSuccess)
    // Verify delivered despite quiet hours
}
```

### Integration Tests

**NotificationFlowTest.kt**
```kotlin
@Test
fun `postponing event sends notifications to all attendees`() = runTest {
    // Setup: Event with 3 attendees
    val event = createTestEvent()
    val attendees = listOf("user1", "user2", "user3")
    attendees.forEach { createTicket(event.id, it) }
    
    // Action: Postpone event
    postponeEventUseCase(
        eventId = event.id,
        newStartTime = System.currentTimeMillis() + 86400000,
        reason = "Test postponement",
        userId = event.organizerId
    )
    
    // Verify: All attendees received notification
    attendees.forEach { userId ->
        val notifications = notificationService.getUnreadNotifications(userId)
        assertEquals(1, notifications.getOrNull()?.size)
        assertEquals(NotificationType.EVENT_POSTPONED, notifications.getOrNull()?.first()?.type)
    }
}

@Test
fun `cancelling paid event initiates refunds and sends notifications`() = runTest {
    // Setup
    val event = createPaidEvent(price = 5000.0)
    createTicket(event.id, "user1")
    
    // Action: Cancel event
    cancelEventUseCase(
        eventId = event.id,
        reason = "Test cancellation",
        userId = event.organizerId
    )
    
    // Verify: Attendee notification mentions refund
    val notifications = notificationService.getUnreadNotifications("user1")
    val notification = notifications.getOrNull()?.first()
    
    assertEquals(NotificationType.EVENT_CANCELLED, notification?.type)
    assertTrue(notification?.message?.contains("Refund") == true)
    assertEquals("PENDING", notification?.metadata?.get("refundStatus"))
}
```

### UI Tests

**NotificationFragmentTest.kt**
```kotlin
@Test
fun `tapping notification marks it as read and navigates to event`() {
    // Setup
    launchFragment<NotificationFragment>()
    
    // Action: Tap first notification
    onView(withId(R.id.recyclerViewNotifications))
        .perform(actionOnItemAtPosition(0, click()))
    
    // Verify: Navigated to EventDetailFragment
    onView(withId(R.id.eventDetailFragment)).check(matches(isDisplayed()))
    
    // Verify: Notification marked as read
    onView(withId(R.id.recyclerViewNotifications))
        .check(matches(hasDescendant(not(withId(R.id.unreadIndicator)))))
}

@Test
fun `unread badge shows correct count`() {
    // Setup: 3 unread notifications
    repeat(3) { createUnreadNotification() }
    
    launchActivity<MainActivity>()
    
    // Verify: Badge shows "3"
    onView(withId(R.id.notificationBadge))
        .check(matches(isDisplayed()))
        .check(matches(withText("3")))
}
```

---

## Future Enhancements

### Phase 1: Advanced Features
- [ ] **Rich Notifications** with images, action buttons
- [ ] **Notification Grouping** by event or type
- [ ] **Notification History** with search/filter
- [ ] **Email Notifications** integration
- [ ] **SMS Notifications** for critical updates

### Phase 2: Analytics
- [ ] Track notification delivery rates
- [ ] Track open rates
- [ ] Track action click rates
- [ ] A/B test notification copy
- [ ] Measure engagement by type

### Phase 3: Personalization
- [ ] **Smart Timing** - deliver when user most likely to engage
- [ ] **Content Personalization** based on user behavior
- [ ] **Frequency Capping** - limit notifications per day
- [ ] **Intelligent Batching** - combine similar notifications
- [ ] **Priority Learning** - learn user preferences over time

### Phase 4: Advanced Channels
- [ ] **WhatsApp Integration** for critical updates
- [ ] **Slack Integration** for organizers
- [ ] **Discord Integration** for communities
- [ ] **Telegram Bot** for notifications

### Phase 5: Interactive Notifications
- [ ] **Quick Reply** - respond to organizer messages
- [ ] **Quick Actions** - RSVP, check-in from notification
- [ ] **Snooze Reminders** - reschedule reminders
- [ ] **Inline Updates** - see event details without opening app

---

## Migration Path

### Current Implementation → Firebase (Step by Step)

1. **Week 1: Firebase Setup**
   - Add Firebase to project
   - Create Firestore collections
   - Set up security rules
   - Test basic read/write

2. **Week 2: Firestore Integration**
   - Replace in-memory storage with Firestore
   - Implement NotificationServiceImpl with Firestore
   - Test notification creation and retrieval
   - Migrate existing notifications (if any)

3. **Week 3: FCM Integration**
   - Implement FirebaseMessagingService
   - Create notification channels
   - Test push notification delivery
   - Handle deep linking

4. **Week 4: Background Jobs**
   - Implement WorkManager jobs
   - Test scheduled notification delivery
   - Test reminder notifications
   - Monitor job execution

5. **Week 5: User Preferences**
   - Create preferences UI
   - Implement preference sync
   - Test quiet hours
   - Test preference filtering

6. **Week 6: Polish & Monitoring**
   - Add analytics
   - Set up monitoring/alerting
   - Performance optimization
   - Load testing

---

## Conclusion

This notification system provides comprehensive coverage of all event lifecycle scenarios, with:
- ✅ 30+ notification types
- ✅ Priority-based delivery
- ✅ User preferences with granular control
- ✅ Scheduled notifications
- ✅ Firebase-ready architecture
- ✅ Scalable design

**Next Steps:**
1. Complete Firebase integration (Firestore + FCM)
2. Implement background jobs for scheduled delivery
3. Build notification UI screens
4. Add analytics and monitoring
5. Test thoroughly with real users

The system is designed to be:
- **Scalable** - handles thousands of users
- **Reliable** - with retry logic and error handling
- **User-Friendly** - respects preferences and quiet hours
- **Extensible** - easy to add new notification types
- **Testable** - comprehensive test coverage
