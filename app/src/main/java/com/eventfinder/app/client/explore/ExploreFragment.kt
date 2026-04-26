package com.eventfinder.app.client.explore

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.eventfinder.app.databinding.FragmentExploreBinding
import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.model.EventCategory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Fragment for exploring and discovering events
 */
@AndroidEntryPoint
class ExploreFragment : Fragment() {

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExploreViewModel by viewModels()
    private lateinit var exploreAdapter: ExploreUpcomingAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler()
        setupSearchBar()
        setupCategoryChips()
        setupSwipeRefresh()
        observeViewModel()
    }

    private fun setupRecycler() {
        exploreAdapter = ExploreUpcomingAdapter(
            emptyList(),
            onItemClick = { selectedEvent ->
                // TODO: Navigate to Event Detail Screen
                // findNavController().navigate(
                //     ExploreFragmentDirections.actionExploreToEventDetail(selectedEvent.id)
                // )
            }
        )

        binding.recyclerExploreEvents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = exploreAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupSearchBar() {
        binding.etSearchEvents.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                if (query.isNotEmpty()) {
                    viewModel.searchEvents(query)
                } else {
                    viewModel.loadEvents()
                }
            }
        })
    }

    private fun setupCategoryChips() {
        binding.chipMusic.setOnClickListener {
            // TODO: Implement category filtering
        }

        binding.chipEducation.setOnClickListener {
            // TODO: Implement category filtering
        }

        binding.chipSports.setOnClickListener {
            // TODO: Implement category filtering
        }

        binding.chipBusiness.setOnClickListener {
            // TODO: Implement category filtering
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    handleUiState(state)
                }
            }
        }
    }

    private fun handleUiState(state: ExploreUiState) {
        // Stop refresh animation
        binding.swipeRefresh.isRefreshing = false

        when (state) {
            is ExploreUiState.Loading -> {
                showLoading(true)
                showError(false)
                showEmpty(false)
                showContent(false)
            }
            is ExploreUiState.Success -> {
                showLoading(false)
                showError(false)
                showEmpty(false)
                showContent(true)
                updateEventsList(state.events)
            }
            is ExploreUiState.Error -> {
                showLoading(false)
                showError(true, state.message)
                showEmpty(false)
                showContent(false)
            }
            is ExploreUiState.Empty -> {
                showLoading(false)
                showError(false)
                showEmpty(true)
                showContent(false)
            }
        }
    }

    private fun updateEventsList(events: List<Event>) {
        exploreAdapter.updateEvents(events)
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.isVisible = show
    }

    private fun showError(show: Boolean, message: String? = null) {
        binding.errorView.isVisible = show
        if (show && message != null) {
            binding.errorMessage.text = message
        }

        binding.btnRetry.setOnClickListener {
            viewModel.loadEvents()
        }
    }

    private fun showEmpty(show: Boolean) {
        binding.emptyView.isVisible = show
    }

    private fun showContent(show: Boolean) {
        binding.swipeRefresh.isVisible = show
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
