package com.eventfinder.app.domain.repository

import android.net.Uri

interface StorageRepository {
    suspend fun uploadProfileImage(userId: String, imageUri: Uri): Result<String>
}