package com.eventfinder.app.client.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventfinder.app.domain.model.User
import com.eventfinder.app.domain.model.UserType
import com.eventfinder.app.domain.usecase.auth.LoginUseCase
import com.eventfinder.app.fcm.FcmTokenManager
import com.eventfinder.app.utils.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val userPreferences: UserPreferences,
    private val fcmTokenManager: FcmTokenManager
) : ViewModel() {

    companion object {
        private const val LOGIN_FLOW_TIMEOUT_MS = 20_000L
    }

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var isLoginInProgress = false

    fun login(email: String, password: String) {
        if (isLoginInProgress) return

        viewModelScope.launch {
            isLoginInProgress = true
            _uiState.value = AuthUiState.Loading

            try {
                withTimeout(LOGIN_FLOW_TIMEOUT_MS) {
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
                            fcmTokenManager.saveCurrentTokenForUser(
                                userId = user.uid,
                                notificationsEnabled = userPreferences.areNotificationsEnabled()
                            )

                            _uiState.value = AuthUiState.Success(
                                user.copy(isProfileComplete = user.isOnboardingComplete())
                            )
                        },
                        onFailure = { error ->
                            _uiState.value = AuthUiState.Error(
                                error.message ?: "Login failed"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Login failed")
            } finally {
                isLoginInProgress = false
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}

private fun User.isOnboardingComplete(): Boolean {
    if (isProfileComplete) return true

    return if (userType == UserType.USER) {
        profile?.fullName?.isNotBlank() == true
    } else {
        val organizer = organizerProfile ?: return false
        organizer.organizationName.isNotBlank() &&
            organizer.contactPerson.isNotBlank() &&
            organizer.phoneNumber.isNotBlank()
    }
}
