package com.eventfinder.app.domain.usecase.auth

import com.eventfinder.app.domain.model.User
import com.eventfinder.app.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for updating user profile
 */
class UpdateProfileUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(user: User): Result<User> {
        return try {
            // Validate based on user type
            if (user.userType == com.eventfinder.app.domain.model.UserType.USER) {
                require(user.profile?.fullName?.isNotBlank() == true) {
                    "Full name is required"
                }
            } else {
                require(user.organizerProfile?.organizationName?.isNotBlank() == true) {
                    "Organization name is required"
                }
                require(user.organizerProfile?.contactPerson?.isNotBlank() == true) {
                    "Contact person is required"
                }
                require(user.organizerProfile?.phoneNumber?.isNotBlank() == true) {
                    "Phone number is required"
                }
            }

            authRepository.updateUserProfile(user.copy(isProfileComplete = true))
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(Exception("Profile update failed: ${e.message}", e))
        }
    }
}
