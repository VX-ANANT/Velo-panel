package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.domain.model.AppNotification
import com.example.domain.model.Tournament
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicInteger

object VelorixNotificationManager {
    private const val TAG = "VelorixNotifMgr"

    const val CHANNEL_TOURNAMENTS = "velorix_tournaments_channel"
    const val CHANNEL_CAMPAIGNS = "velorix_campaigns_channel"
    const val CHANNEL_ADMIN = "velorix_admin_channel"

    private val notificationIdCounter = AtomicInteger(1000)

    // In-app real-time notification event bus
    private val _inAppNotificationEvents = MutableSharedFlow<AppNotification>(extraBufferCapacity = 64)
    val inAppNotificationEvents: SharedFlow<AppNotification> = _inAppNotificationEvents.asSharedFlow()

    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val tourneyChannel = NotificationChannel(
                CHANNEL_TOURNAMENTS,
                "Tournaments & Matches",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Live tournament announcements, match schedules, and bracket updates"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 100, 250)
                setShowBadge(true)
            }

            val campaignChannel = NotificationChannel(
                CHANNEL_CAMPAIGNS,
                "Campaigns & Broadcasts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Promotional campaigns, urgent esports broadcasts, and server notices"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
                setShowBadge(true)
            }

            val adminChannel = NotificationChannel(
                CHANNEL_ADMIN,
                "Admin Operations",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Dispute resolutions, payment verifications, and system audits"
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(tourneyChannel)
            notificationManager.createNotificationChannel(campaignChannel)
            notificationManager.createNotificationChannel(adminChannel)
            Log.d(TAG, "Notification channels initialized successfully.")
        }
    }

    /**
     * Dispatches both a real System Push/Status Bar Notification and an In-App Heads-Up Banner
     * when a tournament is created, published, or announced.
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

        _inAppNotificationEvents.tryEmit(appNotification)

        try {
            val startTimeStr = tournament.startsAt ?: "Upcoming"
            sendSystemNotification(
                context = context,
                channelId = CHANNEL_TOURNAMENTS,
                notificationId = notificationIdCounter.incrementAndGet(),
                title = title,
                shortText = "$gameName • $prizeStr Prize • $entryStr",
                expandedText = "Game: $gameName (${tournament.format})\nPrize Pool: $prizeStr | Entry: $entryStr\nMax Slots: ${tournament.maxPlayers}\nSchedule: $startTimeStr\nTap to inspect tournament bracket and live status.",
                targetTournamentId = tournament.id
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed sending system notification: ${e.message}", e)
        }

        return appNotification
    }

    /**
     * Dispatches notification when a tournament turns LIVE.
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

        _inAppNotificationEvents.tryEmit(appNotification)

        try {
            sendSystemNotification(
                context = context,
                channelId = CHANNEL_TOURNAMENTS,
                notificationId = notificationIdCounter.incrementAndGet(),
                title = title,
                shortText = "Match is LIVE! Tap to get Room ID & Password.",
                expandedText = "${tournament.title} is now LIVE!\n$roomInfo\nDo not delay, match lobby starts shortly!",
                targetTournamentId = tournament.id
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed sending live system notification: ${e.message}", e)
        }

        return appNotification
    }

    /**
     * Dispatches notification when Room ID & Password are published.
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
            type = "TOURNAMENT",
            targetTournamentId = tournament.id,
            game = tournament.game,
            timestamp = System.currentTimeMillis(),
            priority = "URGENT",
            isRead = false
        )

        _inAppNotificationEvents.tryEmit(appNotification)

        try {
            sendSystemNotification(
                context = context,
                channelId = CHANNEL_TOURNAMENTS,
                notificationId = notificationIdCounter.incrementAndGet(),
                title = title,
                shortText = "Room ID: $roomId | Pass: $roomPass",
                expandedText = "${tournament.title}\nRoom ID: $roomId\nPassword: $roomPass\nRules: Join immediately to reserve your allocated slot!",
                targetTournamentId = tournament.id
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed sending room credentials notification: ${e.message}", e)
        }

        return appNotification
    }

    /**
     * Dispatches notification when tournament rules/specs are updated.
     */
    fun dispatchTournamentRulesUpdatedNotification(
        context: Context,
        tournament: Tournament
    ): AppNotification {
        val notifId = "rules_${System.currentTimeMillis()}_${tournament.id.takeLast(4)}"
        val title = "RULES UPDATED: ${tournament.title}"
        val message = "Tournament rules & weapon specs updated: ${tournament.allowedGuns.take(50)}..."

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

        _inAppNotificationEvents.tryEmit(appNotification)
        return appNotification
    }

    /**
     * Dispatches notification when tournament completes.
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

        _inAppNotificationEvents.tryEmit(appNotification)
        return appNotification
    }

    /**
     * Dispatches notification for match proof review.
     */
    fun dispatchMatchProofNotification(
        context: Context,
        username: String,
        prizeAmount: Double,
        approved: Boolean,
        tournamentTitle: String
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

        _inAppNotificationEvents.tryEmit(appNotification)
        return appNotification
    }

    /**
     * Dispatches a Wallet / Payout Status notification (Push + In-App).
     */
    fun dispatchPayoutNotification(
        context: Context,
        username: String,
        amount: Double,
        approved: Boolean,
        details: String
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
            type = "PAYOUT",
            targetTournamentId = "",
            game = "Velorix Wallet",
            timestamp = System.currentTimeMillis(),
            priority = "HIGH",
            isRead = false
        )

        // 1. In-app banner
        _inAppNotificationEvents.tryEmit(appNotification)

        // 2. Android Status Bar System Notification
        try {
            sendSystemNotification(
                context = context,
                channelId = CHANNEL_CAMPAIGNS,
                notificationId = notificationIdCounter.incrementAndGet(),
                title = title,
                shortText = message.take(80),
                expandedText = message,
                targetTournamentId = null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed sending payout notification: ${e.message}", e)
        }

        return appNotification
    }

    /**
     * Dispatches a general Broadcast / Campaign notification (System Push + In-App).
     */
    fun dispatchCampaignBroadcast(
        context: Context,
        title: String,
        message: String,
        level: String = "HIGH",
        tournamentId: String? = null
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

        // 1. In-app banner
        _inAppNotificationEvents.tryEmit(appNotification)

        // 2. Android Status Bar System Notification
        try {
            sendSystemNotification(
                context = context,
                channelId = CHANNEL_CAMPAIGNS,
                notificationId = notificationIdCounter.incrementAndGet(),
                title = title,
                shortText = message.take(80),
                expandedText = message,
                targetTournamentId = tournamentId
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed sending campaign system notification: ${e.message}", e)
        }

        return appNotification
    }

    private fun sendSystemNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        shortText: String,
        expandedText: String,
        targetTournamentId: String? = null
    ) {
        initChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("tournament_id", targetTournamentId)
            putExtra("opened_from_notification", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Use standard system app icon
        val smallIcon = R.mipmap.ic_launcher

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(shortText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(expandedText)
            )
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(0, 250, 100, 250))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(notificationId, notificationBuilder.build())
        Log.d(TAG, "Dispatched system notification #$notificationId: $title")
    }
}
