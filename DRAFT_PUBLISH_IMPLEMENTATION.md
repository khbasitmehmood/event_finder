# Draft/Publish Event Feature Implementation

## Status: ✅ Partially Complete (Core functionality ready)

---

## What's Implemented ✅

### 1. Repository Layer
- ✅ Added `getDraftEvents(organizerId)` method
- ✅ Added `publishEvent(eventId, organizerId)` method
- ✅ Implemented in EventRepositoryImpl, FirestoreEventDataSource, DummyEventDataSource
- ✅ Publish validates ownership and state (must be DRAFT)

### 2. Create Event Screen
- ✅ Two buttons: "Save as Draft" and "Publish Event"
- ✅ "Save as Draft" only requires basic info (title, category, date)
- ✅ "Publish" requires all fields validated
- ✅ Events saved with `EventState.DRAFT` or `EventState.SCHEDULED`
- ✅ Draft events have `publishedAt = null`

### 3. State Management
- ✅ DRAFT events filtered from public views (explore, home, search)
- ✅ Only organizer can see their draft events
- ✅ State transition tracked: DRAFT → SCHEDULED with reason "Event published"

---

## What's Left TODO ⏳

### Phase 3: Organizer Events Screen - Add Draft Tab

**File to modify:** `OrganizerEventsFragment.kt`

#### Current State
- Shows only published events (SCHEDULED, LIVE, COMPLETED, etc.)

#### Needed Changes
1. Add TabLayout with two tabs:
   - **"Published"** - Shows non-draft events
   - **"Drafts"** - Shows DRAFT events

2. Update ViewModel to fetch both:
```kotlin
fun loadPublishedEvents(organizerId: String) {
    viewModelScope.launch {
        val states = listOf(
            EventState.SCHEDULED,
            EventState.LIVE,
            EventState.POSTPONED,
            EventState.COMPLETED,
            EventState.CANCELLED
        )
        val result = eventRepository.getEventsByStates(states)
            .filter { it.organizerId == organizerId }
        // Update UI state
    }
}

fun loadDraftEvents(organizerId: String) {
    viewModelScope.launch {
        val result = eventRepository.getDraftEvents(organizerId)
        // Update UI state
    }
}
```

3. Add tab switching logic

---

### Phase 4: Publish Action in Manage Event

**File to modify:** `ManageEventFragment.kt`

#### Current State
- Shows event actions: Postpone, Reschedule, Cancel
- Missing: Publish action for drafts

#### Needed Changes
1. Check if event is DRAFT in `setupEventActions()`:
```kotlin
private fun setupEventActions() {
    val event = currentEvent ?: return
    
    if (event.state == EventState.DRAFT) {
        // Show Publish button
        binding.btnPublish.isVisible = true
        binding.btnPublish.setOnClickListener {
            showPublishDialog()
        }
    } else {
        // Show normal actions (postpone, reschedule, cancel)
        binding.btnPublish.isVisible = false
    }
}
```

2. Add publish dialog:
```kotlin
private fun showPublishDialog() {
    MaterialAlertDialogBuilder(requireContext())
        .setTitle("Publish Event")
        .setMessage("Are you ready to publish this event? It will become visible to all users.")
        .setPositiveButton("Publish") { _, _ ->
            publishEvent()
        }
        .setNegativeButton("Cancel", null)
        .show()
}

private fun publishEvent() {
    viewLifecycleOwner.lifecycleScope.launch {
        val result = eventRepository.publishEvent(
            eventId = currentEvent?.id ?: return@launch,
            organizerId = userPreferences.getUserId()
        )
        
        result.onSuccess {
            Toast.makeText(context, "Event published successfully!", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }.onFailure { error ->
            Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()
        }
    }
}
```

3. Add Publish button to layout (`fragment_manage_event.xml`):
```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/btnPublish"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="Publish Event"
    android:visibility="gone"
    app:icon="@drawable/ic_publish" />
```

---

### Phase 5: Filter Drafts from Public Views ✅ DONE

Already implemented - `getUserEvents()` filters DRAFT events:
```kotlin
events.filter { it.state != EventState.DRAFT }
```

---

## Testing Checklist

### Create Draft Event
- [ ] Open Create Event screen
- [ ] Fill only: Title, Category, Date, Time
- [ ] Click "Save as Draft"
- [ ] Verify event saved with state = DRAFT
- [ ] Verify publishedAt = null

