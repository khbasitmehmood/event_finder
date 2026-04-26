package com.eventfinder.app.client.auth

import com.eventfinder.app.domain.model.User

/**
 * UI state for authentication screens
 */
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: User) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
