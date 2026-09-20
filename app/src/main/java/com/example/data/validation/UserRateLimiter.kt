package com.example.data.validation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.ceil

/**
 * Enterprise Client-Side Rate Limiter for Velorix Esports.
 *
 * Protects critical user-side interactions from:
 * 1. Rapid multi-click & race conditions (duplicate tournament registrations, double wallet debits)
 * 2. Brute-force authentication attacks with progressive exponential lockouts
 * 3. SMS OTP quota exhaustion & carrier fees (enforces 60s carrier cooldown + max attempts per window)
 * 4. Password reset & Magic Link email flooding
 * 5. Gemini AI 429 quota depletion (enforces sliding window per minute + inter-request debounce)
 * 6. Support ticket flooding / spamming the admin adjudication queue
 * 7. Room credential broadcast storming
 */
object UserRateLimiter {

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var sharedPreferences: android.content.SharedPreferences? = null

    fun initialize(context: android.content.Context) {
        try {
            sharedPreferences = context.getSharedPreferences("velorix_rate_limiter_prefs", android.content.Context.MODE_PRIVATE)
            restorePersistentLockouts()
        } catch (e: Exception) {
            android.util.Log.e("UserRateLimiter", "Init error: ${e.message}")
        }
    }

    enum class ActionType(
        val minIntervalMs: Long,
        val maxAttemptsInWindow: Int,
        val windowDurationMs: Long,
        val defaultLockoutMs: Long,
        val actionDisplayName: String
    ) {
        PHONE_OTP_REQUEST(
            minIntervalMs = 60_000L,
            maxAttemptsInWindow = 4,
            windowDurationMs = 10 * 60_000L,
            defaultLockoutMs = 5 * 60_000L,
            actionDisplayName = "SMS Verification Code"
        ),
        MAGIC_LINK_REQUEST(
            minIntervalMs = 60_000L,
            maxAttemptsInWindow = 3,
            windowDurationMs = 10 * 60_000L,
            defaultLockoutMs = 3 * 60_000L,
            actionDisplayName = "Magic Login Link"
        ),
        PASSWORD_RESET_REQUEST(
            minIntervalMs = 60_000L,
            maxAttemptsInWindow = 3,
            windowDurationMs = 10 * 60_000L,
            defaultLockoutMs = 3 * 60_000L,
            actionDisplayName = "Password Reset Email"
        ),
        AUTH_LOGIN_ATTEMPT(
            minIntervalMs = 1_000L,
            maxAttemptsInWindow = 5,
            windowDurationMs = 2 * 60_000L,
            defaultLockoutMs = 30_000L,
            actionDisplayName = "Account Login"
        ),
        TOURNAMENT_JOIN(
            minIntervalMs = 5_000L,
            maxAttemptsInWindow = 3,
            windowDurationMs = 30_000L,
            defaultLockoutMs = 15_000L,
            actionDisplayName = "Tournament Registration"
        ),
        WALLET_TRANSACTION(
            minIntervalMs = 4_000L,
            maxAttemptsInWindow = 4,
            windowDurationMs = 60_000L,
            defaultLockoutMs = 20_000L,
            actionDisplayName = "Wallet Operation"
        ),
        GEMINI_CHAT_MESSAGE(
            minIntervalMs = 3_000L,
            maxAttemptsInWindow = 10,
            windowDurationMs = 60_000L,
            defaultLockoutMs = 25_000L,
            actionDisplayName = "Gemini AI Query"
        ),
        SUPPORT_TICKET_SUBMISSION(
            minIntervalMs = 15_000L,
            maxAttemptsInWindow = 3,
            windowDurationMs = 5 * 60_000L,
            defaultLockoutMs = 60_000L,
            actionDisplayName = "Support Ticket Submission"
        ),
        BROADCAST_ANNOUNCEMENT(
            minIntervalMs = 8_000L,
            maxAttemptsInWindow = 5,
            windowDurationMs = 60_000L,
            defaultLockoutMs = 30_000L,
            actionDisplayName = "Broadcast Announcement"
        ),
        ROOM_CREDENTIALS_PUBLISH(
            minIntervalMs = 10_000L,
            maxAttemptsInWindow = 4,
            windowDurationMs = 60_000L,
            defaultLockoutMs = 30_000L,
            actionDisplayName = "Room Credentials Release"
        ),
        USER_PROFILE_UPDATE(
            minIntervalMs = 5_000L,
            maxAttemptsInWindow = 4,
            windowDurationMs = 60_000L,
            defaultLockoutMs = 30_000L,
            actionDisplayName = "Profile Update"
        ),
        WITHDRAWAL_REQUEST(
            minIntervalMs = 30_000L,
            maxAttemptsInWindow = 2,
            windowDurationMs = 5 * 60_000L,
            defaultLockoutMs = 10 * 60_000L,
            actionDisplayName = "Withdrawal Request"
        ),
        DEPOSIT_PROOF_SUBMIT(
            minIntervalMs = 20_000L,
            maxAttemptsInWindow = 3,
            windowDurationMs = 5 * 60_000L,
            defaultLockoutMs = 5 * 60_000L,
            actionDisplayName = "Deposit Proof Submission"
        ),
        SLOT_RESERVATION(
            minIntervalMs = 3_000L,
            maxAttemptsInWindow = 5,
            windowDurationMs = 30_000L,
            defaultLockoutMs = 15_000L,
            actionDisplayName = "Slot Reservation"
        ),
        TOKEN_REDEEM_ATTEMPT(
            minIntervalMs = 3_000L,
            maxAttemptsInWindow = 5,
            windowDurationMs = 2 * 60_000L,
            defaultLockoutMs = 5 * 60_000L,
            actionDisplayName = "Token Verification"
        ),
        MATCH_RESULT_REPORT(
            minIntervalMs = 15_000L,
            maxAttemptsInWindow = 3,
            windowDurationMs = 5 * 60_000L,
            defaultLockoutMs = 60_000L,
            actionDisplayName = "Match Result Report"
        ),
        SEARCH_QUERY(
            minIntervalMs = 300L,
            maxAttemptsInWindow = 30,
            windowDurationMs = 60_000L,
            defaultLockoutMs = 10_000L,
            actionDisplayName = "Search Query"
        )
    }

