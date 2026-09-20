package com.example

import android.app.Application
import android.util.Log
import com.example.ui.common.GlobalErrorManager
import com.google.firebase.FirebaseApp

class VelorixApplication : Application() {
    lateinit var appContainer: AppContainer

    override fun onCreate() {
        super.onCreate()

        // Set global uncaught exception handler to prevent hard crashes
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("VelorixApp", "Uncaught exception on thread ${thread.name}: ${throwable.message}", throwable)
            try {
                GlobalErrorManager.emitError("Unexpected error occurred: ${throwable.localizedMessage ?: throwable.message}")
            } catch (_: Exception) {}
            // Fallback to default handler only if necessary
            defaultHandler?.uncaughtException(thread, throwable)
        }

        try {
            FirebaseApp.initializeApp(this)
            com.example.analytics.VelorixAnalytics.initialize(this)
            com.example.notification.VelorixNotificationManager.initChannels(this)
            com.example.config.VelorixRemoteConfigManager.initialize()
            com.example.notification.VelorixFcmManager.initialize(this)
            com.example.data.validation.UserRateLimiter.initialize(this)
        } catch (e: Exception) {
            Log.e("VelorixApp", "Firebase initialization error: ${e.message}", e)
        }

        appContainer = AppContainer(this)
    }
}
