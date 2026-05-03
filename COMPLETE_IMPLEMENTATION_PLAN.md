# Complete Implementation Plan
## Events Tab, Bookings Tab & Save Draft Feature

---

## Overview

Implementing three major features for the Event Finder app:
1. **Events Tab** - Show ALL organizer events (upcoming, past, happening now)
2. **Bookings Tab** - Show all tickets grouped by event with search/filter
3. **Save Draft** - Allow organizers to save incomplete events as drafts

---

## Current Status ✅

### Completed
- [x] Fixed critical ViewModel scoping bug in ManageEventFragment
- [x] Implemented real data loading in ManageEventOverviewFragment
- [x] Removed hardcoded values from ManageEventInsightsFragment
- [x] All three ManageEvent tabs now display real-time data
- [x] Build successful, ready for testing

---

## Phase 1: Events Tab Implementation

### Goal
Create a comprehensive events list showing ALL organizer's events categorized by status (Happening Now, Upcoming, Past).

### Timeline: 2-3 hours

---

### 1.1 ViewModel Layer

**New File:** `app/src/main/java/com/eventfinder/app/client/organizer/OrganizerEventsViewModel.kt`

```kotlin
@HiltViewModel
class OrganizerEventsViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventsUiState())
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

    data class EventsUiState(
        val upcomingEvents: List<Event> = emptyList(),
        val happeningNowEvents: List<Event> = emptyList(),
        val pastEvents: List<Event> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null
    )

    init {
        loadEvents()
    }

    fun loadEvents() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val organizerId = userPreferences.getUserId()
            if (organizerId == null) {
                _uiState.update { 
                    it.copy(isLoading = false, error = "User not logged in") 
                }
                return@launch
            }

            eventRepository.getOrganizerEvents(organizerId).fold(
                onSuccess = { events ->
                    val (upcoming, happeningNow, past) = categorizeEvents(events)
                    _uiState.update {
                        it.copy(
                            upcomingEvents = upcoming,
                            happeningNowEvents = happeningNow,
                            pastEvents = past,
                            isLoading = false
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load events"
                        )
                    }
                }
            )
        }
    }

    private fun categorizeEvents(events: List<Event>): Triple<List<Event>, List<Event>, List<Event>> {
        val now = System.currentTimeMillis()
        
        val upcoming = events
            .filter { it.startTime > now }
            .sortedBy { it.startTime }
        
        val happeningNow = events
            .filter { it.startTime <= now && (it.endTime ?: Long.MAX_VALUE) >= now }
            .sortedBy { it.startTime }
        
        val past = events
            .filter { (it.endTime ?: it.startTime) < now }
            .sortedByDescending { it.endTime ?: it.startTime }
        
        return Triple(upcoming, happeningNow, past)
    }

    fun refreshEvents() {
        loadEvents()
    }
}
```

---

### 1.2 UI Layer

**New File:** `app/src/main/java/com/eventfinder/app/client/organizer/OrganizerEventsFragment.kt`