    data class RateLimitResult(
        val isAllowed: Boolean,
        val remainingCooldownSeconds: Long = 0L,
        val remainingAttempts: Int = 0,
        val reasonMessage: String = ""
    )

    // Store state per action key (actionName + ":" + optionalIdentifier)
    private class Tracker {
        var lastExecutionMs: Long = 0L
        val executionTimestamps = mutableListOf<Long>()
        var lockoutUntilMs: Long = 0L
        var failureStreak: AtomicInteger = AtomicInteger(0)
        var activeCountdownFlow = MutableStateFlow(0L)
        var countdownJob: Job? = null
    }

    private val trackers = ConcurrentHashMap<String, Tracker>()

    private fun getTrackerKey(action: ActionType, identifier: String): String {
        val cleanId = identifier.trim().lowercase()
        return "${action.name}:$cleanId"
    }

    private fun getOrCreateTracker(action: ActionType, identifier: String): Tracker {
        val key = getTrackerKey(action, identifier)
        return trackers.computeIfAbsent(key) {
            val tracker = Tracker()
            sharedPreferences?.let { prefs ->
                val savedLockout = prefs.getLong("${key}_lockout", 0L)
                val now = System.currentTimeMillis()
                if (savedLockout > now) {
                    tracker.lockoutUntilMs = savedLockout
                    val remainingSec = ceil((savedLockout - now) / 1000.0).toLong()
                    startCountdown(tracker, remainingSec)
                }
                tracker.lastExecutionMs = prefs.getLong("${key}_last", 0L)
            }
            tracker
        }
    }

    private fun restorePersistentLockouts() {
        val prefs = sharedPreferences ?: return
        val allEntries = prefs.all
        val now = System.currentTimeMillis()
        for ((key, value) in allEntries) {
            if (key.endsWith("_lockout") && value is Long && value > now) {
                val trackerKey = key.removeSuffix("_lockout")
                val parts = trackerKey.split(":")
                val actionName = parts.getOrNull(0) ?: continue
                val id = parts.getOrNull(1) ?: ""
                try {
                    val action = ActionType.valueOf(actionName)
                    val tracker = getOrCreateTracker(action, id)
                    tracker.lockoutUntilMs = value
                    val remainingSec = ceil((value - now) / 1000.0).toLong()
                    startCountdown(tracker, remainingSec)
                } catch (_: Exception) {}
            }
        }
    }

