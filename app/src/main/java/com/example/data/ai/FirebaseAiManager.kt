package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * FirebaseAiManager: Integrates Firebase AI Logic SDK with Google AI Gemini backend
 * and Firebase App Check protection.
 */
object FirebaseAiManager {
    private const val TAG = "VelorixFirebaseAI"

    @Volatile
    private var isAppCheckConfigured = false

    /**
     * Initializes Firebase App Check.
     * Only installs DebugAppCheckProviderFactory if an explicit token is configured,
     * preventing un-whitelisted random debug tokens from causing "Firebase App Check token is invalid".
     */
    fun initializeAppCheck(context: Context) {
        if (isAppCheckConfigured) return
        try {
            val appCheck = FirebaseAppCheck.getInstance()
            val sharedPrefs = context.getSharedPreferences("com.google.firebase.appcheck.debug.store", Context.MODE_PRIVATE)
            val hasExplicitToken = sharedPrefs.contains("DebugSecret")

            if (hasExplicitToken) {
                appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
                isAppCheckConfigured = true
                Log.d(TAG, "Firebase App Check installed with configured DebugAppCheckProviderFactory.")
            } else {
                Log.d(TAG, "Firebase App Check running in standard mode without un-whitelisted debug tokens.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "App Check initialization notice: ${e.message}")
        }
    }

    /**
     * Gets a GenerativeModel configured with the Google AI Gemini backend via Firebase AI.
     */
    fun getGenerativeModel(modelName: String = "gemini-2.5-flash"): GenerativeModel {
        val mappedModel = when {
            modelName.contains("3.8") || modelName.contains("3.5") || modelName.contains("3.6") || modelName.contains("3.7") -> "gemini-2.5-flash"
            modelName.contains("pro") -> "gemini-2.5-pro"
            else -> modelName.ifBlank { "gemini-2.5-flash" }
        }
        return Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(mappedModel)
    }

    /**
     * Generates text content from a text prompt using Firebase AI Logic.
     */
    suspend fun generateText(
        prompt: String,
        modelName: String = "gemini-2.5-flash"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val model = getGenerativeModel(modelName)
            val response = model.generateContent(prompt)
            val resultText = response.text
            if (!resultText.isNullOrBlank()) {
                Result.success(resultText)
            } else {
                Result.failure(IllegalStateException("No text response received from Firebase AI model."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI generateText error with model $modelName: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Generates text analysis from an image (e.g. Free Fire match result screenshot, dispute proof)
     * and a prompt using Firebase AI multimodal capabilities.
     */
    suspend fun analyzeImageWithPrompt(
        bitmap: Bitmap,
        prompt: String,
        modelName: String = "gemini-2.5-flash"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val model = getGenerativeModel(modelName)
            val userContent = content {
                image(bitmap)
                text(prompt)
            }
            val response = model.generateContent(userContent)
            val resultText = response.text
            if (!resultText.isNullOrBlank()) {
                Result.success(resultText)
            } else {
                Result.failure(IllegalStateException("No text returned by Firebase AI vision analysis."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI image analysis error: ${e.message}", e)
            Result.failure(e)
        }
    }
}
