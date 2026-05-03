# Real-Time Updates Implementation

## Overview
Implemented Firestore snapshot listeners for real-time updates to attendee list and event statistics. The organizer's view now automatically refreshes when data changes in the database (e.g., after QR scan check-in).

---

## What Was Implemented

### 1. Firestore Snapshot Listeners

**Added to:** `FirestoreTicketDataSource.kt`

#### observeEventAttendees()
```kotlin
override fun observeEventAttendees(eventId: String): Flow<List<Ticket>> = callbackFlow {
    val listener = ticketsCollection
        .whereEqualTo("eventId", eventId)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val tickets = snapshot.documents.mapNotNull { doc ->
                    try {
                        val dto = doc.toObject(TicketDto::class.java)
                        dto?.let { TicketMapper.toDomain(it) }
                    } catch (e: Exception) {
                        null
                    }
                }
                trySend(tickets)
            }
        }

    awaitClose { listener.remove() }
}
```

**How it works:**
- Creates a Firestore snapshot listener on the tickets collection
- Filters by eventId to get only relevant tickets
- Emits updates via Kotlin Flow whenever data changes
- Properly cleans up listener when Flow is cancelled

#### observeEventStats()
```kotlin
override fun observeEventStats(eventId: String): Flow<EventStats?> = callbackFlow {
    val listener = statsCollection
        .document(eventId)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                try {
                    val dto = snapshot.toObject(EventStatsDto::class.java)
                    val stats = dto?.let { EventStatsMapper.toDomain(it) }
                    trySend(stats)
                } catch (e: Exception) {
                    trySend(null)
                }
            } else {
                trySend(null)
            }
        }

    awaitClose { listener.remove() }
}
```

**How it works:**
- Listens to a specific event_stats document
- Emits updates whenever stats change
- Handles non-existent documents gracefully

---

### 2. Updated Repository Layer

**File:** `TicketRepositoryImpl.kt`

#### Added Flow-based methods
```kotlin
override fun observeEventAttendees(eventId: String): Flow<Result<List<Ticket>>> {
    return ticketDataSource.observeEventAttendees(eventId)
        .map { tickets -> Result.success(tickets) }
        .catch { e -> emit(Result.failure(e)) }
}

override fun observeEventStats(eventId: String): Flow<Result<EventStats?>> {
    return ticketDataSource.observeEventStats(eventId)
        .map { stats ->
            Result.success(stats ?: EventStats(
                eventId = eventId,
                totalTickets = 0,
                checkedInCount = 0,
                reservedCount = 0,
                cancelledCount = 0,
                totalRevenue = 0.0
            ))
        }
        .catch { e -> emit(Result.failure(e)) }
}
```

**Key Features:**
- Wraps data source Flow in Result type
- Provides default empty stats if none exist
- Error handling with catch operator

---

### 3. Updated ViewModel to Use Listeners

**File:** `ManageEventSharedViewModel.kt`

#### Before (One-time Fetch):
```kotlin
fun loadEventData(eventId: String) {
    viewModelScope.launch {
        ticketRepository.getEventAttendees(eventId).fold(...)
        ticketRepository.getEventStats(eventId).fold(...)
    }
}
```

#### After (Real-Time Listeners):
```kotlin
fun loadEventData(eventId: String) {
    if (currentEventId == eventId) {
        return // Already observing this event
    }
    currentEventId = eventId

    // Start observing attendees in real-time
    viewModelScope.launch {
        ticketRepository.observeEventAttendees(eventId).collect { result ->
            _isLoading.value = false
            result.fold(
                onSuccess = { attendees ->
                    _attendees.value = attendees
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to load attendees"
                }
            )
        }
    }

    // Start observing stats in real-time
    viewModelScope.launch {
        ticketRepository.observeEventStats(eventId).collect { result ->
            result.fold(
                onSuccess = { stats ->
                    _eventStats.value = stats
                },
                onFailure = { exception ->
                    if (_error.value == null) {
                        _error.value = exception.message ?: "Failed to load stats"
                    }
                }
            )
        }
    }
}
```

**Key Changes:**
- Launches two coroutines that continuously collect from Flows
- Listeners remain active for the lifecycle of the ViewModel
- Prevents duplicate subscriptions with eventId check

---

## How Real-Time Updates Work

### User Flow
1. **Organizer opens ManageEventFragment**
   - ViewModel calls `loadEventData(eventId)`
   - Firestore listeners start observing

2. **Organizer scans QR code and checks in attendee**
   - `checkInTicket()` updates ticket status in Firestore
   - Updates event_stats atomically

3. **Firestore triggers snapshot listeners**
   - Attendees listener detects ticket status change
   - Stats listener detects updated counts

4. **Updates flow through layers**
   - DataSource emits new data via Flow
   - Repository wraps in Result and forwards
   - ViewModel updates StateFlow
   - UI automatically refreshes (observing StateFlow)

5. **UI reflects changes instantly**
   - Attendees tab shows updated check-in status
   - Insights tab shows new check-in count and percentage

### No Manual Refresh Needed
- Pull-to-refresh still available but optional
- Auto-refresh on resume is now redundant (but harmless)
- Data stays in sync automatically

---

## Benefits

### 1. Instant Updates
- Organizer sees check-ins immediately
- No need to leave and re-enter screen
- Multiple organizers can check in attendees simultaneously

### 2. Better User Experience
- Feels more responsive and "live"
- Reduces confusion about data freshness
- Professional event management experience

