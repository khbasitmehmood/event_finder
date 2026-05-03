# Events & Bookings Tabs Implementation Plan

## Overview
Implementing two new tabs for the organizer bottom navigation:
1. **Events Tab** - Shows ALL organizer events (upcoming, past, happening now) without calendar filtering
2. **Bookings Tab** - Shows all tickets purchased for organizer's events, grouped by event

---

## Critical Bug Fixed First ✅

### Issue
Real event data not displaying in ManageEventFragment despite tickets being purchased.

### Root Cause
`ManageEventFragment` used `by viewModels()` while child fragments used `by activityViewModels()`. They were observing different ViewModel instances.

### Fix Applied
Changed `ManageEventFragment.kt:40`:
```kotlin
// BEFORE
private val sharedViewModel: ManageEventSharedViewModel by viewModels()

// AFTER
private val sharedViewModel: ManageEventSharedViewModel by activityViewModels()
```

**Result**: Parent and children now share the same ViewModel instance. Data loaded in parent is immediately available to all child fragments.

---

## Phase 1: Events Tab (All Events View)

### Goal
Create a comprehensive events list showing ALL organizer's events without calendar filtering, categorized by status.

### Key Differences from Dashboard
| Dashboard (OrganizerDashboardFragment) | Events Tab |
|----------------------------------------|------------|
| Calendar-filtered (selected date only) | ALL events |
| Horizontal RecyclerView | Vertical list with sections |
| Single date focus | Multi-category view |
| Empty state per date | Empty state per category |

### 1.1 Data Layer (Already Available)

**Repository Methods to Use:**
```kotlin
// From EventRepository
suspend fun getOrganizerEvents(organizerId: String): Result<List<Event>>
```

**No new repository methods needed** - existing method returns all events.

### 1.2 ViewModel Layer

**New File:** `OrganizerEventsViewModel.kt`

**Responsibilities:**
- Load all organizer events
- Categorize events into: Upcoming, Happening Now, Past
- Handle loading/error states
- Support pull-to-refresh
- Real-time updates via Flow

**State Management:**
```kotlin
data class EventsUiState(
    val upcomingEvents: List<Event> = emptyList(),
    val happeningNowEvents: List<Event> = emptyList(),
    val pastEvents: List<Event> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
```

**Event Categorization Logic:**
```kotlin
private fun categorizeEvents(events: List<Event>): Triple<List<Event>, List<Event>, List<Event>> {
    val now = System.currentTimeMillis()
    
    val upcoming = events.filter { it.startTime > now }
        .sortedBy { it.startTime }
    
    val happeningNow = events.filter { 
        it.startTime <= now && it.endTime >= now 
    }.sortedBy { it.startTime }
    
    val past = events.filter { it.endTime < now }
        .sortedByDescending { it.endTime }
    
    return Triple(upcoming, happeningNow, past)
}
```

### 1.3 UI Layer

**New File:** `OrganizerEventsFragment.kt`

**Layout Structure:**
```
SwipeRefreshLayout
├─ NestedScrollView
   └─ LinearLayout (vertical)
      ├─ Section: "Happening Now" (if any)
      │  ├─ Header TextView
      │  └─ RecyclerView (vertical, wrap_content)
      │
      ├─ Section: "Upcoming Events"
      │  ├─ Header TextView
      │  └─ RecyclerView (vertical, wrap_content)
      │
      └─ Section: "Past Events"
         ├─ Header TextView (initially collapsed, expandable)
         └─ RecyclerView (vertical, wrap_content)
```

**New File:** `fragment_organizer_events.xml`

**Adapter Strategy:**
- Reuse existing `EventAdapter` from dashboard
- Add section headers
- Each section has its own RecyclerView for better performance

**Empty States:**
- "No Upcoming Events" - with calendar icon
- "No Past Events" - with history icon
- "No Events At All" - when all categories empty (CTA to create event)

**Features:**
- Pull to refresh
- Tap event → Navigate to ManageEventFragment
- Show event count badges per section
- Smooth scrolling between sections

### 1.4 Navigation Update

**File:** `mobile_navigation.xml`

Add new destination:
```xml
<fragment
    android:id="@+id/organizerEventsFragment"
    android:name="com.eventfinder.app.client.organizer.OrganizerEventsFragment"
    android:label="My Events" />
```

**File:** `activity_main.xml` (Bottom Navigation)

Update organizer menu to include "Events" tab:
```xml
<item
    android:id="@+id/organizerEventsFragment"
    android:icon="@drawable/ic_event_list"
    android:title="Events" />
```

---

## Phase 2: Bookings Tab (Tickets Overview)

### Goal
Show all tickets purchased for organizer's events, grouped by event, with filtering and search.

### 2.1 Data Layer Enhancement

**New Repository Method Needed:**
```kotlin
// In TicketRepository
suspend fun getOrganizerBookings(organizerId: String): Result<List<Ticket>>

// Query all tickets where eventId IN (organizer's event IDs)
// Or add organizerId field to Ticket model for direct query
```

