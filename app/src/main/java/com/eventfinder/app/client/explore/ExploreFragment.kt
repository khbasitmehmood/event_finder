package com.eventfinder.app.client.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.eventfinder.app.R
import com.eventfinder.app.client.home.EventItem
import com.eventfinder.app.databinding.FragmentExploreBinding
import com.eventfinder.app.domain.model.Event
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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
        observeViewModel()
    }

    private fun setupRecycler() {
        exploreAdapter = ExploreUpcomingAdapter(
            emptyList(),
            onItemClick = { selectedEvent ->
                // TODO: Open Detail Screen
            },
            activity = requireActivity() as AppCompatActivity
        )

        binding.recyclerExploreEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerExploreEvents.adapter = exploreAdapter
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
        when (state) {
            is ExploreUiState.Loading -> {
                showLoading(true)
                showError(false)
                showEmpty(false)
            }
            is ExploreUiState.Success -> {
                showLoading(false)
                showError(false)
                showEmpty(false)
                updateEventsList(state.events)
            }
            is ExploreUiState.Error -> {
                showLoading(false)
                showError(true, state.message)
                showEmpty(false)
            }
            is ExploreUiState.Empty -> {
                showLoading(false)
                showError(false)
                showEmpty(true)
            }
        }
    }

    private fun updateEventsList(events: List<Event>) {
        val eventItems = events.map { event ->
            EventItem(
                id = event.id,
                title = event.title,
                location = event.location,
                date = event.date,
                imageRes = R.drawable.ic_event_placeholder
            )
        }
        exploreAdapter.updateEvents(eventItems)
    }

    private fun showLoading(show: Boolean) {
        // Add a ProgressBar to your layout and control visibility here
        // binding.progressBar.isVisible = show
        binding.recyclerExploreEvents.isVisible = !show
    }

    private fun showError(show: Boolean, message: String? = null) {
        // Add an error view to your layout and control visibility here
        // binding.errorView.isVisible = show
        // binding.errorMessage.text = message
    }

    private fun showEmpty(show: Boolean) {
        // Add an empty state view to your layout and control visibility here
        // binding.emptyView.isVisible = show
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
