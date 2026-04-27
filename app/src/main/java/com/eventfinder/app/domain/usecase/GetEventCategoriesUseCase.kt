package com.eventfinder.app.domain.usecase

import com.eventfinder.app.domain.model.EventCategory
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetEventCategoriesUseCase @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend operator fun invoke(): Result<List<EventCategory>> {
        return try {
            val snapshot = firestore.collection("categories")
                .orderBy("name")
                .get()
                .await()

            val categories = snapshot.documents.mapNotNull { doc ->
                val category = doc.toObject(EventCategory::class.java)
                // Fallback to doc.id just in case the id field is missing in the document data
                category?.copy(id = category.id.ifEmpty { doc.id })
            }
            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}