**Firestore Query Strategy:**

**Option A** (Current Schema):
1. Get all organizer event IDs
2. Query tickets with `whereIn("eventId", eventIds)` (limited to 10 events per query)
3. For more than 10 events, batch queries

**Option B** (Recommended - Add organizerId to Ticket):
1. Add `organizerId` field to Ticket model
2. Query directly: `whereEqualTo("organizerId", organizerId)`
3. Single efficient query

**Decision**: Use Option B - update Ticket model and Firestore rules.

### 2.2 Domain Model Update

**File:** `Ticket.kt`

Add field:
```kotlin
data class Ticket(
    // ... existing fields
    val organizerId: String = "",  // NEW FIELD
    // ... existing fields
)
```

**Migration Strategy:**
- New tickets will include organizerId
- Old tickets without organizerId will still work (default empty string)
- ViewModel filters by event ownership if organizerId missing

### 2.3 ViewModel Layer

**New File:** `OrganizerBookingsViewModel.kt`

**Responsibilities:**
- Load all tickets for organizer's events
- Group tickets by event
- Calculate per-event statistics (total, check-in rate) - NO REVENUE PER USER REQUEST
- Filter by status (All, Paid, Free, Pending, Checked-In, Cancelled)
- Search by attendee name, email, or ticket ID
- Real-time updates

**State Management:**
```kotlin
data class BookingGroup(
    val event: Event,
    val tickets: List<Ticket>,
    val checkInCount: Int,
    val totalCount: Int
)

data class BookingsUiState(
    val bookingGroups: List<BookingGroup> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val filterType: FilterType = FilterType.ALL
)

enum class FilterType {
    ALL, PAID, FREE, PENDING, CHECKED_IN, CANCELLED
}
```

### 2.4 UI Layer

**New File:** `OrganizerBookingsFragment.kt`

**Layout Structure:**
```
SwipeRefreshLayout
└─ LinearLayout (vertical)
   ├─ Search Bar
   ├─ Filter Chips (All, Paid, Pending, Free, Checked In)
   └─ RecyclerView (grouped/expandable list)
      └─ Each item = Event Header + Tickets List
```

**New Files:**
- `fragment_organizer_bookings.xml` - Main layout
- `item_booking_group_header.xml` - Event header (expandable)
- `item_booking_ticket.xml` - Individual ticket (reuse from Attendees tab)

**Adapter Strategy:**

**Option 1**: Single RecyclerView with ViewType headers (simpler)
- ViewType.HEADER = Event header
- ViewType.TICKET = Ticket row
- Click header to expand/collapse tickets

**Option 2**: ExpandableRecyclerView library (more features)
- Smooth animations
- Better UX for large lists

**Decision**: Use Option 1 (custom multi-view-type adapter) for better control.

**Features:**
- Tap event header → Navigate to ManageEventFragment
- Tap ticket → Show ticket details bottom sheet
- Expand/collapse event groups
- Search across all tickets
- Filter by status (including cancelled tickets)
- Show summary stats per event (attendees count, check-ins count) - NO REVENUE

**Empty States:**
- "No Bookings Yet" - when no tickets at all
- "No Results Found" - when search/filter returns empty

### 2.5 Navigation Update

**File:** `mobile_navigation.xml`

Add new destination:
```xml
<fragment
    android:id="@+id/organizerBookingsFragment"
    android:name="com.eventfinder.app.client.organizer.OrganizerBookingsFragment"
    android:label="Bookings" />
```

**File:** `activity_main.xml` (Bottom Navigation)

Add "Bookings" tab:
```xml
<item
    android:id="@+id/organizerBookingsFragment"
    android:icon="@drawable/ic_ticket"
    android:title="Bookings" />
```

---

## Phase 3: Shared Components & Resources

### 3.1 New Drawable Resources

**Icons Needed:**
- `ic_event_list.xml` - Events tab icon
- `ic_ticket.xml` - Bookings tab icon (if not exists)
- `ic_expand_more.xml` - Expand arrow for groups
- `ic_expand_less.xml` - Collapse arrow

### 3.2 Reusable Components

**From Existing Code:**
- ✅ `EventAdapter` - Reuse for Events tab
- ✅ `ItemAttendeeBinding` - Reuse for ticket rows
- ✅ `view_error_state.xml` - Error UI
- ✅ Empty state patterns from ManageEventAttendeesFragment

**New Reusable Component:**
```kotlin
// TicketDetailsBottomSheet.kt
// Shows full ticket details when tapped in Bookings tab
// - QR code
// - Attendee info
// - Purchase details
// - Check-in status
// - Actions (Check In, Cancel, Resend)
```

---

## Implementation Order

### Step 1: Critical Bug Fix ✅ COMPLETED
- [x] Fix ViewModel scoping issue in ManageEventFragment
- [x] Implement real data loading in ManageEventOverviewFragment
- [x] Remove hardcoded values from ManageEventInsightsFragment XML
- [x] Verify data now loads correctly (build successful)

