package com.example

import android.app.Application
import android.util.Log
import com.example.ui.common.GlobalErrorManager
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        } catch (e: Exception) {
            Log.e("VelorixApp", "Firebase core initialization error: ${e.message}", e)
        }

        // Asynchronously initialize secondary background services to prevent blocking the main UI thread during cold start
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                com.example.data.ai.FirebaseAiManager.initializeAppCheck(this@VelorixApplication)
                com.example.analytics.VelorixAnalytics.initialize(this@VelorixApplication)
                com.example.notification.VelorixNotificationManager.initChannels(this@VelorixApplication)
                com.example.config.VelorixRemoteConfigManager.initialize()
                com.example.notification.VelorixFcmManager.initialize(this@VelorixApplication)
                com.example.data.validation.UserRateLimiter.initialize(this@VelorixApplication)
                com.example.data.repository.ApiKeyManager.initialize(this@VelorixApplication)
            } catch (e: Exception) {
                Log.e("VelorixApp", "Async background services init error: ${e.message}", e)
            }
        }

        appContainer = AppContainer(this)
    }
}
