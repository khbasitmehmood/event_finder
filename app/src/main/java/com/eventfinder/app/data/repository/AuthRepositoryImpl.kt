package com.eventfinder.app.data.repository

import com.eventfinder.app.data.mapper.UserMapper
import com.eventfinder.app.data.model.UserDto
import com.eventfinder.app.domain.model.User
import com.eventfinder.app.domain.model.UserType
import com.eventfinder.app.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AuthRepository using Firebase Auth and Firestore
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    companion object {
        private const val USERS_COLLECTION = "users"
    }

    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: throw Exception("User ID not found")

            val user = getUserById(uid).getOrThrow() ?: throw Exception("User data not found")

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception("Login failed: ${e.message}", e))
        }
    }

    override suspend fun signup(email: String, password: String, userType: UserType): Result<User> {
        return try {
            // Create auth user
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: throw Exception("User ID not found")

            // Create user document in Firestore
            val user = User(
                uid = uid,
                email = email,
                userType = userType,
                profile = null,
                organizerProfile = null,
                createdAt = System.currentTimeMillis(),
                isProfileComplete = false
            )

            val userDto = UserMapper.toDto(user)
            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .set(userDto)
                .await()

            Result.success(user)
        } catch (e: Exception) {
            val message = when (e) {
                is FirebaseAuthUserCollisionException -> "User already exists. Please log in."
                else -> "Signup failed: ${e.message}"
            }
            Result.failure(Exception(message, e))
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Logout failed: ${e.message}", e))
        }
    }

    override suspend fun getCurrentUser(): Result<User?> {
        return try {
            val uid = firebaseAuth.currentUser?.uid
            if (uid == null) {
                Result.success(null)
            } else {
                getUserById(uid)
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get current user: ${e.message}", e))
        }
    }

    override suspend fun updateUserProfile(user: User): Result<User> {
        return try {
            val userDto = UserMapper.toDto(user.copy(
                updatedAt = System.currentTimeMillis()
            ))

            firestore.collection(USERS_COLLECTION)
                .document(user.uid)
                .set(userDto)
                .await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update profile: ${e.message}", e))
        }
    }

    override suspend fun getUserById(uid: String): Result<User?> {
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .await()

            val user = snapshot.toObject(UserDto::class.java)?.let { dto ->
                UserMapper.toDomain(dto)
            }

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get user: ${e.message}", e))
        }
    }

    override suspend fun isEmailAvailable(email: String): Result<Boolean> {
        return try {
            val methods = firebaseAuth.fetchSignInMethodsForEmail(email).await()
            Result.success(methods.signInMethods?.isEmpty() ?: true)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to check email: ${e.message}", e))
        }
    }

    override fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }
}
