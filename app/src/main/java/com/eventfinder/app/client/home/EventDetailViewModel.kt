package com.eventfinder.app.client.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.usecase.GetEventByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventDetailUiState(
    val event: Event? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val getEventByIdUseCase: GetEventByIdUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventDetailUiState())
    val uiState: StateFlow<EventDetailUiState> = _uiState.asStateFlow()

    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            getEventByIdUseCase(eventId).fold(
                onSuccess = { event ->
                    if (event != null) {
                        _uiState.update { it.copy(event = event, isLoading = false) }
                    } else {
                        _uiState.update { it.copy(error = "Event not found", isLoading = false) }
                    }
                },
                onFailure = { exception ->
                    _uiState.update { 
                        it.copy(error = exception.message ?: "Failed to load event", isLoading = false) 
                    }
                }
            )
        }
    }
}
