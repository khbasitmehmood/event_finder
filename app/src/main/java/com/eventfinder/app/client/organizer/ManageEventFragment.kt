package com.eventfinder.app.client.organizer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
import com.eventfinder.app.utils.UserPreferences
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class ManageEventFragment : Fragment() {

    private var _binding: FragmentManageEventBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EventDetailViewModel by viewModels()
    private val sharedViewModel: ManageEventSharedViewModel by activityViewModels()
    private var currentEventTitle: String = ""

    @Inject
    lateinit var userPreferences: UserPreferences

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

        binding.fabScanQR.setOnClickListener {
            findNavController().navigate(R.id.qrScannerFragment)
        }

        binding.fabPublish.setOnClickListener {
            showPublishDialog()
        }

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
        val userId = userPreferences.getUserId()

        if (eventId != null) {
            viewModel.loadEvent(eventId, userId)
            // Load data for child fragments
            sharedViewModel.loadEventData(eventId)
        } else {
            Toast.makeText(context, "Event not found", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }

        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning from QR scanner
        sharedViewModel.refreshData()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        if (state.error != null) {
                            Toast.makeText(context, state.error, Toast.LENGTH_SHORT).show()
                        } else if (state.event != null) {
                            bindEventData(state.event)
                        }
                    }
                }

                launch {
                    sharedViewModel.isOnline.collect { isOnline ->
                        binding.cardOffline.isVisible = !isOnline
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

        // Show event state
        binding.chipState.text = event.state.getDisplayName()
        val stateColors = getStateColors(event.state)
        binding.chipState.setChipBackgroundColorResource(stateColors.first)
        binding.chipState.setTextColor(resources.getColor(stateColors.second, null))

        // Show/hide Publish FAB based on state
        binding.fabPublish.isVisible = event.state == com.eventfinder.app.domain.model.EventState.DRAFT
        binding.fabScanQR.isVisible = event.state != com.eventfinder.app.domain.model.EventState.DRAFT

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

    private fun getStateColors(state: com.eventfinder.app.domain.model.EventState): Pair<Int, Int> {
        return when (state) {
            com.eventfinder.app.domain.model.EventState.DRAFT ->
                Pair(R.color.md_surface_container_high, R.color.md_on_surface_variant)
            com.eventfinder.app.domain.model.EventState.SCHEDULED ->
                Pair(R.color.md_primary_container, R.color.md_primary)
            com.eventfinder.app.domain.model.EventState.LIVE ->
                Pair(R.color.md_tertiary_container, R.color.md_tertiary)
            com.eventfinder.app.domain.model.EventState.COMPLETED ->
                Pair(R.color.md_surface_container_high, R.color.md_on_surface_variant)
            com.eventfinder.app.domain.model.EventState.CANCELLED ->
                Pair(R.color.md_error_container, R.color.md_error)
            com.eventfinder.app.domain.model.EventState.POSTPONED ->
                Pair(R.color.md_secondary_container, R.color.md_secondary)
            com.eventfinder.app.domain.model.EventState.EXPIRED ->
                Pair(R.color.md_error_container, R.color.md_error)
        }
    }

    private fun showEventActionsBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_event_actions, null)

        bottomSheetView.findViewById<View>(R.id.btnClose).setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetView.findViewById<View>(R.id.btnPostpone).setOnClickListener {
            bottomSheetDialog.dismiss()
            showPostponeEventBottomSheet()
        }

        bottomSheetView.findViewById<View>(R.id.btnReschedule).setOnClickListener {
            bottomSheetDialog.dismiss()
            showRescheduleEventBottomSheet()
        }

        bottomSheetView.findViewById<View>(R.id.btnCancelEvent).setOnClickListener {
            bottomSheetDialog.dismiss()
            showCancelEventDialog()
        }

        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()
    }

    private fun showPostponeEventBottomSheet() {
        val eventId = arguments?.getString("EVENT_ID") ?: return
        val userId = userPreferences.getUserId() ?: return

        val bottomSheet = PostponeEventBottomSheet.newInstance { newStartTime, newEndTime, reason ->
            sharedViewModel.postponeEvent(
                eventId = eventId,
                newStartTime = newStartTime,
                newEndTime = newEndTime,
                reason = reason,
                userId = userId,
                onSuccess = { event ->
                    Toast.makeText(
                        requireContext(),
                        "Event postponed successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Reload event data
                    viewModel.loadEvent(eventId, userId)
                },
                onError = { error ->
                    Toast.makeText(
                        requireContext(),
                        error,
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }

        bottomSheet.show(parentFragmentManager, PostponeEventBottomSheet.TAG)
    }

    private fun showRescheduleEventBottomSheet() {
        val eventId = arguments?.getString("EVENT_ID") ?: return
        val userId = userPreferences.getUserId() ?: return

        // Get current event from viewModel state
        val currentEvent = viewModel.uiState.value.event ?: run {
            Toast.makeText(
                requireContext(),
                "Event not loaded yet. Please try again.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val bottomSheet = RescheduleEventBottomSheet.newInstance(currentEvent) { newStartTime, newEndTime, newLocation, newAddress, reason ->
            sharedViewModel.rescheduleEvent(
                eventId = eventId,
                newStartTime = newStartTime,
                newEndTime = newEndTime,
                newLocation = newLocation,
                newAddress = newAddress,
                reason = reason,
                userId = userId,
                onSuccess = { event ->
                    Toast.makeText(
                        requireContext(),
                        "Event rescheduled successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Reload event data
                    viewModel.loadEvent(eventId, userId)
                },
                onError = { error ->
                    Toast.makeText(
                        requireContext(),
                        error,
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }

        bottomSheet.show(parentFragmentManager, RescheduleEventBottomSheet.TAG)
    }

    private fun showCancelEventDialog() {
        val eventId = arguments?.getString("EVENT_ID") ?: return
        val userId = userPreferences.getUserId() ?: return

        // Get current event from viewModel state
        val currentEvent = viewModel.uiState.value.event ?: run {
            Toast.makeText(
                requireContext(),
                "Event not loaded yet. Please try again.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val dialog = CancelEventDialog.newInstance(currentEvent) { reason ->
            sharedViewModel.cancelEvent(
                eventId = eventId,
                reason = reason,
                userId = userId,
                onSuccess = { event ->
                    Toast.makeText(
                        requireContext(),
                        "Event cancelled successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Reload event data
                    viewModel.loadEvent(eventId, userId)
                },
                onError = { error ->
                    Toast.makeText(
                        requireContext(),
                        error,
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }

        dialog.show(parentFragmentManager, CancelEventDialog.TAG)
    }

    private fun showPublishDialog() {
        val eventId = arguments?.getString("EVENT_ID") ?: return
        val userId = userPreferences.getUserId() ?: return

        val currentEvent = viewModel.uiState.value.event ?: run {
            Toast.makeText(
                requireContext(),
                "Event not loaded yet. Please try again.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Publish Event")
            .setMessage("Are you ready to publish \"${currentEvent.title}\"? It will become visible to all users and they can start purchasing tickets.")
            .setPositiveButton("Publish") { _, _ ->
                publishEvent(eventId, userId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun publishEvent(eventId: String, userId: String) {
        sharedViewModel.publishEvent(
            eventId = eventId,
            organizerId = userId,
            onSuccess = { event ->
                Toast.makeText(
                    requireContext(),
                    "Event published successfully!",
                    Toast.LENGTH_SHORT
                ).show()
                // Reload event data to update UI
                viewModel.loadEvent(eventId, userId)
            },
            onError = { error ->
                Toast.makeText(
                    requireContext(),
                    error,
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class ManageEventPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            val eventId = arguments?.getString("EVENT_ID")
            val bundle = Bundle().apply {
                putString("EVENT_ID", eventId)
            }

            return when (position) {
                0 -> ManageEventOverviewFragment().apply { arguments = bundle }
                1 -> ManageEventAttendeesFragment().apply { arguments = bundle }
                2 -> ManageEventInsightsFragment().apply { arguments = bundle }
                else -> throw IllegalArgumentException("Invalid position")
            }
        }
    }
}