package com.eventfinder.app.domain.usecase.auth

import com.eventfinder.app.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for logging out the user
 */
class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return try {
            authRepository.logout()
        } catch (e: Exception) {
            Result.failure(Exception("Logout failed: ${e.message}", e))
        }
    }
}
