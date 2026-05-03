package com.eventfinder.app.di

import com.eventfinder.app.data.service.FirebaseNotificationServiceImpl
import com.eventfinder.app.data.service.NotificationServiceImpl
import com.eventfinder.app.domain.service.NotificationService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing Notification-related dependencies
 *
 * Toggle between implementations:
 * - NotificationServiceImpl: In-memory (for testing)
 * - FirebaseNotificationServiceImpl: Firestore + FCM (for production)
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {

    @Binds
    @Singleton
    abstract fun bindNotificationService(
        // Use FirebaseNotificationServiceImpl for Firestore + FCM
        firebaseNotificationServiceImpl: FirebaseNotificationServiceImpl

        // OR use NotificationServiceImpl for in-memory testing
        // notificationServiceImpl: NotificationServiceImpl
    ): NotificationService
}