### 3. Reduced Network Calls
- One listener vs. repeated polling
- More efficient than manual refresh
- Firestore handles optimization

### 4. Clean Architecture Maintained
- Flow-based reactive pattern
- Proper separation of concerns
- Repository abstracts Firestore details

---

## Technical Details

### Kotlin Flow with callbackFlow
```kotlin
fun observeData(): Flow<Data> = callbackFlow {
    val listener = firestore.addSnapshotListener { snapshot, error ->
        if (error != null) {
            close(error) // Close flow on error
            return@addSnapshotListener
        }
        trySend(data) // Emit data to flow
    }
    
    awaitClose { listener.remove() } // Cleanup when flow cancelled
}
```

**Key Components:**
- `callbackFlow` - Converts callback-based APIs to Flow
- `trySend()` - Non-blocking emission of values
- `awaitClose {}` - Cleanup block when Flow is cancelled
- `listener.remove()` - Unsubscribes from Firestore

### Lifecycle Management
- Listeners created in `loadEventData()`
- Tied to `viewModelScope` (cancelled when ViewModel cleared)
- Fragment observes ViewModel with `repeatOnLifecycle(STARTED)`
- Automatic cleanup when fragment stops

### Error Handling
- Firestore errors close the Flow
- Repository catches errors and emits `Result.failure()`
- ViewModel shows error to user via Toast
- No crashes from network issues

---

## Performance Considerations

### Firestore Costs
- **Read Operations:** Charged per document read
- **Listener Overhead:** One snapshot per change
- **Initial Load:** Counts as one read per document
- **Updates:** Only changed documents count as reads

### Optimization Strategies
1. **Single Listener:** One listener per event (not per attendee)
2. **Efficient Queries:** Filter by eventId at Firestore level
3. **Proper Cleanup:** Remove listeners when not needed
4. **Caching:** Firestore caches data locally automatically

### Expected Costs (Example Event)
- **Initial Load:** 100 attendees = 100 reads
- **One Check-In:** 2 reads (1 ticket update + 1 stats update)
- **10 Check-Ins:** 20 reads total
- **Very Affordable:** Firestore free tier is 50K reads/day

---

## Files Modified

### Updated Files (6)
1. **TicketDataSource.kt** - Added Flow-based method signatures
2. **FirestoreTicketDataSource.kt** - Implemented snapshot listeners
3. **TicketRepository.kt** - Added Flow-based method signatures
4. **TicketRepositoryImpl.kt** - Implemented Flow wrappers
5. **ManageEventSharedViewModel.kt** - Changed to use listeners
6. **No Fragment Changes** - Transparent to UI layer

---

## Testing Checklist

### Manual Testing
- [x] Build succeeds without errors
- [ ] Open ManageEventFragment - data loads
- [ ] Scan QR and check in attendee
- [ ] Verify Attendees tab updates automatically
- [ ] Verify Insights tab updates check-in count
- [ ] Verify no manual refresh needed
- [ ] Test with multiple organizers (if possible)

### Edge Cases
- [ ] Network disconnection - listeners should reconnect
- [ ] Background/foreground transitions
- [ ] Multiple rapid check-ins
- [ ] Large attendee list (100+ attendees)

### Cleanup Testing
- [ ] Navigate away - listeners should stop
- [ ] Return to screen - listeners should restart
- [ ] Memory leaks - use Android Profiler

---

## Known Limitations

### 1. No Offline Support Yet
- Listeners require network connection
- Firestore SDK handles reconnection automatically
- Planned for future enhancement

### 2. Pull-to-Refresh Still Present
- Now mostly redundant
- Kept for explicit user control
- Can be removed in future if desired

### 3. Auto-Refresh on Resume
- Also redundant with real-time listeners
- Kept for consistency
- Harmless but unnecessary network call

---

## Future Enhancements

### Short Term
1. **Remove redundant refresh logic**
   - Remove auto-refresh in `onResume()`
   - Keep pull-to-refresh as manual override

2. **Add connection state indicator**
   - Show when listeners are active
   - Indicate when disconnected

3. **Implement offline persistence**
   - Enable Firestore offline mode
   - Cache data locally
   - Sync when reconnected

### Long Term
1. **Pagination for large events**
   - Load attendees in chunks
   - Virtual scrolling for 1000+ attendees

2. **Optimistic updates**
   - Update UI immediately on check-in
   - Revert if Firestore update fails

3. **Advanced caching strategy**
   - Persist to Room database
   - Reduce Firestore reads further

---

## Migration Notes

### Breaking Changes
**None** - Backward compatible

### Rollback Plan
If issues arise, can revert to one-time fetch by:
1. Reverting `ManageEventSharedViewModel.kt` changes
2. Using `getEventAttendees()` instead of `observeEventAttendees()`
3. No data model changes needed

### Deployment Considerations
- No database migration needed
- Works with existing Firestore structure
- Safe to deploy incrementally

---

## Summary

Successfully implemented **real-time Firestore listeners** using Kotlin Flow and callbackFlow. The organizer's event management interface now automatically updates when attendees are checked in, providing a seamless, live experience.

**Key Achievement:** Transformed from a "pull" model (manual refresh) to a "push" model (automatic updates) while maintaining clean architecture and proper lifecycle management.

**Impact:** 
- ✅ Better user experience
- ✅ Reduced manual refresh actions
- ✅ More professional feel
- ✅ Efficient network usage
- ✅ Scalable to multiple organizers

**Status:** ✅ Complete and ready for testing
