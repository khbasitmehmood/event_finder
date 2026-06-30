package com.eventfinder.app.client.tickets

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.eventfinder.app.R
import com.eventfinder.app.databinding.FragmentTicketsBinding
import com.eventfinder.app.domain.model.Ticket
import com.eventfinder.app.utils.UserPreferences
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TicketsFragment : Fragment(R.layout.fragment_tickets) {

    private var _binding: FragmentTicketsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TicketsViewModel by viewModels()

    @Inject
    lateinit var userPreferences: UserPreferences

    private lateinit var pagerAdapter: TicketsPagerAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTicketsBinding.bind(view)

        setupViewPager()
        observeViewModel()

        // Load tickets
        val userId = userPreferences.getUserId()
        viewModel.loadUserTickets(userId)
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            viewModel.refreshTickets(userPreferences.getUserId())
        }
    }

    private fun setupViewPager() {
        pagerAdapter = TicketsPagerAdapter(this, emptyList(), emptyList(), emptyList())
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Upcoming"
                1 -> "Past"
                2 -> "Cancelled"
                else -> ""
            }
        }.attach()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading

                    if (state.error != null) {
                        Toast.makeText(context, state.error, Toast.LENGTH_SHORT).show()
                    }

                    // Update ViewPager with new data
                    if (!state.isLoading) {
                        updateViewPager(
                            state.upcomingTickets,
                            state.pastTickets,
                            state.cancelledTickets
                        )
                    }
                }
            }
        }
    }

    private fun updateViewPager(
        upcoming: List<Ticket>,
        past: List<Ticket>,
        cancelled: List<Ticket>
    ) {
        pagerAdapter = TicketsPagerAdapter(this, upcoming, past, cancelled)
        binding.viewPager.adapter = pagerAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * ViewPager2 Adapter for ticket tabs
     */
    inner class TicketsPagerAdapter(
        fragment: Fragment,
        private val upcomingTickets: List<Ticket>,
        private val pastTickets: List<Ticket>,
        private val cancelledTickets: List<Ticket>
    ) : FragmentStateAdapter(fragment) {

        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> TicketListFragment.newInstance(upcomingTickets)
                1 -> TicketListFragment.newInstance(pastTickets)
                2 -> TicketListFragment.newInstance(cancelledTickets)
                else -> throw IllegalArgumentException("Invalid position")
            }
        }
    }
}
