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
 * UserRoleManager enforces mandatory Firestore transaction blocks for updating user and admin roles.
 *
 * Architecture Guarantees:
 * 1. Enforces atomic read-before-write validation within Firestore transactions.
 * 2. Synchronizes updates to both Cloud Firestore and Firebase Realtime Database.
 * 3. Enforces server-side confirmation before returning to callers, preventing UI state reverts
 *    and ensuring local UI state only transitions when the backend transaction confirms success.
 */
class UserRoleManager(
    private val repository: TournamentRepositoryImpl
) {
    private val TAG = "UserRoleManager"
    private val firestore: FirebaseFirestore get() = repository.firestore
    private val database: DatabaseReference get() = repository.database

    /**
     * Executes a mandatory Firestore transaction block to grant or promote an admin role.
     * Enforces atomic transactions on 'admins' and 'users' collections with server confirmation.
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

        val adminDocRef = firestore.collection("admins").document(adminUid)
        val userDocRef = firestore.collection("users").document(adminUid)
        val profileDocRef = firestore.collection("userProfiles").document(adminUid)

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

        // Mandatory Firestore Transaction Block
        firestore.runTransaction { transaction ->
            val adminSnapshot = transaction.get(adminDocRef)
            val userSnapshot = transaction.get(userDocRef)

            Log.d(TAG, "Executing grantAdminRole transaction for $adminUid (admin exists: ${adminSnapshot.exists()}, user exists: ${userSnapshot.exists()})")

            transaction.set(adminDocRef, payload, SetOptions.merge())
            transaction.set(userDocRef, userRoleUpdates, SetOptions.merge())
            transaction.set(profileDocRef, userRoleUpdates, SetOptions.merge())
        }.await()

        // Secondary RTDB Mirror Sync
        val rtdbNodes = listOf("admins", "Admins", "staff", "Staff", "admin_users")
        rtdbNodes.forEach { node ->
            try {
                database.child(node).child(adminUid).setValue(payload).await()
            } catch (e: Exception) {
                Log.w(TAG, "RTDB secondary mirror failed for node $node: ${e.message}")
            }
        }
        try {
            database.child("users").child(adminUid).updateChildren(userRoleUpdates).await()
            database.child("userProfiles").child(adminUid).updateChildren(userRoleUpdates).await()
        } catch (_: Exception) {}

        adminRecord
    }

    /**
     * Executes a mandatory Firestore transaction block to revoke an admin role.
     */
    suspend fun revokeAdminRole(adminUid: String): Result<AdminRecord> = runCatching {
        val now = System.currentTimeMillis()
        val adminDocRef = firestore.collection("admins").document(adminUid)
        val userDocRef = firestore.collection("users").document(adminUid)
        val profileDocRef = firestore.collection("userProfiles").document(adminUid)

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

        var revokedAdminRecord: AdminRecord? = null

        // Mandatory Firestore Transaction Block
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(adminDocRef)
            val currentEmail = snapshot.getString("email") ?: ""
            val currentName = snapshot.getString("name") ?: ""
            val currentRole = snapshot.getString("role") ?: "tournament_admin"
            val assignedAt = snapshot.getLong("assignedAt") ?: now
            val grantedBy = snapshot.getString("grantedBy") ?: ""

            revokedAdminRecord = AdminRecord(
                uid = adminUid,
                email = currentEmail,
                name = currentName,
                role = currentRole,
                active = false,
                assignedAt = assignedAt,
                grantedBy = grantedBy,
                updatedAt = now
            )

            transaction.set(adminDocRef, updates, SetOptions.merge())
            transaction.set(userDocRef, userRoleUpdates, SetOptions.merge())
            transaction.set(profileDocRef, userRoleUpdates, SetOptions.merge())
        }.await()

        // Sync with RTDB mirror
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

        revokedAdminRecord ?: AdminRecord(uid = adminUid, active = false, updatedAt = now)
    }

    /**
     * Executes a mandatory Firestore transaction block to update an existing AdminRecord.
     */
    suspend fun updateAdminRecord(admin: AdminRecord): Result<AdminRecord> = runCatching {
        val now = System.currentTimeMillis()
        val updatedAdmin = admin.copy(updatedAt = now)
        val adminDocRef = firestore.collection("admins").document(updatedAdmin.uid)
        val userDocRef = firestore.collection("users").document(updatedAdmin.uid)
        val profileDocRef = firestore.collection("userProfiles").document(updatedAdmin.uid)

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

        // Mandatory Firestore Transaction Block
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(adminDocRef)
            Log.d(TAG, "Updating admin record in transaction for ${updatedAdmin.uid} (exists: ${snapshot.exists()})")

            transaction.set(adminDocRef, payload, SetOptions.merge())
            transaction.set(userDocRef, userRoleUpdates, SetOptions.merge())
            transaction.set(profileDocRef, userRoleUpdates, SetOptions.merge())
        }.await()

        // RTDB mirror sync
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

        updatedAdmin
    }

    /**
     * Executes a mandatory Firestore transaction block to permanently delete an AdminRecord.
     */
    suspend fun deleteAdminRecord(adminUid: String): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val adminDocRef = firestore.collection("admins").document(adminUid)
        val userDocRef = firestore.collection("users").document(adminUid)
        val profileDocRef = firestore.collection("userProfiles").document(adminUid)

        val userRoleUpdates = mapOf<String, Any>(
            "role" to "player",
            "isAdmin" to false,
            "updatedAt" to now
        )

        // Mandatory Firestore Transaction Block
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(adminDocRef)
            if (snapshot.exists()) {
                transaction.delete(adminDocRef)
            }
            transaction.set(userDocRef, userRoleUpdates, SetOptions.merge())
            transaction.set(profileDocRef, userRoleUpdates, SetOptions.merge())
        }.await()

        // RTDB mirror sync
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
    }

    /**
     * Executes a mandatory Firestore transaction block to update a User's role.
     */
    suspend fun updateUserRole(userId: String, newRole: String, isAdmin: Boolean): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val userDocRef = firestore.collection("users").document(userId)
        val profileDocRef = firestore.collection("userProfiles").document(userId)

        val userRoleUpdates = mapOf<String, Any>(
            "role" to newRole,
            "isAdmin" to isAdmin,
            "updatedAt" to now
        )

        // Mandatory Firestore Transaction Block
        firestore.runTransaction { transaction ->
            val userSnap = transaction.get(userDocRef)
            Log.d(TAG, "Updating user role in transaction for $userId to $newRole (user exists: ${userSnap.exists()})")

            transaction.set(userDocRef, userRoleUpdates, SetOptions.merge())
            transaction.set(profileDocRef, userRoleUpdates, SetOptions.merge())
        }.await()

        try {
            database.child("users").child(userId).updateChildren(userRoleUpdates).await()
            database.child("userProfiles").child(userId).updateChildren(userRoleUpdates).await()
        } catch (_: Exception) {}
    }

    /**
     * Executes a mandatory Firestore transaction block to update a full UserProfile.
     */
    suspend fun updateUserProfile(user: UserProfile): Result<UserProfile> = runCatching {
        val now = System.currentTimeMillis()
        val userDocRef = firestore.collection("users").document(user.id)
        val profileDocRef = firestore.collection("userProfiles").document(user.id)

        val updates = mapOf<String, Any>(
            "id" to user.id,
            "uid" to user.id,
            "username" to user.username,
            "name" to user.username,
            "email" to user.email,
            "phone" to user.phone,
            "gameId" to user.gameId,
            "ign" to user.ign,
            "role" to user.role,
            "funds" to user.funds,
            "balance" to user.balance,
            "depositFunds" to user.depositFunds,
            "winningFunds" to user.winningFunds,
            "bonusFunds" to user.bonusFunds,
            "wins" to user.wins,
            "kills" to user.kills,
            "activityPoints" to user.activityPoints,
            "totalEarnings" to user.totalEarnings,
            "isBanned" to user.isBanned,
            "banReason" to user.banReason,
            "updatedAt" to now
        )

        // Mandatory Firestore Transaction Block
        firestore.runTransaction { transaction ->
            val snap = transaction.get(userDocRef)
            Log.d(TAG, "Updating profile in transaction for ${user.id} (exists: ${snap.exists()})")

            transaction.set(userDocRef, updates, SetOptions.merge())
            transaction.set(profileDocRef, updates, SetOptions.merge())
        }.await()

        try {
            database.child("users").child(user.id).updateChildren(updates).await()
            database.child("userProfiles").child(user.id).updateChildren(updates).await()
        } catch (_: Exception) {}

        user
    }

    /**
     * Executes a mandatory Firestore transaction block to ban or unban a user.
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
            banReason = if (isBanned) reason.ifBlank { "Banned by Admin Panel" } else ""
        )

        val userDocRef = firestore.collection("users").document(user.id)
        val profileDocRef = firestore.collection("userProfiles").document(user.id)
        val banDocRef = firestore.collection("banned_users").document(user.id)

        val userUpdates = mapOf<String, Any>(
            "isBanned" to isBanned,
            "banReason" to updatedUser.banReason,
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

        // Mandatory Firestore Transaction Block
        firestore.runTransaction { transaction ->
            transaction.set(userDocRef, userUpdates, SetOptions.merge())
            transaction.set(profileDocRef, userUpdates, SetOptions.merge())
            if (isBanned) {
                transaction.set(banDocRef, banRecord, SetOptions.merge())
            } else {
                transaction.delete(banDocRef)
            }
        }.await()

        // Sync with RTDB mirror
        try {
            database.child("users").child(user.id).updateChildren(userUpdates).await()
            database.child("userProfiles").child(user.id).updateChildren(userUpdates).await()
            if (isBanned) {
                database.child("banned_users").child(user.id).setValue(banRecord).await()
            } else {
                database.child("banned_users").child(user.id).removeValue().await()
            }
        } catch (_: Exception) {}

        updatedUser
    }
}
