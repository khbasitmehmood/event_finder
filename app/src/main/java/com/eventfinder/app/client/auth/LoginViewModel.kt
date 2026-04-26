package com.eventfinder.app.client.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventfinder.app.domain.model.UserType
import com.eventfinder.app.domain.usecase.auth.LoginUseCase
import com.eventfinder.app.utils.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            loginUseCase(email, password).fold(
                onSuccess = { user ->
                    // Store user ID and name in preferences
                    userPreferences.setUserId(user.uid)
                    user.profile?.fullName?.let {
                        userPreferences.setUserName(it)
                    } ?: user.organizerProfile?.organizationName?.let {
                        userPreferences.setUserName(it)
                    }
                    
                    userPreferences.setUserType(user.userType.name)

                    // Dynamically check if profile is complete
                    val isComplete = if (user.userType == UserType.USER) {
                        user.profile != null &&
                        user.profile.fullName.isNotBlank() &&
                        !user.profile.photoUrl.isNullOrBlank()
                    } else {
                        user.organizerProfile != null &&
                        user.organizerProfile.organizationName.isNotBlank() &&
                        user.organizerProfile.contactPerson.isNotBlank() &&
                        user.organizerProfile.phoneNumber.isNotBlank() &&
                        !user.organizerProfile.logoUrl.isNullOrBlank()
                    }

                    _uiState.value = AuthUiState.Success(user.copy(isProfileComplete = isComplete))
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(
                        error.message ?: "Login failed"
                    )
                }
            )
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
