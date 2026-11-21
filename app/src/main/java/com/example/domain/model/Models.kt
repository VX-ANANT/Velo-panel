package com.example.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

@Serializable
enum class WalletBucket(
    val key: String,
    val displayLabel: String,
    val shortLabel: String,
    val isWithdrawable: Boolean
) {
    DEPOSIT("deposit", "Deposited Money (Non-Withdrawable)", "Deposited Money", false),
    WINNING("winning", "Winning Money (Withdrawable)", "Winning Money", true),
    BONUS("bonus", "Referral Bonus (Non-Withdrawable)", "Referral Bonus", false);

    val displayName: String get() = displayLabel
}

@IgnoreExtraProperties
@Serializable
data class PlayerMatchRecord(
    @get:PropertyName("tournamentId") @set:PropertyName("tournamentId")
    var tournamentId: String = "",
    @get:PropertyName("tournamentTitle") @set:PropertyName("tournamentTitle")
    var tournamentTitle: String = "",
    @get:PropertyName("game") @set:PropertyName("game")
    var game: String = "Free Fire",
    @get:PropertyName("category") @set:PropertyName("category")
    var category: String = "BR",
    @get:PropertyName("format") @set:PropertyName("format")
    var format: String = "SOLO",
    @get:PropertyName("map") @set:PropertyName("map")
    var map: String = "Bermuda",
    @get:PropertyName("joinedAt") @set:PropertyName("joinedAt")
    var joinedAt: Long = 0L,
    @get:PropertyName("entryFeePaid") @set:PropertyName("entryFeePaid")
    var entryFeePaid: Double = 0.0,
    @get:PropertyName("paidFromDeposit") @set:PropertyName("paidFromDeposit")
    var paidFromDeposit: Double = 0.0,
    @get:PropertyName("paidFromWinning") @set:PropertyName("paidFromWinning")
    var paidFromWinning: Double = 0.0,
    @get:PropertyName("paidFromBonus") @set:PropertyName("paidFromBonus")
    var paidFromBonus: Double = 0.0,
    @get:PropertyName("kills") @set:PropertyName("kills")
    var kills: Int = 0,
    @get:PropertyName("rank") @set:PropertyName("rank")
    var rank: Int = 0,
    @get:PropertyName("winningAmount") @set:PropertyName("winningAmount")
    var winningAmount: Double = 0.0,
    @get:PropertyName("status") @set:PropertyName("status")
    var status: String = "JOINED" // JOINED, PLAYED, WON, REFUNDED
) {
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val netProfit: Double get() = winningAmount - entryFeePaid
}

@IgnoreExtraProperties
@Serializable
data class UserProfile(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    @get:PropertyName("username") @set:PropertyName("username")
    var username: String = "",
    @get:PropertyName("avatar_url") @set:PropertyName("avatar_url")
    var avatarUrl: String? = null,
    @get:PropertyName("role") @set:PropertyName("role")
    var role: String = "player",
    @get:PropertyName("email") @set:PropertyName("email")
    var email: String = "",
    @get:PropertyName("depositFunds") @set:PropertyName("depositFunds")
    var depositFunds: Double = 0.0,
    @get:PropertyName("winningFunds") @set:PropertyName("winningFunds")
    var winningFunds: Double = 0.0,
    @get:PropertyName("bonusFunds") @set:PropertyName("bonusFunds")
    var bonusFunds: Double = 0.0,
    @get:PropertyName("funds") @set:PropertyName("funds")
    var funds: Double = 0.0,
    @get:PropertyName("wins") @set:PropertyName("wins")
    var wins: Int = 0,
    @get:PropertyName("kills") @set:PropertyName("kills")
    var kills: Int = 0,
    @get:PropertyName("activityPoints") @set:PropertyName("activityPoints")
    var activityPoints: Int = 0,
    @get:PropertyName("totalEarnings") @set:PropertyName("totalEarnings")
    var totalEarnings: Double = 0.0,
    @get:PropertyName("isBanned") @set:PropertyName("isBanned")
    var isBanned: Boolean = false,
    @get:PropertyName("lastActive") @set:PropertyName("lastActive")
    var lastActive: Long = 0L,
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Long = 0L,
    @get:PropertyName("firstAppJoinAt") @set:PropertyName("firstAppJoinAt")
    var firstAppJoinAt: Long = 0L,
    @get:PropertyName("firstMatchJoinedAt") @set:PropertyName("firstMatchJoinedAt")
    var firstMatchJoinedAt: Long = 0L,
    @get:PropertyName("lastMatchJoinedAt") @set:PropertyName("lastMatchJoinedAt")
    var lastMatchJoinedAt: Long = 0L,
    @get:PropertyName("totalMatchesJoined") @set:PropertyName("totalMatchesJoined")
    var totalMatchesJoined: Int = 0,
    @get:PropertyName("totalDeposited") @set:PropertyName("totalDeposited")
    var totalDeposited: Double = 0.0,
    @get:PropertyName("totalWithdrawn") @set:PropertyName("totalWithdrawn")
    var totalWithdrawn: Double = 0.0,
    @get:PropertyName("totalEntryFeesPaid") @set:PropertyName("totalEntryFeesPaid")
    var totalEntryFeesPaid: Double = 0.0,
    @get:PropertyName("supportCasesCount") @set:PropertyName("supportCasesCount")
    var supportCasesCount: Int = 0,
    @get:PropertyName("joinedMatchIds") @set:PropertyName("joinedMatchIds")
    var joinedMatchIds: List<String> = emptyList(),
    @get:PropertyName("matchHistory") @set:PropertyName("matchHistory")
    var matchHistory: List<PlayerMatchRecord> = emptyList(),
    @get:PropertyName("gameId") @set:PropertyName("gameId")
    var gameId: String = "",
    @get:PropertyName("ign") @set:PropertyName("ign")
    var ign: String = "",
    @get:PropertyName("balance") @set:PropertyName("balance")
    var balance: Double = 0.0,
    @get:PropertyName("tokens") @set:PropertyName("tokens")
    var tokens: Int = 0,
    @get:PropertyName("loginStreak") @set:PropertyName("loginStreak")
    var loginStreak: Int = 0,
    @get:PropertyName("lastLoginClaimDate") @set:PropertyName("lastLoginClaimDate")
    var lastLoginClaimDate: String = "",
    @get:PropertyName("lastMissionClaimDate") @set:PropertyName("lastMissionClaimDate")
    var lastMissionClaimDate: String = "",
    @get:PropertyName("dailyMissionsTokensClaimed") @set:PropertyName("dailyMissionsTokensClaimed")
    var dailyMissionsTokensClaimed: Int = 0,
    @get:PropertyName("phoneOrEmail") @set:PropertyName("phoneOrEmail")
    var phoneOrEmail: String = "",
    @get:PropertyName("referralCode") @set:PropertyName("referralCode")
    var referralCode: String = "",
    @get:PropertyName("referredBy") @set:PropertyName("referredBy")
    var referredBy: String = "",
    @get:PropertyName("referralCount") @set:PropertyName("referralCount")
    var referralCount: Int = 0,
    @get:PropertyName("referralBonusEarned") @set:PropertyName("referralBonusEarned")
    var referralBonusEarned: Double = 0.0,
    @get:PropertyName("phone") @set:PropertyName("phone")
    var phone: String = "",
    @get:PropertyName("banReason") @set:PropertyName("banReason")
    var banReason: String = "",
    @get:PropertyName("banType") @set:PropertyName("banType")
    var banType: String = "",
    @get:PropertyName("banCaseId") @set:PropertyName("banCaseId")
    var banCaseId: String = "",
    @get:PropertyName("bannedAt") @set:PropertyName("bannedAt")
    var bannedAt: Long = 0L,
    @get:PropertyName("isSuspended") @set:PropertyName("isSuspended")
    var isSuspended: Boolean = false,
    @get:PropertyName("suspendedUntil") @set:PropertyName("suspendedUntil")
    var suspendedUntil: Long = 0L,
    @get:PropertyName("suspensionReason") @set:PropertyName("suspensionReason")
    var suspensionReason: String = "",
    @get:PropertyName("suspendReason") @set:PropertyName("suspendReason")
    var suspendReason: String = "",
    @get:PropertyName("status") @set:PropertyName("status")
    var status: String = "ACTIVE",
    @get:PropertyName("isVpnBlocked") @set:PropertyName("isVpnBlocked")
    var isVpnBlocked: Boolean = false,
    @get:PropertyName("isForceUpdateRequired") @set:PropertyName("isForceUpdateRequired")
    var isForceUpdateRequired: Boolean = false,
    @get:PropertyName("minVersionRequired") @set:PropertyName("minVersionRequired")
    var minVersionRequired: String = "",
    @get:PropertyName("isMaintenanceBypass") @set:PropertyName("isMaintenanceBypass")
    var isMaintenanceBypass: Boolean = false,
    @get:PropertyName("deviceModel") @set:PropertyName("deviceModel")
    var deviceModel: String = "",
    @get:PropertyName("ipAddress") @set:PropertyName("ipAddress")
    var ipAddress: String = "",
    @get:PropertyName("dateOfBirth") @set:PropertyName("dateOfBirth")
    var dateOfBirth: String = "",
    @get:PropertyName("age") @set:PropertyName("age")
    var age: Int = 0,
    @get:PropertyName("isUnder18") @set:PropertyName("isUnder18")
    var isUnder18: Boolean = false,
    @get:PropertyName("eligibleForCashTournaments") @set:PropertyName("eligibleForCashTournaments")
    var eligibleForCashTournaments: Boolean = true,
    @get:PropertyName("ageConfirmed") @set:PropertyName("ageConfirmed")
    var ageConfirmed: Boolean = false,
    @get:PropertyName("isGuest") @set:PropertyName("isGuest")
    var isGuest: Boolean = false,
    @get:PropertyName("isAnonymous") @set:PropertyName("isAnonymous")
    var isAnonymous: Boolean = false,
    @get:PropertyName("bio") @set:PropertyName("bio")
    var bio: String = "",
    @get:PropertyName("socialLink") @set:PropertyName("socialLink")
    var socialLink: String = "",
    @get:PropertyName("lockedBalance") @set:PropertyName("lockedBalance")
    var lockedBalance: Double = 0.0,
    @get:PropertyName("kycVerified") @set:PropertyName("kycVerified")
    var kycVerified: Boolean = false,
    @get:PropertyName("dataExported") @set:PropertyName("dataExported")
    var dataExported: Boolean = false,
    @get:PropertyName("lastExportedAt") @set:PropertyName("lastExportedAt")
    var lastExportedAt: Long = 0L,
    @get:PropertyName("lastExportId") @set:PropertyName("lastExportId")
    var lastExportId: String = "",
    @get:PropertyName("accountStatus") @set:PropertyName("accountStatus")
    var accountStatus: String = "ACTIVE",
    @get:PropertyName("deletionRequested") @set:PropertyName("deletionRequested")
    var deletionRequested: Boolean = false,
    @get:PropertyName("deletionRequestId") @set:PropertyName("deletionRequestId")
    var deletionRequestId: String = "",
    @get:PropertyName("deletionReason") @set:PropertyName("deletionReason")
    var deletionReason: String = "",
    @get:PropertyName("deletionDetails") @set:PropertyName("deletionDetails")
    var deletionDetails: String = "",
    @get:PropertyName("deletionRequestedAt") @set:PropertyName("deletionRequestedAt")
    var deletionRequestedAt: Long = 0L,
    @get:PropertyName("isFounder") @set:PropertyName("isFounder")
    var isFounder: Boolean = false,
    @get:PropertyName("founderTier") @set:PropertyName("founderTier")
    var founderTier: String = "",
    @get:PropertyName("version") @set:PropertyName("version")
    var version: Int = 1,
    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt")
    var updatedAt: Long = 0L,
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    var rawAttributes: Map<String, String> = emptyMap()
) {
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val depositBalance: Double get() = depositFunds
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val winningsBalance: Double get() = winningFunds
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val bonusBalance: Double get() = bonusFunds
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val inGameName: String get() = effectiveIgn
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val freeFireId: String get() = gameId
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val referralEarnings: Double get() = maxOf(referralBonusEarned, bonusFunds)
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val suspensionEndTimeMillis: Long get() = suspendedUntil
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val isGuestSession: Boolean get() {
        val idLower = id.trim().lowercase()
        return idLower.startsWith("mock_guest_") || idLower.startsWith("temp_guest_")
    }

    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val isMinor: Boolean get() = isUnder18 || (age in 1..17)
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val canJoinCashTournaments: Boolean get() = !isMinor && eligibleForCashTournaments
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val totalWalletBalance: Double get() = when {
        (depositFunds + winningFunds + bonusFunds) > 0.0 -> depositFunds + winningFunds + bonusFunds
        balance > 0.0 -> balance
        else -> funds
    }
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val walletBalance: Double get() = totalWalletBalance
    /**
     * Strictly ONLY Winning Money is withdrawable. Deposited Money and Bonus Money cannot be withdrawn.
     */
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val withdrawableBalance: Double get() = winningFunds.coerceAtLeast(0.0)
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val nonWithdrawableBalance: Double get() = (depositFunds + bonusFunds).coerceAtLeast(0.0)
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val canRequestWithdrawal: Boolean get() = !isBanned && !isSuspended && withdrawableBalance >= 50.0
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val effectiveFirstAppJoinAt: Long get() = if (firstAppJoinAt > 0L) firstAppJoinAt else createdAt
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val effectiveIgn: String get() = when {
        ign.isNotBlank() && ign != "Player" -> ign
        username.isNotBlank() && username != "Player" && !username.startsWith("Player (") && !username.contains("@") -> username
        gameId.isNotBlank() -> "FF ID: $gameId"
        else -> ign.ifBlank { username.ifBlank { "Player" } }
    }
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val bestDisplayName: String
        get() = when {
            username.isNotBlank() && username != "Player" && !username.startsWith("Player (") && !username.contains("@") -> username
            ign.isNotBlank() && ign != "Player" && !ign.startsWith("Player (") -> ign
            email.isNotBlank() && email.contains("@") -> email.substringBefore("@")
            gameId.isNotBlank() -> "FF ID: $gameId"
            phone.isNotBlank() -> "Player (${phone.takeLast(4)})"
            phoneOrEmail.isNotBlank() && !phoneOrEmail.contains("@") -> "Player (${phoneOrEmail.takeLast(4)})"
            username.isNotBlank() && username != "Player" -> username
            ign.isNotBlank() -> ign
            id.isNotBlank() && !id.startsWith("usr_") && !id.startsWith("acc_") -> "Player (${id.take(6)})"
            else -> "Player"
        }
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val bestEmailOrPhone: String
        get() = when {
            email.isNotBlank() && email.contains("@") -> email
            phoneOrEmail.isNotBlank() && phoneOrEmail.contains("@") -> phoneOrEmail
            phone.isNotBlank() -> phone
            phoneOrEmail.isNotBlank() -> phoneOrEmail
            email.isNotBlank() -> email
            gameId.isNotBlank() -> "FF UID: $gameId"
            id.isNotBlank() -> "UID: ${id.take(10)}"
            else -> "No email/phone linked"
        }
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val matchesWon: Int get() = wins
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val totalKills: Int get() = kills
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val matchesPlayed: Int get() = if (totalMatchesJoined > 0) totalMatchesJoined else (wins + activityPoints)
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val phoneNumber: String get() = phone.ifBlank { if (!phoneOrEmail.contains("@")) phoneOrEmail else "" }
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val gameAccountId: String get() = gameId
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val uid: String get() = id
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val gameUsername: String get() = bestDisplayName
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val freeFireUid: String get() = gameId
}

