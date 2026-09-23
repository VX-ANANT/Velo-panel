package com.example.data.validation

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
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
import kotlin.math.min

/**
 * Backend-Grade Enterprise Rate Limiter for Velorix Esports.
 *
 * Implements a dual-layer Rate Limiting Engine:
 * 1. Continuous Fractional Token Bucket (Smooth burst capacity + steady refill rate per second/minute)
 * 2. Sliding Window Log (Exact sliding timestamps preventing edge-of-window quota abuse)
 * 3. Server-Synchronized Cloud State (Dual-synced to Firebase RTDB and Cloud Firestore)
 * 4. Progressive Penalty Escalation (Exponential cool-down on continuous violation)
 */
object UserRateLimiter {

    private const val TAG = "UserRateLimiter"
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var sharedPreferences: SharedPreferences? = null

    fun initialize(context: Context) {
        try {
            sharedPreferences = context.getSharedPreferences("velorix_rate_limiter_prefs", Context.MODE_PRIVATE)
            restorePersistentLockouts()
            Log.d(TAG, "UserRateLimiter initialized with backend Token-Bucket engine.")
        } catch (e: Exception) {
            Log.e(TAG, "Init error: ${e.message}")
        }
    }

    /**
     * Rate limit definitions with requests-per-minute quota, burst capacity, and window rules.
     */
    enum class ActionType(
        val requestsPerMinute: Double,
        val burstCapacity: Int,
        val minIntervalMs: Long,
        val maxAttemptsInWindow: Int,
        val windowDurationMs: Long,
        val defaultLockoutMs: Long,
        val actionDisplayName: String,
        val category: RateCategory
    ) {
        // AUTHENTICATION & SENSITIVE GATES
        AUTH_LOGIN_ATTEMPT(
            requestsPerMinute = 6.0,       // 6 attempts / min
            burstCapacity = 3,
            minIntervalMs = 800L,
            maxAttemptsInWindow = 6,
            windowDurationMs = 5 * 60_000L, // 5 minute window
            defaultLockoutMs = 45_000L,
            actionDisplayName = "Account Login",
            category = RateCategory.AUTH
        ),
        AUTH_REGISTER_ATTEMPT(
            requestsPerMinute = 4.0,       // 4 registrations / min
            burstCapacity = 2,
            minIntervalMs = 2_000L,
            maxAttemptsInWindow = 4,
            windowDurationMs = 5 * 60_000L,
            defaultLockoutMs = 60_000L,
            actionDisplayName = "Account Registration",
            category = RateCategory.AUTH
        ),
        PHONE_OTP_REQUEST(
            requestsPerMinute = 1.0,       // 1 per minute max
            burstCapacity = 1,
            minIntervalMs = 60_000L,       // 60s carrier debounce
            maxAttemptsInWindow = 3,
            windowDurationMs = 10 * 60_000L,
            defaultLockoutMs = 5 * 60_000L,
            actionDisplayName = "SMS Verification Code",
            category = RateCategory.AUTH
        ),
        MAGIC_LINK_REQUEST(
            requestsPerMinute = 1.0,
            burstCapacity = 1,
            minIntervalMs = 60_000L,
            maxAttemptsInWindow = 3,
            windowDurationMs = 10 * 60_000L,
            defaultLockoutMs = 3 * 60_000L,
            actionDisplayName = "Magic Login Link",
            category = RateCategory.AUTH
        ),
        PASSWORD_RESET_REQUEST(
            requestsPerMinute = 1.0,
            burstCapacity = 1,
            minIntervalMs = 60_000L,
            maxAttemptsInWindow = 3,
            windowDurationMs = 10 * 60_000L,
            defaultLockoutMs = 3 * 60_000L,
            actionDisplayName = "Password Reset",
            category = RateCategory.AUTH
        ),

        // TOURNAMENT & GAME ENGINE OPERATIONS
        TOURNAMENT_JOIN(
            requestsPerMinute = 6.0,
            burstCapacity = 2,
            minIntervalMs = 3_000L,
            maxAttemptsInWindow = 4,
            windowDurationMs = 60_000L,
            defaultLockoutMs = 20_000L,
            actionDisplayName = "Tournament Registration",
            category = RateCategory.MUTATION
        ),
        SLOT_RESERVATION(
            requestsPerMinute = 10.0,
            burstCapacity = 3,
            minIntervalMs = 2_000L,
            maxAttemptsInWindow = 6,
            windowDurationMs = 60_000L,
            defaultLockoutMs = 15_000L,
            actionDisplayName = "Slot Allocation",
            category = RateCategory.MUTATION
        ),
        ROOM_CREDENTIALS_PUBLISH(
            requestsPerMinute = 6.0,
            burstCapacity = 2,
            minIntervalMs = 5_000L,
            maxAttemptsInWindow = 4,
            windowDurationMs = 60_000L,
            defaultLockoutMs = 30_000L,
            actionDisplayName = "Room Credentials Broadcast",
            category = RateCategory.ADMIN_OPS
        ),
        MATCH_RESULT_REPORT(
            requestsPerMinute = 4.0,
            burstCapacity = 2,
            minIntervalMs = 10_000L,
            maxAttemptsInWindow = 3,
            windowDurationMs = 5 * 60_000L,
            defaultLockoutMs = 60_000L,
            actionDisplayName = "Match Result Audit",
            category = RateCategory.MUTATION
        ),

        // WALLET & FINANCIAL SECURITY
        WALLET_TRANSACTION(
            requestsPerMinute = 5.0,
            burstCapacity = 2,
            minIntervalMs = 3_000L,
            maxAttemptsInWindow = 4,
            windowDurationMs = 60_000L,
            defaultLockoutMs = 25_000L,
            actionDisplayName = "Wallet Operation",
            category = RateCategory.FINANCIAL
        ),
        WITHDRAWAL_REQUEST(
            requestsPerMinute = 2.0,
            burstCapacity = 1,
            minIntervalMs = 20_000L,
            maxAttemptsInWindow = 2,
            windowDurationMs = 5 * 60_000L,
            defaultLockoutMs = 10 * 60_000L,
            actionDisplayName = "Withdrawal Request",
            category = RateCategory.FINANCIAL
        ),
        DEPOSIT_PROOF_SUBMIT(
            requestsPerMinute = 3.0,
            burstCapacity = 1,
            minIntervalMs = 15_000L,
            maxAttemptsInWindow = 3,
            windowDurationMs = 5 * 60_000L,
            defaultLockoutMs = 5 * 60_000L,
            actionDisplayName = "Deposit Proof Submission",
            category = RateCategory.FINANCIAL
        ),

        // AI & REAL-TIME INTELLIGENCE
        GEMINI_CHAT_MESSAGE(
            requestsPerMinute = 15.0,     // 15 AI prompts / min
            burstCapacity = 3,
            minIntervalMs = 2_000L,
            maxAttemptsInWindow = 12,
            windowDurationMs = 60_000L,
            defaultLockoutMs = 25_000L,
            actionDisplayName = "Gemini AI Query",
            category = RateCategory.AI_QUERY
        ),

        // SUPPORT & BROADCAST
        SUPPORT_TICKET_SUBMISSION(
            requestsPerMinute = 3.0,
            burstCapacity = 1,
            minIntervalMs = 10_000L,
            maxAttemptsInWindow = 3,
            windowDurationMs = 5 * 60_000L,
            defaultLockoutMs = 60_000L,
            actionDisplayName = "Support Ticket",
            category = RateCategory.MUTATION
        ),
        BROADCAST_ANNOUNCEMENT(
            requestsPerMinute = 6.0,
            burstCapacity = 2,
            minIntervalMs = 5_000L,
            maxAttemptsInWindow = 5,
            windowDurationMs = 60_000L,
            defaultLockoutMs = 30_000L,
            actionDisplayName = "Global Broadcast",
            category = RateCategory.ADMIN_OPS
        ),
        USER_PROFILE_UPDATE(
            requestsPerMinute = 6.0,
            burstCapacity = 2,
            minIntervalMs = 4_000L,
            maxAttemptsInWindow = 5,
            windowDurationMs = 60_000L,
            defaultLockoutMs = 30_000L,
            actionDisplayName = "Profile Update",
            category = RateCategory.MUTATION
        ),
        TOKEN_REDEEM_ATTEMPT(
            requestsPerMinute = 6.0,
            burstCapacity = 2,
            minIntervalMs = 2_000L,
            maxAttemptsInWindow = 5,
            windowDurationMs = 2 * 60_000L,
            defaultLockoutMs = 5 * 60_000L,
            actionDisplayName = "Token Verification",
            category = RateCategory.MUTATION
        ),
        SEARCH_QUERY(
            requestsPerMinute = 60.0,     // High-throughput query engine: 60/min
            burstCapacity = 10,
            minIntervalMs = 200L,
            maxAttemptsInWindow = 40,
            windowDurationMs = 60_000L,
            defaultLockoutMs = 10_000L,
            actionDisplayName = "Database Query",
            category = RateCategory.READ_QUERY
        )
    }

