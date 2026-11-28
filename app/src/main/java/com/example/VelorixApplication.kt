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

        // Clear any stale FCM topic sync queue from previous installs so FirebaseMessaging
        // never triggers unnecessary FirebaseInstanceId topic syncs on the Admin Panel
        try {
            val appidPrefs = getSharedPreferences("com.google.android.gms.appid", MODE_PRIVATE)
            if (appidPrefs.contains("topic_operation_queue")) {
                appidPrefs.edit().remove("topic_operation_queue").apply()
            }
        } catch (_: Exception) {}

        // Initialize Firebase Core, Crashlytics, RTDB 50MB disk persistence, and Firestore 100MB cache first
        try {
            com.example.firebase.VelorixFirebaseSuite.initializeCore(this)
        } catch (e: Exception) {
            Log.e("VelorixApp", "Firebase core suite initialization error: ${e.message}", e)
        }

        // Set global uncaught exception handler to record fatal crashes in Crashlytics and prevent silent failures
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("VelorixApp", "Uncaught exception on thread ${thread.name}: ${throwable.message}", throwable)
            try {
                com.example.firebase.VelorixFirebaseSuite.recordNonFatal(
                    tag = "UncaughtException",
                    throwable = throwable,
                    metadata = mapOf("thread_name" to thread.name),
                    isFatal = true
                )
                GlobalErrorManager.emitError("Unexpected error occurred: ${throwable.localizedMessage ?: throwable.message}")
            } catch (_: Exception) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // Asynchronously initialize secondary background services to prevent blocking the main UI thread during cold start
        CoroutineScope(Dispatchers.IO).launch {
            try {
                com.example.firebase.VelorixFirebaseSuite.initializeSecondaryServices(this@VelorixApplication)
                com.example.data.ai.FirebaseAiManager.initializeAppCheck(this@VelorixApplication)
                com.example.analytics.VelorixAnalytics.initialize(this@VelorixApplication)
                com.example.notification.VelorixNotificationManager.initChannels(this@VelorixApplication)
                com.example.config.VelorixRemoteConfigManager.initialize()
                com.example.notification.VelorixFcmManager.initialize(this@VelorixApplication)
                com.example.data.validation.UserRateLimiter.initialize(this@VelorixApplication)
                com.example.data.repository.ApiKeyManager.initialize(this@VelorixApplication)
            } catch (e: Exception) {
                Log.e("VelorixApp", "Async background services init error: ${e.message}", e)
                com.example.firebase.VelorixFirebaseSuite.recordNonFatal("AsyncInitError", e)
            }
        }

        appContainer = AppContainer(this)
    }
}