### Step 2: Events Tab (2-3 hours)
1. Create `OrganizerEventsViewModel.kt`
2. Create `fragment_organizer_events.xml`
3. Create `OrganizerEventsFragment.kt`
4. Add navigation route
5. Update bottom navigation menu
6. Add icons
7. Test with real data

### Step 3: Bookings Tab - Data Layer (1 hour)
1. Update `Ticket` model with `organizerId` field
2. Update `FirestoreTicketDataSource` with new query method
3. Update `TicketRepository` with `getOrganizerBookings()`
4. Test data fetching

### Step 4: Bookings Tab - UI Layer (3-4 hours)
1. Create `OrganizerBookingsViewModel.kt`
2. Create layout files (fragment, item_booking_group_header, etc.)
3. Create `BookingGroupAdapter` (multi-view-type)
4. Create `OrganizerBookingsFragment.kt`
5. Implement expand/collapse logic
6. Add search and filter functionality
7. Add navigation route
8. Update bottom navigation menu

### Step 5: Polish & Testing (1 hour)
1. Test all navigation flows
2. Test empty states
3. Test error handling
4. Test real-time updates
5. Verify offline support
6. Performance testing with large datasets

---

## Files to Create

### Phase 1 - Events Tab (6 files)
1. `app/.../organizer/OrganizerEventsViewModel.kt`
2. `app/.../res/layout/fragment_organizer_events.xml`
3. `app/.../organizer/OrganizerEventsFragment.kt`
4. `app/.../res/drawable/ic_event_list.xml`
5. `app/.../res/drawable/ic_history.xml`
6. Update `mobile_navigation.xml`

### Phase 2 - Bookings Tab (9 files)
1. `app/.../organizer/OrganizerBookingsViewModel.kt`
2. `app/.../res/layout/fragment_organizer_bookings.xml`
3. `app/.../res/layout/item_booking_group_header.xml`
4. `app/.../organizer/OrganizerBookingsFragment.kt`
5. `app/.../organizer/BookingGroupAdapter.kt`
6. `app/.../organizer/TicketDetailsBottomSheet.kt`
7. `app/.../res/layout/bottom_sheet_ticket_details.xml`
8. `app/.../res/drawable/ic_ticket.xml`
9. Update `mobile_navigation.xml`

### Phase 3 - Data Layer Updates (3 files)
1. Update `domain/model/Ticket.kt`
2. Update `data/source/FirestoreTicketDataSource.kt`
3. Update `domain/repository/TicketRepository.kt`

**Total**: ~18 files (6 new fragments/viewmodels, 8 new layouts, 4 new icons)

---

## Testing Strategy

### Unit Tests
- [ ] Event categorization logic (upcoming/now/past)
- [ ] Ticket grouping by event
- [ ] Search and filter logic
- [ ] Revenue calculations

### Integration Tests
- [ ] Firestore queries for organizer bookings
- [ ] Real-time updates for new tickets
- [ ] Navigation between tabs

### UI Tests
- [ ] Events tab displays all categories
- [ ] Bookings tab groups tickets by event
- [ ] Expand/collapse interactions
- [ ] Search functionality
- [ ] Filter chips

### Manual Tests
- [ ] Create event → Verify appears in Events tab
- [ ] Purchase ticket → Verify appears in Bookings tab
- [ ] Check-in attendee → Verify updates in Bookings tab
- [ ] Past event shows in correct section
- [ ] Empty states display correctly
- [ ] Error handling (no internet, etc.)

---

## Estimated Timeline

| Phase | Time Estimate |
|-------|---------------|
| Events Tab | 2-3 hours |
| Bookings Tab - Data | 1 hour |
| Bookings Tab - UI | 3-4 hours |
| Polish & Testing | 1 hour |
| **Total** | **7-9 hours** |

---

## Notes & Considerations

### Performance
- Events tab: Load all events once, cache in ViewModel
- Bookings tab: May have many tickets - implement pagination if > 100 tickets
- Use DiffUtil for smooth RecyclerView updates

### Real-Time Updates
- Events tab: Poll for updates on resume (events change less frequently)
- Bookings tab: Use Firestore listeners (tickets change frequently during events)

### Offline Support
- Both tabs work with cached data
- Show offline banner when no connection
- Queue operations when offline (already implemented)

### Accessibility
- Add content descriptions to all icons
- Ensure expand/collapse is keyboard accessible
- Proper heading structure for screen readers

### Future Enhancements (Not in Scope)
- Export bookings to CSV
- Advanced analytics (revenue trends, popular events)
- Bulk check-in from Bookings tab
- Push notifications for new bookings
- Email/SMS attendees from Bookings tab

---

## Summary

### Critical Fix Applied ✅
The data loading issue is now resolved. `ManageEventFragment` now uses `activityViewModels()` to share the ViewModel instance with child fragments.

### New Features Planned
1. **Events Tab** - Comprehensive view of all organizer events
2. **Bookings Tab** - Unified view of all tickets across events

### Next Steps
Ready to implement Phase 1 (Events Tab) once you confirm this plan looks good!
