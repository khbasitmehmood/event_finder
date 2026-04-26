package com.eventfinder.app.domain.usecase.auth

import com.eventfinder.app.domain.model.User
import com.eventfinder.app.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case to get current authenticated user
 */
class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<User?> {
        return try {
            authRepository.getCurrentUser()
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get current user: ${e.message}", e))
        }
    }
}
