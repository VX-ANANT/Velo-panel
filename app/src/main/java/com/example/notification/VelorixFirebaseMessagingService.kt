package com.example.notification

import android.util.Log
import com.example.analytics.VelorixAnalytics
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * VelorixFirebaseMessagingService (Admin Panel):
 * Handles incoming FCM messages intended specifically for the Admin Panel (`com.admin.velorix`).
 * Any player-targeted notifications (`targetPanel == "user_panel"` or `targetPackage == "com.tournaments.velorix"`)
 * are ignored on the Admin Panel so test pushes and player broadcasts only appear on the User Panel.
 */
class VelorixFirebaseMessagingService : FirebaseMessagingService() {

    @Deprecated("Deprecated in Java")
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New Admin FCM Registration Token generated: $token")
        VelorixFcmManager.saveTokenToFirebase(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data
        val targetPanel = data["targetPanel"] ?: ""
        val targetPackage = data["targetPackage"] ?: ""
        val senderPackage = data["senderPackage"] ?: ""
        val notificationType = data["type"] ?: "DEFAULT"

        // Never display User Panel notifications on the Admin Panel device
        if (targetPanel.equals("user_panel", ignoreCase = true) ||
            targetPackage.startsWith("com.tournaments.velorix", ignoreCase = true) ||
            senderPackage.equals(VelorixFcmManager.ADMIN_PANEL_PACKAGE, ignoreCase = true) ||
            notificationType in setOf("ROOM_DETAILS", "TRANSACTION_ALERT", "MATCH_REMINDER", "CAMPAIGN", "TOURNAMENT")
        ) {
            Log.d(TAG, "Ignored User Panel push on Admin Panel device (type=$notificationType, target=$targetPackage)")
            return
        }

        val tournamentId = data["tournament_id"] ?: data["targetTournamentId"]
        VelorixAnalytics.logNotificationInteraction(notificationType, "admin_received", tournamentId)
    }

    companion object {
        private const val TAG = "VelorixFCMService"
    }
}
