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
        name: String,
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

                // Start with existing URLs if present
                var photoUrl: String? = if (user.userType == UserType.ORGANIZER) {
                    user.organizerProfile?.logoUrl
                } else {
                    user.profile?.photoUrl
                }

                // If a new image was explicitly selected, upload and replace it
                if (imageUri != null) {
                    val uploadResult = uploadImageUseCase(user.uid, imageUri)
                    photoUrl = uploadResult.getOrThrow()
                }

                val updatedUser = if (user.userType == UserType.ORGANIZER) {
                    user.copy(
                        organizerProfile = OrganizerProfile(
                            organizationName = name,
                            phoneNumber = contactNumber,
                            contactPerson = contactPerson,
                            description = description,
                            offeredEvents = interests, // Saving category IDs to Firestore
                            logoUrl = photoUrl
                        )
                    )
                } else {
                    user.copy(
                        profile = UserProfile(
                            fullName = name,
                            phoneNumber = contactNumber,
                            interests = interests, // Saving category IDs to Firestore
                            photoUrl = photoUrl
                        )
                    )
                }

                updateProfileUseCase(updatedUser).fold(
                    onSuccess = {
                        userPreferences.setUserName(name)
                        userPreferences.setUserType(user.userType.name)
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