```kotlin
@AndroidEntryPoint
class OrganizerEventsFragment : Fragment() {

    private var _binding: FragmentOrganizerEventsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OrganizerEventsViewModel by viewModels()

    private lateinit var happeningNowAdapter: EventAdapter
    private lateinit var upcomingAdapter: EventAdapter
    private lateinit var pastAdapter: EventAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrganizerEventsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerViews()
        setupSwipeRefresh()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        // Happening Now
        happeningNowAdapter = EventAdapter(emptyList(), onClick = ::onEventClick)
        binding.rvHappeningNow.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = happeningNowAdapter
        }

        // Upcoming
        upcomingAdapter = EventAdapter(emptyList(), onClick = ::onEventClick)
        binding.rvUpcoming.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = upcomingAdapter
        }

        // Past
        pastAdapter = EventAdapter(emptyList(), onClick = ::onEventClick)
        binding.rvPast.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pastAdapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshEvents()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.swipeRefresh.isRefreshing = state.isLoading

                    // Happening Now Section
                    if (state.happeningNowEvents.isNotEmpty()) {
                        binding.sectionHappeningNow.isVisible = true
                        binding.tvHappeningNowCount.text = "${state.happeningNowEvents.size}"
                        happeningNowAdapter.updateEvents(state.happeningNowEvents)
                    } else {
                        binding.sectionHappeningNow.isVisible = false
                    }

                    // Upcoming Section
                    if (state.upcomingEvents.isNotEmpty()) {
                        binding.sectionUpcoming.isVisible = true
                        binding.tvUpcomingCount.text = "${state.upcomingEvents.size}"
                        binding.emptyUpcoming.isVisible = false
                        upcomingAdapter.updateEvents(state.upcomingEvents)
                    } else {
                        binding.sectionUpcoming.isVisible = true
                        binding.rvUpcoming.isVisible = false
                        binding.emptyUpcoming.isVisible = true
                    }

                    // Past Section
                    if (state.pastEvents.isNotEmpty()) {
                        binding.sectionPast.isVisible = true
                        binding.tvPastCount.text = "${state.pastEvents.size}"
                        binding.emptyPast.isVisible = false
                        pastAdapter.updateEvents(state.pastEvents)
                    } else {
                        binding.sectionPast.isVisible = true
                        binding.rvPast.isVisible = false
                        binding.emptyPast.isVisible = true
                    }

                    // Show "all empty" state if no events at all
                    val allEmpty = state.happeningNowEvents.isEmpty() && 
                                   state.upcomingEvents.isEmpty() && 
                                   state.pastEvents.isEmpty()
                    binding.layoutAllEmpty.isVisible = allEmpty && !state.isLoading

                    // Error handling
                    if (state.error != null) {
                        Toast.makeText(context, state.error, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun onEventClick(event: Event) {
        val bundle = Bundle().apply {
            putString("EVENT_ID", event.eventId)
        }
        findNavController().navigate(R.id.manageEventFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

---

### 1.3 Layout Files

**New File:** `app/src/main/res/layout/fragment_organizer_events.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.swiperefreshlayout.widget.SwipeRefreshLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/swipeRefresh"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <androidx.core.widget.NestedScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:fillViewport="true">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="@dimen/spacing_large">

            <!-- Happening Now Section -->
            <LinearLayout
                android:id="@+id/sectionHappeningNow"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:visibility="gone"
                tools:visibility="visible">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal">

                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="Happening Now"
                        android:textAppearance="@style/TextAppearance.App.TitleMedium" />

                    <com.google.android.material.chip.Chip
                        android:id="@+id/tvHappeningNowCount"
                        style="@style/Widget.MaterialComponents.Chip.Action"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="0"
                        android:textAppearance="@style/TextAppearance.App.LabelSmall" />
                </LinearLayout>

                <androidx.recyclerview.widget.RecyclerView
                    android:id="@+id/rvHappeningNow"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="@dimen/spacing_medium"
                    android:nestedScrollingEnabled="false"
                    tools:itemCount="2"
                    tools:listitem="@layout/item_event" />
            </LinearLayout>

            <!-- Upcoming Section -->
            <LinearLayout
                android:id="@+id/sectionUpcoming"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/spacing_large"
                android:orientation="vertical">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal">

                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="Upcoming"
                        android:textAppearance="@style/TextAppearance.App.TitleMedium" />

                    <com.google.android.material.chip.Chip
                        android:id="@+id/tvUpcomingCount"
                        style="@style/Widget.MaterialComponents.Chip.Action"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="0"
                        android:textAppearance="@style/TextAppearance.App.LabelSmall" />
                </LinearLayout>

                <androidx.recyclerview.widget.RecyclerView
                    android:id="@+id/rvUpcoming"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="@dimen/spacing_medium"
                    android:nestedScrollingEnabled="false"
                    tools:itemCount="3"
                    tools:listitem="@layout/item_event" />

                <include
                    android:id="@+id/emptyUpcoming"
                    layout="@layout/view_empty_state_simple"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:visibility="gone" />
            </LinearLayout>

            <!-- Past Section -->
            <LinearLayout
                android:id="@+id/sectionPast"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/spacing_large"
                android:orientation="vertical">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal">

                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="Past Events"
                        android:textAppearance="@style/TextAppearance.App.TitleMedium" />

                    <com.google.android.material.chip.Chip
                        android:id="@+id/tvPastCount"
                        style="@style/Widget.MaterialComponents.Chip.Action"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="0"
                        android:textAppearance="@style/TextAppearance.App.LabelSmall" />
                </LinearLayout>

                <androidx.recyclerview.widget.RecyclerView
                    android:id="@+id/rvPast"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="@dimen/spacing_medium"
                    android:nestedScrollingEnabled="false"
                    tools:itemCount="2"
                    tools:listitem="@layout/item_event" />

                <include
                    android:id="@+id/emptyPast"
                    layout="@layout/view_empty_state_simple"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:visibility="gone" />
            </LinearLayout>

            <!-- All Empty State -->
            <LinearLayout
                android:id="@+id/layoutAllEmpty"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:gravity="center"
                android:orientation="vertical"
                android:visibility="gone"
                tools:visibility="visible">

                <ImageView
                    android:layout_width="120dp"
                    android:layout_height="120dp"
                    android:alpha="0.3"
                    android:contentDescription="No events"
                    android:src="@drawable/ic_event_list"
                    app:tint="?attr/colorOnSurfaceVariant" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="@dimen/spacing_large"
                    android:text="No Events Yet"
                    android:textAppearance="@style/TextAppearance.App.TitleMedium" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="@dimen/spacing_small"
                    android:gravity="center"
                    android:text="Create your first event to get started"
                    android:textAppearance="@style/TextAppearance.App.BodyMedium"
                    android:textColor="?attr/colorOnSurfaceVariant" />

                <com.google.android.material.button.MaterialButton
                    android:id="@+id/btnCreateEvent"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="@dimen/spacing_large"
                    android:text="Create Event"
                    app:icon="@drawable/ic_add" />
            </LinearLayout>

        </LinearLayout>
    </androidx.core.widget.NestedScrollView>

</androidx.swiperefreshlayout.widget.SwipeRefreshLayout>
```

**New File:** `app/src/main/res/layout/view_empty_state_simple.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:gravity="center"
    android:orientation="vertical"
    android:padding="@dimen/spacing_xlarge">

    <ImageView
        android:id="@+id/ivEmptyIcon"
        android:layout_width="80dp"
        android:layout_height="80dp"
        android:alpha="0.3"
        android:src="@drawable/ic_event"
        app:tint="?attr/colorOnSurfaceVariant" />

    <TextView
        android:id="@+id/tvEmptyMessage"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="@dimen/spacing_medium"
        android:text="No events in this category"
        android:textAppearance="@style/TextAppearance.App.BodyMedium"
        android:textColor="?attr/colorOnSurfaceVariant" />

