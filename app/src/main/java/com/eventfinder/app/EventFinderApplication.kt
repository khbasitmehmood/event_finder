package com.eventfinder.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.eventfinder.app.utils.UserPreferences
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

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreate() {
        super.onCreate()

        applySavedTheme()

        // Initialize WorkManager with background jobs
        WorkManagerInitializer.initialize(this)
    }

    private fun applySavedTheme() {
        val nightMode = when (userPreferences.getThemeMode()) {
            UserPreferences.THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            UserPreferences.THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
