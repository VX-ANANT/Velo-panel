package com.example.data.auth

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DeviceInfo(
    val deviceName: String,
    val model: String,
    val manufacturer: String,
    val androidVersion: String,
    val deviceId: String
)

data class BoundAccount(
    val email: String,
    val username: String,
    val role: String,
    val isAutoLoginEnabled: Boolean,
    val lastLoginTimestamp: Long,
    val lastLoginFormatted: String,
    val boundDeviceId: String,
    val boundDeviceName: String,
    val uid: String = ""
)

class DeviceAccountBindingManager(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDeviceInfo(): DeviceInfo {
        val rawDeviceId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (_: Exception) {
            null
        } ?: "DEV_${Build.FINGERPRINT.hashCode()}"

        val manufacturer = Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        val model = Build.MODEL
        val deviceName = if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"

        return DeviceInfo(
            deviceName = deviceName,
            model = model,
            manufacturer = manufacturer,
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            deviceId = rawDeviceId
        )
    }

    fun bindAccount(
        email: String,
        username: String = "",
        role: String = "player",
        autoLogin: Boolean = true,
        uid: String = ""
    ) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank()) return

        val deviceInfo = getDeviceInfo()
        val displayName = if (username.isNotBlank()) username else cleanEmail.substringBefore("@")
        val now = System.currentTimeMillis()

        // Also record in persistent accounts set
        val existingSet = prefs.getStringSet(KEY_RECORDED_ACCOUNTS_SET, emptySet())?.toMutableSet() ?: mutableSetOf()
        existingSet.removeAll { it.startsWith("$cleanEmail|||", ignoreCase = true) }
        existingSet.add("$cleanEmail|||$displayName|||$role|||$now|||${deviceInfo.deviceId}|||${deviceInfo.deviceName}|||$uid")

        // Prune any mock / dummy entries
        existingSet.removeAll { entry ->
            val lower = entry.lowercase()
            lower.contains("@example.com") || lower.contains("mock.dummy")
        }

        prefs.edit()
            .putString(KEY_BOUND_EMAIL, cleanEmail)
            .putString(KEY_BOUND_USERNAME, displayName)
            .putString(KEY_BOUND_ROLE, role)
            .putBoolean(KEY_AUTO_LOGIN_ENABLED, autoLogin)
            .putLong(KEY_LAST_LOGIN_TIME, now)
            .putString(KEY_BOUND_DEVICE_ID, deviceInfo.deviceId)
            .putString(KEY_BOUND_DEVICE_NAME, deviceInfo.deviceName)
            .putString(KEY_BOUND_UID, uid)
            .putStringSet(KEY_RECORDED_ACCOUNTS_SET, existingSet)
            .apply()
    }

    fun recordAccount(
        email: String,
        username: String = "",
        role: String = "player",
        autoLogin: Boolean = false,
        uid: String = ""
    ) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank()) return

        val deviceInfo = getDeviceInfo()
        val displayName = if (username.isNotBlank()) username else cleanEmail.substringBefore("@")
        val now = System.currentTimeMillis()

        val existingSet = prefs.getStringSet(KEY_RECORDED_ACCOUNTS_SET, emptySet())?.toMutableSet() ?: mutableSetOf()
        existingSet.removeAll { it.startsWith("$cleanEmail|||", ignoreCase = true) }
        existingSet.add("$cleanEmail|||$displayName|||$role|||$now|||${deviceInfo.deviceId}|||${deviceInfo.deviceName}|||$uid")

        // Prune any mock / dummy entries
        existingSet.removeAll { entry ->
            val lower = entry.lowercase()
            lower.contains("@example.com") || lower.contains("mock.dummy")
        }

        val editor = prefs.edit().putStringSet(KEY_RECORDED_ACCOUNTS_SET, existingSet)
        if (autoLogin) {
            editor
                .putString(KEY_BOUND_EMAIL, cleanEmail)
                .putString(KEY_BOUND_USERNAME, displayName)
                .putString(KEY_BOUND_ROLE, role)
                .putBoolean(KEY_AUTO_LOGIN_ENABLED, true)
                .putLong(KEY_LAST_LOGIN_TIME, now)
                .putString(KEY_BOUND_UID, uid)
        }
        editor.apply()
    }

    fun getAllRecordedAccounts(): List<BoundAccount> {
        val list = mutableListOf<BoundAccount>()
        val seenEmails = mutableSetOf<String>()

        // 1. Current bound account
        getBoundAccount()?.let {
            if (it.email.isNotBlank() && !isMockEmail(it.email)) {
                list.add(it)
                seenEmails.add(it.email.lowercase())
            }
        }

        // 2. All accounts stored in recorded set (pruning any mock accounts)
        val storedSet = prefs.getStringSet(KEY_RECORDED_ACCOUNTS_SET, emptySet()) ?: emptySet()
        val sdf = SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault())
        val cleanedSet = mutableSetOf<String>()

        for (item in storedSet) {
            val parts = item.split("|||")
            if (parts.isNotEmpty()) {
                val email = parts[0].trim()
                if (email.isNotBlank() && !isMockEmail(email)) {
                    cleanedSet.add(item)
                    if (!seenEmails.contains(email.lowercase())) {
                        val username = parts.getOrNull(1)?.ifBlank { email.substringBefore("@") } ?: email.substringBefore("@")
                        val role = parts.getOrNull(2)?.ifBlank { "player" } ?: "player"
                        val timestamp = parts.getOrNull(3)?.toLongOrNull() ?: System.currentTimeMillis()
                        val devId = parts.getOrNull(4) ?: getDeviceInfo().deviceId
                        val devName = parts.getOrNull(5) ?: getDeviceInfo().deviceName
                        val parsedUid = parts.getOrNull(6) ?: ""
                        list.add(
                            BoundAccount(
                                email = email,
                                username = username,
                                role = role,
                                isAutoLoginEnabled = false,
                                lastLoginTimestamp = timestamp,
                                lastLoginFormatted = sdf.format(Date(timestamp)),
                                boundDeviceId = devId,
                                boundDeviceName = devName,
                                uid = parsedUid
                            )
                        )
                        seenEmails.add(email.lowercase())
                    }
                }
            }
        }

        if (cleanedSet.size != storedSet.size) {
            prefs.edit().putStringSet(KEY_RECORDED_ACCOUNTS_SET, cleanedSet).apply()
        }

        return list
    }

    private fun isMockEmail(email: String): Boolean {
        val lower = email.lowercase()
        return lower.contains("@example.com") || lower.contains("mock.dummy")
    }

    fun getBoundAccount(): BoundAccount? {
        val email = prefs.getString(KEY_BOUND_EMAIL, null)?.trim() ?: return null
        if (email.isBlank() || isMockEmail(email)) return null

        val username = prefs.getString(KEY_BOUND_USERNAME, email.substringBefore("@")) ?: email.substringBefore("@")
        val role = prefs.getString(KEY_BOUND_ROLE, "player") ?: "player"
        val autoLogin = prefs.getBoolean(KEY_AUTO_LOGIN_ENABLED, true)
        val lastLogin = prefs.getLong(KEY_LAST_LOGIN_TIME, System.currentTimeMillis())
        val deviceId = prefs.getString(KEY_BOUND_DEVICE_ID, getDeviceInfo().deviceId) ?: getDeviceInfo().deviceId
        val deviceName = prefs.getString(KEY_BOUND_DEVICE_NAME, getDeviceInfo().deviceName) ?: getDeviceInfo().deviceName
        val uid = prefs.getString(KEY_BOUND_UID, "") ?: ""

        val sdf = SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault())
        val formatted = sdf.format(Date(lastLogin))

        return BoundAccount(
            email = email,
            username = username,
            role = role,
            isAutoLoginEnabled = autoLogin,
            lastLoginTimestamp = lastLogin,
            lastLoginFormatted = formatted,
            boundDeviceId = deviceId,
            boundDeviceName = deviceName,
            uid = uid
        )
    }

    fun isAutoLoginEligible(): Boolean {
        val account = getBoundAccount() ?: return false
        return account.isAutoLoginEnabled && account.email.isNotBlank()
    }

    fun setAutoLoginEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_LOGIN_ENABLED, enabled).apply()
    }

    fun unbindAccount() {
        prefs.edit()
            .remove(KEY_BOUND_EMAIL)
            .remove(KEY_BOUND_USERNAME)
            .remove(KEY_BOUND_ROLE)
            .remove(KEY_LAST_LOGIN_TIME)
            .remove(KEY_BOUND_UID)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "VelorixDeviceBindingPrefs"
        private const val KEY_BOUND_EMAIL = "key_bound_email"
        private const val KEY_BOUND_USERNAME = "key_bound_username"
        private const val KEY_BOUND_ROLE = "key_bound_role"
        private const val KEY_AUTO_LOGIN_ENABLED = "key_auto_login_enabled"
        private const val KEY_LAST_LOGIN_TIME = "key_last_login_time"
        private const val KEY_BOUND_DEVICE_ID = "key_bound_device_id"
        private const val KEY_BOUND_DEVICE_NAME = "key_bound_device_name"
        private const val KEY_BOUND_UID = "key_bound_uid"
        private const val KEY_RECORDED_ACCOUNTS_SET = "key_recorded_accounts_set"
    }
}
