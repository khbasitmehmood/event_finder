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

    private lateinit var yourEventsAdapter: UpcomingEventAdapter
    private lateinit var featuredAdapter: HomeEventAdapter
    private lateinit var eventsNearYouAdapter: UpcomingEventAdapter
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
                        val name = user.profile?.fullName
                        name?.let {
                            userPreferences.setUserName(it)
                            binding.tvUserName.text = it
                        }
                    }

                    // 2. Update Events Lists
                    yourEventsAdapter.submitList(state.userEvents)
                    updateYourEventsVisibility(state.userEvents)

                    featuredAdapter.submitList(state.featuredEvents)
                    eventsNearYouAdapter.submitList(state.featuredEvents)

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

        // Setup featured events adapter (horizontal)
        featuredAdapter = HomeEventAdapter { event -> navigateToEventDetail(event) }
        binding.rvFeatured.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = featuredAdapter
            setHasFixedSize(true)
        }

        // Setup categories adapter (grid)
        categoriesAdapter = CategoryAdapter(getCategories())
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = categoriesAdapter
            setHasFixedSize(true)
        }
        
        // Setup events near you adapter (vertical)
        eventsNearYouAdapter = UpcomingEventAdapter { event -> navigateToEventDetail(event) }
        binding.rvEventsNearYou.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = eventsNearYouAdapter
            setHasFixedSize(false)
        }

        // Setup your events adapter
        yourEventsAdapter = UpcomingEventAdapter { event -> navigateToEventDetail(event) }
        binding.rvYourEvents.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = yourEventsAdapter
            setHasFixedSize(false)
        }

        // Button handlers
        binding.btnCreateEvent.setOnClickListener {
            findNavController().navigate(R.id.exploreFragment)
        }

        binding.tvSeeAllEvents.setOnClickListener {
            // TODO: Navigate to user's events list
        }

        binding.btnChat.setOnClickListener {
            findNavController().navigate(R.id.chatbotFragment)
        }
    }

    private fun getCategories(): List<Category> {
        return listOf(
            Category("Music", R.drawable.music_note_2_24px),
            Category("Food", R.drawable.ic_event_placeholder),
            Category("Sports", R.drawable.sports_cricket_24px),
            Category("Business", R.drawable.ic_event_placeholder),
            Category("Family", R.drawable.ic_event_placeholder),
            Category("More", R.drawable.ic_next)
        )
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
            binding.tvSeeAllEvents.isVisible = false
        } else {
            binding.layoutEmptyEvents.isVisible = false
            binding.rvYourEvents.isVisible = true
            binding.tvSeeAllEvents.isVisible = events.size > 2
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