</LinearLayout>
```

---

### 1.4 Drawable Resources

**New File:** `app/src/main/res/drawable/ic_event_list.xml`

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M19,3h-1V1h-2v2H8V1H6v2H5C3.89,3 3.01,3.9 3.01,5L3,19c0,1.1 0.89,2 2,2h14c1.1,0 2,-0.9 2,-2V5C21,3.9 20.1,3 19,3zM19,19H5V9h14V19zM7,11h2v2H7V11zM11,11h2v2h-2V11zM15,11h2v2h-2V11z"/>
</vector>
```

**New File:** `app/src/main/res/drawable/ic_history.xml`

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M13,3c-4.97,0 -9,4.03 -9,9L1,12l3.89,3.89 0.07,0.14L9,12L6,12c0,-3.87 3.13,-7 7,-7s7,3.13 7,7 -3.13,7 -7,7c-1.93,0 -3.68,-0.79 -4.94,-2.06l-1.42,1.42C8.27,19.99 10.51,21 13,21c4.97,0 9,-4.03 9,-9s-4.03,-9 -9,-9zM12,8v5l4.28,2.54 0.72,-1.21 -3.5,-2.08L13.5,8L12,8z"/>
</vector>
```

---

### 1.5 Navigation Updates

**Update:** `app/src/main/res/navigation/mobile_navigation.xml`

```xml
<fragment
    android:id="@+id/organizerEventsFragment"
    android:name="com.eventfinder.app.client.organizer.OrganizerEventsFragment"
    android:label="My Events" />
```

**Update:** `app/src/main/res/menu/organizer_bottom_nav_menu.xml` (or create if doesn't exist)

```xml
<item
    android:id="@+id/organizerEventsFragment"
    android:icon="@drawable/ic_event_list"
    android:title="Events" />
