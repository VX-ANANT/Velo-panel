package com.example.notification

import android.content.Context
import android.util.Log
import com.example.analytics.VelorixAnalytics
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
import kotlinx.coroutines.tasks.await

/**
 * VelorixFcmManager:
 * Manages FCM registration tokens, topic subscriptions, and push message delivery for:
 * - Room Details (Room ID & Password)
 * - Transaction Alerts (Payouts & Wallet)
 * - 15-Minute Match Reminders
 */
object VelorixFcmManager {
    private const val TAG = "VelorixFcmManager"
    private const val RTDB_URL = "https://velorix-tournaments-default-rtdb.asia-southeast1.firebasedatabase.app"

    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    fun initialize(context: Context) {
        // Clear any stuck topic queues from SharedPreferences if an emulator/device hit registration limits
        try {
            val appidPrefs = context.getSharedPreferences("com.google.android.gms.appid", Context.MODE_PRIVATE)
            if (appidPrefs.all.isNotEmpty()) {
                appidPrefs.edit().clear().apply()
            }
        } catch (_: Exception) {}

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch device FCM registration token safely without blocking on topic sync
                FirebaseMessaging.getInstance().token
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val token = task.result
                            _fcmToken.value = token
                            Log.d(TAG, "Current FCM Device Token retrieved: $token")
                            if (!token.isNullOrBlank()) {
                                saveTokenToFirebase(token)
                            }
                        } else {
                            val err = task.exception
                            val errMsg = err?.message ?: "Unknown error"
                            Log.w(TAG, "FCM token registration skipped or unavailable on this device/emulator: $errMsg")
                            if (errMsg.contains("TOO_MANY_REGISTRATIONS", ignoreCase = true)) {
                                try {
                                    FirebaseMessaging.getInstance().deleteToken()
                                } catch (_: Exception) {}
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.w(TAG, "FCM initialization handled safely: ${e.message}")
            }
        }
    }

    /**
     * Subscribes the device to a specific tournament topic so the player receives
     * direct push notifications when Room ID and Password are published.
     */
    fun subscribeToTournamentTopic(tournamentId: String) {
        val topic = "tournament_$tournamentId"
        try {
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnSuccessListener {
                    Log.d(TAG, "Subscribed successfully to FCM topic: $topic")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed subscribing to topic $topic: ${e.message}")
                }
        } catch (e: Exception) {
            Log.w(TAG, "subscribeToTournamentTopic caught: ${e.message}")
        }
    }

    /**
     * Unsubscribes from tournament topic when player leaves or tournament finishes.
     */
    fun unsubscribeFromTournamentTopic(tournamentId: String) {
        val topic = "tournament_$tournamentId"
        try {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                .addOnSuccessListener {
                    Log.d(TAG, "Unsubscribed from FCM topic: $topic")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed unsubscribing from topic $topic: ${e.message}")
                }
        } catch (e: Exception) {
            Log.w(TAG, "unsubscribeFromTournamentTopic caught: ${e.message}")
        }
    }

    /**
     * Stores FCM token in Firebase Firestore and Realtime Database for targeted player messaging.
     */
    fun saveTokenToFirebase(token: String) {
        _fcmToken.value = token
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        try {
            val tokenData = hashMapOf(
                "token" to token,
                "platform" to "android",
                "appId" to "com.admin.velorix",
                "updatedAt" to System.currentTimeMillis()
            )
            if (uid != null) {
                tokenData["userId"] = uid
            }

            // Save to Firestore
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("fcm_tokens").document(token).set(tokenData)
            if (uid != null) {
                firestore.collection("users").document(uid).update("fcmToken", token)
            }

            // Also mirror to RTDB
            val rtdbRef = try {
                FirebaseDatabase.getInstance(RTDB_URL).reference
            } catch (_: Exception) {
                FirebaseDatabase.getInstance().reference
            }
            rtdbRef.child("fcm_tokens").child(token.take(64).replace("[.#$\\[\\]]".toRegex(), "_")).setValue(tokenData)
            if (uid != null) {
                rtdbRef.child("users").child(uid).child("fcmToken").setValue(token)
            }
            Log.d(TAG, "FCM token saved to backend: $token")
        } catch (e: Exception) {
            Log.w(TAG, "Could not save FCM token to backend: ${e.message}")
        }
    }

    /**
     * Admin Tool / Push Trigger: Dispatches a Room Details push notification
     * to all registered players for this tournament.
     */
    fun dispatchRoomDetailsPush(
        context: Context,
        tournament: Tournament,
        roomId: String,
        roomPassword: String
    ) {
        VelorixAnalytics.logNotificationInteraction("ROOM_DETAILS", "sent_broadcast", tournament.id)

        // 1. Dispatch local high-priority notification and in-app banner
        VelorixNotificationManager.dispatchRoomCredentialsNotification(
            context = context,
            tournament = tournament,
            roomId = roomId,
            roomPass = roomPassword
        )

        // 2. Publish to RTDB broadcast feed so all live connected clients receive it immediately
        try {
            val rtdbRef = try {
                FirebaseDatabase.getInstance(RTDB_URL).reference
            } catch (_: Exception) {
                FirebaseDatabase.getInstance().reference
            }
            val broadcastPayload = hashMapOf(
                "type" to "ROOM_DETAILS",
                "tournamentId" to tournament.id,
                "tournamentTitle" to tournament.title,
                "roomId" to roomId,
                "roomPassword" to roomPassword,
                "timestamp" to System.currentTimeMillis()
            )
            rtdbRef.child("tournaments").child(tournament.id).child("roomDetails").setValue(
                mapOf("roomId" to roomId, "roomPassword" to roomPassword, "updatedAt" to System.currentTimeMillis())
            )
            rtdbRef.child("broadcast_notifications").push().setValue(broadcastPayload)
        } catch (e: Exception) {
            Log.w(TAG, "Failed publishing broadcast to RTDB: ${e.message}")
        }
    }

    /**
     * Admin Tool / Push Trigger: Dispatches a 15-Minute Match Reminder.
     */
    fun dispatchMatchReminderPush(
        context: Context,
        tournament: Tournament,
        minutesLeft: Int = 15
    ) {
        VelorixAnalytics.logNotificationInteraction("MATCH_REMINDER", "sent_reminder", tournament.id)

        VelorixNotificationManager.dispatchCampaignBroadcast(
            context = context,
            title = "Match Reminder: ${tournament.title}",
            message = "Your Free Fire match begins in $minutesLeft minutes! Prepare your squad and be ready for custom room entry.",
            level = "URGENT",
            tournamentId = tournament.id
        )
    }
}
