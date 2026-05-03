package com.eventfinder.app.client.organizer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.eventfinder.app.R
import com.eventfinder.app.client.home.UpcomingEventAdapter
import com.eventfinder.app.databinding.FragmentOrganizerEventsBinding
import com.eventfinder.app.domain.model.Event
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OrganizerEventsFragment : Fragment() {

    private var _binding: FragmentOrganizerEventsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OrganizerEventsViewModel by viewModels()

    private lateinit var happeningNowAdapter: UpcomingEventAdapter
    private lateinit var upcomingAdapter: UpcomingEventAdapter
    private lateinit var pastAdapter: UpcomingEventAdapter

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
        setupCreateEventButton()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        // Happening Now
        happeningNowAdapter = UpcomingEventAdapter(onClick = ::onEventClick)
        binding.rvHappeningNow.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = happeningNowAdapter
            isNestedScrollingEnabled = false
        }

        // Upcoming
        upcomingAdapter = UpcomingEventAdapter(onClick = ::onEventClick)
        binding.rvUpcoming.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = upcomingAdapter
            isNestedScrollingEnabled = false
        }

        // Past
        pastAdapter = UpcomingEventAdapter(onClick = ::onEventClick)
        binding.rvPast.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pastAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshEvents()
        }
    }

    private fun setupCreateEventButton() {
        binding.btnCreateEvent.setOnClickListener {
            findNavController().navigate(R.id.createEventFragment)
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
                        binding.rvHappeningNow.isVisible = true
                        happeningNowAdapter.submitList(state.happeningNowEvents)
                    } else {
                        binding.sectionHappeningNow.isVisible = false
                    }

                    // Upcoming Section
                    if (state.upcomingEvents.isNotEmpty()) {
                        binding.sectionUpcoming.isVisible = true
                        binding.tvUpcomingCount.text = "${state.upcomingEvents.size}"
                        binding.rvUpcoming.isVisible = true
                        binding.emptyUpcoming.root.isVisible = false
                        upcomingAdapter.submitList(state.upcomingEvents)
                    } else {
                        binding.sectionUpcoming.isVisible = true
                        binding.rvUpcoming.isVisible = false
                        binding.emptyUpcoming.root.isVisible = true
                        binding.emptyUpcoming.tvEmptyMessage.text = "No upcoming events"
                    }

                    // Past Section
                    if (state.pastEvents.isNotEmpty()) {
                        binding.sectionPast.isVisible = true
                        binding.tvPastCount.text = "${state.pastEvents.size}"
                        binding.rvPast.isVisible = true
                        binding.emptyPast.root.isVisible = false
                        pastAdapter.submitList(state.pastEvents)
                    } else {
                        binding.sectionPast.isVisible = true
                        binding.rvPast.isVisible = false
                        binding.emptyPast.root.isVisible = true
                        binding.emptyPast.tvEmptyMessage.text = "No past events"
                    }

                    // Show "all empty" state if no events at all
                    val allEmpty = state.happeningNowEvents.isEmpty() &&
                            state.upcomingEvents.isEmpty() &&
                            state.pastEvents.isEmpty()
                    binding.layoutAllEmpty.isVisible = allEmpty && !state.isLoading

                    // Hide sections if all empty
                    if (allEmpty) {
                        binding.sectionHappeningNow.isVisible = false
                        binding.sectionUpcoming.isVisible = false
                        binding.sectionPast.isVisible = false
                    }

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

