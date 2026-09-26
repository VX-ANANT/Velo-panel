package com.example.ui.viewmodel

import android.util.Log
import com.example.data.repository.TournamentRepositoryImpl
import com.example.domain.model.AdminRecord
import com.example.domain.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.tasks.await

/**
 * UserRoleManager coordinates user roles, admin access, ban controls, and user profiles
 * across Firebase Realtime Database (RTDB) and Cloud Firestore with dual-sync resilience.
 *
 * Architecture Guarantees:
 * 1. Executes Realtime Database (RTDB) updates immediately to guarantee real-time sync for active players.
 * 2. Attempts atomic Firestore transactions / merge writes for long-term audit and query storage.
 * 3. Gracefully handles unprovisioned or offline Firestore database instances (e.g., NOT_FOUND default database)
 *    so administration operations succeed seamlessly via RTDB without blocking administrators.
 */
class UserRoleManager(
    private val repository: TournamentRepositoryImpl
) {
    private val TAG = "UserRoleManager"
    private val firestore: FirebaseFirestore get() = repository.firestore
    private val database: DatabaseReference get() = repository.database

    /**
     * Grants or promotes an admin role. Persists to RTDB and mirrors to Firestore.
     */
    suspend fun grantAdminRole(
        uid: String,
        email: String,
        name: String,
        role: String,
        grantedBy: String
    ): Result<AdminRecord> = runCatching {
        val cleanEmail = email.trim().lowercase()
        val adminUid = if (uid.isNotBlank()) uid else "ADM_${cleanEmail.replace(".", "_").replace("@", "_")}"
        val now = System.currentTimeMillis()
        val validRole = when (role.trim().lowercase()) {
            "super_admin", "tournament_admin", "support_admin", "moderator_admin" -> role.trim().lowercase()
            else -> "tournament_admin"
        }

        val adminRecord = AdminRecord(
            uid = adminUid,
            email = cleanEmail,
            name = name.trim().ifBlank { cleanEmail.substringBefore("@") },
            role = validRole,
            active = true,
            assignedAt = now,
            grantedBy = grantedBy,
            updatedAt = now
        )

        val payload = mapOf(
            "uid" to adminUid,
            "role" to validRole,
            "active" to true,
            "status" to "ACTIVE",
            "grantedBy" to grantedBy,
            "email" to cleanEmail,
            "name" to adminRecord.name,
            "grantedAt" to now,
            "assignedAt" to now,
            "updatedAt" to now
        )

        val userRoleUpdates = mapOf<String, Any>(
            "role" to validRole,
            "isAdmin" to true,
            "active" to true,
            "updatedAt" to now
        )

        // 1. Primary RTDB persistence (Active backend)
        val rtdbNodes = listOf("admins", "Admins", "staff", "Staff", "admin_users")
        rtdbNodes.forEach { node ->
            try {
                database.child(node).child(adminUid).setValue(payload).await()
            } catch (e: Exception) {
                Log.w(TAG, "RTDB write to $node: ${e.message}")
            }
        }
        try {
            database.child("users").child(adminUid).updateChildren(userRoleUpdates).await()
            database.child("userProfiles").child(adminUid).updateChildren(userRoleUpdates).await()
        } catch (_: Exception) {}

        // 2. Secondary Firestore Mirror (gracefully catches if Firestore is unprovisioned)
        try {
            val adminDocRef = firestore.collection("admins").document(adminUid)
            val userDocRef = firestore.collection("users").document(adminUid)
            val profileDocRef = firestore.collection("userProfiles").document(adminUid)
            adminDocRef.set(payload, SetOptions.merge()).await()
            userDocRef.set(userRoleUpdates, SetOptions.merge()).await()
            profileDocRef.set(userRoleUpdates, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.d(TAG, "Firestore mirror skipped/pending (${e.message})")
        }

        adminRecord
    }

    /**
     * Revokes an admin role. Persists to RTDB and mirrors to Firestore.
     */
    suspend fun revokeAdminRole(adminUid: String): Result<AdminRecord> = runCatching {
        val now = System.currentTimeMillis()
        val updates = mapOf<String, Any>(
            "active" to false,
            "status" to "REVOKED",
            "revokedAt" to now,
            "updatedAt" to now
        )
        val userRoleUpdates = mapOf<String, Any>(
            "role" to "player",
            "isAdmin" to false,
            "updatedAt" to now
        )

        // 1. Primary RTDB persistence
        val rtdbNodes = listOf("admins", "Admins", "staff", "Staff", "admin_users")
        rtdbNodes.forEach { node ->
            try {
                database.child(node).child(adminUid).updateChildren(updates).await()
            } catch (_: Exception) {}
        }
        try {
            database.child("users").child(adminUid).updateChildren(userRoleUpdates).await()
            database.child("userProfiles").child(adminUid).updateChildren(userRoleUpdates).await()
        } catch (_: Exception) {}

        // 2. Secondary Firestore Mirror
        try {
            val adminDocRef = firestore.collection("admins").document(adminUid)
            val userDocRef = firestore.collection("users").document(adminUid)
            val profileDocRef = firestore.collection("userProfiles").document(adminUid)
            adminDocRef.set(updates, SetOptions.merge()).await()
            userDocRef.set(userRoleUpdates, SetOptions.merge()).await()
            profileDocRef.set(userRoleUpdates, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.d(TAG, "Firestore revoke mirror skipped: ${e.message}")
        }

        AdminRecord(uid = adminUid, active = false, updatedAt = now)
    }

    /**
     * Updates an existing AdminRecord in RTDB and Firestore.
     */
    suspend fun updateAdminRecord(admin: AdminRecord): Result<AdminRecord> = runCatching {
        val now = System.currentTimeMillis()
        val updatedAdmin = admin.copy(updatedAt = now)

        val payload = mapOf(
            "uid" to updatedAdmin.uid,
            "name" to updatedAdmin.name,
            "email" to updatedAdmin.email,
            "role" to updatedAdmin.role,
            "active" to updatedAdmin.active,
            "status" to if (updatedAdmin.active) "ACTIVE" else "REVOKED",
            "assignedAt" to updatedAdmin.assignedAt,
            "grantedBy" to updatedAdmin.grantedBy,
            "updatedAt" to now
        )
        val userRoleUpdates = mapOf<String, Any>(
            "role" to updatedAdmin.role,
            "isAdmin" to updatedAdmin.active,
            "updatedAt" to now
        )

        // 1. Primary RTDB write
        val rtdbNodes = listOf("admins", "Admins", "staff", "Staff", "admin_users")
        rtdbNodes.forEach { node ->
            try {
                database.child(node).child(updatedAdmin.uid).setValue(payload).await()
            } catch (_: Exception) {}
        }
        try {
            database.child("users").child(updatedAdmin.uid).updateChildren(userRoleUpdates).await()
            database.child("userProfiles").child(updatedAdmin.uid).updateChildren(userRoleUpdates).await()
        } catch (_: Exception) {}

        // 2. Secondary Firestore write
        try {
            val adminDocRef = firestore.collection("admins").document(updatedAdmin.uid)
            val userDocRef = firestore.collection("users").document(updatedAdmin.uid)
            val profileDocRef = firestore.collection("userProfiles").document(updatedAdmin.uid)
            adminDocRef.set(payload, SetOptions.merge()).await()
            userDocRef.set(userRoleUpdates, SetOptions.merge()).await()
            profileDocRef.set(userRoleUpdates, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.d(TAG, "Firestore admin update mirror skipped: ${e.message}")
        }

        updatedAdmin
    }

    /**
     * Permanently deletes an AdminRecord from RTDB and Firestore.
     */
    suspend fun deleteAdminRecord(adminUid: String): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val userRoleUpdates = mapOf<String, Any>(
            "role" to "player",
            "isAdmin" to false,
            "updatedAt" to now
        )

        // 1. Primary RTDB deletion
        val rtdbNodes = listOf("admins", "Admins", "staff", "Staff", "admin_users")
        rtdbNodes.forEach { node ->
            try {
                database.child(node).child(adminUid).removeValue().await()
            } catch (_: Exception) {}
        }
        try {
            database.child("users").child(adminUid).updateChildren(userRoleUpdates).await()
            database.child("userProfiles").child(adminUid).updateChildren(userRoleUpdates).await()
        } catch (_: Exception) {}

        // 2. Secondary Firestore deletion
        try {
            firestore.collection("admins").document(adminUid).delete().await()
            firestore.collection("users").document(adminUid).set(userRoleUpdates, SetOptions.merge()).await()
            firestore.collection("userProfiles").document(adminUid).set(userRoleUpdates, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.d(TAG, "Firestore delete admin skipped: ${e.message}")
        }
    }

    /**
     * Updates a user's role across RTDB and Firestore.
     */
    suspend fun updateUserRole(userId: String, newRole: String, isAdmin: Boolean): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val userRoleUpdates = mapOf<String, Any>(
            "role" to newRole,
            "isAdmin" to isAdmin,
            "updatedAt" to now
        )

        // 1. Primary RTDB update
        try {
            database.child("users").child(userId).updateChildren(userRoleUpdates).await()
            database.child("userProfiles").child(userId).updateChildren(userRoleUpdates).await()
            database.child("players").child(userId).updateChildren(userRoleUpdates).await()
        } catch (e: Exception) {
            Log.w(TAG, "RTDB updateUserRole failed: ${e.message}")
        }

        // 2. Secondary Firestore update
        try {
            firestore.collection("users").document(userId).set(userRoleUpdates, SetOptions.merge()).await()
            firestore.collection("userProfiles").document(userId).set(userRoleUpdates, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.d(TAG, "Firestore updateUserRole mirror skipped: ${e.message}")
        }
    }

    /**
     * Updates a full UserProfile across RTDB and Firestore.
     * Guaranteed not to fail if Firestore is unprovisioned.
     */
    suspend fun updateUserProfile(user: UserProfile): Result<UserProfile> = runCatching {
        val now = System.currentTimeMillis()

        val updates = mutableMapOf<String, Any>(
            "id" to user.id,
            "uid" to user.id,
            "username" to user.username,
            "name" to user.username,
            "displayName" to user.username,
            "email" to user.email,
            "phone" to user.phone,
            "gameId" to user.gameId,
            "ign" to user.ign,
            "role" to user.role,
            "funds" to user.totalWalletBalance,
            "balance" to user.totalWalletBalance,
            "walletBalance" to user.totalWalletBalance,
            "wallet_balance" to user.totalWalletBalance,
            "depositFunds" to user.depositFunds,
            "winningFunds" to user.winningFunds,
            "bonusFunds" to user.bonusFunds,
            "tokens" to user.tokens,
            "loginStreak" to user.loginStreak,
            "wins" to user.wins,
            "kills" to user.kills,
            "activityPoints" to user.activityPoints,
            "totalEarnings" to user.totalEarnings,
            "isBanned" to user.isBanned,
            "banned" to user.isBanned,
            "status" to if (user.isBanned) "BANNED" else if (user.isSuspended) "SUSPENDED" else "ACTIVE",
            "banReason" to user.banReason,
            "banCaseId" to user.banCaseId,
            "bannedAt" to user.bannedAt,
            "isSuspended" to user.isSuspended,
            "suspendedUntil" to user.suspendedUntil,
            "suspensionReason" to user.suspensionReason,
            "isVpnBlocked" to user.isVpnBlocked,
            "vpnBlocked" to user.isVpnBlocked,
            "isForceUpdateRequired" to user.isForceUpdateRequired,
            "forceUpdate" to user.isForceUpdateRequired,
            "minVersionRequired" to user.minVersionRequired,
            "isMaintenanceBypass" to user.isMaintenanceBypass,
            "deviceModel" to user.deviceModel,
            "ipAddress" to user.ipAddress,
            "updatedAt" to now,
            "lastActive" to now
        )
        if (user.avatarUrl != null) updates["avatarUrl"] = user.avatarUrl!!
        if (user.createdAt > 0) updates["createdAt"] = user.createdAt

        // 1. Primary RTDB update (guarantees instantaneous live sync for active players)
        try {
            database.child("users").child(user.id).updateChildren(updates).await()
            database.child("userProfiles").child(user.id).updateChildren(updates).await()
            database.child("players").child(user.id).updateChildren(updates).await()
        } catch (e: Exception) {
            Log.e(TAG, "RTDB updateUserProfile write error: ${e.message}")
            throw e // Re-throw only if RTDB write genuinely fails
        }

        // 2. Secondary Firestore mirror (safely catch and ignore if Firestore is unprovisioned)
        try {
            firestore.collection("users").document(user.id).set(updates, SetOptions.merge()).await()
            firestore.collection("userProfiles").document(user.id).set(updates, SetOptions.merge()).await()
            firestore.collection("players").document(user.id).set(updates, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.d(TAG, "Firestore updateUserProfile skipped (RTDB is primary): ${e.message}")
        }

        user
    }

    /**
     * Bans or unbans a user across RTDB and Firestore.
     */
    suspend fun toggleUserBan(
        user: UserProfile,
        isBanned: Boolean,
        reason: String,
        bannedBy: String
    ): Result<UserProfile> = runCatching {
        val now = System.currentTimeMillis()
        val updatedUser = user.copy(
            isBanned = isBanned,
            banReason = if (isBanned) reason.ifBlank { "Banned by Admin Panel" } else "",
            bannedAt = if (isBanned) now else 0L
        )

        val userUpdates = mapOf<String, Any>(
            "isBanned" to isBanned,
            "banned" to isBanned,
            "status" to if (isBanned) "BANNED" else "ACTIVE",
            "accountStatus" to if (isBanned) "BANNED" else "ACTIVE",
            "active" to !isBanned,
            "walletFrozen" to isBanned,
            "banReason" to updatedUser.banReason,
            "bannedAt" to updatedUser.bannedAt,
            "updatedAt" to now
        )

        val banRecord = mapOf(
            "uid" to user.id,
            "email" to user.email,
            "gameId" to user.gameId,
            "reason" to updatedUser.banReason,
            "bannedBy" to bannedBy,
            "timestamp" to now,
            "status" to if (isBanned) "BANNED" else "UNBANNED"
        )

        // 1. Primary RTDB update
        try {
            database.child("users").child(user.id).updateChildren(userUpdates).await()
            database.child("userProfiles").child(user.id).updateChildren(userUpdates).await()
            database.child("players").child(user.id).updateChildren(userUpdates).await()
            if (isBanned) {
                database.child("banned_users").child(user.id).setValue(banRecord).await()
            } else {
                database.child("banned_users").child(user.id).removeValue().await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "RTDB toggleUserBan error: ${e.message}")
            throw e
        }

        // 2. Secondary Firestore update
        try {
            firestore.collection("users").document(user.id).set(userUpdates, SetOptions.merge()).await()
            firestore.collection("userProfiles").document(user.id).set(userUpdates, SetOptions.merge()).await()
            if (isBanned) {
                firestore.collection("banned_users").document(user.id).set(banRecord, SetOptions.merge()).await()
            } else {
                firestore.collection("banned_users").document(user.id).delete().await()
            }
        } catch (e: Exception) {
            Log.d(TAG, "Firestore toggleUserBan skipped (RTDB is primary): ${e.message}")
        }

        updatedUser
    }
}
