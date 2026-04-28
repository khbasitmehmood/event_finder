package com.eventfinder.app.client.auth

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventfinder.app.domain.model.EventCategory
import com.eventfinder.app.domain.model.OrganizerProfile
import com.eventfinder.app.domain.model.User
import com.eventfinder.app.domain.model.UserProfile
import com.eventfinder.app.domain.model.UserType
import com.eventfinder.app.domain.usecase.GetEventCategoriesUseCase
import com.eventfinder.app.domain.usecase.auth.GetCurrentUserUseCase
import com.eventfinder.app.domain.usecase.auth.UpdateProfileUseCase
import com.eventfinder.app.domain.usecase.auth.UploadImageUseCase
import com.eventfinder.app.utils.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FillProfileViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val uploadImageUseCase: UploadImageUseCase,
    private val getEventCategoriesUseCase: GetEventCategoriesUseCase,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<FillProfileUiState>(FillProfileUiState.Idle)
    val uiState: StateFlow<FillProfileUiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    private val _categories = MutableStateFlow<List<EventCategory>>(emptyList())
    val categories: StateFlow<List<EventCategory>> = _categories.asStateFlow()

    init {
        loadCurrentUser()
        loadCategories()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            getCurrentUserUseCase().fold(
                onSuccess = { user ->
                    _currentUser.value = user
                },
                onFailure = {
                    _currentUser.value = null
                }
            )
        }
    }
    
    private fun loadCategories() {
        viewModelScope.launch {
            getEventCategoriesUseCase().fold(
                onSuccess = { list ->
                    _categories.value = list
                },
                onFailure = {
                    // Non-fatal, just will show empty chips or fallback
                    _categories.value = emptyList()
                }
            )
        }
    }

    fun updateProfile(
        userType: UserType,
        name: String,
        city: String,
        contactNumber: String,
        contactPerson: String,
        description: String,
        interests: List<String>,
        imageUri: Uri?
    ) {
        viewModelScope.launch {
            _uiState.value = FillProfileUiState.Loading

            try {
                val userResult = getCurrentUserUseCase()
                val user = userResult.getOrNull() ?: throw Exception("User not found")
                val effectiveUserType = userType

                // Start with existing URLs if present
                var photoUrl: String? = if (effectiveUserType == UserType.ORGANIZER) {
                    user.organizerProfile?.logoUrl
                } else {
                    user.profile?.photoUrl
                }

                // If a new image was explicitly selected, upload and replace it
                if (imageUri != null) {
                    val uploadResult = uploadImageUseCase(user.uid, imageUri)
                    photoUrl = uploadResult.getOrThrow()
                }

                val updatedUser = if (effectiveUserType == UserType.ORGANIZER) {
                    val prevInterests = user.organizerProfile?.offeredEvents ?: emptyList()
                    val newInterests = if (interests.isNotEmpty()) interests else prevInterests

                    user.copy(
                        userType = UserType.ORGANIZER,
                        organizerProfile = OrganizerProfile(
                            organizationName = name,
                            phoneNumber = contactNumber,
                            contactPerson = contactPerson,
                            city = city.ifBlank { null },
                            description = description,
                            offeredEvents = newInterests, // Keep existing if not provided here
                            logoUrl = photoUrl
                        )
                    )
                } else {
                    val prevInterests = user.profile?.interests ?: emptyList()
                    val newInterests = if (interests.isNotEmpty()) interests else prevInterests

                    user.copy(
                        userType = UserType.USER,
                        profile = UserProfile(
                            fullName = name,
                            phoneNumber = contactNumber,
                            city = city.ifBlank { null },
                            bio = description.ifBlank { null },
                            interests = newInterests, // Keep existing if not provided here
                            photoUrl = photoUrl
                        )
                    )
                }

                updateProfileUseCase(updatedUser).fold(
                    onSuccess = {
                        userPreferences.setUserName(name)
                        userPreferences.setUserType(effectiveUserType.name)
                        _uiState.value = FillProfileUiState.Success
                    },
                    onFailure = {
                        _uiState.value = FillProfileUiState.Error(it.message ?: "Failed to update profile")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = FillProfileUiState.Error(e.message ?: "An error occurred")
            }
        }
    }

    fun updateInterests(interests: List<String>) {
        viewModelScope.launch {
            _uiState.value = FillProfileUiState.Loading
            try {
                val userResult = getCurrentUserUseCase()
                val user = userResult.getOrNull() ?: throw Exception("User not found")

                val updatedUser = if (user.userType == UserType.ORGANIZER) {
                    val currentProfile = user.organizerProfile ?: OrganizerProfile()
                    user.copy(organizerProfile = currentProfile.copy(offeredEvents = interests))
                } else {
                    val currentProfile = user.profile ?: UserProfile()
                    user.copy(profile = currentProfile.copy(interests = interests))
                }

                updateProfileUseCase(updatedUser).fold(
                    onSuccess = {
                        _uiState.value = FillProfileUiState.Success
                    },
                    onFailure = {
                        _uiState.value = FillProfileUiState.Error(it.message ?: "Failed to update interests")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = FillProfileUiState.Error(e.message ?: "An error occurred")
            }
        }
    }

    fun resetState() {
        _uiState.value = FillProfileUiState.Idle
    }
}

sealed class FillProfileUiState {
    object Idle : FillProfileUiState()
    object Loading : FillProfileUiState()
    object Success : FillProfileUiState()
    data class Error(val message: String) : FillProfileUiState()
}