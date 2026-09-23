package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

/**
 * ApiKeyManager:
 * Manages user-configured and environment-injected Gemini API keys.
 * Allows admins to input their personal Gemini API key directly in the app
 * to ensure 100% live neural network AI responses from Google AI.
 */
object ApiKeyManager {
    private const val PREFS_NAME = "velorix_api_keys"
    private const val KEY_CUSTOM_GEMINI = "custom_gemini_api_key"

    private var sharedPrefs: SharedPreferences? = null

    fun initialize(context: Context) {
        if (sharedPrefs == null) {
            sharedPrefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun getActiveApiKey(): String {
        val customKey = sharedPrefs?.getString(KEY_CUSTOM_GEMINI, "")?.trim() ?: ""
        if (customKey.isNotBlank()) {
            return customKey
        }
        val buildKey = BuildConfig.GEMINI_API_KEY.trim()
        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") {
            return buildKey
        }
        return ""
    }

    fun getCustomApiKey(): String {
        return sharedPrefs?.getString(KEY_CUSTOM_GEMINI, "")?.trim() ?: ""
    }

    fun setCustomApiKey(key: String) {
        sharedPrefs?.edit()?.putString(KEY_CUSTOM_GEMINI, key.trim())?.apply()
    }

    fun clearCustomApiKey() {
        sharedPrefs?.edit()?.remove(KEY_CUSTOM_GEMINI)?.apply()
    }

    fun hasValidCustomKey(): Boolean {
        return getCustomApiKey().isNotBlank()
    }
}
