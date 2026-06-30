package com.eventfinder.app.client.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventfinder.app.domain.model.User
import com.eventfinder.app.domain.model.UserType
import com.eventfinder.app.domain.usecase.auth.GetCurrentUserUseCase
import com.eventfinder.app.fcm.FcmTokenManager
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
    private val userPreferences: UserPreferences,
    private val fcmTokenManager: FcmTokenManager
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
                        fcmTokenManager.saveCurrentTokenForUser(
                            userId = user.uid,
                            notificationsEnabled = userPreferences.areNotificationsEnabled()
                        )
                        
                        user.profile?.fullName?.let {
                            userPreferences.setUserName(it)
                        } ?: user.organizerProfile?.organizationName?.let {
                            userPreferences.setUserName(it)
                        }

                        val isComplete = user.isOnboardingComplete()
                        val pendingAuthStep = userPreferences.getPendingAuthStep()
                        val shouldClearPendingStep = isComplete && when (pendingAuthStep) {
                            AuthPendingStep.FILL_PROFILE_REGISTER,
                            AuthPendingStep.FILL_PROFILE_LOGIN -> true
                            AuthPendingStep.CHOOSE_INTERESTS -> user.hasSelectedInterests()
                            else -> false
                        }
                        if (shouldClearPendingStep) {
                            userPreferences.clearPendingAuthStep()
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

                        if (isComplete) {
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

private fun User.hasSelectedInterests(): Boolean {
    return if (userType == UserType.USER) {
        profile?.interests?.isNotEmpty() == true
    } else {
        organizerProfile?.offeredEvents?.isNotEmpty() == true
    }
}
