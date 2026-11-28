package com.example.data.repository

import android.util.Log
import com.example.domain.model.LeaderboardEntry
import com.example.domain.model.PlayerRegistration
import com.example.domain.model.Tournament
import com.example.domain.model.Match
import com.example.domain.model.UserProfile
import com.example.domain.model.ComplaintTicket
import com.example.domain.model.SupportTicket
import com.example.domain.model.TicketMessage
import com.example.domain.model.PayoutRequest
import com.example.domain.model.AdminRecord
import com.example.domain.model.AdminVerificationResult
import com.example.domain.model.BannedUserRecord
import com.example.domain.model.WalletTransaction
import com.example.domain.model.RoomDetails
import com.example.domain.model.CheckInToken
import com.example.domain.model.AppAnnouncementBanner
import com.example.domain.model.MatchProofSubmission
import com.example.domain.model.GlobalAnnouncement
import com.example.domain.model.AppNotification
import com.example.domain.model.BackendExtractionResult
import com.example.domain.model.TournamentCategory
import com.example.domain.model.FreeFireCategories
import com.example.notification.VelorixNotificationManager
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Source
import com.example.ui.common.GlobalErrorManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.functions.FirebaseFunctions
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

import com.example.data.validation.TournamentBackendValidator
import com.example.data.validation.ValidationResult

class TournamentRepositoryImpl(private val context: android.content.Context? = null) {
    private val TAG = "TournamentRepository"
    private val DB_URL = "https://velorix-tournaments-default-rtdb.asia-southeast1.firebasedatabase.app"
    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    val functions: FirebaseFunctions by lazy { FirebaseFunctions.getInstance() }
    val database: com.google.firebase.database.DatabaseReference by lazy {
        try {
            FirebaseDatabase.getInstance(DB_URL).reference
        } catch (_: Exception) {
            FirebaseDatabase.getInstance().reference
        }
    }
    val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    private inline fun <reified T> DataSnapshot.safeGetObject(): T? {
        if (!exists() || !hasChildren() || value !is Map<*, *>) return null
        return try {
            getValue(T::class.java)
        } catch (e: Exception) {
            Log.w(TAG, "Skipping malformed RTDB node key=$key expected=${T::class.java.simpleName}: ${e.message}")
            null
        }
    }

    suspend fun syncAdminCustomClaimsAndRefreshToken(forceRefresh: Boolean = true): Boolean {
        val user = auth.currentUser ?: return false
        val email = user.email?.trim()?.lowercase() ?: ""
        if (!TournamentBackendValidator.isAllowedAdminEmail(email)) return false
        return try {
            withTimeoutOrNull(8_000L) {
                functions.getHttpsCallable("syncAdminCustomClaims").call().await()
            }
            if (forceRefresh) {
                withTimeoutOrNull(5_000L) {
                    user.getIdToken(true).await()
                }
            }
            Log.d(TAG, "syncAdminCustomClaims completed and ID token refreshed for $email")
            true
        } catch (e: Exception) {
            Log.w(TAG, "syncAdminCustomClaims failed or timed out: ${e.message}")
            if (forceRefresh) {
                try {
                    withTimeoutOrNull(4_000L) {
                        user.getIdToken(true).await()
                    }
                } catch (_: Exception) {}
            }
            false
        }
    }
    val locallyDeletedUserIds: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()
    val locallyDeletedTournamentIds: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()
    val locallyCreatedTournaments: java.util.concurrent.ConcurrentHashMap<String, Tournament> = java.util.concurrent.ConcurrentHashMap()
    val lastExtractedUsers: java.util.concurrent.ConcurrentHashMap<String, UserProfile> = java.util.concurrent.ConcurrentHashMap()
    val recentAdminWalletOverrides: java.util.concurrent.ConcurrentHashMap<String, Pair<UserProfile, Long>> = java.util.concurrent.ConcurrentHashMap()
    var activeAdminEmail: String = ""
    var activeAdminUid: String = ""

    private val deletedTournamentsPrefs by lazy {
        context?.getSharedPreferences("velorix_deleted_tournaments_prefs", android.content.Context.MODE_PRIVATE)
    }

    init {
        // Clear any stale deleted tournament cache from previous broken runs so real tournaments are visible
        try {
            val migratedKey = "deleted_cache_reset_v3"
            if (deletedTournamentsPrefs?.getBoolean(migratedKey, false) != true) {
                deletedTournamentsPrefs?.edit()?.remove("deleted_ids")?.putBoolean(migratedKey, true)?.apply()
                locallyDeletedTournamentIds.clear()
            } else {
                loadDeletedTournamentIdsFromPrefs()
            }
        } catch (_: Exception) {}
    }

    private fun loadDeletedTournamentIdsFromPrefs() {
        try {
            val saved = deletedTournamentsPrefs?.getStringSet("deleted_ids", emptySet()) ?: emptySet()
            locallyDeletedTournamentIds.addAll(saved)
        } catch (_: Exception) {}
    }

    fun clearDeletedTournamentCache() {
        locallyDeletedTournamentIds.clear()
        try {
            deletedTournamentsPrefs?.edit()?.remove("deleted_ids")?.apply()
        } catch (_: Exception) {}
    }

    fun markTournamentDeleted(tournamentId: String) {
        val cleanId = tournamentId.trim()
        if (cleanId.isBlank()) return
        locallyDeletedTournamentIds.add(cleanId)
        locallyCreatedTournaments.remove(cleanId)
        try {
            deletedTournamentsPrefs?.edit()?.putStringSet("deleted_ids", locallyDeletedTournamentIds.toSet())?.apply()
        } catch (_: Exception) {}
    }

    fun unmarkTournamentDeleted(tournamentId: String) {
        val cleanId = tournamentId.trim()
        if (cleanId.isBlank()) return
        locallyDeletedTournamentIds.remove(cleanId)
        try {
            deletedTournamentsPrefs?.edit()?.putStringSet("deleted_ids", locallyDeletedTournamentIds.toSet())?.apply()
        } catch (_: Exception) {}
    }

    fun isPhantomOrDeletedTournament(
        tId: String,
        rawKey: String? = null,
        rawTitle: String? = null,
        entryFee: Float = 0f,
        prizePool: Float = 0f,
        roomId: String? = null,
        roomPass: String? = null
    ): Boolean {
        val cleanId = tId.trim()
        val cleanKey = rawKey?.trim() ?: ""
        if (cleanId.isBlank()) return true
        if (locallyDeletedTournamentIds.contains(cleanId) || (cleanKey.isNotBlank() && locallyDeletedTournamentIds.contains(cleanKey))) return true
        val lowerId = cleanId.lowercase()
        if (lowerId.startsWith("mock_") || lowerId.startsWith("sample_")) return true
        val reservedKeys = setOf(
            "BR", "CS", "LONE_WOLF", "SCRIMS",
            "ROOMDETAILS", "PARTICIPANTS", "SLOTS", "REGISTRATIONS",
            "PLAYERS", "TEAMS", "RULES", "CUSTOM_ROOMS", "CATEGORIES", "ITEMS"
        )
        if (cleanId.uppercase() in reservedKeys || cleanKey.uppercase() in reservedKeys) return true
        return false
    }

    fun recordAccountLocally(email: String, username: String = "", role: String = "super_admin", uid: String = "") {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank()) return
        val name = username.ifBlank { cleanEmail.substringBefore("@") }
        if (context != null) {
            try {
                com.example.data.auth.DeviceAccountBindingManager(context).recordAccount(cleanEmail, name, role, autoLogin = false)
            } catch (_: Exception) {}
        }
    }

    /**
     * Automatically verifies and provisions all user accounts upon login/signup.
     * Ensures zero manual verification barrier so every player & admin can log in instantly.
     */
    suspend fun autoVerifyAndProvisionPlayer(
        uid: String,
        identifier: String,
        displayName: String? = null,
        phone: String? = null,
        authProvider: String = "email",
        dateOfBirth: String? = null,
        age: Int? = null,
        isUnder18: Boolean? = null
    ): Boolean {
        val cleanIdentifier = identifier.trim().lowercase().ifBlank { "player_$uid" }
        val effectiveName = displayName?.ifBlank { null } ?: cleanIdentifier.substringBefore("@").ifBlank { "Player" }
        val now = System.currentTimeMillis()
        val safeKey = cleanIdentifier.replace(".", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_")

        val computedAge = if (age != null && age > 0) age else if (!dateOfBirth.isNullOrBlank()) com.example.data.validation.TournamentBackendValidator.calculateAgeFromDob(dateOfBirth) else 0
        val computedUnder18 = isUnder18 ?: if (computedAge > 0) (computedAge < 18) else false
        val cashEligible = !computedUnder18 && (computedAge >= 18 || computedAge <= 0)

        val profilePayload = mutableMapOf<String, Any>(
            "id" to uid,
            "uid" to uid,
            "email" to (if (cleanIdentifier.contains("@")) cleanIdentifier else ""),
            "phoneNumber" to (phone ?: if (!cleanIdentifier.contains("@")) cleanIdentifier else ""),
            "name" to effectiveName,
            "username" to effectiveName,
            "displayName" to effectiveName,
            "role" to "player",
            "isVerified" to true,
            "verified" to true,
            "accountStatus" to "VERIFIED",
            "status" to "ACTIVE",
            "emailVerified" to true,
            "phoneVerified" to (phone != null || !cleanIdentifier.contains("@")),
            "isBanned" to false,
            "walletFrozen" to false,
            "authProvider" to authProvider,
            "vipTier" to "ACTIVE_PLAYER",
            "dateOfBirth" to (dateOfBirth ?: ""),
            "age" to computedAge,
            "isUnder18" to computedUnder18,
            "eligibleForCashTournaments" to cashEligible,
            "ageConfirmed" to (computedAge > 0 || !dateOfBirth.isNullOrBlank()),
            "lastLoginAt" to now,
            "updatedAt" to now
        )

        return try {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    database.child("users").child(uid).updateChildren(profilePayload)
                    database.child("userProfiles").child(uid).updateChildren(profilePayload)
                    database.child("players").child(uid).updateChildren(profilePayload)
                    if (safeKey.isNotBlank()) {
                        database.child("users").child(safeKey).updateChildren(profilePayload)
                    }
                } catch (_: Exception) {}

                try {
                    firestore.collection("users").document(uid).set(profilePayload, com.google.firebase.firestore.SetOptions.merge())
                    firestore.collection("userProfiles").document(uid).set(profilePayload, com.google.firebase.firestore.SetOptions.merge())
                    firestore.collection("players").document(uid).set(profilePayload, com.google.firebase.firestore.SetOptions.merge())
                } catch (_: Exception) {}
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun verifyAndRegisterAdmin(
        uid: String,
        email: String,
        displayName: String? = null,
        dateOfBirth: String? = null,
        age: Int? = null,
        isUnder18: Boolean? = null
    ): AdminVerificationResult {
        val cleanEmail = email.trim().lowercase()
        if (uid.isBlank() || !TournamentBackendValidator.isAllowedAdminEmail(cleanEmail)) {
            return AdminVerificationResult(
                isAuthorized = false,
                role = "unauthorized",
                errorMessage = "Access Denied: Only authorized Velorix administrators may access this console."
            )
        }
        val effectiveName = displayName?.ifBlank { null } ?: cleanEmail.substringBefore("@").ifBlank { "Admin" }
        val now = System.currentTimeMillis()
        val effectiveRole = "super_admin"
        val emailKey = cleanEmail.replace(".", "_")

        val computedAge = if (age != null && age > 0) age else if (!dateOfBirth.isNullOrBlank()) TournamentBackendValidator.calculateAgeFromDob(dateOfBirth) else 0
        val computedUnder18 = isUnder18 ?: if (computedAge > 0) (computedAge < 18) else false
        val cashEligible = !computedUnder18 && (computedAge >= 18 || computedAge <= 0)

        recordAccountLocally(cleanEmail, effectiveName, effectiveRole, uid)

        val adminPayload = mapOf<String, Any>(
            "uid" to uid,
            "id" to uid,
            "email" to cleanEmail,
            "name" to effectiveName,
            "username" to effectiveName,
            "displayName" to effectiveName,
            "role" to effectiveRole,
            "userType" to "ADMIN",
            "accountType" to "ADMIN",
            "isAdmin" to true,
            "admin" to true,
            "isSuperAdmin" to true,
            "isAuthorized" to true,
            "canEditRules" to true,
            "canManageTournaments" to true,
            "canManageUsers" to true,
            "canManagePayouts" to true,
            "canDeleteTournaments" to true,
            "canApproveProofs" to true,
            "canPublishMatches" to true,
            "canManageRooms" to true,
            "active" to true,
            "status" to "ACTIVE",
            "lastLoginAt" to now,
            "updatedAt" to now,
            "grantedBy" to "system_root",
            "grantedAt" to now
        )

        val userPayload = mapOf<String, Any>(
            "id" to uid,
            "uid" to uid,
            "email" to cleanEmail,
            "name" to effectiveName,
            "username" to effectiveName,
            "role" to effectiveRole,
            "lastLoginAt" to now,
            "isBanned" to false,
            "walletFrozen" to false,
            "dateOfBirth" to (dateOfBirth ?: ""),
            "age" to computedAge,
            "isUnder18" to computedUnder18,
            "eligibleForCashTournaments" to cashEligible,
            "ageConfirmed" to (computedAge > 0 || !dateOfBirth.isNullOrBlank())
        )

        // Immediately sync admin custom claims via Cloud Function and force-refresh Firebase ID token
        syncAdminCustomClaimsAndRefreshToken(forceRefresh = true)

        try {
            kotlinx.coroutines.withTimeoutOrNull(4000L) {
                firestore.collection("admins").document(uid).set(adminPayload, SetOptions.merge()).await()
            }
        } catch (_: Exception) {}

        // Asynchronously sync secondary admin & profile records without overwriting balances
        CoroutineScope(Dispatchers.IO).launch {
            try {
                firestore.collection("admins").document(cleanEmail).set(adminPayload, SetOptions.merge())
                firestore.collection("users").document(uid).set(userPayload, SetOptions.merge())
                firestore.collection("userProfiles").document(uid).set(userPayload, SetOptions.merge())
            } catch (_: Exception) {}

            try {
                database.child("admins").child(uid).updateChildren(adminPayload)
                database.child("admins").child(emailKey).updateChildren(adminPayload)
            } catch (_: Exception) {}
        }

        return AdminVerificationResult(
            isAuthorized = true,
            role = effectiveRole
        )
    }

    suspend fun syncAdminPermissionsNow(targetEmail: String = ""): Boolean {
        return try {
            val user = auth.currentUser ?: return false
            val email = user.email?.trim()?.lowercase() ?: ""
            if (!TournamentBackendValidator.isAllowedAdminEmail(email)) return false
            syncAdminCustomClaimsAndRefreshToken(forceRefresh = true)
            val name = user.displayName ?: email.substringBefore("@")
            val res = verifyAndRegisterAdmin(user.uid, email, name)
            res.isAuthorized
        } catch (_: Exception) {
            false
        }
    }

    suspend fun updateUserDateOfBirth(uid: String, dateOfBirth: String): Boolean {
        val calculatedAge = com.example.data.validation.TournamentBackendValidator.calculateAgeFromDob(dateOfBirth)
        val isUnder18 = calculatedAge < 18
        val eligibleForCash = calculatedAge >= 18
        val now = System.currentTimeMillis()
        val updates = mapOf<String, Any>(
            "dateOfBirth" to dateOfBirth,
            "age" to calculatedAge,
            "isUnder18" to isUnder18,
            "eligibleForCashTournaments" to eligibleForCash,
            "ageConfirmed" to true,
            "updatedAt" to now
        )
        return try {
            database.child("users").child(uid).updateChildren(updates).await()
            database.child("userProfiles").child(uid).updateChildren(updates).await()
            database.child("players").child(uid).updateChildren(updates).await()
            firestore.collection("users").document(uid).set(updates, com.google.firebase.firestore.SetOptions.merge()).await()
            firestore.collection("userProfiles").document(uid).set(updates, com.google.firebase.firestore.SetOptions.merge()).await()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun getProductionRtdbRules(): String {
        return java.io.File("/database.rules.json").let { if (it.exists()) it.readText() else "" }
    }

    fun getProductionFirestoreRules(): String {
        return """
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    function isAdmin() {
      return request.auth != null && (
        request.auth.token.email == 'anantisback47@gmail.com' ||
        request.auth.token.email == 'service.veloxyra@gmail.com' ||
        request.auth.token.email == 'verlorix-test@gmail.com'
      );
    }

    match /admins/{document=**} {
      allow read, write: if request.auth != null;
    }

    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && (request.auth.uid == userId || isAdmin());
    }

    match /userProfiles/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && (request.auth.uid == userId || isAdmin());
    }

    match /tournaments/{document=**} {
      allow read: if true;
      allow write: if isAdmin();
    }

    match /matches/{document=**} {
      allow read: if true;
      allow write: if isAdmin();
    }

    match /payout_requests/{document=**} {
      allow read, write: if request.auth != null;
    }

    match /support_tickets/{document=**} {
      allow read, write: if request.auth != null;
    }

    match /complaints/{document=**} {
      allow read, write: if request.auth != null;
    }

    match /banners/{document=**} {
      allow read: if true;
      allow write: if isAdmin();
    }

    match /{document=**} {
      allow read, write: if isAdmin();
    }
  }
}
        """.trimIndent()
    }

    private val authSessionMutex = kotlinx.coroutines.sync.Mutex()
    @Volatile
    private var lastSyncSignInAttemptMs: Long = 0L

    suspend fun ensureAuthenticatedSession(targetEmail: String = ""): Boolean {
        val current = auth.currentUser
        val authEmail = current?.email?.trim()?.lowercase() ?: ""
        if (current == null || !TournamentBackendValidator.isAllowedAdminEmail(authEmail)) {
            if (current != null) {
                try {
                    auth.signOut()
                } catch (_: Exception) {}
            }
            activeAdminEmail = ""
            activeAdminUid = ""
            return false
        }

        activeAdminUid = current.uid
        activeAdminEmail = authEmail
        val name = current.displayName?.ifBlank { null } ?: authEmail.substringBefore("@")

        try {
            kotlinx.coroutines.withTimeoutOrNull(8000L) {
                verifyAndRegisterAdmin(current.uid, authEmail, name)
            }
        } catch (_: Exception) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    verifyAndRegisterAdmin(current.uid, authEmail, name)
                } catch (_: Exception) {}
            }
        }
        return true
    }

    private fun safeDouble(value: Any?, default: Double = 0.0): Double {
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: default
            else -> default
        }
    }

    private fun safeFloat(value: Any?): Float {
        return when (value) {
            is Number -> value.toFloat()
            is String -> {
                val cleaned = value.replace(Regex("[^0-9.]"), "")
                cleaned.toFloatOrNull() ?: 0f
            }
            else -> 0f
        }
    }

    private fun safeInt(value: Any?, default: Int = 0): Int {
        return when (value) {
            is Number -> value.toInt()
            is String -> {
                val cleaned = value.replace(Regex("[^0-9]"), "")
                cleaned.toIntOrNull() ?: default
            }
            else -> default
        }
    }

    private fun safeLong(value: Any?, default: Long = 0L): Long {
        return when (value) {
            is Number -> value.toLong()
            is String -> {
                val cleaned = value.replace(Regex("[^0-9]"), "")
                cleaned.toLongOrNull() ?: default
            }
            else -> default
        }
    }

    private fun DataSnapshot.toUserProfile(): UserProfile? {
        if (!exists()) return null
        
        fun findString(vararg keys: String): String? {
            // 1. Exact direct key
            for (k in keys) {
                val direct = child(k).value?.toString()
                if (!direct.isNullOrBlank()) return direct
            }
            // 2. Case-insensitive direct key
            for (k in keys) {
                for (c in children) {
                    if (c.key.equals(k, ignoreCase = true)) {
                        val v = c.value?.toString()
                        if (!v.isNullOrBlank()) return v
                    }
                }
            }
            // 3. Common nested containers (exact & case-insensitive)
            for (containerName in listOf("profile", "account", "data", "info", "user", "userData", "wallet", "basic_info", "details", "player")) {
                val container = child(containerName).takeIf { it.exists() }
                    ?: children.find { it.key.equals(containerName, ignoreCase = true) }
                if (container != null && container.exists()) {
                    for (k in keys) {
                        val vDirect = container.child(k).value?.toString()
                        if (!vDirect.isNullOrBlank()) return vDirect
                        for (c in container.children) {
                            if (c.key.equals(k, ignoreCase = true)) {
                                val v = c.value?.toString()
                                if (!v.isNullOrBlank()) return v
                            }
                        }
                    }
                }
            }
            return null
        }

        fun findDouble(vararg keys: String): Double {
            for (k in keys) {
                val v = child(k).value
                if (v != null) {
                    val d = safeDouble(v)
                    if (d != 0.0) return d
                }
            }
            for (k in keys) {
                for (c in children) {
                    if (c.key.equals(k, ignoreCase = true)) {
                        val v = c.value
                        if (v != null) {
                            val d = safeDouble(v)
                            if (d != 0.0) return d
                        }
                    }
                }
            }
            for (containerName in listOf("profile", "account", "data", "info", "wallet", "balances", "funds", "money")) {
                val container = child(containerName).takeIf { it.exists() }
                    ?: children.find { it.key.equals(containerName, ignoreCase = true) }
                if (container != null && container.exists()) {
                    for (k in keys) {
                        val nested = container.child(k).value
                        if (nested != null) {
                            val d = safeDouble(nested)
                            if (d != 0.0) return d
                        }
                        for (c in container.children) {
                            if (c.key.equals(k, ignoreCase = true)) {
                                val cv = c.value
                                if (cv != null) {
                                    val d = safeDouble(cv)
                                    if (d != 0.0) return d
                                }
                            }
                        }
                    }
                }
            }
            return 0.0
        }

        val rawKey = key ?: ""
        val foundId = findString("uid", "id", "userId", "user_id", "playerUid", "accountId", "account_id")
        val uId = when {
            rawKey.isNotBlank() && !rawKey.startsWith("acc_") && !rawKey.startsWith("admin_anant_master") -> rawKey
            !foundId.isNullOrBlank() && !foundId.startsWith("acc_") && !foundId.startsWith("admin_anant_master") -> foundId
            rawKey.isNotBlank() -> rawKey
            else -> foundId ?: return null
        }

        val directIgn = findString("ign", "IGN", "inGameName", "InGameName", "in_game_name", "playerName", "PlayerName", "player_name", "ff_ign", "ffIgn", "FF_IGN", "ff_name", "ffName", "gameUsername", "nick", "nickname", "freeFireName", "free_fire_name") ?: ""
        val rawEmail = findString("email", "Email", "user_email", "userEmail", "UserEmail", "mail", "Mail", "emailAddress", "EmailAddress", "contactEmail", "userMail", "email_id", "emailId", "accountEmail", "loginEmail", "gmail") ?: ""
        val phoneOrEmailVal = findString("phoneOrEmail", "phone_or_email", "PhoneOrEmail") ?: ""
        var email = if (rawEmail.isNotBlank()) rawEmail else if (phoneOrEmailVal.contains("@")) phoneOrEmailVal else ""
        if (email.isBlank()) {
            for (c in children) {
                val str = c.value?.toString()?.trim() ?: ""
                if (str.contains("@") && str.contains(".") && !str.contains(" ") && str.length >= 6) {
                    email = str
                    break
                }
            }
        }

        val directPhone = findString("phone", "Phone", "phoneNumber", "PhoneNumber", "phone_number", "mobile", "Mobile", "contact", "contactNumber", "tel", "phone_no", "mobileNumber")
            ?: (if (!phoneOrEmailVal.contains("@") && phoneOrEmailVal.isNotBlank()) phoneOrEmailVal else "")
        var phone = directPhone
        if (phone.isBlank()) {
            for (c in children) {
                val str = c.value?.toString()?.trim() ?: ""
                if ((str.startsWith("+91") && str.length >= 12) || (str.length in 10..12 && str.all { it.isDigit() || it == '+' })) {
                    phone = str
                    break
                }
            }
        }

        val directUName = findString(
            "name", "Name", "username", "Username", "displayName", "DisplayName", "display_name", "ign", "IGN",
            "inGameName", "in_game_name", "playerName", "player_name",
            "user_name", "ff_name", "nick", "nickname", "fullName", "FullName", "gamerTag"
        )
        val uName = when {
            !directUName.isNullOrBlank() && directUName != "Player" && !directUName.startsWith("Player (") -> directUName
            directIgn.isNotBlank() && directIgn != "Player" -> directIgn
            email.isNotBlank() && email.contains("@") -> email.substringBefore("@")
            phone.isNotBlank() -> "Player (${phone.takeLast(4)})"
            !directUName.isNullOrBlank() && directUName != "Player" -> directUName
            uId.isNotBlank() && !uId.startsWith("usr_") && !uId.startsWith("acc_") && !uId.startsWith("admin_") -> "Player (${uId.take(6)})"
            else -> "Player"
        }

        var avatarUrl = findString(
            "avatarUrl", "avatar_url", "photoUrl", "photo_url", "photoURL",
            "profilePic", "profile_pic", "profilePicture", "profile_picture",
            "profileImage", "profile_image", "avatar", "image", "userAvatar", "user_avatar",
            "picture", "profilePicUrl", "profile_pic_url", "icon", "imageUrl", "image_url", "userPhoto"
        )
        if (avatarUrl.isNullOrBlank()) {
            for (c in children) {
                val str = c.value?.toString()?.trim() ?: ""
                if ((str.startsWith("http://") || str.startsWith("https://")) && 
                    (str.contains("googleusercontent") || str.contains("firebasestorage") || str.contains("avatar") || str.contains("profile") || str.contains("photo") || str.contains("image"))
                ) {
                    avatarUrl = str
                    break
                }
            }
        }
        val role = findString("role", "user_role", "type", "userType") ?: "player"

        var depositFunds = findDouble("depositBalance", "depositFunds", "deposit_funds", "deposit_balance", "deposits", "depositedMoney", "deposited_money")
        if (depositFunds == 0.0) {
            depositFunds = safeDouble(child("walletAndFinanceSummary").child("depositedMoney").value)
        }
        var winningFunds = findDouble("winningsBalance", "winningFunds", "winning_funds", "winnings", "winningBalance", "winning_balance", "winningMoney", "winning_money", "withdrawableBalance", "withdrawable_balance")
        if (winningFunds == 0.0) {
            winningFunds = safeDouble(child("walletAndFinanceSummary").child("winningMoney").value)
        }
        var bonusFunds = findDouble("bonusBalance", "bonusFunds", "bonus_funds", "referralBonus", "referral_bonus", "referralFunds", "referral_funds", "bonus", "promoCoins", "bonusMoney", "bonus_money")
        if (bonusFunds == 0.0) {
            bonusFunds = safeDouble(
                child("walletAndFinanceSummary").child("bonusMoney").value
                    ?: child("walletAndFinanceSummary").child("referralBonus").value
            )
        }
        val lockedBalance = findDouble("lockedBalance", "locked_balance")
        val tokensCount = safeInt(child("tokens").value ?: child("tokenBalance").value ?: child("vtTokens").value, 0)

        val hasExplicitBuckets = hasChild("depositBalance") || hasChild("depositFunds") || hasChild("deposit_funds") ||
            hasChild("winningsBalance") || hasChild("winningFunds") || hasChild("winning_funds") ||
            hasChild("bonusBalance") || hasChild("bonusFunds") || hasChild("bonus_funds") ||
            hasChild("referralBonus") || hasChild("walletAndFinanceSummary")

        var funds = findDouble("balance", "funds", "wallet", "walletBalance", "wallet_balance", "coins", "vtCoins", "amount", "money", "credits")
        if ((depositFunds + winningFunds + bonusFunds) > 0.0 || hasExplicitBuckets) {
            funds = depositFunds + winningFunds + bonusFunds
        } else if (funds > 0.0) {
            // Legacy uncategorized funds default to Deposited Money (non-withdrawable)
            depositFunds = funds
        }

        fun parseExplicitBool(v: Any?): Boolean? = when (v) {
            is Boolean -> v
            is String -> when {
                v.trim().equals("true", ignoreCase = true) -> true
                v.trim().equals("false", ignoreCase = true) -> false
                else -> null
            }
            is Number -> v.toInt() != 0
            else -> null
        }

        val explicitBanBool = parseExplicitBool(child("isBanned").value)
            ?: parseExplicitBool(child("banned").value)
            ?: parseExplicitBool(child("profile").child("isBanned").value)
            ?: parseExplicitBool(child("profile").child("banned").value)
        val rawStatusStr = (findString("accountStatus", "status", "userStatus") ?: "").trim().uppercase()
        val banReason = findString("banReason", "ban_reason", "reason") ?: ""
        val banCaseId = findString("banCaseId", "ban_case_id", "sanctionId", "sanction_id", "caseId") ?: ""
        val bannedAt = safeLong(child("bannedAt").value ?: child("banned_at").value, 0L)
        val activeAppealTicketId = findString("activeAppealTicketId", "activeAppealTicket", "appealTicketId", "ticketId") ?: ""
        val activeAppealStatus = findString("activeAppealStatus", "appealStatus") ?: ""
        val isBanned = when {
            explicitBanBool == false && (rawStatusStr == "ACTIVE" || rawStatusStr == "UNBANNED" || rawStatusStr.isEmpty()) -> false
            explicitBanBool == true -> true
            rawStatusStr == "BANNED" || rawStatusStr == "RESTRICTED" || rawStatusStr == "PERMANENT_BAN" -> true
            explicitBanBool == null && banCaseId.startsWith("BAN-", ignoreCase = true) && rawStatusStr != "ACTIVE" && rawStatusStr != "UNBANNED" -> true
            else -> false
        }

        val isSuspended = (child("isSuspended").value as? Boolean)
            ?: (child("suspended").value as? Boolean)
            ?: (child("status").value?.toString()?.equals("suspended", ignoreCase = true) == true)
            ?: (child("accountStatus").value?.toString()?.equals("SUSPENDED", ignoreCase = true) == true)
            ?: false

        val suspendedUntil = safeLong(
            child("suspensionEndTimeMillis").value ?: child("suspendedUntil").value ?: child("suspended_until").value ?: child("suspensionExpiresAt").value,
            0L
        )
        val suspensionReason = findString("suspensionReason", "suspension_reason") ?: ""

        val isVpnBlocked = (child("isVpnBlocked").value as? Boolean)
            ?: (child("vpnBlocked").value as? Boolean)
            ?: false

        val isForceUpdateRequired = (child("isForceUpdateRequired").value as? Boolean)
            ?: (child("forceUpdate").value as? Boolean)
            ?: false

        val minVersionRequired = findString("minVersionRequired", "minVersion", "min_version") ?: ""
        val isMaintenanceBypass = (child("isMaintenanceBypass").value as? Boolean)
            ?: (child("maintenanceBypass").value as? Boolean)
            ?: false

        val deviceModel = findString("deviceModel", "device_model", "model", "device", "deviceName") ?: ""
        val ipAddress = findString("ipAddress", "ip_address", "ip", "lastIp") ?: ""

        val lastActive = safeLong(
            child("lastActive").value 
                ?: child("lastLoginAt").value 
                ?: child("last_login").value 
                ?: child("updatedAt").value 
                ?: child("createdAt").value,
            System.currentTimeMillis()
        )

        val createdAt = safeLong(
            child("createdAt").value 
                ?: child("created_at").value 
                ?: child("registeredAt").value 
                ?: child("registered_at").value 
                ?: child("joinedAt").value 
                ?: child("timestamp").value,
            0L
        )

        val gameId = findString("gameId", "game_id", "gameAccountId", "freeFireId", "free_fire_id", "ffId", "ff_id", "playerUid", "inGameId", "in_game_id") ?: ""

        val wins = safeInt(child("wins").value ?: child("totalWins").value ?: child("total_wins").value ?: child("matchesWon").value)
        val kills = safeInt(child("kills").value ?: child("totalKills").value ?: child("total_kills").value)
        val activityPoints = safeInt(child("activityPoints").value ?: child("points").value ?: child("activity_points").value)
        val totalEarnings = safeDouble(child("totalEarnings").value ?: child("total_earnings").value ?: child("earnings").value ?: child("won").value)

        val referralCode = findString("referralCode", "referral_code", "refCode") ?: ""
        val referredBy = findString("referredBy", "referred_by", "invitedBy") ?: ""
        val referralCount = safeInt(child("referralCount").value ?: child("referral_count").value ?: child("referrals").value)
        val referralBonusEarned = safeDouble(child("referralEarnings").value ?: child("referralBonusEarned").value ?: child("referral_bonus").value ?: child("referral_earnings").value)
        val bio = findString("bio", "about", "statusMessage") ?: ""
        val socialLink = findString("socialLink", "social_link", "instagram", "youtube") ?: ""
        val kycVerified = (child("kycVerified").value as? Boolean) ?: (child("isKycVerified").value as? Boolean) ?: false
        val dataExported = (child("dataExported").value as? Boolean) ?: false
        val lastExportedAt = safeLong(child("lastExportedAt").value, 0L)
        val lastExportId = findString("lastExportId") ?: ""
        val deletionRequested = (child("deletionRequested").value as? Boolean) ?: false
        val deletionRequestId = findString("deletionRequestId") ?: ""
        val deletionReason = findString("deletionReason") ?: ""
        val deletionDetails = findString("deletionDetails") ?: ""
        val deletionRequestedAt = safeLong(child("deletionRequestedAt").value, 0L)
        val accountStatus = findString("accountStatus", "status")
            ?: (if (isBanned) "BANNED" else if (isSuspended) "SUSPENDED" else if (deletionRequested) "PENDING_DELETION" else "ACTIVE")
        val versionNum = safeInt(child("version").value, 1).coerceAtLeast(1)
        val updatedAtTs = safeLong(child("updatedAt").value, lastActive)

        val directBalanceLong = (child("balance").value as? Long) ?: (child("balance").value as? Number)?.toLong()
        val directBalance = directBalanceLong?.toDouble() ?: findDouble("balance", "funds", "wallet", "walletBalance", "wallet_balance", "coins", "vtCoins", "amount", "money", "credits")

        val dateOfBirth = findString("dateOfBirth", "dob", "birthDate", "birth_date", "date_of_birth") ?: ""
        var age = safeInt(child("age").value ?: child("userAge").value ?: child("user_age").value)
        if (age <= 0 && dateOfBirth.isNotBlank()) {
            age = com.example.data.validation.TournamentBackendValidator.calculateAgeFromDob(dateOfBirth)
        }
        val isUnder18 = (child("isUnder18").value as? Boolean)
            ?: (child("under18").value as? Boolean)
            ?: (age in 1..17)
        val eligibleForCashTournaments = (child("eligibleForCashTournaments").value as? Boolean)
            ?: (!isUnder18 && (age >= 18 || age <= 0))
        val ageConfirmed = (child("ageConfirmed").value as? Boolean)
            ?: (dateOfBirth.isNotBlank())

        android.util.Log.d("AdminPanel", "User: $uId | Email: $email | IGN: $directIgn | Balance: ${directBalanceLong ?: directBalance.toLong()} | DOB: $dateOfBirth | Age: $age | isUnder18: $isUnder18")

        val rawMap = mutableMapOf<String, String>()
        fun extractChildren(prefix: String, snap: DataSnapshot) {
            if (snap.childrenCount > 0) {
                snap.children.forEach { c ->
                    val nextPrefix = if (prefix.isEmpty()) (c.key ?: "") else "$prefix.${c.key ?: ""}"
                    extractChildren(nextPrefix, c)
                }
            } else {
                snap.value?.let { v ->
                    rawMap[if (prefix.isEmpty()) (snap.key ?: "") else prefix] = v.toString()
                }
            }
        }
        extractChildren("", this)

        val resolvedUsername = when {
            uName.isNotBlank() && uName != "Player" && !uName.startsWith("Player (") -> uName
            directIgn.isNotBlank() && directIgn != "Player" -> directIgn
            email.isNotBlank() && email.contains("@") -> email.substringBefore("@")
            directIgn.isNotBlank() -> directIgn
            uName.isNotBlank() && uName != "Player" -> uName
            uId.isNotBlank() && !uId.startsWith("usr_") && !uId.startsWith("acc_") -> "Player (${uId.take(6)})"
            else -> "Player"
        }

        val isGuest = (child("isGuest").value as? Boolean)
            ?: (child("is_guest").value as? Boolean)
            ?: (role.equals("guest", ignoreCase = true))
            ?: (rawMap["isGuest"]?.toBooleanStrictOrNull())
            ?: (rawMap["provider"]?.equals("anonymous", ignoreCase = true) == true)
            ?: (rawMap["providerId"]?.equals("anonymous", ignoreCase = true) == true)
            ?: false

        val isAnonymous = (child("isAnonymous").value as? Boolean)
            ?: (child("is_anonymous").value as? Boolean)
            ?: (role.equals("anonymous", ignoreCase = true))
            ?: (rawMap["isAnonymous"]?.toBooleanStrictOrNull())
            ?: false

        val firstAppJoinAt = safeLong(
            child("firstAppJoinAt").value
                ?: child("accountLifecycle").child("firstAppJoinAt").value
                ?: child("appJoinedAt").value
                ?: child("createdAt").value,
            createdAt
        )
        var firstMatchJoinedAt = safeLong(
            child("firstMatchJoinedAt").value
                ?: child("accountLifecycle").child("firstMatchJoinedAt").value
                ?: child("first_match_joined_at").value,
            0L
        )
        var lastMatchJoinedAt = safeLong(
            child("lastMatchJoinedAt").value
                ?: child("accountLifecycle").child("lastMatchJoinedAt").value
                ?: child("last_match_joined_at").value,
            0L
        )
        var totalMatchesJoined = safeInt(
            child("totalMatchesJoined").value
                ?: child("combatAndMatchSummary").child("totalMatchesJoined").value
                ?: child("matchesJoined").value,
            0
        )
        val totalDeposited = maxOf(
            safeDouble(child("totalDeposited").value ?: child("walletAndFinanceSummary").child("totalLifetimeDeposited").value),
            depositFunds
        )
        val totalWithdrawn = safeDouble(
            child("totalWithdrawn").value ?: child("walletAndFinanceSummary").child("totalLifetimeWithdrawn").value
        )
        var totalEntryFeesPaid = safeDouble(
            child("totalEntryFeesPaid").value ?: child("walletAndFinanceSummary").child("totalLifetimeEntryFeesPaid").value
        )
        val supportCasesCount = safeInt(
            child("supportCasesCount").value ?: child("combatAndMatchSummary").child("supportCasesCount").value ?: child("casesFiled").value,
            0
        )

        val parsedMatchHistory = mutableListOf<com.example.domain.model.PlayerMatchRecord>()
        val joinedMatchesSnap = if (hasChild("joinedMatches")) child("joinedMatches") else child("matchHistory")
        for (mChild in joinedMatchesSnap.children) {
            val mTourneyId = mChild.child("tournamentId").value?.toString() ?: mChild.key ?: ""
            val mTitle = mChild.child("tournamentTitle").value?.toString() ?: mChild.child("title").value?.toString() ?: ""
            if (mTourneyId.isNotBlank()) {
                val mJoinedAt = safeLong(mChild.child("joinedAt").value ?: mChild.child("registeredAt").value, 0L)
                val mFee = safeDouble(mChild.child("entryFeePaid").value ?: mChild.child("entryFee").value)
                parsedMatchHistory.add(
                    com.example.domain.model.PlayerMatchRecord(
                        tournamentId = mTourneyId,
                        tournamentTitle = mTitle.ifBlank { "Match #$mTourneyId" },
                        game = mChild.child("game").value?.toString() ?: "Free Fire",
                        category = mChild.child("category").value?.toString() ?: "BR",
                        format = mChild.child("format").value?.toString() ?: "SOLO",
                        map = mChild.child("map").value?.toString() ?: "Bermuda",
                        joinedAt = mJoinedAt,
                        entryFeePaid = mFee,
                        paidFromDeposit = safeDouble(mChild.child("paidFromDeposit").value),
                        paidFromWinning = safeDouble(mChild.child("paidFromWinning").value),
                        paidFromBonus = safeDouble(mChild.child("paidFromBonus").value),
                        kills = safeInt(mChild.child("kills").value ?: mChild.child("killsInMatch").value),
                        rank = safeInt(mChild.child("rank").value ?: mChild.child("placementRank").value),
                        winningAmount = safeDouble(mChild.child("winningAmount").value ?: mChild.child("prizeWon").value),
                        status = mChild.child("status").value?.toString() ?: "JOINED"
                    )
                )
            }
        }
        if (parsedMatchHistory.isNotEmpty()) {
            val validTimes = parsedMatchHistory.mapNotNull { if (it.joinedAt > 0L) it.joinedAt else null }
            if (firstMatchJoinedAt <= 0L && validTimes.isNotEmpty()) firstMatchJoinedAt = validTimes.minOrNull() ?: 0L
            if (lastMatchJoinedAt <= 0L && validTimes.isNotEmpty()) lastMatchJoinedAt = validTimes.maxOrNull() ?: 0L
            totalMatchesJoined = maxOf(totalMatchesJoined, parsedMatchHistory.size)
            totalEntryFeesPaid = maxOf(totalEntryFeesPaid, parsedMatchHistory.sumOf { it.entryFeePaid })
        }
        val joinedIds = parsedMatchHistory.map { it.tournamentId }.distinct()

        return UserProfile(
            id = uId,
            username = resolvedUsername,
            avatarUrl = avatarUrl,
            role = role,
            email = email,
            phone = phone,
            phoneOrEmail = phoneOrEmailVal.ifBlank { email.ifBlank { phone } },
            bio = bio,
            socialLink = socialLink,
            depositFunds = depositFunds,
            winningFunds = winningFunds,
            bonusFunds = bonusFunds,
            lockedBalance = lockedBalance,
            funds = if ((depositFunds + winningFunds + bonusFunds) > 0.0) (depositFunds + winningFunds + bonusFunds) else if (funds > 0.0) funds else directBalance,
            balance = if ((depositFunds + winningFunds + bonusFunds) > 0.0) (depositFunds + winningFunds + bonusFunds) else directBalance,
            tokens = tokensCount,
            wins = wins,
            kills = maxOf(kills, parsedMatchHistory.sumOf { it.kills }),
            activityPoints = activityPoints,
            totalEarnings = maxOf(totalEarnings, parsedMatchHistory.sumOf { it.winningAmount }),
            kycVerified = kycVerified,
            dataExported = dataExported,
            lastExportedAt = lastExportedAt,
            lastExportId = lastExportId,
            isBanned = isBanned,
            banReason = banReason,
            banCaseId = banCaseId,
            bannedAt = bannedAt,
            activeAppealTicketId = activeAppealTicketId,
            activeAppealStatus = activeAppealStatus,
            isSuspended = isSuspended,
            suspendedUntil = suspendedUntil,
            suspensionReason = suspensionReason,
            accountStatus = accountStatus,
            deletionRequested = deletionRequested,
            deletionRequestId = deletionRequestId,
            deletionReason = deletionReason,
            deletionDetails = deletionDetails,
            deletionRequestedAt = deletionRequestedAt,
            version = versionNum,
            updatedAt = updatedAtTs,
            isVpnBlocked = isVpnBlocked,
            isForceUpdateRequired = isForceUpdateRequired,
            minVersionRequired = minVersionRequired,
            isMaintenanceBypass = isMaintenanceBypass,
            deviceModel = deviceModel,
            ipAddress = ipAddress,
            dateOfBirth = dateOfBirth,
            age = age,
            isUnder18 = isUnder18,
            eligibleForCashTournaments = eligibleForCashTournaments,
            ageConfirmed = ageConfirmed,
            lastActive = lastActive,
            lastActiveAt = safeLong(child("lastActiveAt").value, lastActive),
            isOnline = (child("isOnline").value as? Boolean)
                ?: (child("isOnline").value?.toString()?.equals("true", ignoreCase = true) == true),
            fcmToken = (child("fcmToken").value ?: child("messagingToken").value ?: child("pushToken").value)?.toString()?.trim() ?: "",
            createdAt = if (createdAt > 0L) createdAt else firstAppJoinAt,
            firstAppJoinAt = firstAppJoinAt,
            firstMatchJoinedAt = firstMatchJoinedAt,
            lastMatchJoinedAt = lastMatchJoinedAt,
            totalMatchesJoined = totalMatchesJoined,
            totalDeposited = totalDeposited,
            totalWithdrawn = totalWithdrawn,
            totalEntryFeesPaid = totalEntryFeesPaid,
            supportCasesCount = supportCasesCount,
            joinedMatchIds = joinedIds,
            matchHistory = parsedMatchHistory.sortedByDescending { it.joinedAt },
            gameId = gameId,
            ign = directIgn.ifBlank { if (resolvedUsername != "Player") resolvedUsername else "" },
            referralCode = referralCode,
            referredBy = referredBy,
            referralCount = referralCount,
            referralBonusEarned = referralBonusEarned,
            isGuest = isGuest,
            isAnonymous = isAnonymous,
            rawAttributes = rawMap
        )
    }

    private fun formatScheduleTime(raw: String?): String {
        if (raw.isNullOrBlank()) return "Starting Soon"
        val trimmed = raw.trim()
        val millis = trimmed.toLongOrNull()
        return if (millis != null && millis > 1000000000L) {
            val actualMillis = if (millis < 100000000000L) millis * 1000L else millis
            try {
                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(actualMillis))
            } catch (_: Exception) {
                trimmed
            }
        } else {
            trimmed
        }
    }

    private fun DocumentSnapshot.safeString(vararg keys: String): String? {
        for (k in keys) {
            try {
                val v = get(k) ?: continue
                if (v is com.google.firebase.Timestamp) {
                    return (v.seconds * 1000L + v.nanoseconds / 1_000_000L).toString()
                }
                if (v is Number) {
                    return v.toLong().toString()
                }
                val s = v.toString().trim()
                if (s.isNotEmpty()) return s
            } catch (_: Exception) {}
        }
        return null
    }

    private fun DocumentSnapshot.safeBool(vararg keys: String, default: Boolean = false): Boolean {
        for (k in keys) {
            try {
                val v = get(k) ?: continue
                when (v) {
                    is Boolean -> return v
                    is Number -> return v.toInt() != 0
                    is String -> {
                        val s = v.trim().lowercase()
                        if (s == "true" || s == "1" || s == "yes" || s == "on") return true
                        if (s == "false" || s == "0" || s == "no" || s == "off") return false
                    }
                }
            } catch (_: Exception) {}
        }
        return default
    }

    private fun DataSnapshot.toTournament(): Tournament? {
        if (!exists()) return null
        return try {
            val rawKey = key ?: ""
            val nonTournamentContainerKeys = setOf(
                "BR", "CS", "LONE_WOLF", "SCRIMS", "br", "cs", "lone_wolf", "scrims",
                "FreeFire", "Free Fire", "FREE FIRE", "BGMI", "bgmi",
                "roomDetails", "participants", "slots", "registrations", "players", "teams", "rules",
                "matches", "tournaments", "categories", "items", "custom_rooms"
            )
            if (rawKey in nonTournamentContainerKeys) return null
            if (child("isCustom").exists() || child("defaultFormats").exists() || child("defaultMaps").exists()) {
                return null
            }

            val tId = child("id").value?.toString() 
                ?: child("tournamentId").value?.toString() 
                ?: child("tournament_id").value?.toString() 
                ?: child("matchId").value?.toString()
                ?: child("match_id").value?.toString()
                ?: child("tId").value?.toString()
                ?: key 
                ?: return null

            val rawTitle = child("title").value?.toString() 
                ?: child("name").value?.toString() 
                ?: child("tournamentName").value?.toString() 
                ?: child("tournament_name").value?.toString() 
                ?: child("matchTitle").value?.toString()
                ?: child("match_title").value?.toString()
                ?: child("matchName").value?.toString()
                ?: child("eventTitle").value?.toString()
                ?: child("event_title").value?.toString()
                ?: child("roomName").value?.toString()
                ?: child("room_name").value?.toString()
                ?: child("heading").value?.toString()

            val entryFee = safeFloat(child("entryFee").value ?: child("entry_fee").value ?: child("fee").value ?: child("entry").value ?: child("ticketPrice").value ?: child("coins").value ?: child("price").value)
            val prizePool = safeFloat(child("prizePool").value ?: child("prize_pool").value ?: child("prize").value ?: child("prizepool").value ?: child("totalPrize").value ?: child("winningPrize").value ?: child("pool").value)

            val roomDetails = if (child("roomDetails").exists()) {
                val rChild = child("roomDetails")
                RoomDetails(
                    roomId = rChild.child("roomId").value?.toString() ?: rChild.child("room_id").value?.toString() ?: "",
                    roomPassword = rChild.child("roomPassword").value?.toString() ?: rChild.child("room_password").value?.toString() ?: rChild.child("password").value?.toString() ?: "",
                    updatedAt = safeLong(rChild.child("updatedAt").value, System.currentTimeMillis())
                )
            } else if (child("roomId").exists() || child("room_id").exists()) {
                RoomDetails(
                    roomId = child("roomId").value?.toString() ?: child("room_id").value?.toString() ?: "",
                    roomPassword = child("roomPassword").value?.toString() ?: child("room_password").value?.toString() ?: child("password").value?.toString() ?: "",
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                null
            }

            if (isPhantomOrDeletedTournament(
                    tId = tId,
                    rawKey = rawKey,
                    rawTitle = rawTitle,
                    entryFee = entryFee,
                    prizePool = prizePool,
                    roomId = roomDetails?.roomId,
                    roomPass = roomDetails?.roomPassword
                )
            ) {
                return null
            }

            val hasTournamentIndicators = !rawTitle.isNullOrBlank() ||
                child("entryFee").exists() || child("entry_fee").exists() ||
                child("prizePool").exists() || child("prize_pool").exists() ||
                child("maxPlayers").exists() || child("max_players").exists() || child("slots").exists() ||
                child("format").exists() || child("matchType").exists() || child("mode").exists() ||
                child("map").exists() || child("mapName").exists() ||
                child("startsAt").exists() || child("startTime").exists() ||
                roomDetails != null || child("participants").exists()
            if (!hasTournamentIndicators) return null

            val rawGame = child("game").value?.toString() 
                ?: child("gameName").value?.toString() 
                ?: child("game_name").value?.toString()
                ?: child("game_title").value?.toString() 
                ?: "Free Fire"
            val game = if (rawGame.equals("BR", true) || rawGame.equals("CS", true) || rawGame.equals("LONE_WOLF", true) || rawGame.equals("SCRIMS", true)) "Free Fire" else rawGame

            var regPlayers = safeInt(child("registeredPlayers").value ?: child("registered_players").value ?: child("slotsFilled").value ?: child("slots_filled").value ?: child("joinedPlayers").value ?: child("joined_players").value ?: child("currentPlayers").value ?: child("current_players").value ?: child("participantsCount").value ?: child("joined").value ?: child("filledSlots").value ?: child("slotsBooked").value)
            if (regPlayers == 0) {
                if (child("participants").exists()) {
                    regPlayers = child("participants").childrenCount.toInt()
                } else if (child("players").exists()) {
                    regPlayers = child("players").childrenCount.toInt()
                } else if (child("teams").exists()) {
                    regPlayers = child("teams").childrenCount.toInt()
                }
            }

            val maxPlayers = safeInt(child("maxPlayers").value ?: child("max_players").value ?: child("slots").value ?: child("totalSlots").value ?: child("total_slots").value ?: child("maxSlots").value ?: child("capacity").value ?: child("slotCount").value, 48)
            val format = child("format").value?.toString() ?: child("matchType").value?.toString() ?: child("match_type").value?.toString() ?: child("type").value?.toString() ?: child("mode").value?.toString() ?: child("gameMode").value?.toString() ?: "SOLO"
            val rawStatus = child("status").value?.toString() ?: child("matchStatus").value?.toString() ?: child("match_status").value?.toString() ?: child("state").value?.toString() ?: child("tournamentStatus").value?.toString() ?: "UPCOMING"
            val status = when (rawStatus.trim().uppercase()) {
                "UPCOMING", "SCHEDULED", "OPEN" -> "UPCOMING"
                "LIVE", "ACTIVE" -> "LIVE"
                "COMPLETED", "ENDED" -> "COMPLETED"
                "CANCELLED" -> "CANCELLED"
                else -> rawStatus.trim().uppercase().ifBlank { "UPCOMING" }
            }
            val mapName = child("map").value?.toString() ?: child("mapName").value?.toString() ?: child("map_name").value?.toString() ?: child("arena").value?.toString() ?: "Bermuda"
            val title = rawTitle?.trim()?.takeIf { it.isNotBlank() } ?: "$game $format • $mapName"

            val rawCategory = child("category").value?.toString()
                ?: child("canonicalCategory").value?.toString()
                ?: child("tournamentCategory").value?.toString()
                ?: child("gameCategory").value?.toString()
                ?: child("category_key").value?.toString()
                ?: ""

            val category = when {
                rawCategory.equals("CS", ignoreCase = true) || rawCategory.contains("CLASH", ignoreCase = true) || format.contains("CS", ignoreCase = true) || title.contains("CS", ignoreCase = true) || title.contains("Clash Squad", ignoreCase = true) -> "CS"
                rawCategory.equals("LONE_WOLF", ignoreCase = true) || rawCategory.contains("LONE", ignoreCase = true) || format.contains("Lone", ignoreCase = true) || title.contains("Lone Wolf", ignoreCase = true) || title.contains("1v1", ignoreCase = true) && !format.contains("CS", ignoreCase = true) || mapName.contains("Iron Cage", ignoreCase = true) -> "LONE_WOLF"
                rawCategory.equals("SCRIMS", ignoreCase = true) || rawCategory.contains("SCRIM", ignoreCase = true) || rawCategory.contains("TRAINING", ignoreCase = true) || title.contains("Scrim", ignoreCase = true) || title.contains("Training", ignoreCase = true) -> "SCRIMS"
                else -> "BR"
            }
            val rawStartsAt = child("startsAt").value?.toString() 
                ?: child("startTime").value?.toString() 
                ?: child("start_time").value?.toString() 
                ?: child("schedule").value?.toString() 
                ?: child("date").value?.toString() 
                ?: child("time").value?.toString() 
                ?: child("matchTime").value?.toString() 
                ?: child("match_time").value?.toString() 
                ?: child("startAt").value?.toString() 
                ?: child("timestamp").value?.toString()
                ?: child("scheduledTime").value?.toString()
            val startsAt = formatScheduleTime(rawStartsAt)

            val endsAt = child("endsAt").value?.toString() ?: child("endTime").value?.toString() ?: child("end_time").value?.toString()

            val description = child("description").value?.toString() ?: child("details").value?.toString() ?: child("info").value?.toString() ?: "Official Velorix Free Fire Tournament"
            val bannerUrl = child("bannerUrl").value?.toString() ?: child("banner_url").value?.toString() ?: child("imageUrl").value?.toString() ?: child("image_url").value?.toString() ?: child("banner").value?.toString() ?: child("image").value?.toString() ?: child("poster").value?.toString() ?: ""
            
            val rawRules = child("rules").value ?: child("rule").value ?: child("instructions").value ?: child("rulesText").value ?: child("rulesList").value
            val rules = when (rawRules) {
                is String -> rawRules
                is List<*> -> rawRules.filterNotNull().joinToString("\n") { it.toString() }
                is Map<*, *> -> rawRules.values.filterNotNull().joinToString("\n") { it.toString() }
                else -> rawRules?.toString() ?: "Standard competitive gameplay rules apply."
            }
            
            val allowedGuns = child("allowedGuns").value?.toString() ?: "All Standard Weapons Allowed"
            val bannedGuns = child("bannedGuns").value?.toString() ?: "M79, M82B, Crossbow"
            val gunAttributesAllowed = child("gunAttributesAllowed").value as? Boolean ?: false
            val limitedAmmo = if (child("limitedAmmo").exists()) child("limitedAmmo").value as? Boolean ?: true else true
            val characterSkillsAllowed = if (child("characterSkillsAllowed").exists()) child("characterSkillsAllowed").value as? Boolean ?: true else true
            val allowedSkills = child("allowedSkills").value?.toString() ?: "All Passive & Active Skills"
            val bannedSkills = child("bannedSkills").value?.toString() ?: "None"
            val emulatorAllowed = child("emulatorAllowed").value as? Boolean ?: false
            val roofCampingAllowed = child("roofCampingAllowed").value as? Boolean ?: false
            val airdropAllowed = if (child("airdropAllowed").exists()) child("airdropAllowed").value as? Boolean ?: true else true
            val vehiclesAllowed = if (child("vehiclesAllowed").exists()) child("vehiclesAllowed").value as? Boolean ?: true else true
            val loadoutAllowed = if (child("loadoutAllowed").exists()) child("loadoutAllowed").value as? Boolean ?: true else true
            val allowedCharacters = child("allowedCharacters").value?.toString() ?: "All Characters Allowed"
            val bannedCharacters = child("bannedCharacters").value?.toString() ?: "None"
            val headshotOnly = child("headshotOnly").value as? Boolean ?: false
            val revivalAllowed = if (child("revivalAllowed").exists()) child("revivalAllowed").value as? Boolean ?: true else true
            val fallDamage = if (child("fallDamage").exists()) child("fallDamage").value as? Boolean ?: true else true
            val safeZoneShrinkSpeed = child("safeZoneShrinkSpeed").value?.toString() ?: "Normal"
            val customMatchSettings = child("customMatchSettings").value?.toString() ?: "HP: 200 | EP: 200 | Jump: 100% | Movement: 100% | Gloo Wall Limit: 3"
            val rulesModifiedAt = safeLong(child("rulesModifiedAt").value ?: child("updatedAt").value, 0L)
            val rulesModifiedBy = child("rulesModifiedBy").value?.toString() ?: ""
            val firstPlacePrize = safeFloat(child("firstPlacePrize").value ?: child("first_place_prize").value ?: child("firstPrize").value ?: child("rank1Prize").value)
            val secondPlacePrize = safeFloat(child("secondPlacePrize").value ?: child("second_place_prize").value ?: child("secondPrize").value ?: child("rank2Prize").value)
            val thirdPlacePrize = safeFloat(child("thirdPlacePrize").value ?: child("third_place_prize").value ?: child("thirdPrize").value ?: child("rank3Prize").value)
            val perKillPrize = safeFloat(child("perKillPrize").value ?: child("per_kill_prize").value ?: child("perKill").value ?: child("killPrize").value)
            val cancellationReason = child("cancellationReason").value?.toString() ?: ""
            val cancelledAt = safeLong(child("cancelledAt").value, 0L)
            val isCompletedVal = (child("isCompleted").value as? Boolean) ?: status.equals("COMPLETED", ignoreCase = true)
            val payoutStatusVal = child("payoutStatus").value?.toString() ?: "UNPAID"
            val completedAtVal = safeLong(child("completedAt").value, 0L)
            val totalPrizeDisbursedVal = safeDouble(child("totalPrizeDisbursed").value)
            val winnerUidVal = child("winnerUid").value?.toString() ?: ""
            val winnerNameVal = child("winnerName").value?.toString() ?: ""
            val winnerPrizeVal = safeDouble(child("winnerPrize").value)

            val registrationsList = mutableListOf<PlayerRegistration>()
            val participantsNode = when {
                child("participants").exists() -> child("participants")
                child("slots").exists() -> child("slots")
                child("registrations").exists() -> child("registrations")
                child("players").exists() -> child("players")
                else -> null
            }
            participantsNode?.children?.forEach { pChild ->
                val pId = pChild.key ?: ""
                val pUserId = pChild.child("userId").value?.toString()
                    ?: pChild.child("userUid").value?.toString()
                    ?: pChild.child("uid").value?.toString()
                    ?: pId
                val pName = pChild.child("playerName").value?.toString()
                    ?: pChild.child("username").value?.toString()
                    ?: pChild.child("name").value?.toString()
                    ?: "Player"
                val ign = pChild.child("inGameName").value?.toString()
                    ?: pChild.child("gameUsername").value?.toString()
                    ?: pChild.child("ign").value?.toString()
                    ?: pName
                val ffId = pChild.child("freeFireId").value?.toString()
                    ?: pChild.child("gameId").value?.toString()
                    ?: pChild.child("gameAccountId").value?.toString()
                    ?: ""
                val regAt = safeLong(pChild.child("registeredAt").value ?: pChild.child("joinedAt").value ?: pChild.child("timestamp").value, System.currentTimeMillis())
                val payStatus = pChild.child("paymentStatus").value?.toString() ?: "confirmed"
                val stat = pChild.child("status").value?.toString() ?: "confirmed"
                val feePaid = safeDouble(pChild.child("entryFeePaid").value ?: pChild.child("entryFee").value, entryFee.toDouble())
                val paidDep = safeDouble(pChild.child("paidFromDeposit").value)
                val paidWin = safeDouble(pChild.child("paidFromWinning").value)
                val paidBon = safeDouble(pChild.child("paidFromBonus").value)
                val pKills = safeInt(pChild.child("kills").value ?: pChild.child("killsInMatch").value)
                val pRank = safeInt(pChild.child("rank").value ?: pChild.child("placementRank").value)
                val pWinning = safeDouble(pChild.child("winningAmount").value ?: pChild.child("prizeWon").value)
                registrationsList.add(
                    PlayerRegistration(
                        id = pId,
                        tournamentId = tId,
                        userId = pUserId,
                        playerId = pUserId,
                        playerName = pName,
                        gameUsername = ign,
                        gameId = ffId,
                        paymentStatus = payStatus,
                        registeredAt = regAt,
                        status = stat,
                        entryFeePaid = feePaid,
                        paidFromDeposit = paidDep,
                        paidFromWinning = paidWin,
                        paidFromBonus = paidBon,
                        kills = pKills,
                        rank = pRank,
                        winningAmount = pWinning
                    )
                )
            }
            if (regPlayers == 0 && registrationsList.isNotEmpty()) {
                regPlayers = registrationsList.size
            }

            Tournament(
                id = tId,
                title = title,
                game = game,
                category = category,
                map = mapName,
                entryFee = entryFee,
                prizePool = prizePool,
                registeredPlayers = regPlayers,
                maxPlayers = maxPlayers,
                format = format,
                status = status,
                startsAt = startsAt,
                endsAt = endsAt,
                description = description,
                bannerUrl = bannerUrl,
                rules = rules,
                allowedGuns = allowedGuns,
                bannedGuns = bannedGuns,
                gunAttributesAllowed = gunAttributesAllowed,
                limitedAmmo = limitedAmmo,
                characterSkillsAllowed = characterSkillsAllowed,
                allowedSkills = allowedSkills,
                bannedSkills = bannedSkills,
                emulatorAllowed = emulatorAllowed,
                roofCampingAllowed = roofCampingAllowed,
                airdropAllowed = airdropAllowed,
                vehiclesAllowed = vehiclesAllowed,
                loadoutAllowed = loadoutAllowed,
                allowedCharacters = allowedCharacters,
                bannedCharacters = bannedCharacters,
                headshotOnly = headshotOnly,
                revivalAllowed = revivalAllowed,
                fallDamage = fallDamage,
                safeZoneShrinkSpeed = safeZoneShrinkSpeed,
                customMatchSettings = customMatchSettings,
                rulesModifiedAt = rulesModifiedAt,
                rulesModifiedBy = rulesModifiedBy,
                firstPlacePrize = firstPlacePrize,
                secondPlacePrize = secondPlacePrize,
                thirdPlacePrize = thirdPlacePrize,
                perKillPrize = perKillPrize,
                cancellationReason = cancellationReason,
                cancelledAt = cancelledAt,
                roomDetails = roomDetails,
                isCompleted = isCompletedVal,
                payoutStatus = payoutStatusVal,
                completedAt = completedAtVal,
                totalPrizeDisbursed = totalPrizeDisbursedVal,
                winnerUid = winnerUidVal,
                winnerName = winnerNameVal,
                winnerPrize = winnerPrizeVal,
                registrations = registrationsList
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing DataSnapshot tournament: ${e.message}")
            null
        }
    }

    private fun DocumentSnapshot.toTournament(): Tournament? {
        if (!exists()) return null
        return try {
            if (contains("isCustom") || contains("defaultFormats") || contains("defaultMaps")) {
                return null
            }
            val rawDocId = id
            val tId = safeString("id", "tournamentId", "tournament_id", "matchId", "match_id") ?: id
            val rawTitle = safeString("title", "name", "tournamentName", "tournament_name", "matchTitle", "match_title", "matchName", "eventTitle", "event_title", "roomName", "room_name", "heading")
            val entryFee = safeFloat(get("entryFee") ?: get("entry_fee") ?: get("fee") ?: get("entry") ?: get("ticketPrice") ?: get("coins") ?: get("price"))
            val prizePool = safeFloat(get("prizePool") ?: get("prize_pool") ?: get("prize") ?: get("prizepool") ?: get("totalPrize") ?: get("pool"))

            val roomMap = get("roomDetails") as? Map<*, *>
            val roomDetails = if (roomMap != null) {
                RoomDetails(
                    roomId = roomMap["roomId"]?.toString() ?: roomMap["room_id"]?.toString() ?: "",
                    roomPassword = roomMap["roomPassword"]?.toString() ?: roomMap["room_password"]?.toString() ?: roomMap["password"]?.toString() ?: "",
                    updatedAt = safeLong(roomMap["updatedAt"], System.currentTimeMillis())
                )
            } else {
                val directRoomId = safeString("roomId", "room_id")
                val directPassword = safeString("roomPassword", "room_password", "password")
                if (directRoomId != null || directPassword != null) {
                    RoomDetails(
                        roomId = directRoomId ?: "",
                        roomPassword = directPassword ?: "",
                        updatedAt = System.currentTimeMillis()
                    )
                } else null
            }

            if (isPhantomOrDeletedTournament(
                    tId = tId,
                    rawKey = rawDocId,
                    rawTitle = rawTitle,
                    entryFee = entryFee,
                    prizePool = prizePool,
                    roomId = roomDetails?.roomId,
                    roomPass = roomDetails?.roomPassword
                )
            ) {
                return null
            }

            val hasDocTournamentIndicators = !rawTitle.isNullOrBlank() ||
                contains("entryFee") || contains("entry_fee") ||
                contains("prizePool") || contains("prize_pool") ||
                contains("maxPlayers") || contains("max_players") || contains("slots") ||
                contains("format") || contains("matchType") || contains("mode") ||
                contains("map") || contains("mapName") ||
                contains("startsAt") || contains("startTime") ||
                roomDetails != null || contains("participants")
            if (!hasDocTournamentIndicators) return null

            val rawGame = safeString("game", "gameName", "game_name", "game_title") ?: "Free Fire"
            val game = if (rawGame.equals("BR", true) || rawGame.equals("CS", true) || rawGame.equals("LONE_WOLF", true) || rawGame.equals("SCRIMS", true)) "Free Fire" else rawGame
            val mapName = safeString("map", "mapName", "map_name", "arena") ?: "Bermuda"
            val docFormat = safeString("format", "matchType", "match_type", "type", "mode", "gameMode") ?: "SOLO"
            val title = rawTitle?.trim()?.takeIf { it.isNotBlank() } ?: "$game $docFormat • $mapName"
            var regPlayers = safeInt(get("registeredPlayers") ?: get("registered_players") ?: get("joinedPlayers") ?: get("joined_players") ?: get("currentPlayers") ?: get("participantsCount") ?: get("joined") ?: get("filledSlots"))
            if (regPlayers == 0) {
                val partList = get("participants") as? List<*> ?: (get("participants") as? Map<*, *>)?.keys?.toList()
                if (!partList.isNullOrEmpty()) {
                    regPlayers = partList.size
                }
            }
            val maxPlayers = safeInt(get("maxPlayers") ?: get("max_players") ?: get("slots") ?: get("totalSlots") ?: get("total_slots") ?: get("maxSlots") ?: get("capacity"), 48)
            val format = safeString("format", "matchType", "match_type", "type", "mode", "gameMode") ?: "SOLO"
            val rawStatus = safeString("status", "matchStatus", "match_status", "state", "tournamentStatus") ?: "UPCOMING"
            val status = when (rawStatus.trim().uppercase()) {
                "UPCOMING", "SCHEDULED", "OPEN" -> "UPCOMING"
                "LIVE", "ACTIVE" -> "LIVE"
                "COMPLETED", "ENDED" -> "COMPLETED"
                "CANCELLED" -> "CANCELLED"
                else -> rawStatus.trim().uppercase().ifBlank { "UPCOMING" }
            }

            val rawCategory = safeString("category", "canonicalCategory", "tournamentCategory", "gameCategory", "category_key") ?: ""

            val category = when {
                rawCategory.equals("CS", ignoreCase = true) || rawCategory.contains("CLASH", ignoreCase = true) || format.contains("CS", ignoreCase = true) || title.contains("CS", ignoreCase = true) || title.contains("Clash Squad", ignoreCase = true) -> "CS"
                rawCategory.equals("LONE_WOLF", ignoreCase = true) || rawCategory.contains("LONE", ignoreCase = true) || format.contains("Lone", ignoreCase = true) || title.contains("Lone Wolf", ignoreCase = true) || title.contains("1v1", ignoreCase = true) && !format.contains("CS", ignoreCase = true) || mapName.contains("Iron Cage", ignoreCase = true) -> "LONE_WOLF"
                rawCategory.equals("SCRIMS", ignoreCase = true) || rawCategory.contains("SCRIM", ignoreCase = true) || rawCategory.contains("TRAINING", ignoreCase = true) || title.contains("Scrim", ignoreCase = true) || title.contains("Training", ignoreCase = true) -> "SCRIMS"
                else -> "BR"
            }
            val rawStartsAt = safeString("startsAt", "startTime", "start_time", "schedule", "date", "time", "matchTime", "scheduledTime", "timestamp")
            val startsAt = formatScheduleTime(rawStartsAt)
            val endsAt = safeString("endsAt", "endTime", "end_time")

            val description = safeString("description", "details", "info") ?: "Official Velorix Free Fire Tournament"
            val bannerUrl = safeString("bannerUrl", "banner_url", "imageUrl", "image_url", "banner", "image", "poster") ?: ""
            
            val rulesObj = get("rules") ?: get("rulesList") ?: get("rule") ?: get("instructions") ?: get("rulesText")
            val rules = when (rulesObj) {
                is String -> rulesObj
                is List<*> -> rulesObj.filterNotNull().joinToString("\n") { it.toString() }
                is Map<*, *> -> rulesObj.values.filterNotNull().joinToString("\n") { it.toString() }
                else -> rulesObj?.toString() ?: "Standard competitive gameplay rules apply."
            }

            val allowedGuns = safeString("allowedGuns") ?: "All Standard Weapons Allowed"
            val bannedGuns = safeString("bannedGuns") ?: "M79, M82B, Crossbow"
            val gunAttributesAllowed = safeBool("gunAttributesAllowed", default = false)
            val limitedAmmo = safeBool("limitedAmmo", default = true)
            val characterSkillsAllowed = safeBool("characterSkillsAllowed", default = true)
            val allowedSkills = safeString("allowedSkills") ?: "All Passive & Active Skills"
            val bannedSkills = safeString("bannedSkills") ?: "None"
            val emulatorAllowed = safeBool("emulatorAllowed", default = false)
            val roofCampingAllowed = safeBool("roofCampingAllowed", default = false)
            val airdropAllowed = safeBool("airdropAllowed", default = true)
            val vehiclesAllowed = safeBool("vehiclesAllowed", default = true)
            val loadoutAllowed = safeBool("loadoutAllowed", default = true)
            val allowedCharacters = safeString("allowedCharacters") ?: "All Characters Allowed"
            val bannedCharacters = safeString("bannedCharacters") ?: "None"
            val headshotOnly = safeBool("headshotOnly", default = false)
            val revivalAllowed = safeBool("revivalAllowed", default = true)
            val fallDamage = safeBool("fallDamage", default = true)
            val safeZoneShrinkSpeed = safeString("safeZoneShrinkSpeed") ?: "Normal"
            val customMatchSettings = safeString("customMatchSettings") ?: "HP: 200 | EP: 200 | Jump: 100% | Movement: 100% | Gloo Wall Limit: 3"
            val rulesModifiedAt = safeLong(get("rulesModifiedAt") ?: get("updatedAt"), 0L)
            val rulesModifiedBy = safeString("rulesModifiedBy") ?: ""
            val firstPlacePrize = safeFloat(get("firstPlacePrize") ?: get("first_place_prize") ?: get("firstPrize") ?: get("rank1Prize"))
            val secondPlacePrize = safeFloat(get("secondPlacePrize") ?: get("second_place_prize") ?: get("secondPrize") ?: get("rank2Prize"))
            val thirdPlacePrize = safeFloat(get("thirdPlacePrize") ?: get("third_place_prize") ?: get("thirdPrize") ?: get("rank3Prize"))
            val perKillPrize = safeFloat(get("perKillPrize") ?: get("per_kill_prize") ?: get("perKill") ?: get("killPrize"))
            val cancellationReason = safeString("cancellationReason") ?: ""
            val cancelledAt = safeLong(get("cancelledAt"), 0L)
            val isCompletedVal = safeBool("isCompleted", default = status.equals("COMPLETED", ignoreCase = true))
            val payoutStatusVal = safeString("payoutStatus") ?: "UNPAID"
            val completedAtVal = safeLong(get("completedAt"), 0L)
            val totalPrizeDisbursedVal = safeDouble(get("totalPrizeDisbursed"))
            val winnerUidVal = safeString("winnerUid") ?: ""
            val winnerNameVal = safeString("winnerName") ?: ""
            val winnerPrizeVal = safeDouble(get("winnerPrize"))

            Tournament(
                id = tId,
                title = title,
                game = game,
                category = category,
                map = mapName,
                entryFee = entryFee,
                prizePool = prizePool,
                registeredPlayers = regPlayers,
                maxPlayers = maxPlayers,
                format = format,
                status = status,
                startsAt = startsAt,
                endsAt = endsAt,
                description = description,
                bannerUrl = bannerUrl,
                rules = rules,
                allowedGuns = allowedGuns,
                bannedGuns = bannedGuns,
                gunAttributesAllowed = gunAttributesAllowed,
                limitedAmmo = limitedAmmo,
                characterSkillsAllowed = characterSkillsAllowed,
                allowedSkills = allowedSkills,
                bannedSkills = bannedSkills,
                emulatorAllowed = emulatorAllowed,
                roofCampingAllowed = roofCampingAllowed,
                airdropAllowed = airdropAllowed,
                vehiclesAllowed = vehiclesAllowed,
                loadoutAllowed = loadoutAllowed,
                allowedCharacters = allowedCharacters,
                bannedCharacters = bannedCharacters,
                headshotOnly = headshotOnly,
                revivalAllowed = revivalAllowed,
                fallDamage = fallDamage,
                safeZoneShrinkSpeed = safeZoneShrinkSpeed,
                customMatchSettings = customMatchSettings,
                rulesModifiedAt = rulesModifiedAt,
                rulesModifiedBy = rulesModifiedBy,
                firstPlacePrize = firstPlacePrize,
                secondPlacePrize = secondPlacePrize,
                thirdPlacePrize = thirdPlacePrize,
                perKillPrize = perKillPrize,
                cancellationReason = cancellationReason,
                cancelledAt = cancelledAt,
                roomDetails = roomDetails,
                isCompleted = isCompletedVal,
                payoutStatus = payoutStatusVal,
                completedAt = completedAtVal,
                totalPrizeDisbursed = totalPrizeDisbursedVal,
                winnerUid = winnerUidVal,
                winnerName = winnerNameVal,
                winnerPrize = winnerPrizeVal
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error converting Firestore document ${id} to Tournament: ${e.message}")
            null
        }
    }

    private fun DataSnapshot.toSupportTicket(): SupportTicket? {
        if (!exists()) return null
        val ticketId = child("id").value?.toString() ?: key ?: return null
        val userEmail = child("userEmail").value?.toString() 
            ?: child("user_email").value?.toString() 
            ?: child("email").value?.toString() 
            ?: ""
        val username = child("userName").value?.toString() 
            ?: child("username").value?.toString() 
            ?: child("user_name").value?.toString() 
            ?: child("name").value?.toString() 
            ?: child("displayName").value?.toString() 
            ?: "Player"
        val gameId = child("freeFireId").value?.toString() 
            ?: child("gameId").value?.toString() 
            ?: child("game_id").value?.toString() 
            ?: ""
        val subject = child("subject").value?.toString() 
            ?: child("tournamentTitle").value?.toString() 
            ?: child("tournament_title").value?.toString() 
            ?: "General Issue"
        val issueCategory = child("category").value?.toString() 
            ?: child("issueCategory").value?.toString() 
            ?: child("issue_category").value?.toString() 
            ?: "Other"
        val description = child("description").value?.toString() 
            ?: child("message").value?.toString() 
            ?: child("text").value?.toString() 
            ?: child("details").value?.toString() 
            ?: ""
        val createdAt = safeLong(child("createdAt").value ?: child("created_at").value, System.currentTimeMillis())
        val updatedAt = safeLong(child("updatedAt").value ?: child("updated_at").value, System.currentTimeMillis())
        val rawStatus = child("status").value?.toString() ?: "PENDING"
        val status = when (rawStatus.trim().uppercase()) {
            "RESOLVED", "CLOSED", "DISSOLVED" -> "RESOLVED"
            "INVESTIGATING" -> "INVESTIGATING"
            "REFUNDED" -> "REFUNDED"
            else -> "PENDING"
        }
        val adminNote = child("adminNote").value?.toString() ?: child("admin_note").value?.toString() ?: ""
        val assignedTo = child("assignedTo").value?.toString() ?: child("assigned_to").value?.toString() ?: ""
        val priority = child("priority").value?.toString() 
            ?: if (child("isHighPriority").value == true) "High" else "Medium"

        return SupportTicket(
            id = ticketId,
            userEmail = userEmail,
            username = username,
            gameId = gameId,
            tournamentTitle = subject,
            issueCategory = issueCategory,
            description = description,
            status = status,
            adminNote = adminNote,
            createdAt = createdAt,
            updatedAt = updatedAt,
            assignedTo = assignedTo,
            isHighPriority = priority.equals("High", true) || priority.equals("Urgent", true)
        )
    }

    private fun DataSnapshot.toPayoutRequest(): PayoutRequest? {
        if (!exists()) return null
        val reqId = child("id").value?.toString() ?: child("requestId").value?.toString() ?: key ?: return null
        val uid = child("userId").value?.toString() ?: child("uid").value?.toString() ?: ""
        val username = child("userName").value?.toString() ?: child("username").value?.toString() ?: child("name").value?.toString() ?: "Player"
        val email = child("email").value?.toString() ?: child("userEmail").value?.toString() ?: ""
        val isDeposit = hasChild("utrNumber") || hasChild("paymentRef") || child("paymentMethod").value?.toString()?.contains("UTR", true) == true
        val paymentMethod = child("payoutMethod").value?.toString() 
            ?: child("paymentMethod").value?.toString() 
            ?: child("method").value?.toString() 
            ?: (if (isDeposit) "UPI_MANUAL_UTR" else "UPI")
        val paymentId = child("accountDetails").value?.toString() 
            ?: child("paymentId").value?.toString() 
            ?: child("upiId").value?.toString() 
            ?: child("upi_id").value?.toString() 
            ?: child("utrNumber").value?.toString() 
            ?: child("paymentRef").value?.toString() 
            ?: ""
        val vtAmount = safeDouble(child("amount").value ?: child("vtAmount").value ?: child("vt_amount").value ?: child("balance").value)
        val status = child("status").value?.toString() ?: "pending"
        val rejectionReason = child("rejectionReason").value?.toString() ?: child("rejection_reason").value?.toString() ?: ""
        val requestedAt = safeLong(child("createdAt").value ?: child("requestedAt").value ?: child("timestamp").value, System.currentTimeMillis())
        val processedAt = safeLong(child("processedAt").value ?: child("processed_at").value, 0L)
        val processedBy = child("processedBy").value?.toString() ?: child("processed_by").value?.toString() ?: ""

        return PayoutRequest(
            id = reqId,
            uid = uid,
            username = username,
            email = email,
            paymentMethod = paymentMethod,
            paymentId = paymentId,
            vtAmount = vtAmount,
            status = status,
            rejectionReason = rejectionReason,
            requestedAt = requestedAt,
            processedAt = processedAt,
            processedBy = processedBy
        )
    }

    private fun DataSnapshot.toAdminRecord(): AdminRecord? {
        if (!exists()) return null
        val uid = child("uid").value?.toString() ?: key ?: return null
        val name = child("name").value?.toString() ?: child("displayName").value?.toString() ?: "Admin"
        val email = child("email").value?.toString() ?: ""
        val role = child("role").value?.toString() ?: "tournament_admin"
        val active = child("active").value as? Boolean ?: true
        val assignedAt = safeLong(child("grantedAt").value ?: child("assignedAt").value, System.currentTimeMillis())
        val grantedBy = child("grantedBy").value?.toString() ?: "system"

        return AdminRecord(
            uid = uid,
            name = name,
            email = email,
            role = role,
            active = active,
            assignedAt = assignedAt,
            grantedBy = grantedBy
        )
    }

    private fun DocumentSnapshot.toSupportTicket(): SupportTicket? {
        if (!exists()) return null
        val ticketId = getString("id") ?: id
        val userEmail = getString("userEmail") ?: getString("user_email") ?: getString("email") ?: ""
        val username = getString("userName") ?: getString("username") ?: getString("name") ?: "Player"
        val gameId = getString("freeFireId") ?: getString("gameId") ?: getString("game_id") ?: ""
        val subject = getString("subject") ?: getString("tournamentTitle") ?: "General Issue"
        val issueCategory = getString("category") ?: getString("issueCategory") ?: "Other"
        val description = getString("description") ?: getString("message") ?: ""
        val createdAt = safeLong(get("createdAt") ?: get("created_at"), System.currentTimeMillis())
        val updatedAt = safeLong(get("updatedAt") ?: get("updated_at"), System.currentTimeMillis())
        val rawStatus = getString("status") ?: "PENDING"
        val status = when (rawStatus.trim().uppercase()) {
            "RESOLVED", "CLOSED", "DISSOLVED" -> "RESOLVED"
            "INVESTIGATING" -> "INVESTIGATING"
            "REFUNDED" -> "REFUNDED"
            else -> "PENDING"
        }
        val adminNote = getString("adminNote") ?: getString("admin_note") ?: ""
        val assignedTo = getString("assignedTo") ?: getString("assigned_to") ?: ""
        val priority = getString("priority") ?: ""

        return SupportTicket(
            id = ticketId,
            userEmail = userEmail,
            username = username,
            gameId = gameId,
            tournamentTitle = subject,
            issueCategory = issueCategory,
            description = description,
            status = status,
            adminNote = adminNote,
            createdAt = createdAt,
            updatedAt = updatedAt,
            assignedTo = assignedTo,
            isHighPriority = priority.equals("High", true) || priority.equals("Urgent", true)
        )
    }

    private fun DocumentSnapshot.toPayoutRequest(): PayoutRequest? {
        if (!exists()) return null
        val reqId = getString("id") ?: getString("requestId") ?: id
        val uid = getString("userId") ?: getString("uid") ?: ""
        val username = getString("userName") ?: getString("username") ?: getString("name") ?: "Player"
        val email = getString("email") ?: getString("userEmail") ?: ""
        val isDeposit = contains("utrNumber") || contains("paymentRef") || getString("paymentMethod")?.contains("UTR", true) == true
        val paymentMethod = getString("payoutMethod") 
            ?: getString("paymentMethod") 
            ?: getString("method") 
            ?: (if (isDeposit) "UPI_MANUAL_UTR" else "UPI")
        val paymentId = getString("accountDetails") 
            ?: getString("paymentId") 
            ?: getString("upiId") 
            ?: getString("upi_id") 
            ?: getString("utrNumber") 
            ?: getString("paymentRef") 
            ?: ""
        val vtAmount = safeDouble(get("amount") ?: get("vtAmount") ?: get("balance"))
        val status = getString("status") ?: "pending"
        val rejectionReason = getString("rejectionReason") ?: getString("rejection_reason") ?: ""
        val requestedAt = safeLong(get("createdAt") ?: get("requestedAt") ?: get("timestamp"), System.currentTimeMillis())
        val processedAt = safeLong(get("processedAt") ?: get("processed_at"), 0L)
        val processedBy = getString("processedBy") ?: getString("processed_by") ?: ""

        return PayoutRequest(
            id = reqId,
            uid = uid,
            username = username,
            email = email,
            paymentMethod = paymentMethod,
            paymentId = paymentId,
            vtAmount = vtAmount,
            status = status,
            rejectionReason = rejectionReason,
            requestedAt = requestedAt,
            processedAt = processedAt,
            processedBy = processedBy
        )
    }

    private fun DocumentSnapshot.toUserProfile(): UserProfile? {
        if (!exists()) return null

        val dataMap = data ?: emptyMap<String, Any?>()

        fun findString(vararg keys: String): String? {
            // 1. Direct exact key
            for (k in keys) {
                val direct = getString(k) ?: get(k)?.toString()
                if (!direct.isNullOrBlank()) return direct
            }
            // 2. Direct case-insensitive key in root map
            for (k in keys) {
                val entry = dataMap.entries.find { it.key.equals(k, ignoreCase = true) }
                val v = entry?.value?.toString()
                if (!v.isNullOrBlank()) return v
            }
            // 3. Nested containers (exact & case-insensitive)
            for (containerName in listOf("profile", "account", "data", "info", "user", "userData", "wallet", "basic_info", "details", "player")) {
                val map = (get(containerName) as? Map<*, *>)
                    ?: (dataMap.entries.find { it.key.equals(containerName, ignoreCase = true) }?.value as? Map<*, *>)
                if (map != null) {
                    for (k in keys) {
                        val nested = map[k]?.toString()
                        if (!nested.isNullOrBlank()) return nested
                        val nestedInsensitive = map.entries.find { it.key?.toString()?.equals(k, ignoreCase = true) == true }?.value?.toString()
                        if (!nestedInsensitive.isNullOrBlank()) return nestedInsensitive
                    }
                }
            }
            return null
        }

        fun findDouble(vararg keys: String): Double {
            for (k in keys) {
                val v = get(k)
                if (v != null) {
                    val d = safeDouble(v)
                    if (d != 0.0) return d
                }
            }
            for (k in keys) {
                val entry = dataMap.entries.find { it.key.equals(k, ignoreCase = true) }
                val v = entry?.value
                if (v != null) {
                    val d = safeDouble(v)
                    if (d != 0.0) return d
                }
            }
            for (containerName in listOf("profile", "account", "data", "info", "wallet", "balances", "funds", "money")) {
                val map = (get(containerName) as? Map<*, *>)
                    ?: (dataMap.entries.find { it.key.equals(containerName, ignoreCase = true) }?.value as? Map<*, *>)
                if (map != null) {
                    for (k in keys) {
                        val nested = map[k]
                        if (nested != null) {
                            val d = safeDouble(nested)
                            if (d != 0.0) return d
                        }
                        val nestedInsensitive = map.entries.find { it.key?.toString()?.equals(k, ignoreCase = true) == true }?.value
                        if (nestedInsensitive != null) {
                            val d = safeDouble(nestedInsensitive)
                            if (d != 0.0) return d
                        }
                    }
                }
            }
            return 0.0
        }

        val rawDocId = id
        val foundId = findString("uid", "id", "userId", "user_id", "playerUid", "accountId", "account_id")
        val uId = when {
            rawDocId.isNotBlank() && !rawDocId.startsWith("acc_") && !rawDocId.startsWith("admin_anant_master") -> rawDocId
            !foundId.isNullOrBlank() && !foundId.startsWith("acc_") && !foundId.startsWith("admin_anant_master") -> foundId
            rawDocId.isNotBlank() -> rawDocId
            else -> foundId ?: id
        }

        val directIgn = findString("ign", "IGN", "inGameName", "InGameName", "in_game_name", "playerName", "PlayerName", "player_name", "ff_ign", "ffIgn", "FF_IGN", "ff_name", "ffName", "gameUsername", "nick", "nickname", "freeFireName", "free_fire_name") ?: ""
        val rawEmail = findString("email", "Email", "user_email", "userEmail", "UserEmail", "mail", "Mail", "emailAddress", "EmailAddress", "contactEmail", "userMail", "email_id", "emailId", "accountEmail", "loginEmail", "gmail") ?: ""
        val phoneOrEmailVal = findString("phoneOrEmail", "phone_or_email", "PhoneOrEmail") ?: ""
        var email = if (rawEmail.isNotBlank()) rawEmail else if (phoneOrEmailVal.contains("@")) phoneOrEmailVal else ""
        if (email.isBlank()) {
            for ((_, v) in dataMap) {
                val str = v?.toString()?.trim() ?: ""
                if (str.contains("@") && str.contains(".") && !str.contains(" ") && str.length >= 6) {
                    email = str
                    break
                }
            }
        }

        val directPhone = findString("phone", "Phone", "phoneNumber", "PhoneNumber", "phone_number", "mobile", "Mobile", "contact", "contactNumber", "tel", "phone_no", "mobileNumber")
            ?: (if (!phoneOrEmailVal.contains("@") && phoneOrEmailVal.isNotBlank()) phoneOrEmailVal else "")
        var phone = directPhone
        if (phone.isBlank()) {
            for ((_, v) in dataMap) {
                val str = v?.toString()?.trim() ?: ""
                if ((str.startsWith("+91") && str.length >= 12) || (str.length in 10..12 && str.all { it.isDigit() || it == '+' })) {
                    phone = str
                    break
                }
            }
        }

        val directUName = findString(
            "username", "Username", "displayName", "DisplayName", "name", "Name", "user_name", "ign", "IGN",
            "inGameName", "in_game_name", "playerName", "player_name", "nick", "nickname", "fullName", "FullName", "gamerTag"
        )
        val uName = when {
            !directUName.isNullOrBlank() && directUName != "Player" && !directUName.startsWith("Player (") -> directUName
            directIgn.isNotBlank() && directIgn != "Player" -> directIgn
            email.isNotBlank() && email.contains("@") -> email.substringBefore("@")
            phone.isNotBlank() -> "Player (${phone.takeLast(4)})"
            !directUName.isNullOrBlank() && directUName != "Player" -> directUName
            rawDocId.isNotBlank() && !rawDocId.startsWith("acc_") && !rawDocId.startsWith("usr_") && !rawDocId.startsWith("admin_") -> "Player (${rawDocId.take(6)})"
            else -> "Player"
        }

        var avatarUrl = findString(
            "avatarUrl", "avatar_url", "photoUrl", "photo_url", "photoURL",
            "profilePic", "profile_pic", "profilePicture", "profile_picture",
            "profileImage", "profile_image", "avatar", "image", "userAvatar", "user_avatar",
            "picture", "profilePicUrl", "profile_pic_url", "icon", "imageUrl", "image_url", "userPhoto"
        )
        if (avatarUrl.isNullOrBlank()) {
            for ((_, v) in dataMap) {
                val str = v?.toString()?.trim() ?: ""
                if ((str.startsWith("http://") || str.startsWith("https://")) && 
                    (str.contains("googleusercontent") || str.contains("firebasestorage") || str.contains("avatar") || str.contains("profile") || str.contains("photo") || str.contains("image"))
                ) {
                    avatarUrl = str
                    break
                }
            }
        }
        val role = findString("role", "user_role", "type", "userType") ?: "player"

        var depositFunds = findDouble("depositBalance", "depositFunds", "deposit_funds", "deposit_balance", "deposits", "depositedMoney", "deposited_money")
        var winningFunds = findDouble("winningsBalance", "winningFunds", "winning_funds", "winnings", "winningBalance", "winning_balance", "winningMoney", "winning_money", "withdrawableBalance", "withdrawable_balance")
        val bonusFunds = findDouble("bonusBalance", "bonusFunds", "bonus_funds", "referralBonus", "referral_bonus", "referralFunds", "referral_funds", "bonus", "promoCoins", "bonusMoney", "bonus_money")
        val lockedBalance = findDouble("lockedBalance", "locked_balance")
        val tokensCount = safeInt(get("tokens") ?: get("tokenBalance") ?: get("vtTokens"), 0)

        val hasExplicitBuckets = get("depositBalance") != null || get("depositFunds") != null || get("deposit_funds") != null ||
            get("winningsBalance") != null || get("winningFunds") != null || get("winning_funds") != null ||
            get("bonusBalance") != null || get("bonusFunds") != null || get("bonus_funds") != null ||
            get("referralBonus") != null || get("walletAndFinanceSummary") != null

        var funds = findDouble("funds", "balance", "wallet", "walletBalance", "wallet_balance", "coins", "vtCoins", "amount", "money", "credits")
        if ((depositFunds + winningFunds + bonusFunds) > 0.0 || hasExplicitBuckets) {
            funds = depositFunds + winningFunds + bonusFunds
        } else if (funds > 0.0) {
            depositFunds = funds
        }

        fun parseDocBool(keyName: String): Boolean? {
            val raw = get(keyName) ?: dataMap.entries.find { it.key.equals(keyName, ignoreCase = true) }?.value
            return when (raw) {
                is Boolean -> raw
                is String -> when {
                    raw.trim().equals("true", ignoreCase = true) -> true
                    raw.trim().equals("false", ignoreCase = true) -> false
                    else -> null
                }
                is Number -> raw.toInt() != 0
                else -> null
            }
        }
        val profileMap = get("profile") as? Map<*, *>
        val explicitBanBool = parseDocBool("isBanned")
            ?: parseDocBool("banned")
            ?: (profileMap?.get("isBanned") as? Boolean)
            ?: (profileMap?.get("banned") as? Boolean)
        val rawStatusStr = (findString("accountStatus", "status", "userStatus") ?: "").trim().uppercase()
        val banReason = findString("banReason", "ban_reason", "reason") ?: ""
        val banCaseId = findString("banCaseId", "ban_case_id", "sanctionId", "sanction_id", "caseId") ?: ""
        val bannedAt = safeLong(get("bannedAt") ?: get("banned_at"), 0L)
        val activeAppealTicketId = findString("activeAppealTicketId", "activeAppealTicket", "appealTicketId", "ticketId") ?: ""
        val activeAppealStatus = findString("activeAppealStatus", "appealStatus") ?: ""
        val isBanned = when {
            explicitBanBool == false && (rawStatusStr == "ACTIVE" || rawStatusStr == "UNBANNED" || rawStatusStr.isEmpty()) -> false
            explicitBanBool == true -> true
            rawStatusStr == "BANNED" || rawStatusStr == "RESTRICTED" || rawStatusStr == "PERMANENT_BAN" -> true
            explicitBanBool == null && banCaseId.startsWith("BAN-", ignoreCase = true) && rawStatusStr != "ACTIVE" && rawStatusStr != "UNBANNED" -> true
            else -> false
        }

        val isSuspended = getBoolean("isSuspended")
            ?: getBoolean("suspended")
            ?: (findString("status")?.equals("suspended", ignoreCase = true) == true)
            ?: (findString("accountStatus")?.equals("SUSPENDED", ignoreCase = true) == true)
            ?: false

        val suspendedUntil = safeLong(get("suspensionEndTimeMillis") ?: get("suspendedUntil") ?: get("suspended_until") ?: get("suspensionExpiresAt"), 0L)
        val suspensionReason = findString("suspensionReason", "suspension_reason") ?: ""

        val isVpnBlocked = getBoolean("isVpnBlocked") ?: getBoolean("vpnBlocked") ?: false
        val isForceUpdateRequired = getBoolean("isForceUpdateRequired") ?: getBoolean("forceUpdate") ?: false
        val minVersionRequired = findString("minVersionRequired", "minVersion", "min_version") ?: ""
        val isMaintenanceBypass = getBoolean("isMaintenanceBypass") ?: getBoolean("maintenanceBypass") ?: false

        val deviceModel = findString("deviceModel", "device_model", "model", "device", "deviceName") ?: ""
        val ipAddress = findString("ipAddress", "ip_address", "ip", "lastIp") ?: ""

        val lastActive = safeLong(get("lastActive") ?: get("lastLoginAt") ?: get("updatedAt") ?: get("createdAt"), System.currentTimeMillis())
        val createdAt = safeLong(get("createdAt") ?: get("created_at") ?: get("registeredAt") ?: get("joinedAt"), 0L)
        val gameId = findString("gameId", "game_id", "gameAccountId", "freeFireId", "free_fire_id", "ffId", "ff_id", "playerUid", "inGameId", "in_game_id") ?: ""
        
        val wins = safeInt(get("wins") ?: get("totalWins") ?: get("total_wins") ?: get("matchesWon"))
        val kills = safeInt(get("kills") ?: get("totalKills") ?: get("total_kills"))
        val activityPoints = safeInt(get("activityPoints") ?: get("points") ?: get("activity_points"))
        val totalEarnings = safeDouble(get("totalEarnings") ?: get("earnings") ?: get("won"))
        val referralCode = findString("referralCode", "referral_code", "refCode") ?: ""
        val referredBy = findString("referredBy", "referred_by", "invitedBy") ?: ""
        val referralCount = safeInt(get("referralCount") ?: get("referral_count") ?: get("referrals"))
        val referralBonusEarned = safeDouble(get("referralEarnings") ?: get("referralBonusEarned") ?: get("referral_bonus") ?: get("referral_earnings"))
        val bio = findString("bio", "about", "statusMessage") ?: ""
        val socialLink = findString("socialLink", "social_link", "instagram", "youtube") ?: ""
        val kycVerified = getBoolean("kycVerified") ?: getBoolean("isKycVerified") ?: false
        val dataExported = getBoolean("dataExported") ?: false
        val lastExportedAt = safeLong(get("lastExportedAt"), 0L)
        val lastExportId = findString("lastExportId") ?: ""
        val deletionRequested = getBoolean("deletionRequested") ?: false
        val deletionRequestId = findString("deletionRequestId") ?: ""
        val deletionReason = findString("deletionReason") ?: ""
        val deletionDetails = findString("deletionDetails") ?: ""
        val deletionRequestedAt = safeLong(get("deletionRequestedAt"), 0L)
        val accountStatus = findString("accountStatus", "status")
            ?: (if (isBanned) "BANNED" else if (isSuspended) "SUSPENDED" else if (deletionRequested) "PENDING_DELETION" else "ACTIVE")
        val versionNum = safeInt(get("version"), 1).coerceAtLeast(1)
        val updatedAtTs = safeLong(get("updatedAt"), lastActive)

        val dateOfBirth = findString("dateOfBirth", "dob", "birthDate", "birth_date", "date_of_birth") ?: ""
        var age = safeInt(get("age") ?: get("userAge") ?: get("user_age"))
        if (age <= 0 && dateOfBirth.isNotBlank()) {
            age = com.example.data.validation.TournamentBackendValidator.calculateAgeFromDob(dateOfBirth)
        }
        val isUnder18 = getBoolean("isUnder18")
            ?: getBoolean("under18")
            ?: (age in 1..17)
        val eligibleForCashTournaments = getBoolean("eligibleForCashTournaments")
            ?: (!isUnder18 && (age >= 18 || age <= 0))
        val ageConfirmed = getBoolean("ageConfirmed")
            ?: (dateOfBirth.isNotBlank())

        val rawMap = mutableMapOf<String, String>()
        fun flattenMap(prefix: String, map: Map<*, *>) {
            map.forEach { (k, v) ->
                val fullKey = if (prefix.isEmpty()) k.toString() else "$prefix.$k"
                if (v is Map<*, *>) {
                    flattenMap(fullKey, v)
                } else if (v != null) {
                    rawMap[fullKey] = v.toString()
                }
            }
        }
        data?.let { flattenMap("", it) }

        val resolvedUsername = when {
            uName.isNotBlank() && uName != "Player" && !uName.startsWith("Player (") -> uName
            directIgn.isNotBlank() && directIgn != "Player" -> directIgn
            email.isNotBlank() && email.contains("@") -> email.substringBefore("@")
            directIgn.isNotBlank() -> directIgn
            uName.isNotBlank() && uName != "Player" -> uName
            rawDocId.isNotBlank() && !rawDocId.startsWith("usr_") && !rawDocId.startsWith("acc_") && !rawDocId.startsWith("admin_") -> "Player (${rawDocId.take(6)})"
            else -> "Player"
        }

        val isGuest = getBoolean("isGuest")
            ?: getBoolean("is_guest")
            ?: (role.equals("guest", ignoreCase = true))
            ?: (rawMap["isGuest"]?.toBooleanStrictOrNull())
            ?: (rawMap["provider"]?.equals("anonymous", ignoreCase = true) == true)
            ?: (rawMap["providerId"]?.equals("anonymous", ignoreCase = true) == true)
            ?: false

        val isAnonymous = getBoolean("isAnonymous")
            ?: getBoolean("is_anonymous")
            ?: (role.equals("anonymous", ignoreCase = true))
            ?: (rawMap["isAnonymous"]?.toBooleanStrictOrNull())
            ?: false

        val firstAppJoinAt = safeLong(get("firstAppJoinAt") ?: get("appJoinedAt") ?: get("createdAt"), createdAt)
        val firstMatchJoinedAt = safeLong(get("firstMatchJoinedAt") ?: get("first_match_joined_at"), 0L)
        val lastMatchJoinedAt = safeLong(get("lastMatchJoinedAt") ?: get("last_match_joined_at"), 0L)
        val totalMatchesJoined = safeInt(get("totalMatchesJoined") ?: get("matchesJoined"))
        val totalDeposited = maxOf(safeDouble(get("totalDeposited")), depositFunds)
        val totalWithdrawn = safeDouble(get("totalWithdrawn"))
        val totalEntryFeesPaid = safeDouble(get("totalEntryFeesPaid"))
        val supportCasesCount = safeInt(get("supportCasesCount") ?: get("casesFiled"))

        return UserProfile(
            id = uId,
            username = resolvedUsername,
            avatarUrl = avatarUrl,
            role = role,
            email = email,
            phone = phone,
            phoneOrEmail = phoneOrEmailVal.ifBlank { email.ifBlank { phone } },
            bio = bio,
            socialLink = socialLink,
            depositFunds = depositFunds,
            winningFunds = winningFunds,
            bonusFunds = bonusFunds,
            lockedBalance = lockedBalance,
            funds = funds,
            balance = funds,
            tokens = tokensCount,
            wins = wins,
            kills = kills,
            activityPoints = activityPoints,
            totalEarnings = totalEarnings,
            kycVerified = kycVerified,
            dataExported = dataExported,
            lastExportedAt = lastExportedAt,
            lastExportId = lastExportId,
            isBanned = isBanned,
            banReason = banReason,
            banCaseId = banCaseId,
            bannedAt = bannedAt,
            activeAppealTicketId = activeAppealTicketId,
            activeAppealStatus = activeAppealStatus,
            isSuspended = isSuspended,
            suspendedUntil = suspendedUntil,
            suspensionReason = suspensionReason,
            accountStatus = accountStatus,
            deletionRequested = deletionRequested,
            deletionRequestId = deletionRequestId,
            deletionReason = deletionReason,
            deletionDetails = deletionDetails,
            deletionRequestedAt = deletionRequestedAt,
            version = versionNum,
            updatedAt = updatedAtTs,
            isVpnBlocked = isVpnBlocked,
            isForceUpdateRequired = isForceUpdateRequired,
            minVersionRequired = minVersionRequired,
            isMaintenanceBypass = isMaintenanceBypass,
            deviceModel = deviceModel,
            ipAddress = ipAddress,
            dateOfBirth = dateOfBirth,
            age = age,
            isUnder18 = isUnder18,
            eligibleForCashTournaments = eligibleForCashTournaments,
            ageConfirmed = ageConfirmed,
            lastActive = lastActive,
            lastActiveAt = safeLong(get("lastActiveAt"), lastActive),
            isOnline = safeBool("isOnline"),
            fcmToken = (getString("fcmToken") ?: getString("messagingToken") ?: getString("pushToken")).orEmpty().trim(),
            createdAt = if (createdAt > 0L) createdAt else firstAppJoinAt,
            firstAppJoinAt = firstAppJoinAt,
            firstMatchJoinedAt = firstMatchJoinedAt,
            lastMatchJoinedAt = lastMatchJoinedAt,
            totalMatchesJoined = totalMatchesJoined,
            totalDeposited = totalDeposited,
            totalWithdrawn = totalWithdrawn,
            totalEntryFeesPaid = totalEntryFeesPaid,
            supportCasesCount = supportCasesCount,
            gameId = gameId,
            ign = directIgn.ifBlank { if (resolvedUsername != "Player") resolvedUsername else "" },
            referralCode = referralCode,
            referredBy = referredBy,
            referralCount = referralCount,
            referralBonusEarned = referralBonusEarned,
            isGuest = isGuest,
            isAnonymous = isAnonymous,
            rawAttributes = rawMap
        )
    }

    fun loadUserData(onComplete: ((List<UserProfile>) -> Unit)? = null) {
        val databaseUrl = DB_URL
        val rtdb = try {
            FirebaseDatabase.getInstance(databaseUrl)
        } catch (_: Exception) {
            FirebaseDatabase.getInstance()
        }
        val usersRef = rtdb.getReference("users")

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val rtdbList = mutableListOf<UserProfile>()
                for (userSnapshot in snapshot.children) {
                    val profile = userSnapshot.toUserProfile()
                    if (profile != null && !locallyDeletedUserIds.contains(profile.id) && !profile.isGuestSession) {
                        rtdbList.add(profile)
                    }
                }

                // Reconcile with Firestore users so RTDB & Firestore stay in 100% lockstep
                firestore.collection("users").get().addOnCompleteListener { fsTask ->
                    val combined = mutableListOf<UserProfile>()
                    combined.addAll(rtdbList)
                    if (fsTask.isSuccessful && fsTask.result != null) {
                        for (doc in fsTask.result.documents) {
                            val p = doc.toUserProfile()
                            if (p != null && !locallyDeletedUserIds.contains(p.id) && !p.isGuestSession) {
                                combined.add(p)
                            }
                        }
                    }

                    val finalMerged = mutableListOf<UserProfile>()
                    for (u in combined) {
                        val cid = u.id.trim()
                        val cmail = u.email.trim().lowercase()
                        val idx = finalMerged.indexOfFirst { ex ->
                            (cid.isNotBlank() && ex.id.isNotBlank() && ex.id == cid) ||
                            (cmail.isNotBlank() && cmail.contains("@") && ex.email.isNotBlank() && ex.email.trim().lowercase() == cmail)
                        }
                        if (idx == -1) {
                            finalMerged.add(u)
                        } else {
                            val ex = finalMerged[idx]
                            val prefName = when {
                                ex.username.isNotBlank() && ex.username != "Player" && !ex.username.startsWith("Player (") -> ex.username
                                u.username.isNotBlank() && u.username != "Player" && !u.username.startsWith("Player (") -> u.username
                                ex.ign.isNotBlank() && ex.ign != "Player" -> ex.ign
                                u.ign.isNotBlank() && u.ign != "Player" -> u.ign
                                else -> ex.username.ifBlank { u.username }
                            }
                            val prefMail = if (ex.email.isNotBlank() && ex.email.contains("@")) ex.email else u.email
                            val prefIgn = if (ex.ign.isNotBlank()) ex.ign else u.ign
                            val prefGameId = if (ex.gameId.isNotBlank()) ex.gameId else u.gameId
                            val prefFunds = maxOf(ex.funds, u.funds)
                            finalMerged[idx] = ex.copy(
                                username = prefName,
                                email = prefMail,
                                ign = prefIgn,
                                gameId = prefGameId,
                                funds = prefFunds,
                                isBanned = when {
                                    u.updatedAt > ex.updatedAt && u.updatedAt > 0L -> u.isBanned
                                    ex.updatedAt > u.updatedAt && ex.updatedAt > 0L -> ex.isBanned
                                    else -> ex.isBanned || u.isBanned
                                }
                            )
                        }
                    }
                    onComplete?.invoke(finalMerged.filter { !it.isGuestSession })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("AdminPanel", "Database read blocked: ${error.message}")
                onComplete?.invoke(emptyList())
            }
        })
    }

    suspend fun getLiveUsersStream(): Flow<List<UserProfile>> = callbackFlow {
        val usersSources = mutableMapOf<String, Map<String, UserProfile>>()
        if (lastExtractedUsers.isNotEmpty()) {
            usersSources["extracted_cache"] = lastExtractedUsers.toMap()
        }
        val bannedUids = mutableSetOf<String>()
        val bannedEmails = mutableSetOf<String>()
        val bannedGameIds = mutableSetOf<String>()

        try {
            ensureAuthenticatedSession(activeAdminEmail)
        } catch (_: Exception) {}

        fun extractDigitsFast(input: String): String {
            if (input.isEmpty()) return ""
            val sb = java.lang.StringBuilder(input.length)
            for (i in 0 until input.length) {
                val c = input[i]
                if (c in '0'..'9') sb.append(c)
            }
            return sb.toString()
        }

        fun emitCombinedUsers() {
            val allRawUsers = mutableListOf<UserProfile>()

            // 1. Load locally bound real accounts only if they have genuine Firebase IDs
            if (context != null) {
                try {
                    val bindingManager = com.example.data.auth.DeviceAccountBindingManager(context)
                    val recorded = bindingManager.getAllRecordedAccounts()
                    recorded.forEach { acc ->
                        val uId = acc.uid.ifBlank {
                            if (acc.email.equals(auth.currentUser?.email, ignoreCase = true)) auth.currentUser?.uid ?: "" else ""
                        }
                        if (uId.isNotBlank() && !uId.startsWith("acc_") && !uId.startsWith("admin_")) {
                            allRawUsers.add(
                                UserProfile(
                                    id = uId,
                                    username = acc.username,
                                    email = acc.email,
                                    role = acc.role,
                                    funds = 0.0,
                                    gameId = "",
                                    lastActive = acc.lastLoginTimestamp
                                )
                            )
                        }
                    }
                } catch (_: Exception) {}
            }

            // 2. Realtime streams and database sources
            usersSources.values.forEach { sourceMap ->
                sourceMap.values.forEach { user ->
                    if (user.id.isNotBlank() && !locallyDeletedUserIds.contains(user.id)) {
                        allRawUsers.add(user)
                    }
                }
            }

            // Group and deduplicate users by unique identity using O(1) index maps
            val mergedList = mutableListOf<UserProfile>()
            val idMap = HashMap<String, Int>(allRawUsers.size)
            val emailMap = HashMap<String, Int>(allRawUsers.size)
            val phoneMap = HashMap<String, Int>(allRawUsers.size)
            val gameIdMap = HashMap<String, Int>(allRawUsers.size)

            for (user in allRawUsers) {
                val cleanId = user.id.trim()
                val cleanEmail = user.email.trim().lowercase()
                val cleanPhone = extractDigitsFast(user.phone)
                val cleanGameId = extractDigitsFast(user.gameId)

                val existingIndex = (if (cleanId.isNotBlank()) idMap[cleanId] else null)
                    ?: (if (cleanEmail.contains("@")) emailMap[cleanEmail] else null)
                    ?: (if (cleanPhone.length >= 10) phoneMap[cleanPhone.takeLast(10)] else null)
                    ?: (if (cleanGameId.length >= 5) gameIdMap[cleanGameId] else null)
                    ?: -1

                if (existingIndex == -1) {
                    val newIndex = mergedList.size
                    mergedList.add(user)
                    if (cleanId.isNotBlank()) idMap[cleanId] = newIndex
                    if (cleanEmail.contains("@")) emailMap[cleanEmail] = newIndex
                    if (cleanPhone.length >= 10) phoneMap[cleanPhone.takeLast(10)] = newIndex
                    if (cleanGameId.length >= 5) gameIdMap[cleanGameId] = newIndex
                } else {
                    val existing = mergedList[existingIndex]
                    // Smart field reconciliation - prefer genuine Firebase IDs over any legacy prefixes:
                    val preferredId = when {
                        existing.id.isNotBlank() && !existing.id.contains("@") && !existing.id.startsWith("admin_") && !existing.id.startsWith("acc_") && !existing.id.startsWith("usr_") -> existing.id
                        user.id.isNotBlank() && !user.id.contains("@") && !user.id.startsWith("admin_") && !user.id.startsWith("acc_") && !user.id.startsWith("usr_") -> user.id
                        existing.id.isNotBlank() && !existing.id.contains("@") -> existing.id
                        user.id.isNotBlank() && !user.id.contains("@") -> user.id
                        existing.id.isNotBlank() -> existing.id
                        else -> user.id
                    }

                    val preferredUsername = when {
                        existing.username.isNotBlank() && existing.username != "Player" && !existing.username.startsWith("Player (") && existing.username != "Admin" && !existing.username.contains("@") -> existing.username
                        user.username.isNotBlank() && user.username != "Player" && !user.username.startsWith("Player (") && user.username != "Admin" && !user.username.contains("@") -> user.username
                        existing.ign.isNotBlank() && existing.ign != "Player" -> existing.ign
                        user.ign.isNotBlank() && user.ign != "Player" -> user.ign
                        existing.email.isNotBlank() && existing.email.contains("@") -> existing.email.substringBefore("@")
                        user.email.isNotBlank() && user.email.contains("@") -> user.email.substringBefore("@")
                        existing.gameId.isNotBlank() -> "FF ID: ${existing.gameId}"
                        user.gameId.isNotBlank() -> "FF ID: ${user.gameId}"
                        existing.username.isNotBlank() && existing.username != "Player" -> existing.username
                        user.username.isNotBlank() && user.username != "Player" -> user.username
                        else -> existing.username.ifBlank { user.username.ifBlank { "Player" } }
                    }

                    val preferredEmail = when {
                        existing.email.isNotBlank() && existing.email.contains("@") -> existing.email
                        user.email.isNotBlank() && user.email.contains("@") -> user.email
                        existing.phoneOrEmail.isNotBlank() && existing.phoneOrEmail.contains("@") -> existing.phoneOrEmail
                        user.phoneOrEmail.isNotBlank() && user.phoneOrEmail.contains("@") -> user.phoneOrEmail
                        existing.email.isNotBlank() -> existing.email
                        else -> user.email
                    }

                    val preferredIgn = when {
                        existing.ign.isNotBlank() && existing.ign != "Player" -> existing.ign
                        user.ign.isNotBlank() && user.ign != "Player" -> user.ign
                        existing.ign.isNotBlank() -> existing.ign
                        else -> user.ign
                    }

                    val preferredRole = when {
                        existing.role.contains("super", ignoreCase = true) || user.role.contains("super", ignoreCase = true) -> "super_admin"
                        existing.role.contains("tournament", ignoreCase = true) || user.role.contains("tournament", ignoreCase = true) -> "tournament_admin"
                        existing.role.contains("admin", ignoreCase = true) || user.role.contains("admin", ignoreCase = true) -> "admin"
                        existing.role.isNotBlank() && existing.role != "player" && existing.role != "user" -> existing.role
                        user.role.isNotBlank() -> user.role
                        else -> existing.role
                    }

                    val adminOverride = recentAdminWalletOverrides[preferredId]?.takeIf { System.currentTimeMillis() - it.second < 60_000L }?.first
                    val newerWalletUser = when {
                        adminOverride != null -> adminOverride
                        user.lastActive > existing.lastActive -> user
                        existing.lastActive > user.lastActive -> existing
                        else -> null
                    }
                    val preferredDepositFunds = newerWalletUser?.depositFunds ?: maxOf(user.depositFunds, existing.depositFunds)
                    val preferredWinningFunds = newerWalletUser?.winningFunds ?: maxOf(user.winningFunds, existing.winningFunds)
                    val preferredBonusFunds = newerWalletUser?.bonusFunds ?: maxOf(user.bonusFunds, existing.bonusFunds)
                    val computedBucketTotal = preferredDepositFunds + preferredWinningFunds + preferredBonusFunds
                    val preferredFunds = if (newerWalletUser != null) {
                        newerWalletUser.funds.takeIf { it > 0.0 || computedBucketTotal == 0.0 } ?: computedBucketTotal
                    } else if (computedBucketTotal > 0.0) {
                        computedBucketTotal
                    } else {
                        maxOf(user.funds, existing.funds)
                    }
                    val preferredBalance = preferredFunds
                    val preferredEarnings = maxOf(user.totalEarnings, existing.totalEarnings, preferredWinningFunds)
                    val preferredTotalDeposited = maxOf(user.totalDeposited, existing.totalDeposited, preferredDepositFunds)
                    val preferredTotalWithdrawn = maxOf(user.totalWithdrawn, existing.totalWithdrawn)
                    val preferredTotalEntryFees = maxOf(user.totalEntryFeesPaid, existing.totalEntryFeesPaid)
                    val preferredWins = maxOf(user.wins, existing.wins)
                    val preferredKills = maxOf(user.kills, existing.kills)
                    val preferredSupportCases = maxOf(user.supportCasesCount, existing.supportCasesCount)
                    val preferredActivityPoints = maxOf(user.activityPoints, existing.activityPoints)
                    val preferredGameId = if (existing.gameId.isNotBlank()) existing.gameId else user.gameId
                    val preferredPhone = if (existing.phone.isNotBlank()) existing.phone else user.phone
                    val preferredBanReason = if (existing.banReason.isNotBlank()) existing.banReason else user.banReason
                    val preferredBanCaseId = if (existing.banCaseId.isNotBlank()) existing.banCaseId else user.banCaseId
                    val preferredBannedAt = maxOf(user.bannedAt, existing.bannedAt)
                    val preferredIsSuspended = user.isSuspended || existing.isSuspended
                    val preferredSuspendedUntil = maxOf(user.suspendedUntil, existing.suspendedUntil)
                    val preferredSuspensionReason = if (existing.suspensionReason.isNotBlank()) existing.suspensionReason else user.suspensionReason
                    val preferredIsVpnBlocked = user.isVpnBlocked || existing.isVpnBlocked
                    val preferredIsForceUpdate = user.isForceUpdateRequired || existing.isForceUpdateRequired
                    val preferredMinVersion = if (existing.minVersionRequired.isNotBlank()) existing.minVersionRequired else user.minVersionRequired
                    val preferredIsMaintenanceBypass = user.isMaintenanceBypass || existing.isMaintenanceBypass
                    val preferredDeviceModel = if (existing.deviceModel.isNotBlank()) existing.deviceModel else user.deviceModel
                    val preferredIpAddress = if (existing.ipAddress.isNotBlank()) existing.ipAddress else user.ipAddress
                    val preferredCreatedAt = if (user.createdAt > 0 && existing.createdAt > 0) minOf(user.createdAt, existing.createdAt) else maxOf(user.createdAt, existing.createdAt)
                    val preferredFirstAppJoinAt = if (user.firstAppJoinAt > 0 && existing.firstAppJoinAt > 0) minOf(user.firstAppJoinAt, existing.firstAppJoinAt) else maxOf(user.firstAppJoinAt, existing.firstAppJoinAt, preferredCreatedAt)
                    val preferredFirstMatchJoinedAt = if (user.firstMatchJoinedAt > 0 && existing.firstMatchJoinedAt > 0) minOf(user.firstMatchJoinedAt, existing.firstMatchJoinedAt) else maxOf(user.firstMatchJoinedAt, existing.firstMatchJoinedAt)
                    val preferredLastMatchJoinedAt = maxOf(user.lastMatchJoinedAt, existing.lastMatchJoinedAt)
                    val preferredLastActive = maxOf(user.lastActive, existing.lastActive)

                    val mergedMatchHistoryMap = mutableMapOf<String, com.example.domain.model.PlayerMatchRecord>()
                    existing.matchHistory.forEach { if (it.tournamentId.isNotBlank()) mergedMatchHistoryMap[it.tournamentId] = it }
                    user.matchHistory.forEach { rec ->
                        if (rec.tournamentId.isNotBlank()) {
                            val prev = mergedMatchHistoryMap[rec.tournamentId]
                            mergedMatchHistoryMap[rec.tournamentId] = if (prev == null) rec else prev.copy(
                                tournamentTitle = rec.tournamentTitle.ifBlank { prev.tournamentTitle },
                                joinedAt = if (prev.joinedAt > 0 && rec.joinedAt > 0) minOf(prev.joinedAt, rec.joinedAt) else maxOf(prev.joinedAt, rec.joinedAt),
                                entryFeePaid = maxOf(prev.entryFeePaid, rec.entryFeePaid),
                                paidFromDeposit = maxOf(prev.paidFromDeposit, rec.paidFromDeposit),
                                paidFromWinning = maxOf(prev.paidFromWinning, rec.paidFromWinning),
                                paidFromBonus = maxOf(prev.paidFromBonus, rec.paidFromBonus),
                                kills = maxOf(prev.kills, rec.kills),
                                rank = if (rec.rank > 0) rec.rank else prev.rank,
                                winningAmount = maxOf(prev.winningAmount, rec.winningAmount),
                                status = if (rec.winningAmount > 0.0 || prev.winningAmount > 0.0) "WON" else rec.status.ifBlank { prev.status }
                            )
                        }
                    }
                    val mergedMatchHistory = mergedMatchHistoryMap.values.sortedByDescending { it.joinedAt }
                    val mergedJoinedIds = (existing.joinedMatchIds + user.joinedMatchIds + mergedMatchHistory.map { it.tournamentId }).filter { it.isNotBlank() }.distinct()
                    val preferredTotalMatchesJoined = maxOf(user.totalMatchesJoined, existing.totalMatchesJoined, mergedJoinedIds.size)

                    val preferredAvatarUrl = when {
                        !existing.avatarUrl.isNullOrBlank() -> existing.avatarUrl
                        !user.avatarUrl.isNullOrBlank() -> user.avatarUrl
                        else -> null
                    }
                    val preferredTokens = maxOf(user.tokens, existing.tokens)
                    val preferredLoginStreak = maxOf(user.loginStreak, existing.loginStreak)
                    val preferredReferralCode = if (existing.referralCode.isNotBlank()) existing.referralCode else user.referralCode
                    val preferredReferralCount = maxOf(user.referralCount, existing.referralCount)
                    val preferredReferralBonus = newerWalletUser?.referralBonusEarned?.takeIf { it > 0.0 }
                        ?: maxOf(user.referralBonusEarned, existing.referralBonusEarned, preferredBonusFunds)
                    val preferredAge = if (existing.age > 0) existing.age else user.age
                    val preferredDob = if (existing.dateOfBirth.isNotBlank()) existing.dateOfBirth else user.dateOfBirth

                    val mergedRaw = (existing.rawAttributes + user.rawAttributes).toMutableMap()

                    val resolvedIsBanned = when {
                        user.updatedAt > existing.updatedAt && user.updatedAt > 0L -> user.isBanned
                        existing.updatedAt > user.updatedAt && existing.updatedAt > 0L -> existing.isBanned
                        else -> user.isBanned || existing.isBanned
                    }
                    val preferredAppealTicketId = if (user.activeAppealTicketId.isNotBlank()) user.activeAppealTicketId else existing.activeAppealTicketId
                    val preferredAppealStatus = if (user.activeAppealStatus.isNotBlank()) user.activeAppealStatus else existing.activeAppealStatus
                    val preferredUpdatedAt = maxOf(user.updatedAt, existing.updatedAt)

                    val updatedUser = existing.copy(
                        id = preferredId,
                        username = preferredUsername,
                        avatarUrl = preferredAvatarUrl,
                        email = preferredEmail,
                        phone = preferredPhone,
                        gameId = preferredGameId,
                        ign = preferredIgn,
                        role = preferredRole,
                        funds = preferredFunds,
                        balance = preferredBalance,
                        depositFunds = preferredDepositFunds,
                        winningFunds = preferredWinningFunds,
                        bonusFunds = preferredBonusFunds,
                        totalEarnings = preferredEarnings,
                        totalDeposited = preferredTotalDeposited,
                        totalWithdrawn = preferredTotalWithdrawn,
                        totalEntryFeesPaid = preferredTotalEntryFees,
                        wins = preferredWins,
                        kills = preferredKills,
                        supportCasesCount = preferredSupportCases,
                        activityPoints = preferredActivityPoints,
                        tokens = preferredTokens,
                        loginStreak = preferredLoginStreak,
                        referralCode = preferredReferralCode,
                        referralCount = preferredReferralCount,
                        referralBonusEarned = preferredReferralBonus,
                        age = preferredAge,
                        dateOfBirth = preferredDob,
                        isBanned = resolvedIsBanned,
                        banReason = if (resolvedIsBanned) preferredBanReason else "",
                        banCaseId = if (resolvedIsBanned) preferredBanCaseId else preferredBanCaseId,
                        bannedAt = if (resolvedIsBanned) preferredBannedAt else 0L,
                        activeAppealTicketId = preferredAppealTicketId,
                        activeAppealStatus = preferredAppealStatus,
                        isSuspended = preferredIsSuspended,
                        suspendedUntil = preferredSuspendedUntil,
                        suspensionReason = preferredSuspensionReason,
                        isVpnBlocked = preferredIsVpnBlocked,
                        isForceUpdateRequired = preferredIsForceUpdate,
                        minVersionRequired = preferredMinVersion,
                        isMaintenanceBypass = preferredIsMaintenanceBypass,
                        deviceModel = preferredDeviceModel,
                        ipAddress = preferredIpAddress,
                        createdAt = preferredCreatedAt,
                        firstAppJoinAt = preferredFirstAppJoinAt,
                        firstMatchJoinedAt = preferredFirstMatchJoinedAt,
                        lastMatchJoinedAt = preferredLastMatchJoinedAt,
                        totalMatchesJoined = preferredTotalMatchesJoined,
                        joinedMatchIds = mergedJoinedIds,
                        matchHistory = mergedMatchHistory,
                        lastActive = preferredLastActive,
                        updatedAt = preferredUpdatedAt,
                        rawAttributes = mergedRaw
                    )
                    mergedList[existingIndex] = updatedUser
                    if (preferredId.isNotBlank()) idMap[preferredId] = existingIndex
                    if (preferredEmail.contains("@")) emailMap[preferredEmail] = existingIndex
                    val pPhone = extractDigitsFast(preferredPhone)
                    if (pPhone.length >= 10) phoneMap[pPhone.takeLast(10)] = existingIndex
                    val pGame = extractDigitsFast(preferredGameId)
                    if (pGame.length >= 5) gameIdMap[pGame] = existingIndex
                }
            }

            // 3. Auto sync active administrator profile ONLY if signed in with allowlisted real Firebase Auth
            val currentFirebaseUser = auth.currentUser
            val rawAuthEmail = currentFirebaseUser?.email?.trim()?.lowercase()
            val currentEmail = rawAuthEmail?.takeIf { TournamentBackendValidator.isAllowedAdminEmail(it) }
            val currentUid = currentFirebaseUser?.uid?.takeIf { it.isNotBlank() && currentEmail != null }
            if (!currentEmail.isNullOrBlank() && !currentUid.isNullOrBlank() && !locallyDeletedUserIds.contains(currentUid)) {
                val cleanEmail = currentEmail.trim().lowercase()
                val existingIndex = mergedList.indexOfFirst { existing ->
                    (existing.id == currentUid) ||
                    (existing.email.trim().lowercase() == cleanEmail)
                }
                if (existingIndex == -1) {
                    val username = currentFirebaseUser?.displayName?.ifBlank { null } ?: currentEmail.substringBefore("@").ifBlank { "Super Admin" }
                    mergedList.add(
                        UserProfile(
                            id = currentUid,
                            username = username,
                            email = currentEmail,
                            role = "super_admin",
                            funds = 0.0,
                            gameId = ""
                        )
                    )
                } else {
                    val existing = mergedList[existingIndex]
                    if ((existing.role.isBlank() || existing.role == "player" || existing.role == "user") && !existing.isBanned) {
                        mergedList[existingIndex] = existing.copy(role = "super_admin")
                    }
                }
            }

            // Real-time synchronization of ban status against banned_users table
            val reconciledList = mergedList.map { u ->
                val emailLower = u.email.trim().lowercase()
                val isBannedMatch = u.isBanned ||
                    bannedUids.contains(u.id) ||
                    (emailLower.isNotBlank() && bannedEmails.contains(emailLower)) ||
                    (u.gameId.isNotBlank() && bannedGameIds.contains(u.gameId))
                if (isBannedMatch) u.copy(isBanned = true) else u
            }

            val isMockUser: (UserProfile) -> Boolean = { u ->
                val emailLower = u.email.trim().lowercase()
                val idLower = u.id.trim().lowercase()
                emailLower.contains("@example.com") ||
                emailLower == "velorix.admin.sync@velorix.com" ||
                emailLower.contains("mock.dummy") ||
                idLower.startsWith("mock_")
            }
            val cleanList = reconciledList.filter { !isMockUser(it) && !it.isGuestSession }

            trySend(cleanList.sortedWith(compareByDescending<UserProfile> { it.isBanned }.thenBy { it.username.lowercase() }))
        }

        var emitJob: Job? = null
        fun scheduleEmitUsers() {
            emitJob?.cancel()
            emitJob = launch(Dispatchers.Default) {
                delay(60L)
                emitCombinedUsers()
            }
        }

        emitCombinedUsers()

        // Realtime listener for banned_users node (both tracks active ban IDs AND synthesizes UserProfile entries for banned accounts)
        val bannedUsersListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                bannedUids.clear()
                bannedEmails.clear()
                bannedGameIds.clear()
                val bannedSourceMap = mutableMapOf<String, UserProfile>()
                snapshot.children.forEach { child ->
                    val explicitBool = child.child("isBanned").value
                    val statusVal = (child.child("status").value ?: child.child("accountStatus").value)?.toString()?.trim()?.uppercase().orEmpty()
                    val isActivelyBanned = explicitBool != false &&
                        !explicitBool.toString().equals("false", ignoreCase = true) &&
                        statusVal != "UNBANNED" &&
                        statusVal != "ACTIVE"
                    if (!isActivelyBanned) return@forEach

                    val k = child.key?.trim() ?: ""
                    val uid = (child.child("uid").value ?: child.child("userId").value ?: child.child("id").value)?.toString()?.trim().orEmpty().ifBlank { k }
                    val email = (child.child("email").value ?: child.child("userEmail").value)?.toString()?.trim()?.lowercase().orEmpty()
                    val gameId = (child.child("gameId").value ?: child.child("freeFireId").value)?.toString()?.trim().orEmpty()
                    val ign = (child.child("ign").value ?: child.child("inGameName").value)?.toString()?.trim().orEmpty()
                    val uname = (child.child("username").value ?: child.child("userName").value ?: child.child("name").value ?: child.child("accountName").value)?.toString()?.trim().orEmpty()
                    val reason = (child.child("banReason").value ?: child.child("reason").value)?.toString()?.trim().orEmpty()
                    val caseId = (child.child("banCaseId").value ?: child.child("sanctionId").value ?: child.child("caseId").value)?.toString()?.trim().orEmpty()
                    val appealTicket = (child.child("activeAppealTicketId").value ?: child.child("activeAppealTicket").value ?: child.child("appealTicketId").value ?: child.child("ticketId").value)?.toString()?.trim().orEmpty()
                    val appealStatus = (child.child("activeAppealStatus").value ?: child.child("appealStatus").value)?.toString()?.trim().orEmpty()
                    val bannedAtTs = safeLong(child.child("bannedAt").value ?: child.child("timestamp").value, System.currentTimeMillis())

                    if (k.isNotBlank()) {
                        if (!k.contains("@") && !k.contains(",")) bannedUids.add(k)
                        if (k.contains("@") || k.contains("_") || k.contains(",")) {
                            bannedEmails.add(k.replace(",", ".").replace("_", ".").lowercase())
                        }
                    }
                    if (uid.isNotBlank()) bannedUids.add(uid)
                    if (email.isNotBlank()) bannedEmails.add(email)
                    if (gameId.isNotBlank()) bannedGameIds.add(gameId)

                    // Synthesize UserProfile so accounts present in /banned_users (e.g. Velorix Test) always appear in Players list
                    val profileId = uid.ifBlank { k }
                    if (profileId.isNotBlank() && !locallyDeletedUserIds.contains(profileId)) {
                        val parsed = child.toUserProfile()
                        val displayName = uname.ifBlank { ign.ifBlank { parsed?.username?.takeIf { it != "Player" }.orEmpty() } }.ifBlank {
                            if (email.contains("@")) email.substringBefore("@") else "Player (${profileId.take(6)})"
                        }
                        bannedSourceMap[profileId] = (parsed ?: UserProfile(id = profileId)).copy(
                            id = profileId,
                            username = displayName,
                            ign = ign.ifBlank { parsed?.ign.orEmpty() },
                            email = email.ifBlank { parsed?.email.orEmpty() },
                            gameId = gameId.ifBlank { parsed?.gameId.orEmpty() },
                            isBanned = true,
                            status = "BANNED",
                            accountStatus = "BANNED",
                            banReason = reason.ifBlank { parsed?.banReason.orEmpty().ifBlank { "Violation of Fair Play Terms / Anti-Cheat & Third-Party Tool Detection" } },
                            banCaseId = caseId.ifBlank { parsed?.banCaseId.orEmpty() },
                            activeAppealTicketId = appealTicket.ifBlank { parsed?.activeAppealTicketId.orEmpty() },
                            activeAppealStatus = appealStatus.ifBlank { parsed?.activeAppealStatus.orEmpty() },
                            bannedAt = bannedAtTs
                        )
                    }
                }
                usersSources["rtdb_banned_users"] = bannedSourceMap
                scheduleEmitUsers()
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        val bannedUsersRef = database.child("banned_users")
        bannedUsersRef.addValueEventListener(bannedUsersListener)

        // Realtime listener for ban appeals & support tickets to synthesize/enrich any banned player who submitted an appeal (e.g. Velorix Test • VRX-772B81 • BAN-RDKIT1)
        val appealUserNodes = listOf(
            "appeal_ban_appeals" to database.child("ban_appeals"),
            "appeal_appeals" to database.child("appeals"),
            "appeal_admin_all" to database.child("admin_requests").child("all"),
            "appeal_admin_ban" to database.child("admin_requests").child("ban_appeals"),
            "appeal_admin_acc" to database.child("admin_requests").child("account_issue"),
            "appeal_support_tickets" to database.child("support_tickets"),
            "appeal_reports" to database.child("reports")
        )
        val appealListeners = mutableListOf<Pair<DatabaseReference, ValueEventListener>>()
        appealUserNodes.forEach { (srcKey, ref) ->
            val l = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val map = mutableMapOf<String, UserProfile>()
                    snapshot.children.forEach { c ->
                        val cat = (c.child("category").value ?: c.child("issueCategory").value ?: c.child("type").value)?.toString()?.trim()?.uppercase().orEmpty()
                        val ch = (c.child("channel").value ?: c.child("channelLabel").value)?.toString()?.trim()?.lowercase().orEmpty()
                        val related = (c.child("relatedId").value ?: c.child("banCaseId").value ?: c.child("sanctionId").value ?: c.child("caseId").value)?.toString()?.trim().orEmpty()
                        val subj = (c.child("subject").value ?: c.child("title").value ?: c.child("reason").value)?.toString()?.trim().orEmpty()
                        val desc = (c.child("description").value ?: c.child("message").value ?: c.child("details").value)?.toString()?.trim().orEmpty()
                        val isAppeal = srcKey.contains("ban") || srcKey.contains("appeal") ||
                            cat.contains("BAN") || cat.contains("APPEAL") || cat.contains("ACCOUNT_ISSUE") ||
                            ch.contains("ban") || ch.contains("appeal") ||
                            related.startsWith("BAN-", ignoreCase = true) ||
                            subj.contains("ban", ignoreCase = true) || subj.contains("appeal", ignoreCase = true) ||
                            desc.contains("BAN-", ignoreCase = true)
                        if (!isAppeal) return@forEach

                        val ticketId = (c.child("id").value ?: c.child("ticketId").value ?: c.child("requestId").value ?: c.key)?.toString()?.trim().orEmpty()
                        val ticketStatus = (c.child("status").value ?: c.child("appealStatus").value)?.toString()?.trim()?.uppercase().orEmpty().ifBlank { "PENDING" }
                        val uid = (c.child("userId").value ?: c.child("uid").value)?.toString()?.trim().orEmpty()
                        val uname = (c.child("username").value ?: c.child("userName").value ?: c.child("name").value ?: c.child("accountName").value)?.toString()?.trim().orEmpty()
                        val ign = (c.child("inGameName").value ?: c.child("ign").value)?.toString()?.trim().orEmpty()
                        val email = (c.child("userEmail").value ?: c.child("email").value ?: c.child("userContact").value)?.toString()?.trim().orEmpty()
                        val gameId = (c.child("freeFireId").value ?: c.child("gameId").value)?.toString()?.trim().orEmpty()
                        val created = safeLong(c.child("createdAt").value ?: c.child("timestamp").value, System.currentTimeMillis())

                        val syntheticId = uid.ifBlank {
                            if (related.isNotBlank()) "appeal_${related.lowercase()}"
                            else if (ticketId.isNotBlank()) "appeal_${ticketId.lowercase()}"
                            else ""
                        }
                        if (syntheticId.isNotBlank() && !locallyDeletedUserIds.contains(syntheticId)) {
                            val isStillBanned = ticketStatus != "RESOLVED" && ticketStatus != "APPROVED" && ticketStatus != "UNBANNED"
                            val displayName = uname.ifBlank { ign.ifBlank { if (email.contains("@")) email.substringBefore("@") else "Player (${syntheticId.take(6)})" } }
                            map[syntheticId] = UserProfile(
                                id = syntheticId,
                                username = displayName,
                                ign = ign.ifBlank { displayName },
                                email = if (email.contains("@")) email else "",
                                phone = if (!email.contains("@") && email.any { it.isDigit() }) email else "",
                                gameId = gameId,
                                isBanned = isStillBanned,
                                status = if (isStillBanned) "BANNED" else "ACTIVE",
                                accountStatus = if (isStillBanned) "BANNED" else "ACTIVE",
                                banReason = desc.ifBlank { subj.ifBlank { "Violation of Fair Play Terms / Anti-Cheat & Third-Party Tool Detection" } },
                                banCaseId = related,
                                activeAppealTicketId = ticketId,
                                activeAppealStatus = ticketStatus,
                                bannedAt = created,
                                createdAt = created,
                                lastActive = created
                            )
                        }
                    }
                    usersSources[srcKey] = map
                    scheduleEmitUsers()
                }
                override fun onCancelled(error: DatabaseError) {}
            }
            ref.addValueEventListener(l)
            appealListeners.add(ref to l)
        }

        try {
            ensureAuthenticatedSession(activeAdminEmail)
        } catch (_: Exception) {}

        fun ingestUsersFromSnapshot(sourceKey: String, snapshot: DataSnapshot) {
            val sourceMap = mutableMapOf<String, UserProfile>()
            snapshot.children.forEach { child ->
                val user = child.toUserProfile()
                if (user != null && user.id.isNotBlank() && !locallyDeletedUserIds.contains(user.id)) {
                    val emailLower = user.email.trim().lowercase()
                    if (!emailLower.contains("@example.com")) {
                        sourceMap[user.id] = user
                        lastExtractedUsers[user.id] = user
                    }
                }
            }
            if (sourceKey == "users" && sourceMap.isNotEmpty()) {
                val cached = usersSources["extracted_cache"]?.toMutableMap()
                if (cached != null) {
                    sourceMap.keys.forEach { cached.remove(it) }
                    usersSources["extracted_cache"] = cached
                }
            }
            usersSources[sourceKey] = sourceMap
            scheduleEmitUsers()
        }

        // Listen to all user, player, account, and wallet nodes in RTDB
        val userNodes = listOf(
            "users" to database.child("users"),
            "Users" to database.child("Users"),
            "userProfiles" to database.child("userProfiles"),
            "user_profiles" to database.child("user_profiles"),
            "players" to database.child("players"),
            "Players" to database.child("Players"),
            "accounts" to database.child("accounts"),
            "registered_users" to database.child("registered_users"),
            "wallet_accounts" to database.child("wallet_accounts"),
            "wallets" to database.child("wallets"),
            "leaderboard" to database.child("leaderboard")
        )

        // Initial one-shot fetch for rapid UI population
        userNodes.forEach { (key, ref) ->
            ref.get().addOnSuccessListener { ingestUsersFromSnapshot(key, it) }
        }

        val listenersMap = mutableMapOf<String, ValueEventListener>()
        userNodes.forEach { (key, ref) ->
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) { ingestUsersFromSnapshot(key, snapshot) }
                override fun onCancelled(error: DatabaseError) { Log.w(TAG, "User node $key cancelled: ${error.message}") }
            }
            listenersMap[key] = listener
            ref.addValueEventListener(listener)
        }

        // Extract registered admins
        val adminsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sourceMap = mutableMapOf<String, UserProfile>()
                snapshot.children.forEach { child ->
                    val rawKey = child.key ?: ""
                    val email = child.child("email").value?.toString() ?: ""
                    val adminUid = if (rawKey.contains("@")) "" else rawKey
                    val name = child.child("name").value?.toString()
                        ?: child.child("username").value?.toString()
                        ?: child.child("displayName").value?.toString()
                        ?: email.substringBefore("@").ifBlank { "Admin" }
                    val role = child.child("role").value?.toString() ?: "tournament_admin"
                    if (adminUid.isNotBlank() &&
                        !locallyDeletedUserIds.contains(adminUid) &&
                        !email.contains("@example.com") &&
                        !email.equals("velorix.admin.sync@velorix.com", ignoreCase = true)
                    ) {
                        sourceMap[adminUid] = UserProfile(
                            id = adminUid,
                            username = name,
                            email = email,
                            role = role,
                            funds = 0.0,
                            gameId = ""
                        )
                    }
                }
                usersSources["admins"] = sourceMap
                scheduleEmitUsers()
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        val adminsRef = database.child("admins")
        adminsRef.addValueEventListener(adminsListener)

        // Tournament registrations listener
        val regListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sourceMap = mutableMapOf<String, UserProfile>()
                fun parseRegNode(regChild: DataSnapshot) {
                    val regUid = regChild.child("userId").value?.toString()
                        ?: regChild.child("uid").value?.toString()
                        ?: regChild.child("playerUid").value?.toString()
                        ?: regChild.child("playerId").value?.toString()
                        ?: regChild.key
                        ?: ""
                    val ign = regChild.child("ign").value?.toString()
                        ?: regChild.child("inGameName").value?.toString()
                        ?: regChild.child("gameUsername").value?.toString()
                        ?: regChild.child("name").value?.toString()
                        ?: regChild.child("playerName").value?.toString()
                        ?: regChild.child("username").value?.toString()
                        ?: ""
                    val gameId = regChild.child("gameId").value?.toString()
                        ?: regChild.child("freeFireId").value?.toString()
                        ?: regChild.child("ffId").value?.toString()
                        ?: regChild.child("gameAccountId").value?.toString() ?: ""
                    val email = regChild.child("email").value?.toString()?.trim() ?: ""
                    val phone = regChild.child("phone").value?.toString()
                        ?: regChild.child("phoneNumber").value?.toString()
                        ?: regChild.child("mobile").value?.toString() ?: ""

                    val isAuthenticUid = regUid.isNotBlank() &&
                        !regUid.startsWith("slot_") &&
                        !locallyDeletedUserIds.contains(regUid)

                    if (isAuthenticUid && (email.isNotBlank() || ign.isNotBlank() || gameId.isNotBlank())) {
                        val rawMap = mutableMapOf<String, String>()
                        regChild.children.forEach { c ->
                            val v = c.value
                            if (v != null) rawMap[c.key ?: ""] = v.toString()
                        }
                        sourceMap[regUid] = UserProfile(
                            id = regUid,
                            username = ign.ifBlank { if (email.contains("@")) email.substringBefore("@") else "Player (${regUid.take(6)})" },
                            email = email,
                            phone = phone,
                            role = "player",
                            gameId = gameId,
                            ign = ign,
                            rawAttributes = rawMap
                        )
                    }
                }
                snapshot.children.forEach { child ->
                    if (child.hasChild("userId") || child.hasChild("playerName") || child.hasChild("ign") || child.hasChild("gameUsername")) {
                        parseRegNode(child)
                    } else {
                        child.children.forEach { regChild -> parseRegNode(regChild) }
                    }
                }
                if (sourceMap.isNotEmpty()) {
                    usersSources["registrations_${snapshot.key}"] = sourceMap
                    scheduleEmitUsers()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        val regRef = database.child("registrations")
        val tourneyRegRef = database.child("tournament_registrations")
        regRef.addValueEventListener(regListener)
        tourneyRegRef.addValueEventListener(regListener)

        // Firestore real user collection listeners
        val fsCollections = listOf("users", "Users", "userProfiles", "user_profiles", "players", "Players", "accounts", "wallets", "wallet_accounts")
        val fsListeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()

        // Initial one-shot fetch from Firestore so users load immediately in lockstep with RTDB
        fsCollections.forEach { collName ->
            try {
                firestore.collection(collName).get().addOnSuccessListener { snapshot ->
                    if (snapshot != null && !snapshot.isEmpty) {
                        val sourceMap = mutableMapOf<String, UserProfile>()
                        snapshot.documents.mapNotNull { it.toUserProfile() }.forEach { user ->
                            if (user.id.isNotBlank() && !locallyDeletedUserIds.contains(user.id) && !user.email.contains("@example.com")) {
                                sourceMap[user.id] = user
                            }
                        }
                        usersSources["fs_$collName"] = sourceMap
                        scheduleEmitUsers()
                    }
                }
            } catch (_: Exception) {}
        }

        fsCollections.forEach { collName ->
            try {
                val listener = firestore.collection(collName).addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null && !snapshot.isEmpty) {
                        val sourceMap = mutableMapOf<String, UserProfile>()
                        snapshot.documents.mapNotNull { it.toUserProfile() }.forEach { user ->
                            if (user.id.isNotBlank() && !locallyDeletedUserIds.contains(user.id) && !user.email.contains("@example.com")) {
                                sourceMap[user.id] = user
                                lastExtractedUsers[user.id] = user
                            }
                        }
                        if (collName == "users" && sourceMap.isNotEmpty()) {
                            val cached = usersSources["extracted_cache"]?.toMutableMap()
                            if (cached != null) {
                                sourceMap.keys.forEach { cached.remove(it) }
                                usersSources["extracted_cache"] = cached
                            }
                        }
                        usersSources["fs_$collName"] = sourceMap
                        scheduleEmitUsers()
                    }
                }
                fsListeners.add(listener)
            } catch (e: Exception) {
                Log.d(TAG, "Firestore collection $collName optional: ${e.message}")
            }
        }

        // Also listen to Firestore banned_users & ban_appeals collections
        try {
            val fsBannedListener = firestore.collection("banned_users").addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val map = mutableMapOf<String, UserProfile>()
                    snapshot.documents.forEach { d ->
                        val isBannedField = d.getBoolean("isBanned")
                        val statusVal = (d.getString("status") ?: d.getString("accountStatus")).orEmpty().trim().uppercase()
                        if (isBannedField == false || statusVal == "UNBANNED" || statusVal == "ACTIVE") return@forEach

                        val uid = (d.getString("uid") ?: d.getString("userId") ?: d.getString("id") ?: d.id).trim()
                        val email = (d.getString("email") ?: d.getString("userEmail")).orEmpty().trim().lowercase()
                        val gameId = (d.getString("gameId") ?: d.getString("freeFireId")).orEmpty().trim()
                        val ign = (d.getString("ign") ?: d.getString("inGameName")).orEmpty().trim()
                        val uname = (d.getString("username") ?: d.getString("userName") ?: d.getString("name")).orEmpty().trim()
                        val reason = (d.getString("banReason") ?: d.getString("reason")).orEmpty().trim()
                        val caseId = (d.getString("banCaseId") ?: d.getString("sanctionId")).orEmpty().trim()
                        val appealTicket = (d.getString("activeAppealTicketId") ?: d.getString("activeAppealTicket") ?: d.getString("appealTicketId")).orEmpty().trim()
                        if (uid.isNotBlank() && !locallyDeletedUserIds.contains(uid)) {
                            bannedUids.add(uid)
                            if (email.isNotBlank()) bannedEmails.add(email)
                            if (gameId.isNotBlank()) bannedGameIds.add(gameId)
                            val displayName = uname.ifBlank { ign.ifBlank { if (email.contains("@")) email.substringBefore("@") else "Player (${uid.take(6)})" } }
                            map[uid] = (d.toUserProfile() ?: UserProfile(id = uid)).copy(
                                id = uid,
                                username = displayName,
                                ign = ign.ifBlank { displayName },
                                email = email,
                                gameId = gameId,
                                isBanned = true,
                                status = "BANNED",
                                accountStatus = "BANNED",
                                banReason = reason.ifBlank { "Violation of Fair Play Terms / Anti-Cheat & Third-Party Tool Detection" },
                                banCaseId = caseId,
                                activeAppealTicketId = appealTicket
                            )
                        }
                    }
                    usersSources["fs_banned_users"] = map
                    scheduleEmitUsers()
                }
            }
            fsListeners.add(fsBannedListener)
        } catch (_: Exception) {}

        awaitClose {
            emitJob?.cancel()
            userNodes.forEach { (key, ref) ->
                listenersMap[key]?.let { ref.removeEventListener(it) }
            }
            appealListeners.forEach { (r, l) -> r.removeEventListener(l) }
            bannedUsersRef.removeEventListener(bannedUsersListener)
            adminsRef.removeEventListener(adminsListener)
            regRef.removeEventListener(regListener)
            tourneyRegRef.removeEventListener(regListener)
            fsListeners.forEach { it.remove() }
        }
    }

    suspend fun getLiveCategoriesStream(): Flow<List<TournamentCategory>> = callbackFlow {
        val categoriesMap = mutableMapOf<String, TournamentCategory>()

        // 1. Initialize with default Free Fire categories
        FreeFireCategories.DEFAULT_CATEGORIES.forEach { cat ->
            categoriesMap[cat.id.uppercase()] = cat
        }

        fun emitCategories() {
            val allTourneys = locallyCreatedTournaments.values.toList()
            val result = categoriesMap.values.map { cat ->
                val count = allTourneys.count { t -> t.canonicalCategory.equals(cat.id, ignoreCase = true) }
                cat.copy(tournamentCount = count)
            }.sortedWith(
                compareBy<TournamentCategory> {
                    when (it.id.uppercase()) {
                        "BR" -> 0
                        "CS" -> 1
                        "LONE_WOLF" -> 2
                        "SCRIMS" -> 3
                        else -> 4
                    }
                }.thenBy { it.name }
            )
            trySend(result)
        }

        emitCategories()

        // 2. Listen to RTDB category nodes
        val rtdbCategoryNodes = listOf(
            database.child("categories"),
            database.child("FreeFire").child("categories"),
            database.child("tournament_categories")
        )

        val rtdbListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                snapshot.children.forEach { child ->
                    if (child.value is Boolean) return@forEach
                    val key = child.key ?: return@forEach
                    val isMapNode = child.hasChildren() && child.value is Map<*, *>
                    val id = if (isMapNode) (child.child("id").value?.toString() ?: key) else key
                    val name = if (isMapNode) {
                        child.child("name").value?.toString()
                            ?: child.child("label").value?.toString()
                            ?: key
                    } else {
                        (child.value as? String)?.takeIf { it.isNotBlank() } ?: return@forEach
                    }
                    val label = if (isMapNode) (child.child("label").value?.toString() ?: name) else name
                    val desc = if (isMapNode) (child.child("description").value?.toString() ?: "") else ""
                    val banner = if (isMapNode) {
                        child.child("bannerUrl").value?.toString()
                            ?: child.child("imageUrl").value?.toString() ?: ""
                    } else ""
                    val icon = if (isMapNode) (child.child("iconUrl").value?.toString() ?: "") else ""
                    val maxPlayers = if (isMapNode) {
                        safeInt(child.child("maxPlayers").value ?: child.child("defaultMaxPlayers").value, 48)
                    } else 48

                    val existing = categoriesMap[id.uppercase()]
                    categoriesMap[id.uppercase()] = TournamentCategory(
                        id = id.uppercase(),
                        name = name,
                        label = label,
                        shortName = name,
                        description = desc.ifBlank { existing?.description ?: "" },
                        iconUrl = icon.ifBlank { existing?.iconUrl ?: "" },
                        bannerUrl = banner.ifBlank { existing?.bannerUrl ?: "" },
                        defaultFormats = existing?.defaultFormats ?: listOf("SOLO", "DUO", "SQUAD"),
                        defaultMaps = existing?.defaultMaps ?: listOf("Bermuda", "Purgatory", "Kalahari"),
                        defaultMaxPlayers = maxPlayers,
                        isCustom = existing?.isCustom ?: (!FreeFireCategories.ALL_CATEGORIES.any { it.key.equals(id, ignoreCase = true) })
                    )
                }
                emitCategories()
            }
            override fun onCancelled(error: DatabaseError) {}
        }

        rtdbCategoryNodes.forEach { it.addValueEventListener(rtdbListener) }

        // 3. Listen to Firestore "categories" collection
        val fsListener = try {
            firestore.collection("categories").addSnapshotListener { snapshots, error ->
                if (error == null && snapshots != null) {
                    for (doc in snapshots.documents) {
                        val id = doc.id
                        val name = doc.getString("name") ?: doc.getString("label") ?: id
                        val label = doc.getString("label") ?: name
                        val desc = doc.getString("description") ?: ""
                        val banner = doc.getString("bannerUrl") ?: doc.getString("imageUrl") ?: ""
                        val icon = doc.getString("iconUrl") ?: ""
                        val maxP = doc.getLong("defaultMaxPlayers")?.toInt() ?: 48
                        val existing = categoriesMap[id.uppercase()]

                        categoriesMap[id.uppercase()] = TournamentCategory(
                            id = id.uppercase(),
                            name = name,
                            label = label,
                            shortName = name,
                            description = desc.ifBlank { existing?.description ?: "" },
                            iconUrl = icon.ifBlank { existing?.iconUrl ?: "" },
                            bannerUrl = banner.ifBlank { existing?.bannerUrl ?: "" },
                            defaultFormats = existing?.defaultFormats ?: listOf("SOLO", "DUO", "SQUAD"),
                            defaultMaps = existing?.defaultMaps ?: listOf("Bermuda", "Purgatory", "Kalahari"),
                            defaultMaxPlayers = maxP,
                            isCustom = !FreeFireCategories.ALL_CATEGORIES.any { it.key.equals(id, ignoreCase = true) }
                        )
                    }
                    emitCategories()
                }
            }
        } catch (_: Exception) { null }

        awaitClose {
            rtdbCategoryNodes.forEach { it.removeEventListener(rtdbListener) }
            fsListener?.remove()
        }
    }

    suspend fun saveCategory(category: TournamentCategory) {
        val catId = category.id.trim().uppercase().replace(" ", "_")
        val map = mapOf(
            "id" to catId,
            "name" to category.name,
            "label" to category.label,
            "shortName" to category.shortName,
            "description" to category.description,
            "iconUrl" to category.iconUrl,
            "bannerUrl" to category.bannerUrl,
            "defaultFormats" to category.defaultFormats,
            "defaultMaps" to category.defaultMaps,
            "defaultMaxPlayers" to category.defaultMaxPlayers,
            "isCustom" to true,
            "updatedAt" to System.currentTimeMillis()
        )
        try {
            database.child("categories").child(catId).setValue(map).await()
            database.child("FreeFire").child("categories").child(catId).setValue(map).await()
        } catch (e: Exception) {
            Log.w(TAG, "Error saving category to RTDB: ${e.message}")
        }
        try {
            firestore.collection("categories").document(catId).set(map).await()
        } catch (e: Exception) {
            Log.w(TAG, "Error saving category to Firestore: ${e.message}")
        }
        GlobalErrorManager.emitSuccess("Category '${category.name}' saved and synced across Firebase!")
    }

    suspend fun deleteCategory(categoryId: String) {
        val catId = categoryId.trim().uppercase()
        try {
            database.child("categories").child(catId).removeValue().await()
            database.child("FreeFire").child("categories").child(catId).removeValue().await()
            firestore.collection("categories").document(catId).delete().await()
            GlobalErrorManager.emitSuccess("Category '$catId' deleted from Firebase")
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to delete category: ${e.message}")
        }
    }

    private fun collectTournamentsRecursively(
        snapshot: DataSnapshot,
        outMap: MutableMap<String, Tournament>,
        depth: Int = 0
    ) {
        if (!snapshot.exists() || depth > 4) return
        val direct = snapshot.toTournament()
        if (direct != null && direct.id.isNotBlank() && !locallyDeletedTournamentIds.contains(direct.id)) {
            val existing = outMap[direct.id]
            if (existing == null || direct.registeredPlayers >= existing.registeredPlayers) {
                outMap[direct.id] = direct
            }
            return
        }
        if (snapshot.hasChildren()) {
            snapshot.children.forEach { child ->
                collectTournamentsRecursively(child, outMap, depth + 1)
            }
        }
    }

    suspend fun getLiveTournamentsStream(): Flow<List<Tournament>> = callbackFlow {
        val tournamentSources = mutableMapOf<String, Map<String, Tournament>>()

        try {
            ensureAuthenticatedSession(activeAdminEmail)
        } catch (_: Exception) {}

        val isMockTournament: (Tournament) -> Boolean = { t ->
            isPhantomOrDeletedTournament(
                tId = t.id,
                rawTitle = t.title,
                entryFee = t.entryFee,
                prizePool = t.prizePool,
                roomId = t.roomDetails?.roomId,
                roomPass = t.roomDetails?.roomPassword
            )
        }

        fun emitTournaments() {
            val combined = mutableMapOf<String, Tournament>()
            // 1. First add all tournaments created by admin locally so they never disappear
            locallyCreatedTournaments.forEach { (id, t) ->
                if (!locallyDeletedTournamentIds.contains(id) && !isMockTournament(t)) {
                    combined[id] = t
                }
            }
            // 2. Merge backend streaming sources
            tournamentSources.values.forEach { sourceMap ->
                sourceMap.forEach { (id, t) ->
                    if (!locallyDeletedTournamentIds.contains(id) && !isMockTournament(t)) {
                        val existing = combined[id]
                        if (existing == null || t.registeredPlayers >= existing.registeredPlayers) {
                            combined[id] = t
                        }
                    }
                }
            }
            trySend(combined.values.toList().sortedByDescending { it.startsAt ?: "0" })
        }

        emitTournaments()

        fun ingestTournamentsFromSnapshot(sourceKey: String, snapshot: DataSnapshot) {
            val sourceMap = mutableMapOf<String, Tournament>()
            collectTournamentsRecursively(snapshot, sourceMap, 0)
            tournamentSources[sourceKey] = sourceMap
            emitTournaments()
        }

        val rtdbNodes = listOf(
            "tournaments" to database.child("tournaments"),
            "Tournaments" to database.child("Tournaments"),
            "tournament" to database.child("tournament"),
            "Tournament" to database.child("Tournament"),
            "matches" to database.child("matches"),
            "Matches" to database.child("Matches"),
            "match" to database.child("match"),
            "Match" to database.child("Match"),
            "active_tournaments" to database.child("active_tournaments"),
            "all_tournaments" to database.child("all_tournaments"),
            "published_matches" to database.child("published_matches"),
            "custom_rooms" to database.child("custom_rooms"),
            "categories" to database.child("categories"),
            "tournaments_by_category" to database.child("tournaments_by_category"),
            "matches_by_category" to database.child("matches_by_category"),
            "FreeFire_matches" to database.child("FreeFire").child("matches"),
            "FreeFire_tournaments" to database.child("FreeFire").child("tournaments"),
            "FreeFire_categories" to database.child("FreeFire").child("categories"),
            "Free_Fire_matches" to database.child("Free Fire").child("matches"),
            "Free_Fire_tournaments" to database.child("Free Fire").child("tournaments"),
            "BGMI_matches" to database.child("BGMI").child("matches"),
            "BGMI_tournaments" to database.child("BGMI").child("tournaments"),
            "game_tournaments" to database.child("game_tournaments")
        )

        // Rapid initial fetch on all nodes
        rtdbNodes.forEach { (key, nodeRef) ->
            nodeRef.get().addOnSuccessListener { ingestTournamentsFromSnapshot(key, it) }
        }

        val listenersMap = mutableMapOf<String, ValueEventListener>()
        rtdbNodes.forEach { (key, nodeRef) ->
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    ingestTournamentsFromSnapshot(key, snapshot)
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Tournaments RTDB cancelled for $key: ${error.message}")
                }
            }
            listenersMap[key] = listener
            nodeRef.addValueEventListener(listener)
        }

        // Firestore listener with graceful fallback across root and category subcollections
        val fsCollections = listOf(
            "tournaments", "Tournaments", "matches", "Matches",
            "active_tournaments", "all_tournaments", "published_matches", "custom_rooms",
            "categories/BR/matches", "categories/CS/matches", "categories/LONE_WOLF/matches", "categories/SCRIMS/matches",
            "categories/br/matches", "categories/cs/matches", "categories/lone_wolf/matches", "categories/scrims/matches",
            "categories/BR/tournaments", "categories/CS/tournaments", "categories/LONE_WOLF/tournaments", "categories/SCRIMS/tournaments",
            "tournaments_by_category/BR/items", "tournaments_by_category/CS/items", "tournaments_by_category/LONE_WOLF/items", "tournaments_by_category/SCRIMS/items"
        )
        fsCollections.forEach { colName ->
            try {
                firestore.collection(colName).get().addOnSuccessListener { snapshot ->
                    if (snapshot != null && !snapshot.isEmpty) {
                        val sourceMap = mutableMapOf<String, Tournament>()
                        snapshot.documents.mapNotNull { it.toTournament() }.forEach { t ->
                            if (t.id.isNotBlank() && !locallyDeletedTournamentIds.contains(t.id) && !isMockTournament(t)) {
                                sourceMap[t.id] = t
                            }
                        }
                        tournamentSources["fs_$colName"] = sourceMap
                        emitTournaments()
                    }
                }
            } catch (_: Exception) {}
        }

        val fsRegistrations = fsCollections.mapNotNull { colName ->
            try {
                firestore.collection(colName).addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null) {
                        val sourceMap = mutableMapOf<String, Tournament>()
                        snapshot.documents.mapNotNull { it.toTournament() }.forEach { t ->
                            if (t.id.isNotBlank() && !locallyDeletedTournamentIds.contains(t.id) && !isMockTournament(t)) {
                                sourceMap[t.id] = t
                            }
                        }
                        tournamentSources["fs_$colName"] = sourceMap
                        emitTournaments()
                    }
                }
            } catch (_: Exception) {
                null
            }
        }

        awaitClose {
            rtdbNodes.forEach { (key, nodeRef) ->
                listenersMap[key]?.let { nodeRef.removeEventListener(it) }
            }
            fsRegistrations.forEach { reg ->
                reg.remove()
            }
        }
    }

    suspend fun getAvailableTournaments(): List<Tournament> {
        try {
            ensureAuthenticatedSession(activeAdminEmail)
        } catch (_: Exception) {}
        val result = mutableMapOf<String, Tournament>()
        val nodes = listOf(
            "tournaments", "Tournaments", "tournament", "Tournament",
            "matches", "Matches", "match", "Match",
            "active_tournaments", "all_tournaments", "published_matches", "custom_rooms",
            "categories", "tournaments_by_category", "matches_by_category",
            "FreeFire/matches", "Free Fire/matches", "FreeFire/tournaments", "Free Fire/tournaments",
            "FreeFire/categories", "BGMI/matches", "BGMI/tournaments", "game_tournaments"
        )
        for (node in nodes) {
            try {
                val snap = database.child(node).get().await()
                collectTournamentsRecursively(snap, result, 0)
            } catch (_: Exception) {}
        }

        // Firestore aggregate fallback with Source.SERVER
        val fsCollections = listOf(
            "tournaments", "Tournaments", "matches", "Matches",
            "active_tournaments", "all_tournaments", "published_matches", "custom_rooms",
            "categories/BR/matches", "categories/CS/matches", "categories/LONE_WOLF/matches", "categories/SCRIMS/matches",
            "categories/br/matches", "categories/cs/matches", "categories/lone_wolf/matches", "categories/scrims/matches",
            "categories/BR/tournaments", "categories/CS/tournaments", "categories/LONE_WOLF/tournaments", "categories/SCRIMS/tournaments",
            "tournaments_by_category/BR/items", "tournaments_by_category/CS/items", "tournaments_by_category/LONE_WOLF/items", "tournaments_by_category/SCRIMS/items"
        )
        for (col in fsCollections) {
            try {
                val fsSnap = try {
                    firestore.collection(col).get(Source.SERVER).await()
                } catch (_: Exception) {
                    firestore.collection(col).get().await()
                }
                fsSnap.documents.mapNotNull { it.toTournament() }.forEach { t ->
                    if (t.id.isNotBlank() && !locallyDeletedTournamentIds.contains(t.id)) {
                        result[t.id] = t
                    }
                }
            } catch (_: Exception) {}
        }

        // Always preserve and merge all locally created/updated tournaments so they are never dropped
        locallyCreatedTournaments.forEach { (id, t) ->
            if (!locallyDeletedTournamentIds.contains(id)) {
                result[id] = t
            }
        }

        return result.values.toList().sortedBy { it.id }
    }

    /**
     * Authoritative Live Backend Extraction Engine.
     * Directly queries Firebase Realtime Database (RTDB) and Cloud Firestore (using Source.SERVER to bypass cache).
     * Bypasses all local in-memory lock states and extracts genuine server-side tournaments, users, and stats.
     */
    suspend fun extractRealDataFromBackend(forceServer: Boolean = true): BackendExtractionResult = withContext(Dispatchers.IO) {
        try {
            // 0. Ensure Firebase Auth session is active so Firestore & RTDB security rules grant full read access
            try {
                ensureAuthenticatedSession(activeAdminEmail)
            } catch (_: Exception) {}

            // 1. Force network reconnection on RTDB
            try {
                database.database.goOnline()
            } catch (_: Exception) {}

            // 2. Reload persistent deleted tournament IDs (without destructive cloud auto-purge)
            loadDeletedTournamentIdsFromPrefs()
            locallyDeletedUserIds.clear()

            val isMockTournament: (Tournament) -> Boolean = { t ->
                isPhantomOrDeletedTournament(
                    tId = t.id,
                    rawTitle = t.title,
                    entryFee = t.entryFee,
                    prizePool = t.prizePool,
                    roomId = t.roomDetails?.roomId,
                    roomPass = t.roomDetails?.roomPassword
                )
            }

            // 3. Extract Tournaments directly from Realtime Database (all paths, recursively)
            val extractedTournaments = mutableMapOf<String, Tournament>()
            val rtdbTournamentNodes = listOf(
                "tournaments", "Tournaments", "tournament", "Tournament",
                "matches", "Matches", "match", "Match",
                "active_tournaments", "all_tournaments", "published_matches", "custom_rooms",
                "categories", "tournaments_by_category", "matches_by_category",
                "FreeFire/tournaments", "FreeFire/matches", "FreeFire/categories",
                "Free Fire/tournaments", "Free Fire/matches",
                "BGMI/tournaments", "BGMI/matches", "game_tournaments"
            )
            for (node in rtdbTournamentNodes) {
                try {
                    val snap = database.child(node).get().await()
                    collectTournamentsRecursively(snap, extractedTournaments, 0)
                } catch (e: Exception) {
                    Log.w(TAG, "RTDB extract node $node: ${e.message}")
                }
            }

            // 4. Extract Tournaments directly from Firestore (root collections + category subcollections)
            val fsCollections = listOf(
                "tournaments", "Tournaments", "matches", "Matches",
                "active_tournaments", "all_tournaments", "published_matches", "custom_rooms",
                "categories/BR/matches", "categories/CS/matches", "categories/LONE_WOLF/matches", "categories/SCRIMS/matches",
                "categories/br/matches", "categories/cs/matches", "categories/lone_wolf/matches", "categories/scrims/matches",
                "categories/BR/tournaments", "categories/CS/tournaments", "categories/LONE_WOLF/tournaments", "categories/SCRIMS/tournaments",
                "tournaments_by_category/BR/items", "tournaments_by_category/CS/items", "tournaments_by_category/LONE_WOLF/items", "tournaments_by_category/SCRIMS/items"
            )
            for (col in fsCollections) {
                try {
                    val fsSnap = if (forceServer) {
                        try {
                            firestore.collection(col).get(Source.SERVER).await()
                        } catch (_: Exception) {
                            firestore.collection(col).get().await()
                        }
                    } else {
                        firestore.collection(col).get().await()
                    }
                    fsSnap.documents.mapNotNull { it.toTournament() }.forEach { t ->
                        if (t.id.isNotBlank() && !locallyDeletedTournamentIds.contains(t.id) && !isMockTournament(t)) {
                            val existing = extractedTournaments[t.id]
                            if (existing == null || t.registeredPlayers >= existing.registeredPlayers) {
                                extractedTournaments[t.id] = t
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore extract col $col: ${e.message}")
                }
            }

            // Always preserve all admin-created tournaments
            locallyCreatedTournaments.forEach { (id, t) ->
                if (!locallyDeletedTournamentIds.contains(id) && !isMockTournament(t)) {
                    extractedTournaments[id] = t
                }
            }

            // 5. Extract Users directly from RTDB across all user/player/wallet/account nodes
            val extractedUsers = mutableMapOf<String, UserProfile>()
            val rtdbUserNodes = listOf(
                "users", "Users", "userProfiles", "user_profiles",
                "players", "Players", "accounts", "registered_users",
                "wallet_accounts", "wallets", "leaderboard"
            )
            for (node in rtdbUserNodes) {
                try {
                    val snap = database.child(node).get().await()
                    snap.children.forEach { child ->
                        val u = child.toUserProfile()
                        if (u != null && u.id.isNotBlank() && !u.email.contains("@example.com")) {
                            val existing = extractedUsers[u.id]
                            if (existing != null) {
                                extractedUsers[u.id] = existing.copy(
                                    username = if (existing.username.isNotBlank() && !existing.username.startsWith("Player")) existing.username else u.username,
                                    email = existing.email.ifBlank { u.email },
                                    phone = existing.phone.ifBlank { u.phone },
                                    gameId = existing.gameId.ifBlank { u.gameId },
                                    ign = existing.ign.ifBlank { u.ign },
                                    funds = maxOf(existing.funds, u.funds),
                                    winningFunds = maxOf(existing.winningFunds, u.winningFunds),
                                    depositFunds = maxOf(existing.depositFunds, u.depositFunds),
                                    bonusFunds = maxOf(existing.bonusFunds, u.bonusFunds)
                                )
                            } else {
                                extractedUsers[u.id] = u
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "RTDB extract user node $node: ${e.message}")
                }
            }

            // Extract admins from RTDB
            try {
                val adminSnap = database.child("admins").get().await()
                adminSnap.children.forEach { child ->
                    val rawKey = child.key ?: ""
                    val email = child.child("email").value?.toString() ?: ""
                    val adminUid = if (rawKey.contains("@")) "" else rawKey
                    val name = child.child("name").value?.toString()
                        ?: child.child("username").value?.toString()
                        ?: email.substringBefore("@").ifBlank { "Admin" }
                    val role = child.child("role").value?.toString() ?: "tournament_admin"
                    if (adminUid.isNotBlank() &&
                        !email.contains("@example.com") &&
                        !email.equals("velorix.admin.sync@velorix.com", ignoreCase = true)
                    ) {
                        val existing = extractedUsers[adminUid]
                        extractedUsers[adminUid] = existing?.copy(role = role) ?: UserProfile(
                            id = adminUid,
                            username = name,
                            email = email,
                            role = role
                        )
                    }
                }
            } catch (_: Exception) {}

            // Extract tournament registrations and tournament participants to capture all active players
            for (regNodeName in listOf("registrations", "tournament_registrations")) {
                try {
                    val regSnap = database.child(regNodeName).get().await()
                    fun extractRegChild(regChild: DataSnapshot) {
                        val regUid = regChild.child("userId").value?.toString()
                            ?: regChild.child("uid").value?.toString()
                            ?: regChild.child("playerId").value?.toString()
                            ?: regChild.key
                            ?: ""
                        val ign = regChild.child("ign").value?.toString()
                            ?: regChild.child("inGameName").value?.toString()
                            ?: regChild.child("gameUsername").value?.toString()
                            ?: regChild.child("playerName").value?.toString()
                            ?: regChild.child("name").value?.toString()
                            ?: ""
                        val email = regChild.child("email").value?.toString()?.trim() ?: ""
                        val gameId = regChild.child("gameId").value?.toString()
                            ?: regChild.child("freeFireId").value?.toString()
                            ?: ""
                        if (regUid.isNotBlank() && !regUid.startsWith("slot_")) {
                            if (!extractedUsers.containsKey(regUid)) {
                                extractedUsers[regUid] = UserProfile(
                                    id = regUid,
                                    username = ign.ifBlank { if (email.contains("@")) email.substringBefore("@") else "Player (${regUid.take(6)})" },
                                    email = email,
                                    gameId = gameId,
                                    ign = ign,
                                    role = "player"
                                )
                            }
                        }
                    }
                    regSnap.children.forEach { child ->
                        if (child.hasChild("userId") || child.hasChild("playerName") || child.hasChild("ign") || child.hasChild("gameUsername")) {
                            extractRegChild(child)
                        } else {
                            child.children.forEach { sub -> extractRegChild(sub) }
                        }
                    }
                } catch (_: Exception) {}
            }

            // Also extract any participants embedded inside extractedTournaments
            extractedTournaments.values.forEach { t ->
                t.registrations.forEach { p ->
                    val pUid = p.userId.ifBlank { p.playerId.ifBlank { p.id } }
                    if (pUid.isNotBlank() && !pUid.startsWith("slot_") && !extractedUsers.containsKey(pUid)) {
                        extractedUsers[pUid] = UserProfile(
                            id = pUid,
                            username = p.playerName.ifBlank { p.gameUsername.ifBlank { "Player (${pUid.take(6)})" } },
                            ign = p.gameUsername.ifBlank { p.playerName },
                            gameId = p.gameId,
                            role = "player"
                        )
                    }
                }
            }

            // 6. Extract Users from Cloud Firestore with Source.SERVER
            val fsUserCols = listOf("users", "Users", "userProfiles", "user_profiles", "players", "Players", "accounts", "wallets", "wallet_accounts")
            for (col in fsUserCols) {
                try {
                    val snap = if (forceServer) {
                        try {
                            firestore.collection(col).get(Source.SERVER).await()
                        } catch (_: Exception) {
                            firestore.collection(col).get().await()
                        }
                    } else {
                        firestore.collection(col).get().await()
                    }
                    snap.documents.mapNotNull { it.toUserProfile() }.forEach { u ->
                        if (u.id.isNotBlank() && !u.email.contains("@example.com")) {
                            val existing = extractedUsers[u.id]
                            if (existing != null) {
                                extractedUsers[u.id] = existing.copy(
                                    username = if (existing.username.isNotBlank() && !existing.username.startsWith("Player")) existing.username else u.username,
                                    email = existing.email.ifBlank { u.email },
                                    phone = existing.phone.ifBlank { u.phone },
                                    gameId = existing.gameId.ifBlank { u.gameId },
                                    ign = existing.ign.ifBlank { u.ign },
                                    funds = maxOf(existing.funds, u.funds),
                                    winningFunds = maxOf(existing.winningFunds, u.winningFunds),
                                    depositFunds = maxOf(existing.depositFunds, u.depositFunds),
                                    bonusFunds = maxOf(existing.bonusFunds, u.bonusFunds),
                                    tokens = maxOf(existing.tokens, u.tokens),
                                    wins = maxOf(existing.wins, u.wins),
                                    kills = maxOf(existing.kills, u.kills),
                                    avatarUrl = existing.avatarUrl ?: u.avatarUrl,
                                    role = if (existing.role.contains("admin", ignoreCase = true)) existing.role else u.role
                                )
                            } else {
                                extractedUsers[u.id] = u
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore extract users col $col: ${e.message}")
                }
            }

            // 7. Check Banned Users status from RTDB & Firestore and synthesize any missing banned accounts
            try {
                val banSnap = database.child("banned_users").get().await()
                banSnap.children.forEach { child ->
                    val explicitBool = child.child("isBanned").value
                    val statusVal = (child.child("status").value ?: child.child("accountStatus").value)?.toString()?.trim()?.uppercase().orEmpty()
                    if (explicitBool == false || explicitBool.toString().equals("false", true) || statusVal == "UNBANNED" || statusVal == "ACTIVE") return@forEach
                    val k = child.key?.trim().orEmpty()
                    val uid = (child.child("uid").value ?: child.child("userId").value ?: child.child("id").value)?.toString()?.trim().orEmpty().ifBlank { k }
                    val email = (child.child("email").value ?: child.child("userEmail").value)?.toString()?.trim().orEmpty()
                    val gameId = (child.child("gameId").value ?: child.child("freeFireId").value)?.toString()?.trim().orEmpty()
                    val ign = (child.child("ign").value ?: child.child("inGameName").value)?.toString()?.trim().orEmpty()
                    val uname = (child.child("username").value ?: child.child("userName").value ?: child.child("name").value)?.toString()?.trim().orEmpty()
                    val reason = (child.child("banReason").value ?: child.child("reason").value)?.toString()?.trim().orEmpty()
                    val caseId = (child.child("banCaseId").value ?: child.child("sanctionId").value)?.toString()?.trim().orEmpty()
                    val appealTicket = (child.child("activeAppealTicketId").value ?: child.child("activeAppealTicket").value ?: child.child("appealTicketId").value)?.toString()?.trim().orEmpty()
                    if (uid.isNotBlank()) {
                        val existing = extractedUsers[uid]
                        if (existing != null) {
                            extractedUsers[uid] = existing.copy(
                                isBanned = true,
                                banReason = existing.banReason.ifBlank { reason },
                                banCaseId = existing.banCaseId.ifBlank { caseId },
                                activeAppealTicketId = existing.activeAppealTicketId.ifBlank { appealTicket }
                            )
                        } else {
                            val displayName = uname.ifBlank { ign.ifBlank { if (email.contains("@")) email.substringBefore("@") else "Player (${uid.take(6)})" } }
                            extractedUsers[uid] = (child.toUserProfile() ?: UserProfile(id = uid)).copy(
                                id = uid,
                                username = displayName,
                                ign = ign.ifBlank { displayName },
                                email = email,
                                gameId = gameId,
                                isBanned = true,
                                status = "BANNED",
                                accountStatus = "BANNED",
                                banReason = reason.ifBlank { "Violation of Fair Play Terms / Anti-Cheat & Third-Party Tool Detection" },
                                banCaseId = caseId,
                                activeAppealTicketId = appealTicket
                            )
                        }
                    }
                }
            } catch (_: Exception) {}

            // 8. Count support tickets & payout requests and enrich per-player telemetry
            var ticketsCount = 0
            var payoutsCount = 0
            val ticketsPerUser = mutableMapOf<String, Int>()
            try {
                val tSnap = database.child("support_tickets").get().await()
                ticketsCount = tSnap.childrenCount.toInt()
                tSnap.children.forEach { child ->
                    val uId = child.child("userId").value?.toString()?.trim() ?: ""
                    val uEmail = child.child("userEmail").value?.toString()?.trim()?.lowercase() ?: ""
                    if (uId.isNotBlank()) ticketsPerUser[uId] = (ticketsPerUser[uId] ?: 0) + 1
                    if (uEmail.isNotBlank()) ticketsPerUser[uEmail] = (ticketsPerUser[uEmail] ?: 0) + 1
                }
            } catch (_: Exception) {}
            try {
                payoutsCount = database.child("payout_requests").get().await().childrenCount.toInt()
            } catch (_: Exception) {}

            val cleanTourneys = extractedTournaments.values.toList().sortedByDescending { it.startsAt ?: "0" }

            // 9. Cross-reference tournaments & participants into each user's matchHistory & lifecycle timestamps
            extractedUsers.keys.toList().forEach { uid ->
                val u = extractedUsers[uid] ?: return@forEach
                val matchMap = mutableMapOf<String, com.example.domain.model.PlayerMatchRecord>()
                u.matchHistory.forEach { if (it.tournamentId.isNotBlank()) matchMap[it.tournamentId] = it }

                cleanTourneys.forEach { tourney ->
                    val participant = tourney.participants.find { p ->
                        p.userId == u.id || p.playerId == u.id || p.id == u.id ||
                        (u.gameId.isNotBlank() && (p.gameId == u.gameId || p.gameAccountId == u.gameId))
                    }
                    if (participant != null) {
                        val existingRec = matchMap[tourney.id]
                        val joinedTs = if (participant.registeredAt > 0L) participant.registeredAt else (existingRec?.joinedAt ?: 0L)
                        val feePaid = if (participant.entryFeePaid > 0.0) participant.entryFeePaid else (existingRec?.entryFeePaid ?: tourney.entryFee.toDouble())
                        matchMap[tourney.id] = com.example.domain.model.PlayerMatchRecord(
                            tournamentId = tourney.id,
                            tournamentTitle = tourney.title,
                            game = tourney.game,
                            category = tourney.canonicalCategory,
                            format = tourney.format,
                            map = tourney.map,
                            joinedAt = joinedTs,
                            entryFeePaid = feePaid,
                            paidFromDeposit = if (participant.paidFromDeposit > 0.0) participant.paidFromDeposit else (existingRec?.paidFromDeposit ?: feePaid),
                            paidFromWinning = maxOf(participant.paidFromWinning, existingRec?.paidFromWinning ?: 0.0),
                            paidFromBonus = maxOf(participant.paidFromBonus, existingRec?.paidFromBonus ?: 0.0),
                            kills = maxOf(participant.kills, existingRec?.kills ?: 0),
                            rank = if (participant.rank > 0) participant.rank else (existingRec?.rank ?: 0),
                            winningAmount = maxOf(participant.winningAmount, existingRec?.winningAmount ?: 0.0),
                            status = if (maxOf(participant.winningAmount, existingRec?.winningAmount ?: 0.0) > 0.0) "WON" else tourney.status
                        )
                    }
                }

                val fullHistory = matchMap.values.sortedByDescending { it.joinedAt }
                val joinedIds = (u.joinedMatchIds + fullHistory.map { it.tournamentId }).filter { it.isNotBlank() }.distinct()
                val validJoinTimes = fullHistory.map { it.joinedAt }.filter { it > 0L }
                val computedFirstMatchAt = when {
                    u.firstMatchJoinedAt > 0L && validJoinTimes.isNotEmpty() -> minOf(u.firstMatchJoinedAt, validJoinTimes.minOrNull()!!)
                    validJoinTimes.isNotEmpty() -> validJoinTimes.minOrNull()!!
                    else -> u.firstMatchJoinedAt
                }
                val computedLastMatchAt = maxOf(u.lastMatchJoinedAt, validJoinTimes.maxOrNull() ?: 0L)
                val computedFirstAppAt = if (u.firstAppJoinAt > 0L) u.firstAppJoinAt else if (u.createdAt > 0L) u.createdAt else computedFirstMatchAt
                val totalEntryPaid = maxOf(u.totalEntryFeesPaid, fullHistory.sumOf { it.entryFeePaid })
                val totalMatchWinnings = maxOf(u.totalEarnings, fullHistory.sumOf { it.winningAmount })
                val totalMatchKills = maxOf(u.kills, fullHistory.sumOf { it.kills })
                val casesCount = maxOf(u.supportCasesCount, (ticketsPerUser[u.id] ?: 0) + (ticketsPerUser[u.email.trim().lowercase()] ?: 0))

                val enrichedUser = u.copy(
                    firstAppJoinAt = computedFirstAppAt,
                    createdAt = if (u.createdAt > 0L) u.createdAt else computedFirstAppAt,
                    firstMatchJoinedAt = computedFirstMatchAt,
                    lastMatchJoinedAt = computedLastMatchAt,
                    totalMatchesJoined = maxOf(u.totalMatchesJoined, joinedIds.size),
                    joinedMatchIds = joinedIds,
                    matchHistory = fullHistory,
                    totalEntryFeesPaid = totalEntryPaid,
                    totalEarnings = totalMatchWinnings,
                    kills = totalMatchKills,
                    supportCasesCount = casesCount
                )
                extractedUsers[uid] = enrichedUser
            }

            val cleanUserList = extractedUsers.values
                .filter { u ->
                    !u.isGuestSession &&
                    !u.email.equals("velorix.admin.sync@velorix.com", ignoreCase = true) &&
                    !u.email.contains("@example.com") &&
                    !u.id.startsWith("mock_", ignoreCase = true)
                }
                .sortedWith(compareByDescending<UserProfile> { it.isBanned }.thenBy { it.username.lowercase() })

            // Cache extracted tournaments & users so stream restarts never emit empty state
            cleanTourneys.forEach { t ->
                if (!locallyDeletedTournamentIds.contains(t.id)) {
                    locallyCreatedTournaments[t.id] = t
                }
            }
            cleanUserList.forEach { u ->
                if (!locallyDeletedUserIds.contains(u.id)) {
                    lastExtractedUsers[u.id] = u
                }
            }

            Log.i(TAG, "Backend extraction complete: ${cleanTourneys.size} tournaments, ${cleanUserList.size} users extracted live from Firebase.")
            BackendExtractionResult(
                success = true,
                tournamentsCount = cleanTourneys.size,
                usersCount = cleanUserList.size,
                supportTicketsCount = ticketsCount,
                payoutRequestsCount = payoutsCount,
                tournaments = cleanTourneys,
                users = cleanUserList,
                message = "Successfully extracted ${cleanTourneys.size} tournaments & ${cleanUserList.size} users from Firebase backend!"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Backend extraction failed: ${e.message}", e)
            BackendExtractionResult(
                success = false,
                tournamentsCount = 0,
                usersCount = 0,
                message = "Extraction error: ${e.message}"
            )
        }
    }

    suspend fun getPendingRegistrations(): List<PlayerRegistration> {
        return try {
            val rtdbSnap = database.child("tournament_registrations").get().await()
            val list = rtdbSnap.children.mapNotNull { child ->
                if (!child.hasChildren() || child.value !is Map<*, *>) null
                else child.safeGetObject<PlayerRegistration>()
            }
            val pendingFromRtdb = list.filter { it.status.equals("pending", ignoreCase = true) }
            if (pendingFromRtdb.isNotEmpty()) {
                pendingFromRtdb
            } else {
                val snapshot = firestore.collection("tournament_registrations")
                    .whereEqualTo("status", "pending")
                    .get().await()
                snapshot.documents.mapNotNull { it.toObject(PlayerRegistration::class.java) }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateRegistrationStatus(registrationId: String, status: String) {
        try {
            val regRef = database.child("tournament_registrations").child(registrationId)
            val regSnap = regRef.get().await()
            val tourneyId = regSnap.child("tournamentId").value?.toString() ?: ""
            val userId = regSnap.child("userId").value?.toString() ?: ""

            regRef.child("status").setValue(status).await()
            if (tourneyId.isNotBlank() && userId.isNotBlank()) {
                database.child("tournaments").child(tourneyId).child("participants").child(userId).child("status").setValue(status)
            }
            try {
                firestore.collection("tournament_registrations").document(registrationId).update("status", status).await()
            } catch (ignored: Exception) {}
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getTournamentParticipants(tournamentId: String): List<PlayerRegistration> {
        val result = mutableMapOf<String, PlayerRegistration>()
        fun parseParticipant(pChild: DataSnapshot) {
            val pId = pChild.key ?: ""
            val pUserId = pChild.child("userId").value?.toString()
                ?: pChild.child("userUid").value?.toString()
                ?: pChild.child("uid").value?.toString()
                ?: pId
            val pName = pChild.child("playerName").value?.toString()
                ?: pChild.child("username").value?.toString()
                ?: pChild.child("name").value?.toString()
                ?: "Player"
            val ign = pChild.child("inGameName").value?.toString()
                ?: pChild.child("gameUsername").value?.toString()
                ?: pChild.child("ign").value?.toString()
                ?: pName
            val ffId = pChild.child("freeFireId").value?.toString()
                ?: pChild.child("gameId").value?.toString()
                ?: pChild.child("gameAccountId").value?.toString()
                ?: ""
            val regAt = safeLong(pChild.child("registeredAt").value ?: pChild.child("joinedAt").value ?: pChild.child("timestamp").value, System.currentTimeMillis())
            val payStatus = pChild.child("paymentStatus").value?.toString() ?: "confirmed"
            val stat = pChild.child("status").value?.toString() ?: "confirmed"
            result[pUserId] = PlayerRegistration(
                id = pId,
                tournamentId = tournamentId,
                userId = pUserId,
                playerId = pUserId,
                playerName = pName,
                gameUsername = ign,
                gameId = ffId,
                paymentStatus = payStatus,
                registeredAt = regAt,
                status = stat
            )
        }

        return try {
            val rtdbSnap = database.child("tournaments").child(tournamentId).child("participants").get().await()
            rtdbSnap.children.forEach { parseParticipant(it) }

            val slotsSnap = database.child("tournaments").child(tournamentId).child("slots").get().await()
            slotsSnap.children.forEach { parseParticipant(it) }

            val regSnap = database.child("registrations").child(tournamentId).get().await()
            regSnap.children.forEach { parseParticipant(it) }

            val allRegSnap = database.child("tournament_registrations").get().await()
            allRegSnap.children.forEach {
                val tId = it.child("tournamentId").value?.toString() ?: ""
                if (tId == tournamentId || it.key?.startsWith("${tournamentId}_") == true) {
                    parseParticipant(it)
                }
            }

            try {
                val fsSnap = firestore.collection("tournaments").document(tournamentId)
                    .collection("participants").get().await()
                fsSnap.documents.forEach { doc ->
                    val uid = doc.getString("userId") ?: doc.getString("userUid") ?: doc.id
                    if (!result.containsKey(uid)) {
                        result[uid] = PlayerRegistration(
                            id = doc.id,
                            tournamentId = tournamentId,
                            userId = uid,
                            playerId = uid,
                            playerName = doc.getString("playerName") ?: doc.getString("username") ?: "Player",
                            gameUsername = doc.getString("inGameName") ?: doc.getString("gameUsername") ?: "Player",
                            gameId = doc.getString("freeFireId") ?: doc.getString("gameId") ?: "",
                            paymentStatus = doc.getString("paymentStatus") ?: "confirmed",
                            registeredAt = safeLong(doc.get("joinedAt") ?: doc.get("registeredAt"), System.currentTimeMillis()),
                            status = doc.getString("status") ?: "confirmed"
                        )
                    }
                }
            } catch (_: Exception) {}

            result.values.toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun buildTournamentMap(tournament: Tournament): Map<String, Any?> {
        val canonicalGame = tournament.game.ifBlank { "Free Fire" }
        val canonicalCategory = tournament.canonicalCategory
        val categoryDisplayName = tournament.categoryDisplayName
        val canonicalTitle = tournament.title.ifBlank { "Velorix Esports Match" }
        val canonicalMap = tournament.map.ifBlank { "Bermuda" }
        val canonicalStatus = tournament.status.ifBlank { "UPCOMING" }
        val canonicalFormat = tournament.format.ifBlank { "SOLO" }
        val slotsRemaining = (tournament.maxPlayers - tournament.registeredPlayers).coerceAtLeast(0)
        val now = System.currentTimeMillis()
        val createdAt = if (tournament.rulesModifiedAt > 0L) tournament.rulesModifiedAt else now

        val ruleStatus = when (canonicalStatus.uppercase()) {
            "LIVE", "ACTIVE" -> "live"
            "COMPLETED", "ENDED" -> "completed"
            "CANCELLED" -> "cancelled"
            else -> "upcoming"
        }
        val ruleFormat = when {
            canonicalFormat.contains("duo", true) -> "Duo"
            canonicalFormat.contains("squad", true) -> "Squad"
            canonicalFormat.contains("cs", true) || canonicalFormat.contains("4v4", true) -> "CS 4v4"
            else -> "Solo"
        }
        val parsedStartTime = tournament.startsAt?.toLongOrNull()
            ?: (if (tournament.rulesModifiedAt > 0L) tournament.rulesModifiedAt else (now + 3600_000L))
        val formattedScheduleStr = tournament.startsAt ?: formatScheduleTime(parsedStartTime.toString())

        val roomMap = tournament.roomDetails?.let {
            mapOf(
                "roomId" to it.roomId,
                "room_id" to it.roomId,
                "roomPassword" to it.roomPassword,
                "room_password" to it.roomPassword,
                "password" to it.roomPassword,
                "updatedAt" to it.updatedAt
            )
        }

        return mapOf(
            // Primary Canonical Identifiers
            "id" to tournament.id,
            "tournamentId" to tournament.id,
            "tournament_id" to tournament.id,
            "matchId" to tournament.id,
            "match_id" to tournament.id,

            // Titles & Names
            "title" to canonicalTitle,
            "name" to canonicalTitle,
            "tournamentName" to canonicalTitle,
            "tournament_name" to canonicalTitle,
            "matchTitle" to canonicalTitle,
            "match_title" to canonicalTitle,

            // Game & Categories (supporting both exact, uppercase and subcategory matching)
            "game" to canonicalGame,
            "gameName" to canonicalGame,
            "game_name" to canonicalGame,
            "gameTitle" to canonicalGame,
            "game_title" to canonicalGame,
            "category" to canonicalCategory,
            "canonicalCategory" to canonicalCategory,
            "canonical_category" to canonicalCategory,
            "categoryName" to categoryDisplayName,
            "category_name" to categoryDisplayName,
            "categoryShort" to canonicalCategory,
            "gameCategory" to canonicalCategory,
            "game_category" to canonicalCategory,
            "tournamentCategory" to canonicalCategory,
            "tournament_category" to canonicalCategory,
            "categoryKey" to canonicalCategory,
            "category_key" to canonicalCategory,

            // Map
            "map" to canonicalMap,
            "mapName" to canonicalMap,
            "map_name" to canonicalMap,

            // Entry Fee (Float, Double, and String formats)
            "entryFee" to tournament.entryFee.toDouble(),
            "entry_fee" to tournament.entryFee.toDouble(),
            "fee" to tournament.entryFee.toDouble(),
            "entry_fee_str" to if (tournament.entryFee > 0f) "₹${tournament.entryFee.toInt()}" else "FREE",

            // Prize Pool & Distribution
            "prizePool" to tournament.prizePool.toDouble(),
            "prize_pool" to tournament.prizePool.toDouble(),
            "prize" to tournament.prizePool.toDouble(),
            "pool" to tournament.prizePool.toDouble(),
            "prize_pool_str" to "₹${tournament.prizePool.toInt()}",
            "firstPlacePrize" to tournament.firstPlacePrize.toDouble(),
            "first_place_prize" to tournament.firstPlacePrize.toDouble(),
            "firstPrize" to tournament.firstPlacePrize.toDouble(),
            "first_prize" to tournament.firstPlacePrize.toDouble(),
            "secondPlacePrize" to tournament.secondPlacePrize.toDouble(),
            "second_place_prize" to tournament.secondPlacePrize.toDouble(),
            "secondPrize" to tournament.secondPlacePrize.toDouble(),
            "thirdPlacePrize" to tournament.thirdPlacePrize.toDouble(),
            "third_place_prize" to tournament.thirdPlacePrize.toDouble(),
            "thirdPrize" to tournament.thirdPlacePrize.toDouble(),
            "perKillPrize" to tournament.perKillPrize.toDouble(),
            "per_kill_prize" to tournament.perKillPrize.toDouble(),
            "perKill" to tournament.perKillPrize.toDouble(),
            "per_kill" to tournament.perKillPrize.toDouble(),
            "rank1Prize" to tournament.firstPlacePrize.toDouble(),
            "rank_1_prize" to tournament.firstPlacePrize.toDouble(),
            "rank2Prize" to tournament.secondPlacePrize.toDouble(),
            "rank_2_prize" to tournament.secondPlacePrize.toDouble(),
            "rank3Prize" to tournament.thirdPlacePrize.toDouble(),
            "rank_3_prize" to tournament.thirdPlacePrize.toDouble(),
            "killBounty" to tournament.perKillPrize.toDouble(),
            "kill_bounty" to tournament.perKillPrize.toDouble(),

            // Player Slots (Registered & Max)
            "registeredPlayers" to tournament.registeredPlayers,
            "registered_players" to tournament.registeredPlayers,
            "joinedPlayers" to tournament.registeredPlayers,
            "joined_players" to tournament.registeredPlayers,
            "joinedCount" to tournament.registeredPlayers,
            "filledSlots" to tournament.registeredPlayers,
            "filled_slots" to tournament.registeredPlayers,
            "currentParticipants" to tournament.registeredPlayers,
            "slotsBooked" to tournament.registeredPlayers,
            "slots_booked" to tournament.registeredPlayers,
            "current_players" to tournament.registeredPlayers,
            "maxPlayers" to tournament.maxPlayers,
            "max_players" to tournament.maxPlayers,
            "slots" to tournament.maxPlayers,
            "totalSlots" to tournament.maxPlayers,
            "total_slots" to tournament.maxPlayers,
            "maxSlots" to tournament.maxPlayers,
            "availableSlots" to slotsRemaining,
            "available_slots" to slotsRemaining,
            "slotsAvailable" to slotsRemaining,
            "slots_available" to slotsRemaining,
            "remainingSlots" to slotsRemaining,
            "remaining_slots" to slotsRemaining,

            // Format & Match Type (contract compliant uppercase e.g. SOLO, DUO, SQUAD, 4v4, 1v1 and ruleFormat)
            "displayTournamentId" to tournament.displayTournamentId.ifBlank { tournament.id },
            "matchCategory" to tournament.matchCategory.ifBlank {
                when (canonicalCategory) {
                    "CS" -> "CLASH_SQUAD"
                    "LONE_WOLF" -> "LONE_WOLF"
                    "SCRIMS" -> "CUSTOM_ROOM"
                    else -> "BATTLE_ROYALE"
                }
            },
            "matchMode" to tournament.matchMode.ifBlank { "PER_KILL" },
            "customRuleBadge" to tournament.customRuleBadge.ifBlank { "STANDARD RULES" },
            "format" to canonicalFormat.uppercase(),
            "gameMode" to categoryDisplayName,
            "teamType" to ruleFormat,
            "ruleFormat" to ruleFormat,
            "formatUpper" to canonicalFormat.uppercase(),
            "matchType" to canonicalFormat,
            "match_type" to canonicalFormat,
            "type" to canonicalFormat,
            "mapType" to canonicalMap,
            "map_type" to canonicalMap,
            "perspective" to tournament.perspective.ifBlank { if (canonicalFormat.contains("FPP", true)) "FPP" else "TPP" },

            // Status & Visibility Flags (contract compliant UPCOMING, LIVE, COMPLETED, CANCELLED)
            "status" to canonicalStatus.uppercase(),
            "statusLower" to ruleStatus,
            "statusUpper" to canonicalStatus.uppercase(),
            "matchStatus" to canonicalStatus.uppercase(),
            "match_status" to canonicalStatus.uppercase(),
            "state" to canonicalStatus.uppercase(),
            "matchState" to canonicalStatus.uppercase(),
            "tournamentStatus" to canonicalStatus.uppercase(),
            "isActive" to (canonicalStatus.uppercase() != "CANCELLED" && canonicalStatus.uppercase() != "COMPLETED"),
            "is_active" to (canonicalStatus.uppercase() != "CANCELLED" && canonicalStatus.uppercase() != "COMPLETED"),
            "active" to (canonicalStatus.uppercase() != "CANCELLED" && canonicalStatus.uppercase() != "COMPLETED"),
            "published" to true,
            "isPublished" to true,
            "visibility" to "PUBLIC",

            // Schedules (contract field scheduledTime, plus numeric startTime for database rules, formatted string for display)
            "scheduledTime" to parsedStartTime,
            "scheduled_time" to parsedStartTime,
            "startsAt" to formattedScheduleStr,
            "startTime" to formattedScheduleStr,
            "startTimeMillis" to parsedStartTime,
            "start_time" to parsedStartTime,
            "startTimeStr" to formattedScheduleStr,
            "dateTimeStr" to formattedScheduleStr,
            "date_time_str" to formattedScheduleStr,
            "bannerIdx" to 1,
            "banner_idx" to 1,
            "time" to formattedScheduleStr,
            "schedule" to formattedScheduleStr,
            "date" to formattedScheduleStr,
            "endsAt" to (tournament.endsAt ?: ""),
            "endTime" to (tournament.endsAt ?: ""),
            "end_time" to (tournament.endsAt ?: ""),

            // Descriptions & Media (Multi-Key Image Rule: imageUrl, bannerUrl, coverImage)
            "description" to tournament.description,
            "bannerUrl" to tournament.bannerUrl,
            "banner_url" to tournament.bannerUrl,
            "imageUrl" to tournament.bannerUrl,
            "image_url" to tournament.bannerUrl,
            "coverImage" to tournament.bannerUrl,
            "banner" to tournament.bannerUrl,
            "prizeDistribution" to "1st: ${tournament.firstPlacePrize.toInt()} VT | 2nd: ${tournament.secondPlacePrize.toInt()} VT | 3rd: ${tournament.thirdPlacePrize.toInt()} VT",

            // Rules & Match Configs
            "rules" to tournament.rules,
            "rulesText" to tournament.rules.take(2000),
            "allowedGuns" to tournament.allowedGuns,
            "bannedGuns" to tournament.bannedGuns,
            "gunAttributesAllowed" to tournament.gunAttributesAllowed,
            "limitedAmmo" to tournament.limitedAmmo,
            "characterSkillsAllowed" to tournament.characterSkillsAllowed,
            "allowedSkills" to tournament.allowedSkills,
            "bannedSkills" to tournament.bannedSkills,
            "emulatorAllowed" to tournament.emulatorAllowed,
            "roofCampingAllowed" to tournament.roofCampingAllowed,
            "airdropAllowed" to tournament.airdropAllowed,
            "vehiclesAllowed" to tournament.vehiclesAllowed,
            "loadoutAllowed" to tournament.loadoutAllowed,
            "allowedCharacters" to tournament.allowedCharacters,
            "bannedCharacters" to tournament.bannedCharacters,
            "headshotOnly" to tournament.headshotOnly,
            "revivalAllowed" to tournament.revivalAllowed,
            "fallDamage" to tournament.fallDamage,
            "safeZoneShrinkSpeed" to tournament.safeZoneShrinkSpeed,
            "customMatchSettings" to tournament.customMatchSettings,
            "rulesModifiedAt" to tournament.rulesModifiedAt,
            "rulesModifiedBy" to tournament.rulesModifiedBy,
            "cancellationReason" to tournament.cancellationReason,
            "cancelledAt" to tournament.cancelledAt,
            "isCompleted" to (tournament.isCompleted || canonicalStatus.equals("COMPLETED", ignoreCase = true)),
            "payoutStatus" to tournament.payoutStatus,
            "completedAt" to tournament.completedAt,
            "totalPrizeDisbursed" to tournament.totalPrizeDisbursed,
            "winnerUid" to tournament.winnerUid,
            "winnerName" to tournament.winnerName,
            "winnerPrize" to tournament.winnerPrize,

            // Room Credentials
            "roomDetails" to roomMap,
            "roomId" to (tournament.roomDetails?.roomId ?: ""),
            "room_id" to (tournament.roomDetails?.roomId ?: ""),
            "roomPassword" to (tournament.roomDetails?.roomPassword ?: ""),
            "room_password" to (tournament.roomDetails?.roomPassword ?: ""),

            // Production Audits & Metadata
            "createdAt" to createdAt,
            "created_at" to createdAt,
            "updatedAt" to now,
            "updated_at" to now,
            "timestamp" to now,
            "platform" to "MOBILE",
            "server" to "ASIA"
        )
    }

    private fun getSyncRtdbPaths(tournamentId: String, gameName: String, categoryName: String = "BR"): List<String> {
        val canonicalGame = gameName.ifBlank { "Free Fire" }
        val cat = when {
            categoryName.equals("CS", true) || categoryName.contains("CLASH", true) -> "CS"
            categoryName.equals("LONE_WOLF", true) || categoryName.contains("LONE", true) -> "LONE_WOLF"
            categoryName.equals("SCRIMS", true) || categoryName.contains("SCRIM", true) -> "SCRIMS"
            else -> "BR"
        }
        val paths = mutableListOf(
            "tournaments/$tournamentId",
            "Tournaments/$tournamentId",
            "matches/$tournamentId",
            "Matches/$tournamentId",
            "active_tournaments/$tournamentId",
            "all_tournaments/$tournamentId",
            "published_matches/$tournamentId",
            "games/$canonicalGame/tournaments/$tournamentId",
            "games/$canonicalGame/matches/$tournamentId",
            "game_tournaments/$canonicalGame/$tournamentId",
            // Direct category root trees for User Panel category tab synchronization
            "categories/$cat/$tournamentId",
            "categories/${cat.lowercase()}/$tournamentId",
            "tournaments_by_category/$cat/$tournamentId",
            "tournaments_by_category/${cat.lowercase()}/$tournamentId",
            "matches_by_category/$cat/$tournamentId",
            "matches_by_category/${cat.lowercase()}/$tournamentId",
            "FreeFire/categories/$cat/$tournamentId",
            "Free Fire/categories/$cat/$tournamentId",
            "games/$canonicalGame/categories/$cat/$tournamentId",
            "games/$canonicalGame/categories/${cat.lowercase()}/$tournamentId"
        )
        if (canonicalGame.contains("Free Fire", ignoreCase = true) || canonicalGame.contains("FreeFire", ignoreCase = true)) {
            paths.add("FreeFire/matches/$tournamentId")
            paths.add("Free Fire/matches/$tournamentId")
            paths.add("FreeFire/tournaments/$tournamentId")
            paths.add("Free Fire/tournaments/$tournamentId")
            paths.add("FREE FIRE/matches/$tournamentId")
            paths.add("FREE FIRE/tournaments/$tournamentId")
            paths.add("FreeFire/$cat/$tournamentId")
            paths.add("FreeFire/${cat.lowercase()}/$tournamentId")
            paths.add("Free Fire/$cat/$tournamentId")
        } else if (canonicalGame.contains("BGMI", ignoreCase = true)) {
            paths.add("BGMI/matches/$tournamentId")
            paths.add("BGMI/tournaments/$tournamentId")
            paths.add("bgmi/matches/$tournamentId")
            paths.add("BGMI/categories/$cat/$tournamentId")
        }
        return paths
    }

    /**
     * Realtime Stream Listener for a specific tournament on shared Firestore path & RTDB
     * Guarantees instantaneous updates for Prize Pool, Price Distribution, Registration Numbers, Status, and Rules.
     */
    fun getTournamentLiveStream(tournamentId: String): Flow<Tournament?> = callbackFlow {
        var latestTournament: Tournament? = null

        val emitLatest = {
            latestTournament?.let { trySend(it) }
        }

        val rtdbNodes = listOf(
            database.child("tournaments").child(tournamentId),
            database.child("Tournaments").child(tournamentId),
            database.child("matches").child(tournamentId),
            database.child("Matches").child(tournamentId),
            database.child("active_tournaments").child(tournamentId)
        )

        val rtdbListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val t = snapshot.toTournament()
                    if (t != null) {
                        latestTournament = t
                        emitLatest()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "RTDB tournament stream cancelled: ${error.message}")
            }
        }
        rtdbNodes.forEach {
            it.get().addOnSuccessListener { snap ->
                if (snap.exists()) {
                    snap.toTournament()?.let { t ->
                        latestTournament = t
                        emitLatest()
                    }
                }
            }
            it.addValueEventListener(rtdbListener)
        }

        // Shared Firestore document listeners
        val fsCollections = listOf("tournaments", "Tournaments", "matches", "Matches", "active_tournaments")
        val fsListeners = fsCollections.mapNotNull { col ->
            try {
                firestore.collection(col).document(tournamentId).addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null && snapshot.exists()) {
                        val t = snapshot.toTournament()
                        if (t != null) {
                            latestTournament = t
                            emitLatest()
                        }
                    }
                }
            } catch (_: Exception) {
                null
            }
        }

        awaitClose {
            fsListeners.forEach { it.remove() }
            rtdbNodes.forEach { it.removeEventListener(rtdbListener) }
        }
    }

    suspend fun createTournament(
        tournament: Tournament,
        adminEmail: String? = null,
        adminUid: String? = null
    ) {
        val effectiveEmail = adminEmail ?: auth.currentUser?.email ?: activeAdminEmail
        val effectiveUid = adminUid ?: auth.currentUser?.uid ?: activeAdminUid

        // 1. Immediately record in local created cache and remove from deleted cache so it shows up without delay
        locallyCreatedTournaments[tournament.id] = tournament
        unmarkTournamentDeleted(tournament.id)

        // 2. Ensure current admin session is authenticated and admin privileges are written
        try {
            ensureAuthenticatedSession(effectiveEmail)
        } catch (_: Exception) {}
        if (effectiveUid.isNotBlank()) {
            try {
                verifyAndRegisterAdmin(effectiveUid, effectiveEmail, effectiveEmail.substringBefore("@"))
            } catch (_: Exception) {}
        }

        val authValidation = TournamentBackendValidator.validateAdminPermission(
            effectiveEmail,
            effectiveUid,
            "create tournament"
        )
        if (!authValidation.isValid) {
            val reason = (authValidation as ValidationResult.Invalid).reason
            GlobalErrorManager.emitError(reason)
            return
        }

        val validation = TournamentBackendValidator.validateTournament(tournament)
        if (!validation.isValid) {
            val reason = (validation as ValidationResult.Invalid).reason
            GlobalErrorManager.emitError("Validation error: $reason")
            return
        }

        var firestoreSaved = false
        var lastError: Exception? = null

        val map = buildTournamentMap(tournament)

        // Primary write target: Firestore tournaments/{id} (Cloud Functions automatically mirror RTDB & global_app_state)
        try {
            firestore.collection("tournaments").document(tournament.id).set(map).await()
            firestoreSaved = true
        } catch (e: Exception) {
            lastError = e
            Log.w(TAG, "Failed writing tournament to Firestore tournaments/${tournament.id}: ${e.message}")
        }

        if (firestoreSaved) {
            try {
                dispatchAutoTournamentCampaign(tournament)
            } catch (e: Exception) {
                Log.w(TAG, "Failed auto-dispatching campaign: ${e.message}")
            }
            GlobalErrorManager.emitSuccess("Tournament \"${tournament.title}\" saved to Firestore & syncing live across all apps!")
        } else {
            GlobalErrorManager.emitError("Failed to create tournament: ${lastError?.message ?: "Unknown error"}", lastError)
        }
    }

    suspend fun updateTournament(
        tournament: Tournament,
        adminEmail: String? = null,
        adminUid: String? = null
    ) {
        val effectiveEmail = adminEmail ?: auth.currentUser?.email ?: activeAdminEmail
        try {
            ensureAuthenticatedSession(effectiveEmail)
        } catch (_: Exception) {}
        val authValidation = TournamentBackendValidator.validateAdminPermission(
            effectiveEmail,
            adminUid ?: auth.currentUser?.uid ?: activeAdminUid,
            "modify tournament details and rules"
        )
        if (!authValidation.isValid) {
            val reason = (authValidation as ValidationResult.Invalid).reason
            GlobalErrorManager.emitError(reason)
            return
        }

        val validation = TournamentBackendValidator.validateTournament(tournament)
        if (!validation.isValid) {
            val reason = (validation as ValidationResult.Invalid).reason
            GlobalErrorManager.emitError("Validation error: $reason")
            return
        }

        var firestoreSaved = false
        var lastError: Exception? = null

        val map = buildTournamentMap(tournament)

        // Primary write target: Firestore tournaments/{id} (Cloud Functions automatically mirror RTDB & global_app_state)
        try {
            firestore.collection("tournaments").document(tournament.id).set(map, SetOptions.merge()).await()
            firestoreSaved = true
        } catch (e: Exception) {
            lastError = e
            Log.w(TAG, "Failed updating tournament in Firestore tournaments/${tournament.id}: ${e.message}")
        }

        if (firestoreSaved) {
            GlobalErrorManager.emitSuccess("Tournament \"${tournament.title}\" updated in Firestore & syncing live!")
            
            // Dispatch live / completed / rules notification to all users
            context?.let { ctx ->
                if (tournament.status.equals("LIVE", ignoreCase = true)) {
                    val notif = VelorixNotificationManager.dispatchTournamentLiveNotification(ctx, tournament)
                    syncNotificationToCloud(notif)
                } else if (tournament.status.equals("COMPLETED", ignoreCase = true)) {
                    val notif = VelorixNotificationManager.dispatchTournamentCompletedNotification(ctx, tournament)
                    syncNotificationToCloud(notif)
                } else {
                    val notif = VelorixNotificationManager.dispatchTournamentRulesUpdatedNotification(ctx, tournament)
                    syncNotificationToCloud(notif)
                }
            }
        } else {
            GlobalErrorManager.emitError("Failed to update tournament: ${lastError?.message ?: "Unknown error"}", lastError)
        }
    }

    suspend fun updateTournamentRules(
        tournament: Tournament,
        adminEmail: String? = null,
        adminUid: String? = null
    ) {
        updateTournament(tournament, adminEmail, adminUid)
    }

    suspend fun cancelTournament(
        tournamentId: String,
        reason: String,
        adminEmail: String? = null,
        adminUid: String? = null
    ) {
        val authValidation = TournamentBackendValidator.validateAdminPermission(
            adminEmail ?: auth.currentUser?.email,
            adminUid ?: auth.currentUser?.uid,
            "cancel tournament"
        )
        if (!authValidation.isValid) {
            val err = (authValidation as ValidationResult.Invalid).reason
            GlobalErrorManager.emitError(err)
            return
        }

        val cancelMap = mapOf<String, Any>(
            "status" to "CANCELLED",
            "matchStatus" to "CANCELLED",
            "match_status" to "CANCELLED",
            "state" to "CANCELLED",
            "isActive" to false,
            "is_active" to false,
            "cancellationReason" to reason,
            "cancelledAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )
        // Primary write target: Firestore tournaments/{id} (Cloud Functions mirror RTDB & global_app_state)
        try {
            firestore.collection("tournaments").document(tournamentId).set(cancelMap, SetOptions.merge()).await()
            GlobalErrorManager.emitSuccess("Tournament cancelled: $reason")
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to cancel tournament: ${e.message}", e)
        }
    }

    suspend fun purgeOrphanAndDeletedTournamentsFromCloud() {
        if (locallyDeletedTournamentIds.isEmpty()) return
        val rtdbScanNodes = listOf(
            "tournaments", "Tournaments", "tournament", "Tournament",
            "matches", "Matches", "match", "Match",
            "active_tournaments", "all_tournaments", "published_matches",
            "custom_rooms", "tournaments_by_category", "matches_by_category",
            "FreeFire/tournaments", "FreeFire/matches", "Free Fire/tournaments", "Free Fire/matches",
            "BGMI/tournaments", "BGMI/matches", "game_tournaments"
        )
        for (node in rtdbScanNodes) {
            try {
                val snap = database.child(node).get().await()
                snap.children.forEach { child ->
                    val cKey = child.key ?: ""
                    val cId = child.child("id").value?.toString()
                        ?: child.child("tournamentId").value?.toString()
                        ?: child.child("matchId").value?.toString()
                        ?: cKey
                    if (locallyDeletedTournamentIds.contains(cId) || locallyDeletedTournamentIds.contains(cKey)) {
                        try { child.ref.removeValue().await() } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}
        }

        val fsScanCollections = listOf(
            "tournaments", "Tournaments", "matches", "Matches",
            "active_tournaments", "all_tournaments", "published_matches", "custom_rooms"
        )
        for (col in fsScanCollections) {
            try {
                val snap = firestore.collection(col).get().await()
                for (doc in snap.documents) {
                    val dId = doc.getString("id") ?: doc.getString("tournamentId") ?: doc.id
                    if (locallyDeletedTournamentIds.contains(dId) || locallyDeletedTournamentIds.contains(doc.id)) {
                        try { doc.reference.delete().await() } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}
        }
    }

    suspend fun deleteTournament(
        tournamentId: String,
        adminEmail: String? = null,
        adminUid: String? = null
    ) {
        markTournamentDeleted(tournamentId)
        val effectiveEmail = adminEmail?.takeIf { it.isNotBlank() } ?: auth.currentUser?.email ?: activeAdminEmail
        try {
            ensureAuthenticatedSession(effectiveEmail)
        } catch (_: Exception) {}
        val effectiveUid = adminUid?.takeIf { it.isNotBlank() } ?: auth.currentUser?.uid ?: activeAdminUid

        val authValidation = TournamentBackendValidator.validateAdminPermission(
            effectiveEmail,
            effectiveUid,
            "delete tournament"
        )
        if (!authValidation.isValid) {
            val err = (authValidation as ValidationResult.Invalid).reason
            GlobalErrorManager.emitError(err)
            return
        }

        var deleted = false
        var lastError: Exception? = null

        // Primary delete target: Firestore tournaments/{id} (Cloud Functions automatically clean RTDB mirrors & update global_app_state)
        try {
            firestore.collection("tournaments").document(tournamentId).delete().await()
            deleted = true
        } catch (e: Exception) {
            lastError = e
            Log.w(TAG, "Failed deleting Firestore tournaments/$tournamentId: ${e.message}")
        }

        // Clean any legacy secondary Firestore copies if present
        for (col in listOf("matches", "active_tournaments", "all_tournaments")) {
            try {
                firestore.collection(col).document(tournamentId).delete().await()
            } catch (_: Exception) {}
        }

        if (deleted) {
            GlobalErrorManager.emitSuccess("Tournament permanently deleted from Firestore & syncing removal across apps!")
        } else {
            GlobalErrorManager.emitError("Failed to delete tournament: ${lastError?.message ?: "Unknown error"}", lastError)
        }
    }

    suspend fun exportAllTournamentsJson(): String {
        return try {
            val list = getAvailableTournaments()
            val jsonArray = JSONArray()
            for (t in list) {
                val obj = JSONObject(buildTournamentMap(t))
                jsonArray.put(obj)
            }
            jsonArray.toString(2)
        } catch (e: Exception) {
            "[]"
        }
    }

    suspend fun importTournamentsFromJson(jsonStr: String): Pair<Int, String> {
        var successCount = 0
        return try {
            val trimmed = jsonStr.trim()
            if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val t = parseJsonToTournament(obj)
                    if (t != null) {
                        createTournament(t)
                        successCount++
                    }
                }
            } else if (trimmed.startsWith("{")) {
                val obj = JSONObject(trimmed)
                val t = parseJsonToTournament(obj)
                if (t != null) {
                    createTournament(t)
                    successCount++
                }
            }
            Pair(successCount, "Successfully extracted & pushed $successCount tournaments!")
        } catch (e: Exception) {
            Pair(successCount, "JSON Import parsing error: ${e.message}")
        }
    }

    private fun parseJsonToTournament(obj: JSONObject): Tournament? {
        val tId = if (obj.has("id")) obj.optString("id") 
            else if (obj.has("tournamentId")) obj.optString("tournamentId") 
            else if (obj.has("tournament_id")) obj.optString("tournament_id")
            else "TRN_${System.currentTimeMillis()}"

        val title = if (obj.has("title")) obj.optString("title") 
            else if (obj.has("name")) obj.optString("name")
            else if (obj.has("tournamentName")) obj.optString("tournamentName")
            else "Esports Match"

        val game = if (obj.has("game")) obj.optString("game") else "Free Fire"
        val mapName = if (obj.has("map")) obj.optString("map") else "Bermuda"
        val entryFee = obj.optDouble("entryFee", obj.optDouble("entry_fee", obj.optDouble("fee", 0.0))).toFloat()
        val prizePool = obj.optDouble("prizePool", obj.optDouble("prize_pool", obj.optDouble("prize", 1000.0))).toFloat()
        val maxPlayers = obj.optInt("maxPlayers", obj.optInt("max_players", obj.optInt("slots", 48)))
        val format = if (obj.has("format")) obj.optString("format") else "SOLO"
        val status = if (obj.has("status")) obj.optString("status") else "UPCOMING"
        val startsAt = obj.optString("startsAt", obj.optString("startTime", ""))
        val bannerUrl = obj.optString("bannerUrl", obj.optString("banner_url", ""))
        val description = obj.optString("description", "Velorix Esports Match")
        val rules = obj.optString("rules", "Standard tournament rules apply.")

        val rawCategory = obj.optString("category", obj.optString("canonicalCategory", obj.optString("gameCategory", "")))
        val category = when {
            rawCategory.equals("CS", ignoreCase = true) || rawCategory.contains("CLASH", ignoreCase = true) || format.contains("CS", ignoreCase = true) || title.contains("CS", ignoreCase = true) || title.contains("Clash Squad", ignoreCase = true) -> "CS"
            rawCategory.equals("LONE_WOLF", ignoreCase = true) || rawCategory.contains("LONE", ignoreCase = true) || format.contains("Lone", ignoreCase = true) || title.contains("Lone Wolf", ignoreCase = true) || mapName.contains("Iron Cage", ignoreCase = true) -> "LONE_WOLF"
            rawCategory.equals("SCRIMS", ignoreCase = true) || rawCategory.contains("SCRIM", ignoreCase = true) || rawCategory.contains("TRAINING", ignoreCase = true) || title.contains("Scrim", ignoreCase = true) || title.contains("Training", ignoreCase = true) -> "SCRIMS"
            else -> "BR"
        }

        return Tournament(
            id = tId,
            title = title,
            game = game,
            category = category,
            map = mapName,
            entryFee = entryFee,
            prizePool = prizePool,
            registeredPlayers = obj.optInt("registeredPlayers", 0),
            maxPlayers = maxPlayers,
            format = format,
            status = status,
            startsAt = if (startsAt.isNotBlank()) startsAt else null,
            endsAt = obj.optString("endsAt", "").ifBlank { null },
            description = description,
            bannerUrl = bannerUrl,
            rules = rules,
            allowedGuns = obj.optString("allowedGuns", "All Standard Weapons Allowed"),
            bannedGuns = obj.optString("bannedGuns", "M79, M82B, Crossbow"),
            gunAttributesAllowed = obj.optBoolean("gunAttributesAllowed", false),
            limitedAmmo = obj.optBoolean("limitedAmmo", true),
            characterSkillsAllowed = obj.optBoolean("characterSkillsAllowed", true),
            allowedSkills = obj.optString("allowedSkills", "All Active & Passive Skills"),
            bannedSkills = obj.optString("bannedSkills", "None"),
            emulatorAllowed = obj.optBoolean("emulatorAllowed", false),
            firstPlacePrize = obj.optDouble("firstPlacePrize", prizePool * 0.5).toFloat(),
            secondPlacePrize = obj.optDouble("secondPlacePrize", prizePool * 0.25).toFloat(),
            thirdPlacePrize = obj.optDouble("thirdPlacePrize", prizePool * 0.15).toFloat(),
            perKillPrize = obj.optDouble("perKillPrize", 20.0).toFloat()
        )
    }

    suspend fun syncAllTournamentsToCloud(): Pair<Int, String> {
        return try {
            val list = getAvailableTournaments()
            var pushed = 0
            for (t in list) {
                val map = buildTournamentMap(t)
                val paths = getSyncRtdbPaths(t.id, t.game, t.canonicalCategory)
                for (path in paths) {
                    try { database.child(path).setValue(map).await() } catch (_: Exception) {}
                }
                val fsCollections = listOf(
                    "tournaments", "matches", "active_tournaments", "all_tournaments",
                    "categories/${t.canonicalCategory}/matches",
                    "categories/${t.canonicalCategory.lowercase()}/matches",
                    "categories/${t.canonicalCategory}/tournaments",
                    "categories/${t.canonicalCategory.lowercase()}/tournaments",
                    "tournaments_by_category/${t.canonicalCategory}/items",
                    "tournaments_by_category/${t.canonicalCategory.lowercase()}/items"
                )
                for (col in fsCollections) {
                    try { firestore.collection(col).document(t.id).set(map).await() } catch (_: Exception) {}
                }
                pushed++
            }
            GlobalErrorManager.emitSuccess("Synced $pushed tournaments with all categories live!")
            Pair(pushed, "Synced $pushed tournaments successfully across all endpoints & categories.")
        } catch (e: Exception) {
            Pair(0, "Sync error: ${e.message}")
        }
    }

    suspend fun getMatches(tournamentId: String): List<Match> {
        return try {
            val rtdbSnap = database.child("matches").child(tournamentId).get().await()
            val list = rtdbSnap.children.mapNotNull { child ->
                if (!child.hasChildren() || child.value !is Map<*, *>) null
                else child.safeGetObject<Match>()
            }
            if (list.isNotEmpty()) list else {
                val snapshot = firestore.collection("matches")
                    .whereEqualTo("tournament_id", tournamentId)
                    .get().await()
                snapshot.documents.mapNotNull { it.toObject(Match::class.java) }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateMatch(match: Match) {
        try {
            firestore.collection("tournaments").document(match.tournamentId)
                .collection("matches").document(match.id).set(match).await()
            try {
                firestore.collection("matches").document(match.id).set(match).await()
            } catch (_: Exception) {}
            try {
                database.child("matches").child(match.tournamentId).child(match.id).setValue(match).await()
            } catch (_: Exception) {}
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun advancePlayerInBracket(
        tournamentId: String,
        sourceMatchId: String?,
        targetMatchId: String,
        playerId: String,
        targetSlot: Int
    ) {
        try {
            if (!sourceMatchId.isNullOrBlank()) {
                val sourceSnap = database.child("matches").child(tournamentId).child(sourceMatchId).get().await()
                val sourceMatch = sourceSnap.safeGetObject<Match>()
                if (sourceMatch != null) {
                    val updatedSource = sourceMatch.copy(
                        winnerId = playerId,
                        status = "COMPLETED"
                    )
                    updateMatch(updatedSource)
                }
            }

            val targetSnap = database.child("matches").child(tournamentId).child(targetMatchId).get().await()
            val targetMatch = targetSnap.safeGetObject<Match>() ?: Match(
                id = targetMatchId,
                tournamentId = tournamentId,
                status = "SCHEDULED"
            )
            val updatedTarget = if (targetSlot == 1) {
                targetMatch.copy(
                    player1Id = playerId,
                    status = if (targetMatch.player2Id != null) "READY" else "SCHEDULED"
                )
            } else {
                targetMatch.copy(
                    player2Id = playerId,
                    status = if (targetMatch.player1Id != null) "READY" else "SCHEDULED"
                )
            }
            updateMatch(updatedTarget)
            GlobalErrorManager.emitSuccess("Advancement saved: $playerId placed in Match $targetMatchId (Slot $targetSlot)")
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to advance player: ${e.message}", e)
        }
    }

    suspend fun getLiveMatchesStream(tournamentId: String): Flow<List<Match>> = callbackFlow {
        val matchesMap = mutableMapOf<String, Match>()

        fun emitCombined() {
            val list = matchesMap.values.toList().sortedWith(compareBy({ it.round }, { it.matchNumber }))
            trySend(list)
        }

        // 1. Observe Realtime Database (guarded against primitive/Boolean nodes)
        val rtdbListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    if (!child.hasChildren() || child.value !is Map<*, *>) continue
                    val m = child.safeGetObject<Match>()
                    if (m != null && m.id.isNotBlank()) {
                        matchesMap[m.id] = m
                    }
                }
                emitCombined()
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "RTDB matches stream error: ${error.message}")
            }
        }
        val ref = database.child("matches").child(tournamentId)
        ref.addValueEventListener(rtdbListener)

        // 2. Observe Cloud Firestore: tournaments/{tournamentId}/matches subcollection
        val fsSubListener = try {
            firestore.collection("tournaments").document(tournamentId).collection("matches")
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null) {
                        snapshot.documents.forEach { doc ->
                            val m = doc.toObject(Match::class.java)?.copy(id = doc.id)
                            if (m != null && m.id.isNotBlank()) {
                                matchesMap[m.id] = m
                            }
                        }
                        emitCombined()
                    }
                }
        } catch (_: Exception) { null }

        // 3. Observe Cloud Firestore: root matches collection where tournamentId == tournamentId
        val fsRootListener = try {
            firestore.collection("matches")
                .whereEqualTo("tournamentId", tournamentId)
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null) {
                        snapshot.documents.forEach { doc ->
                            val m = doc.toObject(Match::class.java)?.copy(id = doc.id)
                            if (m != null && m.id.isNotBlank()) {
                                matchesMap[m.id] = m
                            }
                        }
                        emitCombined()
                    }
                }
        } catch (_: Exception) { null }

        awaitClose {
            ref.removeEventListener(rtdbListener)
            fsSubListener?.remove()
            fsRootListener?.remove()
        }
    }

    suspend fun getLiveLeaderboardStream(tournamentId: String): Flow<List<LeaderboardEntry>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val entries = snapshot.children.mapNotNull { child ->
                    if (!child.hasChildren() || child.value !is Map<*, *>) null
                    else child.safeGetObject<LeaderboardEntry>()
                }.sortedByDescending { it.score }
                trySend(entries)
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        val ref = database.child("leaderboard").child(tournamentId)
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun getLiveSupportTicketsStream(): Flow<List<SupportTicket>> = callbackFlow {
        val ticketsMap = mutableMapOf<String, SupportTicket>()

        val isMockTicket: (SupportTicket) -> Boolean = { t ->
            val e = t.userEmail.trim().lowercase()
            val id = t.id.trim().lowercase()
            val u = t.username.trim().lowercase()
            e.contains("@example.com") || e.contains("mock.dummy") || e.contains("test@") || e.contains("demo@") ||
            id.startsWith("mock_") || id.startsWith("demo_") || id.startsWith("test_") ||
            u.contains("mock user") || u.contains("demo user") || u.contains("test player")
        }

        fun emitCombined() {
            val list = ticketsMap.values.filter { !isMockTicket(it) }.sortedByDescending { it.updatedAt }
            trySend(list)
        }

        emitCombined()

        database.child("support_tickets").get().addOnSuccessListener { snapshot ->
            snapshot.children.mapNotNull { it.toSupportTicket() }.forEach { if (it.id.isNotBlank()) ticketsMap[it.id] = it }
            emitCombined()
        }
        database.child("complaints").get().addOnSuccessListener { snapshot ->
            snapshot.children.mapNotNull { it.toSupportTicket() }.forEach { if (it.id.isNotBlank()) ticketsMap[it.id] = it }
            emitCombined()
        }
        database.child("reports").get().addOnSuccessListener { snapshot ->
            snapshot.children.mapNotNull { it.toSupportTicket() }.forEach { if (it.id.isNotBlank()) ticketsMap[it.id] = it }
            emitCombined()
        }

        val rtdbListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.mapNotNull { it.toSupportTicket() }.forEach { ticket ->
                    if (ticket.id.isNotBlank()) ticketsMap[ticket.id] = ticket
                }
                emitCombined()
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        val rtdbRef = database.child("support_tickets")
        rtdbRef.addValueEventListener(rtdbListener)

        val legacyComplaintsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.mapNotNull { it.toSupportTicket() }.forEach { ticket ->
                    if (ticket.id.isNotBlank()) ticketsMap[ticket.id] = ticket
                }
                emitCombined()
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        val legacyRef = database.child("complaints")
        legacyRef.addValueEventListener(legacyComplaintsListener)

        val reportsRef = database.child("reports")
        reportsRef.addValueEventListener(legacyComplaintsListener)

        val problemRef = database.child("problem_reports")
        problemRef.addValueEventListener(legacyComplaintsListener)

        val fsTicketsListener = try {
            firestore.collection("support_tickets").addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    snapshot.documents.mapNotNull { it.toSupportTicket() }.forEach { ticket ->
                        if (ticket.id.isNotBlank()) ticketsMap[ticket.id] = ticket
                    }
                    emitCombined()
                }
            }
        } catch (_: Exception) { null }

        val fsProblemListener = try {
            firestore.collection("problem_reports").addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    snapshot.documents.mapNotNull { it.toSupportTicket() }.forEach { ticket ->
                        if (ticket.id.isNotBlank()) ticketsMap[ticket.id] = ticket
                    }
                    emitCombined()
                }
            }
        } catch (_: Exception) { null }

        val fsComplaintsListener = try {
            firestore.collection("complaints").addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    snapshot.documents.mapNotNull { it.toSupportTicket() }.forEach { ticket ->
                        if (ticket.id.isNotBlank()) ticketsMap[ticket.id] = ticket
                    }
                    emitCombined()
                }
            }
        } catch (_: Exception) { null }

        val fsReportsListener = try {
            firestore.collection("reports").addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    snapshot.documents.mapNotNull { it.toSupportTicket() }.forEach { ticket ->
                        if (ticket.id.isNotBlank()) ticketsMap[ticket.id] = ticket
                    }
                    emitCombined()
                }
            }
        } catch (_: Exception) { null }

        awaitClose {
            rtdbRef.removeEventListener(rtdbListener)
            legacyRef.removeEventListener(legacyComplaintsListener)
            reportsRef.removeEventListener(legacyComplaintsListener)
            problemRef.removeEventListener(legacyComplaintsListener)
            fsTicketsListener?.remove()
            fsProblemListener?.remove()
            fsComplaintsListener?.remove()
            fsReportsListener?.remove()
        }
    }

    suspend fun updateSupportTicket(ticket: SupportTicket) {
        try {
            val now = System.currentTimeMillis()
            ticket.updatedAt = now
            val userId = ticket.userId.ifBlank { if (ticket.userEmail.isNotBlank()) "USR_${ticket.userEmail.hashCode()}" else ticket.id }
            val replyText = ticket.adminNote.ifBlank { ticket.adminReply }
            val payload = mapOf(
                "id" to ticket.id,
                "ticketId" to ticket.id,
                "userId" to userId,
                "userEmail" to ticket.userEmail,
                "user_email" to ticket.userEmail,
                "email" to ticket.userEmail,
                "userName" to ticket.username,
                "username" to ticket.username,
                "freeFireId" to ticket.gameId,
                "gameId" to ticket.gameId,
                "subject" to ticket.tournamentTitle.ifBlank { ticket.subject.ifBlank { "Support Ticket" } },
                "tournamentTitle" to ticket.tournamentTitle.ifBlank { ticket.subject.ifBlank { "Support Ticket" } },
                "category" to ticket.issueCategory,
                "issueCategory" to ticket.issueCategory,
                "description" to ticket.description,
                "priority" to if (ticket.isHighPriority) "High" else "Medium",
                "isHighPriority" to ticket.isHighPriority,
                "status" to ticket.status,
                "createdAt" to ticket.createdAt,
                "updatedAt" to now,
                "repliedAt" to now,
                "adminNote" to replyText,
                "admin_note" to replyText,
                "adminReply" to replyText,
                "assignedTo" to ticket.assignedTo
            )
            // Primary write target: Firestore support_tickets & companion ticket collections (Cloud Functions mirror RTDB & global_app_state)
            firestore.collection("support_tickets").document(ticket.id).set(payload, SetOptions.merge()).await()
            try { firestore.collection("reports").document(ticket.id).set(payload, SetOptions.merge()).await() } catch (_: Exception) {}
            try { firestore.collection("problem_reports").document(ticket.id).set(payload, SetOptions.merge()).await() } catch (_: Exception) {}
            try { firestore.collection("complaints").document(ticket.id).set(payload, SetOptions.merge()).await() } catch (_: Exception) {}
            try { firestore.collection("admin_channels").document("support_tickets").collection("items").document(ticket.id).set(payload, SetOptions.merge()).await() } catch (_: Exception) {}

            if (userId.isNotBlank() && replyText.isNotBlank()) {
                val notifId = "notif_sup_${now}"
                val notifMap = mapOf(
                    "id" to notifId,
                    "userId" to userId,
                    "title" to "Support Ticket Update (#${ticket.id.takeLast(6)})",
                    "message" to "Admin replied: $replyText (Status: ${ticket.status})",
                    "type" to "SUPPORT",
                    "timestamp" to now,
                    "isRead" to false
                )
                try { firestore.collection("users").document(userId).collection("notifications").document(notifId).set(notifMap).await() } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
            GlobalErrorManager.emitError("Failed to update ticket: ${e.message}")
        }
    }

    suspend fun deleteSupportTicket(ticketId: String) {
        try {
            // Primary delete target: Firestore (Cloud Functions handle RTDB cleanup)
            firestore.collection("support_tickets").document(ticketId).delete().await()
            try { firestore.collection("problem_reports").document(ticketId).delete().await() } catch (_: Exception) {}
            try { firestore.collection("complaints").document(ticketId).delete().await() } catch (_: Exception) {}
            try { firestore.collection("reports").document(ticketId).delete().await() } catch (_: Exception) {}
        } catch (e: Exception) {
            e.printStackTrace()
            GlobalErrorManager.emitError("Failed to delete ticket: ${e.message}")
        }
    }

    suspend fun getLiveTicketMessagesStream(ticketId: String): Flow<List<TicketMessage>> = callbackFlow {
        val messagesMap = mutableMapOf<String, TicketMessage>()

        fun emitMessages() {
            val list = messagesMap.values.toList().sortedBy { it.timestamp }
            trySend(list)
        }

        emitMessages()

        // 1. Firestore real-time listener for support_tickets/{ticketId}/messages
        var fsListener: com.google.firebase.firestore.ListenerRegistration? = null
        try {
            fsListener = firestore.collection("support_tickets")
                .document(ticketId)
                .collection("messages")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null) {
                        snapshot.documents.forEach { doc ->
                            val msg = doc.toObject(TicketMessage::class.java)?.copy(id = doc.id)
                                ?: TicketMessage(
                                    id = doc.id,
                                    ticketId = doc.getString("ticketId") ?: ticketId,
                                    senderId = doc.getString("senderId") ?: "",
                                    senderName = doc.getString("senderName") ?: "Support",
                                    senderRole = doc.getString("senderRole") ?: "ADMIN",
                                    message = doc.getString("message") ?: "",
                                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                )
                            if (msg.id.isNotBlank() && msg.message.isNotBlank()) {
                                messagesMap[msg.id] = msg
                            }
                        }
                        emitMessages()
                    }
                }
        } catch (_: Exception) {}

        // 2. Realtime Database listener for real-time fallback/sync (guarded against primitive/Boolean nodes)
        val rtdbRef = database.child("support_tickets").child(ticketId).child("messages")
        val rtdbListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    if (!child.hasChildren() || child.value !is Map<*, *>) continue
                    val msg = child.safeGetObject<TicketMessage>()?.copy(id = child.key ?: "")
                        ?: TicketMessage(
                            id = child.key ?: "",
                            ticketId = child.child("ticketId").value?.toString() ?: ticketId,
                            senderId = child.child("senderId").value?.toString() ?: "",
                            senderName = child.child("senderName").value?.toString() ?: "Support",
                            senderRole = child.child("senderRole").value?.toString() ?: "ADMIN",
                            message = child.child("message").value?.toString() ?: "",
                            timestamp = child.child("timestamp").value?.toString()?.toLongOrNull() ?: System.currentTimeMillis()
                        )
                    if (msg.id.isNotBlank() && msg.message.isNotBlank()) {
                        messagesMap[msg.id] = msg
                    }
                }
                emitMessages()
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        rtdbRef.addValueEventListener(rtdbListener)

        awaitClose {
            fsListener?.remove()
            rtdbRef.removeEventListener(rtdbListener)
        }
    }

    suspend fun sendTicketMessage(
        ticketId: String,
        senderId: String,
        senderName: String,
        senderRole: String,
        messageText: String
    ): Boolean {
        val cleanMsg = com.example.data.validation.SecuritySanitizer.sanitizeInput(messageText, maxLength = 2000)
        if (cleanMsg.isBlank() || ticketId.isBlank()) return false

        val now = System.currentTimeMillis()
        val msgId = "MSG_${now}_${java.util.UUID.randomUUID().toString().take(6)}"

        val payload = mapOf(
            "id" to msgId,
            "ticketId" to ticketId,
            "senderId" to senderId,
            "senderName" to senderName,
            "senderRole" to senderRole,
            "message" to cleanMsg,
            "timestamp" to now
        )

        var success = false

        // 1. Primary write target: Firestore support_tickets/{ticketId}/messages (Cloud Functions mirror RTDB)
        try {
            firestore.collection("support_tickets").document(ticketId).collection("messages").document(msgId).set(payload).await()
            success = true
        } catch (e: Exception) {
            Log.w(TAG, "Failed writing ticket message to Firestore support_tickets: ${e.message}")
        }

        try {
            firestore.collection("complaints").document(ticketId).collection("messages").document(msgId).set(payload).await()
            success = true
        } catch (_: Exception) {}

        // 2. Update ticket metadata primarily in Firestore (updatedAt, adminNote, adminReply, repliedAt, status = IN_PROGRESS)
        try {
            val ticketUpdates = mapOf<String, Any>(
                "updatedAt" to now,
                "repliedAt" to now,
                "adminNote" to cleanMsg,
                "adminReply" to cleanMsg,
                "status" to "IN_PROGRESS"
            )
            try { firestore.collection("support_tickets").document(ticketId).set(ticketUpdates, SetOptions.merge()).await() } catch (_: Exception) {}
            try { firestore.collection("problem_reports").document(ticketId).set(ticketUpdates, SetOptions.merge()).await() } catch (_: Exception) {}
            try { firestore.collection("complaints").document(ticketId).set(ticketUpdates, SetOptions.merge()).await() } catch (_: Exception) {}
        } catch (_: Exception) {}

        return success
    }

    suspend fun getLivePayoutRequestsStream(): Flow<List<PayoutRequest>> = callbackFlow {
        val payoutsMap = mutableMapOf<String, PayoutRequest>()

        try {
            ensureAuthenticatedSession(activeAdminEmail)
        } catch (_: Exception) {}

        fun emitPayouts() {
            trySend(payoutsMap.values.toList().sortedByDescending { it.requestedAt })
        }

        emitPayouts()

        val rtdbNodes = listOf("withdrawal_requests", "payout_requests", "withdrawals", "withdraw_requests", "deposit_requests", "wallet_requests")
        rtdbNodes.forEach { nodeName ->
            database.child(nodeName).get().addOnSuccessListener { snapshot ->
                snapshot.children.mapNotNull { it.toPayoutRequest() }.forEach { req ->
                    if (req.id.isNotBlank()) payoutsMap[req.id] = req
                }
                emitPayouts()
            }
        }

        val rtdbListeners = mutableListOf<Pair<com.google.firebase.database.DatabaseReference, ValueEventListener>>()
        rtdbNodes.forEach { nodeName ->
            val ref = database.child(nodeName)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.mapNotNull { it.toPayoutRequest() }.forEach { req ->
                        if (req.id.isNotBlank()) payoutsMap[req.id] = req
                    }
                    emitPayouts()
                }
                override fun onCancelled(error: DatabaseError) {
                    emitPayouts()
                }
            }
            ref.addValueEventListener(listener)
            rtdbListeners.add(ref to listener)
        }

        val fsListeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()
        rtdbNodes.forEach { colName ->
            try {
                val reg = firestore.collection(colName).addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null) {
                        snapshot.documents.mapNotNull { it.toPayoutRequest() }.forEach { req ->
                            if (req.id.isNotBlank()) payoutsMap[req.id] = req
                        }
                        emitPayouts()
                    }
                }
                fsListeners.add(reg)
            } catch (_: Exception) {}
        }

        awaitClose {
            rtdbListeners.forEach { (ref, listener) -> ref.removeEventListener(listener) }
            fsListeners.forEach { it.remove() }
        }
    }

    suspend fun approvePayout(request: PayoutRequest, adminEmail: String) {
        try {
            val now = System.currentTimeMillis()
            val reqAmount = request.vtAmount.toDouble()

            // Strictly enforce that withdrawals ONLY come from Winning Money (winningFunds / winningsBalance) and clear lockedBalance
            if (request.uid.isNotBlank()) {
                val userRef = database.child("users").child(request.uid)
                val snap = userRef.get().await()
                val currentDeposit = safeDouble(snap.child("depositBalance").value ?: snap.child("depositFunds").value ?: snap.child("deposit_funds").value)
                val currentWinning = safeDouble(snap.child("winningsBalance").value ?: snap.child("winningFunds").value ?: snap.child("winning_funds").value)
                val currentBonus = safeDouble(snap.child("bonusBalance").value ?: snap.child("bonusFunds").value ?: snap.child("bonus_funds").value)
                val currentLocked = safeDouble(snap.child("lockedBalance").value)
                val currentVersion = safeInt(snap.child("version").value, 1)
                val currentTotalWithdrawn = safeDouble(snap.child("totalWithdrawn").value ?: snap.child("walletAndFinanceSummary").child("totalWithdrawn").value)

                // If funds were already moved into lockedBalance by the user app, clear lockedBalance; otherwise deduct from winningFunds
                val updatedWinning = if (currentLocked >= reqAmount) {
                    currentWinning.coerceAtLeast(0.0)
                } else if (currentWinning >= reqAmount) {
                    (currentWinning - reqAmount).coerceAtLeast(0.0)
                } else {
                    currentWinning.coerceAtLeast(0.0)
                }
                val updatedLocked = (currentLocked - reqAmount).coerceAtLeast(0.0)
                val updatedTotal = (currentDeposit + updatedWinning + currentBonus).coerceAtLeast(0.0)
                val updatedWithdrawn = currentTotalWithdrawn + reqAmount

                val walletUpdates = mapOf<String, Any>(
                    "depositBalance" to currentDeposit,
                    "depositFunds" to currentDeposit,
                    "deposit_funds" to currentDeposit,
                    "winningsBalance" to updatedWinning,
                    "winningFunds" to updatedWinning,
                    "winning_funds" to updatedWinning,
                    "bonusBalance" to currentBonus,
                    "bonusFunds" to currentBonus,
                    "bonus_funds" to currentBonus,
                    "lockedBalance" to updatedLocked,
                    "withdrawableBalance" to updatedWinning,
                    "nonWithdrawableBalance" to (currentDeposit + currentBonus),
                    "totalWithdrawn" to updatedWithdrawn,
                    "balance" to updatedTotal,
                    "funds" to updatedTotal,
                    "walletBalance" to updatedTotal,
                    "version" to (currentVersion + 1),
                    "updatedAt" to now,
                    "lastActive" to now
                )
                try { userRef.updateChildren(walletUpdates).await() } catch (_: Exception) {}
                try { database.child("userProfiles").child(request.uid).updateChildren(walletUpdates).await() } catch (_: Exception) {}
                try { database.child("players").child(request.uid).updateChildren(walletUpdates).await() } catch (_: Exception) {}
                try { database.child("wallet_accounts").child(request.uid).updateChildren(walletUpdates).await() } catch (_: Exception) {}
                try { firestore.collection("users").document(request.uid).set(walletUpdates, com.google.firebase.firestore.SetOptions.merge()).await() } catch (_: Exception) {}
            }

            val payload = mapOf(
                "id" to request.id,
                "requestId" to request.id,
                "userId" to request.uid,
                "username" to request.username,
                "userName" to request.username,
                "amount" to request.vtAmount,
                "walletBucket" to "WINNING",
                "withdrawnFromBucket" to "WINNING_MONEY",
                "payoutMethod" to request.paymentMethod,
                "method" to request.paymentMethod,
                "accountDetails" to request.paymentId,
                "upiId" to request.paymentId,
                "status" to "APPROVED",
                "createdAt" to request.requestedAt,
                "requestedAt" to request.requestedAt,
                "processedAt" to now,
                "processedBy" to adminEmail
            )
            val targets = listOf("withdrawal_requests", "payout_requests", "withdrawals", "withdraw_requests", "wallet_requests")
            targets.forEach { target ->
                try { database.child(target).child(request.id).updateChildren(payload).await() } catch (_: Exception) {}
                try { firestore.collection(target).document(request.id).set(payload, com.google.firebase.firestore.SetOptions.merge()).await() } catch (_: Exception) {}
            }
            try { database.child("admin_channels").child("withdrawal_requests").child(request.id).updateChildren(payload).await() } catch (_: Exception) {}
            try { firestore.collection("admin_channels").document("withdrawal_requests").collection("items").document(request.id).set(payload, com.google.firebase.firestore.SetOptions.merge()).await() } catch (_: Exception) {}

            if (request.uid.isNotBlank()) {
                try { database.child("users").child(request.uid).child("withdraw_requests").child(request.id).updateChildren(payload).await() } catch (_: Exception) {}
                try { firestore.collection("users").document(request.uid).collection("withdraw_requests").document(request.id).set(payload).await() } catch (_: Exception) {}

                val txId = "TX-${now}"
                val txPayload = mapOf(
                    "id" to txId,
                    "userId" to request.uid,
                    "amount" to request.vtAmount,
                    "type" to "WITHDRAWAL",
                    "walletBucket" to "WINNING",
                    "timestamp" to now,
                    "status" to "COMPLETED",
                    "referenceId" to request.id,
                    "description" to "Winning Money Payout approved via ${request.paymentMethod} (${request.paymentId})"
                )
                try { database.child("transactions").child(request.uid).child(request.id).updateChildren(mapOf("status" to "COMPLETED", "processedAt" to now)).await() } catch (_: Exception) {}
                try { database.child("transactions").child(txId).setValue(txPayload).await() } catch (_: Exception) {}
                try { database.child("wallet_transactions").child(request.uid).child(txId).setValue(txPayload).await() } catch (_: Exception) {}
                try { firestore.collection("transactions").document(request.id).set(txPayload, com.google.firebase.firestore.SetOptions.merge()).await() } catch (_: Exception) {}
                try { firestore.collection("transactions").document(txId).set(txPayload).await() } catch (_: Exception) {}

                val notifId = "notif_wd_ok_${now}"
                val notifMap = mapOf(
                    "id" to notifId,
                    "title" to "Withdrawal Approved!",
                    "message" to "Your payout of ₹${request.vtAmount.toInt()} via ${request.paymentMethod} (${request.paymentId}) has been processed!",
                    "type" to "WALLET",
                    "timestamp" to now,
                    "isRead" to false
                )
                try { database.child("notifications").child(request.uid).child(notifId).setValue(notifMap).await() } catch (_: Exception) {}
                try { firestore.collection("users").document(request.uid).collection("notifications").document(notifId).set(notifMap).await() } catch (_: Exception) {}
            }

            GlobalErrorManager.emitSuccess("Winning Money Payout of ₹${request.vtAmount.toInt()} approved for ${request.username}!")

            context?.let { ctx ->
                val notif = VelorixNotificationManager.dispatchPayoutNotification(
                    context = ctx,
                    username = request.username,
                    amount = request.vtAmount.toDouble(),
                    approved = true,
                    details = "${request.paymentMethod} (${request.paymentId})",
                    targetUserId = request.uid
                )
                syncNotificationToCloud(notif)
            }
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to approve payout: ${e.message}", e)
        }
    }

    suspend fun rejectPayout(request: PayoutRequest, rejectionReason: String, adminEmail: String) {
        try {
            val now = System.currentTimeMillis()
            val payload = mapOf(
                "id" to request.id,
                "requestId" to request.id,
                "userId" to request.uid,
                "username" to request.username,
                "userName" to request.username,
                "amount" to request.vtAmount,
                "walletBucket" to "WINNING",
                "payoutMethod" to request.paymentMethod,
                "method" to request.paymentMethod,
                "accountDetails" to request.paymentId,
                "upiId" to request.paymentId,
                "status" to "REJECTED",
                "rejectionReason" to rejectionReason,
                "createdAt" to request.requestedAt,
                "requestedAt" to request.requestedAt,
                "processedAt" to now,
                "processedBy" to adminEmail
            )
            val targets = listOf("withdrawal_requests", "payout_requests", "withdrawals", "withdraw_requests", "wallet_requests")
            targets.forEach { target ->
                try { database.child(target).child(request.id).updateChildren(payload).await() } catch (_: Exception) {}
                try { firestore.collection(target).document(request.id).set(payload, com.google.firebase.firestore.SetOptions.merge()).await() } catch (_: Exception) {}
            }
            try { database.child("admin_channels").child("withdrawal_requests").child(request.id).updateChildren(payload).await() } catch (_: Exception) {}
            try { firestore.collection("admin_channels").document("withdrawal_requests").collection("items").document(request.id).set(payload, com.google.firebase.firestore.SetOptions.merge()).await() } catch (_: Exception) {}

            // Refund strictly to Winning Money (winningsBalance / winningFunds) and clear lockedBalance
            if (request.uid.isNotBlank()) {
                val userRef = database.child("users").child(request.uid)
                val snap = userRef.get().await()
                val currentDeposit = safeDouble(snap.child("depositBalance").value ?: snap.child("depositFunds").value ?: snap.child("deposit_funds").value)
                val currentWinning = safeDouble(snap.child("winningsBalance").value ?: snap.child("winningFunds").value ?: snap.child("winning_funds").value)
                val currentBonus = safeDouble(snap.child("bonusBalance").value ?: snap.child("bonusFunds").value ?: snap.child("bonus_funds").value)
                val currentLocked = safeDouble(snap.child("lockedBalance").value)
                val currentVersion = safeInt(snap.child("version").value, 1)
                val updatedWinning = currentWinning + request.vtAmount
                val updatedLocked = (currentLocked - request.vtAmount).coerceAtLeast(0.0)
                val updatedBalance = currentDeposit + updatedWinning + currentBonus

                val refundUpdates = mapOf<String, Any>(
                    "winningsBalance" to updatedWinning,
                    "winningFunds" to updatedWinning,
                    "winning_funds" to updatedWinning,
                    "withdrawableBalance" to updatedWinning,
                    "lockedBalance" to updatedLocked,
                    "balance" to updatedBalance,
                    "funds" to updatedBalance,
                    "walletBalance" to updatedBalance,
                    "version" to (currentVersion + 1),
                    "updatedAt" to now,
                    "lastActive" to now
                )
                userRef.updateChildren(refundUpdates).await()
                try { database.child("userProfiles").child(request.uid).updateChildren(refundUpdates).await() } catch (_: Exception) {}
                try { database.child("players").child(request.uid).updateChildren(refundUpdates).await() } catch (_: Exception) {}
                try { database.child("wallet_accounts").child(request.uid).updateChildren(refundUpdates).await() } catch (_: Exception) {}
                try { firestore.collection("users").document(request.uid).set(refundUpdates, com.google.firebase.firestore.SetOptions.merge()).await() } catch (_: Exception) {}
                try { database.child("users").child(request.uid).child("withdraw_requests").child(request.id).updateChildren(payload).await() } catch (_: Exception) {}
                try { firestore.collection("users").document(request.uid).collection("withdraw_requests").document(request.id).set(payload).await() } catch (_: Exception) {}

                val txId = "TX-REFUND-${now}"
                val txPayload = mapOf(
                    "id" to txId,
                    "userId" to request.uid,
                    "amount" to request.vtAmount,
                    "type" to "REFUND",
                    "walletBucket" to "WINNING",
                    "timestamp" to now,
                    "status" to "REFUNDED",
                    "referenceId" to request.id,
                    "description" to "Payout rejected & refunded to Winning Balance: $rejectionReason"
                )
                try { database.child("transactions").child(request.uid).child(request.id).updateChildren(mapOf("status" to "REFUNDED", "processedAt" to now)).await() } catch (_: Exception) {}
                try { database.child("transactions").child(txId).setValue(txPayload).await() } catch (_: Exception) {}
                try { database.child("wallet_transactions").child(request.uid).child(txId).setValue(txPayload).await() } catch (_: Exception) {}
                try { firestore.collection("transactions").document(request.id).set(txPayload, com.google.firebase.firestore.SetOptions.merge()).await() } catch (_: Exception) {}
                try { firestore.collection("transactions").document(txId).set(txPayload).await() } catch (_: Exception) {}

                val notifId = "notif_wd_rej_${now}"
                val notifMap = mapOf(
                    "id" to notifId,
                    "title" to "Withdrawal Rejected & Refunded",
                    "message" to "Your withdrawal of ₹${request.vtAmount.toInt()} was rejected ($rejectionReason). Funds have been refunded to your Winnings Balance.",
                    "type" to "WALLET",
                    "timestamp" to now,
                    "isRead" to false
                )
                try { database.child("notifications").child(request.uid).child(notifId).setValue(notifMap).await() } catch (_: Exception) {}
                try { firestore.collection("users").document(request.uid).collection("notifications").document(notifId).set(notifMap).await() } catch (_: Exception) {}
            }

            GlobalErrorManager.emitSuccess("Payout rejected and ₹${request.vtAmount.toInt()} refunded to ${request.username}'s Winning Balance.")

            context?.let { ctx ->
                val notif = VelorixNotificationManager.dispatchPayoutNotification(
                    context = ctx,
                    username = request.username,
                    amount = request.vtAmount.toDouble(),
                    approved = false,
                    details = rejectionReason,
                    targetUserId = request.uid
                )
                syncNotificationToCloud(notif)
            }
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to reject payout: ${e.message}", e)
        }
    }

    suspend fun approveDeposit(request: PayoutRequest, adminEmail: String) {
        try {
            val now = System.currentTimeMillis()
            val payload = mapOf(
                "id" to request.id,
                "requestId" to request.id,
                "userId" to request.uid,
                "username" to request.username,
                "userName" to request.username,
                "amount" to request.vtAmount,
                "walletBucket" to "DEPOSIT",
                "withdrawable" to false,
                "utrNumber" to request.paymentId,
                "paymentRef" to request.paymentId,
                "paymentMethod" to request.paymentMethod.ifBlank { "UPI_MANUAL_UTR" },
                "status" to "APPROVED",
                "processedAt" to now,
                "processedBy" to adminEmail
            )
            try { database.child("deposit_requests").child(request.id).updateChildren(payload).await() } catch (_: Exception) {}
            try { database.child("wallet_requests").child(request.id).updateChildren(payload).await() } catch (_: Exception) {}
            try { database.child("admin_channels").child("deposit_requests").child(request.id).updateChildren(payload).await() } catch (_: Exception) {}
            try { firestore.collection("deposit_requests").document(request.id).set(payload, com.google.firebase.firestore.SetOptions.merge()).await() } catch (_: Exception) {}
            try { firestore.collection("wallet_requests").document(request.id).set(payload, com.google.firebase.firestore.SetOptions.merge()).await() } catch (_: Exception) {}
            try { firestore.collection("admin_channels").document("deposit_requests").collection("items").document(request.id).set(payload, com.google.firebase.firestore.SetOptions.merge()).await() } catch (_: Exception) {}

            // Credit strictly into Deposited Money (depositBalance / depositFunds - non-withdrawable)
            if (request.uid.isNotBlank()) {
                val userRef = database.child("users").child(request.uid)
                val snap = userRef.get().await()
                val currentDeposit = safeDouble(snap.child("depositBalance").value ?: snap.child("depositFunds").value ?: snap.child("deposit_funds").value)
                val currentWinning = safeDouble(snap.child("winningsBalance").value ?: snap.child("winningFunds").value ?: snap.child("winning_funds").value)
                val currentBonus = safeDouble(snap.child("bonusBalance").value ?: snap.child("bonusFunds").value ?: snap.child("bonus_funds").value)
                val currentLegacy = safeDouble(snap.child("balance").value ?: snap.child("funds").value)
                val currentVersion = safeInt(snap.child("version").value, 1)
                val currentTotalDeposited = safeDouble(snap.child("totalDeposited").value ?: snap.child("walletAndFinanceSummary").child("totalDeposited").value)
                val referredByCodeOrUid = snap.child("referredBy").value?.toString()?.trim() ?: ""

                val baseDeposit = if (currentDeposit == 0.0 && currentWinning == 0.0 && currentBonus == 0.0 && currentLegacy > 0.0) currentLegacy else currentDeposit
                val updatedDeposit = baseDeposit + request.vtAmount
                val updatedBalance = updatedDeposit + currentWinning + currentBonus
                val updatedTotalDeposited = maxOf(currentTotalDeposited, baseDeposit) + request.vtAmount

                val depUpdates = mapOf<String, Any>(
                    "depositBalance" to updatedDeposit,
                    "depositFunds" to updatedDeposit,
                    "deposit_funds" to updatedDeposit,
                    "winningsBalance" to currentWinning,
                    "winningFunds" to currentWinning,
                    "winning_funds" to currentWinning,
                    "bonusBalance" to currentBonus,
                    "bonusFunds" to currentBonus,
                    "bonus_funds" to currentBonus,
                    "withdrawableBalance" to currentWinning,
                    "nonWithdrawableBalance" to (updatedDeposit + currentBonus),
                    "totalDeposited" to updatedTotalDeposited,
                    "balance" to updatedBalance,
                    "funds" to updatedBalance,
                    "walletBalance" to updatedBalance,
                    "version" to (currentVersion + 1),
                    "updatedAt" to now,
                    "lastActive" to now
                )
                userRef.updateChildren(depUpdates).await()
                try { database.child("userProfiles").child(request.uid).updateChildren(depUpdates).await() } catch (_: Exception) {}
                try { database.child("players").child(request.uid).updateChildren(depUpdates).await() } catch (_: Exception) {}
                try { database.child("wallet_accounts").child(request.uid).updateChildren(depUpdates).await() } catch (_: Exception) {}
                try { firestore.collection("users").document(request.uid).set(depUpdates, com.google.firebase.firestore.SetOptions.merge()).await() } catch (_: Exception) {}
                try { database.child("users").child(request.uid).child("deposit_requests").child(request.id).updateChildren(payload).await() } catch (_: Exception) {}
                try { firestore.collection("users").document(request.uid).collection("deposit_requests").document(request.id).set(payload).await() } catch (_: Exception) {}

                val userNotifId = "notif_dep_ok_${now}"
                val userNotifMap = mapOf(
                    "id" to userNotifId,
                    "title" to "Deposit Approved!",
                    "message" to "₹${request.vtAmount.toInt()} VT has been verified and credited to your Deposit Balance.",
                    "type" to "WALLET",
                    "timestamp" to now,
                    "isRead" to false
                )
                try { database.child("notifications").child(request.uid).child(userNotifId).setValue(userNotifMap).await() } catch (_: Exception) {}
                try { firestore.collection("users").document(request.uid).collection("notifications").document(userNotifId).set(userNotifMap).await() } catch (_: Exception) {}

                // Referral Commission Check (10%–15% tier commission on deposit amount)
                if (referredByCodeOrUid.isNotBlank()) {
                    try {
                        val allUsersSnap = database.child("users").get().await()
                        var referrerSnap: DataSnapshot? = null
                        for (uChild in allUsersSnap.children) {
                            val uKey = uChild.key ?: ""
                            val rCode = uChild.child("referralCode").value?.toString()?.trim() ?: ""
                            if (uKey != request.uid && (uKey == referredByCodeOrUid || (rCode.isNotBlank() && rCode.equals(referredByCodeOrUid, true)))) {
                                referrerSnap = uChild
                                break
                            }
                        }
                        if (referrerSnap != null) {
                            val refUid = referrerSnap.key ?: ""
                            val refCount = safeInt(referrerSnap.child("referralCount").value, 1)
                            val commissionRate = when {
                                refCount >= 25 -> 0.15 // 15% Diamond Tier
                                refCount >= 10 -> 0.12 // 12% Gold Tier
                                else -> 0.10           // 10% Standard Tier
                            }
                            val commissionVt = (request.vtAmount * commissionRate * 100.0).toInt() / 100.0
                            if (commissionVt > 0.0 && refUid.isNotBlank()) {
                                val rDep = safeDouble(referrerSnap.child("depositBalance").value ?: referrerSnap.child("depositFunds").value)
                                val rWin = safeDouble(referrerSnap.child("winningsBalance").value ?: referrerSnap.child("winningFunds").value)
                                val rBon = safeDouble(referrerSnap.child("bonusBalance").value ?: referrerSnap.child("bonusFunds").value)
                                val rEarned = safeDouble(referrerSnap.child("referralEarnings").value ?: referrerSnap.child("referralBonusEarned").value)
                                val rVer = safeInt(referrerSnap.child("version").value, 1)
                                val newRBon = rBon + commissionVt
                                val newRTotal = rDep + rWin + newRBon
                                val newREarned = rEarned + commissionVt
                                val refUpdates = mapOf<String, Any>(
                                    "bonusBalance" to newRBon,
                                    "bonusFunds" to newRBon,
                                    "bonus_funds" to newRBon,
                                    "referralBonus" to newRBon,
                                    "referralEarnings" to newREarned,
                                    "referralBonusEarned" to newREarned,
                                    "balance" to newRTotal,
                                    "funds" to newRTotal,
                                    "version" to (rVer + 1),
                                    "updatedAt" to now
                                )
                                try { database.child("users").child(refUid).updateChildren(refUpdates).await() } catch (_: Exception) {}
                                try { database.child("userProfiles").child(refUid).updateChildren(refUpdates).await() } catch (_: Exception) {}
                                try { firestore.collection("users").document(refUid).set(refUpdates, com.google.firebase.firestore.SetOptions.merge()).await() } catch (_: Exception) {}

                                val refNotifId = "notif_ref_comm_${now}"
                                val refNotif = mapOf(
                                    "id" to refNotifId,
                                    "title" to "Referral Commission Earned!",
                                    "message" to "You earned +₹$commissionVt Referral Bonus (${(commissionRate * 100).toInt()}% commission) from ${request.username}'s deposit!",
                                    "type" to "REFERRAL",
                                    "timestamp" to now,
                                    "isRead" to false
                                )
                                try { database.child("notifications").child(refUid).child(refNotifId).setValue(refNotif).await() } catch (_: Exception) {}
                                try { firestore.collection("users").document(refUid).collection("notifications").document(refNotifId).set(refNotif).await() } catch (_: Exception) {}
                            }
                        }
                    } catch (_: Exception) {}
                }
            }

            val cleanUtr = request.paymentId.trim()
            if (cleanUtr.isNotBlank()) {
                val utrPayload = mapOf(
                    "userId" to request.uid,
                    "amount" to request.vtAmount,
                    "walletBucket" to "DEPOSIT",
                    "verifiedAt" to now,
                    "requestId" to request.id,
                    "verifiedBy" to adminEmail
                )
                try { database.child("processed_utrs").child(cleanUtr).setValue(utrPayload).await() } catch (_: Exception) {}
                try { database.child("verified_bank_deposits").child(cleanUtr).child("claimed").setValue(true).await() } catch (_: Exception) {}
            }

            val txId = "TX-DEP-${now}"
            val txPayload = mapOf(
                "id" to txId,
                "userId" to request.uid,
                "amount" to request.vtAmount,
                "type" to "DEPOSIT",
                "walletBucket" to "DEPOSIT",
                "withdrawable" to false,
                "status" to "COMPLETED",
                "timestamp" to now,
                "description" to "Manual UTR Deposit verified into Deposited Money [Non-Withdrawable] (Ref: $cleanUtr)"
            )
            try { database.child("transactions").child(request.uid).child(request.id).updateChildren(mapOf("status" to "COMPLETED", "processedAt" to now)).await() } catch (_: Exception) {}
            try { database.child("transactions").child(txId).setValue(txPayload).await() } catch (_: Exception) {}
            try { database.child("wallet_transactions").child(request.uid).child(txId).setValue(txPayload).await() } catch (_: Exception) {}
            try { firestore.collection("transactions").document(request.id).set(txPayload, com.google.firebase.firestore.SetOptions.merge()).await() } catch (_: Exception) {}
            try { firestore.collection("transactions").document(txId).set(txPayload).await() } catch (_: Exception) {}

            GlobalErrorManager.emitSuccess("Deposit of ₹${request.vtAmount.toInt()} verified & credited to ${request.username}'s Deposited Balance!")
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to approve deposit: ${e.message}", e)
        }
    }

    suspend fun rejectDeposit(request: PayoutRequest, rejectionReason: String, adminEmail: String) {
        try {
            val now = System.currentTimeMillis()
            val payload = mapOf(
                "id" to request.id,
                "requestId" to request.id,
                "userId" to request.uid,
                "username" to request.username,
                "userName" to request.username,
                "amount" to request.vtAmount,
                "utrNumber" to request.paymentId,
                "paymentRef" to request.paymentId,
                "status" to "REJECTED",
                "rejectionReason" to rejectionReason,
                "processedAt" to now,
                "processedBy" to adminEmail
            )
            try { database.child("deposit_requests").child(request.id).updateChildren(payload).await() } catch (_: Exception) {}
            try { database.child("wallet_requests").child(request.id).updateChildren(payload).await() } catch (_: Exception) {}
            try { database.child("admin_channels").child("deposit_requests").child(request.id).updateChildren(payload).await() } catch (_: Exception) {}
            try { firestore.collection("deposit_requests").document(request.id).set(payload, com.google.firebase.firestore.SetOptions.merge()).await() } catch (_: Exception) {}
            try { firestore.collection("wallet_requests").document(request.id).set(payload, com.google.firebase.firestore.SetOptions.merge()).await() } catch (_: Exception) {}
            try { firestore.collection("admin_channels").document("deposit_requests").collection("items").document(request.id).set(payload, com.google.firebase.firestore.SetOptions.merge()).await() } catch (_: Exception) {}
            if (request.uid.isNotBlank()) {
                try { database.child("users").child(request.uid).child("deposit_requests").child(request.id).updateChildren(payload).await() } catch (_: Exception) {}
                try { firestore.collection("users").document(request.uid).collection("deposit_requests").document(request.id).set(payload).await() } catch (_: Exception) {}
                val notifId = "notif_dep_rej_${now}"
                val notifMap = mapOf(
                    "id" to notifId,
                    "title" to "Deposit Request Rejected",
                    "message" to "Your deposit request of ₹${request.vtAmount.toInt()} (UTR: ${request.paymentId}) was rejected: $rejectionReason",
                    "type" to "WALLET",
                    "timestamp" to now,
                    "isRead" to false
                )
                try { database.child("notifications").child(request.uid).child(notifId).setValue(notifMap).await() } catch (_: Exception) {}
                try { firestore.collection("users").document(request.uid).collection("notifications").document(notifId).set(notifMap).await() } catch (_: Exception) {}
            }
            GlobalErrorManager.emitSuccess("Deposit request rejected.")
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to reject deposit: ${e.message}", e)
        }
    }

    suspend fun getLiveAdminsStream(): Flow<List<AdminRecord>> = callbackFlow {
        val adminsMap = mutableMapOf<String, AdminRecord>()

        try {
            ensureAuthenticatedSession(activeAdminEmail)
        } catch (_: Exception) {}

        fun emitAdmins() {
            val curr = auth.currentUser
            val currentEmail = curr?.email?.trim()?.lowercase()?.ifBlank { null } ?: activeAdminEmail.trim().lowercase()
            val currentUid = curr?.uid?.trim()?.ifBlank { null } ?: activeAdminUid.trim().ifBlank { "admin_${currentEmail.hashCode()}" }

            val mergedAdmins = mutableMapOf<String, AdminRecord>()

            // Ingest all records from map, merging duplicate emails or uids
            adminsMap.values.forEach { admin ->
                val normEmail = admin.email.trim().lowercase()
                val uid = admin.uid.trim()
                val dedupeKey = if (normEmail.isNotBlank() && normEmail.contains("@")) {
                    "email:$normEmail"
                } else if (uid.isNotBlank()) {
                    "uid:$uid"
                } else {
                    ""
                }

                if (dedupeKey.isNotBlank()) {
                    val existing = mergedAdmins[dedupeKey]
                    if (existing == null) {
                        mergedAdmins[dedupeKey] = admin
                    } else {
                        // Merge fields, keeping the best UID and status
                        val bestUid = if (uid.length >= 20 && !uid.contains("@") && !uid.contains(".")) uid else existing.uid
                        val bestName = if (admin.name.isNotBlank() && !admin.name.contains("@")) admin.name else existing.name
                        val bestRole = if (admin.role.equals("super_admin", true) || existing.role.equals("super_admin", true)) "super_admin" else admin.role
                        val bestActive = admin.active || existing.active
                        mergedAdmins[dedupeKey] = existing.copy(
                            uid = bestUid,
                            name = bestName,
                            role = bestRole,
                            active = bestActive
                        )
                    }
                }
            }

            // Ensure current logged-in super admin is present
            val currentDedupeKey = "email:$currentEmail"
            if (!mergedAdmins.containsKey(currentDedupeKey)) {
                mergedAdmins[currentDedupeKey] = AdminRecord(
                    uid = currentUid,
                    email = currentEmail,
                    name = curr?.displayName?.ifBlank { null } ?: currentEmail.substringBefore("@").ifBlank { "Super Admin" },
                    role = "super_admin",
                    active = true,
                    assignedAt = System.currentTimeMillis(),
                    grantedBy = "SYSTEM"
                )
            }

            val isMockAdmin: (AdminRecord) -> Boolean = { a ->
                val e = a.email.trim().lowercase()
                val uid = a.uid.trim().lowercase()
                val n = a.name.trim().lowercase()
                e.contains("@example.com") || e.contains("mock.dummy") || e.contains("test@") || e.contains("demo@") ||
                uid.startsWith("mock_") || uid.startsWith("demo_") || uid.startsWith("test_") ||
                n.contains("mock admin") || n.contains("demo admin")
            }

            val finalList = mergedAdmins.values.filter { !isMockAdmin(it) }.sortedBy { it.name.lowercase() }
            trySend(finalList)
        }

        emitAdmins()

        // Primary authoritative nodes
        val primaryRtdbRef = database.child("admins")
        val primaryFsCol = firestore.collection("admins")

        val rtdbListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentIds = mutableSetOf<String>()
                snapshot.children.mapNotNull { it.toAdminRecord() }.forEach { admin ->
                    if (admin.uid.isNotBlank()) {
                        adminsMap[admin.uid] = admin
                        currentIds.add(admin.uid)
                    }
                }
                emitAdmins()
            }
            override fun onCancelled(error: DatabaseError) {
                emitAdmins()
            }
        }
        primaryRtdbRef.addValueEventListener(rtdbListener)

        var fsListener: com.google.firebase.firestore.ListenerRegistration? = null
        try {
            fsListener = primaryFsCol.addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    snapshot.documents.forEach { doc ->
                        val uid = doc.id
                        val email = doc.getString("email") ?: ""
                        val name = doc.getString("name") ?: email.substringBefore("@")
                        val role = doc.getString("role") ?: "tournament_admin"
                        val active = doc.getBoolean("active") ?: true
                        val assignedAt = doc.getLong("assignedAt") ?: doc.getLong("grantedAt") ?: System.currentTimeMillis()
                        val grantedBy = doc.getString("grantedBy") ?: ""
                        if (uid.isNotBlank()) {
                            adminsMap[uid] = AdminRecord(
                                uid = uid,
                                email = email,
                                name = name,
                                role = role,
                                active = active,
                                assignedAt = assignedAt,
                                grantedBy = grantedBy
                            )
                        }
                    }
                    emitAdmins()
                }
            }
        } catch (_: Exception) {}

        awaitClose {
            primaryRtdbRef.removeEventListener(rtdbListener)
            fsListener?.remove()
        }
    }

    suspend fun grantAdminAccess(uid: String, email: String, name: String, role: String, grantedBy: String) {
        val cleanEmail = email.trim().lowercase()
        val adminUid = if (uid.isNotBlank()) uid else "ADM_${cleanEmail.replace(".", "_").replace("@", "_")}"
        val now = System.currentTimeMillis()
        val validRole = when (role.trim().lowercase()) {
            "super_admin", "tournament_admin", "support_admin", "moderator_admin" -> role.trim().lowercase()
            else -> "tournament_admin"
        }
        val payload = mapOf(
            "uid" to adminUid,
            "role" to validRole,
            "active" to true,
            "status" to "ACTIVE",
            "grantedBy" to grantedBy,
            "email" to email.trim(),
            "name" to name.trim(),
            "grantedAt" to now,
            "assignedAt" to now
        )
        val userRoleUpdates = mapOf<String, Any>(
            "role" to validRole,
            "isAdmin" to true,
            "active" to true
        )

        var anySuccess = false

        val rtdbNodes = listOf("admins", "Admins", "staff", "Staff", "admin_users")
        rtdbNodes.forEach { node ->
            try {
                database.child(node).child(adminUid).setValue(payload).await()
                anySuccess = true
            } catch (_: Exception) {}
        }

        val fsCols = listOf("admins", "Admins", "staff")
        fsCols.forEach { col ->
            try {
                firestore.collection(col).document(adminUid).set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
                anySuccess = true
            } catch (_: Exception) {}
        }

        // Sync to user profiles
        try {
            database.child("users").child(adminUid).updateChildren(userRoleUpdates).await()
            database.child("userProfiles").child(adminUid).updateChildren(userRoleUpdates).await()
            firestore.collection("users").document(adminUid).set(userRoleUpdates, com.google.firebase.firestore.SetOptions.merge()).await()
            firestore.collection("userProfiles").document(adminUid).set(userRoleUpdates, com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (_: Exception) {}

        if (anySuccess) {
            GlobalErrorManager.emitSuccess("Admin access granted to $name ($validRole)!")
        } else {
            GlobalErrorManager.emitError("Permission denied: Check Firebase Realtime Database and Firestore security rules.")
        }
    }

    suspend fun revokeAdminAccess(adminUid: String) {
        var anySuccess = false
        val updates = mapOf<String, Any>(
            "active" to false,
            "status" to "REVOKED",
            "revokedAt" to System.currentTimeMillis()
        )
        val userRoleUpdates = mapOf<String, Any>(
            "role" to "player",
            "isAdmin" to false
        )

        val rtdbNodes = listOf("admins", "Admins", "staff", "Staff", "admin_users")
        rtdbNodes.forEach { node ->
            try {
                database.child(node).child(adminUid).updateChildren(updates).await()
                anySuccess = true
            } catch (_: Exception) {}
        }

        val fsCols = listOf("admins", "Admins", "staff")
        fsCols.forEach { col ->
            try {
                firestore.collection(col).document(adminUid).set(updates, com.google.firebase.firestore.SetOptions.merge()).await()
                anySuccess = true
            } catch (_: Exception) {}
        }

        // Update user profile status
        try {
            database.child("users").child(adminUid).updateChildren(userRoleUpdates).await()
            database.child("userProfiles").child(adminUid).updateChildren(userRoleUpdates).await()
            firestore.collection("users").document(adminUid).set(userRoleUpdates, com.google.firebase.firestore.SetOptions.merge()).await()
            firestore.collection("userProfiles").document(adminUid).set(userRoleUpdates, com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (_: Exception) {}

        if (anySuccess) {
            GlobalErrorManager.emitSuccess("Admin access revoked.")
        }
    }

    suspend fun updateAdminRole(adminUid: String, newRole: String) {
        var anySuccess = false
        val updates = mapOf<String, Any>(
            "role" to newRole,
            "updatedAt" to System.currentTimeMillis()
        )
        val userRoleUpdates = mapOf<String, Any>(
            "role" to newRole,
            "isAdmin" to true
        )

        val rtdbNodes = listOf("admins", "Admins", "staff", "Staff", "admin_users")
        rtdbNodes.forEach { node ->
            try {
                database.child(node).child(adminUid).updateChildren(updates).await()
                anySuccess = true
            } catch (_: Exception) {}
        }

        val fsCols = listOf("admins", "Admins", "staff")
        fsCols.forEach { col ->
            try {
                firestore.collection(col).document(adminUid).set(updates, com.google.firebase.firestore.SetOptions.merge()).await()
                anySuccess = true
            } catch (_: Exception) {}
        }

        try {
            database.child("users").child(adminUid).updateChildren(userRoleUpdates).await()
            database.child("userProfiles").child(adminUid).updateChildren(userRoleUpdates).await()
            firestore.collection("users").document(adminUid).set(userRoleUpdates, com.google.firebase.firestore.SetOptions.merge()).await()
            firestore.collection("userProfiles").document(adminUid).set(userRoleUpdates, com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (_: Exception) {}

        if (anySuccess) {
            GlobalErrorManager.emitSuccess("Role changed to $newRole")
        }
    }

    suspend fun updateAdminRecord(admin: AdminRecord) {
        val payload = mapOf(
            "uid" to admin.uid,
            "name" to admin.name,
            "email" to admin.email,
            "role" to admin.role,
            "active" to admin.active,
            "status" to if (admin.active) "ACTIVE" else "REVOKED",
            "assignedAt" to admin.assignedAt,
            "grantedBy" to admin.grantedBy,
            "updatedAt" to System.currentTimeMillis()
        )
        val userRoleUpdates = mapOf<String, Any>(
            "role" to admin.role,
            "isAdmin" to admin.active
        )

        var anySuccess = false

        val rtdbNodes = listOf("admins", "Admins", "staff", "Staff", "admin_users")
        rtdbNodes.forEach { node ->
            try {
                database.child(node).child(admin.uid).setValue(payload).await()
                anySuccess = true
            } catch (_: Exception) {}
        }

        val fsCols = listOf("admins", "Admins", "staff")
        fsCols.forEach { col ->
            try {
                firestore.collection(col).document(admin.uid).set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
                anySuccess = true
            } catch (_: Exception) {}
        }

        try {
            database.child("users").child(admin.uid).updateChildren(userRoleUpdates).await()
            database.child("userProfiles").child(admin.uid).updateChildren(userRoleUpdates).await()
            firestore.collection("users").document(admin.uid).set(userRoleUpdates, com.google.firebase.firestore.SetOptions.merge()).await()
            firestore.collection("userProfiles").document(admin.uid).set(userRoleUpdates, com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (_: Exception) {}

        if (anySuccess) {
            GlobalErrorManager.emitSuccess("Admin record for ${admin.name} updated!")
        } else {
            GlobalErrorManager.emitError("Failed to update admin: Permission denied on Firebase. Ensure your Database Rules allow authenticated admin writes.")
        }
    }

    suspend fun deleteAdminRecord(adminUid: String) {
        var anySuccess = false

        val rtdbNodes = listOf("admins", "Admins", "staff", "Staff", "admin_users")
        rtdbNodes.forEach { node ->
            try {
                database.child(node).child(adminUid).removeValue().await()
                anySuccess = true
            } catch (_: Exception) {}
        }

        val fsCols = listOf("admins", "Admins", "staff")
        fsCols.forEach { col ->
            try {
                firestore.collection(col).document(adminUid).delete().await()
                anySuccess = true
            } catch (_: Exception) {}
        }

        val userRoleUpdates = mapOf<String, Any>(
            "role" to "player",
            "isAdmin" to false
        )
        try {
            database.child("users").child(adminUid).updateChildren(userRoleUpdates).await()
            database.child("userProfiles").child(adminUid).updateChildren(userRoleUpdates).await()
            firestore.collection("users").document(adminUid).set(userRoleUpdates, com.google.firebase.firestore.SetOptions.merge()).await()
            firestore.collection("userProfiles").document(adminUid).set(userRoleUpdates, com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (_: Exception) {}

        if (anySuccess) {
            GlobalErrorManager.emitSuccess("Admin access removed completely!")
        } else {
            GlobalErrorManager.emitError("Permission denied: Unable to delete admin record from Firebase.")
        }
    }

    suspend fun getLiveBannedUsersStream(): Flow<Map<String, BannedUserRecord>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = mutableMapOf<String, BannedUserRecord>()
                for (child in snapshot.children) {
                    if (!child.hasChildren() || child.value !is Map<*, *>) continue
                    val record = child.safeGetObject<BannedUserRecord>()
                    if (record != null && record.uid.isNotBlank()) {
                        map[record.uid] = record
                    }
                }
                trySend(map)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        val ref = database.child("banned_users")
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun unbanAllGuestAndNonLoggedInSessions(): Int {
        var unbannedCount = 0
        try {
            val unbanUpdates = mapOf<String, Any>(
                "isBanned" to false,
                "banned" to false,
                "status" to "ACTIVE",
                "accountStatus" to "ACTIVE",
                "active" to true,
                "isAuthorized" to true,
                "walletFrozen" to false,
                "banReason" to ""
            )

            // 1. Scan banned_users table in RTDB
            try {
                val banSnap = database.child("banned_users").get().await()
                for (child in banSnap.children) {
                    val key = child.key ?: ""
                    val bUid = child.child("uid").value?.toString()?.trim() ?: ""
                    val bEmail = child.child("email").value?.toString()?.trim()?.lowercase() ?: ""
                    val bGameId = child.child("gameId").value?.toString()?.trim() ?: ""
                    val bReason = child.child("reason").value?.toString()?.trim()?.lowercase() ?: ""

                    var isGuestSessionBan = key.isBlank() ||
                        key.startsWith("guest", ignoreCase = true) ||
                        key.startsWith("anon", ignoreCase = true) ||
                        bUid.startsWith("guest", ignoreCase = true) ||
                        bUid.startsWith("anon", ignoreCase = true) ||
                        bEmail.contains("guest") ||
                        bEmail.contains("anonymous") ||
                        (!bEmail.contains("@") && bGameId.isBlank()) ||
                        bReason.contains("guest")

                    val targetUid = bUid.ifBlank { if (!key.contains("@") && !key.contains(".")) key else "" }
                    if (!isGuestSessionBan && targetUid.isNotBlank()) {
                        try {
                            val userSnap = database.child("users").child(targetUid).get().await()
                            if (userSnap.exists()) {
                                val uEmail = userSnap.child("email").value?.toString()?.trim()?.lowercase() ?: ""
                                val uRole = userSnap.child("role").value?.toString()?.trim()?.lowercase() ?: ""
                                val uName = userSnap.child("username").value?.toString()?.trim()?.lowercase() ?: ""
                                val isAnon = userSnap.child("isAnonymous").value == true ||
                                    userSnap.child("authProvider").value?.toString()?.equals("anonymous", ignoreCase = true) == true
                                if (uEmail.isBlank() || !uEmail.contains("@") || isAnon || uRole == "guest" || uRole == "anonymous" ||
                                    uName.startsWith("guest") || uName.startsWith("player (") || uName == "player") {
                                    isGuestSessionBan = true
                                }
                            }
                        } catch (_: Exception) {}
                    }

                    if (isGuestSessionBan) {
                        try { child.ref.removeValue().await() } catch (_: Exception) {}
                        try { firestore.collection("banned_users").document(key).delete().await() } catch (_: Exception) {}
                        if (targetUid.isNotBlank()) {
                            try { firestore.collection("banned_users").document(targetUid).delete().await() } catch (_: Exception) {}
                            try { database.child("users").child(targetUid).updateChildren(unbanUpdates).await() } catch (_: Exception) {}
                            try { database.child("userProfiles").child(targetUid).updateChildren(unbanUpdates).await() } catch (_: Exception) {}
                            try { database.child("players").child(targetUid).updateChildren(unbanUpdates).await() } catch (_: Exception) {}
                            try { firestore.collection("users").document(targetUid).update(unbanUpdates).await() } catch (_: Exception) {}
                            try { firestore.collection("userProfiles").document(targetUid).update(unbanUpdates).await() } catch (_: Exception) {}
                            try { firestore.collection("players").document(targetUid).update(unbanUpdates).await() } catch (_: Exception) {}
                        }
                        unbannedCount++
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "unban RTDB banned_users error: ${e.message}")
            }

            // 2. (Firestore banned_users is blocked by zero-trust firestore.rules; sanctions live on users/{uid})

            // 3. Scan all users in RTDB /users to sweep any banned user without a valid logged-in email
            try {
                val usersSnap = database.child("users").get().await()
                for (uSnap in usersSnap.children) {
                    val uid = uSnap.key ?: ""
                    val isBanned = uSnap.child("isBanned").value == true ||
                                  uSnap.child("banned").value == true ||
                                  uSnap.child("status").value?.toString()?.equals("BANNED", ignoreCase = true) == true
                    if (isBanned) {
                        val email = uSnap.child("email").value?.toString()?.trim()?.lowercase() ?: ""
                        val phone = uSnap.child("phoneNumber").value?.toString()?.trim() ?: uSnap.child("phone").value?.toString()?.trim() ?: ""
                        val role = uSnap.child("role").value?.toString()?.trim()?.lowercase() ?: ""
                        val uname = uSnap.child("username").value?.toString()?.trim()?.lowercase() ?: ""
                        val isAnon = uSnap.child("isAnonymous").value == true ||
                                     uSnap.child("authProvider").value?.toString()?.equals("anonymous", ignoreCase = true) == true ||
                                     role == "guest" || role == "anonymous"

                        if (isAnon || (!email.contains("@") && phone.isBlank()) || uname.startsWith("guest") || uname.startsWith("player (") || uname == "player") {
                            try { database.child("banned_users").child(uid).removeValue().await() } catch (_: Exception) {}
                            try { uSnap.ref.updateChildren(unbanUpdates).await() } catch (_: Exception) {}
                            try { database.child("userProfiles").child(uid).updateChildren(unbanUpdates).await() } catch (_: Exception) {}
                            try { database.child("players").child(uid).updateChildren(unbanUpdates).await() } catch (_: Exception) {}
                            try { firestore.collection("users").document(uid).set(unbanUpdates, SetOptions.merge()).await() } catch (_: Exception) {}
                            unbannedCount++
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "unban RTDB users sweep error: ${e.message}")
            }

            // 4. Sweep Firestore users
            try {
                val fsUsers = firestore.collection("users").whereEqualTo("isBanned", true).get().await()
                for (doc in fsUsers.documents) {
                    val uid = doc.id
                    val email = doc.getString("email")?.trim()?.lowercase() ?: ""
                    val phone = doc.getString("phone")?.trim() ?: doc.getString("phoneNumber")?.trim() ?: ""
                    val role = doc.getString("role")?.trim()?.lowercase() ?: ""
                    val uname = doc.getString("username")?.trim()?.lowercase() ?: ""
                    val isAnon = doc.getBoolean("isAnonymous") == true ||
                                 doc.getString("authProvider")?.equals("anonymous", ignoreCase = true) == true ||
                                 role == "guest" || role == "anonymous"

                    if (isAnon || (!email.contains("@") && phone.isBlank()) || uname.startsWith("guest") || uname.startsWith("player (") || uname == "player") {
                        try { doc.reference.update(unbanUpdates).await() } catch (_: Exception) {}
                        try { database.child("banned_users").child(uid).removeValue().await() } catch (_: Exception) {}
                        try { database.child("users").child(uid).updateChildren(unbanUpdates).await() } catch (_: Exception) {}
                        try { database.child("userProfiles").child(uid).updateChildren(unbanUpdates).await() } catch (_: Exception) {}
                        try { database.child("players").child(uid).updateChildren(unbanUpdates).await() } catch (_: Exception) {}
                        unbannedCount++
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "unban Firestore users sweep error: ${e.message}")
            }

            logAuditAction(
                action = "UNBAN_GUEST_SESSIONS",
                details = "Swept and restored unauthenticated/guest session access ($unbannedCount accounts unbanned)",
                category = "SECURITY"
            )
            GlobalErrorManager.emitSuccess("Non-logged in & guest session bans completely removed ($unbannedCount restored)!")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "unbanAllGuestAndNonLoggedInSessions: ${e.message}")
        }
        return unbannedCount
    }

    suspend fun cleanupBannedGuestSessions() {
        unbanAllGuestAndNonLoggedInSessions()
    }

    suspend fun banPlayer(
        uid: String,
        email: String = "",
        reason: String = "",
        bannedBy: String = "",
        gameId: String = ""
    ) {
        val cleanUid = uid.trim()
        val cleanEmail = email.trim().lowercase()
        val cleanGameId = gameId.trim()

        // STRICT GUARD: Never ban guest or anonymous accounts!
        if (cleanUid.startsWith("guest", ignoreCase = true) ||
            cleanUid.startsWith("anon", ignoreCase = true) ||
            cleanEmail.contains("guest") ||
            cleanEmail.contains("anonymous") ||
            (cleanUid.isBlank() && cleanEmail.isBlank() && cleanGameId.isBlank())
        ) {
            android.util.Log.w(TAG, "Blocked ban attempt on guest session: uid=$uid, email=$email")
            GlobalErrorManager.emitError("Guest/anonymous sessions cannot be banned globally as this affects unauthenticated app access.")
            return
        }

        val now = System.currentTimeMillis()
        val effectiveReason = reason.ifBlank { "Violation of tournament rules" }
        val effectiveBannedBy = bannedBy.ifBlank { activeAdminEmail.ifBlank { "anantisback47@gmail.com" } }
        val banPayload = mapOf(
            "reason" to effectiveReason,
            "bannedAt" to now,
            "bannedBy" to effectiveBannedBy,
            "permanent" to true,
            "email" to email,
            "uid" to uid,
            "gameId" to gameId
        )

        val userBanUpdates = mapOf<String, Any>(
            "isBanned" to true,
            "banned" to true,
            "status" to "BANNED",
            "accountStatus" to "BANNED",
            "active" to false,
            "isAuthorized" to false,
            "walletFrozen" to true,
            "banReason" to effectiveReason,
            "bannedAt" to now,
            "bannedBy" to effectiveBannedBy
        )

        // Primary write target: Firestore users/{uid} (Cloud Functions mirror RTDB /users, /banned_users & global_app_state)
        if (uid.isNotBlank()) {
            lastExtractedUsers[uid]?.let { cached ->
                lastExtractedUsers[uid] = cached.copy(
                    isBanned = true,
                    status = "BANNED",
                    accountStatus = "BANNED",
                    banReason = effectiveReason,
                    bannedAt = now,
                    updatedAt = now
                )
            }
            try {
                firestore.collection("users").document(uid).set(userBanUpdates, SetOptions.merge()).await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed banning user in Firestore users/$uid: ${e.message}")
            }
            try { firestore.collection("userProfiles").document(uid).set(userBanUpdates, SetOptions.merge()).await() } catch (_: Exception) {}
        }
    }

    suspend fun unbanPlayer(uid: String, email: String = "", gameId: String = "") {
        val now = System.currentTimeMillis()
        val userUnbanUpdates = mapOf<String, Any>(
            "isBanned" to false,
            "banned" to false,
            "status" to "ACTIVE",
            "accountStatus" to "ACTIVE",
            "active" to true,
            "isAuthorized" to true,
            "walletFrozen" to false,
            "matchmakingLocked" to false,
            "withdrawalsLocked" to false,
            "banReason" to "",
            "reason" to "",
            "banType" to "",
            "sanction" to "",
            "sanctionType" to "",
            "banCaseId" to "",
            "sanctionId" to "",
            "bannedAt" to 0L,
            "updatedAt" to now
        )

        // Immediately update local cache so stream emission never reverts to stale banned=true
        if (uid.isNotBlank()) {
            lastExtractedUsers[uid]?.let { cached ->
                lastExtractedUsers[uid] = cached.copy(
                    isBanned = false,
                    status = "ACTIVE",
                    accountStatus = "ACTIVE",
                    banReason = "",
                    banCaseId = "",
                    bannedAt = 0L,
                    activeAppealStatus = "RESOLVED",
                    updatedAt = now
                )
            }
        }
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isNotBlank()) {
            lastExtractedUsers.entries.forEach { (k, v) ->
                if (v.email.trim().lowercase() == cleanEmail) {
                    lastExtractedUsers[k] = v.copy(
                        isBanned = false,
                        status = "ACTIVE",
                        accountStatus = "ACTIVE",
                        banReason = "",
                        banCaseId = "",
                        bannedAt = 0L,
                        activeAppealStatus = "RESOLVED",
                        updatedAt = now
                    )
                }
            }
        }

        // Primary write target: Firestore users/{uid} (Cloud Functions clean RTDB /banned_users & mirror /users + global_app_state)
        if (uid.isNotBlank()) {
            try {
                firestore.collection("users").document(uid).set(userUnbanUpdates, SetOptions.merge()).await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed unbanning user in Firestore users/$uid: ${e.message}")
            }
            try { firestore.collection("userProfiles").document(uid).set(userUnbanUpdates, SetOptions.merge()).await() } catch (_: Exception) {}
            try { firestore.collection("players").document(uid).set(userUnbanUpdates, SetOptions.merge()).await() } catch (_: Exception) {}
        }
    }

    suspend fun isPlayerOrAccountBanned(
        uid: String? = null,
        email: String? = null,
        gameId: String? = null
    ): Boolean {
        try {
            val cleanUid = uid?.trim() ?: ""
            val cleanEmail = email?.trim()?.lowercase() ?: ""
            val cleanGameId = gameId?.trim() ?: ""

            // Guests, anonymous sessions, or blank accounts are NEVER considered banned
            if (cleanUid.isBlank() && cleanEmail.isBlank() && cleanGameId.isBlank()) return false
            if (cleanUid.startsWith("guest", ignoreCase = true) || cleanUid.startsWith("anon", ignoreCase = true)) return false
            if (cleanEmail.contains("guest") || cleanEmail.contains("anonymous")) return false

            // Direct check in banned_users table
            if (cleanUid.isNotBlank()) {
                val s1 = database.child("banned_users").child(cleanUid).get().await()
                if (s1.exists()) return true
            }
            if (cleanEmail.isNotBlank()) {
                val s2 = database.child("banned_users").child(cleanEmail.replace(".", "_")).get().await()
                if (s2.exists()) return true
            }
            if (cleanGameId.isNotBlank()) {
                val s3 = database.child("banned_users").child(cleanGameId).get().await()
                if (s3.exists()) return true
            }

            // Scan all banned_users entries
            val allBannedSnap = database.child("banned_users").get().await()
            for (child in allBannedSnap.children) {
                val k = child.key ?: ""
                val bUid = child.child("uid").value?.toString()?.trim() ?: ""
                val bEmail = child.child("email").value?.toString()?.trim()?.lowercase() ?: ""
                val bGameId = child.child("gameId").value?.toString()?.trim() ?: ""
                if (cleanUid.isNotBlank() && (k == cleanUid || bUid == cleanUid)) return true
                if (cleanEmail.isNotBlank() && (k == cleanEmail || k == cleanEmail.replace(".", "_") || bEmail == cleanEmail)) return true
                if (cleanGameId.isNotBlank() && (k == cleanGameId || bGameId == cleanGameId)) return true
            }

            // Check users node
            if (cleanUid.isNotBlank()) {
                val uSnap = database.child("users").child(cleanUid).get().await()
                if (uSnap.exists()) {
                    val isB = uSnap.child("isBanned").value == true ||
                              uSnap.child("banned").value == true ||
                              uSnap.child("status").value?.toString()?.equals("BANNED", ignoreCase = true) == true
                    if (isB) return true
                }
            }
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking ban status: ${e.message}")
            return false
        }
    }

    /**
     * Builds the comprehensive, structured database payload for a player (`accountLifecycle`,
     * `combatAndMatchSummary`, `walletAndFinanceSummary`, and `joinedMatches`) so Firebase RTDB
     * and Cloud Firestore contain dedicated sections for player join timestamps, match history,
     * kills/winnings per match, and withdrawable vs non-withdrawable wallet buckets.
     */
    fun buildStructuredPlayerDatabaseMap(user: UserProfile): Map<String, Any> {
        val now = System.currentTimeMillis()
        val firstAppJoin = user.effectiveFirstAppJoinAt.takeIf { it > 0L } ?: now
        val validJoinTimes = user.matchHistory.map { it.joinedAt }.filter { it > 0L }
        val firstMatchJoin = when {
            user.firstMatchJoinedAt > 0L && validJoinTimes.isNotEmpty() -> minOf(user.firstMatchJoinedAt, validJoinTimes.minOrNull()!!)
            validJoinTimes.isNotEmpty() -> validJoinTimes.minOrNull()!!
            else -> user.firstMatchJoinedAt
        }
        val lastMatchJoin = maxOf(user.lastMatchJoinedAt, validJoinTimes.maxOrNull() ?: 0L)
        val joinedIds = (user.joinedMatchIds + user.matchHistory.map { it.tournamentId }).filter { it.isNotBlank() }.distinct()
        val totalMatches = maxOf(user.totalMatchesJoined, joinedIds.size)
        val hasJoinedMatch = totalMatches > 0 || firstMatchJoin > 0L

        val depMoney = user.depositFunds.coerceAtLeast(0.0)
        val winMoney = user.winningFunds.coerceAtLeast(0.0)
        val bonMoney = user.bonusFunds.coerceAtLeast(0.0)
        val totalWallet = (depMoney + winMoney + bonMoney).let { sum ->
            if (sum > 0.0) sum else maxOf(user.funds, user.balance).coerceAtLeast(0.0)
        }
        val effectiveDepMoney = if (depMoney == 0.0 && winMoney == 0.0 && bonMoney == 0.0 && totalWallet > 0.0) totalWallet else depMoney

        val totalDepositedLifetime = maxOf(user.totalDeposited, effectiveDepMoney)
        val totalEntryFeesLifetime = maxOf(user.totalEntryFeesPaid, user.matchHistory.sumOf { it.entryFeePaid })
        val totalEarningsLifetime = maxOf(user.totalEarnings, winMoney, user.matchHistory.sumOf { it.winningAmount })
        val totalKillsLifetime = maxOf(user.kills, user.matchHistory.sumOf { it.kills })

        val sdf = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
        val firstAppFormatted = if (firstAppJoin > 0L) sdf.format(java.util.Date(firstAppJoin)) else "Not Recorded"
        val firstMatchFormatted = if (firstMatchJoin > 0L) sdf.format(java.util.Date(firstMatchJoin)) else "Never Joined Any Match"
        val lastMatchFormatted = if (lastMatchJoin > 0L) sdf.format(java.util.Date(lastMatchJoin)) else "Never Joined Any Match"

        val accountLifecycleSection = mapOf<String, Any>(
            "firstAppJoinAt" to firstAppJoin,
            "firstAppJoinFormatted" to firstAppFormatted,
            "firstMatchJoinedAt" to firstMatchJoin,
            "firstMatchJoinedFormatted" to firstMatchFormatted,
            "lastMatchJoinedAt" to lastMatchJoin,
            "lastMatchJoinedFormatted" to lastMatchFormatted,
            "hasJoinedAnyMatch" to hasJoinedMatch,
            "lastActiveAt" to now
        )

        val combatAndMatchSummarySection = mapOf<String, Any>(
            "hasJoinedAnyMatch" to hasJoinedMatch,
            "totalMatchesJoined" to totalMatches,
            "joinedMatchIds" to joinedIds,
            "kills" to totalKillsLifetime,
            "totalKills" to totalKillsLifetime,
            "wins" to user.wins,
            "totalWins" to user.wins,
            "supportCasesCount" to user.supportCasesCount,
            "casesFiled" to user.supportCasesCount,
            "activityPoints" to user.activityPoints
        )

        val referralBonusTotal = maxOf(user.referralBonusEarned, bonMoney)

        val walletAndFinanceSummarySection = mapOf<String, Any>(
            "depositedMoney" to effectiveDepMoney,
            "depositFunds" to effectiveDepMoney,
            "depositWithdrawable" to false,
            "winningMoney" to winMoney,
            "winningFunds" to winMoney,
            "winningWithdrawable" to true,
            "bonusMoney" to bonMoney,
            "bonusFunds" to bonMoney,
            "referralBonus" to bonMoney,
            "referral_bonus" to bonMoney,
            "referralBonusEarned" to referralBonusTotal,
            "bonusWithdrawable" to false,
            "withdrawableBalance" to winMoney,
            "nonWithdrawableBalance" to (effectiveDepMoney + bonMoney),
            "canSendWithdrawRequest" to (winMoney > 0.0 && !user.isBanned && !user.isSuspended),
            "withdrawPolicyRule" to "ONLY_WINNING_MONEY_IS_WITHDRAWABLE_DEPOSITED_AND_REFERRAL_BONUS_LOCKED",
            "totalWalletBalance" to totalWallet,
            "totalDeposited" to totalDepositedLifetime,
            "totalEarnings" to totalEarningsLifetime,
            "totalWithdrawn" to user.totalWithdrawn,
            "totalEntryFeesPaid" to totalEntryFeesLifetime
        )

        val joinedMatchesMap = user.matchHistory.associate { rec ->
            rec.tournamentId to mapOf<String, Any>(
                "tournamentId" to rec.tournamentId,
                "tournamentTitle" to rec.tournamentTitle,
                "game" to rec.game,
                "category" to rec.category,
                "format" to rec.format,
                "map" to rec.map,
                "joinedAt" to rec.joinedAt,
                "joinedAtFormatted" to (if (rec.joinedAt > 0L) sdf.format(java.util.Date(rec.joinedAt)) else "Recorded"),
                "entryFeePaid" to rec.entryFeePaid,
                "paidFromDeposit" to rec.paidFromDeposit,
                "paidFromWinning" to rec.paidFromWinning,
                "paidFromBonus" to rec.paidFromBonus,
                "kills" to rec.kills,
                "rank" to rec.rank,
                "winningAmount" to rec.winningAmount,
                "netProfit" to (rec.winningAmount - rec.entryFeePaid),
                "status" to rec.status
            )
        }

        val nextVersion = (user.version + 1).coerceAtLeast(1)
        val resolvedAccountStatus = when {
            user.isBanned -> "BANNED"
            user.isSuspended -> "SUSPENDED"
            user.deletionRequested || user.accountStatus.equals("PENDING_DELETION", true) -> "PENDING_DELETION"
            user.accountStatus.isNotBlank() -> user.accountStatus.uppercase()
            else -> "ACTIVE"
        }

        val map = mutableMapOf<String, Any>(
            "id" to user.id,
            "uid" to user.id,
            "username" to user.username,
            "name" to user.username,
            "displayName" to user.username,
            "ign" to user.ign.ifBlank { user.username },
            "inGameName" to user.ign.ifBlank { user.username },
            "email" to user.email,
            "phone" to user.phone,
            "phoneNumber" to user.phone,
            "gameId" to user.gameId,
            "freeFireId" to user.gameId,
            "bio" to user.bio,
            "socialLink" to user.socialLink,
            "avatarUrl" to (user.avatarUrl ?: "avatar_1"),
            "role" to user.role,
            "depositBalance" to effectiveDepMoney,
            "depositFunds" to effectiveDepMoney,
            "deposit_funds" to effectiveDepMoney,
            "depositedMoney" to effectiveDepMoney,
            "winningsBalance" to winMoney,
            "winningFunds" to winMoney,
            "winning_funds" to winMoney,
            "winningMoney" to winMoney,
            "bonusBalance" to bonMoney,
            "bonusFunds" to bonMoney,
            "bonus_funds" to bonMoney,
            "bonusMoney" to bonMoney,
            "lockedBalance" to user.lockedBalance,
            "referralBonus" to bonMoney,
            "referral_bonus" to bonMoney,
            "referralBonusEarned" to referralBonusTotal,
            "referralEarnings" to referralBonusTotal,
            "referralCode" to user.referralCode,
            "referredBy" to user.referredBy,
            "referralCount" to user.referralCount,
            "withdrawableBalance" to winMoney,
            "nonWithdrawableBalance" to (effectiveDepMoney + bonMoney),
            "funds" to totalWallet,
            "balance" to totalWallet,
            "walletBalance" to totalWallet,
            "wallet_balance" to totalWallet,
            "totalWalletBalance" to totalWallet,
            "tokens" to user.tokens,
            "kycVerified" to user.kycVerified,
            "dataExported" to user.dataExported,
            "lastExportedAt" to user.lastExportedAt,
            "lastExportId" to user.lastExportId,
            "wins" to user.wins,
            "kills" to totalKillsLifetime,
            "activityPoints" to user.activityPoints,
            "totalEarnings" to totalEarningsLifetime,
            "totalDeposited" to totalDepositedLifetime,
            "totalWithdrawn" to user.totalWithdrawn,
            "totalEntryFeesPaid" to totalEntryFeesLifetime,
            "supportCasesCount" to user.supportCasesCount,
            "createdAt" to firstAppJoin,
            "firstAppJoinAt" to firstAppJoin,
            "firstMatchJoinedAt" to firstMatchJoin,
            "lastMatchJoinedAt" to lastMatchJoin,
            "totalMatchesJoined" to totalMatches,
            "joinedMatchIds" to joinedIds,
            "accountLifecycle" to accountLifecycleSection,
            "combatAndMatchSummary" to combatAndMatchSummarySection,
            "walletAndFinanceSummary" to walletAndFinanceSummarySection,
            "isBanned" to user.isBanned,
            "banned" to user.isBanned,
            "status" to resolvedAccountStatus,
            "accountStatus" to resolvedAccountStatus,
            "active" to (!user.isBanned && !user.isSuspended),
            "isAuthorized" to (!user.isBanned && !user.isSuspended),
            "walletFrozen" to (user.isBanned || user.isSuspended),
            "banReason" to user.banReason,
            "banCaseId" to user.banCaseId,
            "bannedAt" to user.bannedAt,
            "isSuspended" to user.isSuspended,
            "suspendedUntil" to user.suspendedUntil,
            "suspensionEndTimeMillis" to user.suspendedUntil,
            "suspensionReason" to user.suspensionReason,
            "deletionRequested" to user.deletionRequested,
            "deletionRequestId" to user.deletionRequestId,
            "deletionReason" to user.deletionReason,
            "deletionDetails" to user.deletionDetails,
            "deletionRequestedAt" to user.deletionRequestedAt,
            "isVpnBlocked" to user.isVpnBlocked,
            "vpnBlocked" to user.isVpnBlocked,
            "isForceUpdateRequired" to user.isForceUpdateRequired,
            "forceUpdate" to user.isForceUpdateRequired,
            "minVersionRequired" to user.minVersionRequired,
            "isMaintenanceBypass" to user.isMaintenanceBypass,
            "deviceModel" to user.deviceModel,
            "ipAddress" to user.ipAddress,
            "version" to nextVersion,
            "updatedAt" to now,
            "lastActive" to now
        )
        if (joinedMatchesMap.isNotEmpty()) {
            map["joinedMatches"] = joinedMatchesMap
        }
        return map
    }

    suspend fun syncPlayerStructuredDatabaseRecord(user: UserProfile) {
        if (user.id.isBlank()) return
        val structuredMap = buildStructuredPlayerDatabaseMap(user)
        try { database.child("users").child(user.id).updateChildren(structuredMap).await() } catch (_: Exception) {}
        try { database.child("userProfiles").child(user.id).updateChildren(structuredMap).await() } catch (_: Exception) {}
        try { database.child("players").child(user.id).updateChildren(structuredMap).await() } catch (_: Exception) {}
        try { database.child("player_database_records").child(user.id).updateChildren(structuredMap).await() } catch (_: Exception) {}
        try {
            val walletOnly = structuredMap["walletAndFinanceSummary"] as? Map<String, Any> ?: emptyMap()
            database.child("wallet_accounts").child(user.id).updateChildren(walletOnly + mapOf(
                "uid" to user.id,
                "username" to user.username,
                "gameId" to user.gameId,
                "updatedAt" to System.currentTimeMillis()
            )).await()
        } catch (_: Exception) {}
        try { firestore.collection("users").document(user.id).set(structuredMap, com.google.firebase.firestore.SetOptions.merge()).await() } catch (_: Exception) {}
        try { firestore.collection("userProfiles").document(user.id).set(structuredMap, com.google.firebase.firestore.SetOptions.merge()).await() } catch (_: Exception) {}
        try { firestore.collection("player_database_records").document(user.id).set(structuredMap, com.google.firebase.firestore.SetOptions.merge()).await() } catch (_: Exception) {}
    }

    suspend fun adjustUserBalance(uid: String, newBalance: Double, reason: String, adminEmail: String) {
        try {
            val now = System.currentTimeMillis()
            val userRef = database.child("users").child(uid)
            val snap = userRef.get().await()
            val currentWinning = safeDouble(snap.child("winningFunds").value ?: snap.child("winning_funds").value)
            val currentBonus = safeDouble(snap.child("bonusFunds").value ?: snap.child("bonus_funds").value)
            val updatedDeposit = (newBalance - currentWinning - currentBonus).coerceAtLeast(0.0)
            val total = updatedDeposit + currentWinning + currentBonus

            val balanceUpdates = mapOf<String, Any>(
                "depositFunds" to updatedDeposit,
                "deposit_funds" to updatedDeposit,
                "winningFunds" to currentWinning,
                "winning_funds" to currentWinning,
                "bonusFunds" to currentBonus,
                "bonus_funds" to currentBonus,
                "withdrawableBalance" to currentWinning,
                "nonWithdrawableBalance" to (updatedDeposit + currentBonus),
                "balance" to total,
                "funds" to total,
                "walletBalance" to total,
                "wallet_balance" to total,
                "totalWalletBalance" to total,
                "lastActive" to now
            )
            try { database.child("users").child(uid).updateChildren(balanceUpdates).await() } catch (_: Exception) {}
            try { database.child("userProfiles").child(uid).updateChildren(balanceUpdates).await() } catch (_: Exception) {}
            try { database.child("players").child(uid).updateChildren(balanceUpdates).await() } catch (_: Exception) {}
            try { database.child("wallet_accounts").child(uid).updateChildren(balanceUpdates).await() } catch (_: Exception) {}

            val txId = "TX-ADJ-${now}"
            val txPayload = mapOf(
                "id" to txId,
                "uid" to uid,
                "amount" to total,
                "type" to "deposit",
                "walletBucket" to "DEPOSIT",
                "timestamp" to now,
                "status" to "completed",
                "referenceId" to "ADMIN_ADJUSTMENT",
                "description" to "Admin balance adjustment ($reason) by $adminEmail"
            )
            try { database.child("wallet_transactions").child(uid).child(txId).setValue(txPayload).await() } catch (_: Exception) {}
            try { firestore.collection("users").document(uid).set(balanceUpdates, com.google.firebase.firestore.SetOptions.merge()).await() } catch (_: Exception) {}
            try { firestore.collection("userProfiles").document(uid).set(balanceUpdates, com.google.firebase.firestore.SetOptions.merge()).await() } catch (_: Exception) {}
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to adjust balance: ${e.message}", e)
        }
    }

    suspend fun adjustUserWalletByBucket(
        user: UserProfile,
        deltaAmount: Double,
        bucket: com.example.domain.model.WalletBucket,
        reason: String,
        adminEmail: String
    ): UserProfile? {
        return try {
            val uid = user.id
            if (uid.isBlank()) return null

            val overrideBase = recentAdminWalletOverrides[uid]?.takeIf { System.currentTimeMillis() - it.second < 60_000L }?.first
            val baseUser = overrideBase ?: user

            val baseDeposit = baseUser.depositFunds.coerceAtLeast(0.0)
            val baseWinning = baseUser.winningFunds.coerceAtLeast(0.0)
            val baseBonus = baseUser.bonusFunds.coerceAtLeast(0.0)

            val newDeposit = when (bucket) {
                com.example.domain.model.WalletBucket.DEPOSIT -> (baseDeposit + deltaAmount).coerceAtLeast(0.0)
                else -> baseDeposit
            }
            val newWinning = when (bucket) {
                com.example.domain.model.WalletBucket.WINNING -> (baseWinning + deltaAmount).coerceAtLeast(0.0)
                else -> baseWinning
            }
            val newBonus = when (bucket) {
                com.example.domain.model.WalletBucket.BONUS -> (baseBonus + deltaAmount).coerceAtLeast(0.0)
                else -> baseBonus
            }
            val newTotal = newDeposit + newWinning + newBonus

            val newTotalDeposited = if (bucket == com.example.domain.model.WalletBucket.DEPOSIT && deltaAmount > 0.0) {
                maxOf(baseUser.totalDeposited, baseDeposit) + deltaAmount
            } else {
                maxOf(baseUser.totalDeposited, newDeposit)
            }
            val newTotalEarnings = if (bucket == com.example.domain.model.WalletBucket.WINNING && deltaAmount > 0.0) {
                maxOf(baseUser.totalEarnings, baseWinning) + deltaAmount
            } else {
                maxOf(baseUser.totalEarnings, newWinning)
            }
            val newReferralBonusEarned = if (bucket == com.example.domain.model.WalletBucket.BONUS && deltaAmount > 0.0) {
                maxOf(baseUser.referralBonusEarned, baseBonus) + deltaAmount
            } else {
                maxOf(baseUser.referralBonusEarned, newBonus)
            }

            val now = System.currentTimeMillis()
            val updatedUser = baseUser.copy(
                depositFunds = newDeposit,
                winningFunds = newWinning,
                bonusFunds = newBonus,
                referralBonusEarned = newReferralBonusEarned,
                funds = newTotal,
                balance = newTotal,
                totalDeposited = newTotalDeposited,
                totalEarnings = newTotalEarnings,
                lastActive = now
            )

            recentAdminWalletOverrides[uid] = updatedUser to now
            syncPlayerStructuredDatabaseRecord(updatedUser)

            val txId = "TX-BUCKET-${bucket.name}-${now}"
            val sign = if (deltaAmount >= 0) "+" else ""
            val txPayload = mapOf(
                "id" to txId,
                "uid" to uid,
                "userId" to uid,
                "amount" to deltaAmount,
                "walletBucket" to bucket.name,
                "withdrawable" to bucket.isWithdrawable,
                "type" to if (deltaAmount >= 0) "ADMIN_${bucket.name}_CREDIT" else "ADMIN_${bucket.name}_DEBIT",
                "status" to "COMPLETED",
                "timestamp" to now,
                "referenceId" to "ADMIN_${bucket.name}_ADJUSTMENT",
                "description" to "${bucket.displayName}: ${sign}₹${deltaAmount.toInt()} ($reason) by $adminEmail [New Balances -> Dep: ₹${newDeposit.toInt()}, Win: ₹${newWinning.toInt()}, RefBonus: ₹${newBonus.toInt()}]"
            )
            try { database.child("wallet_transactions").child(uid).child(txId).setValue(txPayload).await() } catch (_: Exception) {}
            try { database.child("transactions").child(txId).setValue(txPayload).await() } catch (_: Exception) {}
            try { firestore.collection("transactions").document(txId).set(txPayload).await() } catch (_: Exception) {}

            logAuditAction(
                action = "WALLET_BUCKET_ADJUST",
                details = "Adjusted ${updatedUser.username} (${bucket.displayName}) by ${sign}₹${deltaAmount.toInt()}: $reason. New Dep=₹${newDeposit.toInt()}, Win=₹${newWinning.toInt()}, RefBonus=₹${newBonus.toInt()}",
                targetUid = uid,
                category = "FINANCE"
            )
            GlobalErrorManager.emitSuccess("${sign}₹${deltaAmount.toInt()} sent to ${updatedUser.username}'s ${bucket.shortLabel}!")
            updatedUser
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to adjust ${bucket.displayName}: ${e.message}", e)
            null
        }
    }

    suspend fun recordPlayerMatchStatsAndWinning(
        user: UserProfile,
        tournamentId: String,
        tournamentTitle: String,
        kills: Int,
        rank: Int,
        winningAmount: Double,
        entryFeePaid: Double,
        adminEmail: String
    ): UserProfile? {
        return try {
            val now = System.currentTimeMillis()
            val cleanTourneyId = tournamentId.trim().ifBlank { "MATCH_${now}" }
            val existingRec = user.matchHistory.find { it.tournamentId == cleanTourneyId }
            val previousWinningForMatch = existingRec?.winningAmount ?: 0.0
            val previousKillsForMatch = existingRec?.kills ?: 0

            val winningDelta = (winningAmount - previousWinningForMatch).coerceAtLeast(0.0)
            val killsDelta = (kills - previousKillsForMatch).coerceAtLeast(0)

            val joinedTime = existingRec?.joinedAt?.takeIf { it > 0L } ?: now
            val newRec = com.example.domain.model.PlayerMatchRecord(
                tournamentId = cleanTourneyId,
                tournamentTitle = tournamentTitle.ifBlank { existingRec?.tournamentTitle ?: "Match #$cleanTourneyId" },
                game = existingRec?.game ?: "Free Fire",
                category = existingRec?.category ?: "BR",
                format = existingRec?.format ?: "SOLO",
                map = existingRec?.map ?: "Bermuda",
                joinedAt = joinedTime,
                entryFeePaid = if (entryFeePaid >= 0.0) entryFeePaid else (existingRec?.entryFeePaid ?: 0.0),
                paidFromDeposit = existingRec?.paidFromDeposit ?: entryFeePaid.coerceAtLeast(0.0),
                paidFromWinning = existingRec?.paidFromWinning ?: 0.0,
                paidFromBonus = existingRec?.paidFromBonus ?: 0.0,
                kills = kills.coerceAtLeast(0),
                rank = rank.coerceAtLeast(0),
                winningAmount = winningAmount.coerceAtLeast(0.0),
                status = if (winningAmount > 0.0) "WON" else "COMPLETED"
            )

            val updatedHistory = (user.matchHistory.filterNot { it.tournamentId == cleanTourneyId } + newRec)
                .sortedByDescending { it.joinedAt }
            val updatedJoinedIds = (user.joinedMatchIds + cleanTourneyId).distinct()

            val newWinningFunds = (user.winningFunds + winningDelta).coerceAtLeast(0.0)
            val newTotalWallet = user.depositFunds + newWinningFunds + user.bonusFunds
            val newTotalEarnings = maxOf(user.totalEarnings + winningDelta, updatedHistory.sumOf { it.winningAmount })
            val newTotalKills = maxOf(user.kills + killsDelta, updatedHistory.sumOf { it.kills })
            val newWins = if (rank == 1 && (existingRec?.rank ?: 0) != 1) user.wins + 1 else user.wins

            val updatedUser = user.copy(
                winningFunds = newWinningFunds,
                funds = newTotalWallet,
                balance = newTotalWallet,
                totalEarnings = newTotalEarnings,
                kills = newTotalKills,
                wins = newWins,
                firstMatchJoinedAt = if (user.firstMatchJoinedAt > 0L) minOf(user.firstMatchJoinedAt, joinedTime) else joinedTime,
                lastMatchJoinedAt = maxOf(user.lastMatchJoinedAt, joinedTime),
                totalMatchesJoined = maxOf(user.totalMatchesJoined, updatedJoinedIds.size),
                joinedMatchIds = updatedJoinedIds,
                matchHistory = updatedHistory,
                lastActive = now
            )

            syncPlayerStructuredDatabaseRecord(updatedUser)

            val matchNodeMap = mapOf<String, Any>(
                "tournamentId" to newRec.tournamentId,
                "tournamentTitle" to newRec.tournamentTitle,
                "game" to newRec.game,
                "category" to newRec.category,
                "format" to newRec.format,
                "map" to newRec.map,
                "joinedAt" to newRec.joinedAt,
                "entryFeePaid" to newRec.entryFeePaid,
                "paidFromDeposit" to newRec.paidFromDeposit,
                "paidFromWinning" to newRec.paidFromWinning,
                "paidFromBonus" to newRec.paidFromBonus,
                "kills" to newRec.kills,
                "rank" to newRec.rank,
                "winningAmount" to newRec.winningAmount,
                "netProfit" to (newRec.winningAmount - newRec.entryFeePaid),
                "status" to newRec.status,
                "updatedAt" to now,
                "updatedBy" to adminEmail
            )
            try { database.child("users").child(user.id).child("joinedMatches").child(cleanTourneyId).setValue(matchNodeMap).await() } catch (_: Exception) {}
            try { database.child("user_match_history").child(user.id).child(cleanTourneyId).setValue(matchNodeMap).await() } catch (_: Exception) {}
            try { database.child("tournaments").child(cleanTourneyId).child("participants").child(user.id).updateChildren(matchNodeMap).await() } catch (_: Exception) {}
            try { firestore.collection("users").document(user.id).collection("joinedMatches").document(cleanTourneyId).set(matchNodeMap).await() } catch (_: Exception) {}

            if (winningDelta > 0.0) {
                val txId = "TX-MATCH-WIN-${now}"
                val txPayload = mapOf(
                    "id" to txId,
                    "uid" to user.id,
                    "userId" to user.id,
                    "amount" to winningDelta,
                    "walletBucket" to "WINNING",
                    "withdrawable" to true,
                    "type" to "MATCH_WINNING",
                    "status" to "COMPLETED",
                    "timestamp" to now,
                    "referenceId" to cleanTourneyId,
                    "description" to "Match Winning credited to Withdrawable Winning Balance for ${newRec.tournamentTitle} (Rank #${newRec.rank}, ${newRec.kills} Kills)"
                )
                try { database.child("wallet_transactions").child(user.id).child(txId).setValue(txPayload).await() } catch (_: Exception) {}
                try { database.child("transactions").child(txId).setValue(txPayload).await() } catch (_: Exception) {}
            }

            GlobalErrorManager.emitSuccess("Match stats & ₹${winningAmount.toInt()} Winning Money recorded for ${user.username}!")
            updatedUser
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to record match result: ${e.message}", e)
            null
        }
    }

    suspend fun updateRoomCredentials(tournamentId: String, roomId: String, roomPassword: String) {
        try {
            val now = System.currentTimeMillis()
            val roomDetails = RoomDetails(
                roomId = roomId,
                roomPassword = roomPassword,
                updatedAt = now
            )
            val credUpdates = mapOf<String, Any>(
                "roomId" to roomId,
                "room_id" to roomId,
                "roomPass" to roomPassword,
                "roomPassword" to roomPassword,
                "room_password" to roomPassword,
                "password" to roomPassword,
                "roomDetails" to mapOf(
                    "roomId" to roomId,
                    "room_id" to roomId,
                    "roomPass" to roomPassword,
                    "roomPassword" to roomPassword,
                    "room_password" to roomPassword,
                    "password" to roomPassword,
                    "updatedAt" to now
                )
            )
            // Primary write target: Firestore tournaments/{id} (Cloud Functions mirror RTDB & global_app_state)
            firestore.collection("tournaments").document(tournamentId).set(credUpdates, SetOptions.merge()).await()
            GlobalErrorManager.emitSuccess("Room credentials updated in Firestore and syncing to players.")
        } catch (e: Exception) {
            e.printStackTrace()
            GlobalErrorManager.emitError("Failed to update room credentials: ${e.message}", e)
        }
    }

    suspend fun getLiveComplaintsStream(): Flow<List<ComplaintTicket>> = callbackFlow {
        getLiveSupportTicketsStream().collect { tickets ->
            trySend(tickets)
        }
    }

    suspend fun saveComplaint(ticket: ComplaintTicket) {
        updateSupportTicket(ticket)
    }

    suspend fun getLiveTokensStream(): Flow<List<CheckInToken>> = callbackFlow {
        val tokensMap = mutableMapOf<String, CheckInToken>()

        fun emitTokens() {
            trySend(tokensMap.values.toList().sortedByDescending { it.createdAt })
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    if (!child.hasChildren() || child.value !is Map<*, *>) continue
                    val t = child.safeGetObject<CheckInToken>()
                    if (t != null && t.id.isNotBlank()) tokensMap[t.id] = t
                }
                emitTokens()
            }
            override fun onCancelled(error: DatabaseError) {
                emitTokens()
            }
        }
        val ref = database.child("tokens")
        ref.addValueEventListener(listener)

        val fsListener = try {
            firestore.collection("tokens").addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    snapshot.documents.mapNotNull { it.toObject(CheckInToken::class.java) }.forEach { t ->
                        if (t.id.isNotBlank()) tokensMap[t.id] = t
                    }
                    emitTokens()
                }
            }
        } catch (_: Exception) {
            null
        }

        awaitClose {
            ref.removeEventListener(listener)
            fsListener?.remove()
        }
    }

    suspend fun saveToken(token: CheckInToken) {
        try {
            firestore.collection("tokens").document(token.id).set(token).await()
            try { database.child("tokens").child(token.id).setValue(token).await() } catch (_: Exception) {}
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getLiveBannersStream(): Flow<List<AppAnnouncementBanner>> = callbackFlow {
        val fsBanners = mutableMapOf<String, AppAnnouncementBanner>()
        val rtdbBanners = mutableMapOf<String, AppAnnouncementBanner>()

        fun emitCombined() {
            val merged = LinkedHashMap<String, AppAnnouncementBanner>()
            rtdbBanners.forEach { (k, v) -> merged[k] = v }
            fsBanners.forEach { (k, v) -> merged[k] = v }
            trySend(merged.values.sortedByDescending { it.createdAt })
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                rtdbBanners.clear()
                for (child in snapshot.children) {
                    if (!child.hasChildren() || child.value !is Map<*, *>) continue
                    val banner = child.safeGetObject<AppAnnouncementBanner>()
                    if (banner != null) {
                        val key = banner.id.ifBlank { child.key ?: "" }
                        if (key.isNotBlank()) rtdbBanners[key] = banner.copy(id = key)
                    }
                }
                emitCombined()
            }
            override fun onCancelled(error: DatabaseError) {
                emitCombined()
            }
        }
        val ref = database.child("banners")
        ref.addValueEventListener(listener)

        val fsListener = try {
            firestore.collection("banners").addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    fsBanners.clear()
                    snapshot.documents.forEach { doc ->
                        val banner = try {
                            doc.toObject(AppAnnouncementBanner::class.java)?.copy(id = doc.id)
                        } catch (_: Exception) {
                            null
                        } ?: AppAnnouncementBanner(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            subtitle = doc.getString("subtitle") ?: doc.getString("description") ?: doc.getString("content") ?: "",
                            content = doc.getString("content") ?: doc.getString("description") ?: doc.getString("subtitle") ?: "",
                            bannerType = doc.getString("bannerType") ?: doc.getString("badgeText") ?: "ANNOUNCEMENT",
                            isActive = doc.getBoolean("isActive") ?: doc.getBoolean("active") ?: true,
                            createdAt = safeLong(doc.get("createdAt"), System.currentTimeMillis())
                        )
                        if (banner.id.isNotBlank()) fsBanners[banner.id] = banner
                    }
                    emitCombined()
                }
            }
        } catch (_: Exception) {
            null
        }

        awaitClose {
            ref.removeEventListener(listener)
            fsListener?.remove()
        }
    }

    suspend fun getLiveAuditLogsStream(): Flow<List<com.example.domain.model.AuditLogEntry>> = callbackFlow {
        val logsMap = mutableMapOf<String, com.example.domain.model.AuditLogEntry>()
        fun emitLogs() {
            trySend(logsMap.values.toList().sortedByDescending { it.timestamp })
        }

        fun parseAuditSnapshot(snapshot: DataSnapshot) {
            for (child in snapshot.children) {
                if (!child.hasChildren() || child.value !is Map<*, *>) continue
                val id = child.key ?: ""
                val action = child.child("action").value?.toString() ?: ""
                val performedBy = child.child("performedBy").value?.toString() ?: "Admin"
                val targetUid = child.child("targetUid").value?.toString() ?: ""
                val details = child.child("details").value?.toString() ?: ""
                val category = child.child("category").value?.toString() ?: "SYSTEM"
                val timestamp = safeLong(child.child("timestamp").value, System.currentTimeMillis())
                if (action.isNotBlank()) {
                    logsMap[id] = com.example.domain.model.AuditLogEntry(
                        id = id,
                        action = action,
                        performedBy = performedBy,
                        targetUid = targetUid,
                        details = details,
                        category = category,
                        timestamp = timestamp
                    )
                }
            }
            emitLogs()
        }

        // Fast initial query
        database.child("audit_logs").get().addOnSuccessListener { snapshot ->
            parseAuditSnapshot(snapshot)
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                parseAuditSnapshot(snapshot)
            }
            override fun onCancelled(error: DatabaseError) {
                emitLogs()
            }
        }

        val ref = database.child("audit_logs")
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun logAuditAction(
        action: String,
        details: String,
        targetUid: String = "",
        category: String = "SYSTEM"
    ) {
        try {
            val logId = "LOG_${System.currentTimeMillis()}_${(100..999).random()}"
            val currUser = auth.currentUser
            val performedBy = currUser?.email ?: currUser?.displayName ?: "Super Admin"
            val entry = com.example.domain.model.AuditLogEntry(
                id = logId,
                action = action,
                performedBy = performedBy,
                targetUid = targetUid,
                details = details,
                category = category,
                timestamp = System.currentTimeMillis()
            )
            firestore.collection("audit_logs").document(logId).set(entry).await()
        } catch (_: Exception) {}
    }

    suspend fun saveBanner(banner: AppAnnouncementBanner) {
        try {
            val bId = banner.id.ifBlank { "banner_${System.currentTimeMillis()}" }
            val bannerMap = mapOf(
                "id" to bId,
                "title" to banner.title,
                "subtitle" to banner.content,
                "description" to banner.content,
                "content" to banner.content,
                "bannerType" to banner.bannerType,
                "badgeText" to banner.bannerType,
                "actionType" to "ANNOUNCEMENT",
                "active" to banner.isActive,
                "isActive" to banner.isActive,
                "order" to 1,
                "createdAt" to banner.createdAt,
                "updatedAt" to System.currentTimeMillis(),
                "ctaText" to "View Details"
            )
            // Primary write target: Firestore banners/{id} (Cloud Functions mirror RTDB & global_app_state)
            firestore.collection("banners").document(bId).set(bannerMap, SetOptions.merge()).await()
            GlobalErrorManager.emitSuccess("Banner '${banner.title}' saved to Firestore & syncing to user app.")
        } catch (e: Exception) {
            e.printStackTrace()
            GlobalErrorManager.emitError("Failed to save banner: ${e.message}")
        }
    }

    suspend fun setSystemMaintenance(
        isMaintenance: Boolean,
        message: String = "Velorix Tournaments is currently undergoing maintenance. Please check back shortly!",
        title: String = "Under Maintenance",
        until: String = "Shortly"
    ) {
        val payload = mapOf(
            "isMaintenance" to isMaintenance,
            "is_maintenance" to isMaintenance,
            "maintenance" to isMaintenance,
            "maintenanceMode" to isMaintenance,
            "maintenanceTitle" to title,
            "maintenance_title" to title,
            "maintenanceMessage" to message,
            "maintenance_message" to message,
            "maintenanceUntil" to until,
            "updatedAt" to System.currentTimeMillis()
        )
        // Primary write target: Firestore system_config (Cloud Functions mirror RTDB & global_app_state)
        try {
            firestore.collection("system_config").document("global").set(payload, SetOptions.merge()).await()
            try { firestore.collection("system_config").document("config").set(payload, SetOptions.merge()).await() } catch (_: Exception) {}
            try { firestore.collection("system_config").document("app_settings").set(payload, SetOptions.merge()).await() } catch (_: Exception) {}
            GlobalErrorManager.emitSuccess(if (isMaintenance) "Emergency Maintenance Lock Activated" else "Maintenance Mode Disabled - App is Live")
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to update maintenance state: ${e.message}")
        }
    }

    suspend fun updateUserProfile(user: UserProfile) {
        if (user.isGuestSession && user.isBanned) {
            GlobalErrorManager.emitError("Action blocked: Guest sessions cannot be banned.")
            return
        }
        try {
            if (user.isBanned) {
                try {
                    banPlayer(
                        uid = user.id,
                        email = user.email,
                        reason = user.banReason.ifBlank { "Administrative ban" },
                        bannedBy = activeAdminEmail,
                        gameId = user.gameId
                    )
                } catch (_: Exception) {}
            } else {
                try {
                    unbanPlayer(
                        uid = user.id,
                        email = user.email,
                        gameId = user.gameId
                    )
                } catch (_: Exception) {}
            }

            syncPlayerStructuredDatabaseRecord(user)

            logAuditAction(
                action = "UPDATE_USER_CONTROLS",
                details = "Updated profile & structured telemetry for ${user.username} (${user.id}): Win=₹${user.winningFunds.toInt()}, Dep=₹${user.depositFunds.toInt()}, Bon=₹${user.bonusFunds.toInt()}, Matches=${user.totalMatchesJoined}",
                targetUid = user.id,
                category = "USER_MANAGEMENT"
            )
            GlobalErrorManager.emitSuccess("User profile & structured database sections saved to Firebase!")
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to update user profile: ${e.message}", e)
        }
    }

    suspend fun adjust2DWalletBalance(
        uid: String,
        depositDelta: Double,
        winningDelta: Double,
        bonusDelta: Double,
        reason: String,
        adminEmail: String
    ) {
        try {
            val userRef = database.child("users").child(uid)
            val snap = userRef.get().await()
            val currentDeposit = safeDouble(snap.child("depositFunds").value ?: snap.child("deposit_funds").value)
            val currentWinning = safeDouble(snap.child("winningFunds").value ?: snap.child("winning_funds").value)
            val currentBonus = safeDouble(snap.child("bonusFunds").value ?: snap.child("bonus_funds").value)
            val currentLegacy = safeDouble(snap.child("balance").value ?: snap.child("funds").value)

            val baseDeposit = if (currentDeposit == 0.0 && currentWinning == 0.0 && currentBonus == 0.0 && currentLegacy > 0.0) currentLegacy else currentDeposit
            val newDeposit = (baseDeposit + depositDelta).coerceAtLeast(0.0)
            val newWinning = (currentWinning + winningDelta).coerceAtLeast(0.0)
            val newBonus = (currentBonus + bonusDelta).coerceAtLeast(0.0)
            val newTotal = newDeposit + newWinning + newBonus
            val now = System.currentTimeMillis()

            val updates = mapOf<String, Any>(
                "depositFunds" to newDeposit,
                "deposit_funds" to newDeposit,
                "winningFunds" to newWinning,
                "winning_funds" to newWinning,
                "bonusFunds" to newBonus,
                "bonus_funds" to newBonus,
                "withdrawableBalance" to newWinning,
                "nonWithdrawableBalance" to (newDeposit + newBonus),
                "funds" to newTotal,
                "balance" to newTotal,
                "walletBalance" to newTotal,
                "lastActive" to now
            )
            userRef.updateChildren(updates).await()
            database.child("userProfiles").child(uid).updateChildren(updates).await()
            database.child("players").child(uid).updateChildren(updates).await()
            database.child("wallet_accounts").child(uid).updateChildren(updates).await()
            try {
                firestore.collection("users").document(uid).set(updates, com.google.firebase.firestore.SetOptions.merge()).await()
            } catch (ignored: Exception) {}

            val txId = "TX-ADJ-2D-${now}"
            val totalDelta = depositDelta + winningDelta + bonusDelta
            val txPayload = mapOf(
                "id" to txId,
                "uid" to uid,
                "amount" to totalDelta,
                "type" to if (totalDelta >= 0) "CREDIT_ADJUSTMENT" else "DEBIT_ADJUSTMENT",
                "status" to "COMPLETED",
                "timestamp" to now,
                "referenceId" to "ADMIN_2D_ADJUSTMENT",
                "description" to "3-Bucket Wallet adjusted [Dep: ₹$depositDelta, Win: ₹$winningDelta, Bon: ₹$bonusDelta] Reason: $reason (by $adminEmail)"
            )
            database.child("wallet_transactions").child(uid).child(txId).setValue(txPayload).await()
            GlobalErrorManager.emitSuccess("Wallet buckets adjusted successfully.")
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to adjust wallet buckets: ${e.message}", e)
        }
    }

    suspend fun processReferralReward(
        refereeUid: String,
        referralCode: String,
        referrerBonus: Double = 25.0,
        refereeBonus: Double = 25.0
    ): Result<String> {
        return try {
            val cleanCode = referralCode.trim().uppercase()
            if (cleanCode.isBlank()) return Result.failure(Exception("Referral code cannot be empty"))

            // Find referrer by referralCode in users
            val usersSnap = database.child("users").get().await()
            var referrerUid: String? = null
            var referrerName: String = "Friend"

            for (child in usersSnap.children) {
                val code = child.child("referralCode").value?.toString() 
                    ?: child.child("referral_code").value?.toString()
                    ?: child.child("username").value?.toString()?.uppercase()
                val uid = child.key ?: ""
                if (code?.equals(cleanCode, ignoreCase = true) == true && uid.isNotEmpty()) {
                    referrerUid = uid
                    referrerName = child.child("username").value?.toString() ?: "Friend"
                    break
                }
            }

            if (referrerUid == null) {
                return Result.failure(Exception("Invalid referral code. No user found with code $cleanCode."))
            }

            if (referrerUid == refereeUid) {
                return Result.failure(Exception("You cannot use your own referral code."))
            }

            // Check if referee already used a referral
            val refereeSnap = database.child("users").child(refereeUid).get().await()
            val existingRef = refereeSnap.child("referredBy").value?.toString() ?: ""
            if (existingRef.isNotEmpty()) {
                return Result.failure(Exception("You have already claimed a referral bonus."))
            }

            val now = System.currentTimeMillis()

            // 1. Credit referee bonus
            val refBonus = safeDouble(refereeSnap.child("bonusFunds").value ?: refereeSnap.child("bonus_funds").value)
            val refDep = safeDouble(refereeSnap.child("depositFunds").value ?: refereeSnap.child("deposit_funds").value)
            val refWin = safeDouble(refereeSnap.child("winningFunds").value ?: refereeSnap.child("winning_funds").value)
            val newRefBonus = refBonus + refereeBonus
            val newRefTotal = refDep + refWin + newRefBonus

            val refereeUpdates = mapOf<String, Any>(
                "bonusFunds" to newRefBonus,
                "bonus_funds" to newRefBonus,
                "balance" to newRefTotal,
                "funds" to newRefTotal,
                "referredBy" to referrerUid
            )
            database.child("users").child(refereeUid).updateChildren(refereeUpdates).await()
            database.child("userProfiles").child(refereeUid).updateChildren(refereeUpdates).await()
            try { firestore.collection("users").document(refereeUid).update(refereeUpdates).await() } catch (ignored: Exception) {}

            val refTxId = "TX-REF-REWARD-${now}"
            val refTxPayload = mapOf(
                "id" to refTxId,
                "uid" to refereeUid,
                "amount" to refereeBonus,
                "type" to "BONUS",
                "status" to "COMPLETED",
                "timestamp" to now,
                "referenceId" to "REFERRAL_SIGNUP_BONUS",
                "description" to "Welcome bonus for joining via referral code $cleanCode (referred by $referrerName)"
            )
            database.child("wallet_transactions").child(refereeUid).child(refTxId).setValue(refTxPayload).await()

            // 2. Credit referrer bonus & increment referralCount
            val referrerSnap = database.child("users").child(referrerUid).get().await()
            val rBonus = safeDouble(referrerSnap.child("bonusFunds").value ?: referrerSnap.child("bonus_funds").value)
            val rDep = safeDouble(referrerSnap.child("depositFunds").value ?: referrerSnap.child("deposit_funds").value)
            val rWin = safeDouble(referrerSnap.child("winningFunds").value ?: referrerSnap.child("winning_funds").value)
            val rCount = safeInt(referrerSnap.child("referralCount").value ?: referrerSnap.child("referrals").value) + 1
            val rEarned = safeDouble(referrerSnap.child("referralBonusEarned").value ?: referrerSnap.child("referral_earnings").value) + referrerBonus
            val newRBonus = rBonus + referrerBonus
            val newRTotal = rDep + rWin + newRBonus

            val referrerUpdates = mapOf<String, Any>(
                "bonusFunds" to newRBonus,
                "bonus_funds" to newRBonus,
                "balance" to newRTotal,
                "funds" to newRTotal,
                "referralCount" to rCount,
                "referralBonusEarned" to rEarned
            )
            database.child("users").child(referrerUid).updateChildren(referrerUpdates).await()
            database.child("userProfiles").child(referrerUid).updateChildren(referrerUpdates).await()
            try { firestore.collection("users").document(referrerUid).update(referrerUpdates).await() } catch (ignored: Exception) {}

            val rTxId = "TX-REF-COMMISSION-${now}"
            val rTxPayload = mapOf(
                "id" to rTxId,
                "uid" to referrerUid,
                "amount" to referrerBonus,
                "type" to "BONUS",
                "status" to "COMPLETED",
                "timestamp" to now,
                "referenceId" to "REFERRAL_INVITE_REWARD",
                "description" to "Referral reward: Player ${refereeSnap.child("username").value ?: "User"} joined using your code!"
            )
            database.child("wallet_transactions").child(referrerUid).child(rTxId).setValue(rTxPayload).await()

            GlobalErrorManager.emitSuccess("Referral bonus ₹$refereeBonus claimed successfully!")
            Result.success("Success! ₹$refereeBonus bonus added to your wallet.")
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Referral processing failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteUserProfile(uid: String) {
        locallyDeletedUserIds.add(uid)
        try {
            try { database.child("users").child(uid).removeValue().await() } catch (_: Exception) {}
            try { database.child("userProfiles").child(uid).removeValue().await() } catch (_: Exception) {}
            try { database.child("players").child(uid).removeValue().await() } catch (_: Exception) {}
            try { database.child("banned_users").child(uid).removeValue().await() } catch (_: Exception) {}
            try { database.child("admins").child(uid).removeValue().await() } catch (_: Exception) {}
            try { database.child("payout_requests").child(uid).removeValue().await() } catch (_: Exception) {}
            try { database.child("withdrawals").child(uid).removeValue().await() } catch (_: Exception) {}
            try { database.child("wallet_transactions").child(uid).removeValue().await() } catch (_: Exception) {}
            try { firestore.collection("users").document(uid).delete().await() } catch (ignored: Exception) {}
            try { firestore.collection("userProfiles").document(uid).delete().await() } catch (ignored: Exception) {}
            try { firestore.collection("players").document(uid).delete().await() } catch (ignored: Exception) {}
            GlobalErrorManager.emitSuccess("User profile removed from database")
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to delete user: ${e.message}", e)
        }
    }

    // ==========================================
    // BACKEND ENGINE: TOURNAMENT REGISTRATION & SLOTS
    // ==========================================

    suspend fun joinTournament(
        tournamentId: String,
        player: PlayerRegistration,
        entryFee: Float
    ): Boolean {
        try {
            val tourneyRef = database.child("tournaments").child(tournamentId)
            val tourneySnap = tourneyRef.get().await()
            val maxSlots = safeInt(tourneySnap.child("maxPlayers").value, 48)
            var currentCount = safeInt(tourneySnap.child("registeredPlayers").value, 0)

            // Check if slots full
            if (currentCount >= maxSlots) {
                GlobalErrorManager.emitError("Registration failed: Tournament slots are full ($maxSlots/$maxSlots)")
                return false
            }

            // Comprehensive multi-factor ban check (UID, Email, Game ID)
            val playerUid = player.userId.ifBlank { player.playerId.ifBlank { player.id } }
            val playerEmail = player.profile?.email ?: ""
            val playerGameId = player.gameId.ifBlank { player.gameAccountId ?: "" }
            val isBanned = isPlayerOrAccountBanned(
                uid = playerUid,
                email = playerEmail,
                gameId = playerGameId
            )
            val isProfileBanned = player.profile?.isBanned == true
            if (isBanned || isProfileBanned) {
                val displayName = player.playerName.ifBlank { player.gameUsername.ifBlank { playerUid } }
                GlobalErrorManager.emitError("Registration blocked: Player ($displayName) has been permanently banned from tournaments.")
                return false
            }

            // Legal Age-Gating Security Enforcement: Minors (<18) can only join Training/Scrim matches!
            val prizePoolVal = safeDouble(tourneySnap.child("prizePool").value)
            val isMoneyTournament = (entryFee > 0f) || (prizePoolVal > 0.0)
            if (isMoneyTournament) {
                val userRefForAge = database.child("users").child(playerUid)
                val userSnapForAge = userRefForAge.get().await()
                val isMinorInDb = userSnapForAge.child("isUnder18").value as? Boolean
                val ageInDb = safeInt(userSnapForAge.child("age").value, 0)
                val isMinorFromAge = if (ageInDb in 1..17) true else false
                val isMinorFromPlayerObj = (player.age != null && player.age!! in 1..17)
                val isMinorFromProfile = player.profile?.isUnder18 == true

                val isPlayerMinor = (isMinorInDb == true) || isMinorFromAge || isMinorFromPlayerObj || isMinorFromProfile

                if (isPlayerMinor) {
                    val displayName = player.playerName.ifBlank { player.gameUsername.ifBlank { "Player" } }
                    val err = "Legal Age Restriction: $displayName is under 18 years old. Minors are permitted to join Free Training & Scrim matches only. Participation in real-money tournaments (Entry ₹$entryFee / Prize ₹$prizePoolVal) requires age 18+."
                    GlobalErrorManager.emitError(err)
                    return false
                }
            }

            val nowJoin = System.currentTimeMillis()
            val userId = player.userId.ifBlank { player.playerId.ifBlank { player.id } }
            val userRef = database.child("users").child(userId)
            val userSnap = userRef.get().await()

            var bonusToDeduct = 0.0
            var depositToDeduct = 0.0
            var winningToDeduct = 0.0

            // If entry fee > 0, safely deduct from 3-bucket wallet (Bonus -> Deposit -> Winnings)
            if (entryFee > 0f) {
                val depositFunds = safeDouble(userSnap.child("depositFunds").value ?: userSnap.child("deposit_funds").value)
                val winningFunds = safeDouble(userSnap.child("winningFunds").value ?: userSnap.child("winning_funds").value)
                val bonusFunds = safeDouble(userSnap.child("bonusFunds").value ?: userSnap.child("bonus_funds").value)
                val legacyFunds = safeDouble(userSnap.child("balance").value ?: userSnap.child("funds").value)

                val totalAvailable = if ((depositFunds + winningFunds + bonusFunds) > 0.0) {
                    depositFunds + winningFunds + bonusFunds
                } else {
                    legacyFunds
                }

                if (totalAvailable < entryFee) {
                    GlobalErrorManager.emitError("Insufficient wallet balance (₹$totalAvailable). Entry fee is ₹$entryFee.")
                    return false
                }

                var remaining = entryFee.toDouble()

                bonusToDeduct = minOf(bonusFunds, remaining)
                val newBonus = bonusFunds - bonusToDeduct
                remaining -= bonusToDeduct

                depositToDeduct = if (depositFunds > 0.0) minOf(depositFunds, remaining) else minOf(legacyFunds, remaining)
                val newDeposit = (depositFunds - depositToDeduct).coerceAtLeast(0.0)
                remaining -= depositToDeduct

                winningToDeduct = minOf(winningFunds, remaining)
                val newWinning = (winningFunds - winningToDeduct).coerceAtLeast(0.0)
                remaining -= winningToDeduct

                val newTotal = (newDeposit + newWinning + newBonus).coerceAtLeast(0.0)
                val prevEntryFeesPaid = safeDouble(userSnap.child("totalEntryFeesPaid").value ?: userSnap.child("walletAndFinanceSummary").child("totalEntryFeesPaid").value)
                val newEntryFeesPaid = prevEntryFeesPaid + entryFee.toDouble()

                val updates = mapOf<String, Any>(
                    "depositFunds" to newDeposit,
                    "deposit_funds" to newDeposit,
                    "winningFunds" to newWinning,
                    "winning_funds" to newWinning,
                    "bonusFunds" to newBonus,
                    "bonus_funds" to newBonus,
                    "withdrawableBalance" to newWinning,
                    "nonWithdrawableBalance" to (newDeposit + newBonus),
                    "totalEntryFeesPaid" to newEntryFeesPaid,
                    "funds" to newTotal,
                    "balance" to newTotal,
                    "walletBalance" to newTotal,
                    "lastActive" to nowJoin
                )
                userRef.updateChildren(updates).await()
                database.child("userProfiles").child(userId).updateChildren(updates).await()
                database.child("players").child(userId).updateChildren(updates).await()
                database.child("wallet_accounts").child(userId).updateChildren(updates).await()
                try {
                    firestore.collection("users").document(userId).set(updates, com.google.firebase.firestore.SetOptions.merge()).await()
                } catch (ignored: Exception) {}

                // Record debit transaction with 3-bucket breakdown
                val txId = "TX-REG-${nowJoin}"
                val txPayload = mapOf(
                    "id" to txId,
                    "uid" to userId,
                    "userId" to userId,
                    "amount" to -entryFee.toDouble(),
                    "paidFromBonus" to bonusToDeduct,
                    "paidFromDeposit" to depositToDeduct,
                    "paidFromWinning" to winningToDeduct,
                    "type" to "TOURNAMENT_ENTRY",
                    "status" to "COMPLETED",
                    "timestamp" to nowJoin,
                    "referenceId" to tournamentId,
                    "description" to "Entry fee for tournament ID: $tournamentId [Deducted: Bonus ₹$bonusToDeduct, Deposit ₹$depositToDeduct, Winnings ₹$winningToDeduct]"
                )
                database.child("wallet_transactions").child(userId).child(txId).setValue(txPayload).await()
            }

            // Register player with per-match fee and telemetry fields
            val regId = if (player.id.isNotBlank()) player.id else "${tournamentId}_${userId.ifBlank { "P" }}"
            val tourneyTitle = tourneySnap.child("title").value?.toString() ?: "Tournament #$tournamentId"
            val tourneyGame = tourneySnap.child("game").value?.toString() ?: "Free Fire"
            val tourneyCategory = tourneySnap.child("category").value?.toString() ?: "BR"
            val tourneyFormat = tourneySnap.child("format").value?.toString() ?: "SOLO"
            val tourneyMap = tourneySnap.child("map").value?.toString() ?: "Bermuda"

            val regPayload = mapOf(
                "id" to regId,
                "tournamentId" to tournamentId,
                "tournamentTitle" to tourneyTitle,
                "userId" to userId,
                "playerId" to userId,
                "playerName" to player.playerName.ifBlank { player.gameUsername },
                "gameUsername" to player.gameUsername,
                "gameId" to player.gameId,
                "gameAccountId" to (player.gameAccountId ?: ""),
                "region" to (player.region ?: ""),
                "age" to (player.age ?: 0),
                "paymentStatus" to player.paymentStatus.ifBlank { "confirmed" },
                "status" to player.status.ifBlank { "confirmed" },
                "registeredAt" to nowJoin,
                "joinedAt" to nowJoin,
                "entryFeePaid" to entryFee.toDouble(),
                "paidFromDeposit" to depositToDeduct,
                "paidFromWinning" to winningToDeduct,
                "paidFromBonus" to bonusToDeduct,
                "kills" to 0,
                "rank" to 0,
                "winningAmount" to 0.0
            )

            database.child("tournament_registrations").child(regId).setValue(regPayload).await()
            database.child("tournaments").child(tournamentId).child("participants").child(userId).setValue(regPayload).await()

            // Persist player's match participation record & lifecycle join timestamps in database
            if (userId.isNotBlank()) {
                val existingFirstAppJoin = safeLong(userSnap.child("firstAppJoinAt").value ?: userSnap.child("createdAt").value, nowJoin)
                val existingFirstMatchJoin = safeLong(userSnap.child("firstMatchJoinedAt").value, 0L)
                val firstMatchJoin = if (existingFirstMatchJoin > 0L) minOf(existingFirstMatchJoin, nowJoin) else nowJoin
                val prevMatchesJoined = safeInt(userSnap.child("totalMatchesJoined").value, 0) + 1

                val matchHistoryEntry = mapOf<String, Any>(
                    "tournamentId" to tournamentId,
                    "tournamentTitle" to tourneyTitle,
                    "game" to tourneyGame,
                    "category" to tourneyCategory,
                    "format" to tourneyFormat,
                    "map" to tourneyMap,
                    "joinedAt" to nowJoin,
                    "entryFeePaid" to entryFee.toDouble(),
                    "paidFromDeposit" to depositToDeduct,
                    "paidFromWinning" to winningToDeduct,
                    "paidFromBonus" to bonusToDeduct,
                    "kills" to 0,
                    "rank" to 0,
                    "winningAmount" to 0.0,
                    "status" to "JOINED"
                )
                val lifecycleUpdates = mapOf<String, Any>(
                    "firstAppJoinAt" to existingFirstAppJoin,
                    "firstMatchJoinedAt" to firstMatchJoin,
                    "lastMatchJoinedAt" to nowJoin,
                    "totalMatchesJoined" to prevMatchesJoined,
                    "lastActive" to nowJoin
                )
                try { database.child("users").child(userId).updateChildren(lifecycleUpdates).await() } catch (_: Exception) {}
                try { database.child("users").child(userId).child("joinedMatches").child(tournamentId).setValue(matchHistoryEntry).await() } catch (_: Exception) {}
                try { database.child("userProfiles").child(userId).updateChildren(lifecycleUpdates).await() } catch (_: Exception) {}
                try { database.child("userProfiles").child(userId).child("joinedMatches").child(tournamentId).setValue(matchHistoryEntry).await() } catch (_: Exception) {}
                try { database.child("user_match_history").child(userId).child(tournamentId).setValue(matchHistoryEntry).await() } catch (_: Exception) {}
                try { firestore.collection("users").document(userId).set(lifecycleUpdates, com.google.firebase.firestore.SetOptions.merge()).await() } catch (_: Exception) {}
                try { firestore.collection("users").document(userId).collection("joinedMatches").document(tournamentId).set(matchHistoryEntry).await() } catch (_: Exception) {}
            }

            // Update registered players count and slots across all multi-node paths
            currentCount += 1
            val gameName = tourneySnap.child("game").value?.toString() ?: "Free Fire"
            val slotUpdates = mapOf<String, Any>(
                "registeredPlayers" to currentCount,
                "registered_players" to currentCount,
                "joinedPlayers" to currentCount,
                "joined_players" to currentCount,
                "slotsBooked" to currentCount,
                "slots_booked" to currentCount,
                "current_players" to currentCount,
                "availableSlots" to (maxSlots - currentCount).coerceAtLeast(0),
                "available_slots" to (maxSlots - currentCount).coerceAtLeast(0),
                "slotsAvailable" to (maxSlots - currentCount).coerceAtLeast(0),
                "slots_available" to (maxSlots - currentCount).coerceAtLeast(0),
                "remainingSlots" to (maxSlots - currentCount).coerceAtLeast(0),
                "remaining_slots" to (maxSlots - currentCount).coerceAtLeast(0)
            )

            val paths = (
                getSyncRtdbPaths(tournamentId, gameName, "BR") +
                getSyncRtdbPaths(tournamentId, gameName, "CS") +
                getSyncRtdbPaths(tournamentId, gameName, "LONE_WOLF") +
                getSyncRtdbPaths(tournamentId, gameName, "SCRIMS")
            ).distinct()
            for (path in paths) {
                try {
                    database.child(path).updateChildren(slotUpdates).await()
                } catch (_: Exception) {}
            }

            val fsCollections = listOf(
                "tournaments", "matches", "active_tournaments", "all_tournaments",
                "categories/BR/matches", "categories/CS/matches", "categories/LONE_WOLF/matches", "categories/SCRIMS/matches",
                "categories/br/matches", "categories/cs/matches", "categories/lone_wolf/matches", "categories/scrims/matches",
                "categories/BR/tournaments", "categories/CS/tournaments", "categories/LONE_WOLF/tournaments", "categories/SCRIMS/tournaments",
                "tournaments_by_category/BR/items", "tournaments_by_category/CS/items", "tournaments_by_category/LONE_WOLF/items", "tournaments_by_category/SCRIMS/items"
            )
            for (col in fsCollections) {
                try {
                    firestore.collection(col).document(tournamentId).update(slotUpdates).await()
                } catch (_: Exception) {}
            }

            GlobalErrorManager.emitSuccess("Successfully registered for tournament!")
            return true
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to register: ${e.message}", e)
            return false
        }
    }

    suspend fun cancelTournamentAndRefund(
        tournamentId: String,
        reason: String,
        cancelledBy: String
    ): Boolean {
        try {
            val tourneyRef = database.child("tournaments").child(tournamentId)
            val tourneySnap = tourneyRef.get().await()
            val entryFee = safeFloat(tourneySnap.child("entryFee").value)

            // Refund all registered participants
            val participantsSnap = tourneySnap.child("participants")
            participantsSnap.children.forEach { pChild ->
                val uid = pChild.key ?: pChild.child("userId").value?.toString() ?: ""
                if (uid.isNotBlank() && entryFee > 0f) {
                    try {
                        val userRef = database.child("users").child(uid)
                        val uSnap = userRef.get().await()
                        val currentDeposit = safeDouble(uSnap.child("depositFunds").value ?: uSnap.child("deposit_funds").value)
                        val currentTotal = safeDouble(uSnap.child("funds").value ?: uSnap.child("balance").value)
                        val newDeposit = currentDeposit + entryFee
                        val newTotal = currentTotal + entryFee

                        val updates = mapOf<String, Any>(
                            "depositFunds" to newDeposit,
                            "funds" to newTotal,
                            "balance" to newTotal
                        )
                        userRef.updateChildren(updates).await()
                        database.child("userProfiles").child(uid).updateChildren(updates).await()

                        val now = System.currentTimeMillis()
                        val txId = "TX-CANCEL-REFUND-${now}-${uid.take(4)}"
                        val txPayload = mapOf(
                            "id" to txId,
                            "uid" to uid,
                            "amount" to entryFee.toDouble(),
                            "type" to "REFUND",
                            "status" to "COMPLETED",
                            "timestamp" to now,
                            "referenceId" to tournamentId,
                            "description" to "Automatic 100% refund for cancelled match: $reason"
                        )
                        database.child("wallet_transactions").child(uid).child(txId).setValue(txPayload).await()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to refund user $uid: ${e.message}")
                    }
                }
            }

            val cancelPayload = mapOf<String, Any>(
                "status" to "CANCELLED",
                "cancellationReason" to reason,
                "cancelledAt" to System.currentTimeMillis()
            )
            tourneyRef.updateChildren(cancelPayload).await()
            database.child("active_tournaments").child(tournamentId).removeValue().await()
            try {
                firestore.collection("tournaments").document(tournamentId).update(cancelPayload).await()
            } catch (ignored: Exception) {}

            GlobalErrorManager.emitSuccess("Tournament cancelled and refunds processed successfully.")
            return true
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to cancel tournament: ${e.message}", e)
            return false
        }
    }

    suspend fun leaveTournament(
        tournamentId: String,
        userId: String,
        refundFee: Float
    ): Boolean {
        try {
            val regId = "${tournamentId}_${userId}"
            database.child("tournament_registrations").child(regId).removeValue().await()
            database.child("tournaments").child(tournamentId).child("participants").child(userId).removeValue().await()

            val tourneyRef = database.child("tournaments").child(tournamentId)
            val tourneySnap = tourneyRef.get().await()
            val currentCount = (safeInt(tourneySnap.child("registeredPlayers").value, 1) - 1).coerceAtLeast(0)
            tourneyRef.child("registeredPlayers").setValue(currentCount).await()

            // Refund fee if applicable
            if (refundFee > 0f) {
                val userRef = database.child("users").child(userId)
                val userSnap = userRef.get().await()
                val currentBalance = safeDouble(userSnap.child("balance").value ?: userSnap.child("funds").value)
                val newBalance = currentBalance + refundFee
                userRef.child("balance").setValue(newBalance).await()
                userRef.child("funds").setValue(newBalance).await()
                database.child("userProfiles").child(userId).child("funds").setValue(newBalance).await()

                val now = System.currentTimeMillis()
                val txId = "TX-REFUND-${now}"
                val txPayload = mapOf(
                    "id" to txId,
                    "amount" to refundFee.toDouble(),
                    "type" to "REFUND",
                    "status" to "COMPLETED",
                    "timestamp" to now,
                    "referenceId" to tournamentId,
                    "description" to "Tournament registration cancelled refund: $tournamentId"
                )
                database.child("wallet_transactions").child(userId).child(txId).setValue(txPayload).await()
            }

            GlobalErrorManager.emitSuccess("Registration cancelled and fee refunded.")
            return true
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to cancel registration: ${e.message}", e)
            return false
        }
    }

    // ==========================================
    // BACKEND ENGINE: MATCH RESULTS & PRIZE POOL DISTRIBUTION
    // ==========================================

    data class WinnerAward(
        val userId: String,
        val placement: Int, // 1, 2, 3
        val prizeAmount: Double,
        val kills: Int = 0,
        val killPrize: Double = 0.0
    )

    suspend fun distributeTournamentPrizes(
        tournamentId: String,
        awards: List<WinnerAward>,
        adminEmail: String
    ): Boolean {
        try {
            val now = System.currentTimeMillis()

            val tourneySnap = try { database.child("tournaments").child(tournamentId).get().await() } catch (_: Exception) { null }
            val tourneyTitle = tourneySnap?.child("title")?.value?.toString() ?: "Tournament #$tournamentId"
            val tourneyEntryFee = safeDouble(tourneySnap?.child("entryFee")?.value)

            awards.forEach { award ->
                if (award.userId.isNotBlank()) {
                    val totalAward = award.prizeAmount + award.killPrize
                    if (totalAward > 0 || award.kills > 0) {
                        val userRef = database.child("users").child(award.userId)
                        val snap = userRef.get().await()
                        val currentDeposit = safeDouble(snap.child("depositFunds").value ?: snap.child("deposit_funds").value)
                        val currentWinning = safeDouble(snap.child("winningFunds").value ?: snap.child("winning_funds").value)
                        val currentBonus = safeDouble(snap.child("bonusFunds").value ?: snap.child("bonus_funds").value)
                        val currentWins = safeInt(snap.child("wins").value) + (if (award.placement == 1) 1 else 0)
                        val currentKills = safeInt(snap.child("kills").value) + award.kills
                        val currentEarnings = safeDouble(snap.child("totalEarnings").value) + totalAward

                        val newWinning = currentWinning + totalAward
                        val newBalance = currentDeposit + newWinning + currentBonus

                        val userUpdates = mapOf<String, Any>(
                            "balance" to newBalance,
                            "funds" to newBalance,
                            "walletBalance" to newBalance,
                            "winningFunds" to newWinning,
                            "winning_funds" to newWinning,
                            "withdrawableBalance" to newWinning,
                            "nonWithdrawableBalance" to (currentDeposit + currentBonus),
                            "wins" to currentWins,
                            "kills" to currentKills,
                            "totalEarnings" to currentEarnings,
                            "lastActive" to now
                        )
                        userRef.updateChildren(userUpdates).await()
                        database.child("userProfiles").child(award.userId).updateChildren(userUpdates).await()
                        database.child("players").child(award.userId).updateChildren(userUpdates).await()
                        database.child("wallet_accounts").child(award.userId).updateChildren(userUpdates).await()
                        try {
                            firestore.collection("users").document(award.userId).set(userUpdates, com.google.firebase.firestore.SetOptions.merge()).await()
                        } catch (ignored: Exception) {}

                        // Update per-match history record for this player
                        val matchWinUpdates = mapOf<String, Any>(
                            "tournamentId" to tournamentId,
                            "tournamentTitle" to tourneyTitle,
                            "kills" to award.kills,
                            "rank" to award.placement,
                            "winningAmount" to totalAward,
                            "entryFeePaid" to tourneyEntryFee,
                            "status" to if (totalAward > 0.0) "WON" else "COMPLETED",
                            "updatedAt" to now
                        )
                        try { database.child("users").child(award.userId).child("joinedMatches").child(tournamentId).updateChildren(matchWinUpdates).await() } catch (_: Exception) {}
                        try { database.child("user_match_history").child(award.userId).child(tournamentId).updateChildren(matchWinUpdates).await() } catch (_: Exception) {}
                        try { database.child("tournaments").child(tournamentId).child("participants").child(award.userId).updateChildren(matchWinUpdates).await() } catch (_: Exception) {}
                        try { firestore.collection("users").document(award.userId).collection("joinedMatches").document(tournamentId).set(matchWinUpdates, com.google.firebase.firestore.SetOptions.merge()).await() } catch (_: Exception) {}

                        // Record prize transaction into Winning Money
                        val txId = "TX-PRIZE-${now}-${award.userId.take(5)}"
                        val txPayload = mapOf(
                            "id" to txId,
                            "uid" to award.userId,
                            "userId" to award.userId,
                            "amount" to totalAward,
                            "walletBucket" to "WINNING",
                            "withdrawable" to true,
                            "type" to "PRIZE",
                            "status" to "COMPLETED",
                            "timestamp" to now,
                            "referenceId" to tournamentId,
                            "description" to "Prize for #${award.placement} rank in $tourneyTitle ($tournamentId) credited to Withdrawable Winning Money. Kill bounty: ₹${award.killPrize.toInt()} (${award.kills} kills)."
                        )
                        database.child("wallet_transactions").child(award.userId).child(txId).setValue(txPayload).await()
                    }
                }
            }

            // Set tournament status to COMPLETED
            val compMap = mapOf<String, Any>(
                "status" to "COMPLETED",
                "endsAt" to now.toString(),
                "completedBy" to adminEmail
            )
            database.child("tournaments").child(tournamentId).updateChildren(compMap).await()
            try {
                firestore.collection("tournaments").document(tournamentId).update(compMap).await()
            } catch (ignored: Exception) {}

            GlobalErrorManager.emitSuccess("Prizes successfully distributed and match marked COMPLETED!")
            return true
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to distribute prizes: ${e.message}", e)
            return false
        }
    }

    // ==========================================
    // BACKEND ENGINE: BRACKETS & ADVANCEMENT
    // ==========================================

    suspend fun generateSingleEliminationBracket(
        tournamentId: String,
        participants: List<PlayerRegistration>
    ) {
        try {
            if (participants.isEmpty()) {
                GlobalErrorManager.emitError("Cannot generate bracket: No registered participants.")
                return
            }

            val shuffled = participants.shuffled()
            val matches = mutableListOf<Match>()
            var matchCounter = 1

            for (i in shuffled.indices step 2) {
                val p1 = shuffled[i]
                val p2 = if (i + 1 < shuffled.size) shuffled[i + 1] else null
                val p1Id = p1.userId.ifBlank { p1.id }
                val p2Id = p2?.userId?.ifBlank { p2.id }
                
                val m = Match(
                    id = "M-${matchCounter}",
                    tournamentId = tournamentId,
                    round = 1,
                    matchNumber = matchCounter,
                    player1Id = p1Id,
                    player2Id = p2Id,
                    winnerId = if (p2Id == null) p1Id else null,
                    status = if (p2Id == null) "COMPLETED" else "SCHEDULED",
                    score1 = if (p2Id == null) 1 else 0,
                    score2 = 0
                )
                matches.add(m)
                matchCounter++
            }

            // Generate subsequent advancement rounds (Quarter-Finals, Semi-Finals, Finals)
            var currentRoundMatches = matches.filter { it.round == 1 }
            var currentRound = 1
            while (currentRoundMatches.size > 1) {
                val nextRound = currentRound + 1
                val nextRoundMatches = mutableListOf<Match>()
                for (j in currentRoundMatches.indices step 2) {
                    val m1 = currentRoundMatches[j]
                    val m2 = if (j + 1 < currentRoundMatches.size) currentRoundMatches[j + 1] else null
                    
                    val p1Adv = if (m1.winnerId != null && m1.status == "COMPLETED") m1.winnerId else null
                    val p2Adv = if (m2?.winnerId != null && m2.status == "COMPLETED") m2.winnerId else null

                    val nextM = Match(
                        id = "M-${matchCounter}",
                        tournamentId = tournamentId,
                        round = nextRound,
                        matchNumber = matchCounter,
                        player1Id = p1Adv,
                        player2Id = p2Adv,
                        winnerId = null,
                        status = if (p1Adv != null && p2Adv != null) "READY" else "SCHEDULED",
                        score1 = 0,
                        score2 = 0
                    )
                    nextRoundMatches.add(nextM)
                    matches.add(nextM)
                    matchCounter++
                }
                currentRoundMatches = nextRoundMatches
                currentRound = nextRound
            }

            // Save to RTDB and Cloud Firestore (both root and subcollection)
            matches.forEach { match ->
                database.child("matches").child(tournamentId).child(match.id).setValue(match).await()
                try {
                    firestore.collection("matches").document(match.id).set(match).await()
                    firestore.collection("tournaments").document(tournamentId)
                        .collection("matches").document(match.id).set(match).await()
                } catch (ignored: Exception) {}
            }

            GlobalErrorManager.emitSuccess("Tournament bracket generated with ${matches.size} matches!")
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to generate bracket: ${e.message}", e)
        }
    }

    suspend fun recordMatchScore(
        tournamentId: String,
        matchId: String,
        winnerId: String,
        score1: Int,
        score2: Int,
        status: String = "COMPLETED"
    ) {
        try {
            val payload = mapOf<String, Any>(
                "score1" to score1,
                "score2" to score2,
                "winnerId" to winnerId,
                "status" to status
            )
            database.child("matches").child(tournamentId).child(matchId).updateChildren(payload).await()
            try {
                firestore.collection("matches").document(matchId).update(payload).await()
            } catch (ignored: Exception) {}

            GlobalErrorManager.emitSuccess("Match $matchId updated: $winnerId won ($score1 - $score2)")
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to update match score: ${e.message}", e)
        }
    }

    // ==========================================
    // BACKEND ENGINE: PLAYER GAME ID VERIFICATION
    // ==========================================

    suspend fun submitPlayerVerification(
        uid: String,
        gameId: String,
        ign: String,
        proofUrl: String? = null
    ) {
        try {
            val now = System.currentTimeMillis()
            val payload = mapOf(
                "id" to uid,
                "userId" to uid,
                "gameId" to gameId,
                "ign" to ign,
                "proofUrl" to (proofUrl ?: ""),
                "status" to "pending",
                "submittedAt" to now
            )
            database.child("verifications").child(uid).setValue(payload).await()
            database.child("users").child(uid).child("gameId").setValue(gameId).await()
            database.child("userProfiles").child(uid).child("gameId").setValue(gameId).await()

            GlobalErrorManager.emitSuccess("Verification submitted for Game ID: $gameId")
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to submit verification: ${e.message}", e)
        }
    }

    suspend fun approvePlayerVerification(uid: String, gameId: String) {
        try {
            val payload = mapOf(
                "status" to "verified",
                "verifiedAt" to System.currentTimeMillis()
            )
            database.child("verifications").child(uid).updateChildren(payload).await()
            database.child("users").child(uid).child("gameId").setValue(gameId).await()
            database.child("userProfiles").child(uid).child("gameId").setValue(gameId).await()

            GlobalErrorManager.emitSuccess("Game ID $gameId verified for user $uid!")
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to approve verification: ${e.message}", e)
        }
    }

    suspend fun rejectPlayerVerification(uid: String, reason: String) {
        try {
            val payload = mapOf(
                "status" to "rejected",
                "rejectionReason" to reason,
                "reviewedAt" to System.currentTimeMillis()
            )
            database.child("verifications").child(uid).updateChildren(payload).await()
            GlobalErrorManager.emitSuccess("Verification rejected: $reason")
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to reject verification: ${e.message}", e)
        }
    }

    // ==========================================
    // BACKEND ENGINE: WALLET TRANSACTIONS STREAM
    // ==========================================

    suspend fun getLiveUserTransactionsStream(uid: String): Flow<List<WalletTransaction>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    val id = child.child("id").value?.toString() ?: child.key ?: ""
                    val amount = safeDouble(child.child("amount").value)
                    val type = child.child("type").value?.toString() ?: "DEPOSIT"
                    val desc = child.child("description").value?.toString() ?: ""
                    val time = safeLong(child.child("timestamp").value, System.currentTimeMillis())
                    val status = child.child("status").value?.toString() ?: "COMPLETED"
                    WalletTransaction(
                        id = id,
                        uid = uid,
                        type = type,
                        amount = amount,
                        description = desc,
                        timestamp = time,
                        status = status
                    )
                }.sortedByDescending { it.timestamp }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        val ref = database.child("wallet_transactions").child(uid)
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // ==========================================
    // BACKEND ENGINE: 5-MIN ROOM CREDENTIALS BROADCASTER
    // ==========================================

    suspend fun publishRoomCredentialsWithTimeCheck(
        tournamentId: String,
        roomId: String,
        roomPass: String,
        startsAtTimestamp: Long? = null,
        bypassCheck: Boolean = false,
        adminEmail: String? = null,
        adminUid: String? = null
    ): Boolean {
        val authValidation = TournamentBackendValidator.validateAdminPermission(
            adminEmail ?: auth.currentUser?.email,
            adminUid ?: auth.currentUser?.uid,
            "publish room credentials"
        )
        if (!authValidation.isValid) {
            val err = (authValidation as ValidationResult.Invalid).reason
            GlobalErrorManager.emitError(err)
            return false
        }

        try {
            val now = System.currentTimeMillis()
            if (startsAtTimestamp != null && startsAtTimestamp > 0L && !bypassCheck) {
                val diffMs = startsAtTimestamp - now
                val fiveMinutesMs = 5 * 60 * 1000L
                if (diffMs > fiveMinutesMs) {
                    val minutesLeft = (diffMs / 60000L)
                    GlobalErrorManager.emitError("Room ID & Password can ONLY be entered 5 minutes before match time ($minutesLeft mins remaining).")
                    return false
                }
            }

            val roomDetails = RoomDetails(
                roomId = roomId.trim(),
                roomPassword = roomPass.trim(),
                updatedAt = now
            )

            val roomMap = mapOf<String, Any>(
                "roomId" to roomId.trim(),
                "room_id" to roomId.trim(),
                "roomPass" to roomPass.trim(),
                "roomPassword" to roomPass.trim(),
                "room_password" to roomPass.trim(),
                "password" to roomPass.trim(),
                "updatedAt" to now
            )
            val credUpdates = mapOf<String, Any>(
                "roomDetails" to roomMap,
                "roomId" to roomId.trim(),
                "room_id" to roomId.trim(),
                "roomPass" to roomPass.trim(),
                "roomPassword" to roomPass.trim(),
                "room_password" to roomPass.trim(),
                "password" to roomPass.trim(),
                "status" to "LIVE",
                "updatedAt" to now
            )

            // Primary write target: Firestore tournaments/{id} (Cloud Functions mirror RTDB & global_app_state)
            var firestoreSaved = false
            try {
                firestore.collection("tournaments").document(tournamentId).set(
                    credUpdates,
                    SetOptions.merge()
                ).await()
                firestoreSaved = true
            } catch (fe: Exception) {
                Log.w(TAG, "Firestore credentials update: ${fe.message}")
            }

            // Dispatch instant room credentials notification to all joined players primarily in Firestore
            try {
                val participants = getTournamentParticipants(tournamentId)
                val notifId = "notif_room_${tournamentId}_${now}"
                val roomNotifPayload = mapOf(
                    "id" to notifId,
                    "title" to "Room ID & Password Live!",
                    "message" to "Room ID: ${roomId.trim()} | Pass: ${roomPass.trim()}. Join lobby immediately!",
                    "type" to "ROOM_CREDENTIALS",
                    "targetTournamentId" to tournamentId,
                    "roomId" to roomId.trim(),
                    "roomPassword" to roomPass.trim(),
                    "timestamp" to now,
                    "isRead" to false
                )
                participants.forEach { p ->
                    val uid = p.userId.ifBlank { p.playerId }
                    if (uid.isNotBlank()) {
                        try { firestore.collection("users").document(uid).collection("notifications").document(notifId).set(roomNotifPayload).await() } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}

            context?.let { ctx ->
                val notif = VelorixNotificationManager.dispatchRoomCredentialsNotification(
                    context = ctx,
                    tournament = Tournament(id = tournamentId, title = "Match #$tournamentId", roomDetails = roomDetails, status = "LIVE"),
                    roomId = roomId.trim(),
                    roomPass = roomPass.trim()
                )
                syncNotificationToCloud(notif)
            }

            if (!firestoreSaved) {
                GlobalErrorManager.emitError("Failed to broadcast credentials: Could not sync to Firestore backend.")
                return false
            }

            GlobalErrorManager.emitSuccess("Room ID & Password broadcasted to checked-in players!")
            return true
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to broadcast credentials: ${e.message}", e)
            return false
        }
    }

    // ==========================================
    // BACKEND ENGINE: MATCH PROOFS & ANTI-CHEAT INSPECTOR
    // ==========================================

    suspend fun submitMatchProof(proof: MatchProofSubmission) {
        try {
            val pId = proof.id.ifBlank { "PRF-${System.currentTimeMillis()}-${proof.userId.take(4)}" }
            val updated = proof.copy(id = pId, submittedAt = System.currentTimeMillis(), status = "pending")
            database.child("match_proofs").child(pId).setValue(updated).await()
            try {
                firestore.collection("match_proofs").document(pId).set(updated).await()
            } catch (ignored: Exception) {}
            GlobalErrorManager.emitSuccess("Match proof screenshot submitted for verification!")
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to submit match proof: ${e.message}", e)
        }
    }

    suspend fun getMatchProofsStream(tournamentId: String? = null): Flow<List<MatchProofSubmission>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    if (!child.hasChildren() || child.value !is Map<*, *>) null
                    else child.safeGetObject<MatchProofSubmission>()
                }
                    .filter { proof ->
                        val idLower = proof.id.trim().lowercase()
                        val playerLower = proof.username.trim().lowercase()
                        val isMock = idLower.startsWith("mock_") || idLower.startsWith("test_") || idLower.startsWith("demo_") ||
                                     playerLower.contains("mock") || playerLower.contains("demo")
                        !isMock && (tournamentId == null || proof.tournamentId == tournamentId)
                    }
                    .sortedByDescending { it.submittedAt }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        val ref = database.child("match_proofs")
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun approveMatchProof(
        proofId: String,
        adminEmail: String,
        prizeAmount: Double,
        killsCount: Int
    ) {
        try {
            val snap = database.child("match_proofs").child(proofId).get().await()
            val proof = snap.safeGetObject<MatchProofSubmission>()
            if (proof == null) {
                GlobalErrorManager.emitError("Proof record not found.")
                return
            }

            val now = System.currentTimeMillis()
            val updates = mapOf<String, Any>(
                "status" to "approved",
                "prizeAwarded" to prizeAmount,
                "reviewedAt" to now,
                "reviewedBy" to adminEmail
            )
            database.child("match_proofs").child(proofId).updateChildren(updates).await()

            // Credit wallet and statistics
            if (proof.userId.isNotBlank()) {
                val userRef = database.child("users").child(proof.userId)
                val userSnap = userRef.get().await()
                val currentDeposit = safeDouble(userSnap.child("depositFunds").value ?: userSnap.child("deposit_funds").value)
                val currentWinning = safeDouble(userSnap.child("winningFunds").value ?: userSnap.child("winning_funds").value)
                val currentBonus = safeDouble(userSnap.child("bonusFunds").value ?: userSnap.child("bonus_funds").value)
                val currentWins = safeInt(userSnap.child("wins").value) + (if (proof.claimedRank == 1) 1 else 0)
                val currentKills = safeInt(userSnap.child("kills").value) + killsCount
                val currentEarnings = safeDouble(userSnap.child("totalEarnings").value) + prizeAmount

                val newWinning = currentWinning + prizeAmount
                val newBal = currentDeposit + newWinning + currentBonus

                val userUpdates = mapOf<String, Any>(
                    "balance" to newBal,
                    "funds" to newBal,
                    "walletBalance" to newBal,
                    "winningFunds" to newWinning,
                    "winning_funds" to newWinning,
                    "withdrawableBalance" to newWinning,
                    "nonWithdrawableBalance" to (currentDeposit + currentBonus),
                    "wins" to currentWins,
                    "kills" to currentKills,
                    "totalEarnings" to currentEarnings,
                    "lastActive" to now
                )
                userRef.updateChildren(userUpdates).await()
                database.child("userProfiles").child(proof.userId).updateChildren(userUpdates).await()
                database.child("players").child(proof.userId).updateChildren(userUpdates).await()
                database.child("wallet_accounts").child(proof.userId).updateChildren(userUpdates).await()
                try {
                    firestore.collection("users").document(proof.userId).set(userUpdates, com.google.firebase.firestore.SetOptions.merge()).await()
                } catch (ignored: Exception) {}

                if (proof.tournamentId.isNotBlank()) {
                    val matchProofUpdates = mapOf<String, Any>(
                        "tournamentId" to proof.tournamentId,
                        "tournamentTitle" to proof.tournamentTitle,
                        "kills" to killsCount,
                        "rank" to proof.claimedRank,
                        "winningAmount" to prizeAmount,
                        "status" to if (prizeAmount > 0.0) "WON" else "COMPLETED",
                        "updatedAt" to now
                    )
                    try { database.child("users").child(proof.userId).child("joinedMatches").child(proof.tournamentId).updateChildren(matchProofUpdates).await() } catch (_: Exception) {}
                    try { database.child("user_match_history").child(proof.userId).child(proof.tournamentId).updateChildren(matchProofUpdates).await() } catch (_: Exception) {}
                    try { database.child("tournaments").child(proof.tournamentId).child("participants").child(proof.userId).updateChildren(matchProofUpdates).await() } catch (_: Exception) {}
                }

                // Transaction log
                val txId = "TX-WIN-${now}"
                val txPayload = mapOf(
                    "id" to txId,
                    "uid" to proof.userId,
                    "userId" to proof.userId,
                    "amount" to prizeAmount,
                    "walletBucket" to "WINNING",
                    "withdrawable" to true,
                    "type" to "PRIZE",
                    "status" to "COMPLETED",
                    "timestamp" to now,
                    "referenceId" to proof.tournamentId,
                    "description" to "Prize verified for ${proof.tournamentTitle} (Rank #${proof.claimedRank}, $killsCount Kills) credited to Winning Money"
                )
                database.child("wallet_transactions").child(proof.userId).child(txId).setValue(txPayload).await()
            }

            GlobalErrorManager.emitSuccess("Proof approved! ₹${prizeAmount.toInt()} credited to ${proof.username}.")
            
            context?.let { ctx ->
                val notif = VelorixNotificationManager.dispatchMatchProofNotification(
                    context = ctx,
                    username = proof.username,
                    prizeAmount = prizeAmount,
                    approved = true,
                    tournamentTitle = proof.tournamentTitle,
                    targetUserId = proof.userId
                )
                syncNotificationToCloud(notif)
            }
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to approve proof: ${e.message}", e)
        }
    }

    suspend fun rejectMatchProof(proofId: String, reason: String, adminEmail: String) {
        try {
            val snap = database.child("match_proofs").child(proofId).get().await()
            val proof = snap.safeGetObject<MatchProofSubmission>()
            val username = proof?.username ?: "Player"
            val tourneyTitle = proof?.tournamentTitle ?: "Tournament"

            val payload = mapOf<String, Any>(
                "status" to "rejected",
                "rejectionReason" to reason,
                "reviewedAt" to System.currentTimeMillis(),
                "reviewedBy" to adminEmail
            )
            database.child("match_proofs").child(proofId).updateChildren(payload).await()
            GlobalErrorManager.emitSuccess("Proof rejected: $reason")

            context?.let { ctx ->
                val notif = VelorixNotificationManager.dispatchMatchProofNotification(
                    context = ctx,
                    username = username,
                    prizeAmount = 0.0,
                    approved = false,
                    tournamentTitle = tourneyTitle,
                    targetUserId = proof?.userId ?: ""
                )
                syncNotificationToCloud(notif)
            }
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to reject proof: ${e.message}", e)
        }
    }

    // ==========================================
    // BACKEND ENGINE: GLOBAL LIVE ANNOUNCEMENTS
    // ==========================================

    private val localAnnouncementsMap = mutableMapOf<String, GlobalAnnouncement>()
    private val locallyDeletedAnnouncementIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    suspend fun publishGlobalAnnouncement(announcement: GlobalAnnouncement) {
        val id = announcement.id.ifBlank { "ANN-${System.currentTimeMillis()}" }
        val item = announcement.copy(id = id, createdAt = System.currentTimeMillis())
        locallyDeletedAnnouncementIds.remove(id)
        
        // Cache in memory immediately for instantaneous responsiveness
        localAnnouncementsMap[id] = item

        val notif = AppNotification(
            id = "notif_$id",
            title = announcement.title,
            message = announcement.message,
            type = "CAMPAIGN",
            game = "Esports Campaign",
            timestamp = System.currentTimeMillis(),
            priority = announcement.level,
            isRead = false
        )

        val notifMap = mapOf(
            "id" to notif.id,
            "title" to notif.title,
            "message" to notif.message,
            "type" to notif.type,
            "game" to notif.game,
            "timestamp" to notif.timestamp,
            "priority" to notif.priority,
            "isRead" to false
        )

        // Resilient writes to RTDB
        try { database.child("announcements").child(id).setValue(item).await() } catch (e: Exception) { Log.d(TAG, "RTDB announcement write note: ${e.message}") }
        try { database.child("notifications").child("notif_$id").setValue(notifMap).await() } catch (_: Exception) {}
        try { database.child("campaigns").child("notif_$id").setValue(notifMap).await() } catch (_: Exception) {}

        // Resilient writes to Firestore
        try { firestore.collection("announcements").document(id).set(item).await() } catch (e: Exception) { Log.d(TAG, "Firestore announcement write note: ${e.message}") }
        try { firestore.collection("notifications").document("notif_$id").set(notifMap).await() } catch (_: Exception) {}
        try { firestore.collection("campaigns").document("notif_$id").set(notifMap).await() } catch (_: Exception) {}

        // Dispatch local system notification & broadcast
        context?.let { ctx ->
            VelorixNotificationManager.dispatchCampaignBroadcast(ctx, announcement.title, announcement.message, announcement.level)
        }

        GlobalErrorManager.emitSuccess("Global announcement broadcasted successfully to all players!")
    }

    suspend fun getGlobalAnnouncementsStream(): Flow<List<GlobalAnnouncement>> = callbackFlow {
        val announcementsMap = mutableMapOf<String, GlobalAnnouncement>()
        localAnnouncementsMap.forEach { (k, v) ->
            if (!locallyDeletedAnnouncementIds.contains(k)) {
                announcementsMap[k] = v
            }
        }

        fun emitAnnouncements() {
            val list = announcementsMap.values
                .filter { it.isActive && !locallyDeletedAnnouncementIds.contains(it.id) }
                .sortedByDescending { it.createdAt }
            trySend(list)
        }

        emitAnnouncements()

        val rtdbListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentRtdbIds = mutableSetOf<String>()
                for (child in snapshot.children) {
                    if (!child.hasChildren() || child.value !is Map<*, *>) continue
                    val ann = child.safeGetObject<GlobalAnnouncement>()
                    val id = ann?.id?.ifBlank { null } ?: child.key ?: ""
                    val title = ann?.title ?: child.child("title").value?.toString() ?: ""
                    val message = ann?.message ?: child.child("message").value?.toString() ?: ""
                    val level = ann?.level ?: child.child("level").value?.toString() ?: child.child("priority").value?.toString() ?: "INFO"
                    val createdAt = ann?.createdAt ?: safeLong(child.child("createdAt").value, System.currentTimeMillis())
                    val isActive = ann?.isActive ?: (child.child("isActive").value as? Boolean ?: true)

                    if (id.isNotBlank() && title.isNotBlank() && !locallyDeletedAnnouncementIds.contains(id)) {
                        currentRtdbIds.add(id)
                        announcementsMap[id] = GlobalAnnouncement(
                            id = id,
                            title = title,
                            message = message,
                            level = level,
                            createdAt = createdAt,
                            author = ann?.author ?: "Velorix Admin",
                            isActive = isActive
                        )
                    }
                }
                // Purge any remote deletions that were removed from RTDB
                announcementsMap.keys.removeAll { key ->
                    !currentRtdbIds.contains(key) && !localAnnouncementsMap.containsKey(key)
                }
                emitAnnouncements()
            }
            override fun onCancelled(error: DatabaseError) {
                emitAnnouncements()
            }
        }
        val ref = database.child("announcements")
        ref.addValueEventListener(rtdbListener)

        val fsListener = try {
            firestore.collection("announcements").addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val currentFsIds = mutableSetOf<String>()
                    snapshot.documents.forEach { doc ->
                        val id = doc.id
                        val title = doc.getString("title") ?: ""
                        val message = doc.getString("message") ?: ""
                        val level = doc.getString("level") ?: doc.getString("priority") ?: "INFO"
                        val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                        val isActive = doc.getBoolean("isActive") ?: true
                        if (id.isNotBlank() && title.isNotBlank() && !locallyDeletedAnnouncementIds.contains(id)) {
                            currentFsIds.add(id)
                            announcementsMap[id] = GlobalAnnouncement(
                                id = id,
                                title = title,
                                message = message,
                                level = level,
                                createdAt = createdAt,
                                author = doc.getString("author") ?: "Velorix Admin",
                                isActive = isActive
                            )
                        }
                    }
                    emitAnnouncements()
                }
            }
        } catch (_: Exception) {
            null
        }

        awaitClose {
            ref.removeEventListener(rtdbListener)
            fsListener?.remove()
        }
    }

    suspend fun deleteGlobalAnnouncement(id: String) {
        locallyDeletedAnnouncementIds.add(id)
        localAnnouncementsMap.remove(id)
        try { database.child("announcements").child(id).removeValue().await() } catch (_: Exception) {}
        try { database.child("notifications").child("notif_$id").removeValue().await() } catch (_: Exception) {}
        try { database.child("campaigns").child("notif_$id").removeValue().await() } catch (_: Exception) {}
        try { firestore.collection("announcements").document(id).delete().await() } catch (_: Exception) {}
        try { firestore.collection("notifications").document("notif_$id").delete().await() } catch (_: Exception) {}
        try { firestore.collection("campaigns").document("notif_$id").delete().await() } catch (_: Exception) {}
        GlobalErrorManager.emitSuccess("Announcement removed.")
    }

    // ==========================================
    // BACKEND ENGINE: NOTIFICATIONS & CAMPAIGNS
    // ==========================================

    suspend fun dispatchAutoTournamentCampaign(tournament: Tournament) {
        val notifId = "notif_${tournament.id}"
        val gameName = tournament.game
        val prizeStr = if (tournament.prizePool > 0) "₹${tournament.prizePool.toInt()}" else "Free"
        val entryStr = if (tournament.entryFee > 0) "₹${tournament.entryFee.toInt()}" else "FREE"
        
        val title = "New Tournament Announced: ${tournament.title}"
        val message = "$gameName • Prize Pool: $prizeStr • Entry: $entryStr • ${tournament.maxPlayers} Slots (${tournament.format}). Join now!"
        
        val notif = AppNotification(
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
        
        val ann = GlobalAnnouncement(
            id = "ANN-${tournament.id}",
            title = title,
            message = message,
            level = "TOURNAMENT",
            createdAt = System.currentTimeMillis(),
            author = "Velorix Esports",
            isActive = true
        )

        // 1. Sync to RTDB paths
        val notifMap = mapOf(
            "id" to notif.id,
            "title" to notif.title,
            "message" to notif.message,
            "type" to notif.type,
            "targetTournamentId" to notif.targetTournamentId,
            "game" to notif.game,
            "timestamp" to notif.timestamp,
            "priority" to notif.priority,
            "isRead" to false
        )
        try { database.child("notifications").child(notifId).setValue(notifMap).await() } catch (_: Exception) {}
        try { database.child("campaigns").child(notifId).setValue(notifMap).await() } catch (_: Exception) {}
        try { database.child("announcements").child("ANN-${tournament.id}").setValue(ann).await() } catch (_: Exception) {}
        try { database.child("meta").child("last_campaign_time").setValue(System.currentTimeMillis()).await() } catch (_: Exception) {}

        // 2. Sync to Firestore
        try { firestore.collection("notifications").document(notifId).set(notifMap).await() } catch (_: Exception) {}
        try { firestore.collection("campaigns").document(notifId).set(notifMap).await() } catch (_: Exception) {}
        try { firestore.collection("announcements").document("ANN-${tournament.id}").set(ann).await() } catch (_: Exception) {}

        // 3. Dispatch System Push Notification & In-App Heads-Up Banner
        context?.let { ctx ->
            VelorixNotificationManager.dispatchTournamentCampaignNotification(ctx, tournament)
        }
    }

    suspend fun getNotificationsStream(): Flow<List<AppNotification>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    if (!child.hasChildren() || child.value !is Map<*, *>) null
                    else child.safeGetObject<AppNotification>()
                }.sortedByDescending { it.timestamp }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        val ref = database.child("notifications")
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun markNotificationAsRead(id: String) {
        try {
            database.child("notifications").child(id).child("isRead").setValue(true).await()
            try { firestore.collection("notifications").document(id).update("isRead", true).await() } catch (_: Exception) {}
        } catch (_: Exception) {}
    }

    suspend fun clearAllNotifications() {
        try {
            database.child("notifications").removeValue().await()
            GlobalErrorManager.emitSuccess("Notifications cleared.")
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to clear notifications: ${e.message}")
        }
    }

    suspend fun publishCampaignNotification(
        title: String,
        message: String,
        priority: String = "HIGH",
        tournamentId: String? = null
    ) {
        val notifId = "camp_${System.currentTimeMillis()}"
        val notif = AppNotification(
            id = notifId,
            title = title,
            message = message,
            type = "CAMPAIGN",
            targetTournamentId = tournamentId ?: "",
            game = "Esports Campaign",
            timestamp = System.currentTimeMillis(),
            priority = priority,
            isRead = false
        )
        val ann = GlobalAnnouncement(
            id = "ANN-$notifId",
            title = title,
            message = message,
            level = priority,
            createdAt = System.currentTimeMillis(),
            author = "Velorix Admin",
            isActive = true
        )
        var anySuccess = false
        try {
            database.child("notifications").child(notifId).setValue(notif).await()
            database.child("campaigns").child(notifId).setValue(notif).await()
            database.child("announcements").child("ANN-$notifId").setValue(ann).await()
            anySuccess = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            firestore.collection("notifications").document(notifId).set(notif).await()
            firestore.collection("campaigns").document(notifId).set(notif).await()
            firestore.collection("announcements").document("ANN-$notifId").set(ann).await()
            anySuccess = true
        } catch (_: Exception) {}
        
        context?.let { ctx ->
            VelorixNotificationManager.dispatchCampaignBroadcast(ctx, title, message, priority, tournamentId)
        }
        if (anySuccess) {
            GlobalErrorManager.emitSuccess("Campaign broadcasted to all players!")
        } else {
            GlobalErrorManager.emitError("Permission denied: Check Firebase Database and Firestore security rules.")
        }
    }

    suspend fun syncNotificationToCloud(notif: AppNotification) {
        val notifMap = mapOf(
            "id" to notif.id,
            "title" to notif.title,
            "message" to notif.message,
            "type" to notif.type,
            "targetTournamentId" to notif.targetTournamentId,
            "game" to notif.game,
            "timestamp" to notif.timestamp,
            "priority" to notif.priority,
            "isRead" to notif.isRead
        )
        try { database.child("notifications").child(notif.id).setValue(notifMap).await() } catch (_: Exception) {}
        try { database.child("campaigns").child(notif.id).setValue(notifMap).await() } catch (_: Exception) {}
        try { firestore.collection("notifications").document(notif.id).set(notifMap).await() } catch (_: Exception) {}
        try { firestore.collection("campaigns").document(notif.id).set(notifMap).await() } catch (_: Exception) {}
    }

    // ==========================================
    // DATABASE CLEANUP & DATA PURGE UTILITY
    // ==========================================

    suspend fun purgeAllDemoAndMockData(
        adminEmail: String,
        adminUid: String = auth.currentUser?.uid ?: ""
    ): Pair<Boolean, String> {
        val effectiveUid = adminUid.ifBlank { auth.currentUser?.uid ?: "admin_master_uid" }
        val effectiveEmail = adminEmail.ifBlank { auth.currentUser?.email ?: "anantisback47@gmail.com" }

        // Ensure admin claims and records are written to RTDB and Firestore before attempting deletion
        try {
            verifyAndRegisterAdmin(effectiveUid, effectiveEmail)
        } catch (_: Exception) {}

        var totalPurged = 0

        // Resilient RTDB node cleanup: tries bulk removeValue, falls back to child-by-child removal
        suspend fun safePurgeRtdb(node: String, filter: ((com.google.firebase.database.DataSnapshot) -> Boolean)? = null) {
            try {
                if (filter == null) {
                    try {
                        database.child(node).removeValue().await()
                        totalPurged++
                        return
                    } catch (_: Exception) {
                        // Root remove failed due to security/validation rules; proceed to delete individual children
                    }
                }
                val snap = database.child(node).get().await()
                for (child in snap.children) {
                    if (filter == null || filter(child)) {
                        try {
                            child.ref.removeValue().await()
                            totalPurged++
                        } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}
        }

        // Resilient Firestore collection cleanup
        suspend fun safePurgeFirestore(collectionName: String, filter: ((com.google.firebase.firestore.DocumentSnapshot) -> Boolean)? = null) {
            try {
                val snap = firestore.collection(collectionName).get().await()
                for (doc in snap.documents) {
                    if (filter == null || filter(doc)) {
                        try {
                            doc.reference.delete().await()
                            totalPurged++
                        } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}
        }

        // 1. Tournaments across all potential RTDB nodes and category structures
        val allTournamentNodes = listOf(
            "tournaments", "Tournaments", "tournament", "Tournament",
            "matches", "Matches", "match", "Match",
            "active_tournaments", "all_tournaments", "published_matches",
            "categories", "tournaments_by_category", "matches_by_category",
            "FreeFire", "BGMI", "custom_rooms", "game_tournaments"
        )
        allTournamentNodes.forEach { node ->
            safePurgeRtdb(node)
        }
        val allTournamentCollections = listOf(
            "tournaments", "matches", "active_tournaments", "all_tournaments",
            "categories", "published_matches", "custom_rooms"
        )
        allTournamentCollections.forEach { col ->
            safePurgeFirestore(col)
        }
        locallyDeletedTournamentIds.clear()
        locallyDeletedAnnouncementIds.clear()

        // 2. Complaints & Support tickets
        safePurgeRtdb("complaints")
        safePurgeRtdb("support_tickets")
        safePurgeFirestore("support_tickets")

        // 3. Payouts & cashouts
        safePurgeRtdb("payout_requests")
        safePurgeRtdb("payouts")
        safePurgeRtdb("cashouts")
        safePurgeFirestore("payout_requests")

        // 4. Match proofs
        safePurgeRtdb("match_proofs")
        safePurgeFirestore("match_proofs")

        // 5. Announcements, Notifications & Campaigns
        safePurgeRtdb("announcements")
        safePurgeRtdb("notifications")
        safePurgeRtdb("campaigns")
        safePurgeFirestore("announcements")
        safePurgeFirestore("notifications")

        // 6. Non-admin users (preserve current admin)
        safePurgeRtdb("users") { child ->
            val uEmail = child.child("email").value?.toString()?.lowercase() ?: ""
            val uId = child.key ?: ""
            uEmail != effectiveEmail.lowercase() && uId != effectiveUid
        }
        safePurgeFirestore("users") { doc ->
            val uEmail = doc.getString("email")?.lowercase() ?: ""
            uEmail != effectiveEmail.lowercase() && doc.id != effectiveUid
        }

        // Re-register current admin so permissions stay intact
        try {
            verifyAndRegisterAdmin(effectiveUid, effectiveEmail)
        } catch (_: Exception) {}

        GlobalErrorManager.emitSuccess("Clean slate restored! Demo data purged.")
        return Pair(true, "All demo & mock data purged successfully.")
    }
}