```

---

## Phase 2: Bookings Tab Implementation

### Goal
Show all tickets purchased for organizer's events, grouped by event, with search/filter functionality. NO revenue calculations per user request.

### Timeline: 4-5 hours

---

### 2.1 Data Layer - Add organizerId to Tickets

**Update:** `app/src/main/java/com/eventfinder/app/domain/model/Ticket.kt`

```kotlin
data class Ticket(
    val ticketId: String = "",
    val eventId: String = "",
    val eventTitle: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val organizerId: String = "",  // ⭐ NEW FIELD
    // ... rest of fields
)
```

**Update:** `app/src/main/java/com/eventfinder/app/data/source/FirestoreTicketDataSource.kt`

Add new method:

```kotlin
override suspend fun getOrganizerBookings(organizerId: String): Result<List<Ticket>> {
    return try {
        val snapshot = ticketsCollection
            .whereEqualTo("organizerId", organizerId)
            .get()
            .await()

        val tickets = snapshot.documents.mapNotNull { doc ->
            doc.toObject(TicketDto::class.java)?.toDomain()
        }

        Result.success(tickets)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

**Update:** `app/src/main/java/com/eventfinder/app/domain/repository/TicketRepository.kt`

Add interface method:

```kotlin
interface TicketRepository {
    // ... existing methods
    suspend fun getOrganizerBookings(organizerId: String): Result<List<Ticket>>
}
```

---

### 2.2 ViewModel Layer

**New File:** `app/src/main/java/com/eventfinder/app/client/organizer/OrganizerBookingsViewModel.kt`

```kotlin
@HiltViewModel
class OrganizerBookingsViewModel @Inject constructor(
    private val ticketRepository: TicketRepository,
    private val eventRepository: EventRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingsUiState())
    val uiState: StateFlow<BookingsUiState> = _uiState.asStateFlow()

    private var allBookings: List<BookingGroup> = emptyList()
    private var currentFilter: FilterType = FilterType.ALL
    private var currentSearchQuery: String = ""

    data class BookingsUiState(
        val bookingGroups: List<BookingGroup> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val isEmpty: Boolean = false
    )

    data class BookingGroup(
        val event: Event,
        val tickets: List<Ticket>,
        val totalCount: Int,
        val checkInCount: Int,
        val paidCount: Int,
        val freeCount: Int,
        val cancelledCount: Int,
        val isExpanded: Boolean = false
    )

    enum class FilterType {
        ALL, PAID, FREE, PENDING, CHECKED_IN, CANCELLED
    }

    init {
        loadBookings()
    }

    fun loadBookings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val organizerId = userPreferences.getUserId()
            if (organizerId == null) {
                _uiState.update {
                    it.copy(isLoading = false, error = "User not logged in")
                }
                return@launch
            }

            // Get all tickets for organizer's events
            ticketRepository.getOrganizerBookings(organizerId).fold(
                onSuccess = { tickets ->
                    // Get unique event IDs
                    val eventIds = tickets.map { it.eventId }.distinct()
                    
                    // Fetch event details for each
                    val events = mutableMapOf<String, Event>()
                    eventIds.forEach { eventId ->
                        eventRepository.getEventById(eventId).fold(
                            onSuccess = { event -> events[eventId] = event },
                            onFailure = { /* Skip if event not found */ }
                        )
                    }

                    // Group tickets by event
                    val grouped = tickets.groupBy { it.eventId }.mapNotNull { (eventId, eventTickets) ->
                        val event = events[eventId] ?: return@mapNotNull null
                        
                        BookingGroup(
                            event = event,
                            tickets = eventTickets,
                            totalCount = eventTickets.count { it.status != TicketStatus.CANCELLED },
                            checkInCount = eventTickets.count { it.status == TicketStatus.CHECKED_IN },
                            paidCount = eventTickets.count { 
                                it.ticketType == TicketType.PAID && it.status != TicketStatus.CANCELLED 
                            },
                            freeCount = eventTickets.count {
                                (it.ticketType == TicketType.FREE_PRIVATE || 
                                 it.ticketType == TicketType.PUBLIC_RESERVATION) &&
                                it.status != TicketStatus.CANCELLED
                            },
                            cancelledCount = eventTickets.count { it.status == TicketStatus.CANCELLED }
                        )
                    }.sortedByDescending { it.event.startTime }

                    allBookings = grouped
                    applyFiltersAndSearch()
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load bookings"
                        )
                    }
                }
            )
        }
    }

    fun setFilter(filter: FilterType) {
        currentFilter = filter
        applyFiltersAndSearch()
    }

    fun setSearchQuery(query: String) {
        currentSearchQuery = query.lowercase()
        applyFiltersAndSearch()
    }

    fun toggleGroupExpanded(eventId: String) {
        allBookings = allBookings.map { group ->
            if (group.event.eventId == eventId) {
                group.copy(isExpanded = !group.isExpanded)
            } else {
                group
            }
        }
        applyFiltersAndSearch()
    }

    private fun applyFiltersAndSearch() {
        var filtered = allBookings

        // Apply filter
        if (currentFilter != FilterType.ALL) {
            filtered = filtered.map { group ->
                val filteredTickets = when (currentFilter) {
                    FilterType.PAID -> group.tickets.filter {
                        it.ticketType == TicketType.PAID && it.status != TicketStatus.CANCELLED
                    }
                    FilterType.FREE -> group.tickets.filter {
                        (it.ticketType == TicketType.FREE_PRIVATE || 
                         it.ticketType == TicketType.PUBLIC_RESERVATION) &&
                        it.status != TicketStatus.CANCELLED
                    }
                    FilterType.PENDING -> group.tickets.filter {
                        it.status == TicketStatus.RESERVED
                    }
                    FilterType.CHECKED_IN -> group.tickets.filter {
                        it.status == TicketStatus.CHECKED_IN
                    }
                    FilterType.CANCELLED -> group.tickets.filter {
                        it.status == TicketStatus.CANCELLED
                    }
                    else -> group.tickets
                }
                group.copy(tickets = filteredTickets)
            }.filter { it.tickets.isNotEmpty() }
        }

        // Apply search
        if (currentSearchQuery.isNotEmpty()) {
            filtered = filtered.map { group ->
                val searchedTickets = group.tickets.filter { ticket ->
                    ticket.userName.lowercase().contains(currentSearchQuery) ||
                    ticket.userEmail.lowercase().contains(currentSearchQuery) ||
                    ticket.ticketId.lowercase().contains(currentSearchQuery)
                }
                group.copy(tickets = searchedTickets)
            }.filter { it.tickets.isNotEmpty() }
        }

        _uiState.update {
            it.copy(
                bookingGroups = filtered,
                isLoading = false,
                isEmpty = filtered.isEmpty()
            )
        }
    }

    fun refreshBookings() {
        loadBookings()
    }
}
```

---

### 2.3 UI Layer

**New File:** `app/src/main/java/com/eventfinder/app/client/organizer/OrganizerBookingsFragment.kt`

```kotlin
@AndroidEntryPoint
class OrganizerBookingsFragment : Fragment() {

    private var _binding: FragmentOrganizerBookingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OrganizerBookingsViewModel by viewModels()
    private lateinit var adapter: BookingGroupAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrganizerBookingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSearch()
        setupFilters()
        setupSwipeRefresh()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = BookingGroupAdapter(
            onEventClick = ::onEventClick,
            onToggleExpand = ::onToggleExpand
        )
        binding.rvBookings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@OrganizerBookingsFragment.adapter
        }
    }

    private fun setupSearch() {
        binding.searchInput.addTextChangedListener { text ->
            viewModel.setSearchQuery(text?.toString() ?: "")
        }
    }

    private fun setupFilters() {
        binding.chipAll.setOnClickListener { viewModel.setFilter(FilterType.ALL) }
        binding.chipPaid.setOnClickListener { viewModel.setFilter(FilterType.PAID) }
        binding.chipFree.setOnClickListener { viewModel.setFilter(FilterType.FREE) }
        binding.chipPending.setOnClickListener { viewModel.setFilter(FilterType.PENDING) }
        binding.chipCheckedIn.setOnClickListener { viewModel.setFilter(FilterType.CHECKED_IN) }
        binding.chipCancelled.setOnClickListener { viewModel.setFilter(FilterType.CANCELLED) }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshBookings()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.swipeRefresh.isRefreshing = state.isLoading
                    binding.rvBookings.isVisible = !state.isEmpty
                    binding.layoutEmpty.isVisible = state.isEmpty && !state.isLoading

                    adapter.submitList(state.bookingGroups)

                    if (state.error != null) {
                        Toast.makeText(context, state.error, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun onEventClick(event: Event) {
        val bundle = Bundle().apply {
            putString("EVENT_ID", event.eventId)
        }
        findNavController().navigate(R.id.manageEventFragment, bundle)
    }

    private fun onToggleExpand(eventId: String) {
        viewModel.toggleGroupExpanded(eventId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

**New File:** `app/src/main/java/com/eventfinder/app/client/organizer/BookingGroupAdapter.kt`

```kotlin
class BookingGroupAdapter(
    private val onEventClick: (Event) -> Unit,
    private val onToggleExpand: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var bookingGroups: List<BookingGroup> = emptyList()

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_TICKET = 1
    }

    fun submitList(groups: List<BookingGroup>) {
        bookingGroups = groups
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return bookingGroups.sumOf { group ->
            1 + if (group.isExpanded) group.tickets.size else 0
        }
    }

    override fun getItemViewType(position: Int): Int {
        var currentPos = 0
        for (group in bookingGroups) {
            if (currentPos == position) {
                return VIEW_TYPE_HEADER
            }
            currentPos++
            
            if (group.isExpanded) {
                if (position < currentPos + group.tickets.size) {
                    return VIEW_TYPE_TICKET
                }
                currentPos += group.tickets.size
            }
        }
        return VIEW_TYPE_HEADER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val binding = ItemBookingGroupHeaderBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            HeaderViewHolder(binding)
        } else {
            val binding = ItemAttendeeBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            TicketViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                val group = getGroupAtPosition(position)
                if (group != null) {
                    holder.bind(group, onEventClick, onToggleExpand)
                }
            }
            is TicketViewHolder -> {
                val ticket = getTicketAtPosition(position)
                if (ticket != null) {
                    holder.bind(ticket)
                }
            }
        }
    }

    private fun getGroupAtPosition(position: Int): BookingGroup? {
        var currentPos = 0
        for (group in bookingGroups) {
            if (currentPos == position) {
                return group
            }
            currentPos += 1 + if (group.isExpanded) group.tickets.size else 0
        }
        return null
    }

    private fun getTicketAtPosition(position: Int): Ticket? {
        var currentPos = 0
        for (group in bookingGroups) {
            currentPos++ // Skip header
            if (group.isExpanded) {
                val ticketIndex = position - currentPos
                if (ticketIndex >= 0 && ticketIndex < group.tickets.size) {
                    return group.tickets[ticketIndex]
                }
                currentPos += group.tickets.size
            }
        }
        return null
    }

    class HeaderViewHolder(
        private val binding: ItemBookingGroupHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            group: BookingGroup,
            onEventClick: (Event) -> Unit,
            onToggleExpand: (String) -> Unit
        ) {
            binding.tvEventTitle.text = group.event.title
            binding.tvEventDate.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                .format(Date(group.event.startTime))
            
            binding.tvTotalCount.text = "${group.totalCount} bookings"
            binding.tvCheckedIn.text = "${group.checkInCount} checked in"
            
            binding.ivExpandIcon.rotation = if (group.isExpanded) 180f else 0f

            binding.root.setOnClickListener {
                onToggleExpand(group.event.eventId)
            }

            binding.btnViewEvent.setOnClickListener {
                onEventClick(group.event)
            }
        }
    }

    class TicketViewHolder(
        private val binding: ItemAttendeeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(ticket: Ticket) {
            // Reuse existing attendee item layout logic
            binding.tvName.text = ticket.userName
            binding.tvInitials.text = getInitials(ticket.userName)
            
            val ticketTypeText = when (ticket.ticketType) {
                TicketType.PUBLIC_RESERVATION -> "Public Event"
                TicketType.FREE_PRIVATE -> "Free Ticket"
                TicketType.PAID -> "Paid Ticket"
            }
            binding.tvTicketInfo.text = ticketTypeText
            
            binding.tvBookingId.text = "ID: ${ticket.ticketId.take(8).uppercase()}"
            
            val statusText = when (ticket.status) {
                TicketStatus.RESERVED -> "Reserved"
                TicketStatus.PURCHASED -> "Purchased"
                TicketStatus.CHECKED_IN -> "Checked In"
                TicketStatus.CANCELLED -> "Cancelled"
                TicketStatus.EXPIRED -> "Expired"
            }
            binding.tvStatus.text = statusText
        }

        private fun getInitials(name: String): String {
            val parts = name.trim().split(" ")
            return when {
                parts.size >= 2 -> "${parts[0].firstOrNull() ?: ""}${parts[1].firstOrNull() ?: ""}".uppercase()
                parts.size == 1 -> parts[0].take(2).uppercase()
                else -> "NA"
            }
        }
    }
}
```

---

### 2.4 Layout Files

**New File:** `app/src/main/res/layout/fragment_organizer_bookings.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.swiperefreshlayout.widget.SwipeRefreshLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/swipeRefresh"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:background="?attr/colorSurface">

        <!-- Search Bar -->
        <com.google.android.material.textfield.TextInputLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_margin="@dimen/spacing_large"
            app:boxCornerRadiusTopStart="@dimen/corner_radius_xlarge"
            app:boxCornerRadiusTopEnd="@dimen/corner_radius_xlarge"
            app:boxCornerRadiusBottomStart="@dimen/corner_radius_xlarge"
            app:boxCornerRadiusBottomEnd="@dimen/corner_radius_xlarge"
            app:boxStrokeWidth="0dp"
            app:boxBackgroundColor="?attr/colorSurfaceVariant"
            app:startIconDrawable="@drawable/ic_search"
            app:hintEnabled="false">

            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/searchInput"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:hint="Search by name, email, or ticket ID"
                android:textAppearance="@style/TextAppearance.App.BodyMedium"/>
        </com.google.android.material.textfield.TextInputLayout>

        <!-- Filter Chips -->
        <HorizontalScrollView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:scrollbars="none"
            android:paddingHorizontal="@dimen/spacing_large"
            android:clipToPadding="false">
            
            <LinearLayout
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:orientation="horizontal">

                <com.google.android.material.chip.Chip
                    android:id="@+id/chipAll"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="All"
                    android:checked="true"
                    style="@style/Widget.MaterialComponents.Chip.Choice"
                    android:layout_marginEnd="@dimen/spacing_small"/>

                <com.google.android.material.chip.Chip
                    android:id="@+id/chipPaid"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Paid"
                    style="@style/Widget.MaterialComponents.Chip.Choice"
                    android:layout_marginEnd="@dimen/spacing_small"/>

                <com.google.android.material.chip.Chip
                    android:id="@+id/chipFree"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Free"
                    style="@style/Widget.MaterialComponents.Chip.Choice"
                    android:layout_marginEnd="@dimen/spacing_small"/>

                <com.google.android.material.chip.Chip
                    android:id="@+id/chipPending"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Pending"
                    style="@style/Widget.MaterialComponents.Chip.Choice"
                    android:layout_marginEnd="@dimen/spacing_small"/>

                <com.google.android.material.chip.Chip
                    android:id="@+id/chipCheckedIn"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Checked In"
                    style="@style/Widget.MaterialComponents.Chip.Choice"
                    android:layout_marginEnd="@dimen/spacing_small"/>

                <com.google.android.material.chip.Chip
                    android:id="@+id/chipCancelled"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Cancelled"
                    style="@style/Widget.MaterialComponents.Chip.Choice"/>
            </LinearLayout>
        </HorizontalScrollView>

        <!-- Bookings List -->
        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/rvBookings"
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1"
            android:layout_marginTop="@dimen/spacing_medium"
            android:clipToPadding="false"
            android:paddingBottom="@dimen/spacing_large"/>

        <!-- Empty State -->
        <include
            android:id="@+id/layoutEmpty"
            layout="@layout/view_empty_state"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:visibility="gone"/>

    </LinearLayout>

