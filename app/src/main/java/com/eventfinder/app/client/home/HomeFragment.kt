package com.eventfinder.app.client.home

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.eventfinder.app.R
import com.eventfinder.app.databinding.FragmentHomeBinding
import androidx.navigation.fragment.findNavController
import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.utils.UserPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    @Inject
    lateinit var userPreferences: UserPreferences

    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var yourEventsAdapter: HomeEventAdapter
    private lateinit var featuredAdapter: HomeEventAdapter

    private var currentWeekStart = Calendar.getInstance()
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        // Read cached user type to apply UI immediately and prevent flickering
        val cachedIsOrganizer = userPreferences.getUserType() == com.eventfinder.app.domain.model.UserType.ORGANIZER.name
        updateUIForUserType(cachedIsOrganizer)

        setupUI()
        observeViewModel()
        setupScrollBehavior()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // 1. Update User Details
                    state.user?.let { user ->
                        val name = user.profile?.fullName ?: user.organizerProfile?.organizationName
                        name?.let {
                            userPreferences.setUserName(it)
                            binding.tvUserName.text = it
                        }
                        userPreferences.setUserType(user.userType.name)
                        updateUIForUserType(state.isOrganizer)
                    }

                    // 2. Update Events Lists
                    yourEventsAdapter.submitList(state.userEvents)
                    updateYourEventsVisibility(state.userEvents, state.isOrganizer)
                    
                    featuredAdapter.submitList(state.featuredEvents)

                    // 3. Handle Errors
                    state.error?.let {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    private fun updateUIForUserType(isOrganizer: Boolean) {
        if (isOrganizer) {
            binding.layoutCalendarHeader.isVisible = true
            binding.rvCalendar.isVisible = true

            binding.layoutYourEventsHeader.isVisible = true
            binding.tvYourEvents.text = "Your Events"

            binding.tvEmptyStateTitle.text = "No Events Yet"
            binding.tvEmptyStateDesc.text = "Create your first event and start managing attendees"
            binding.btnCreateEvent.text = "Create Event"
            binding.btnCreateEvent.setIconResource(R.drawable.ic_add)

            binding.btnCreateEvent.setOnClickListener {
                findNavController().navigate(R.id.createEventFragment)
            }
            binding.btnCreateEventIcon.setOnClickListener {
                findNavController().navigate(R.id.createEventFragment)
            }
        } else {
            binding.layoutCalendarHeader.isVisible = false
            binding.rvCalendar.isVisible = false

            binding.layoutYourEventsHeader.isVisible = true
            binding.tvYourEvents.text = "Upcoming Events"

            binding.tvEmptyStateTitle.text = "No upcoming events"
            binding.tvEmptyStateDesc.text = "Explore events to find something interesting to attend"
            binding.btnCreateEvent.text = "Explore Events"
            binding.btnCreateEvent.setIconResource(R.drawable.ic_explore)

            binding.btnCreateEvent.setOnClickListener {
                findNavController().navigate(R.id.exploreFragment)
            }
            binding.btnCreateEventIcon.isVisible = false
        }
    }

    private fun setupUI() {
        binding.tvUserName.text = userPreferences.getUserName()

        setupCalendar()

        yourEventsAdapter = HomeEventAdapter { event -> navigateToEventDetail(event) }
        binding.rvYourEvents.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = yourEventsAdapter
            setHasFixedSize(false)
        }

        featuredAdapter = HomeEventAdapter { event -> navigateToEventDetail(event) }
        binding.rvFeatured.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = featuredAdapter
            setHasFixedSize(false)
        }

        binding.tvSeeAllEvents.setOnClickListener {
            // TODO: Navigate to user's events list
        }

        binding.btnChat.setOnClickListener {
            findNavController().navigate(R.id.chatbotFragment)
        }
    }

    private fun setupCalendar() {
        currentWeekStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }

        updateMonthYearDisplay()

        calendarAdapter = CalendarAdapter(generateWeek(currentWeekStart)) { day ->
            Toast.makeText(context, "Selected: ${day.dayName} ${day.dayNumber}", Toast.LENGTH_SHORT).show()
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

    private fun navigateToEventDetail(event: Event) {
        val bundle = Bundle().apply {
            putString("EVENT_ID", event.id)
            putString("EVENT_TITLE", event.title)
        }
        findNavController().navigate(R.id.eventDetailFragment, bundle)
    }

    private fun setupScrollBehavior() {
        binding.homeScrollView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (scrollY > oldScrollY + 10) binding.btnChat.shrink()
            else if (scrollY < oldScrollY - 10) binding.btnChat.extend()
        }
    }

    private fun updateYourEventsVisibility(events: List<Event>, isOrganizer: Boolean) {
        if (events.isEmpty()) {
            binding.layoutEmptyEvents.isVisible = true
            binding.rvYourEvents.isVisible = false
            binding.tvSeeAllEvents.isVisible = false
            binding.btnCreateEventIcon.isVisible = false
        } else {
            binding.layoutEmptyEvents.isVisible = false
            binding.rvYourEvents.isVisible = true
            binding.tvSeeAllEvents.isVisible = events.size > 2
            if (isOrganizer) binding.btnCreateEventIcon.isVisible = true
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}