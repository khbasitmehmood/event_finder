package com.eventfinder.app.domain.usecase.auth

import com.eventfinder.app.domain.model.User
import com.eventfinder.app.domain.model.UserType
import com.eventfinder.app.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for user signup/registration
 */
class SignupUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        confirmPassword: String,
        userType: UserType
    ): Result<User> {
        return try {
            // Validate input
            require(email.isNotBlank()) { "Email cannot be empty" }
            require(password.isNotBlank()) { "Password cannot be empty" }
            require(android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                "Invalid email format"
            }
            require(password.length >= 6) { "Password must be at least 6 characters" }
            require(password == confirmPassword) { "Passwords do not match" }

            authRepository.signup(email.trim(), password, userType)
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(Exception("Signup failed: ${e.message}", e))
        }
    }
}
