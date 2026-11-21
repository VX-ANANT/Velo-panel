package com.example.notification

import android.content.Context
import android.util.Log
import com.example.analytics.VelorixAnalytics
import com.example.domain.model.AppNotification
import com.example.domain.model.GlobalAnnouncement
import com.example.domain.model.Tournament
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * VelorixFcmManager:
 * Routes all Admin Panel push notifications, test pushes, room credentials,
 * match reminders, and payout alerts exclusively to the User Panel apps:
 * - com.tournaments.velorix (1:27931798964:android:8e63979e3ef30c1ebe56e1)
 * - com.tournaments.velorix.mskqap (1:27931798964:android:1e70339a88bafbbebe56e1)
 *
 * Never triggers self-notifications on the Admin Panel (com.admin.velorix).
 */
object VelorixFcmManager {
    private const val TAG = "VelorixFcmManager"
    const val RTDB_URL = "https://velorix-tournaments-default-rtdb.asia-southeast1.firebasedatabase.app"

    const val ADMIN_PANEL_PACKAGE = "com.admin.velorix"
    const val USER_PANEL_PACKAGE = "com.tournaments.velorix"
    const val USER_PANEL_PACKAGE_ALT = "com.tournaments.velorix.mskqap"
    const val USER_PANEL_APP_ID = "1:27931798964:android:8e63979e3ef30c1ebe56e1"
    const val USER_PANEL_APP_ID_ALT = "1:27931798964:android:1e70339a88bafbbebe56e1"

    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    private fun getRtdbRef() = try {
        FirebaseDatabase.getInstance(RTDB_URL).reference
    } catch (_: Exception) {
        FirebaseDatabase.getInstance().reference
    }

