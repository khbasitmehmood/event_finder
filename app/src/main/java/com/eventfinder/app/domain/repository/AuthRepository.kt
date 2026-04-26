package com.eventfinder.app.domain.repository

import com.eventfinder.app.domain.model.User
import com.eventfinder.app.domain.model.UserType

/**
 * Repository interface for authentication operations
 */
interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun signup(email: String, password: String, userType: UserType): Result<User>
    suspend fun logout(): Result<Unit>
    suspend fun getCurrentUser(): Result<User?>
    suspend fun updateUserProfile(user: User): Result<User>
    suspend fun getUserById(uid: String): Result<User?>
    suspend fun isEmailAvailable(email: String): Result<Boolean>
    fun getCurrentUserId(): String?
}
