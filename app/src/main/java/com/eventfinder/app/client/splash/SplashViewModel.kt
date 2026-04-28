package com.eventfinder.app.client.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventfinder.app.domain.model.UserType
import com.eventfinder.app.domain.usecase.auth.GetCurrentUserUseCase
import com.eventfinder.app.utils.AuthFlowSource
import com.eventfinder.app.utils.AuthPendingStep
import com.eventfinder.app.utils.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _navigationState = MutableStateFlow<SplashNavigationState>(SplashNavigationState.Idle)
    val navigationState: StateFlow<SplashNavigationState> = _navigationState.asStateFlow()

    fun checkSession() {
        viewModelScope.launch {
            // Small delay for splash effect
            delay(1000)

            getCurrentUserUseCase().fold(
                onSuccess = { user ->
                    if (user != null) {
                        // User is logged in, update preferences
                        userPreferences.setUserId(user.uid)
                        userPreferences.setUserType(user.userType.name)
                        
                        user.profile?.fullName?.let {
                            userPreferences.setUserName(it)
                        } ?: user.organizerProfile?.organizationName?.let {
                            userPreferences.setUserName(it)
                        }

                        when (userPreferences.getPendingAuthStep()) {
                            AuthPendingStep.CHOOSE_INTERESTS -> {
                                _navigationState.value = SplashNavigationState.NavigateToChooseInterests(user.userType)
                                return@fold
                            }
                            AuthPendingStep.FILL_PROFILE_REGISTER -> {
                                _navigationState.value = SplashNavigationState.NavigateToFillProfile(user.userType, AuthFlowSource.REGISTER)
                                return@fold
                            }
                            AuthPendingStep.FILL_PROFILE_LOGIN -> {
                                _navigationState.value = SplashNavigationState.NavigateToFillProfile(user.userType, AuthFlowSource.LOGIN)
                                return@fold
                            }
                        }

                        // Verify profile completeness
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

                        if (isComplete) {
                            userPreferences.clearPendingAuthStep()
                            // Navigate to appropriate home based on user type
                            _navigationState.value = if (user.userType == UserType.ORGANIZER) {
                                SplashNavigationState.NavigateToDashboard
                            } else {
                                SplashNavigationState.NavigateToHome
                            }
                        } else {
                            _navigationState.value = SplashNavigationState.NavigateToFillProfile(user.userType, AuthFlowSource.LOGIN)
                        }
                    } else {
                        // No user logged in, go to login
                        _navigationState.value = SplashNavigationState.NavigateToLogin
                    }
                },
                onFailure = {
                    // Error or no session, go to login
                    _navigationState.value = SplashNavigationState.NavigateToLogin
                }
            )
        }
    }
}

sealed class SplashNavigationState {
    object Idle : SplashNavigationState()
    object NavigateToHome : SplashNavigationState()
    object NavigateToDashboard : SplashNavigationState()
    object NavigateToLogin : SplashNavigationState()
    data class NavigateToFillProfile(val userType: UserType, val flowSource: String) : SplashNavigationState()
    data class NavigateToChooseInterests(val userType: UserType) : SplashNavigationState()
}
