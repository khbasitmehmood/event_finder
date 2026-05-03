package com.eventfinder.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Configuration for Hilt + WorkManager integration
 * Provides HiltWorkerFactory to WorkManager so workers can use @Inject
 */
@Singleton
class HiltWorkerConfiguration @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workerFactory: HiltWorkerFactory
) {
    /**
     * Initialize WorkManager with Hilt worker factory
     * Call from Application.onCreate() before any WorkManager usage
     */
    fun initialize() {
        val config = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

        WorkManager.initialize(context, config)
    }
}