    enum class RateCategory(val label: String) {
        READ_QUERY("Read / Query"),
        MUTATION("Data Mutation"),
        AUTH("Authentication Gate"),
        FINANCIAL("Financial / Wallet"),
        AI_QUERY("AI Inference Engine"),
        ADMIN_OPS("Admin Command")
    }

    data class RateLimitResult(
        val isAllowed: Boolean,
        val remainingCooldownSeconds: Long = 0L,
        val remainingAttempts: Int = 0,
        val currentTokens: Double = 0.0,
        val maxTokens: Int = 0,
        val reasonMessage: String = ""
    )

    data class RateLimitSnapshot(
        val actionName: String,
        val category: String,
        val requestsPerMinute: Double,
        val availableTokens: Double,
        val burstCapacity: Int,
        val isLocked: Boolean,
        val cooldownSeconds: Long,
        val slidingWindowHits: Int
    )

    /**
     * Internal Tracker managing both Token Bucket and Sliding Window State.
     */
    private class Tracker(val action: ActionType) {
        var lastExecutionMs: Long = 0L
        var lastRefillMs: Long = System.currentTimeMillis()
        var tokens: Double = action.burstCapacity.toDouble()
        val executionTimestamps = mutableListOf<Long>()
        var lockoutUntilMs: Long = 0L
        var failureStreak: AtomicInteger = AtomicInteger(0)
        var totalRequestsServed: Long = 0L
        var totalRequestsThrottled: Long = 0L
        val activeCountdownFlow = MutableStateFlow(0L)
        var countdownJob: Job? = null