@IgnoreExtraProperties
@Serializable
data class AppAnnouncementBanner(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    @get:PropertyName("title") @set:PropertyName("title")
    var title: String = "",
    @get:PropertyName("subtitle") @set:PropertyName("subtitle")
    var subtitle: String = "",
    @get:PropertyName("message") @set:PropertyName("message")
    var message: String = "",
    @get:PropertyName("content") @set:PropertyName("content")
    var content: String = "",
    @get:PropertyName("imageUrl") @set:PropertyName("imageUrl")
    var imageUrl: String = "",
    @get:PropertyName("actionUrl") @set:PropertyName("actionUrl")
    var actionUrl: String = "",
    @get:PropertyName("targetUrl") @set:PropertyName("targetUrl")
    var targetUrl: String = "",
    @get:PropertyName("bannerType") @set:PropertyName("bannerType")
    var bannerType: String = "NEWS", // NEWS, PROMO, ALERT, UPDATE
    @get:PropertyName("badgeText") @set:PropertyName("badgeText")
    var badgeText: String = "OFFICIAL",
    @get:PropertyName("ctaText") @set:PropertyName("ctaText")
    var ctaText: String = "JOIN NOW",
    @get:PropertyName("actionType") @set:PropertyName("actionType")
    var actionType: String = "TOURNAMENT",
    @get:PropertyName("targetId") @set:PropertyName("targetId")
    var targetId: String = "",
    @get:PropertyName("gradientTheme") @set:PropertyName("gradientTheme")
    var gradientTheme: String = "INDIGO_VIOLET",
    @get:PropertyName("isActive") @set:PropertyName("isActive")
    var isActive: Boolean = true,
    @get:PropertyName("priority") @set:PropertyName("priority")
    var priority: Int = 1,
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Long = System.currentTimeMillis()
)

typealias CashoutRequest = PayoutRequest

@IgnoreExtraProperties
@Serializable
data class BannedUserRecord(
    @get:PropertyName("uid") @set:PropertyName("uid")
    var uid: String = "",
    @get:PropertyName("email") @set:PropertyName("email")
    var email: String = "",
    @get:PropertyName("reason") @set:PropertyName("reason")
    var reason: String = "",
    @get:PropertyName("bannedAt") @set:PropertyName("bannedAt")
    var bannedAt: Long = System.currentTimeMillis(),
    @get:PropertyName("bannedBy") @set:PropertyName("bannedBy")
    var bannedBy: String = ""
)

