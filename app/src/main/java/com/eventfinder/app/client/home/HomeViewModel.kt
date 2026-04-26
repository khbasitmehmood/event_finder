package com.eventfinder.app.client.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.model.User
import com.eventfinder.app.domain.model.UserType
import com.eventfinder.app.domain.usecase.GetExploreEventsUseCase
import com.eventfinder.app.domain.usecase.GetUserEventsUseCase
import com.eventfinder.app.domain.usecase.auth.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Combined UI State for Home Screen
 */
data class HomeUiState(
    val user: User? = null,
    val userEvents: List<Event> = emptyList(),
    val featuredEvents: List<Event> = emptyList(),
    val isLoadingUserEvents: Boolean = false,
    val isLoadingFeatured: Boolean = false,
    val error: String? = null
) {
    val isOrganizer: Boolean
        get() = user?.userType == UserType.ORGANIZER
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getUserEventsUseCase: GetUserEventsUseCase,
    private val getExploreEventsUseCase: GetExploreEventsUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    /**
     * Loads the user and subsequently their events and featured events
     */
    fun loadData() {
        viewModelScope.launch {
            // Fetch User
            getCurrentUserUseCase().fold(
                onSuccess = { user ->
                    _uiState.update { it.copy(user = user) }
                    // Load events requiring user context
                    user?.uid?.let { fetchUserEvents(it) }
                },
                onFailure = {
                    _uiState.update { it.copy(user = null, error = "Failed to load user session.") }
                }
            )
            
            // Fetch Featured Events in parallel
            fetchFeaturedEvents()
        }
    }

    private suspend fun fetchUserEvents(userId: String) {
        _uiState.update { it.copy(isLoadingUserEvents = true, error = null) }
        
        getUserEventsUseCase(userId).fold(
            onSuccess = { events ->
                _uiState.update { it.copy(userEvents = events, isLoadingUserEvents = false) }
            },
            onFailure = { exception ->
                _uiState.update { 
                    it.copy(
                        error = exception.message ?: "Failed to load your events",
                        isLoadingUserEvents = false
                    ) 
                }
            }
        )
    }

    private suspend fun fetchFeaturedEvents() {
        _uiState.update { it.copy(isLoadingFeatured = true, error = null) }

        getExploreEventsUseCase().fold(
            onSuccess = { events ->
                _uiState.update { 
                    it.copy(
                        featuredEvents = events.take(3), 
                        isLoadingFeatured = false
                    ) 
                }
            },
            onFailure = { exception ->
                _uiState.update { 
                    it.copy(
                        error = exception.message ?: "Failed to load featured events",
                        isLoadingFeatured = false
                    ) 
                }
            }
        )
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}