        fun refill(now: Long) {
            val elapsedMs = (now - lastRefillMs).coerceAtLeast(0L)
            val tokensToAdd = (elapsedMs / 60_000.0) * action.requestsPerMinute
            if (tokensToAdd > 0.0) {
                tokens = min(action.burstCapacity.toDouble(), tokens + tokensToAdd)
                lastRefillMs = now
            }
        }
    }

    private val trackers = ConcurrentHashMap<String, Tracker>()

    private fun getTrackerKey(action: ActionType, identifier: String): String {
        val cleanId = identifier.trim().lowercase()
        return "${action.name}:$cleanId"
    }

    private fun getOrCreateTracker(action: ActionType, identifier: String): Tracker {
        val key = getTrackerKey(action, identifier)
        return trackers.computeIfAbsent(key) {
            val tracker = Tracker(action)
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
                    val database = FirebaseDatabase.getInstance("https://velorix-tournaments-default-rtdb.asia-southeast1.firebasedatabase.app").reference
                    val payload = mapOf<String, Any>(
                        "action" to action.name,
                        "actionName" to action.actionDisplayName,
                        "category" to action.category.label,
                        "requestsPerMinute" to action.requestsPerMinute,
                        "identifier" to cleanId,
                        "lockoutUntil" to lockoutUntilMs,
                        "reason" to reason,
                        "updatedAt" to System.currentTimeMillis(),
                        "isLocked" to (lockoutUntilMs > System.currentTimeMillis())
                    )
                    database.child("rate_limits").child(cleanId).child(action.name).setValue(payload)
                } catch (_: Exception) {}

                try {
                    val firestore = FirebaseFirestore.getInstance()
                    firestore.collection("rate_limits").document("${cleanId}_${action.name}").set(
                        mapOf(
                            "action" to action.name,
                            "identifier" to cleanId,
                            "lockoutUntil" to lockoutUntilMs,
                            "reason" to reason,
                            "updatedAt" to com.google.firebase.Timestamp.now()
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Checks whether an action can be performed right now under Token-Bucket + Sliding Window constraints.
     */
    fun canExecute(action: ActionType, identifier: String = ""): RateLimitResult {
        val now = System.currentTimeMillis()
        val tracker = getOrCreateTracker(action, identifier)

        synchronized(tracker) {
            tracker.refill(now)

            // 1. Check if persistent lockout is active
            if (now < tracker.lockoutUntilMs) {
                val remainingSec = ceil((tracker.lockoutUntilMs - now) / 1000.0).toLong().coerceAtLeast(1L)
                return RateLimitResult(
                    isAllowed = false,
                    remainingCooldownSeconds = remainingSec,
                    remainingAttempts = 0,
                    currentTokens = tracker.tokens,
                    maxTokens = action.burstCapacity,
                    reasonMessage = "Security cooldown active for ${action.actionDisplayName}. Please wait ${remainingSec}s before retrying."
                )
            }

            // 2. Minimum interval throttle check
            val elapsedSinceLast = now - tracker.lastExecutionMs
            if (tracker.lastExecutionMs > 0L && elapsedSinceLast < action.minIntervalMs) {
                val remainingSec = ceil((action.minIntervalMs - elapsedSinceLast) / 1000.0).toLong().coerceAtLeast(1L)
                return RateLimitResult(
                    isAllowed = false,
                    remainingCooldownSeconds = remainingSec,
                    remainingAttempts = 0,
                    currentTokens = tracker.tokens,
                    maxTokens = action.burstCapacity,
                    reasonMessage = "Please wait ${remainingSec}s before requesting another ${action.actionDisplayName}."
                )
            }

            // 3. Sliding Window Log check
            val windowStart = now - action.windowDurationMs
            tracker.executionTimestamps.removeAll { it < windowStart }

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
                    currentTokens = tracker.tokens,
                    maxTokens = action.burstCapacity,
                    reasonMessage = "Rate limit reached for ${action.actionDisplayName} (Max ${action.requestsPerMinute.toInt()} req/min). Try again in ${remainingSec}s."
                )
            }

            // 4. Token Bucket check
            if (tracker.tokens < 1.0) {
                val secToNextToken = ceil(((1.0 - tracker.tokens) / action.requestsPerMinute) * 60.0).toLong().coerceAtLeast(1L)
                return RateLimitResult(
                    isAllowed = false,
                    remainingCooldownSeconds = secToNextToken,
                    remainingAttempts = 0,
                    currentTokens = tracker.tokens,
                    maxTokens = action.burstCapacity,
                    reasonMessage = "Token quota exhausted for ${action.actionDisplayName}. Next token in ${secToNextToken}s."
                )
            }

            val remainingQuota = (action.maxAttemptsInWindow - tracker.executionTimestamps.size).coerceAtLeast(0)
            return RateLimitResult(
                isAllowed = true,
                remainingCooldownSeconds = 0L,
                remainingAttempts = remainingQuota,
                currentTokens = tracker.tokens,
                maxTokens = action.burstCapacity,
                reasonMessage = ""
            )
        }
    }

    /**
     * Checks rate limit and atomically records the token consumption and sliding window execution.
     */
    fun checkAndRecord(action: ActionType, identifier: String = ""): RateLimitResult {
        val check = canExecute(action, identifier)
        if (!check.isAllowed) {
            val tracker = getOrCreateTracker(action, identifier)
            synchronized(tracker) {
                tracker.totalRequestsThrottled++
            }
            return check
        }

        val now = System.currentTimeMillis()
        val tracker = getOrCreateTracker(action, identifier)
        synchronized(tracker) {
            tracker.tokens = (tracker.tokens - 1.0).coerceAtLeast(0.0)
            tracker.lastExecutionMs = now
            tracker.executionTimestamps.add(now)
            tracker.totalRequestsServed++
            val key = getTrackerKey(action, identifier)
            persistLockout(key, 0L, now)
        }

        return check.copy(currentTokens = tracker.tokens)
    }

    fun recordSuccess(action: ActionType, identifier: String = "") {
        val tracker = getOrCreateTracker(action, identifier)
        synchronized(tracker) {
            tracker.failureStreak.set(0)
            tracker.lockoutUntilMs = 0L
            tracker.activeCountdownFlow.value = 0L
            tracker.countdownJob?.cancel()
            val key = getTrackerKey(action, identifier)
            persistLockout(key, 0L, tracker.lastExecutionMs)
        }
    }

    fun recordFailure(action: ActionType, identifier: String = ""): RateLimitResult {
        val now = System.currentTimeMillis()
        val tracker = getOrCreateTracker(action, identifier)
        val streak = tracker.failureStreak.incrementAndGet()

        val multiplier = when {
            streak >= 5 -> 4.0
            streak >= 3 -> 2.0
            else -> 1.0
        }

        val lockoutDuration = (action.defaultLockoutMs * multiplier).toLong()
        val lockoutUntil = now + lockoutDuration

        synchronized(tracker) {
            tracker.lockoutUntilMs = lockoutUntil
            val remainingSec = ceil(lockoutDuration / 1000.0).toLong()
            startCountdown(tracker, remainingSec)
            val key = getTrackerKey(action, identifier)
            persistLockout(key, lockoutUntil, tracker.lastExecutionMs)
            syncRateLimitToFirebase(action, identifier, lockoutUntil, "Consecutive authentication failures (Streak: $streak)")
            return RateLimitResult(
                isAllowed = false,
                remainingCooldownSeconds = remainingSec,
                remainingAttempts = 0,
                currentTokens = tracker.tokens,
                maxTokens = action.burstCapacity,
                reasonMessage = "Security Lockout: $streak failed attempts. Locked for ${remainingSec}s."
            )
        }
    }

    fun resetCooldown(action: ActionType, identifier: String = "") {
        val tracker = getOrCreateTracker(action, identifier)
        synchronized(tracker) {
            tracker.lockoutUntilMs = 0L
            tracker.failureStreak.set(0)
            tracker.tokens = action.burstCapacity.toDouble()
            tracker.executionTimestamps.clear()
            tracker.activeCountdownFlow.value = 0L
            tracker.countdownJob?.cancel()
            val key = getTrackerKey(action, identifier)
            persistLockout(key, 0L, 0L)
        }
    }

    fun resetAllLimits() {
        trackers.values.forEach { tracker ->
            synchronized(tracker) {
                tracker.lockoutUntilMs = 0L
                tracker.failureStreak.set(0)
                tracker.tokens = tracker.action.burstCapacity.toDouble()
                tracker.executionTimestamps.clear()
                tracker.activeCountdownFlow.value = 0L
                tracker.countdownJob?.cancel()
            }
        }
        sharedPreferences?.edit()?.clear()?.apply()
    }

    fun isUserLockedOut(identifier: String): Boolean {
        if (identifier.isBlank()) return false
        val now = System.currentTimeMillis()
        val cleanId = identifier.trim().lowercase()
        return ActionType.values().any { action ->
            val key = getTrackerKey(action, cleanId)
            val tracker = trackers[key] ?: return@any false
            tracker.lockoutUntilMs > now
        }
    }

    fun getActiveLockoutSummary(identifier: String): String? {
        if (identifier.isBlank()) return null
        val now = System.currentTimeMillis()
        val cleanId = identifier.trim().lowercase()
        val activeLockouts = ActionType.values().mapNotNull { action ->
            val key = getTrackerKey(action, cleanId)
            val tracker = trackers[key] ?: return@mapNotNull null
            if (tracker.lockoutUntilMs > now) {
                val sec = ceil((tracker.lockoutUntilMs - now) / 1000.0).toLong()
                "${action.actionDisplayName} (${sec}s remaining)"
            } else null
        }
        return if (activeLockouts.isNotEmpty()) activeLockouts.joinToString(", ") else null
    }

    fun resetUserLimits(identifier: String): Int {
        if (identifier.isBlank()) return 0
        var count = 0
        val cleanId = identifier.trim().lowercase()
        ActionType.values().forEach { action ->
            val key = getTrackerKey(action, cleanId)
            val tracker = trackers[key]
            if (tracker != null) {
                synchronized(tracker) {
                    tracker.lockoutUntilMs = 0L
                    tracker.failureStreak.set(0)
                    tracker.tokens = action.burstCapacity.toDouble()
                    tracker.executionTimestamps.clear()
                    tracker.activeCountdownFlow.value = 0L
                    tracker.countdownJob?.cancel()
                    persistLockout(key, 0L, 0L)
                    count++
                }
            }
        }
        return count
    }

    fun resetLimitsForUser(identifier: String): Int = resetUserLimits(identifier)

    fun observeCooldownSeconds(action: ActionType, identifier: String = ""): StateFlow<Long> {
        val tracker = getOrCreateTracker(action, identifier)
        return tracker.activeCountdownFlow.asStateFlow()
    }

    fun getAllSnapshots(identifier: String = ""): List<RateLimitSnapshot> {
        val now = System.currentTimeMillis()
        return ActionType.values().map { action ->
            val tracker = getOrCreateTracker(action, identifier)
            synchronized(tracker) {
                tracker.refill(now)
                val isLocked = now < tracker.lockoutUntilMs
                val cooldownSec = if (isLocked) ceil((tracker.lockoutUntilMs - now) / 1000.0).toLong() else 0L
                val windowHits = tracker.executionTimestamps.count { it >= now - action.windowDurationMs }
                RateLimitSnapshot(
                    actionName = action.actionDisplayName,
                    category = action.category.label,
                    requestsPerMinute = action.requestsPerMinute,
                    availableTokens = tracker.tokens,
                    burstCapacity = action.burstCapacity,
                    isLocked = isLocked,
                    cooldownSeconds = cooldownSec,
                    slidingWindowHits = windowHits
                )
            }
        }
    }

    private fun startCountdown(tracker: Tracker, initialSeconds: Long) {
        tracker.countdownJob?.cancel()
        tracker.activeCountdownFlow.value = initialSeconds
        tracker.countdownJob = scope.launch {
            var current = initialSeconds
            while (current > 0) {
                tracker.activeCountdownFlow.value = current
                delay(1000L)
                current--
            }
            tracker.activeCountdownFlow.value = 0L
            tracker.lockoutUntilMs = 0L
        }
    }
}