    private fun persistLockout(key: String, lockoutUntilMs: Long, lastExecutionMs: Long = 0L) {
        sharedPreferences?.edit()?.apply {
            if (lockoutUntilMs > 0L) {
                putLong("${key}_lockout", lockoutUntilMs)
            } else {
                remove("${key}_lockout")
            }
            if (lastExecutionMs > 0L) {
                putLong("${key}_last", lastExecutionMs)
            }
            apply()
        }
    }

    private fun syncRateLimitToFirebase(action: ActionType, identifier: String, lockoutUntilMs: Long, reason: String) {
        val cleanId = identifier.trim().replace(".", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_")
        if (cleanId.isNotBlank()) {
            scope.launch {
                try {
                    val database = com.google.firebase.database.FirebaseDatabase.getInstance("https://velorix-tournaments-default-rtdb.asia-southeast1.firebasedatabase.app").reference
                    val payload = mapOf<String, Any>(
                        "action" to action.name,
                        "actionName" to action.actionDisplayName,
                        "identifier" to cleanId,
                        "lockoutUntil" to lockoutUntilMs,
                        "reason" to reason,
                        "updatedAt" to System.currentTimeMillis(),
                        "isLocked" to (lockoutUntilMs > System.currentTimeMillis())
                    )
                    database.child("rate_limits").child(cleanId).child(action.name).setValue(payload)
                } catch (_: Exception) {}
            }
        }
    }

    private fun clearRateLimitInFirebase(identifier: String, actionName: String? = null) {
        val cleanId = identifier.trim().replace(".", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_")
        if (cleanId.isNotBlank()) {
            scope.launch {
                try {
                    val database = com.google.firebase.database.FirebaseDatabase.getInstance("https://velorix-tournaments-default-rtdb.asia-southeast1.firebasedatabase.app").reference
                    if (actionName != null) {
                        database.child("rate_limits").child(cleanId).child(actionName).removeValue()
                    } else {
                        database.child("rate_limits").child(cleanId).removeValue()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Checks whether an action can be performed right now without recording it yet.
     */
    fun canExecute(action: ActionType, identifier: String = ""): RateLimitResult {
        val now = System.currentTimeMillis()
        val tracker = getOrCreateTracker(action, identifier)

        synchronized(tracker) {
            // 1. Check if lockout is currently active
            if (now < tracker.lockoutUntilMs) {
                val remainingSec = ceil((tracker.lockoutUntilMs - now) / 1000.0).toLong().coerceAtLeast(1L)
                return RateLimitResult(
                    isAllowed = false,
                    remainingCooldownSeconds = remainingSec,
                    remainingAttempts = 0,
                    reasonMessage = "Security cooldown active for ${action.actionDisplayName}. Please wait ${remainingSec}s before retrying."
                )
            }

            // 2. Check minimum interval between requests
            val elapsedSinceLast = now - tracker.lastExecutionMs
            if (tracker.lastExecutionMs > 0L && elapsedSinceLast < action.minIntervalMs) {
                val remainingSec = ceil((action.minIntervalMs - elapsedSinceLast) / 1000.0).toLong().coerceAtLeast(1L)
                return RateLimitResult(
                    isAllowed = false,
                    remainingCooldownSeconds = remainingSec,
                    remainingAttempts = 0,
                    reasonMessage = "Please wait ${remainingSec}s before requesting another ${action.actionDisplayName}."
                )
            }

            // 3. Prune old timestamps outside the sliding window
            val windowStart = now - action.windowDurationMs
            tracker.executionTimestamps.removeAll { it < windowStart }

            // 4. Check attempts within window
            if (tracker.executionTimestamps.size >= action.maxAttemptsInWindow) {
                val lockoutMs = action.defaultLockoutMs
                tracker.lockoutUntilMs = now + lockoutMs
                val remainingSec = ceil(lockoutMs / 1000.0).toLong()
                startCountdown(tracker, remainingSec)
                val key = getTrackerKey(action, identifier)
                persistLockout(key, tracker.lockoutUntilMs, tracker.lastExecutionMs)
                syncRateLimitToFirebase(action, identifier, tracker.lockoutUntilMs, "Exceeded ${action.maxAttemptsInWindow} attempts in window")
                return RateLimitResult(
                    isAllowed = false,
                    remainingCooldownSeconds = remainingSec,
                    remainingAttempts = 0,
                    reasonMessage = "Rate limit reached for ${action.actionDisplayName} (${action.maxAttemptsInWindow} allowed per ${action.windowDurationMs / 60000}m). Try again in ${remainingSec}s."
                )
            }

            val remainingQuota = (action.maxAttemptsInWindow - tracker.executionTimestamps.size).coerceAtLeast(0)
            return RateLimitResult(
                isAllowed = true,
                remainingCooldownSeconds = 0L,
                remainingAttempts = remainingQuota,
                reasonMessage = ""
            )
        }
    }

    /**
     * Checks rate limit and atomically records the execution if allowed.
     */
    fun checkAndRecord(action: ActionType, identifier: String = ""): RateLimitResult {
        val check = canExecute(action, identifier)
        if (!check.isAllowed) {
            return check
        }

        val now = System.currentTimeMillis()
        val tracker = getOrCreateTracker(action, identifier)

        synchronized(tracker) {
            tracker.lastExecutionMs = now
            tracker.executionTimestamps.add(now)

            val key = getTrackerKey(action, identifier)
            persistLockout(key, tracker.lockoutUntilMs, tracker.lastExecutionMs)

            // Start countdown timer for the minimum interval cooldown
            val cooldownSec = ceil(action.minIntervalMs / 1000.0).toLong()
            if (cooldownSec > 0) {
                startCountdown(tracker, cooldownSec)
            }

            val remainingQuota = (action.maxAttemptsInWindow - tracker.executionTimestamps.size).coerceAtLeast(0)
            return RateLimitResult(
                isAllowed = true,
                remainingCooldownSeconds = 0L,
                remainingAttempts = remainingQuota,
                reasonMessage = ""
            )
        }
    }

    /**
     * Records a failed attempt (e.g., incorrect password), which can trigger exponential lockout.
     */
    fun recordFailure(action: ActionType, identifier: String = ""): RateLimitResult {
        val now = System.currentTimeMillis()
        val tracker = getOrCreateTracker(action, identifier)

        synchronized(tracker) {
            val streak = tracker.failureStreak.incrementAndGet()
            if (streak >= action.maxAttemptsInWindow) {
                // Progressive exponential lockout: 30s -> 60s -> 120s -> max 300s
                val multiplier = (1 shl ((streak - action.maxAttemptsInWindow).coerceIn(0, 3)))
                val lockoutMs = (action.defaultLockoutMs * multiplier).coerceAtMost(300_000L)
                tracker.lockoutUntilMs = now + lockoutMs
                val lockoutSec = ceil(lockoutMs / 1000.0).toLong()
                startCountdown(tracker, lockoutSec)

                val key = getTrackerKey(action, identifier)
                persistLockout(key, tracker.lockoutUntilMs, tracker.lastExecutionMs)
                syncRateLimitToFirebase(action, identifier, tracker.lockoutUntilMs, "Too many failed attempts ($streak)")

                return RateLimitResult(
                    isAllowed = false,
                    remainingCooldownSeconds = lockoutSec,
                    remainingAttempts = 0,
                    reasonMessage = "Too many failed ${action.actionDisplayName} attempts ($streak failures). Security lockout active: wait ${lockoutSec}s."
                )
            }

            val attemptsLeft = (action.maxAttemptsInWindow - streak).coerceAtLeast(0)
            return RateLimitResult(
                isAllowed = true,
                remainingCooldownSeconds = 0L,
                remainingAttempts = attemptsLeft,
                reasonMessage = if (attemptsLeft <= 2) "Warning: $attemptsLeft attempts remaining before security lockout." else ""
            )
        }
    }

    /**
     * Resets failure counter and lockout for a successful login or verification.
     */
    fun recordSuccess(action: ActionType, identifier: String = "") {
        val tracker = getOrCreateTracker(action, identifier)
        synchronized(tracker) {
            tracker.failureStreak.set(0)
            tracker.lockoutUntilMs = 0L
            val key = getTrackerKey(action, identifier)
            persistLockout(key, 0L, tracker.lastExecutionMs)
            clearRateLimitInFirebase(identifier, action.name)
        }
    }

    /**
     * Resets all rate limits and active lockouts for a specific user ID or identifier.
     * Useful for admin unblocking / support resolution.
     */
    fun resetUserLimits(identifier: String): Int {
        val cleanId = identifier.trim().lowercase()
        var count = 0
        trackers.forEach { (key, tracker) ->
            if (key.endsWith(":$cleanId") || key.contains(":$cleanId")) {
                synchronized(tracker) {
                    tracker.lockoutUntilMs = 0L
                    tracker.failureStreak.set(0)
                    tracker.executionTimestamps.clear()
                    tracker.activeCountdownFlow.value = 0L
                    tracker.countdownJob?.cancel()
                }
                persistLockout(key, 0L, 0L)
                count++
            }
        }
        clearRateLimitInFirebase(identifier)
        return count
    }

    /**
     * Checks if a user or identifier is currently locked out by any action.
     */
    fun isUserLockedOut(identifier: String): Boolean {
        val cleanId = identifier.trim().lowercase()
        val now = System.currentTimeMillis()
        trackers.forEach { (key, tracker) ->
            if (key.endsWith(":$cleanId") || key.contains(":$cleanId")) {
                if (now < tracker.lockoutUntilMs) return true
            }
        }
        return false
    }

    /**
     * Gets summary string of current active lockout for a user.
     */
    fun getActiveLockoutSummary(identifier: String): String? {
        val cleanId = identifier.trim().lowercase()
        val now = System.currentTimeMillis()
        trackers.forEach { (key, tracker) ->
            if (key.endsWith(":$cleanId") || key.contains(":$cleanId")) {
                if (now < tracker.lockoutUntilMs) {
                    val remainingSec = ceil((tracker.lockoutUntilMs - now) / 1000.0).toLong()
                    val actionName = key.substringBefore(":")
                    return "$actionName: ${remainingSec}s"
                }
            }
        }
        return null
    }

    /**
     * Returns a live StateFlow for observing remaining cooldown in seconds for UI countdown displays.
     */
    fun observeCooldownSeconds(action: ActionType, identifier: String = ""): StateFlow<Long> {
        val tracker = getOrCreateTracker(action, identifier)
        return tracker.activeCountdownFlow.asStateFlow()
    }

    /**
     * Returns the current remaining cooldown seconds.
     */
    fun getRemainingCooldownSeconds(action: ActionType, identifier: String = ""): Long {
        val now = System.currentTimeMillis()
        val tracker = getOrCreateTracker(action, identifier)
        synchronized(tracker) {
            if (now < tracker.lockoutUntilMs) {
                return ceil((tracker.lockoutUntilMs - now) / 1000.0).toLong().coerceAtLeast(0L)
            }
            val elapsed = now - tracker.lastExecutionMs
            if (tracker.lastExecutionMs > 0L && elapsed < action.minIntervalMs) {
                return ceil((action.minIntervalMs - elapsed) / 1000.0).toLong().coerceAtLeast(0L)
            }
            return 0L
        }
    }

    /**
     * Starts a 1-second interval countdown coroutine to drive real-time UI text/buttons.
     */
    private fun startCountdown(tracker: Tracker, initialSeconds: Long) {
        tracker.countdownJob?.cancel()
        tracker.activeCountdownFlow.value = initialSeconds
        tracker.countdownJob = scope.launch {
            var remaining = initialSeconds
            while (remaining > 0) {
                tracker.activeCountdownFlow.value = remaining
                delay(1000L)
                remaining--
            }
            tracker.activeCountdownFlow.value = 0L
        }
    }

    /**
     * Clears all tracking state (e.g. on logout or user switch).
     */
    fun clearAll() {
        trackers.values.forEach { it.countdownJob?.cancel() }
        trackers.clear()
    }
}
