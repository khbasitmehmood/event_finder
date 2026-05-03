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
import com.eventfinder.app.domain.model.EventCategory
import com.eventfinder.app.utils.UserPreferences
import com.google.android.material.chip.Chip
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

    private lateinit var yourEventsAdapter: UpcomingEventAdapter
    private lateinit var featuredAdapter: HomeEventAdapter
    private lateinit var eventsNearYouAdapter: SmallEventAdapter
    private lateinit var dateFilteredAdapter: HomeEventAdapter
    private lateinit var calendarAdapter: CalendarAdapter

    private var currentWeekStart = Calendar.getInstance()
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MMMM d", Locale.getDefault())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        setupUI()
        setupCalendar()
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
                    }

                    // 2. Update Category Chips
                    updateCategoryChips(state.userCategories)

                    // 3. Update Date Filtered Events
                    updateDateFilteredEvents(state.dateFilteredEvents, state.selectedDate)

                    // 4. Update Events Lists
                    yourEventsAdapter.submitList(state.userEvents)
                    updateYourEventsVisibility(state.userEvents)

                    featuredAdapter.submitList(state.featuredEvents)
                    eventsNearYouAdapter.submitList(state.featuredEvents)

                    // Update Loading states
                    val isAnyLoading = state.isLoadingFeatured || state.isLoadingUserEvents
                    binding.swipeRefreshLayout.isRefreshing = isAnyLoading

                    // Show placeholders if empty and loading
                    binding.progressBarFeatured.isVisible = state.isLoadingFeatured && state.featuredEvents.isEmpty()
                    binding.progressBarNearYou.isVisible = state.isLoadingFeatured && state.featuredEvents.isEmpty()
                    binding.progressBarYourEvents.isVisible = state.isLoadingUserEvents && state.userEvents.isEmpty()

                    // 5. Handle Errors
                    state.error?.let {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }


    private fun setupUI() {
        // Extract first name from full name
        val fullName = userPreferences.getUserName()
        val firstName = fullName.split(" ").firstOrNull() ?: fullName
        binding.tvUserName.text = firstName

        // Setup SwipeRefreshLayout
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadData()
        }

        // Setup featured events adapter (horizontal, big cards)
        featuredAdapter = HomeEventAdapter(isHorizontal = true) { event -> navigateToEventDetail(event) }
        binding.rvFeatured.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = featuredAdapter
            setHasFixedSize(true)
        }

        // Setup date filtered events adapter (horizontal, big cards)
        dateFilteredAdapter = HomeEventAdapter(isHorizontal = true) { event -> navigateToEventDetail(event) }
        binding.rvDateFilteredEvents.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = dateFilteredAdapter
            setHasFixedSize(true)
        }

        // Setup events near you adapter (horizontal, small cards)
        eventsNearYouAdapter = SmallEventAdapter { event -> navigateToEventDetail(event) }
        binding.rvEventsNearYou.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = eventsNearYouAdapter
            setHasFixedSize(false)
        }

        // Setup your events adapter (horizontal, using Upcoming items)
        yourEventsAdapter = UpcomingEventAdapter { event -> navigateToEventDetail(event) }
        binding.rvYourEvents.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = yourEventsAdapter
            setHasFixedSize(false)
        }

        // Button handlers
        binding.btnExploreEvents.setOnClickListener {
            findNavController().navigate(R.id.exploreFragment)
        }

        binding.tvSeeAllEvents.setOnClickListener {
            // TODO: Navigate to user's events list
        }

        binding.btnChat.setOnClickListener {
            findNavController().navigate(R.id.chatbotFragment)
        }

        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.notificationsFragment)
        }
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

    private fun setupCalendar() {
        currentWeekStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }

        updateMonthYearDisplay()

        calendarAdapter = CalendarAdapter(generateWeek(currentWeekStart)) { day ->
            viewModel.selectDate(day.date.time)
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
                    hasEvents = false
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

    private fun updateCategoryChips(categories: List<EventCategory>) {
        binding.chipGroupCategories.removeAllViews()

        if (categories.isEmpty()) {
            binding.layoutCategoriesSection.isVisible = false
            return
        }

        binding.layoutCategoriesSection.isVisible = true

        categories.forEach { category ->
            val chip = Chip(requireContext()).apply {
                text = category.name
                isClickable = true
                isCheckable = false
                setChipBackgroundColorResource(R.color.md_primary_container)
                setTextColor(resources.getColor(R.color.md_on_primary_container, null))
                setOnClickListener {
                    // TODO: Filter events by category
                    Toast.makeText(context, "Filter by ${category.name}", Toast.LENGTH_SHORT).show()
                }
            }
            binding.chipGroupCategories.addView(chip)
        }
    }

    private fun updateDateFilteredEvents(events: List<Event>, selectedDate: Long) {
        if (events.isEmpty()) {
            binding.layoutDateFilteredSection.isVisible = false
        } else {
            binding.layoutDateFilteredSection.isVisible = true
            binding.tvDateFilteredTitle.text = "Events on ${dateFormat.format(Date(selectedDate))}"
            binding.tvDateFilteredEmpty.isVisible = false
            binding.rvDateFilteredEvents.isVisible = true
            dateFilteredAdapter.submitList(events)
        }
    }

    private fun updateYourEventsVisibility(events: List<Event>) {
        if (events.isEmpty()) {
            binding.layoutEmptyEvents.isVisible = true
            binding.rvYourEvents.isVisible = false
        } else {
            binding.layoutEmptyEvents.isVisible = false
            binding.rvYourEvents.isVisible = true
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