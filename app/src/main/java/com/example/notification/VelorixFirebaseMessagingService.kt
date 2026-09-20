package com.example.notification

import android.util.Log
import com.example.analytics.VelorixAnalytics
import com.example.domain.model.Tournament
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * VelorixFirebaseMessagingService:
 * Real-time Firebase Cloud Messaging (FCM) service for handling push messages:
 * 1. Room Details (Instant Room ID & Password alert for registered players)
 * 2. Transaction Alerts (Instant wallet withdrawal & payout confirmations)
 * 3. Match Reminders (15-minute countdown reminders before tournament kick-off)
 */
class VelorixFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Registration Token generated: $token")
        VelorixFcmManager.saveTokenToFirebase(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message received from: ${remoteMessage.from}, data: ${remoteMessage.data}")

        val data = remoteMessage.data
        val notificationType = data["type"] ?: "DEFAULT"
        val tournamentId = data["tournament_id"] ?: data["targetTournamentId"]

        VelorixAnalytics.logNotificationInteraction(notificationType, "received", tournamentId)

        when (notificationType) {
            "ROOM_DETAILS" -> {
                val roomId = data["room_id"] ?: data["roomId"] ?: ""
                val roomPassword = data["room_password"] ?: data["roomPassword"] ?: ""
                val tournamentTitle = data["tournament_title"] ?: "Free Fire Tournament"
                val game = data["game"] ?: "Free Fire"

                if (roomId.isNotBlank()) {
                    val tournamentStub = Tournament(
                        id = tournamentId ?: "tourney_${System.currentTimeMillis()}",
                        title = tournamentTitle,
                        game = game
                    )
                    VelorixNotificationManager.dispatchRoomCredentialsNotification(
                        context = applicationContext,
                        tournament = tournamentStub,
                        roomId = roomId,
                        roomPass = roomPassword
                    )
                }
            }

            "TRANSACTION_ALERT" -> {
                val username = data["username"] ?: "Player"
                val amount = data["amount"]?.toDoubleOrNull() ?: 0.0
                val approved = data["status"]?.equals("COMPLETED", ignoreCase = true) == true ||
                               data["status"]?.equals("APPROVED", ignoreCase = true) == true
                val details = data["details"] ?: "UPI Transfer"

                VelorixNotificationManager.dispatchPayoutNotification(
                    context = applicationContext,
                    username = username,
                    amount = amount,
                    approved = approved,
                    details = details
                )
            }

            "MATCH_REMINDER" -> {
                val tournamentTitle = data["tournament_title"] ?: "Tournament Match"
                val minutesLeft = data["minutes_left"] ?: "15"

                VelorixNotificationManager.dispatchCampaignBroadcast(
                    context = applicationContext,
                    title = "Match Reminder: $tournamentTitle",
                    message = "Your Free Fire match starts in $minutesLeft minutes! Get ready to enter the custom room.",
                    level = "URGENT",
                    tournamentId = tournamentId
                )
            }

            else -> {
                // Check if notification payload is present
                val title = remoteMessage.notification?.title ?: data["title"] ?: "Velorix Esports"
                val body = remoteMessage.notification?.body ?: data["message"] ?: "New tournament notification received."

                VelorixNotificationManager.dispatchCampaignBroadcast(
                    context = applicationContext,
                    title = title,
                    message = body,
                    level = "HIGH",
                    tournamentId = tournamentId
                )
            }
        }
    }

    companion object {
        private const val TAG = "VelorixFCMService"
    }
}