### View Draft Events
- [ ] Go to Organizer Events screen
- [ ] Click "Drafts" tab
- [ ] See your draft event
- [ ] Verify it's NOT in "Published" tab
- [ ] Verify it's NOT visible in Explore/Home (public views)

### Publish Draft Event
- [ ] Click on draft event → Manage Event
- [ ] See "Publish" button
- [ ] Click Publish
- [ ] Verify state changes to SCHEDULED
- [ ] Verify publishedAt is set
- [ ] Verify event now appears in "Published" tab
- [ ] Verify event now visible in Explore/Home

### Create Published Event
- [ ] Open Create Event screen
- [ ] Fill all required fields
- [ ] Click "Publish Event"
- [ ] Verify event created with state = SCHEDULED
- [ ] Verify immediately visible in Explore/Home
- [ ] Verify in "Published" tab, NOT in "Drafts" tab

---

## Implementation Priority

1. **HIGH**: Phase 4 - Publish action in Manage Event
   - Users need a way to publish draft events
   - Currently no way to convert DRAFT → SCHEDULED

2. **MEDIUM**: Phase 3 - Draft tab in Organizer Events
   - Nice to have for organization
   - Can manually navigate to draft events via database

3. **LOW**: UI polish
   - Draft badge/indicator
   - Better messaging

---

## Quick Implementation Guide

### For Phase 3 (Draft Tab):

1. Add TabLayout to `fragment_organizer_events.xml`:
```xml
<com.google.android.material.tabs.TabLayout
    android:id="@+id/tabLayout"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:tabMode="fixed">
    
    <com.google.android.material.tabs.TabItem
        android:text="Published" />
    
    <com.google.android.material.tabs.TabItem
        android:text="Drafts" />
</com.google.android.material.tabs.TabLayout>
```

2. In `OrganizerEventsFragment.kt`:
```kotlin
binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
    override fun onTabSelected(tab: TabLayout.Tab?) {
        when (tab?.position) {
            0 -> viewModel.loadPublishedEvents(userId)
            1 -> viewModel.loadDraftEvents(userId)
        }
    }
    // ... other methods
})
```

### For Phase 4 (Publish Button):

1. Add method to `ManageEventSharedViewModel.kt`:
```kotlin
fun publishEvent(eventId: String, organizerId: String) {
    viewModelScope.launch {
        val result = eventRepository.publishEvent(eventId, organizerId)
        result.onSuccess { event ->
            _event.value = event
            // Navigate back or show success
        }.onFailure { error ->
            // Show error
        }
    }
}
```

2. Wire up in `ManageEventFragment.kt` (see Phase 4 section above)

---

## Current Behavior

**Creating Events:**
- ✅ "Save as Draft" → Creates DRAFT event (not public)
- ✅ "Publish Event" → Creates SCHEDULED event (public immediately)

**Viewing Events:**
- ✅ Public views (Explore, Home, Search) → Only show non-DRAFT events
- ✅ My Events → Shows all your events (DRAFT and non-DRAFT mixed)
- ⏳ Need separate Draft tab

**Managing Events:**
- ⏳ No way to publish DRAFT events yet
- ⏳ Need Publish button in Manage Event screen

---

## Files Modified

1. ✅ `EventRepository.kt` - Added interface methods
2. ✅ `EventRepositoryImpl.kt` - Implemented methods
3. ✅ `EventDataSource.kt` - Added interface method
4. ✅ `FirestoreEventDataSource.kt` - Implemented publishEvent()
5. ✅ `DummyEventDataSource.kt` - Implemented publishEvent()
6. ✅ `CreateEventViewModel.kt` - Added saveAsDraft parameter
7. ✅ `CreateEventFragment.kt` - Wired up both buttons

## Files to Modify Next

8. ⏳ `OrganizerEventsFragment.kt` - Add Draft tab
9. ⏳ `fragment_organizer_events.xml` - Add TabLayout
10. ⏳ `ManageEventFragment.kt` - Add Publish button/action
11. ⏳ `fragment_manage_event.xml` - Add Publish button
12. ⏳ `ManageEventSharedViewModel.kt` - Add publishEvent method

---

## Summary

**Core functionality (80%) is ready!** You can now:
- ✅ Create draft events
- ✅ Create published events
- ✅ Drafts are hidden from public
- ⏳ Need UI to publish drafts (Phase 4 - HIGH priority)
- ⏳ Need UI to view drafts separately (Phase 3 - MEDIUM priority)

The backend is complete. Just need the frontend UI to expose the functionality.