@IgnoreExtraProperties
@Serializable
data class AdminRecord(
    @get:PropertyName("uid") @set:PropertyName("uid")
    var uid: String = "",
    @get:PropertyName("name") @set:PropertyName("name")
    var name: String = "",
    @get:PropertyName("email") @set:PropertyName("email")
    var email: String = "",
    @get:PropertyName("role") @set:PropertyName("role")
    var role: String = "support_admin", // super_admin, tournament_admin, support_admin
    @get:PropertyName("active") @set:PropertyName("active")
    var active: Boolean = true,
    @get:PropertyName("assignedAt") @set:PropertyName("assignedAt")
    var assignedAt: Long = System.currentTimeMillis(),
    @get:PropertyName("grantedBy") @set:PropertyName("grantedBy")
    var grantedBy: String = "",
    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt")
    var updatedAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
@Serializable
data class SupportTicket(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    @get:PropertyName("userId") @set:PropertyName("userId")
    var rawUserId: String = "",
    @get:PropertyName("userEmail") @set:PropertyName("userEmail")
    var userEmail: String = "",
    @get:PropertyName("username") @set:PropertyName("username")
    var username: String = "",
    @get:PropertyName("gameId") @set:PropertyName("gameId")
    var gameId: String = "",
    @get:PropertyName("category") @set:PropertyName("category")
    var category: String = "",
    @get:PropertyName("issueCategory") @set:PropertyName("issueCategory")
    var issueCategory: String = "General", // PAYMENT_ISSUE, TOURNAMENT_DISPUTE, BUG_REPORT, HACKER_REPORT, GENERAL_SUPPORT
    @get:PropertyName("subject") @set:PropertyName("subject")
    var subject: String = "",
    @get:PropertyName("description") @set:PropertyName("description")
    var description: String = "",
    @get:PropertyName("screenshotUrl") @set:PropertyName("screenshotUrl")
    var screenshotUrl: String = "",
    @get:PropertyName("status") @set:PropertyName("status")
    var status: String = "open", // OPEN, IN_PROGRESS, RESOLVED, CLOSED
    @get:PropertyName("adminNote") @set:PropertyName("adminNote")
    var adminNote: String = "",
    @get:PropertyName("adminReply") @set:PropertyName("adminReply")
    var adminReply: String = "",
    @get:PropertyName("repliedAt") @set:PropertyName("repliedAt")
    var repliedAt: Long = 0L,
    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt")
    var updatedAt: Long = System.currentTimeMillis(),
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Long = System.currentTimeMillis(),
    @get:PropertyName("timestamp") @set:PropertyName("timestamp")
    var timestamp: Long = System.currentTimeMillis(),
    @get:PropertyName("assignedTo") @set:PropertyName("assignedTo")
    var assignedTo: String = "",
    @get:PropertyName("tournamentTitle") @set:PropertyName("tournamentTitle")
    var tournamentTitle: String = "",
    @get:PropertyName("isHighPriority") @set:PropertyName("isHighPriority")
    var isHighPriority: Boolean = false
) {
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val userId: String get() = rawUserId.ifBlank { userEmail }
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val messageText: String get() = description
}

@IgnoreExtraProperties
@Serializable
data class TicketMessage(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    @get:PropertyName("ticketId") @set:PropertyName("ticketId")
    var ticketId: String = "",
    @get:PropertyName("senderId") @set:PropertyName("senderId")
    var senderId: String = "",
    @get:PropertyName("senderName") @set:PropertyName("senderName")
    var senderName: String = "",
    @get:PropertyName("senderRole") @set:PropertyName("senderRole")
    var senderRole: String = "ADMIN", // "ADMIN" or "USER"
    @get:PropertyName("message") @set:PropertyName("message")
    var message: String = "",
    @get:PropertyName("timestamp") @set:PropertyName("timestamp")
    var timestamp: Long = System.currentTimeMillis()
) {
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val messageText: String get() = message
}

// Legacy alias to keep existing components functioning
typealias ComplaintTicket = SupportTicket

@IgnoreExtraProperties
@Serializable
data class PayoutRequest(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    @get:PropertyName("uid") @set:PropertyName("uid")
    var uid: String = "",
    @get:PropertyName("username") @set:PropertyName("username")
    var username: String = "",
    @get:PropertyName("email") @set:PropertyName("email")
    var email: String = "",
    @get:PropertyName("paymentMethod") @set:PropertyName("paymentMethod")
    var paymentMethod: String = "UPI", // UPI, Paytm, Bank Transfer
    @get:PropertyName("paymentId") @set:PropertyName("paymentId")
    var paymentId: String = "", // UPI ID / Phone / A/C
    @get:PropertyName("vtAmount") @set:PropertyName("vtAmount")
    var vtAmount: Double = 0.0,
    @get:PropertyName("status") @set:PropertyName("status")
    var status: String = "pending", // pending, approved, completed, rejected
    @get:PropertyName("rejectionReason") @set:PropertyName("rejectionReason")
    var rejectionReason: String = "",
    @get:PropertyName("requestedAt") @set:PropertyName("requestedAt")
    var requestedAt: Long = System.currentTimeMillis(),
    @get:PropertyName("processedAt") @set:PropertyName("processedAt")
    var processedAt: Long = 0L,
    @get:PropertyName("processedBy") @set:PropertyName("processedBy")
    var processedBy: String = "",
    @get:PropertyName("isDeposit") @set:PropertyName("isDeposit")
    var isDeposit: Boolean = false
) {
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val userId: String get() = uid
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val amount: Double get() = vtAmount
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val upiId: String get() = paymentId
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val createdAt: Long get() = requestedAt
}

@IgnoreExtraProperties
@Serializable
data class DepositRequest(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    @get:PropertyName("requestId") @set:PropertyName("requestId")
    var requestId: String = "",
    @get:PropertyName("transactionId") @set:PropertyName("transactionId")
    var transactionId: String = "",
    @get:PropertyName("uid") @set:PropertyName("uid")
    var uid: String = "",
    @get:PropertyName("userId") @set:PropertyName("userId")
    var userId: String = "",
    @get:PropertyName("username") @set:PropertyName("username")
    var username: String = "",
    @get:PropertyName("userName") @set:PropertyName("userName")
    var userName: String = "",
    @get:PropertyName("userEmail") @set:PropertyName("userEmail")
    var userEmail: String = "",
    @get:PropertyName("amount") @set:PropertyName("amount")
    var amount: Double = 0.0,
    @get:PropertyName("utrNumber") @set:PropertyName("utrNumber")
    var utrNumber: String = "",
    @get:PropertyName("paymentMethod") @set:PropertyName("paymentMethod")
    var paymentMethod: String = "UPI_MANUAL", // UPI_MANUAL | SPACEPAY | RAZORPAY
    @get:PropertyName("paymentScreenshotUrl") @set:PropertyName("paymentScreenshotUrl")
    var paymentScreenshotUrl: String = "",
    @get:PropertyName("screenshotUrl") @set:PropertyName("screenshotUrl")
    var screenshotUrl: String = "",
    @get:PropertyName("status") @set:PropertyName("status")
    var status: String = "PENDING", // PENDING, APPROVED, REJECTED
    @get:PropertyName("adminNote") @set:PropertyName("adminNote")
    var adminNote: String = "",
    @get:PropertyName("requestedAt") @set:PropertyName("requestedAt")
    var requestedAt: Long = System.currentTimeMillis(),
    @get:PropertyName("processedAt") @set:PropertyName("processedAt")
    var processedAt: Long = 0L,
    @get:PropertyName("processedBy") @set:PropertyName("processedBy")
    var processedBy: String = "",
    @get:PropertyName("timestamp") @set:PropertyName("timestamp")
    var timestamp: Long = System.currentTimeMillis()
) {
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val effectiveUserId: String get() = userId.ifBlank { uid }
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val createdAt: Long get() = if (requestedAt > 0L) requestedAt else timestamp
}

@IgnoreExtraProperties
@Serializable
data class WalletTransaction(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    @get:PropertyName("uid") @set:PropertyName("uid")
    var uid: String = "",
    @get:PropertyName("type") @set:PropertyName("type")
    var type: String = "DEPOSIT", // DEPOSIT, CASHOUT, REFUND, BONUS, PRIZE
    @get:PropertyName("amount") @set:PropertyName("amount")
    var amount: Double = 0.0,
    @get:PropertyName("description") @set:PropertyName("description")
    var description: String = "",
    @get:PropertyName("timestamp") @set:PropertyName("timestamp")
    var timestamp: Long = System.currentTimeMillis(),
    @get:PropertyName("status") @set:PropertyName("status")
    var status: String = "COMPLETED"
)

@IgnoreExtraProperties
@Serializable
data class RoomDetails(
    @get:PropertyName("roomId") @set:PropertyName("roomId")
    var roomId: String = "",
    @get:PropertyName("roomPassword") @set:PropertyName("roomPassword")
    var roomPassword: String = "",
    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt")
    var updatedAt: Long = System.currentTimeMillis()
) {
    @get:com.google.firebase.database.Exclude
    @get:com.google.firebase.firestore.Exclude
    val password: String get() = roomPassword
}

@IgnoreExtraProperties
@Serializable
data class Tournament(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    @get:PropertyName("displayTournamentId") @set:PropertyName("displayTournamentId")
    var displayTournamentId: String = "",
    @get:PropertyName("title") @set:PropertyName("title")
    var title: String = "",
    @get:PropertyName("bannerUrl") @set:PropertyName("bannerUrl")
    var bannerUrl: String = "",
    @get:PropertyName("game") @set:PropertyName("game")
    var game: String = "Free Fire",
    @get:PropertyName("category") @set:PropertyName("category")
    var category: String = "BR", // BR (Battle Royale / Full Map), CS (Clash Squad), LONE_WOLF (Lone Wolf), SCRIMS (Training / Practice)
    @get:PropertyName("matchCategory") @set:PropertyName("matchCategory")
    var matchCategory: String = "BATTLE_ROYALE", // BATTLE_ROYALE, CLASH_SQUAD, LONE_WOLF, CUSTOM_ROOM
    @get:PropertyName("matchMode") @set:PropertyName("matchMode")
    var matchMode: String = "PER_KILL", // PER_KILL, SURVIVAL, HEADSHOT_ONLY, BODY_DAMAGE_ON, SNIPER_ONLY, LIMITED_AMMO, UNLIMITED_AMMO, PISTOL_ONLY
    @get:PropertyName("customRuleBadge") @set:PropertyName("customRuleBadge")
    var customRuleBadge: String = "STANDARD RULES",
    @get:PropertyName("perspective") @set:PropertyName("perspective")
    var perspective: String = "TPP", // TPP, FPP
    @get:PropertyName("map") @set:PropertyName("map")
    var map: String = "Bermuda", // Bermuda, Purgatory, Kalahari, Alpine, NexTerra
    @get:PropertyName("entryFee") @set:PropertyName("entryFee")
    var entryFee: Float = 0f,
    @get:PropertyName("prizePool") @set:PropertyName("prizePool")
    var prizePool: Float = 0f,
    @get:PropertyName("registeredPlayers") @set:PropertyName("registeredPlayers")
    var registeredPlayers: Int = 0,
    @get:PropertyName("maxPlayers") @set:PropertyName("maxPlayers")
    var maxPlayers: Int = 48,
    @get:PropertyName("format") @set:PropertyName("format")
    var format: String = "SOLO", // SOLO, DUO, SQUAD, 1v1 CS, 4v4 CS
    @get:PropertyName("status") @set:PropertyName("status")
    var status: String = "UPCOMING", // UPCOMING, LIVE, COMPLETED, CANCELLED
    @get:PropertyName("startsAt") @set:PropertyName("startsAt")
    var startsAt: String? = null,
    @get:PropertyName("endsAt") @set:PropertyName("endsAt")
    var endsAt: String? = null,
    @get:PropertyName("description") @set:PropertyName("description")
    var description: String = "Official Velorix Free Fire Competitive Tournament. Compete against top players, climb the leaderboards, and claim the cash prize pool.",
    @get:PropertyName("rules") @set:PropertyName("rules")
    var rules: String = "1. Players must join the custom room within 10 minutes of Room ID release.\n2. Hacks, scripts, emulators (unless allowed), and teaming are strictly prohibited and result in permanent ban.\n3. Take screenshot of final kill/rank leaderboard as proof of match victory.\n4. Dispute window is 15 minutes after match completion.",
    
    // Weapon & Gun Rules
    @get:PropertyName("allowedGuns") @set:PropertyName("allowedGuns")
    var allowedGuns: String = "All Standard Weapons Allowed (AR, SMG, Shotguns, Snipers)",
    @get:PropertyName("bannedGuns") @set:PropertyName("bannedGuns")
    var bannedGuns: String = "M79 Grenade Launcher, M82B, Crossbow, Flash Freeze",
    @get:PropertyName("gunAttributesAllowed") @set:PropertyName("gunAttributesAllowed")
    var gunAttributesAllowed: Boolean = false, // Gun Skin Attributes On / Off
    @get:PropertyName("limitedAmmo") @set:PropertyName("limitedAmmo")
    var limitedAmmo: Boolean = true, // Limited Ammo Yes/No
    
    // Character Skill Rules
    @get:PropertyName("characterSkillsAllowed") @set:PropertyName("characterSkillsAllowed")
    var characterSkillsAllowed: Boolean = true,
    @get:PropertyName("allowedSkills") @set:PropertyName("allowedSkills")
    var allowedSkills: String = "All Standard Active & Passive Skills (Alok, Tatsuya, Kelly, Hayato, Moco)",
    @get:PropertyName("bannedSkills") @set:PropertyName("bannedSkills")
    var bannedSkills: String = "Chrono, Dimitry (Or none if all allowed)",
    
    // Game & Device Rules
    @get:PropertyName("emulatorAllowed") @set:PropertyName("emulatorAllowed")
    var emulatorAllowed: Boolean = false, // PC / Emulator Allowed (Mobile Only by default)
    @get:PropertyName("roofCampingAllowed") @set:PropertyName("roofCampingAllowed")
    var roofCampingAllowed: Boolean = false,
    @get:PropertyName("airdropAllowed") @set:PropertyName("airdropAllowed")
    var airdropAllowed: Boolean = true,
    @get:PropertyName("vehiclesAllowed") @set:PropertyName("vehiclesAllowed")
    var vehiclesAllowed: Boolean = true,
    @get:PropertyName("loadoutAllowed") @set:PropertyName("loadoutAllowed")
    var loadoutAllowed: Boolean = true,
    @get:PropertyName("allowedCharacters") @set:PropertyName("allowedCharacters")
    var allowedCharacters: String = "All Standard Characters Allowed",
    @get:PropertyName("bannedCharacters") @set:PropertyName("bannedCharacters")
    var bannedCharacters: String = "None",
    @get:PropertyName("headshotOnly") @set:PropertyName("headshotOnly")
    var headshotOnly: Boolean = false,
    @get:PropertyName("revivalAllowed") @set:PropertyName("revivalAllowed")
    var revivalAllowed: Boolean = false,
    @get:PropertyName("fallDamage") @set:PropertyName("fallDamage")
    var fallDamage: Boolean = true,
    @get:PropertyName("safeZoneShrinkSpeed") @set:PropertyName("safeZoneShrinkSpeed")
    var safeZoneShrinkSpeed: String = "Normal",
    @get:PropertyName("customMatchSettings") @set:PropertyName("customMatchSettings")
    var customMatchSettings: String = "HP: 200 | EP: 200 | Jump: 100% | Movement: 100% | Gloo Wall Limit: 3",
    @get:PropertyName("rulesModifiedAt") @set:PropertyName("rulesModifiedAt")
    var rulesModifiedAt: Long = 0L,
    @get:PropertyName("rulesModifiedBy") @set:PropertyName("rulesModifiedBy")
    var rulesModifiedBy: String = "",

    // Prize Breakdown Graph / Distribution
    @get:PropertyName("firstPlacePrize") @set:PropertyName("firstPlacePrize")
    var firstPlacePrize: Float = 0f,
    @get:PropertyName("secondPlacePrize") @set:PropertyName("secondPlacePrize")
    var secondPlacePrize: Float = 0f,
    @get:PropertyName("thirdPlacePrize") @set:PropertyName("thirdPlacePrize")
    var thirdPlacePrize: Float = 0f,
    @get:PropertyName("perKillPrize") @set:PropertyName("perKillPrize")
    var perKillPrize: Float = 0f,

    // Manual Cancellation Details
    @get:PropertyName("cancellationReason") @set:PropertyName("cancellationReason")
    var cancellationReason: String = "",
    @get:PropertyName("cancelledAt") @set:PropertyName("cancelledAt")
    var cancelledAt: Long = 0L,

    @get:PropertyName("roomDetails") @set:PropertyName("roomDetails")
    var roomDetails: RoomDetails? = null,

    @get:PropertyName("isCompleted") @set:PropertyName("isCompleted")
    var isCompleted: Boolean = false,
    @get:PropertyName("payoutStatus") @set:PropertyName("payoutStatus")
    var payoutStatus: String = "UNPAID",
    @get:PropertyName("completedAt") @set:PropertyName("completedAt")
    var completedAt: Long = 0L,
    @get:PropertyName("totalPrizeDisbursed") @set:PropertyName("totalPrizeDisbursed")
    var totalPrizeDisbursed: Double = 0.0,
    @get:PropertyName("winnerUid") @set:PropertyName("winnerUid")
    var winnerUid: String = "",
    @get:PropertyName("winnerName") @set:PropertyName("winnerName")
    var winnerName: String = "",
    @get:PropertyName("winnerPrize") @set:PropertyName("winnerPrize")
    var winnerPrize: Double = 0.0,
    @get:PropertyName("finalLeaderboard") @set:PropertyName("finalLeaderboard")
    var finalLeaderboard: List<TournamentStandingEntry> = emptyList(),
    @get:PropertyName("standings") @set:PropertyName("standings")
    var standings: List<TournamentStandingEntry> = emptyList(),

    @get:PropertyName("registrations") @set:PropertyName("registrations")
    var registrations: List<PlayerRegistration> = emptyList()
) {
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val perKillReward: Float get() = perKillPrize
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val participants: List<PlayerRegistration> get() = registrations
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val isCashTournament: Boolean get() = entryFee > 0f || prizePool > 0f
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val isTrainingMatch: Boolean get() = entryFee == 0f || title.contains("Training", ignoreCase = true) || title.contains("Scrim", ignoreCase = true) || title.contains("Practice", ignoreCase = true) || description.contains("training", ignoreCase = true)

    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val canonicalCategory: String get() {
        val cat = category.trim().uppercase()
        return when {
            cat == "CS" || cat.contains("CLASH") || format.contains("CS", true) || title.contains("CS", true) || title.contains("Clash Squad", true) -> "CS"
            cat == "LONE_WOLF" || cat == "LONEWOLF" || format.contains("Lone", true) || title.contains("Lone Wolf", true) || title.contains("1v1", true) && !format.contains("CS", true) || map.contains("Iron Cage", true) -> "LONE_WOLF"
            cat == "SCRIMS" || cat == "TRAINING" || isTrainingMatch -> "SCRIMS"
            else -> "BR"
        }
    }

    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val categoryDisplayName: String get() = when (canonicalCategory) {
        "BR" -> "Battle Royale (Full Map)"
        "CS" -> "Clash Squad (CS)"
        "LONE_WOLF" -> "Lone Wolf (1v1 / 2v2)"
        "SCRIMS" -> "Custom Scrims & Training"
        else -> "Battle Royale"
    }

    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val categoryShortBadge: String get() = when (canonicalCategory) {
        "BR" -> "BR FULL MAP"
        "CS" -> "CLASH SQUAD"
        "LONE_WOLF" -> "LONE WOLF"
        "SCRIMS" -> "SCRIMS / FREE"
        else -> "BATTLE ROYALE"
    }

    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val isBattleRoyale: Boolean get() = canonicalCategory == "BR"
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val isClashSquad: Boolean get() = canonicalCategory == "CS"
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val isLoneWolf: Boolean get() = canonicalCategory == "LONE_WOLF"
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val isScrim: Boolean get() = canonicalCategory == "SCRIMS"

    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val parsedStartsAtMillis: Long?
        get() {
            val raw = startsAt?.trim() ?: return null
            raw.toLongOrNull()?.let { return it }
            val patterns = arrayOf(
                "yyyy-MM-dd HH:mm",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd",
                "dd-MM-yyyy HH:mm",
                "dd/MM/yyyy HH:mm"
            )
            for (p in patterns) {
                try {
                    val sdf = java.text.SimpleDateFormat(p, java.util.Locale.getDefault())
                    sdf.isLenient = false
                    val date = sdf.parse(raw)
                    if (date != null) return date.time
                } catch (_: Exception) {}
            }
            return null
        }
}

data class TournamentCategory(
    val id: String = "",
    val name: String = "",
    val label: String = name,
    val shortName: String = name,
    val description: String = "",
    val iconUrl: String = "",
    val bannerUrl: String = "",
    val defaultFormats: List<String> = listOf("SOLO", "DUO", "SQUAD"),
    val defaultMaps: List<String> = listOf("Bermuda", "Purgatory", "Kalahari", "Alpine"),
    val defaultMaxPlayers: Int = 48,
    val isCustom: Boolean = false,
    val tournamentCount: Int = 0
) {
    fun toCategoryMeta(): FreeFireCategories.CategoryMeta {
        return FreeFireCategories.CategoryMeta(
            key = id,
            label = label.ifBlank { name },
            shortName = shortName.ifBlank { name },
            description = description,
            defaultFormats = defaultFormats.ifEmpty { listOf("SOLO", "DUO", "SQUAD") },
            defaultMaps = defaultMaps.ifEmpty { listOf("Bermuda", "Purgatory", "Kalahari") },
            defaultMaxPlayers = if (defaultMaxPlayers > 0) defaultMaxPlayers else 48
        )
    }
}

object FreeFireCategories {
    const val BR = "BR"
    const val CS = "CS"
    const val LONE_WOLF = "LONE_WOLF"
    const val SCRIMS = "SCRIMS"

    data class CategoryMeta(
        val key: String,
        val label: String,
        val shortName: String,
        val description: String,
        val defaultFormats: List<String>,
        val defaultMaps: List<String>,
        val defaultMaxPlayers: Int
    )

    val ALL_CATEGORIES = listOf(
        CategoryMeta(
            key = BR,
            label = "Battle Royale (Full Map)",
            shortName = "BR",
            description = "Standard 48-player competitive survival matches across Bermuda, Purgatory, Kalahari & Alpine.",
            defaultFormats = listOf("SOLO", "DUO", "SQUAD"),
            defaultMaps = listOf("Bermuda", "Purgatory", "Kalahari", "Alpine", "NexTerra"),
            defaultMaxPlayers = 48
        ),
        CategoryMeta(
            key = CS,
            label = "Clash Squad (CS)",
            shortName = "CS",
            description = "High-octane round-based tactical combat. 4v4, 2v2, or 1v1 best of 7/13 rounds.",
            defaultFormats = listOf("4v4 CS", "2v2 CS", "1v1 CS"),
            defaultMaps = listOf("Bermuda CS", "Kalahari CS", "Purgatory CS", "Alpine CS"),
            defaultMaxPlayers = 8
        ),
        CategoryMeta(
            key = LONE_WOLF,
            label = "Lone Wolf (Duel)",
            shortName = "Lone Wolf",
            description = "Intense 1v1 and 2v2 weapon-pick showdown arena matches in Iron Cage.",
            defaultFormats = listOf("1v1 LONE WOLF", "2v2 LONE WOLF"),
            defaultMaps = listOf("Iron Cage", "Sci-Turf", "Bermuda Cage"),
            defaultMaxPlayers = 2
        ),
        CategoryMeta(
            key = SCRIMS,
            label = "Custom Scrims & Training",
            shortName = "Scrims",
            description = "Guild practice, mock tournament scrims, and free entry training rooms for minor and pro players.",
            defaultFormats = listOf("SQUAD SCRIMS", "SOLO PRACTICE", "DUO SCRIMS"),
            defaultMaps = listOf("Bermuda", "Purgatory", "Kalahari"),
            defaultMaxPlayers = 48
        )
    )

    val DEFAULT_CATEGORIES: List<TournamentCategory> = ALL_CATEGORIES.map { meta ->
        TournamentCategory(
            id = meta.key,
            name = meta.shortName,
            label = meta.label,
            shortName = meta.shortName,
            description = meta.description,
            defaultFormats = meta.defaultFormats,
            defaultMaps = meta.defaultMaps,
            defaultMaxPlayers = meta.defaultMaxPlayers,
            isCustom = false
        )
    }

    fun getCategoryMeta(key: String, dynamicCategories: List<TournamentCategory> = emptyList()): CategoryMeta {
        dynamicCategories.firstOrNull { it.id.equals(key, ignoreCase = true) }?.let {
            return it.toCategoryMeta()
        }
        return ALL_CATEGORIES.firstOrNull { it.key.equals(key, ignoreCase = true) }
            ?: CategoryMeta(
                key = key,
                label = key.replace("_", " "),
                shortName = key,
                description = "Category: $key",
                defaultFormats = listOf("SOLO", "DUO", "SQUAD"),
                defaultMaps = listOf("Bermuda", "Purgatory", "Kalahari"),
                defaultMaxPlayers = 48
            )
    }
}

object TournamentBannerPresets {
    data class BannerPreset(val id: String, val title: String, val map: String, val url: String)

    val PRESETS = listOf(
        BannerPreset(
            id = "ff_bermuda_inferno",
            title = "Bermuda Inferno",
            map = "Bermuda",
            url = "https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format&fit=crop&w=1200&q=80"
        ),
        BannerPreset(
            id = "ff_kalahari_warzone",
            title = "Kalahari Badlands",
            map = "Kalahari",
            url = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=1200&q=80"
        ),
        BannerPreset(
            id = "ff_purgatory_cyber",
            title = "Purgatory Cyber",
            map = "Purgatory",
            url = "https://images.unsplash.com/photo-1511512578047-dfb367046420?auto=format&fit=crop&w=1200&q=80"
        ),
        BannerPreset(
            id = "ff_esports_championship",
            title = "Grand Championship",
            map = "All Maps",
            url = "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?auto=format&fit=crop&w=1200&q=80"
        ),
        BannerPreset(
            id = "ff_clash_squad_4v4",
            title = "Clash Squad 4v4",
            map = "Clash Squad",
            url = "https://images.unsplash.com/photo-1563089145-599997674d42?auto=format&fit=crop&w=1200&q=80"
        ),
        BannerPreset(
            id = "ff_sniper_one_shot",
            title = "One-Shot Snipers",
            map = "Alpine",
            url = "https://images.unsplash.com/photo-1579373903781-fd5c0c30c4cd?auto=format&fit=crop&w=1200&q=80"
        )
    )
}

@IgnoreExtraProperties
@Serializable
data class PlayerRegistration(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    @get:PropertyName("tournamentId") @set:PropertyName("tournamentId")
    var tournamentId: String = "",
    @get:PropertyName("userId") @set:PropertyName("userId")
    var userId: String = "",
    @get:PropertyName("playerId") @set:PropertyName("playerId")
    var playerId: String = "",
    @get:PropertyName("playerName") @set:PropertyName("playerName")
    var playerName: String = "",
    @get:PropertyName("gameUsername") @set:PropertyName("gameUsername")
    var gameUsername: String = "",
    @get:PropertyName("gameId") @set:PropertyName("gameId")
    var gameId: String = "",
    @get:PropertyName("paymentStatus") @set:PropertyName("paymentStatus")
    var paymentStatus: String = "confirmed",
    @get:PropertyName("registeredAt") @set:PropertyName("registeredAt")
    var registeredAt: Long = System.currentTimeMillis(),
    @get:PropertyName("status") @set:PropertyName("status")
    var status: String = "confirmed",
    @get:PropertyName("gameAccountId") @set:PropertyName("gameAccountId")
    var gameAccountId: String? = null,
    @get:PropertyName("region") @set:PropertyName("region")
    var region: String? = null,
    @get:PropertyName("age") @set:PropertyName("age")
    var age: Int? = null,
    @get:PropertyName("entryFeePaid") @set:PropertyName("entryFeePaid")
    var entryFeePaid: Double = 0.0,
    @get:PropertyName("paidFromDeposit") @set:PropertyName("paidFromDeposit")
    var paidFromDeposit: Double = 0.0,
    @get:PropertyName("paidFromWinning") @set:PropertyName("paidFromWinning")
    var paidFromWinning: Double = 0.0,
    @get:PropertyName("paidFromBonus") @set:PropertyName("paidFromBonus")
    var paidFromBonus: Double = 0.0,
    @get:PropertyName("kills") @set:PropertyName("kills")
    var kills: Int = 0,
    @get:PropertyName("rank") @set:PropertyName("rank")
    var rank: Int = 0,
    @get:PropertyName("winningAmount") @set:PropertyName("winningAmount")
    var winningAmount: Double = 0.0,
    @get:PropertyName("profile") @set:PropertyName("profile")
    var profile: UserProfile? = null,
    @get:PropertyName("tournament") @set:PropertyName("tournament")
    var tournament: Tournament? = null 
) {
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val joinedAt: Long get() = registeredAt
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val freeFireUid: String get() = gameId.ifBlank { gameAccountId ?: "" }
    @get:com.google.firebase.database.Exclude @get:com.google.firebase.firestore.Exclude
    val inGameName: String get() = gameUsername.ifBlank { playerName }
}

@IgnoreExtraProperties
@Serializable
data class LeaderboardEntry(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    @get:PropertyName("tournamentId") @set:PropertyName("tournamentId")
    var tournamentId: String = "",
    @get:PropertyName("userId") @set:PropertyName("userId")
    var userId: String = "",
    @get:PropertyName("score") @set:PropertyName("score")
    var score: Int = 0,
    @get:PropertyName("rank") @set:PropertyName("rank")
    var rank: Int? = null,
    @get:PropertyName("profile") @set:PropertyName("profile")
    var profile: UserProfile? = null
)

@IgnoreExtraProperties
@Serializable
data class Match(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    @get:PropertyName("tournamentId") @set:PropertyName("tournamentId")
    var tournamentId: String = "",
    @get:PropertyName("round") @set:PropertyName("round")
    var round: Int = 0,
    @get:PropertyName("matchNumber") @set:PropertyName("matchNumber")
    var matchNumber: Int = 0,
    @get:PropertyName("player1Id") @set:PropertyName("player1Id")
    var player1Id: String? = null,
    @get:PropertyName("player2Id") @set:PropertyName("player2Id")
    var player2Id: String? = null,
    @get:PropertyName("winnerId") @set:PropertyName("winnerId")
    var winnerId: String? = null,
    @get:PropertyName("status") @set:PropertyName("status")
    var status: String = "",
    @get:PropertyName("score1") @set:PropertyName("score1")
    var score1: Int? = null,
    @get:PropertyName("score2") @set:PropertyName("score2")
    var score2: Int? = null
)

@IgnoreExtraProperties
@Serializable
data class CheckInToken(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    @get:PropertyName("tokenCode") @set:PropertyName("tokenCode")
    var tokenCode: String = "",
    @get:PropertyName("tournamentTitle") @set:PropertyName("tournamentTitle")
    var tournamentTitle: String = "",
    @get:PropertyName("playerUsername") @set:PropertyName("playerUsername")
    var playerUsername: String = "",
    @get:PropertyName("gameAccountId") @set:PropertyName("gameAccountId")
    var gameAccountId: String = "",
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: String = "",
    @get:PropertyName("isUsed") @set:PropertyName("isUsed")
    var isUsed: Boolean = false,
    @get:PropertyName("isExpired") @set:PropertyName("isExpired")
    var isExpired: Boolean = false
)

@IgnoreExtraProperties
@Serializable
data class MatchProofSubmission(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    @get:PropertyName("tournamentId") @set:PropertyName("tournamentId")
    var tournamentId: String = "",
    @get:PropertyName("tournamentTitle") @set:PropertyName("tournamentTitle")
    var tournamentTitle: String = "",
    @get:PropertyName("userId") @set:PropertyName("userId")
    var userId: String = "",
    @get:PropertyName("username") @set:PropertyName("username")
    var username: String = "",
    @get:PropertyName("gameAccountId") @set:PropertyName("gameAccountId")
    var gameAccountId: String = "",
    @get:PropertyName("claimedRank") @set:PropertyName("claimedRank")
    var claimedRank: Int = 1,
    @get:PropertyName("claimedKills") @set:PropertyName("claimedKills")
    var claimedKills: Int = 0,
    @get:PropertyName("screenshotUrl") @set:PropertyName("screenshotUrl")
    var screenshotUrl: String = "",
    @get:PropertyName("status") @set:PropertyName("status")
    var status: String = "pending", // pending, approved, rejected
    @get:PropertyName("rejectionReason") @set:PropertyName("rejectionReason")
    var rejectionReason: String = "",
    @get:PropertyName("prizeAwarded") @set:PropertyName("prizeAwarded")
    var prizeAwarded: Double = 0.0,
    @get:PropertyName("submittedAt") @set:PropertyName("submittedAt")
    var submittedAt: Long = System.currentTimeMillis(),
    @get:PropertyName("reviewedAt") @set:PropertyName("reviewedAt")
    var reviewedAt: Long = 0L,
    @get:PropertyName("reviewedBy") @set:PropertyName("reviewedBy")
    var reviewedBy: String = ""
)

@IgnoreExtraProperties
@Serializable
data class GlobalAnnouncement(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    @get:PropertyName("title") @set:PropertyName("title")
    var title: String = "",
    @get:PropertyName("message") @set:PropertyName("message")
    var message: String = "",
    @get:PropertyName("level") @set:PropertyName("level")
    var level: String = "INFO", // INFO, URGENT, TOURNAMENT, MAINTENANCE
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Long = System.currentTimeMillis(),
    @get:PropertyName("author") @set:PropertyName("author")
    var author: String = "Velorix Admin",
    @get:PropertyName("isActive") @set:PropertyName("isActive")
    var isActive: Boolean = true
) {
    val priority: String get() = level

    constructor(id: String, title: String, message: String, priority: String) : this(
        id = id,
        title = title,
        message = message,
        level = priority,
        createdAt = System.currentTimeMillis(),
        author = "Velorix Admin",
        isActive = true
    )
}

@IgnoreExtraProperties
@Serializable
data class AppNotification(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    @get:PropertyName("title") @set:PropertyName("title")
    var title: String = "",
    @get:PropertyName("message") @set:PropertyName("message")
    var message: String = "",
    @get:PropertyName("type") @set:PropertyName("type")
    var type: String = "TOURNAMENT", // TOURNAMENT, CAMPAIGN, SYSTEM, DISPUTE, PAYOUT
    @get:PropertyName("targetTournamentId") @set:PropertyName("targetTournamentId")
    var targetTournamentId: String = "",
    @get:PropertyName("game") @set:PropertyName("game")
    var game: String = "",
    @get:PropertyName("timestamp") @set:PropertyName("timestamp")
    var timestamp: Long = System.currentTimeMillis(),
    @get:PropertyName("priority") @set:PropertyName("priority")
    var priority: String = "HIGH", // LOW, MEDIUM, HIGH, URGENT
    @get:PropertyName("isRead") @set:PropertyName("isRead")
    var isRead: Boolean = false,
    @get:PropertyName("actionUrl") @set:PropertyName("actionUrl")
    var actionUrl: String = ""
)

data class AdminVerificationResult(
    val isAuthorized: Boolean,
    val role: String = "admin",
    val errorMessage: String? = null
)

@IgnoreExtraProperties
@Serializable
data class AuditLogEntry(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    @get:PropertyName("action") @set:PropertyName("action")
    var action: String = "",
    @get:PropertyName("performedBy") @set:PropertyName("performedBy")
    var performedBy: String = "Admin",
    @get:PropertyName("targetUid") @set:PropertyName("targetUid")
    var targetUid: String = "",
    @get:PropertyName("details") @set:PropertyName("details")
    var details: String = "",
    @get:PropertyName("category") @set:PropertyName("category")
    var category: String = "SYSTEM", // TOURNAMENT, SECURITY, CASHOUT, SUPPORT, SYSTEM
    @get:PropertyName("timestamp") @set:PropertyName("timestamp")
    var timestamp: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
@Serializable
data class BackendExtractionResult(
    val success: Boolean = true,
    val tournamentsCount: Int = 0,
    val usersCount: Int = 0,
    val supportTicketsCount: Int = 0,
    val payoutRequestsCount: Int = 0,
    val tournaments: List<Tournament> = emptyList(),
    val users: List<UserProfile> = emptyList(),
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
enum class AdminChannelType(
    val key: String,
    val channelLabel: String,
    val displayTitle: String,
    val description: String = displayTitle,
    val isOperationalLog: Boolean = false
) {
    // 6 User Support Channels (Master Prompt Section 4)
    PAYMENT_DEPOSIT("payment_deposit", "#payment-deposit", "Payment & Deposit", "UPI / UTR Deposit & Add Cash issues"),
    PAYMENT_WITHDRAW("payment_withdraw", "#payment-withdraw", "Withdrawal & Payout", "Withdrawal delays & bank/UPI payout issues"),
    ROOM_CREDENTIALS("room_credentials", "#room-credentials", "Room ID & Password", "Custom Room ID, Password & Slot issues"),
    CHEATER_HACKER("cheater_hacker", "#cheater-hacker", "Cheater / Hacker Report", "Report Aimbot, Wallhack, Teaming or Emulator"),
    ACCOUNT_ISSUE("account_issue", "#account-issue", "Account & Profile", "Free Fire UID change, login or ban appeal"),
    VERIFICATION("verification", "#verification", "Verification & Founder", "Founder Pass, KYC or Tournament Host request"),

    // Operational Dispatch Logs (Master Prompt Section 4)
    SLOT_CONFIRMATIONS("slot_confirmations", "#slot-confirmations", "Slot Confirmations", "Automated slot confirmation dispatches", true),
    ROOM_CREDENTIAL_REQUESTS("room_credential_requests", "#room-cred-requests", "Room Credential Requests", "Player room credential requests", true),
    MATCH_REMINDERS("match_reminders", "#match-reminders", "Match Reminders", "Automated match countdown reminders", true),
    REFUND_DISPATCHES("refund_dispatches", "#refund-dispatches", "Refund Dispatches", "Automated tournament cancellation refunds", true),
    TOURNAMENT_OPENINGS("tournament_openings", "#tournament-openings", "Tournament Openings", "New tournament lobby open dispatches", true),
    ENGAGEMENT_PULSES("engagement_pulses", "#engagement-pulses", "Engagement Pulses", "Attracting esports & festival alert pulses", true),
    OTA_LIVE_PATCHES("ota_live_patches", "#ota-live-patches", "OTA Live Patches", "Play-Store-free OTA configuration patches", true),

    // Additional Core Admin Queues
    ACCOUNT_DELETIONS("account_deletion_requests", "#account-deletions", "Account Deletions", "DPDP Act 2023 account erasure & restore requests"),
    DATA_EXPORTS("data_export_requests", "#data-exports", "Data Exports (DPDP)", "User portable JSON data archives & audit snapshots"),
    DEPOSITS("deposit_requests", "#deposits", "Deposit Verifications", "Manual UPI UTR, SpacePay & Razorpay deposit verification queue"),
    WITHDRAWALS("withdrawal_requests", "#withdrawals", "Withdrawal Payouts", "Winning balance cashout & UPI payout queue"),
    SUPPORT_TICKETS("support_tickets", "#support-tickets", "Support Tickets", "Live player support tickets & issue threads"),
    PLAYER_REPORTS("player_reports", "#player-reports", "Player / Cheat Reports", "Anti-cheat reports, emulator/hack flags & evidence"),
    KYC_VERIFICATIONS("kyc_requests", "#kyc-verifications", "KYC 18+ Verifications", "Identity & age verification submissions"),
    MATCH_DISPUTES("tournament_disputes", "#match-disputes", "Match Disputes", "Scoreboard, room & prize distribution disputes"),
    TOKEN_LEDGER("token_conversions", "#token-ledger", "Token Ledger (10:1 VT)", "Reward token to VT conversions & mission grants"),
    SITUATION_TESTER("admin_situation_tests", "#situation-tester", "Situation Tester", "Live dual-DB pipeline diagnostic probes");

    val channelKey: String get() = key
    val hashtag: String get() = channelLabel
    val title: String get() = displayTitle

    companion object {
        val ACCOUNT_DELETION = ACCOUNT_DELETIONS
        val DATA_EXPORT = DATA_EXPORTS
        val DEPOSIT_REQUESTS = DEPOSITS
        val TOKEN_CONVERSIONS = TOKEN_LEDGER
        val ADMIN_SITUATION_TESTS = SITUATION_TESTER
    }
}

@IgnoreExtraProperties
@Serializable
data class AdminChannelItem(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    @get:PropertyName("channelType") @set:PropertyName("channelType")
    var channelType: String = "",
    @get:PropertyName("adminChannel") @set:PropertyName("adminChannel")
    var adminChannel: String = "",
    @get:PropertyName("channelLabel") @set:PropertyName("channelLabel")
    var channelLabel: String = "",
    @get:PropertyName("userId") @set:PropertyName("userId")
    var userId: String = "",
    @get:PropertyName("username") @set:PropertyName("username")
    var username: String = "",
    @get:PropertyName("userEmail") @set:PropertyName("userEmail")
    var userEmail: String = "",
    @get:PropertyName("freeFireId") @set:PropertyName("freeFireId")
    var freeFireId: String = "",
    @get:PropertyName("title") @set:PropertyName("title")
    var title: String = "",
    @get:PropertyName("subtitle") @set:PropertyName("subtitle")
    var subtitle: String = "",
    @get:PropertyName("message") @set:PropertyName("message")
    var message: String = "",
    @get:PropertyName("senderUid") @set:PropertyName("senderUid")
    var senderUid: String = "",
    @get:PropertyName("senderName") @set:PropertyName("senderName")
    var senderName: String = "",
    @get:PropertyName("status") @set:PropertyName("status")
    var status: String = "PENDING",
    @get:PropertyName("amount") @set:PropertyName("amount")
    var amount: Double = 0.0,
    @get:PropertyName("utrNumber") @set:PropertyName("utrNumber")
    var utrNumber: String = "",
    @get:PropertyName("paymentMethod") @set:PropertyName("paymentMethod")
    var paymentMethod: String = "",
    @get:PropertyName("upiId") @set:PropertyName("upiId")
    var upiId: String = "",
    @get:PropertyName("screenshotUrl") @set:PropertyName("screenshotUrl")
    var screenshotUrl: String = "",
    @get:PropertyName("exportedJson") @set:PropertyName("exportedJson")
    var exportedJson: String = "",
    @get:PropertyName("reason") @set:PropertyName("reason")
    var reason: String = "",
    @get:PropertyName("details") @set:PropertyName("details")
    var details: String = "",
    @get:PropertyName("adminReply") @set:PropertyName("adminReply")
    var adminReply: String = "",
    @get:PropertyName("rawPayload") @set:PropertyName("rawPayload")
    var rawPayload: Map<String, String> = emptyMap(),
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Long = System.currentTimeMillis(),
    @get:PropertyName("timestamp") @set:PropertyName("timestamp")
    var timestamp: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
@Serializable
data class AccountDeletionRequest(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    @get:PropertyName("requestId") @set:PropertyName("requestId")
    var requestId: String = "",
    @get:PropertyName("userId") @set:PropertyName("userId")
    var userId: String = "",
    @get:PropertyName("username") @set:PropertyName("username")
    var username: String = "",
    @get:PropertyName("email") @set:PropertyName("email")
    var email: String = "",
    @get:PropertyName("phone") @set:PropertyName("phone")
    var phone: String = "",
    @get:PropertyName("phoneOrEmail") @set:PropertyName("phoneOrEmail")
    var phoneOrEmail: String = "",
    @get:PropertyName("inGameName") @set:PropertyName("inGameName")
    var inGameName: String = "",
    @get:PropertyName("freeFireId") @set:PropertyName("freeFireId")
    var freeFireId: String = "",
    @get:PropertyName("balance") @set:PropertyName("balance")
    var balance: Double = 0.0,
    @get:PropertyName("balanceAtRequest") @set:PropertyName("balanceAtRequest")
    var balanceAtRequest: Double = 0.0,
    @get:PropertyName("depositBalance") @set:PropertyName("depositBalance")
    var depositBalance: Double = 0.0,
    @get:PropertyName("winningsBalance") @set:PropertyName("winningsBalance")
    var winningsBalance: Double = 0.0,
    @get:PropertyName("bonusBalance") @set:PropertyName("bonusBalance")
    var bonusBalance: Double = 0.0,
    @get:PropertyName("tokens") @set:PropertyName("tokens")
    var tokens: Int = 0,
    @get:PropertyName("dataExportedBeforeDelete") @set:PropertyName("dataExportedBeforeDelete")
    var dataExportedBeforeDelete: Boolean = false,
    @get:PropertyName("reason") @set:PropertyName("reason")
    var reason: String = "",
    @get:PropertyName("details") @set:PropertyName("details")
    var details: String = "",
    @get:PropertyName("channelNode") @set:PropertyName("channelNode")
    var channelNode: String = "account_deletion_requests",
    @get:PropertyName("adminChannel") @set:PropertyName("adminChannel")
    var adminChannel: String = "admin_channels/account_deletion_requests",
    @get:PropertyName("channelLabel") @set:PropertyName("channelLabel")
    var channelLabel: String = "#account-deletions",
    @get:PropertyName("status") @set:PropertyName("status")
    var status: String = "PENDING_DELETION", // PENDING_DELETION, COMPLETED, RESTORED
    @get:PropertyName("requestedAt") @set:PropertyName("requestedAt")
    var requestedAt: Long = System.currentTimeMillis(),
    @get:PropertyName("processedAt") @set:PropertyName("processedAt")
    var processedAt: Long = 0L,
    @get:PropertyName("processedBy") @set:PropertyName("processedBy")
    var processedBy: String = "",
    @get:PropertyName("timestamp") @set:PropertyName("timestamp")
    var timestamp: Long = System.currentTimeMillis()
) {
    val dataExported: Boolean get() = dataExportedBeforeDelete
}

@IgnoreExtraProperties
@Serializable
data class DataExportRequest(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    @get:PropertyName("exportId") @set:PropertyName("exportId")
    var exportId: String = "",
    @get:PropertyName("userId") @set:PropertyName("userId")
    var userId: String = "",
    @get:PropertyName("username") @set:PropertyName("username")
    var username: String = "",
    @get:PropertyName("email") @set:PropertyName("email")
    var email: String = "",
    @get:PropertyName("phone") @set:PropertyName("phone")
    var phone: String = "",
    @get:PropertyName("phoneOrEmail") @set:PropertyName("phoneOrEmail")
    var phoneOrEmail: String = "",
    @get:PropertyName("freeFireId") @set:PropertyName("freeFireId")
    var freeFireId: String = "",
    @get:PropertyName("balance") @set:PropertyName("balance")
    var balance: Double = 0.0,
    @get:PropertyName("tokens") @set:PropertyName("tokens")
    var tokens: Int = 0,
    @get:PropertyName("transactionCount") @set:PropertyName("transactionCount")
    var transactionCount: Int = 0,
    @get:PropertyName("joinedTournamentCount") @set:PropertyName("joinedTournamentCount")
    var joinedTournamentCount: Int = 0,
    @get:PropertyName("exportedJson") @set:PropertyName("exportedJson")
    var exportedJson: String = "",
    @get:PropertyName("status") @set:PropertyName("status")
    var status: String = "EXPORTED",
    @get:PropertyName("requestedAt") @set:PropertyName("requestedAt")
    var requestedAt: Long = System.currentTimeMillis(),
    @get:PropertyName("timestamp") @set:PropertyName("timestamp")
    var timestamp: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
@Serializable
data class TokenConversionRecord(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    @get:PropertyName("userId") @set:PropertyName("userId")
    var userId: String = "",
    @get:PropertyName("username") @set:PropertyName("username")
    var username: String = "",
    @get:PropertyName("tokensConverted") @set:PropertyName("tokensConverted")
    var tokensConverted: Int = 0,
    @get:PropertyName("tokensSpent") @set:PropertyName("tokensSpent")
    var tokensSpent: Int = 0,
    @get:PropertyName("vtCredited") @set:PropertyName("vtCredited")
    var vtCredited: Double = 0.0,
    @get:PropertyName("conversionRate") @set:PropertyName("conversionRate")
    var conversionRate: Int = 10,
    @get:PropertyName("status") @set:PropertyName("status")
    var status: String = "COMPLETED",
    @get:PropertyName("timestamp") @set:PropertyName("timestamp")
    var timestamp: Long = System.currentTimeMillis()
) {
    val createdAt: Long get() = timestamp
}

@IgnoreExtraProperties
@Serializable
data class MissionItem(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    @get:PropertyName("title") @set:PropertyName("title")
    var title: String = "",
    @get:PropertyName("description") @set:PropertyName("description")
    var description: String = "",
    @get:PropertyName("rewardTokens") @set:PropertyName("rewardTokens")
    var rewardTokens: Int = 20,
    @get:PropertyName("rewardVt") @set:PropertyName("rewardVt")
    var rewardVt: Double = 0.0,
    @get:PropertyName("targetCount") @set:PropertyName("targetCount")
    var targetCount: Int = 1,
    @get:PropertyName("targetProgress") @set:PropertyName("targetProgress")
    var targetProgress: Int = 1,
    @get:PropertyName("missionType") @set:PropertyName("missionType")
    var missionType: String = "DAILY", // DAILY, WEEKLY, SPECIAL
    @get:PropertyName("category") @set:PropertyName("category")
    var category: String = "DAILY",
    @get:PropertyName("actionType") @set:PropertyName("actionType")
    var actionType: String = "DAILY",
    @get:PropertyName("isActive") @set:PropertyName("isActive")
    var isActive: Boolean = true,
    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt")
    var updatedAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
@Serializable
data class TournamentChatMessage(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    @get:PropertyName("tournamentId") @set:PropertyName("tournamentId")
    var tournamentId: String = "",
    @get:PropertyName("senderId") @set:PropertyName("senderId")
    var senderId: String = "admin",
    @get:PropertyName("senderName") @set:PropertyName("senderName")
    var senderName: String = "VeloRix Official",
    @get:PropertyName("senderAvatar") @set:PropertyName("senderAvatar")
    var senderAvatar: String = "",
    @get:PropertyName("senderRole") @set:PropertyName("senderRole")
    var senderRole: String = "ADMIN",
    @get:PropertyName("message") @set:PropertyName("message")
    var message: String = "",
    @get:PropertyName("isAdmin") @set:PropertyName("isAdmin")
    var isAdmin: Boolean = true,
    @get:PropertyName("isPinned") @set:PropertyName("isPinned")
    var isPinned: Boolean = false,
    @get:PropertyName("timestamp") @set:PropertyName("timestamp")
    var timestamp: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
@Serializable
data class SystemConfig(
    @get:PropertyName("maintenanceMode") @set:PropertyName("maintenanceMode")
    var maintenanceMode: Boolean = false,
    @get:PropertyName("maintenanceTitle") @set:PropertyName("maintenanceTitle")
    var maintenanceTitle: String = "Scheduled Server Maintenance",
    @get:PropertyName("maintenanceMessage") @set:PropertyName("maintenanceMessage")
    var maintenanceMessage: String = "Scheduled server upgrade in progress. Back in 30 minutes!",
    @get:PropertyName("maintenanceEndTimeMillis") @set:PropertyName("maintenanceEndTimeMillis")
    var maintenanceEndTimeMillis: Long = 0L,
    @get:PropertyName("forceUpdateRequired") @set:PropertyName("forceUpdateRequired")
    var forceUpdateRequired: Boolean = false,
    @get:PropertyName("latestVersionCode") @set:PropertyName("latestVersionCode")
    var latestVersionCode: Int = 2,
    @get:PropertyName("latestVersionName") @set:PropertyName("latestVersionName")
    var latestVersionName: String = "1.1.0",
    @get:PropertyName("updateDownloadUrl") @set:PropertyName("updateDownloadUrl")
    var updateDownloadUrl: String = "https://velorix.esports/download/velorix-latest.apk",
    @get:PropertyName("updateChangelog") @set:PropertyName("updateChangelog")
    var updateChangelog: String = "• Faster live match updates\n• Enhanced anti-cheat protection",
    @get:PropertyName("showBanners") @set:PropertyName("showBanners")
    var showBanners: Boolean = true,
    @get:PropertyName("showDeveloperModal") @set:PropertyName("showDeveloperModal")
    var showDeveloperModal: Boolean = false,
    @get:PropertyName("upiId") @set:PropertyName("upiId")
    var upiId: String = "velorix.pay@okaxis",
    @get:PropertyName("upiMerchantName") @set:PropertyName("upiMerchantName")
    var upiMerchantName: String = "VeloRix Esports",
    @get:PropertyName("minDepositVt") @set:PropertyName("minDepositVt")
    var minDepositVt: Double = 10.0,
    @get:PropertyName("minWithdrawalVt") @set:PropertyName("minWithdrawalVt")
    var minWithdrawalVt: Double = 50.0,
    @get:PropertyName("tokenConversionRate") @set:PropertyName("tokenConversionRate")
    var tokenConversionRate: Int = 10,
    @get:PropertyName("whatsappSupportUrl") @set:PropertyName("whatsappSupportUrl")
    var whatsappSupportUrl: String = "https://wa.me/919999999999",
    @get:PropertyName("telegramChannelUrl") @set:PropertyName("telegramChannelUrl")
    var telegramChannelUrl: String = "https://t.me/velorixesports",
    @get:PropertyName("instagramUrl") @set:PropertyName("instagramUrl")
    var instagramUrl: String = "https://instagram.com/velorixesports",
    @get:PropertyName("discordUrl") @set:PropertyName("discordUrl")
    var discordUrl: String = "https://discord.gg/velorix",
    @get:PropertyName("vpnBlockEnabled") @set:PropertyName("vpnBlockEnabled")
    var vpnBlockEnabled: Boolean = true,
    // OTA Live Config, Calendar Festivals & 14 Feature Flags (Play-Store-Free Updates)
    @get:PropertyName("otaPatchVersion") @set:PropertyName("otaPatchVersion")
    var otaPatchVersion: Int = 1,
    @get:PropertyName("otaReleaseNotes") @set:PropertyName("otaReleaseNotes")
    var otaReleaseNotes: String = "Live OTA Hot-Patch Active — Zero Play Store Update Needed",
    @get:PropertyName("liveLobbyTicker") @set:PropertyName("liveLobbyTicker")
    var liveLobbyTicker: String = "",
    @get:PropertyName("activeFestivalOverride") @set:PropertyName("activeFestivalOverride")
    var activeFestivalOverride: String = "AUTO",
    @get:PropertyName("customFestivalTitle") @set:PropertyName("customFestivalTitle")
    var customFestivalTitle: String = "",
    @get:PropertyName("customFestivalMessage") @set:PropertyName("customFestivalMessage")
    var customFestivalMessage: String = "",
    @get:PropertyName("customAttractingTitle") @set:PropertyName("customAttractingTitle")
    var customAttractingTitle: String = "",
    @get:PropertyName("customAttractingMessage") @set:PropertyName("customAttractingMessage")
    var customAttractingMessage: String = "",
    @get:PropertyName("attractingAlertWeight") @set:PropertyName("attractingAlertWeight")
    var attractingAlertWeight: Int = 65,
    @get:PropertyName("timeContextualWeight") @set:PropertyName("timeContextualWeight")
    var timeContextualWeight: Int = 20,
    @get:PropertyName("weeklyCalendarWeight") @set:PropertyName("weeklyCalendarWeight")
    var weeklyCalendarWeight: Int = 15,
    @get:PropertyName("featureFlags") @set:PropertyName("featureFlags")
    var featureFlags: Map<String, Boolean> = defaultFeatureFlags(),
    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt")
    var updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        val ALL_FESTIVAL_OPTIONS = listOf(
            "AUTO" to "Auto-Detect Real-World Calendar Date",
            "NEW_YEAR_EVE" to "New Year's Eve (Dec 31)",
            "NEW_YEAR" to "New Year Kickoff (Jan 1–3)",
            "HARVEST_FESTIVALS" to "Makar Sankranti / Pongal / Lohri (Jan 13–15)",
            "REPUBLIC_DAY" to "Republic Day Tricolor Cup (Jan 26)",
            "VALENTINE_DUO" to "Valentine's Duo Showdown (Feb 14)",
            "MAHA_SHIVRATRI" to "Maha Shivratri Night Clash",
            "HOLI" to "Holi Festival of Colors",
            "EID_UL_FITR" to "Eid-ul-Fitr Mubarak Drop",
            "GUDI_PADWA_NAVRATRI" to "Gudi Padwa / Ugadi / Chaitra Navratri",
            "AMBEDKAR_BAISAKHI" to "Baisakhi / Vishu / Puthandu (Apr 13–15)",
            "EID_UL_ADHA" to "Eid-ul-Adha Champions Arena",
            "INDEPENDENCE_DAY" to "Independence Day Freedom Cup (Aug 15)",
            "RAKSHA_BANDHAN" to "Raksha Bandhan Duo Special",
            "JANMASHTAMI" to "Krishna Janmashtami Midnight Clash",
            "GANESH_CHATURTHI" to "Ganesh Chaturthi Arena",
            "GANDHI_JAYANTI" to "Gandhi Jayanti Special (Oct 2)",
            "NAVRATRI_DUSSEHRA" to "Navratri & Vijayadashami Showdown",
            "HALLOWEEN" to "Halloween Phantom Drop (Oct 31)",
            "DIWALI" to "Diwali Grand Dhamaka",
            "GURU_NANAK_JAYANTI" to "Gurpurab Special Lobby",
            "CHRISTMAS" to "Winterland & Christmas Drop (Dec 24–26)",
            "CUSTOM" to "Custom Admin Festival Override"
        )

        val ALL_FEATURE_FLAG_METADATA = listOf(
            Triple("token_conversion", "Token → VT Conversion", "10 Tokens = 1 VT in Wallet"),
            Triple("tournament_join", "Tournament Joining", "Master switch for joining matches"),
            Triple("room_credentials_delivery", "Room ID Delivery", "Show Room ID & Password to joined players"),
            Triple("instant_withdrawals", "Instant Withdrawals", "Allow players to submit payout requests"),
            Triple("daily_login_rewards", "Daily Login Streak", "7-Day Login Streak & Daily Missions"),
            Triple("founder_pass_hub", "Founder Pass Hub", "VIP Founder Pass tiers & perks"),
            Triple("community_chat", "Community Lobby Chat", "Live global & match chat"),
            Triple("ai_sentinel_support", "AI Sentinel Support", "6-Channel Admin Router & AI Help Desk"),
            Triple("calendar_festival_sync", "Calendar Festival Sync", "Auto-detect Indian & global festivals"),
            Triple("attracting_esports_alerts", "Attracting Esports Alerts", "High-energy Free Fire squad callouts"),
            Triple("hyperos_super_island", "HyperOS Super Island", "Dynamic Island pill notifications"),
            Triple("mj_signature_sfx", "Signature Audio SFX", "Custom notification & action sound effects"),
            Triple("ota_live_hot_patch", "OTA Live Hot-Patch", "Play-Store-free live config sync"),
            Triple("auto_upi_utr_listener", "Auto UPI UTR Listener", "Instant UTR verification queue")
        )

        fun defaultFeatureFlags(): Map<String, Boolean> =
            ALL_FEATURE_FLAG_METADATA.associate { (key, _, _) -> key to true }
    }
}

@IgnoreExtraProperties
@Serializable
data class TournamentStandingEntry(
    @get:PropertyName("rank") @set:PropertyName("rank")
    var rank: Int = 1,
    @get:PropertyName("userId") @set:PropertyName("userId")
    var userId: String = "",
    @get:PropertyName("username") @set:PropertyName("username")
    var username: String = "",
    @get:PropertyName("inGameName") @set:PropertyName("inGameName")
    var inGameName: String = "",
    @get:PropertyName("characterId") @set:PropertyName("characterId")
    var characterId: String = "",
    @get:PropertyName("teamName") @set:PropertyName("teamName")
    var teamName: String = "",
    @get:PropertyName("slotNumber") @set:PropertyName("slotNumber")
    var slotNumber: Int = 1,
    @get:PropertyName("kills") @set:PropertyName("kills")
    var kills: Int = 0,
    @get:PropertyName("points") @set:PropertyName("points")
    var points: Int = 0,
    @get:PropertyName("rankPrize") @set:PropertyName("rankPrize")
    var rankPrize: Double = 0.0,
    @get:PropertyName("killPrize") @set:PropertyName("killPrize")
    var killPrize: Double = 0.0,
    @get:PropertyName("prizeWon") @set:PropertyName("prizeWon")
    var prizeWon: Double = 0.0,
    @get:PropertyName("wonAnything") @set:PropertyName("wonAnything")
    var wonAnything: Boolean = false,
    @get:PropertyName("payoutStatus") @set:PropertyName("payoutStatus")
    var payoutStatus: String = "NO_PRIZE",
    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt")
    var updatedAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
@Serializable
data class AdminAuditLogItem(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    @get:PropertyName("action") @set:PropertyName("action")
    var action: String = "",
    @get:PropertyName("detail") @set:PropertyName("detail")
    var detail: String = "",
    @get:PropertyName("adminEmail") @set:PropertyName("adminEmail")
    var adminEmail: String = "",
    @get:PropertyName("targetId") @set:PropertyName("targetId")
    var targetId: String = "",
    @get:PropertyName("timestamp") @set:PropertyName("timestamp")
    var timestamp: Long = System.currentTimeMillis()
)

