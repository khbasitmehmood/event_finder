package com.eventfinder.app.domain.usecase.auth

import android.net.Uri
import com.eventfinder.app.domain.repository.StorageRepository
import javax.inject.Inject

class UploadImageUseCase @Inject constructor(
    private val storageRepository: StorageRepository
) {
    suspend operator fun invoke(userId: String, imageUri: Uri): Result<String> {
        return storageRepository.uploadProfileImage(userId, imageUri)
    }
}