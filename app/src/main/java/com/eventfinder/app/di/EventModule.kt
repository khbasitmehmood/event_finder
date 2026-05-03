package com.eventfinder.app.di

import com.eventfinder.app.data.repository.EventRepositoryImpl
import com.eventfinder.app.data.source.EventDataSource
import com.eventfinder.app.data.source.FirestoreEventDataSource
import com.eventfinder.app.domain.repository.EventRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing Event-related dependencies
 * Uses FirestoreEventDataSource for all event operations
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class EventModule {

    @Binds
    @Singleton
    abstract fun bindEventDataSource(
        firestoreEventDataSource: FirestoreEventDataSource
    ): EventDataSource

    @Binds
    @Singleton
    abstract fun bindEventRepository(
        eventRepositoryImpl: EventRepositoryImpl
    ): EventRepository
}
