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
import androidx.viewpager2.adapter.FragmentStateAdapter
import coil.load
import com.eventfinder.app.R
import com.eventfinder.app.client.home.EventDetailViewModel
import com.eventfinder.app.databinding.FragmentManageEventBinding
import com.eventfinder.app.domain.model.Event
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@AndroidEntryPoint
class ManageEventFragment : Fragment() {

    private var _binding: FragmentManageEventBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EventDetailViewModel by viewModels()
    private var currentEventTitle: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_more -> {
                    showEventActionsBottomSheet()
                    true
                }
                else -> false
            }
        }
        
        binding.toolbar.title = ""

        binding.appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            if (abs(verticalOffset) >= appBarLayout.totalScrollRange) {
                // Collapsed completely
                binding.toolbar.title = currentEventTitle
            } else {
                // Expanded
                binding.toolbar.title = ""
            }
        })

        val adapter = ManageEventPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Overview"
                1 -> "Attendees"
                2 -> "Insights"
                else -> ""
            }
        }.attach()

        val eventId = arguments?.getString("EVENT_ID")
        if (eventId != null) {
            viewModel.loadEvent(eventId)
        } else {
            Toast.makeText(context, "Event not found", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.error != null) {
                        Toast.makeText(context, state.error, Toast.LENGTH_SHORT).show()
                    } else if (state.event != null) {
                        bindEventData(state.event)
                    }
                }
            }
        }
    }

    private fun bindEventData(event: Event) {
        currentEventTitle = event.title
        binding.tvEventTitle.text = event.title
        binding.tvLocation.text = event.address ?: "Location TBD"

        val sdf = SimpleDateFormat("EEE, dd MMM yyyy - hh:mm a", Locale.getDefault())
        binding.tvDateTime.text = sdf.format(Date(event.startTime))

        if (event.category != null) {
            binding.chipCategory.isVisible = true
            binding.chipCategory.text = event.category.name
        } else {
            binding.chipCategory.isVisible = false
        }

        if (!event.mainImageUrl.isNullOrBlank()) {
            binding.ivEventImage.load(event.mainImageUrl) {
                crossfade(true)
                placeholder(R.drawable.event_img)
                error(R.drawable.event_img)
            }
        } else {
            binding.ivEventImage.setImageResource(R.drawable.event_img)
        }
    }

    private fun showEventActionsBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_event_actions, null)
        
        bottomSheetView.findViewById<View>(R.id.btnClose).setOnClickListener {
            bottomSheetDialog.dismiss()
        }
        
        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class ManageEventPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ManageEventOverviewFragment()
                1 -> ManageEventAttendeesFragment()
                2 -> ManageEventInsightsFragment()
                else -> throw IllegalArgumentException("Invalid position")
            }
        }
    }
}