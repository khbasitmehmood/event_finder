package com.eventfinder.app.client.explore

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
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
import com.eventfinder.app.databinding.FragmentExploreBinding
import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.model.User
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Fragment for exploring and discovering events with advanced filtering
 */
@AndroidEntryPoint
class ExploreFragment : Fragment(R.layout.fragment_explore) {

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExploreViewModel by viewModels()

    private lateinit var exploreAdapter: ExploreUpcomingAdapter
    private lateinit var organizerAdapter: OrganizerAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentExploreBinding.bind(view)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // Setup events RecyclerView
        exploreAdapter = ExploreUpcomingAdapter(
            emptyList(),
            onItemClick = { event -> navigateToEventDetail(event) }
        )
        binding.recyclerExploreEvents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = exploreAdapter
            setHasFixedSize(false)
        }

        // Setup organizers RecyclerView
        organizerAdapter = OrganizerAdapter { organizer ->
            navigateToOrganizerProfile(organizer)
        }
        binding.rvOrganizers.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = organizerAdapter
            setHasFixedSize(true)
        }

        // Setup search
        setupSearchBar()

        // Setup filter button
        binding.btnFilter.setOnClickListener {
            showFilterBottomSheet()
        }

        // Setup user interests chip
        binding.chipUserInterests.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.toggleUserInterestsFilter()
            } else {
                viewModel.toggleUserInterestsFilter()
            }
        }

        // Setup swipe refresh
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    private fun setupSearchBar() {
        binding.etSearchEvents.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                viewModel.searchEvents(query)
            }
        })
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUI(state)
                }
            }
        }
    }

    private fun updateUI(state: ExploreState) {
        // Loading state
        binding.swipeRefresh.isRefreshing = state.isLoading
        binding.progressBar.isVisible = state.isLoading && state.filteredEvents.isEmpty()

        // Events
        exploreAdapter.updateEvents(state.filteredEvents)
        binding.layoutEventsSection.isVisible = state.filteredEvents.isNotEmpty()
        binding.tvEventCount.text = "${state.filteredEvents.size} events"

        // Organizers - Show all for now
        android.util.Log.d("ExploreFragment", "Organizers count: ${state.organizers.size}")
        organizerAdapter.submitList(state.organizers)
        // Always show section if we have events loaded (even if extraction failed)
        binding.layoutOrganizersSection.isVisible = state.organizers.isNotEmpty() || state.allEvents.isNotEmpty()

        // User interests chip
        binding.chipUserInterests.isVisible = state.userInterests.isNotEmpty()
        binding.chipUserInterests.isChecked = state.filters.onlyUserInterests

        // Active filters display
        updateActiveFiltersDisplay(state.filters)

        // Empty state
        binding.emptyView.isVisible =
            !state.isLoading && state.filteredEvents.isEmpty() && state.error == null

        // Error handling
        state.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    private fun updateActiveFiltersDisplay(filters: ExploreFilters) {
        binding.chipGroupActiveFilters.removeAllViews()

        val hasFilters = filters.selectedCategories.isNotEmpty() ||
                        filters.priceFilter != PriceFilter.ALL

        binding.activeFiltersContainer.isVisible = hasFilters

        if (!hasFilters) return

        // Add price filter chip
        if (filters.priceFilter != PriceFilter.ALL) {
            val chip = Chip(requireContext()).apply {
                text = when (filters.priceFilter) {
                    PriceFilter.FREE -> "Free"
                    PriceFilter.PAID -> "Paid"
                    else -> ""
                }
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    val newFilters = filters.copy(priceFilter = PriceFilter.ALL)
                    viewModel.applyFilters(newFilters)
                }
            }
            binding.chipGroupActiveFilters.addView(chip)
        }

        // Add category filter chips
        filters.selectedCategories.forEach { categoryId ->
            val state = viewModel.uiState.value
            val category = state.allCategories.find { it.id == categoryId }
            category?.let {
                val chip = Chip(requireContext()).apply {
                    text = it.name
                    isCloseIconVisible = true
                    setOnCloseIconClickListener {
                        val newCategories = filters.selectedCategories.toMutableSet()
                        newCategories.remove(categoryId)
                        val newFilters = filters.copy(selectedCategories = newCategories)
                        viewModel.applyFilters(newFilters)
                    }
                }
                binding.chipGroupActiveFilters.addView(chip)
            }
        }
    }

    private fun showFilterBottomSheet() {
        val state = viewModel.uiState.value
        FilterBottomSheet(
            allCategories = state.allCategories,
            userInterests = state.userInterests,
            currentFilters = state.filters
        ) { filters ->
            viewModel.applyFilters(filters)
        }.show(childFragmentManager, FilterBottomSheet.TAG)
    }

    private fun navigateToEventDetail(event: Event) {
        val bundle = Bundle().apply {
            putString("EVENT_ID", event.id)
            putString("EVENT_TITLE", event.title)
        }
        findNavController().navigate(R.id.eventDetailFragment, bundle)
    }

    private fun navigateToOrganizerProfile(organizer: User) {
        // TODO: Navigate to organizer profile screen
        Toast.makeText(
            context,
            "View ${organizer.organizerProfile?.organizationName}",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
