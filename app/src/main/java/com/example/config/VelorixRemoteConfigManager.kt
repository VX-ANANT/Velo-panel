package com.example.config

import android.util.Log
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Data representation of dynamic parameters managed by Firebase Remote Config & A/B Testing.
 */
data class RemoteConfigState(
    val isMaintenanceMode: Boolean = false,
    val maintenanceMessage: String = "Velorix Tournaments is currently undergoing maintenance. Please check back shortly!",
    val minAppVersion: Long = 1,
    val isVipExperienceEnabled: Boolean = true,
    val isFeaturedBannerEnabled: Boolean = true,
    val ctaButtonText: String = "Join Tournament", // A/B test variant: "Join Tournament" vs "Register Now"
    val notificationCopyVariant: String = "Are you ready for battle? Tournament starting soon!",
    val matchReminderLeadMinutes: Long = 15,
    val maxSlotsPerTournament: Long = 48,
    val freeFireAnnouncementTicker: String = "Fair Play Esports • Instant UPI/Bank Withdrawals • Anti-Cheat Verification Active",
    val isConfigFetched: Boolean = false,
    val lastFetchStatus: String = "Initialized"
)

object VelorixRemoteConfigManager {
    private const val TAG = "VelorixRemoteConfig"

    private val _configState = MutableStateFlow(RemoteConfigState())
    val configState: StateFlow<RemoteConfigState> = _configState.asStateFlow()

    private var remoteConfig: FirebaseRemoteConfig? = null

    /**
     * Initializes Firebase Remote Config with sensible defaults and registers real-time update listeners.
     */
    fun initialize() {
        try {
            val config = Firebase.remoteConfig
            remoteConfig = config

            val configSettings = remoteConfigSettings {
                // Minimum fetch interval 0 allows instant updates in development & testing
                minimumFetchIntervalInSeconds = 0
            }
            config.setConfigSettingsAsync(configSettings)

            // Define in-code defaults for immediate offline & startup usability
            val defaultValues: Map<String, Any> = mapOf(
                "is_maintenance_mode" to false,
                "maintenance_message" to "Velorix Tournaments is currently undergoing maintenance. Please check back shortly!",
                "min_app_version" to 1L,
                "vip_experience_enabled" to true,
                "featured_banner_enabled" to true,
                "cta_button_text" to "Join Tournament",
                "notification_copy_variant" to "Are you ready for battle? Tournament starting soon!",
                "match_reminder_lead_minutes" to 15L,
                "max_slots_per_tournament" to 48L,
                "announcement_ticker" to "Fair Play Esports • Instant UPI/Bank Withdrawals • Anti-Cheat Verification Active"
            )
            config.setDefaultsAsync(defaultValues)

            // Update internal state from cache/defaults
            updateStateFromConfig(config, "Defaults Applied")

            // Realtime Remote Config update listener (Firebase Remote Config SDK 21.3+)
            try {
                config.addOnConfigUpdateListener(object : ConfigUpdateListener {
                    override fun onUpdate(configUpdate: ConfigUpdate) {
                        Log.d(TAG, "Realtime Remote Config update detected: ${configUpdate.updatedKeys}")
                        config.activate().addOnCompleteListener {
                            updateStateFromConfig(config, "Live Remote Config Updated")
                        }
                    }

                    override fun onError(error: FirebaseRemoteConfigException) {
                        Log.w(TAG, "Config update listener error: ${error.message}")
                    }
                })
            } catch (e: Throwable) {
                Log.d(TAG, "Config update listener setup note: ${e.message}")
            }

            // Perform initial fetch and activate asynchronously
            fetchAndActivate()
            Log.d(TAG, "Velorix Remote Config initialized.")
        } catch (e: Exception) {
            Log.e(TAG, "Remote config init error: ${e.message}", e)
        }
    }

    /**
     * Triggers fetch and activate against Firebase Remote Config servers.
     */
    fun fetchAndActivate(scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {
        val config = remoteConfig ?: return
        scope.launch {
            try {
                val fetchSuccess = config.fetchAndActivate().await()
                updateStateFromConfig(config, if (fetchSuccess) "Fetched & Activated" else "Cached Config Active")
                Log.d(TAG, "Remote config fetch finished. fetchSuccess=$fetchSuccess, state=${_configState.value}")
            } catch (e: Exception) {
                Log.w(TAG, "Remote config fetch failed: ${e.message}")
                _configState.value = _configState.value.copy(
                    lastFetchStatus = "Fetch failed: ${e.localizedMessage ?: e.message}"
                )
            }
        }
    }

    private fun updateStateFromConfig(config: FirebaseRemoteConfig, status: String) {
        val maintenance = config.getBoolean("is_maintenance_mode")
        val message = config.getString("maintenance_message").ifBlank {
            "Velorix Tournaments is currently undergoing maintenance. Please check back shortly!"
        }
        val minVersion = config.getLong("min_app_version")
        val vipEnabled = config.getBoolean("vip_experience_enabled")
        val featuredBanner = config.getBoolean("featured_banner_enabled")
        val ctaText = config.getString("cta_button_text").ifBlank { "Join Tournament" }
        val copyVariant = config.getString("notification_copy_variant").ifBlank {
            "Are you ready for battle? Tournament starting soon!"
        }
        val leadMinutes = config.getLong("match_reminder_lead_minutes").let { if (it > 0) it else 15L }
        val maxSlots = config.getLong("max_slots_per_tournament").let { if (it > 0) it else 48L }
        val ticker = config.getString("announcement_ticker").ifBlank {
            "Fair Play Esports • Instant UPI/Bank Withdrawals • Anti-Cheat Verification Active"
        }

        _configState.value = RemoteConfigState(
            isMaintenanceMode = maintenance,
            maintenanceMessage = message,
            minAppVersion = if (minVersion > 0) minVersion else 1L,
            isVipExperienceEnabled = vipEnabled,
            isFeaturedBannerEnabled = featuredBanner,
            ctaButtonText = ctaText,
            notificationCopyVariant = copyVariant,
            matchReminderLeadMinutes = leadMinutes,
            maxSlotsPerTournament = maxSlots,
            freeFireAnnouncementTicker = ticker,
            isConfigFetched = true,
            lastFetchStatus = status
        )
    }
}