</androidx.swiperefreshlayout.widget.SwipeRefreshLayout>
```

**New File:** `app/src/main/res/layout/item_booking_group_header.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginHorizontal="@dimen/spacing_large"
    android:layout_marginVertical="@dimen/spacing_small"
    app:cardElevation="@dimen/elevation_small">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="@dimen/spacing_medium_large">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal">

            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:orientation="vertical">

                <TextView
                    android:id="@+id/tvEventTitle"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="Event Title"
                    android:textAppearance="@style/TextAppearance.App.TitleMedium"
                    android:maxLines="2"
                    android:ellipsize="end"/>

                <TextView
                    android:id="@+id/tvEventDate"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="@dimen/spacing_small"
                    android:text="Mar 15, 2024"
                    android:textAppearance="@style/TextAppearance.App.BodySmall"
                    android:textColor="?attr/colorOnSurfaceVariant"/>
            </LinearLayout>

            <ImageView
                android:id="@+id/ivExpandIcon"
                android:layout_width="24dp"
                android:layout_height="24dp"
                android:layout_gravity="center_vertical"
                android:src="@drawable/ic_expand_more"
                app:tint="?attr/colorOnSurfaceVariant"/>
        </LinearLayout>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="@dimen/spacing_medium"
            android:orientation="horizontal">

            <TextView
                android:id="@+id/tvTotalCount"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="25 bookings"
                android:textAppearance="@style/TextAppearance.App.LabelMedium"
                android:textColor="@color/md_primary"/>

            <TextView
                android:id="@+id/tvCheckedIn"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="12 checked in"
                android:textAppearance="@style/TextAppearance.App.LabelMedium"
                android:textColor="?attr/colorOnSurfaceVariant"/>

            <com.google.android.material.button.MaterialButton
                android:id="@+id/btnViewEvent"
                style="@style/Widget.MaterialComponents.Button.TextButton"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:minHeight="0dp"
                android:paddingVertical="@dimen/spacing_small"
                android:text="View"
                android:textAppearance="@style/TextAppearance.App.LabelSmall"/>
        </LinearLayout>

    </LinearLayout>

