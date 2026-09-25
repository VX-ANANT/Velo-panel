package com.example.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

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
    var rawAttributes: Map<String, String> = emptyMap()
) {
    val isMinor: Boolean get() = isUnder18 || (age in 1..17)
    val canJoinCashTournaments: Boolean get() = !isMinor && eligibleForCashTournaments
    val totalWalletBalance: Double get() = when {
        balance > 0.0 -> balance
        (depositFunds + winningFunds + bonusFunds) > 0.0 -> depositFunds + winningFunds + bonusFunds
        else -> funds
    }
    val walletBalance: Double get() = totalWalletBalance
    val effectiveIgn: String get() = when {
        ign.isNotBlank() && ign != "Player" -> ign
        username.isNotBlank() && username != "Player" && !username.startsWith("Player (") && !username.contains("@") -> username
        gameId.isNotBlank() -> "FF ID: $gameId"
        else -> ign.ifBlank { username.ifBlank { "Player" } }
    }
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
    val matchesWon: Int get() = wins
    val totalKills: Int get() = kills
    val matchesPlayed: Int get() = wins + activityPoints
    val phoneNumber: String get() = phone.ifBlank { if (!phoneOrEmail.contains("@")) phoneOrEmail else "" }
    val gameAccountId: String get() = gameId
    val uid: String get() = id
    val gameUsername: String get() = bestDisplayName
    val freeFireUid: String get() = gameId
}

@IgnoreExtraProperties
@Serializable
data class AppAnnouncementBanner(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    @get:PropertyName("title") @set:PropertyName("title")
    var title: String = "",
    @get:PropertyName("content") @set:PropertyName("content")
    var content: String = "",
    @get:PropertyName("bannerType") @set:PropertyName("bannerType")
    var bannerType: String = "NEWS", // NEWS, PROMO, ALERT, UPDATE
    @get:PropertyName("isActive") @set:PropertyName("isActive")
    var isActive: Boolean = true,
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
    @get:PropertyName("userEmail") @set:PropertyName("userEmail")
    var userEmail: String = "",
    @get:PropertyName("username") @set:PropertyName("username")
    var username: String = "",
    @get:PropertyName("gameId") @set:PropertyName("gameId")
    var gameId: String = "",
    @get:PropertyName("issueCategory") @set:PropertyName("issueCategory")
    var issueCategory: String = "General", // "Withdrawal Issue", "Room ID Error", "Cheater Report", "App Error"
    @get:PropertyName("description") @set:PropertyName("description")
    var description: String = "",
    @get:PropertyName("status") @set:PropertyName("status")
    var status: String = "open", // open, in_progress, resolved, closed
    @get:PropertyName("adminNote") @set:PropertyName("adminNote")
    var adminNote: String = "",
    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt")
    var updatedAt: Long = System.currentTimeMillis(),
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Long = System.currentTimeMillis(),
    @get:PropertyName("assignedTo") @set:PropertyName("assignedTo")
    var assignedTo: String = "",
    @get:PropertyName("tournamentTitle") @set:PropertyName("tournamentTitle")
    var tournamentTitle: String = "",
    @get:PropertyName("isHighPriority") @set:PropertyName("isHighPriority")
    var isHighPriority: Boolean = false
) {
    val userId: String get() = userEmail
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
    var processedBy: String = ""
) {
    val userId: String get() = uid
    val amount: Double get() = vtAmount
    val upiId: String get() = paymentId
    val createdAt: Long get() = requestedAt
}

@IgnoreExtraProperties
@Serializable
data class DepositRequest(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    @get:PropertyName("uid") @set:PropertyName("uid")
    var uid: String = "",
    @get:PropertyName("username") @set:PropertyName("username")
    var username: String = "",
    @get:PropertyName("amount") @set:PropertyName("amount")
    var amount: Double = 0.0,
    @get:PropertyName("utrNumber") @set:PropertyName("utrNumber")
    var utrNumber: String = "",
    @get:PropertyName("paymentScreenshotUrl") @set:PropertyName("paymentScreenshotUrl")
    var paymentScreenshotUrl: String = "",
    @get:PropertyName("status") @set:PropertyName("status")
    var status: String = "PENDING", // PENDING, SUCCESS, REJECTED
    @get:PropertyName("timestamp") @set:PropertyName("timestamp")
    var timestamp: Long = System.currentTimeMillis()
)

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
    @get:com.google.firebase.firestore.Exclude
    val password: String get() = roomPassword
}

@IgnoreExtraProperties
@Serializable
data class Tournament(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    @get:PropertyName("title") @set:PropertyName("title")
    var title: String = "",
    @get:PropertyName("bannerUrl") @set:PropertyName("bannerUrl")
    var bannerUrl: String = "",
    @get:PropertyName("game") @set:PropertyName("game")
    var game: String = "Free Fire",
    @get:PropertyName("category") @set:PropertyName("category")
    var category: String = "BR", // BR (Battle Royale / Full Map), CS (Clash Squad), LONE_WOLF (Lone Wolf), SCRIMS (Training / Practice)
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

    @get:PropertyName("registrations") @set:PropertyName("registrations")
    var registrations: List<PlayerRegistration> = emptyList()
) {
    val isCashTournament: Boolean get() = entryFee > 0f || prizePool > 0f
    val isTrainingMatch: Boolean get() = entryFee == 0f || title.contains("Training", ignoreCase = true) || title.contains("Scrim", ignoreCase = true) || title.contains("Practice", ignoreCase = true) || description.contains("training", ignoreCase = true)

    val canonicalCategory: String get() {
        val cat = category.trim().uppercase()
        return when {
            cat == "CS" || cat.contains("CLASH") || format.contains("CS", true) || title.contains("CS", true) || title.contains("Clash Squad", true) -> "CS"
            cat == "LONE_WOLF" || cat == "LONEWOLF" || format.contains("Lone", true) || title.contains("Lone Wolf", true) || title.contains("1v1", true) && !format.contains("CS", true) || map.contains("Iron Cage", true) -> "LONE_WOLF"
            cat == "SCRIMS" || cat == "TRAINING" || isTrainingMatch -> "SCRIMS"
            else -> "BR"
        }
    }

    val categoryDisplayName: String get() = when (canonicalCategory) {
        "BR" -> "Battle Royale (Full Map)"
        "CS" -> "Clash Squad (CS)"
        "LONE_WOLF" -> "Lone Wolf (1v1 / 2v2)"
        "SCRIMS" -> "Custom Scrims & Training"
        else -> "Battle Royale"
    }

    val categoryShortBadge: String get() = when (canonicalCategory) {
        "BR" -> "BR FULL MAP"
        "CS" -> "CLASH SQUAD"
        "LONE_WOLF" -> "LONE WOLF"
        "SCRIMS" -> "SCRIMS / FREE"
        else -> "BATTLE ROYALE"
    }

    val isBattleRoyale: Boolean get() = canonicalCategory == "BR"
    val isClashSquad: Boolean get() = canonicalCategory == "CS"
    val isLoneWolf: Boolean get() = canonicalCategory == "LONE_WOLF"
    val isScrim: Boolean get() = canonicalCategory == "SCRIMS"
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

    fun getCategoryMeta(key: String): CategoryMeta {
        return ALL_CATEGORIES.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: ALL_CATEGORIES[0]
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
    @get:PropertyName("profile") @set:PropertyName("profile")
    var profile: UserProfile? = null,
    @get:PropertyName("tournament") @set:PropertyName("tournament")
    var tournament: Tournament? = null 
)

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



