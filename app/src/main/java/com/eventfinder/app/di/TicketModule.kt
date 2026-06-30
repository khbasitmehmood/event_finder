package com.eventfinder.app.di

import com.eventfinder.app.data.payment.CloudflareWorkerPaymentGateway
import com.eventfinder.app.data.repository.TicketRepositoryImpl
import com.eventfinder.app.data.source.FirestoreTicketDataSource
import com.eventfinder.app.data.source.TicketDataSource
import com.eventfinder.app.domain.repository.PaymentGateway
import com.eventfinder.app.domain.repository.TicketRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for Ticket-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object TicketModule {

    @Provides
    @Singleton
    fun provideTicketDataSource(
        firestore: FirebaseFirestore
    ): TicketDataSource {
        return FirestoreTicketDataSource(firestore)
    }

    @Provides
    @Singleton
    fun provideTicketRepository(
        ticketDataSource: TicketDataSource
    ): TicketRepository {
        return TicketRepositoryImpl(ticketDataSource)
    }

    @Provides
    @Singleton
    fun providePaymentGateway(
        cloudflareWorkerPaymentGateway: CloudflareWorkerPaymentGateway
    ): PaymentGateway {
        return cloudflareWorkerPaymentGateway
    }
}