</com.google.android.material.card.MaterialCardView>
```

**New Drawable:** `app/src/main/res/drawable/ic_expand_more.xml`

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M16.59,8.59L12,13.17 7.41,8.59 6,10l6,6 6,-6z"/>
</vector>
```

---

## Phase 3: Save Draft Feature

### Goal
Allow organizers to save incomplete event creation forms as drafts for later completion.

### Timeline: 2-3 hours

---

### 3.1 Data Layer - Draft Storage

**New File:** `app/src/main/java/com/eventfinder/app/domain/model/EventDraft.kt`

```kotlin
data class EventDraft(
    val draftId: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String? = null,
    val selectedCategories: List<EventCategory> = emptyList(),
    val startTimeMillis: Long? = null,
    val endTimeMillis: Long? = null,
    val locationName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val maxParticipants: Int? = null,
    val isFree: Boolean = true,
    val price: Double? = null,
    val currency: String? = null,
    val tags: List<String> = emptyList(),
    val visibility: EventVisibility = EventVisibility.PUBLIC,
    val requiresTicket: Boolean = false,
    val organizerId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

**New File:** `app/src/main/java/com/eventfinder/app/data/local/DraftPreferences.kt`

```kotlin
@Singleton
class DraftPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("event_drafts", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveDraft(draft: EventDraft) {
        val json = gson.toJson(draft)
        prefs.edit().putString(draft.draftId, json).apply()
        
        // Also save to list of draft IDs
        val draftIds = getDraftIds().toMutableSet()
        draftIds.add(draft.draftId)
        prefs.edit().putStringSet("draft_ids", draftIds).apply()
    }

    fun getDraft(draftId: String): EventDraft? {
        val json = prefs.getString(draftId, null) ?: return null
        return try {
            gson.fromJson(json, EventDraft::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun getAllDrafts(): List<EventDraft> {
        return getDraftIds().mapNotNull { getDraft(it) }
            .sortedByDescending { it.updatedAt }
    }

    fun deleteDraft(draftId: String) {
        prefs.edit().remove(draftId).apply()
        
        val draftIds = getDraftIds().toMutableSet()
        draftIds.remove(draftId)
        prefs.edit().putStringSet("draft_ids", draftIds).apply()
    }

    private fun getDraftIds(): Set<String> {
        return prefs.getStringSet("draft_ids", emptySet()) ?: emptySet()
    }
}
```

---

### 3.2 Update ViewModel

**Update:** `app/src/main/java/com/eventfinder/app/client/createevent/CreateEventViewModel.kt`

```kotlin
@HiltViewModel
class CreateEventViewModel @Inject constructor(
    private val createEventUseCase: CreateEventUseCase,
    private val getEventCategoriesUseCase: GetEventCategoriesUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val draftPreferences: DraftPreferences  // ⭐ NEW DEPENDENCY
) : ViewModel() {

    // ... existing code ...

    /**
     * Save event as draft to local storage
     */
    fun saveDraft(
        title: String,
        description: String?,
        selectedCategories: List<EventCategory>,
        startTimeMillis: Long?,
        endTimeMillis: Long?,
        locationName: String?,
        latitude: Double?,
        longitude: Double?,
        address: String?,
        maxParticipants: Int?,
        isFree: Boolean,
        price: Double?,
        currency: String?,
        organizerId: String,
        tags: List<String>,
        visibility: EventVisibility,
        requiresTicket: Boolean
    ) {
        viewModelScope.launch {
            _draftState.value = DraftState.Saving

            try {
                val draft = EventDraft(
                    title = title.trim(),
                    description = description?.trim(),
                    selectedCategories = selectedCategories,
                    startTimeMillis = startTimeMillis,
                    endTimeMillis = endTimeMillis,
                    locationName = locationName,
                    latitude = latitude,
                    longitude = longitude,
                    address = address,
                    maxParticipants = maxParticipants,
                    isFree = isFree,
                    price = price,
                    currency = currency,
                    tags = tags,
                    visibility = visibility,
                    requiresTicket = requiresTicket,
                    organizerId = organizerId
                )

                draftPreferences.saveDraft(draft)
                
                _draftState.value = DraftState.Saved
            } catch (e: Exception) {
                _draftState.value = DraftState.Error(
                    e.message ?: "Failed to save draft"
                )
            }
        }
    }

    fun loadDraft(draftId: String): EventDraft? {
        return draftPreferences.getDraft(draftId)
    }

    fun getAllDrafts(): List<EventDraft> {
        return draftPreferences.getAllDrafts()
    }

    fun deleteDraft(draftId: String) {
        draftPreferences.deleteDraft(draftId)
    }
}
```

---

### 3.3 Update Create Event Fragment

**Update:** `app/src/main/java/com/eventfinder/app/client/createevent/CreateEventFragment.kt`

Update the `btnSaveDraft` click listener:

```kotlin
// Save Draft - UPDATED
binding.btnSaveDraft.setOnClickListener {
    if (validateBasicInfo()) {
        val title = binding.etEventTitle.text?.toString()?.trim() ?: ""
        val description = binding.etDescription.text?.toString()?.trim()
        val location = binding.etLocation.text?.toString()?.trim()
        
        // Combine date and time if available
        var startTimeMillis: Long? = null
        if (selectedDate != null && selectedStartTime != null) {
            val startDateTime = Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedDate!!.get(Calendar.YEAR))
                set(Calendar.MONTH, selectedDate!!.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, selectedDate!!.get(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, selectedStartTime!!.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, selectedStartTime!!.get(Calendar.MINUTE))
            }
            startTimeMillis = startDateTime.timeInMillis
        }

        var endTimeMillis: Long? = null
        if (selectedDate != null && selectedEndTime != null) {
            val endDateTime = Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedDate!!.get(Calendar.YEAR))
                set(Calendar.MONTH, selectedDate!!.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, selectedDate!!.get(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, selectedEndTime!!.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, selectedEndTime!!.get(Calendar.MINUTE))
            }
            endTimeMillis = endDateTime.timeInMillis
        }

        val isFree = binding.chipFree.isChecked
        val price = if (!isFree) {
            binding.etPrice.text?.toString()?.toDoubleOrNull()
        } else null
        
        val currency = if (!isFree) {
            binding.etCurrency.text?.toString()?.trim() ?: "PKR"
        } else null

        val maxParticipants = binding.etMaxParticipants.text?.toString()?.toIntOrNull()

        viewModel.saveDraft(
            title = title,
            description = description,
            selectedCategories = selectedCategories.toList(),
            startTimeMillis = startTimeMillis,
            endTimeMillis = endTimeMillis,
            locationName = location,
            latitude = selectedLatitude,
            longitude = selectedLongitude,
            address = selectedLocationAddress,
            maxParticipants = maxParticipants,
            isFree = isFree,
            price = price,
            currency = currency,
            organizerId = userPreferences.getUserId() ?: "",
            tags = emptyList(),
            visibility = EventVisibility.PUBLIC,
            requiresTicket = false
        )
    }
}
```

---

### 3.4 Drafts List UI (Optional Enhancement)

**New Fragment:** `DraftListFragment.kt` - Shows list of saved drafts

Users can:
- View all saved drafts
- Resume editing a draft
- Delete a draft
- Create event from draft

This could be accessible from:
- Create Event screen (button to "Load Draft")
- Organizer Dashboard (section showing drafts)

---

## Implementation Timeline Summary

| Phase | Feature | Time Estimate | Priority |
|-------|---------|---------------|----------|
| **Phase 1** | Events Tab | 2-3 hours | High |
| **Phase 2** | Bookings Tab | 4-5 hours | High |
| **Phase 3** | Save Draft | 2-3 hours | Medium |
| **Total** | All Features | **8-11 hours** | - |

---

## Files to Create/Modify

### Phase 1 - Events Tab (7 files)
1. ✅ `OrganizerEventsViewModel.kt` - NEW
2. ✅ `OrganizerEventsFragment.kt` - NEW
3. ✅ `fragment_organizer_events.xml` - NEW
4. ✅ `view_empty_state_simple.xml` - NEW
5. ✅ `ic_event_list.xml` - NEW
6. ✅ `ic_history.xml` - NEW
7. ✅ `mobile_navigation.xml` - UPDATE (add route)

### Phase 2 - Bookings Tab (10 files)
1. ✅ `Ticket.kt` - UPDATE (add organizerId field)
2. ✅ `FirestoreTicketDataSource.kt` - UPDATE (add query method)
3. ✅ `TicketRepository.kt` - UPDATE (add interface method)
4. ✅ `OrganizerBookingsViewModel.kt` - NEW
5. ✅ `OrganizerBookingsFragment.kt` - NEW
6. ✅ `BookingGroupAdapter.kt` - NEW
7. ✅ `fragment_organizer_bookings.xml` - NEW
8. ✅ `item_booking_group_header.xml` - NEW
9. ✅ `ic_expand_more.xml` - NEW
10. ✅ `mobile_navigation.xml` - UPDATE (add route)

### Phase 3 - Save Draft (5 files)
1. ✅ `EventDraft.kt` - NEW
2. ✅ `DraftPreferences.kt` - NEW
3. ✅ `CreateEventViewModel.kt` - UPDATE (add draft methods)
4. ✅ `CreateEventFragment.kt` - UPDATE (save all form data)
5. ⭐ `DraftListFragment.kt` - NEW (Optional)

**Total**: ~22 files

---

## Testing Checklist

### Events Tab
- [ ] Shows "Happening Now" events correctly
- [ ] Shows "Upcoming" events sorted by date
- [ ] Shows "Past Events" sorted by most recent
- [ ] Empty states display for each section
- [ ] Pull to refresh works
- [ ] Tap event navigates to ManageEventFragment
- [ ] Event counts display correctly

### Bookings Tab
- [ ] All tickets grouped by event correctly
- [ ] Expand/collapse works for each group
- [ ] Search filters across all tickets
- [ ] Filter chips work (All, Paid, Free, Pending, Checked In, Cancelled)
- [ ] Displays correct counts per event
- [ ] "View Event" navigates to ManageEventFragment
- [ ] Empty state shows when no bookings
- [ ] Pull to refresh works

### Save Draft
- [ ] Draft saves with only title (minimum requirement)
- [ ] Draft saves all filled fields
- [ ] Draft persists after app restart
- [ ] Success message displays
- [ ] Navigates back after save
- [ ] Button disabled during save operation
- [ ] (Optional) Draft list shows all saved drafts
- [ ] (Optional) Can resume editing draft
- [ ] (Optional) Can delete draft

---

## Implementation Order

### Step 1: Phase 1 - Events Tab ⏳
1. Create ViewModel
2. Create Fragment
3. Create layouts
4. Add drawable resources
5. Update navigation
6. Test with real data

### Step 2: Phase 2 - Bookings Tab ⏳
1. Update Ticket model (add organizerId)
2. Update data layer (repository, data source)
3. Create ViewModel with grouping logic
4. Create Fragment
5. Create BookingGroupAdapter
6. Create layouts
7. Test with real data

### Step 3: Phase 3 - Save Draft ⏳
1. Create EventDraft model
2. Create DraftPreferences for local storage
3. Update CreateEventViewModel
4. Update CreateEventFragment save logic
5. (Optional) Create DraftListFragment
6. Test save/load flow

---

## Next Steps

Ready to begin implementation! Recommend starting with **Phase 1 (Events Tab)** as it:
- Has no dependencies on other phases
- Reuses existing EventAdapter
- Provides immediate value to organizers
- Is relatively straightforward

Once Phase 1 is complete and tested, move to Phase 2, then Phase 3.

Would you like me to start implementing Phase 1 now?
