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
    private lateinit var eventsNearYouAdapter: HomeEventAdapter
    private lateinit var categoriesAdapter: CategoryAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

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
                    }

                    // 2. Update Events & Categories Lists
                    categoriesAdapter.submitList(state.userCategories)
                    
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

                    // 3. Handle Errors
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

        // Setup featured events adapter (horizontal)
        featuredAdapter = HomeEventAdapter(isHorizontal = true) { event -> navigateToEventDetail(event) }
        binding.rvFeatured.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = featuredAdapter
            setHasFixedSize(true)
        }

        // Setup categories adapter (horizontal)
        categoriesAdapter = CategoryAdapter()
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = categoriesAdapter
            setHasFixedSize(true)
        }
        
        // Setup events near you adapter (vertical)
        eventsNearYouAdapter = HomeEventAdapter(isHorizontal = false) { event -> navigateToEventDetail(event) }
        binding.rvEventsNearYou.apply {
            layoutManager = LinearLayoutManager(context)
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