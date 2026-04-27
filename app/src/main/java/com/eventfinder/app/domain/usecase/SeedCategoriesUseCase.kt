package com.eventfinder.app.domain.usecase

import com.eventfinder.app.domain.model.EventCategory
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeedCategoriesUseCase @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend operator fun invoke(): Result<Unit> {
        return try {
            val categoriesCollection = firestore.collection("categories")
            
            // Check if the NEW updated categories already exist by checking for "cat_cinema"
            val cinemaDoc = categoriesCollection.document("cat_cinema").get().await()
            if (cinemaDoc.exists()) {
                return Result.success(Unit) // Already seeded with the new concise categories
            }

            // If we are here, we either have the old long categories or nothing.
            // Let's wipe everything in the collection first to start fresh.
            val allExistingDocs = categoriesCollection.get().await()
            val batch = firestore.batch()
            
            for (doc in allExistingDocs.documents) {
                batch.delete(doc.reference)
            }

            // The new succinct 25 categories
            val defaultCategories = listOf(
                EventCategory("cat_music", "Music"),
                EventCategory("cat_food", "Food"),
                EventCategory("cat_tech", "Tech"),
                EventCategory("cat_qawwali", "Qawwali"),
                EventCategory("cat_mushaira", "Mushaira"),
                EventCategory("cat_sports", "Sports"),
                EventCategory("cat_business", "Business"),
                EventCategory("cat_art", "Art"),
                EventCategory("cat_theater", "Theater"),
                EventCategory("cat_cinema", "Cinema"),
                EventCategory("cat_workshops", "Workshops"),
                EventCategory("cat_education", "Education"),
                EventCategory("cat_comedy", "Comedy"),
                EventCategory("cat_fashion", "Fashion"),
                EventCategory("cat_auto", "Auto"),
                EventCategory("cat_gaming", "Gaming"),
                EventCategory("cat_health", "Health"),
                EventCategory("cat_real_estate", "Real Estate"),
                EventCategory("cat_charity", "Charity"),
                EventCategory("cat_religion", "Religion"),
                EventCategory("cat_culture", "Culture"),
                EventCategory("cat_photography", "Photography"),
                EventCategory("cat_pets", "Pets"),
                EventCategory("cat_fitness", "Fitness"),
                EventCategory("cat_books", "Books")
            )

            // Add the new categories to the batch
            for (category in defaultCategories) {
                val docRef = categoriesCollection.document(category.id)
                batch.set(docRef, category)
            }
            
            // Commit deletion of old and addition of new categories
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}