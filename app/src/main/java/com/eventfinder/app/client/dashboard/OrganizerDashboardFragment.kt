package com.eventfinder.app.client.dashboard

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eventfinder.app.R
import com.eventfinder.app.client.home.CalendarAdapter
import com.eventfinder.app.client.home.CalendarDay
import com.eventfinder.app.client.home.HomeEventAdapter
import com.eventfinder.app.databinding.FragmentOrganizerDashboardBinding
import com.eventfinder.app.domain.model.Event
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class OrganizerDashboardFragment : Fragment(R.layout.fragment_organizer_dashboard) {

    private var _binding: FragmentOrganizerDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OrganizerDashboardViewModel by viewModels()

    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var eventsAdapter: HomeEventAdapter
    private val upcomingEventsSnapHelper = LinearSnapHelper()

    private var currentWeekStart = Calendar.getInstance()
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOrganizerDashboardBinding.bind(view)

        setupUI()
        setupCalendar()
        observeViewModel()
    }

    private fun setupUI() {
        // Setup events RecyclerView
        eventsAdapter = HomeEventAdapter(isHorizontal = true) { event ->
            viewModel.selectEvent(event.dashboardEventId())
        }
        binding.rvUpcomingEvents.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = eventsAdapter
            setHasFixedSize(false)
            upcomingEventsSnapHelper.attachToRecyclerView(this)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        selectSnappedUpcomingEvent(recyclerView)
                    }
                }
            })
        }

        // Create Event buttons
        binding.btnCreateEventEmptyState.setOnClickListener {
            findNavController().navigate(R.id.createEventFragment)
        }


        // Create More Event card as button
        binding.creatMoreEventsCard.setOnClickListener {
            findNavController().navigate(R.id.createEventFragment)
        }

        binding.btnScanTicket.setOnClickListener {
            navigateToScannerForSelectedEvent()
        }


        // Chat button
        binding.btnChat.setOnClickListener {
            findNavController().navigate(R.id.chatbotFragment)
        }

        // Scroll behavior for chat FAB
        binding.dashboardScrollView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (scrollY > oldScrollY + 10) binding.btnChat.shrink()
            else if (scrollY < oldScrollY - 10) binding.btnChat.extend()
        }
    }

    private fun setupCalendar() {
        currentWeekStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }

        updateMonthYearDisplay()

        calendarAdapter = CalendarAdapter(generateWeek(currentWeekStart)) { day ->
            Toast.makeText(context, "Selected: ${day.dayName} ${day.dayNumber}", Toast.LENGTH_SHORT)
                .show()
        }

        binding.rvCalendar.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = calendarAdapter
            setHasFixedSize(true)
        }

        binding.btnPreviousWeek.setOnClickListener {
            currentWeekStart.add(Calendar.WEEK_OF_YEAR, -1)
            updateCalendar()
        }

        binding.btnNextWeek.setOnClickListener {
            currentWeekStart.add(Calendar.WEEK_OF_YEAR, 1)
            updateCalendar()
        }
    }

    private fun updateCalendar() {
        calendarAdapter.updateDays(generateWeek(currentWeekStart))
        updateMonthYearDisplay()
    }

    private fun updateMonthYearDisplay() {
        binding.tvMonthYear.text = monthYearFormat.format(currentWeekStart.time)
    }

    private fun generateWeek(weekStart: Calendar): List<CalendarDay> {
        val calendar = weekStart.clone() as Calendar
        val today = Date()
        val weekDays = mutableListOf<CalendarDay>()
        val dayNameFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dayNumberFormat = SimpleDateFormat("dd", Locale.getDefault())

        repeat(7) {
            val date = calendar.time
            val isToday = isSameDay(date, today)

            weekDays.add(
                CalendarDay(
                    date = date,
                    dayName = dayNameFormat.format(date).uppercase(),
                    dayNumber = dayNumberFormat.format(date),
                    isToday = isToday,
                    isSelected = false,
                    hasEvents = false // TODO: Check if day has events
                )
            )
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return weekDays
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Update organizer name
                    state.user?.let { user ->
                        val name = user.organizerProfile?.organizationName
                            ?: user.profile?.fullName
                            ?: "Organizer"
                        binding.tvOrganizerName.text = name
                    }

                    // Update events list
                    eventsAdapter.submitList(state.upcomingEvents)
                    updateEventsVisibility(state.upcomingEvents)
                    updateSelectedEventStats(state)

                    // Handle errors
                    state.error?.let {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    private fun updateEventsVisibility(events: List<Event>) {
        if (events.isEmpty()) {
            binding.emptyLayout.isVisible = true
            binding.eventListLayout.isVisible = false
            binding.btnScanTicket.isEnabled = false
        } else {
            binding.eventListLayout.isVisible = true
            binding.emptyLayout.isVisible = false
        }
    }

    private fun updateSelectedEventStats(state: OrganizerDashboardViewModel.OrganizerDashboardUiState) {
        binding.tvTicketsSoldValue.text = if (state.isLoadingSelectedStats) {
            "..."
        } else {
            state.selectedEventStats.ticketsSold.toString()
        }
        binding.tvAttendeeValue.text = if (state.isLoadingSelectedStats) {
            "..."
        } else {
            state.selectedEventStats.peopleAttending.toString()
        }

        val canScan = state.selectedEventId != null && state.upcomingEvents.isNotEmpty()
        binding.btnScanTicket.isEnabled = canScan
        binding.btnScanTicket.alpha = if (canScan) 1f else 0.5f
    }

    private fun selectSnappedUpcomingEvent(recyclerView: RecyclerView) {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        val snappedView = upcomingEventsSnapHelper.findSnapView(layoutManager) ?: return
        val position = layoutManager.getPosition(snappedView)
        val event = eventsAdapter.currentList.getOrNull(position) ?: return
        viewModel.selectEvent(event.dashboardEventId())
    }

    private fun navigateToScannerForSelectedEvent() {
        val state = viewModel.uiState.value
        val selectedEvent = state.upcomingEvents.firstOrNull {
            it.dashboardEventId() == state.selectedEventId
        }

        if (selectedEvent == null) {
            Toast.makeText(context, "Select an event first", Toast.LENGTH_SHORT).show()
            return
        }

        val bundle = Bundle().apply {
            putString("EVENT_ID", selectedEvent.dashboardEventId())
            putString("EVENT_TITLE", selectedEvent.title)
        }
        findNavController().navigate(R.id.qrScannerFragment, bundle)
    }

    private fun navigateToEventDetail(event: Event) {
        val bundle = Bundle().apply {
            putString("EVENT_ID", event.id)
            putString("EVENT_TITLE", event.title)
        }
        findNavController().navigate(R.id.action_organizerDashboardFragment_to_manageEventFragment, bundle)
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun Event.dashboardEventId(): String {
        return eventId.ifBlank { id }
    }
}
