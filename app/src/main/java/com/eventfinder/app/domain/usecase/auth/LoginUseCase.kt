package com.eventfinder.app.domain.usecase.auth

import com.eventfinder.app.domain.model.User
import com.eventfinder.app.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for user login
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<User> {
        return try {
            // Validate input
            require(email.isNotBlank()) { "Email cannot be empty" }
            require(password.isNotBlank()) { "Password cannot be empty" }
            require(android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                "Invalid email format"
            }

            authRepository.login(email.trim(), password)
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(Exception("Login failed: ${e.message}", e))
        }
    }
}
