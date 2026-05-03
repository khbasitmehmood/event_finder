package com.eventfinder.app.client.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventfinder.app.domain.model.EventNotification
import com.eventfinder.app.domain.service.NotificationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for notifications screen
 */
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationService: NotificationService
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Loading)
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    // TODO: Replace with actual user ID from auth
    private val currentUserId = "user_placeholder"

    init {
        loadNotifications()
        loadUnreadCount()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = NotificationsUiState.Loading

            val result = notificationService.getUserNotifications(
                userId = currentUserId,
                limit = 50
            )

            result.onSuccess { notifications ->
                if (notifications.isEmpty()) {
                    _uiState.value = NotificationsUiState.Empty
                } else {
                    val unread = notifications.filter { !it.isRead }
                    val read = notifications.filter { it.isRead }
                    _uiState.value = NotificationsUiState.Success(
                        unreadNotifications = unread,
                        allNotifications = read
                    )
                }
            }.onFailure { error ->
                _uiState.value = NotificationsUiState.Error(
                    error.message ?: "Failed to load notifications"
                )
            }

            loadUnreadCount()
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationService.markAsRead(notificationId)
            loadNotifications()
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            notificationService.markAllAsRead(currentUserId)
            loadNotifications()
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            notificationService.deleteNotification(notificationId)
            loadNotifications()
        }
    }

    private fun loadUnreadCount() {
        viewModelScope.launch {
            val result = notificationService.getUnreadCount(currentUserId)
            result.onSuccess { count ->
                _unreadCount.value = count
            }
        }
    }

    /**
     * Get unread count for badge display
     * Call this from other screens to show notification badge
     */
    fun refreshUnreadCount() {
        loadUnreadCount()
    }
}

/**
 * UI state for notifications screen
 */
sealed class NotificationsUiState {
    object Loading : NotificationsUiState()
    object Empty : NotificationsUiState()
    data class Success(
        val unreadNotifications: List<EventNotification>,
        val allNotifications: List<EventNotification>
    ) : NotificationsUiState()
    data class Error(val message: String) : NotificationsUiState()
}