    fun initialize(context: Context) {
        try {
            val appidPrefs = context.getSharedPreferences("com.google.android.gms.appid", Context.MODE_PRIVATE)
            if (appidPrefs.all.isNotEmpty()) {
                appidPrefs.edit().clear().apply()
            }
        } catch (_: Exception) {}

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Ensure the Admin Panel unsubscribes from player topics so player pushes only hit the User Panel
                val messaging = FirebaseMessaging.getInstance()
                listOf("all_players", "user_panel", "tournaments", "velorix_players", "global_announcements").forEach { playerTopic ->
                    try {
                        messaging.unsubscribeFromTopic(playerTopic)
                    } catch (_: Exception) {}
                }

                @Suppress("DEPRECATION")
                messaging.token
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val token = task.result
                            _fcmToken.value = token
                            Log.d(TAG, "Admin Panel FCM Token retrieved: $token")
                            if (!token.isNullOrBlank()) {
                                saveTokenToFirebase(token)
                            }
                        } else {
                            val err = task.exception
                            val errMsg = err?.message ?: "Unknown error"
                            Log.w(TAG, "FCM token registration skipped on this device: $errMsg")
                            if (errMsg.contains("TOO_MANY_REGISTRATIONS", ignoreCase = true)) {
                                try {
                                    @Suppress("DEPRECATION")
                                    messaging.deleteToken()
                                } catch (_: Exception) {}
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.w(TAG, "FCM initialization handled safely: ${e.message}")
            }
        }
    }

    fun subscribeToTournamentTopic(tournamentId: String) {
        // Admin panel does not subscribe to player tournament topics to prevent self-notification
        Log.d(TAG, "Skipped subscribing Admin Panel to player topic tournament_$tournamentId")
    }

    fun unsubscribeFromTournamentTopic(tournamentId: String) {
        val topic = "tournament_$tournamentId"
        try {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
        } catch (_: Exception) {}
    }

    /**
     * Stores Admin FCM token separately under `admin_fcm_tokens` and removes it from player `fcm_tokens`
     * so user-panel broadcasts never target the Admin Panel device.
     */
    fun saveTokenToFirebase(token: String) {
        _fcmToken.value = token
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        try {
            val tokenKey = token.take(64).replace("[.#$\\[\\]]".toRegex(), "_")
            val tokenData = hashMapOf<String, Any>(
                "token" to token,
                "platform" to "android",
                "appId" to ADMIN_PANEL_PACKAGE,
                "role" to "admin",
                "targetPanel" to "admin_panel",
                "updatedAt" to System.currentTimeMillis()
            )
            if (uid != null) {
                tokenData["userId"] = uid
            }

            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("admin_fcm_tokens").document(token).set(tokenData)
            // Remove from player fcm_tokens collection if previously registered there
            firestore.collection("fcm_tokens").document(token).delete()

            val rtdbRef = getRtdbRef()
            rtdbRef.child("admin_fcm_tokens").child(tokenKey).setValue(tokenData)
            rtdbRef.child("fcm_tokens").child(tokenKey).removeValue()
        } catch (e: Exception) {
            Log.w(TAG, "Could not save Admin FCM token to backend: ${e.message}")
        }
    }

    /**
     * Core dispatcher that sends any notification payload strictly to the User Panel
     * (`com.tournaments.velorix` and `com.tournaments.velorix.mskqap`) across both
     * Firebase Realtime Database and Cloud Firestore.
     */
    fun dispatchToUserPanel(
        notification: AppNotification,
        audience: String = "ALL",
        targetUserId: String? = null,
        targetTournamentId: String? = notification.targetTournamentId.ifBlank { null },
        extraData: Map<String, Any> = emptyMap(),
        pinToGlobalAnnouncements: Boolean = true
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = if (notification.timestamp > 0L) notification.timestamp else System.currentTimeMillis()
                val notifId = notification.id.ifBlank { "notif_$now" }
                val effectiveTournamentId = targetTournamentId ?: ""
                val effectiveUserId = targetUserId?.trim() ?: ""

                val payload = HashMap<String, Any>().apply {
                    put("id", notifId)
                    put("notificationId", notifId)
                    put("title", notification.title)
                    put("message", notification.message)
                    put("body", notification.message)
                    put("type", notification.type)
                    put("game", notification.game)
                    put("priority", notification.priority)
                    put("level", notification.priority)
                    put("timestamp", now)
                    put("createdAt", now)
                    put("isRead", false)
                    put("audience", audience)
                    put("targetTournamentId", effectiveTournamentId)
                    put("tournamentId", effectiveTournamentId)
                    put("targetUserId", effectiveUserId)
                    put("userId", effectiveUserId)
                    // Explicit User Panel routing metadata
                    put("targetPanel", "user_panel")
                    put("targetPackage", USER_PANEL_PACKAGE)
                    put("targetPackages", listOf(USER_PANEL_PACKAGE, USER_PANEL_PACKAGE_ALT))
                    put("targetAppIds", listOf(USER_PANEL_APP_ID, USER_PANEL_APP_ID_ALT))
                    put("senderPackage", ADMIN_PANEL_PACKAGE)
                    put("topic", if (effectiveTournamentId.isNotBlank()) "tournament_$effectiveTournamentId" else "all_players")
                    putAll(extraData)
                }

                val rtdbRef = getRtdbRef()
                val firestore = FirebaseFirestore.getInstance()

                // 1. Write to User Panel RTDB notification & broadcast streams
                rtdbRef.child("user_notifications").child(notifId).setValue(payload)
                rtdbRef.child("notifications").child(notifId).setValue(payload)
                rtdbRef.child("broadcast_notifications").child(notifId).setValue(payload)
                rtdbRef.child("campaigns").child(notifId).setValue(payload)
                rtdbRef.child("fcm_outbox").child(notifId).setValue(payload)
                rtdbRef.child("push_queue").child(notifId).setValue(payload)
                rtdbRef.child("meta").child("last_user_push_timestamp").setValue(now)

                // 2. If targeted to a specific user, write to their personal notification inbox
                if (effectiveUserId.isNotBlank()) {
                    rtdbRef.child("users").child(effectiveUserId).child("notifications").child(notifId).setValue(payload)
                    rtdbRef.child("user_notifications_by_uid").child(effectiveUserId).child(notifId).setValue(payload)
                    try {
                        firestore.collection("users").document(effectiveUserId)
                            .collection("notifications").document(notifId).set(payload)
                    } catch (_: Exception) {}
                }

                // 3. If targeted to a specific tournament, write to dedicated tournament_notifications node
                // (Never write partial sub-keys directly under /tournaments/{id} as that creates orphan tournament stubs if the tournament was deleted)
                if (effectiveTournamentId.isNotBlank()) {
                    rtdbRef.child("tournament_notifications").child(effectiveTournamentId).child(notifId).setValue(payload)
                }

                // 4. Pin to User Panel Global Announcements feed if enabled
                if (pinToGlobalAnnouncements && effectiveUserId.isBlank()) {
                    val annId = "ANN-$notifId"
                    val announcement = GlobalAnnouncement(
                        id = annId,
                        title = notification.title,
                        message = notification.message,
                        level = notification.priority,
                        createdAt = now,
                        author = "Velorix Admin",
                        isActive = true
                    )
                    val annMap = HashMap<String, Any>().apply {
                        put("id", annId)
                        put("title", announcement.title)
                        put("message", announcement.message)
                        put("body", announcement.message)
                        put("level", announcement.level)
                        put("priority", announcement.level)
                        put("createdAt", now)
                        put("timestamp", now)
                        put("author", announcement.author)
                        put("isActive", true)
                        put("targetPanel", "user_panel")
                        put("targetPackage", USER_PANEL_PACKAGE)
                        put("targetPackages", listOf(USER_PANEL_PACKAGE, USER_PANEL_PACKAGE_ALT))
                        put("targetTournamentId", effectiveTournamentId)
                    }
                    rtdbRef.child("announcements").child(annId).setValue(annMap)
                    rtdbRef.child("global_announcements").child(annId).setValue(annMap)
                    try { firestore.collection("announcements").document(annId).set(annMap) } catch (_: Exception) {}
                    try { firestore.collection("global_announcements").document(annId).set(annMap) } catch (_: Exception) {}
                }

                // 5. Mirror to Cloud Firestore User Panel collections
                try { firestore.collection("user_notifications").document(notifId).set(payload) } catch (_: Exception) {}
                try { firestore.collection("notifications").document(notifId).set(payload) } catch (_: Exception) {}
                try { firestore.collection("broadcast_notifications").document(notifId).set(payload) } catch (_: Exception) {}
                try { firestore.collection("campaigns").document(notifId).set(payload) } catch (_: Exception) {}
                try { firestore.collection("fcm_outbox").document(notifId).set(payload) } catch (_: Exception) {}

                Log.d(TAG, "Dispatched notification [$notifId] to User Panel ($USER_PANEL_PACKAGE): ${notification.title}")
            } catch (e: Exception) {
                Log.w(TAG, "Failed dispatching notification to User Panel: ${e.message}")
            }
        }
    }

    /**
     * Dispatches a Test Push Notification directly to the User Panel (`com.tournaments.velorix`).
     */
    fun dispatchTestPushToUserPanel(
        tournament: Tournament? = null
    ) {
        val now = System.currentTimeMillis()
        val notifId = "test_push_$now"
        val title = if (tournament != null) {
            "Match Alert: ${tournament.title}"
        } else {
            "Velorix Esports Live Alert"
        }
        val message = if (tournament != null) {
            "${tournament.game} • Prize Pool: ₹${tournament.prizePool.toInt()} • ${tournament.maxPlayers} Slots (${tournament.format}). Open User App to join!"
        } else {
            "Live Push from Velorix Admin Panel received on User Panel (${USER_PANEL_PACKAGE})."
        }

        val notif = AppNotification(
            id = notifId,
            title = title,
            message = message,
            type = if (tournament != null) "TOURNAMENT" else "CAMPAIGN",
            targetTournamentId = tournament?.id ?: "",
            game = tournament?.game ?: "Free Fire",
            timestamp = now,
            priority = "URGENT",
            isRead = false
        )

        VelorixAnalytics.logNotificationInteraction("TEST_USER_PUSH", "sent_to_user_panel", tournament?.id)
        dispatchToUserPanel(
            notification = notif,
            audience = "ALL",
            targetTournamentId = tournament?.id,
            extraData = mapOf("isTestPush" to true),
            pinToGlobalAnnouncements = true
        )
    }

    /**
     * Dispatches Room ID & Password push notification strictly to the User Panel (`com.tournaments.velorix`).
     */
    fun dispatchRoomDetailsPush(
        context: Context,
        tournament: Tournament,
        roomId: String,
        roomPassword: String
    ) {
        VelorixAnalytics.logNotificationInteraction("ROOM_DETAILS", "sent_to_user_panel", tournament.id)
        val now = System.currentTimeMillis()
        val roomCredentialsMap = mapOf(
            "id" to tournament.id,
            "title" to tournament.title,
            "roomId" to roomId,
            "roomPassword" to roomPassword,
            "room_id" to roomId,
            "room_password" to roomPassword,
            "updatedAt" to now
        )

        if (tournament.id.isNotBlank() && tournament.title.isNotBlank() && !tournament.title.equals("Tournament", ignoreCase = true)) {
            try {
                val rtdbRef = getRtdbRef()
                rtdbRef.child("tournaments").child(tournament.id).updateChildren(
                    mapOf(
                        "id" to tournament.id,
                        "title" to tournament.title,
                        "roomDetails" to roomCredentialsMap,
                        "roomId" to roomId,
                        "roomPassword" to roomPassword
                    )
                )
                rtdbRef.child("custom_rooms").child(tournament.id).setValue(roomCredentialsMap)
            } catch (e: Exception) {
                Log.w(TAG, "Failed updating RTDB roomDetails: ${e.message}")
            }
        }

        // Delegate to VelorixNotificationManager which forwards to User Panel without notifying Admin device
        VelorixNotificationManager.dispatchRoomCredentialsNotification(
            context = context,
            tournament = tournament,
            roomId = roomId,
            roomPass = roomPassword
        )
    }

    /**
     * Dispatches a 15-Minute Match Reminder strictly to the User Panel (`com.tournaments.velorix`).
     */
    fun dispatchMatchReminderPush(
        context: Context,
        tournament: Tournament,
        minutesLeft: Int = 15
    ) {
        VelorixAnalytics.logNotificationInteraction("MATCH_REMINDER", "sent_to_user_panel", tournament.id)
        val now = System.currentTimeMillis()
        val notif = AppNotification(
            id = "reminder_${now}_${tournament.id.takeLast(4)}",
            title = "Match Reminder: ${tournament.title}",
            message = "Your ${tournament.game} match begins in $minutesLeft minutes! Prepare your squad and be ready for custom room entry.",
            type = "MATCH_REMINDER",
            targetTournamentId = tournament.id,
            game = tournament.game,
            timestamp = now,
            priority = "URGENT",
            isRead = false
        )
        dispatchToUserPanel(
            notification = notif,
            audience = "TOURNAMENT",
            targetTournamentId = tournament.id,
            extraData = mapOf(
                "tournament_title" to tournament.title,
                "minutes_left" to minutesLeft.toString()
            ),
            pinToGlobalAnnouncements = true
        )
    }

    /**
     * Dispatches a Payout Settlement Alert strictly to the User Panel (`com.tournaments.velorix`).
     */
    fun dispatchPayoutSettlementPush(
        context: Context,
        userId: String = "",
        username: String,
        amount: Double,
        approved: Boolean,
        details: String
    ) {
        VelorixAnalytics.logNotificationInteraction("TRANSACTION_ALERT", "sent_to_user_panel", userId)
        VelorixNotificationManager.dispatchPayoutNotification(
            context = context,
            username = username,
            amount = amount,
            approved = approved,
            details = details,
            targetUserId = userId
        )
    }
}
