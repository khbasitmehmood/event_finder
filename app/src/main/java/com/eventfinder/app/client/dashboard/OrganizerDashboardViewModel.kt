package com.eventfinder.app.client.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.model.User
import com.eventfinder.app.domain.usecase.auth.GetCurrentUserUseCase
import com.eventfinder.app.domain.usecase.GetUserEventsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrganizerDashboardViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getUserEventsUseCase: GetUserEventsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrganizerDashboardUiState())
    val uiState: StateFlow<OrganizerDashboardUiState> = _uiState.asStateFlow()

    data class OrganizerDashboardUiState(
        val user: User? = null,
        val userEvents: List<Event> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null
    )

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Load current user
            getCurrentUserUseCase().fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(user = user)

                    // Load organizer's events
                    user?.let {
                        loadUserEvents(it.uid)
                    }
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load user data"
                    )
                }
            )
        }
    }

    private suspend fun loadUserEvents(userId: String) {
        getUserEventsUseCase(userId).fold(
            onSuccess = { events ->
                _uiState.value = _uiState.value.copy(
                    userEvents = events,
                    isLoading = false
                )
            },
            onFailure = { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = exception.message ?: "Failed to load events"
                )
            }
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
