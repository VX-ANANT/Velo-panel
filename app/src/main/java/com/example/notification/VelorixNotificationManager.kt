package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.domain.model.AppNotification
import com.example.domain.model.Tournament
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * VelorixNotificationManager (Admin Panel):
 * Builds structured player notifications and dispatches them directly to the User Panel
 * (`com.tournaments.velorix` and `com.tournaments.velorix.mskqap`) via Firebase RTDB & Firestore.
 *
 * IMPORTANT: Does NOT trigger local Android system notifications or player banners on this
 * Admin Panel device (`com.admin.velorix`) when sending pushes to players.
 */
object VelorixNotificationManager {
    private const val TAG = "VelorixNotifMgr"

    const val CHANNEL_TOURNAMENTS = "velorix_tournaments_channel"
    const val CHANNEL_CAMPAIGNS = "velorix_campaigns_channel"
    const val CHANNEL_ADMIN = "velorix_admin_channel"

    // In-app real-time notification event bus (reserved only for incoming admin-specific events)
    private val _inAppNotificationEvents = MutableSharedFlow<AppNotification>(extraBufferCapacity = 64)
    val inAppNotificationEvents: SharedFlow<AppNotification> = _inAppNotificationEvents.asSharedFlow()

    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val adminChannel = NotificationChannel(
                CHANNEL_ADMIN,
                "Admin Operations",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Dispute resolutions, payment verifications, and system audits"
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(adminChannel)
            Log.d(TAG, "Admin notification channel initialized.")
        }
    }

    /**
     * Dispatches a tournament announcement push directly to the User Panel (`com.tournaments.velorix`).
     */
    fun dispatchTournamentCampaignNotification(
        context: Context,
        tournament: Tournament,
        customTitle: String? = null,
        customMessage: String? = null
    ): AppNotification {
        val notifId = "notif_${System.currentTimeMillis()}_${tournament.id.takeLast(4)}"
        val gameName = tournament.game
        val prizeStr = if (tournament.prizePool > 0) "₹${tournament.prizePool.toInt()}" else "Free / Points"
        val entryStr = if (tournament.entryFee > 0) "₹${tournament.entryFee.toInt()}" else "FREE ENTRY"

        val title = customTitle ?: "New Tournament Announced: ${tournament.title}"
        val message = customMessage ?: "$gameName • Prize Pool: $prizeStr • Entry: $entryStr • ${tournament.maxPlayers} Slots (${tournament.format}). Join now!"

        val appNotification = AppNotification(
            id = notifId,
            title = title,
            message = message,
            type = "TOURNAMENT",
            targetTournamentId = tournament.id,
            game = gameName,
            timestamp = System.currentTimeMillis(),
            priority = "HIGH",
            isRead = false
        )

        VelorixFcmManager.dispatchToUserPanel(
            notification = appNotification,
            audience = "ALL",
            targetTournamentId = tournament.id,
            extraData = mapOf(
                "tournament_id" to tournament.id,
                "tournament_title" to tournament.title,
                "prizePool" to tournament.prizePool,
                "entryFee" to tournament.entryFee
            ),
            pinToGlobalAnnouncements = true
        )

        return appNotification
    }

    /**
     * Dispatches notification to User Panel when a tournament turns LIVE.
     */
    fun dispatchTournamentLiveNotification(
        context: Context,
        tournament: Tournament
    ): AppNotification {
        val notifId = "live_${System.currentTimeMillis()}_${tournament.id.takeLast(4)}"
        val title = "TOURNAMENT IS LIVE: ${tournament.title}"
        val roomInfo = if (tournament.roomDetails?.roomId?.isNotBlank() == true) {
            "Room ID: ${tournament.roomDetails?.roomId} | Password: ${tournament.roomDetails?.roomPassword}"
        } else {
            "Match starting now! Join room promptly."
        }
        val message = "${tournament.game} match is NOW LIVE! $roomInfo"

        val appNotification = AppNotification(
            id = notifId,
            title = title,
            message = message,
            type = "TOURNAMENT",
            targetTournamentId = tournament.id,
            game = tournament.game,
            timestamp = System.currentTimeMillis(),
            priority = "URGENT",
            isRead = false
        )

        VelorixFcmManager.dispatchToUserPanel(
            notification = appNotification,
            audience = "TOURNAMENT",
            targetTournamentId = tournament.id,
            extraData = mapOf(
                "tournament_id" to tournament.id,
                "tournament_title" to tournament.title,
                "status" to "LIVE"
            ),
            pinToGlobalAnnouncements = true
        )

        return appNotification
    }

    /**
     * Dispatches notification to User Panel when Room ID & Password are published.
     */
    fun dispatchRoomCredentialsNotification(
        context: Context,
        tournament: Tournament,
        roomId: String,
        roomPass: String
    ): AppNotification {
        val notifId = "room_${System.currentTimeMillis()}_${tournament.id.takeLast(4)}"
        val title = "ROOM ID RELEASED: ${tournament.title}"
        val message = "Room ID: $roomId | Password: $roomPass. Join the custom room within 10 minutes!"

        val appNotification = AppNotification(
            id = notifId,
            title = title,
            message = message,
            type = "ROOM_DETAILS",
            targetTournamentId = tournament.id,
            game = tournament.game,
            timestamp = System.currentTimeMillis(),
            priority = "URGENT",
            isRead = false
        )

        VelorixFcmManager.dispatchToUserPanel(
            notification = appNotification,
            audience = "TOURNAMENT",
            targetTournamentId = tournament.id,
            extraData = mapOf(
                "room_id" to roomId,
                "roomId" to roomId,
                "room_password" to roomPass,
                "roomPassword" to roomPass,
                "tournament_id" to tournament.id,
                "tournament_title" to tournament.title
            ),
            pinToGlobalAnnouncements = true
        )

        return appNotification
    }

    /**
     * Dispatches notification to User Panel when tournament rules/specs are updated.
     */
    fun dispatchTournamentRulesUpdatedNotification(
        context: Context,
        tournament: Tournament
    ): AppNotification {
        val notifId = "rules_${System.currentTimeMillis()}_${tournament.id.takeLast(4)}"
        val title = "RULES UPDATED: ${tournament.title}"
        val message = "Tournament rules & weapon specs updated: ${tournament.allowedGuns.take(50)}"

        val appNotification = AppNotification(
            id = notifId,
            title = title,
            message = message,
            type = "TOURNAMENT",
            targetTournamentId = tournament.id,
            game = tournament.game,
            timestamp = System.currentTimeMillis(),
            priority = "MEDIUM",
            isRead = false
        )

        VelorixFcmManager.dispatchToUserPanel(
            notification = appNotification,
            audience = "TOURNAMENT",
            targetTournamentId = tournament.id,
            pinToGlobalAnnouncements = false
        )
        return appNotification
    }

    /**
     * Dispatches notification to User Panel when tournament completes.
     */
    fun dispatchTournamentCompletedNotification(
        context: Context,
        tournament: Tournament
    ): AppNotification {
        val notifId = "completed_${System.currentTimeMillis()}_${tournament.id.takeLast(4)}"
        val title = "MATCH COMPLETED: ${tournament.title}"
        val message = "Tournament has concluded! Submit your screenshot proof to claim prize rewards."

        val appNotification = AppNotification(
            id = notifId,
            title = title,
            message = message,
            type = "TOURNAMENT",
            targetTournamentId = tournament.id,
            game = tournament.game,
            timestamp = System.currentTimeMillis(),
            priority = "HIGH",
            isRead = false
        )

        VelorixFcmManager.dispatchToUserPanel(
            notification = appNotification,
            audience = "TOURNAMENT",
            targetTournamentId = tournament.id,
            pinToGlobalAnnouncements = false
        )
        return appNotification
    }

    /**
     * Dispatches notification to User Panel for match proof review.
     */
    fun dispatchMatchProofNotification(
        context: Context,
        username: String,
        prizeAmount: Double,
        approved: Boolean,
        tournamentTitle: String,
        targetUserId: String = ""
    ): AppNotification {
        val notifId = "proof_${System.currentTimeMillis()}"
        val title = if (approved) "Victory Proof Approved: $tournamentTitle" else "Match Proof Rejected"
        val message = if (approved) {
            "Congratulations $username! ₹${prizeAmount.toInt()} prize has been credited to your winning balance."
        } else {
            "Proof submitted by $username was reviewed and rejected. Contact support for dispute review."
        }

        val appNotification = AppNotification(
            id = notifId,
            title = title,
            message = message,
            type = "DISPUTE",
            targetTournamentId = "",
            game = "Esports",
            timestamp = System.currentTimeMillis(),
            priority = if (approved) "HIGH" else "MEDIUM",
            isRead = false
        )

        VelorixFcmManager.dispatchToUserPanel(
            notification = appNotification,
            audience = if (targetUserId.isNotBlank()) "USER" else "ALL",
            targetUserId = targetUserId,
            pinToGlobalAnnouncements = false
        )
        return appNotification
    }

    /**
     * Dispatches a Wallet / Payout Status notification strictly to the User Panel (`com.tournaments.velorix`).
     */
    fun dispatchPayoutNotification(
        context: Context,
        username: String,
        amount: Double,
        approved: Boolean,
        details: String,
        targetUserId: String = ""
    ): AppNotification {
        val notifId = "payout_${System.currentTimeMillis()}"
        val title = if (approved) "Payout Approved: ₹${amount.toInt()}" else "Payout Request Rejected"
        val message = if (approved) {
            "Your withdrawal of ₹${amount.toInt()} has been successfully processed to $details."
        } else {
            "Your withdrawal request of ₹${amount.toInt()} was rejected: $details. The funds have been refunded to your wallet."
        }

        val appNotification = AppNotification(
            id = notifId,
            title = title,
            message = message,
            type = "TRANSACTION_ALERT",
            targetTournamentId = "",
            game = "Velorix Wallet",
            timestamp = System.currentTimeMillis(),
            priority = "HIGH",
            isRead = false
        )

        VelorixFcmManager.dispatchToUserPanel(
            notification = appNotification,
            audience = if (targetUserId.isNotBlank()) "USER" else "ALL",
            targetUserId = targetUserId,
            extraData = mapOf(
                "username" to username,
                "amount" to amount.toString(),
                "status" to if (approved) "APPROVED" else "REJECTED",
                "details" to details
            ),
            pinToGlobalAnnouncements = false
        )

        return appNotification
    }

    /**
     * Dispatches a general Broadcast / Campaign notification strictly to the User Panel (`com.tournaments.velorix`).
     */
    fun dispatchCampaignBroadcast(
        context: Context,
        title: String,
        message: String,
        level: String = "HIGH",
        tournamentId: String? = null,
        targetUserId: String? = null
    ): AppNotification {
        val notifId = "camp_${System.currentTimeMillis()}"
        val appNotification = AppNotification(
            id = notifId,
            title = title,
            message = message,
            type = "CAMPAIGN",
            targetTournamentId = tournamentId ?: "",
            game = "Esports Campaign",
            timestamp = System.currentTimeMillis(),
            priority = level,
            isRead = false
        )

        VelorixFcmManager.dispatchToUserPanel(
            notification = appNotification,
            audience = when {
                !targetUserId.isNullOrBlank() -> "USER"
                !tournamentId.isNullOrBlank() -> "TOURNAMENT"
                else -> "ALL"
            },
            targetUserId = targetUserId,
            targetTournamentId = tournamentId,
            pinToGlobalAnnouncements = targetUserId.isNullOrBlank()
        )

        return appNotification
    }
}
