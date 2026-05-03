package com.eventfinder.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.eventfinder.app.worker.WorkManagerInitializer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class for Hilt initialization and WorkManager setup
 */
@HiltAndroidApp
class EventFinderApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // Initialize WorkManager with background jobs
        WorkManagerInitializer.initialize(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
