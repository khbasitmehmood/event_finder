package com.eventfinder.app.client.organizer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.eventfinder.app.R
import com.eventfinder.app.databinding.FragmentOrganizerBookingsBinding
import com.eventfinder.app.domain.model.Event
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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
        binding.chipAll.setOnClickListener {
            viewModel.setFilter(OrganizerBookingsViewModel.FilterType.ALL)
        }
        binding.chipPaid.setOnClickListener {
            viewModel.setFilter(OrganizerBookingsViewModel.FilterType.PAID)
        }
        binding.chipFree.setOnClickListener {
            viewModel.setFilter(OrganizerBookingsViewModel.FilterType.FREE)
        }
        binding.chipPending.setOnClickListener {
            viewModel.setFilter(OrganizerBookingsViewModel.FilterType.PENDING)
        }
        binding.chipCheckedIn.setOnClickListener {
            viewModel.setFilter(OrganizerBookingsViewModel.FilterType.CHECKED_IN)
        }
        binding.chipCancelled.setOnClickListener {
            viewModel.setFilter(OrganizerBookingsViewModel.FilterType.CANCELLED)
        }
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
                    binding.layoutEmpty.root.isVisible = state.isEmpty && !state.isLoading

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

