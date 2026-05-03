package com.eventfinder.app.client.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.eventfinder.app.R
import com.eventfinder.app.databinding.FragmentNotificationsBinding
import com.eventfinder.app.domain.model.EventNotification
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Fragment displaying user notifications
 */
@AndroidEntryPoint
class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotificationsViewModel by viewModels()

    private lateinit var unreadAdapter: NotificationAdapter
    private lateinit var allAdapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerViews()
        setupSwipeRefresh()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_mark_all_read -> {
                    viewModel.markAllAsRead()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerViews() {
        unreadAdapter = NotificationAdapter { notification ->
            onNotificationClick(notification)
        }

        allAdapter = NotificationAdapter { notification ->
            onNotificationClick(notification)
        }

        binding.recyclerViewUnread.adapter = unreadAdapter
        binding.recyclerViewAll.adapter = allAdapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadNotifications()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                handleUiState(state)
            }
        }
    }

    private fun handleUiState(state: NotificationsUiState) {
        binding.swipeRefreshLayout.isRefreshing = false

        when (state) {
            is NotificationsUiState.Loading -> {
                binding.loadingState.isVisible = true
                binding.emptyState.isVisible = false
                binding.unreadSection.isVisible = false
                binding.allSection.isVisible = false
            }

            is NotificationsUiState.Empty -> {
                binding.loadingState.isVisible = false
                binding.emptyState.isVisible = true
                binding.unreadSection.isVisible = false
                binding.allSection.isVisible = false
            }

            is NotificationsUiState.Success -> {
                binding.loadingState.isVisible = false
                binding.emptyState.isVisible = false

                // Show unread section
                if (state.unreadNotifications.isNotEmpty()) {
                    binding.unreadSection.isVisible = true
                    unreadAdapter.submitList(state.unreadNotifications)
                } else {
                    binding.unreadSection.isVisible = false
                }

                // Show all section
                if (state.allNotifications.isNotEmpty()) {
                    binding.allSection.isVisible = true
                    allAdapter.submitList(state.allNotifications)
                } else {
                    binding.allSection.isVisible = false
                }
            }

            is NotificationsUiState.Error -> {
                binding.loadingState.isVisible = false
                binding.emptyState.isVisible = false
                Snackbar.make(
                    binding.root,
                    state.message,
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun onNotificationClick(notification: EventNotification) {
        // Mark as read
        if (!notification.isRead) {
            viewModel.markAsRead(notification.notificationId)
        }

        // Navigate to relevant screen based on notification type
        // For now, just show a message
        Snackbar.make(
            binding.root,
            "Clicked: ${notification.title}",
            Snackbar.LENGTH_SHORT
        ).show()

        // TODO: Implement deep linking based on notification.actionUrl or notification.eventId
        // Example:
        // if (notification.eventId.isNotEmpty()) {
        //     val action = NotificationsFragmentDirections
        //         .actionNotificationsToEventDetail(notification.eventId)
        //     findNavController().navigate(action)
        // }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
