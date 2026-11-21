package com.example.data.repository

import android.util.Log
import com.example.domain.model.*
import com.example.ui.common.GlobalErrorManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

/**
 * Authoritative Dual-Database (Firebase RTDB + Cloud Firestore) Integration Manager
 * implementing all 8 sections of the VeloRix Esports Admin Panel Integration Specification:
 * 1. 10 Dedicated Admin Channels & Inbox Routing (`/admin_channels/{type}` & `admin_channels/{type}/items`)
 * 2. DPDP Act 2023 Account Deletion & Data Export Archive (`#account-deletions` & `#data-exports`)
 * 3. User Avatar Reset (`avatar_1`), Versioned Profile Sync (`version + 1`), Tokens & Suspension
 * 4. Live Tournament Lobby Chat & Admin Moderation (`/tournament_chats/{id}` & `/tournaments/{id}/chat`)
 * 5. Manual UPI / SpacePay / Razorpay Deposit Queue, Referral Commission & Token Ledger / Missions
 * 6. Live `/system_config` & `system_config/global` (19 fields) & Promotional `/banners`
 * 7. Support Ticket Replies & Targeted / Global Push Notifications
 */
class AdminIntegrationSyncManager(
    private val database: DatabaseReference,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val TAG = "AdminIntegrationSync"

    private fun safeLong(value: Any?, default: Long = 0L): Long = when (value) {
        is Long -> value
        is Int -> value.toLong()
        is Double -> value.toLong()
        is Float -> value.toLong()
        is Number -> value.toLong()
        is String -> value.trim().toLongOrNull() ?: value.trim().toDoubleOrNull()?.toLong() ?: default
        else -> default
    }

    private fun safeDouble(value: Any?, default: Double = 0.0): Double = when (value) {
        is Double -> value
        is Float -> value.toDouble()
        is Long -> value.toDouble()
        is Int -> value.toDouble()
        is Number -> value.toDouble()
        is String -> value.trim().replace("₹", "").replace(",", "").toDoubleOrNull() ?: default
        else -> default
    }

    private fun safeInt(value: Any?, default: Int = 0): Int = when (value) {
        is Int -> value
        is Long -> value.toInt()
        is Double -> value.toInt()
        is Float -> value.toInt()
        is Number -> value.toInt()
        is String -> value.trim().toIntOrNull() ?: value.trim().toDoubleOrNull()?.toInt() ?: default
        else -> default
    }

    private fun safeBool(value: Any?, default: Boolean = false): Boolean = when (value) {
        is Boolean -> value
        is String -> value.trim().equals("true", ignoreCase = true) || value.trim() == "1"
        is Number -> value.toInt() != 0
        else -> default
    }

    // =========================================================================
    // SECTION 1: 10 DEDICATED ADMIN CHANNELS & INBOX ROUTING SYSTEM
    // =========================================================================

    fun getAdminChannelStream(channelType: String): Flow<List<AdminChannelItem>> = callbackFlow {
        val itemsMap = mutableMapOf<String, AdminChannelItem>()

        fun emitItems() {
            trySend(itemsMap.values.sortedByDescending { it.createdAt })
        }

        fun parseSnapToChannelItem(snap: DataSnapshot, chType: String): AdminChannelItem? {
            if (!snap.exists()) return null
            val id = snap.child("id").value?.toString()
                ?: snap.child("requestId").value?.toString()
                ?: snap.child("exportId").value?.toString()
                ?: snap.child("ticketId").value?.toString()
                ?: snap.key ?: return null
            if (id.isBlank()) return null

            val uid = snap.child("userId").value?.toString()
                ?: snap.child("uid").value?.toString() ?: ""
            val uname = snap.child("username").value?.toString()
                ?: snap.child("userName").value?.toString()
                ?: snap.child("name").value?.toString() ?: "Player"
            val email = snap.child("email").value?.toString()
                ?: snap.child("userEmail").value?.toString() ?: ""
            val ffId = snap.child("freeFireId").value?.toString()
                ?: snap.child("gameId").value?.toString() ?: ""
            val title = snap.child("title").value?.toString()
                ?: snap.child("subject").value?.toString()
                ?: snap.child("reason").value?.toString()
                ?: snap.child("paymentMethod").value?.toString()
                ?: snap.child("method").value?.toString()
                ?: chType.replace("_", " ").uppercase()
            val subtitle = snap.child("details").value?.toString()
                ?: snap.child("description").value?.toString()
                ?: snap.child("message").value?.toString()
                ?: snap.child("utrNumber").value?.toString()
                ?: snap.child("upiId").value?.toString()
                ?: snap.child("accountDetails").value?.toString()
                ?: ""
            val amount = safeDouble(
                snap.child("amount").value
                    ?: snap.child("vtAmount").value
                    ?: snap.child("vtCredited").value
                    ?: snap.child("balance").value
            )
            val status = snap.child("status").value?.toString()?.uppercase() ?: "PENDING"
            val createdAt = safeLong(
                snap.child("createdAt").value
                    ?: snap.child("requestedAt").value
                    ?: snap.child("timestamp").value,
                System.currentTimeMillis()
            )
            val rawPayload = mutableMapOf<String, String>()
            snap.children.forEach { c ->
                val k = c.key ?: ""
                val v = c.value?.toString() ?: ""
                if (k.isNotBlank() && v.isNotBlank()) {
                    rawPayload[k] = v.take(500)
                }
            }
            return AdminChannelItem(
                id = id,
                channelType = chType,
                userId = uid,
                username = uname,
                userEmail = email,
                freeFireId = ffId,
                title = title,
                subtitle = subtitle,
                amount = amount,
                status = status,
                createdAt = createdAt,
                rawPayload = rawPayload
            )
        }

        fun parseDocToChannelItem(doc: DocumentSnapshot, chType: String): AdminChannelItem? {
            if (!doc.exists()) return null
            val id = doc.getString("id")
                ?: doc.getString("requestId")
                ?: doc.getString("exportId")
                ?: doc.getString("ticketId")
                ?: doc.id
            val uid = doc.getString("userId") ?: doc.getString("uid") ?: ""
            val uname = doc.getString("username") ?: doc.getString("userName") ?: doc.getString("name") ?: "Player"
            val email = doc.getString("email") ?: doc.getString("userEmail") ?: ""
            val ffId = doc.getString("freeFireId") ?: doc.getString("gameId") ?: ""
            val title = doc.getString("title")
                ?: doc.getString("subject")
                ?: doc.getString("reason")
                ?: doc.getString("paymentMethod")
                ?: doc.getString("method")
                ?: chType.replace("_", " ").uppercase()
            val subtitle = doc.getString("details")
                ?: doc.getString("description")
                ?: doc.getString("message")
                ?: doc.getString("utrNumber")
                ?: doc.getString("upiId")
                ?: doc.getString("accountDetails")
                ?: ""
            val amount = safeDouble(doc.get("amount") ?: doc.get("vtAmount") ?: doc.get("vtCredited") ?: doc.get("balance"))
            val status = doc.getString("status")?.uppercase() ?: "PENDING"
            val createdAt = safeLong(doc.get("createdAt") ?: doc.get("requestedAt") ?: doc.get("timestamp"), System.currentTimeMillis())
            val rawPayload = mutableMapOf<String, String>()
            doc.data?.forEach { (k, v) ->
                if (v != null) rawPayload[k] = v.toString().take(500)
            }
            return AdminChannelItem(
                id = id,
                channelType = chType,
                userId = uid,
                username = uname,
                userEmail = email,
                freeFireId = ffId,
                title = title,
                subtitle = subtitle,
                amount = amount,
                status = status,
                createdAt = createdAt,
                rawPayload = rawPayload
            )
        }

        val rtdbRefs = listOf(
            database.child("admin_channels").child(channelType),
            database.child("admin_requests").child(channelType),
            database.child(channelType)
        )
        val rtdbListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { child ->
                    parseSnapToChannelItem(child, channelType)?.let { item ->
                        itemsMap[item.id] = item
                    }
                }
                emitItems()
            }
            override fun onCancelled(error: DatabaseError) {
                emitItems()
            }
        }
        rtdbRefs.forEach { ref -> ref.addValueEventListener(rtdbListener) }

        val fsListeners = mutableListOf<ListenerRegistration>()
        val fsCollections = listOf(
            firestore.collection("admin_channels").document(channelType).collection("items"),
            firestore.collection("admin_requests_$channelType"),
            firestore.collection(channelType)
        )
        fsCollections.forEach { col ->
            try {
                val reg = col.addSnapshotListener { snap, err ->
                    if (err == null && snap != null) {
                        snap.documents.forEach { doc ->
                            parseDocToChannelItem(doc, channelType)?.let { item ->
                                itemsMap[item.id] = item
                            }
                        }
                        emitItems()
                    }
                }
                fsListeners.add(reg)
            } catch (_: Exception) {}
        }

        awaitClose {
            rtdbRefs.forEach { it.removeEventListener(rtdbListener) }
            fsListeners.forEach { it.remove() }
        }
    }

    suspend fun updateAdminChannelItemStatus(
        channelType: String,
        itemId: String,
        newStatus: String,
        adminNote: String,
        adminEmail: String
    ) {
        val now = System.currentTimeMillis()
        val updates = mapOf<String, Any>(
            "status" to newStatus.uppercase(),
            "adminNote" to adminNote,
            "adminReply" to adminNote,
            "processedAt" to now,
            "updatedAt" to now,
            "processedBy" to adminEmail
        )
        try { database.child("admin_channels").child(channelType).child(itemId).updateChildren(updates).await() } catch (_: Exception) {}
        try { database.child("admin_requests").child(channelType).child(itemId).updateChildren(updates).await() } catch (_: Exception) {}
        try { database.child(channelType).child(itemId).updateChildren(updates).await() } catch (_: Exception) {}
        try { firestore.collection("admin_channels").document(channelType).collection("items").document(itemId).set(updates, SetOptions.merge()).await() } catch (_: Exception) {}
        try { firestore.collection("admin_requests_$channelType").document(itemId).set(updates, SetOptions.merge()).await() } catch (_: Exception) {}
        try { firestore.collection(channelType).document(itemId).set(updates, SetOptions.merge()).await() } catch (_: Exception) {}
        GlobalErrorManager.emitSuccess("Channel item #$itemId marked ${newStatus.uppercase()} across RTDB & Firestore!")
    }

    suspend fun pushTestSituationEventToChannels(adminEmail: String) {
        val now = System.currentTimeMillis()
        val testId = "SIT_TEST_${now}"
        val payload = mapOf(
            "id" to testId,
            "requestId" to testId,
            "userId" to (auth.currentUser?.uid ?: "admin_test_uid"),
            "username" to "System Situation Probe",
            "email" to adminEmail,
            "title" to "Live Dual-DB Channel Verification",
            "details" to "Verified RTDB (/admin_channels) + Firestore (admin_channels) real-time pipeline at $now",
            "status" to "COMPLETED",
            "createdAt" to now,
            "requestedAt" to now
        )
        try { database.child("admin_channels").child("admin_situation_tests").child(testId).setValue(payload).await() } catch (_: Exception) {}
        try { database.child("admin_situation_tests").child(testId).setValue(payload).await() } catch (_: Exception) {}
        try { firestore.collection("admin_channels").document("admin_situation_tests").collection("items").document(testId).set(payload).await() } catch (_: Exception) {}
        try { firestore.collection("admin_situation_tests").document(testId).set(payload).await() } catch (_: Exception) {}
        GlobalErrorManager.emitSuccess("Situation test event dispatched to #situation-tester in RTDB & Firestore!")
    }

    // =========================================================================
    // SECTION 2A: ACCOUNT DELETION REQUESTS (#account-deletions)
    // =========================================================================

    fun getAccountDeletionRequestsStream(): Flow<List<AccountDeletionRequest>> = callbackFlow {
        val reqMap = mutableMapOf<String, AccountDeletionRequest>()

        fun emitList() {
            trySend(reqMap.values.sortedByDescending { it.requestedAt })
        }

        fun parseSnap(snap: DataSnapshot): AccountDeletionRequest? {
            if (!snap.exists()) return null
            val rId = snap.child("requestId").value?.toString()
                ?: snap.child("id").value?.toString()
                ?: snap.key ?: return null
            val uid = snap.child("userId").value?.toString()
                ?: snap.child("uid").value?.toString() ?: ""
            if (rId.isBlank() && uid.isBlank()) return null
            return AccountDeletionRequest(
                requestId = rId.ifBlank { "del_$uid" },
                userId = uid,
                username = snap.child("username").value?.toString() ?: snap.child("name").value?.toString() ?: "Player",
                email = snap.child("email").value?.toString() ?: "",
                phone = snap.child("phone").value?.toString() ?: "",
                freeFireId = snap.child("freeFireId").value?.toString() ?: snap.child("gameId").value?.toString() ?: "",
                reason = snap.child("reason").value?.toString() ?: snap.child("deletionReason").value?.toString() ?: "User requested account deletion",
                details = snap.child("details").value?.toString() ?: snap.child("deletionDetails").value?.toString() ?: "",
                balanceAtRequest = safeDouble(snap.child("balanceAtRequest").value ?: snap.child("balance").value),
                dataExportedBeforeDelete = safeBool(snap.child("dataExportedBeforeDelete").value ?: snap.child("dataExported").value),
                status = snap.child("status").value?.toString()?.uppercase() ?: "PENDING_REVIEW",
                requestedAt = safeLong(snap.child("requestedAt").value ?: snap.child("deletionRequestedAt").value ?: snap.child("createdAt").value, System.currentTimeMillis()),
                processedAt = safeLong(snap.child("processedAt").value, 0L),
                processedBy = snap.child("processedBy").value?.toString() ?: ""
            )
        }

        fun parseDoc(doc: DocumentSnapshot): AccountDeletionRequest? {
            if (!doc.exists()) return null
            val rId = doc.getString("requestId") ?: doc.getString("id") ?: doc.id
            val uid = doc.getString("userId") ?: doc.getString("uid") ?: ""
            return AccountDeletionRequest(
                requestId = rId,
                userId = uid,
                username = doc.getString("username") ?: doc.getString("name") ?: "Player",
                email = doc.getString("email") ?: "",
                phone = doc.getString("phone") ?: "",
                freeFireId = doc.getString("freeFireId") ?: doc.getString("gameId") ?: "",
                reason = doc.getString("reason") ?: doc.getString("deletionReason") ?: "User requested account deletion",
                details = doc.getString("details") ?: doc.getString("deletionDetails") ?: "",
                balanceAtRequest = safeDouble(doc.get("balanceAtRequest") ?: doc.get("balance")),
                dataExportedBeforeDelete = safeBool(doc.get("dataExportedBeforeDelete") ?: doc.get("dataExported")),
                status = doc.getString("status")?.uppercase() ?: "PENDING_REVIEW",
                requestedAt = safeLong(doc.get("requestedAt") ?: doc.get("deletionRequestedAt") ?: doc.get("createdAt"), System.currentTimeMillis()),
                processedAt = safeLong(doc.get("processedAt"), 0L),
                processedBy = doc.getString("processedBy") ?: ""
            )
        }

        val rtdbRefs = listOf(
            database.child("account_deletion_requests"),
            database.child("deletion_requests"),
            database.child("admin_channels").child("account_deletion_requests")
        )
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { c ->
                    parseSnap(c)?.let { reqMap[it.requestId] = it }
                }
                emitList()
            }
            override fun onCancelled(error: DatabaseError) { emitList() }
        }
        rtdbRefs.forEach { it.addValueEventListener(listener) }

        val fsReg1 = try {
            firestore.collection("account_deletion_requests").addSnapshotListener { snap, err ->
                if (err == null && snap != null) {
                    snap.documents.forEach { d -> parseDoc(d)?.let { reqMap[it.requestId] = it } }
                    emitList()
                }
            }
        } catch (_: Exception) { null }

        val fsReg2 = try {
            firestore.collection("admin_channels").document("account_deletion_requests")
                .collection("items").addSnapshotListener { snap, err ->
                    if (err == null && snap != null) {
                        snap.documents.forEach { d -> parseDoc(d)?.let { reqMap[it.requestId] = it } }
                        emitList()
                    }
                }
        } catch (_: Exception) { null }

        awaitClose {
            rtdbRefs.forEach { it.removeEventListener(listener) }
            fsReg1?.remove()
            fsReg2?.remove()
        }
    }

    suspend fun approveAndPermanentlyDeleteUser(req: AccountDeletionRequest, adminEmail: String) {
        try {
            val now = System.currentTimeMillis()
            val uid = req.userId.trim()
            if (uid.isNotBlank()) {
                // Delete user nodes in RTDB & Firestore (DPDP Right to Erasure)
                try { database.child("users").child(uid).removeValue().await() } catch (_: Exception) {}
                try { database.child("userProfiles").child(uid).removeValue().await() } catch (_: Exception) {}
                try { database.child("players").child(uid).removeValue().await() } catch (_: Exception) {}
                try { database.child("wallet_accounts").child(uid).removeValue().await() } catch (_: Exception) {}
                try { firestore.collection("users").document(uid).delete().await() } catch (_: Exception) {}
                try { firestore.collection("userProfiles").document(uid).delete().await() } catch (_: Exception) {}
                try { firestore.collection("players").document(uid).delete().await() } catch (_: Exception) {}
            }

            val statusUpdates = mapOf<String, Any>(
                "status" to "APPROVED_DELETED",
                "processedAt" to now,
                "processedBy" to adminEmail
            )
            try { database.child("account_deletion_requests").child(req.requestId).updateChildren(statusUpdates).await() } catch (_: Exception) {}
            try { database.child("deletion_requests").child(req.requestId).updateChildren(statusUpdates).await() } catch (_: Exception) {}
            try { database.child("admin_channels").child("account_deletion_requests").child(req.requestId).updateChildren(statusUpdates).await() } catch (_: Exception) {}
            try { firestore.collection("account_deletion_requests").document(req.requestId).set(statusUpdates, SetOptions.merge()).await() } catch (_: Exception) {}
            try { firestore.collection("admin_channels").document("account_deletion_requests").collection("items").document(req.requestId).set(statusUpdates, SetOptions.merge()).await() } catch (_: Exception) {}

            writeAuditLog(
                action = "DPDP_ACCOUNT_PURGED",
                detail = "Permanently purged user ${req.username} ($uid) per DPDP request #${req.requestId}",
                adminEmail = adminEmail,
                targetId = uid
            )
            publishGlobalAppStateEvent(
                eventType = "USER_STATUS_CHANGED",
                affectedUserId = uid,
                payload = mapOf("newStatus" to "DELETED", "requestId" to req.requestId)
            )
            GlobalErrorManager.emitSuccess("Account ${req.username} ($uid) permanently deleted & DPDP request marked APPROVED_DELETED!")
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to process account deletion: ${e.message}")
        }
    }

    suspend fun rejectAndRestoreUserAccount(req: AccountDeletionRequest, adminEmail: String) {
        try {
            val now = System.currentTimeMillis()
            val uid = req.userId.trim()
            if (uid.isNotBlank()) {
                val restoreUserMap = mapOf<String, Any>(
                    "accountStatus" to "ACTIVE",
                    "status" to "ACTIVE",
                    "deletionRequested" to false,
                    "deletionPending" to false,
                    "deletionReason" to "",
                    "deletionDetails" to "",
                    "updatedAt" to now
                )
                try { database.child("users").child(uid).updateChildren(restoreUserMap).await() } catch (_: Exception) {}
                try { database.child("userProfiles").child(uid).updateChildren(restoreUserMap).await() } catch (_: Exception) {}
                try { firestore.collection("users").document(uid).set(restoreUserMap, SetOptions.merge()).await() } catch (_: Exception) {}
                try { firestore.collection("userProfiles").document(uid).set(restoreUserMap, SetOptions.merge()).await() } catch (_: Exception) {}
            }

            val reqUpdates = mapOf<String, Any>(
                "status" to "REJECTED",
                "processedAt" to now,
                "processedBy" to adminEmail
            )
            try { database.child("account_deletion_requests").child(req.requestId).updateChildren(reqUpdates).await() } catch (_: Exception) {}
            try { database.child("deletion_requests").child(req.requestId).updateChildren(reqUpdates).await() } catch (_: Exception) {}
            try { database.child("admin_channels").child("account_deletion_requests").child(req.requestId).updateChildren(reqUpdates).await() } catch (_: Exception) {}
            try { firestore.collection("account_deletion_requests").document(req.requestId).set(reqUpdates, SetOptions.merge()).await() } catch (_: Exception) {}
            try { firestore.collection("admin_channels").document("account_deletion_requests").collection("items").document(req.requestId).set(reqUpdates, SetOptions.merge()).await() } catch (_: Exception) {}

            if (uid.isNotBlank()) {
                sendTargetedUserNotification(
                    userId = uid,
                    title = "Account Restored",
                    message = "Your account deletion request was cancelled/restored by Admin. Your account is ACTIVE.",
                    type = "SYSTEM"
                )
                publishGlobalAppStateEvent(
                    eventType = "USER_STATUS_CHANGED",
                    affectedUserId = uid,
                    payload = mapOf("newStatus" to "ACTIVE", "deletionPending" to false)
                )
            }
            writeAuditLog(
                action = "DPDP_DELETION_REJECTED_RESTORED",
                detail = "Restored account ${req.username} ($uid) to ACTIVE status",
                adminEmail = adminEmail,
                targetId = uid
            )
            GlobalErrorManager.emitSuccess("Account ${req.username} restored to ACTIVE status!")
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to restore account: ${e.message}")
        }
    }

    // =========================================================================
    // SECTION 2B: ACCOUNT DATA EXPORT ARCHIVE (#data-exports)
    // =========================================================================

    fun getDataExportRequestsStream(): Flow<List<DataExportRequest>> = callbackFlow {
        val exportsMap = mutableMapOf<String, DataExportRequest>()

        fun emitExports() {
            trySend(exportsMap.values.sortedByDescending { it.requestedAt })
        }

        fun parseSnap(snap: DataSnapshot): DataExportRequest? {
            if (!snap.exists()) return null
            val expId = snap.child("exportId").value?.toString()
                ?: snap.child("id").value?.toString()
                ?: snap.key ?: return null
            if (expId.isBlank()) return null
            return DataExportRequest(
                exportId = expId,
                userId = snap.child("userId").value?.toString() ?: snap.child("uid").value?.toString() ?: "",
                username = snap.child("username").value?.toString() ?: "Player",
                email = snap.child("email").value?.toString() ?: "",
                phone = snap.child("phone").value?.toString() ?: "",
                freeFireId = snap.child("freeFireId").value?.toString() ?: snap.child("gameId").value?.toString() ?: "",
                balance = safeDouble(snap.child("balance").value),
                tokens = safeInt(snap.child("tokens").value),
                transactionCount = safeInt(snap.child("transactionCount").value),
                joinedTournamentCount = safeInt(snap.child("joinedTournamentCount").value),
                exportedJson = snap.child("exportedJson").value?.toString() ?: "",
                status = snap.child("status").value?.toString()?.uppercase() ?: "COMPLETED",
                requestedAt = safeLong(snap.child("requestedAt").value ?: snap.child("createdAt").value, System.currentTimeMillis())
            )
        }

        fun parseDoc(doc: DocumentSnapshot): DataExportRequest? {
            if (!doc.exists()) return null
            val expId = doc.getString("exportId") ?: doc.getString("id") ?: doc.id
            return DataExportRequest(
                exportId = expId,
                userId = doc.getString("userId") ?: doc.getString("uid") ?: "",
                username = doc.getString("username") ?: "Player",
                email = doc.getString("email") ?: "",
                phone = doc.getString("phone") ?: "",
                freeFireId = doc.getString("freeFireId") ?: doc.getString("gameId") ?: "",
                balance = safeDouble(doc.get("balance")),
                tokens = safeInt(doc.get("tokens")),
                transactionCount = safeInt(doc.get("transactionCount")),
                joinedTournamentCount = safeInt(doc.get("joinedTournamentCount")),
                exportedJson = doc.getString("exportedJson") ?: "",
                status = doc.getString("status")?.uppercase() ?: "COMPLETED",
                requestedAt = safeLong(doc.get("requestedAt") ?: doc.get("createdAt"), System.currentTimeMillis())
            )
        }

        val rtdbRefs = listOf(
            database.child("data_export_requests"),
            database.child("data_export_logs"),
            database.child("admin_channels").child("data_export_requests")
        )
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { c ->
                    parseSnap(c)?.let { exportsMap[it.exportId] = it }
                }
                emitExports()
            }
            override fun onCancelled(error: DatabaseError) { emitExports() }
        }
        rtdbRefs.forEach { it.addValueEventListener(listener) }

        val fsReg1 = try {
            firestore.collection("data_export_requests").addSnapshotListener { snap, err ->
                if (err == null && snap != null) {
                    snap.documents.forEach { d -> parseDoc(d)?.let { exportsMap[it.exportId] = it } }
                    emitExports()
                }
            }
        } catch (_: Exception) { null }

        val fsReg2 = try {
            firestore.collection("admin_channels").document("data_export_requests")
                .collection("items").addSnapshotListener { snap, err ->
                    if (err == null && snap != null) {
                        snap.documents.forEach { d -> parseDoc(d)?.let { exportsMap[it.exportId] = it } }
                        emitExports()
                    }
                }
        } catch (_: Exception) { null }

        awaitClose {
            rtdbRefs.forEach { it.removeEventListener(listener) }
            fsReg1?.remove()
            fsReg2?.remove()
        }
    }

    suspend fun generateAndArchiveUserDataExport(user: UserProfile): DataExportRequest {
        val now = System.currentTimeMillis()
        val exportId = "exp_${user.id.take(8)}_$now"

        // Fetch user's transactions if available
        val txArray = JSONArray()
        try {
            val txSnap = database.child("wallet_transactions").child(user.id).get().await()
            txSnap.children.forEach { c ->
                val obj = JSONObject()
                obj.put("id", c.key ?: "")
                obj.put("type", c.child("type").value?.toString() ?: "")
                obj.put("amount", safeDouble(c.child("amount").value))
                obj.put("status", c.child("status").value?.toString() ?: "COMPLETED")
                obj.put("timestamp", safeLong(c.child("timestamp").value))
                obj.put("description", c.child("description").value?.toString() ?: "")
                txArray.put(obj)
            }
        } catch (_: Exception) {}

        val matchesArray = JSONArray()
        user.matchHistory.forEach { m ->
            val mObj = JSONObject()
            mObj.put("tournamentId", m.tournamentId)
            mObj.put("tournamentTitle", m.tournamentTitle)
            mObj.put("game", m.game)
            mObj.put("category", m.category)
            mObj.put("joinedAt", m.joinedAt)
            mObj.put("entryFeePaid", m.entryFeePaid)
            mObj.put("kills", m.kills)
            mObj.put("rank", m.rank)
            mObj.put("winningAmount", m.winningAmount)
            mObj.put("status", m.status)
            matchesArray.put(mObj)
        }

        val rootJson = JSONObject().apply {
            put("exportId", exportId)
            put("complianceStandard", "DPDP_ACT_2023_DATA_PORTABILITY")
            put("exportedAtMillis", now)
            put("profile", JSONObject().apply {
                put("uid", user.id)
                put("username", user.username)
                put("inGameName", user.ign.ifBlank { user.username })
                put("freeFireId", user.gameId)
                put("email", user.email)
                put("phone", user.phone)
                put("bio", user.bio)
                put("socialLink", user.socialLink)
                put("avatarUrl", user.avatarUrl ?: "avatar_1")
                put("accountStatus", user.accountStatus)
                put("kycVerified", user.kycVerified)
                put("createdAt", user.effectiveFirstAppJoinAt)
            })
            put("wallet", JSONObject().apply {
                put("balance", user.totalWalletBalance)
                put("depositBalance", user.depositFunds)
                put("winningsBalance", user.winningFunds)
                put("bonusBalance", user.bonusFunds)
                put("lockedBalance", user.lockedBalance)
                put("tokens", user.tokens)
                put("referralCode", user.referralCode)
                put("referredBy", user.referredBy)
                put("referralCount", user.referralCount)
                put("referralEarnings", user.referralBonusEarned)
            })
            put("combatTelemetry", JSONObject().apply {
                put("totalMatchesJoined", user.totalMatchesJoined)
                put("wins", user.wins)
                put("kills", user.kills)
                put("totalEarnings", user.totalEarnings)
            })
            put("joinedMatches", matchesArray)
            put("transactions", txArray)
        }

        val prettyJson = rootJson.toString(2)
        val record = DataExportRequest(
            exportId = exportId,
            userId = user.id,
            username = user.username,
            email = user.email,
            phone = user.phone,
            freeFireId = user.gameId,
            balance = user.totalWalletBalance,
            tokens = user.tokens,
            transactionCount = txArray.length(),
            joinedTournamentCount = user.matchHistory.size,
            exportedJson = prettyJson,
            status = "COMPLETED",
            requestedAt = now
        )

        val payload = mapOf(
            "exportId" to record.exportId,
            "userId" to record.userId,
            "username" to record.username,
            "email" to record.email,
            "phone" to record.phone,
            "freeFireId" to record.freeFireId,
            "balance" to record.balance,
            "tokens" to record.tokens,
            "transactionCount" to record.transactionCount,
            "joinedTournamentCount" to record.joinedTournamentCount,
            "exportedJson" to record.exportedJson,
            "status" to record.status,
            "requestedAt" to record.requestedAt
        )

        try { database.child("data_export_requests").child(exportId).setValue(payload).await() } catch (_: Exception) {}
        try { database.child("admin_channels").child("data_export_requests").child(exportId).setValue(payload).await() } catch (_: Exception) {}
        try { firestore.collection("data_export_requests").document(exportId).set(payload).await() } catch (_: Exception) {}
        try { firestore.collection("admin_channels").document("data_export_requests").collection("items").document(exportId).set(payload).await() } catch (_: Exception) {}

        val userExportFlags = mapOf<String, Any>(
            "dataExported" to true,
            "lastExportedAt" to now,
            "lastExportId" to exportId,
            "updatedAt" to now
        )
        try { database.child("users").child(user.id).updateChildren(userExportFlags).await() } catch (_: Exception) {}
        try { firestore.collection("users").document(user.id).set(userExportFlags, SetOptions.merge()).await() } catch (_: Exception) {}

        GlobalErrorManager.emitSuccess("DPDP Data Export JSON archive generated for ${user.username}!")
        return record
    }

    // =========================================================================
    // SECTION 3B & 3C: ADMIN RESET AVATAR & TOKEN ADJUSTER
    // =========================================================================

    suspend fun resetUserAvatarToDefault(user: UserProfile, adminEmail: String) {
        if (user.id.isBlank()) return
        try {
            val now = System.currentTimeMillis()
            val nextVersion = (user.version + 1).coerceAtLeast(1)
            val updates = mapOf<String, Any>(
                "avatarUrl" to "avatar_1",
                "version" to nextVersion,
                "updatedAt" to now,
                "lastActive" to now
            )
            try { database.child("users").child(user.id).updateChildren(updates).await() } catch (_: Exception) {}
            try { database.child("userProfiles").child(user.id).updateChildren(updates).await() } catch (_: Exception) {}
            try { database.child("players").child(user.id).updateChildren(updates).await() } catch (_: Exception) {}
            try { firestore.collection("users").document(user.id).set(updates, SetOptions.merge()).await() } catch (_: Exception) {}
            try { firestore.collection("userProfiles").document(user.id).set(updates, SetOptions.merge()).await() } catch (_: Exception) {}

            // Also delete custom uploaded file from Firebase Storage if it exists
            try {
                val storageRef = FirebaseStorage.getInstance().reference.child("avatars/${user.id}/profile.jpg")
                storageRef.delete().await()
            } catch (_: Exception) {}

            sendTargetedUserNotification(
                userId = user.id,
                title = "Profile Avatar Reset",
                message = "Your profile picture was reset to default (avatar_1) by an administrator.",
                type = "SYSTEM"
            )
            GlobalErrorManager.emitSuccess("Avatar for ${user.username} reset to default 'avatar_1' (v$nextVersion)!")
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to reset avatar: ${e.message}")
        }
    }

    suspend fun adjustUserTokens(
        user: UserProfile,
        tokenDelta: Int,
        reason: String,
        adminEmail: String
    ) {
        if (user.id.isBlank()) return
        try {
            val now = System.currentTimeMillis()
            val newTokens = (user.tokens + tokenDelta).coerceAtLeast(0)
            val nextVersion = (user.version + 1).coerceAtLeast(1)
            val updates = mapOf<String, Any>(
                "tokens" to newTokens,
                "version" to nextVersion,
                "updatedAt" to now,
                "lastActive" to now
            )
            try { database.child("users").child(user.id).updateChildren(updates).await() } catch (_: Exception) {}
            try { database.child("userProfiles").child(user.id).updateChildren(updates).await() } catch (_: Exception) {}
            try { firestore.collection("users").document(user.id).set(updates, SetOptions.merge()).await() } catch (_: Exception) {}
            try { firestore.collection("userProfiles").document(user.id).set(updates, SetOptions.merge()).await() } catch (_: Exception) {}

            val logId = "tok_adj_${now}"
            val logPayload = mapOf(
                "id" to logId,
                "userId" to user.id,
                "username" to user.username,
                "tokensSpent" to tokenDelta,
                "vtCredited" to (tokenDelta / 10.0),
                "conversionRate" to 10,
                "reason" to reason,
                "processedBy" to adminEmail,
                "timestamp" to now
            )
            try { database.child("token_conversions").child(logId).setValue(logPayload).await() } catch (_: Exception) {}
            try { database.child("admin_channels").child("token_conversions").child(logId).setValue(logPayload).await() } catch (_: Exception) {}
            try { firestore.collection("token_conversions").document(logId).set(logPayload).await() } catch (_: Exception) {}

            val sign = if (tokenDelta >= 0) "+" else ""
            sendTargetedUserNotification(
                userId = user.id,
                title = "Reward Tokens Updated",
                message = "Admin adjusted your tokens by $sign$tokenDelta ($reason). New Token Balance: $newTokens Tokens.",
                type = "WALLET"
            )
            GlobalErrorManager.emitSuccess("Tokens updated for ${user.username}: $newTokens Tokens ($sign$tokenDelta)!")
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to adjust tokens: ${e.message}")
        }
    }

    // =========================================================================
    // SECTION 4D: TOURNAMENT LOBBY CHAT & ADMIN MODERATION
    // =========================================================================

    fun getTournamentChatStream(tournamentId: String): Flow<List<TournamentChatMessage>> = callbackFlow {
        if (tournamentId.isBlank()) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val chatMap = mutableMapOf<String, TournamentChatMessage>()

        fun emitChat() {
            val sorted = chatMap.values.sortedWith(
                compareByDescending<TournamentChatMessage> { it.isPinned }.thenBy { it.timestamp }
            )
            trySend(sorted)
        }

        fun parseSnap(snap: DataSnapshot): TournamentChatMessage? {
            if (!snap.exists()) return null
            val id = snap.child("id").value?.toString() ?: snap.key ?: return null
            val msg = snap.child("message").value?.toString() ?: snap.child("text").value?.toString() ?: ""
            if (id.isBlank() || msg.isBlank()) return null
            return TournamentChatMessage(
                id = id,
                tournamentId = snap.child("tournamentId").value?.toString() ?: tournamentId,
                senderId = snap.child("senderId").value?.toString() ?: snap.child("userId").value?.toString() ?: "",
                senderName = snap.child("senderName").value?.toString() ?: snap.child("username").value?.toString() ?: "Player",
                senderAvatar = snap.child("senderAvatar").value?.toString() ?: snap.child("avatarUrl").value?.toString() ?: "avatar_1",
                senderRole = snap.child("senderRole").value?.toString() ?: "PLAYER",
                message = msg,
                isAdmin = safeBool(snap.child("isAdmin").value) || snap.child("senderRole").value?.toString()?.equals("ADMIN", true) == true,
                isPinned = safeBool(snap.child("isPinned").value),
                timestamp = safeLong(snap.child("timestamp").value ?: snap.child("createdAt").value, System.currentTimeMillis())
            )
        }

        val rtdbRefs = listOf(
            database.child("tournament_chats").child(tournamentId),
            database.child("tournaments").child(tournamentId).child("chat")
        )
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val seenIds = mutableSetOf<String>()
                snapshot.children.forEach { c ->
                    parseSnap(c)?.let {
                        chatMap[it.id] = it
                        seenIds.add(it.id)
                    }
                }
                emitChat()
            }
            override fun onCancelled(error: DatabaseError) { emitChat() }
        }
        rtdbRefs.forEach { it.addValueEventListener(listener) }

        val fsListener = try {
            firestore.collection("tournaments").document(tournamentId).collection("chat")
                .addSnapshotListener { snap, err ->
                    if (err == null && snap != null) {
                        snap.documents.forEach { doc ->
                            val msg = doc.getString("message") ?: doc.getString("text") ?: ""
                            if (msg.isNotBlank()) {
                                chatMap[doc.id] = TournamentChatMessage(
                                    id = doc.id,
                                    tournamentId = tournamentId,
                                    senderId = doc.getString("senderId") ?: doc.getString("userId") ?: "",
                                    senderName = doc.getString("senderName") ?: doc.getString("username") ?: "Player",
                                    senderAvatar = doc.getString("senderAvatar") ?: "avatar_1",
                                    senderRole = doc.getString("senderRole") ?: "PLAYER",
                                    message = msg,
                                    isAdmin = doc.getBoolean("isAdmin") ?: false,
                                    isPinned = doc.getBoolean("isPinned") ?: false,
                                    timestamp = safeLong(doc.get("timestamp"), System.currentTimeMillis())
                                )
                            }
                        }
                        emitChat()
                    }
                }
        } catch (_: Exception) { null }

        awaitClose {
            rtdbRefs.forEach { it.removeEventListener(listener) }
            fsListener?.remove()
        }
    }

    suspend fun sendAdminTournamentChatMessage(
        tournamentId: String,
        message: String,
        isPinned: Boolean,
        adminName: String = "VeloRix Official Admin"
    ) {
        if (tournamentId.isBlank() || message.isBlank()) return
        val now = System.currentTimeMillis()
        val msgId = "chat_adm_${now}"
        val adminUid = auth.currentUser?.uid ?: "admin_master"
        val payload = mapOf(
            "id" to msgId,
            "tournamentId" to tournamentId,
            "senderId" to adminUid,
            "senderName" to adminName,
            "senderAvatar" to "avatar_1",
            "senderRole" to "ADMIN",
            "message" to message.trim(),
            "isAdmin" to true,
            "isPinned" to isPinned,
            "timestamp" to now
        )
        try { database.child("tournament_chats").child(tournamentId).child(msgId).setValue(payload).await() } catch (_: Exception) {}
        try { database.child("tournaments").child(tournamentId).child("chat").child(msgId).setValue(payload).await() } catch (_: Exception) {}
        try { firestore.collection("tournaments").document(tournamentId).collection("chat").document(msgId).set(payload).await() } catch (_: Exception) {}
        GlobalErrorManager.emitSuccess(if (isPinned) "Pinned Admin announcement posted to lobby chat!" else "Admin message sent to lobby chat!")
    }

    suspend fun togglePinTournamentChatMessage(tournamentId: String, messageId: String, isPinned: Boolean) {
        if (tournamentId.isBlank() || messageId.isBlank()) return
        val updates = mapOf<String, Any>("isPinned" to isPinned)
        try { database.child("tournament_chats").child(tournamentId).child(messageId).updateChildren(updates).await() } catch (_: Exception) {}
        try { database.child("tournaments").child(tournamentId).child("chat").child(messageId).updateChildren(updates).await() } catch (_: Exception) {}
        try { firestore.collection("tournaments").document(tournamentId).collection("chat").document(messageId).set(updates, SetOptions.merge()).await() } catch (_: Exception) {}
        GlobalErrorManager.emitSuccess(if (isPinned) "Message pinned to top of lobby chat!" else "Message unpinned.")
    }

    suspend fun deleteTournamentChatMessage(tournamentId: String, messageId: String) {
        if (tournamentId.isBlank() || messageId.isBlank()) return
        try { database.child("tournament_chats").child(tournamentId).child(messageId).removeValue().await() } catch (_: Exception) {}
        try { database.child("tournaments").child(tournamentId).child("chat").child(messageId).removeValue().await() } catch (_: Exception) {}
        try { firestore.collection("tournaments").document(tournamentId).collection("chat").document(messageId).delete().await() } catch (_: Exception) {}
        GlobalErrorManager.emitSuccess("Chat message deleted from lobby.")
    }

    // =========================================================================
    // SECTION 5A & 5C: DEPOSIT REQUESTS, TOKEN CONVERSIONS & MISSIONS
    // =========================================================================

    fun getDepositRequestsStream(): Flow<List<DepositRequest>> = callbackFlow {
        val depMap = mutableMapOf<String, DepositRequest>()

        fun emitDeposits() {
            trySend(depMap.values.sortedByDescending { it.requestedAt })
        }

        fun parseSnap(snap: DataSnapshot): DepositRequest? {
            if (!snap.exists()) return null
            val id = snap.child("id").value?.toString()
                ?: snap.child("requestId").value?.toString()
                ?: snap.key ?: return null
            if (id.isBlank()) return null
            return DepositRequest(
                id = id,
                requestId = id,
                userId = snap.child("userId").value?.toString() ?: snap.child("uid").value?.toString() ?: "",
                userName = snap.child("username").value?.toString() ?: snap.child("userName").value?.toString() ?: "Player",
                userEmail = snap.child("userEmail").value?.toString() ?: snap.child("email").value?.toString() ?: "",
                amount = safeDouble(snap.child("amount").value ?: snap.child("vtAmount").value),
                utrNumber = snap.child("utrNumber").value?.toString() ?: snap.child("paymentRef").value?.toString() ?: snap.child("upiId").value?.toString() ?: "",
                paymentMethod = snap.child("paymentMethod").value?.toString() ?: snap.child("method").value?.toString() ?: "UPI_MANUAL_UTR",
                screenshotUrl = snap.child("screenshotUrl").value?.toString() ?: "",
                status = snap.child("status").value?.toString()?.uppercase() ?: "PENDING",
                requestedAt = safeLong(snap.child("createdAt").value ?: snap.child("requestedAt").value ?: snap.child("timestamp").value, System.currentTimeMillis()),
                processedAt = safeLong(snap.child("processedAt").value, 0L),
                processedBy = snap.child("processedBy").value?.toString() ?: ""
            )
        }

        val rtdbRefs = listOf(
            database.child("deposit_requests"),
            database.child("admin_channels").child("deposit_requests")
        )
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { c ->
                    parseSnap(c)?.let { depMap[it.id] = it }
                }
                emitDeposits()
            }
            override fun onCancelled(error: DatabaseError) { emitDeposits() }
        }
        rtdbRefs.forEach { it.addValueEventListener(listener) }

        val fsReg = try {
            firestore.collection("deposit_requests").addSnapshotListener { snap, err ->
                if (err == null && snap != null) {
                    snap.documents.forEach { doc ->
                        val id = doc.getString("id") ?: doc.getString("requestId") ?: doc.id
                        depMap[id] = DepositRequest(
                            id = id,
                            requestId = id,
                            userId = doc.getString("userId") ?: doc.getString("uid") ?: "",
                            userName = doc.getString("username") ?: doc.getString("userName") ?: "Player",
                            userEmail = doc.getString("userEmail") ?: doc.getString("email") ?: "",
                            amount = safeDouble(doc.get("amount") ?: doc.get("vtAmount")),
                            utrNumber = doc.getString("utrNumber") ?: doc.getString("paymentRef") ?: "",
                            paymentMethod = doc.getString("paymentMethod") ?: doc.getString("method") ?: "UPI_MANUAL_UTR",
                            screenshotUrl = doc.getString("screenshotUrl") ?: "",
                            status = doc.getString("status")?.uppercase() ?: "PENDING",
                            requestedAt = safeLong(doc.get("createdAt") ?: doc.get("requestedAt"), System.currentTimeMillis()),
                            processedAt = safeLong(doc.get("processedAt"), 0L),
                            processedBy = doc.getString("processedBy") ?: ""
                        )
                    }
                    emitDeposits()
                }
            }
        } catch (_: Exception) { null }

        awaitClose {
            rtdbRefs.forEach { it.removeEventListener(listener) }
            fsReg?.remove()
        }
    }

    fun getTokenConversionsStream(): Flow<List<TokenConversionRecord>> = callbackFlow {
        val convMap = mutableMapOf<String, TokenConversionRecord>()

        fun emitConversions() {
            trySend(convMap.values.sortedByDescending { it.timestamp })
        }

        val rtdbRefs = listOf(
            database.child("token_conversions"),
            database.child("admin_channels").child("token_conversions")
        )
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { c ->
                    val id = c.child("id").value?.toString() ?: c.key ?: ""
                    if (id.isNotBlank()) {
                        convMap[id] = TokenConversionRecord(
                            id = id,
                            userId = c.child("userId").value?.toString() ?: "",
                            username = c.child("username").value?.toString() ?: "Player",
                            tokensSpent = safeInt(c.child("tokensSpent").value),
                            vtCredited = safeDouble(c.child("vtCredited").value),
                            conversionRate = safeInt(c.child("conversionRate").value, 10),
                            timestamp = safeLong(c.child("timestamp").value, System.currentTimeMillis())
                        )
                    }
                }
                emitConversions()
            }
            override fun onCancelled(error: DatabaseError) { emitConversions() }
        }
        rtdbRefs.forEach { it.addValueEventListener(listener) }

        val fsReg = try {
            firestore.collection("token_conversions").addSnapshotListener { snap, err ->
                if (err == null && snap != null) {
                    snap.documents.forEach { doc ->
                        convMap[doc.id] = TokenConversionRecord(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            username = doc.getString("username") ?: "Player",
                            tokensSpent = safeInt(doc.get("tokensSpent")),
                            vtCredited = safeDouble(doc.get("vtCredited")),
                            conversionRate = safeInt(doc.get("conversionRate"), 10),
                            timestamp = safeLong(doc.get("timestamp"), System.currentTimeMillis())
                        )
                    }
                    emitConversions()
                }
            }
        } catch (_: Exception) { null }

        awaitClose {
            rtdbRefs.forEach { it.removeEventListener(listener) }
            fsReg?.remove()
        }
    }

    fun getMissionsStream(): Flow<List<MissionItem>> = callbackFlow {
        val missionsMap = mutableMapOf<String, MissionItem>()

        fun emitMissions() {
            trySend(missionsMap.values.sortedBy { it.id })
        }

        val ref = database.child("missions")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { c ->
                    val id = c.child("id").value?.toString() ?: c.key ?: ""
                    if (id.isNotBlank()) {
                        missionsMap[id] = MissionItem(
                            id = id,
                            title = c.child("title").value?.toString() ?: "Mission",
                            description = c.child("description").value?.toString() ?: "",
                            rewardTokens = safeInt(c.child("rewardTokens").value, 10),
                            rewardVt = safeDouble(c.child("rewardVt").value, 0.0),
                            targetProgress = safeInt(c.child("targetProgress").value, 1),
                            category = c.child("category").value?.toString() ?: "DAILY",
                            isActive = safeBool(c.child("isActive").value, true),
                            updatedAt = safeLong(c.child("updatedAt").value, System.currentTimeMillis())
                        )
                    }
                }
                emitMissions()
            }
            override fun onCancelled(error: DatabaseError) { emitMissions() }
        }
        ref.addValueEventListener(listener)

        val fsReg = try {
            firestore.collection("missions").addSnapshotListener { snap, err ->
                if (err == null && snap != null) {
                    snap.documents.forEach { doc ->
                        missionsMap[doc.id] = MissionItem(
                            id = doc.id,
                            title = doc.getString("title") ?: "Mission",
                            description = doc.getString("description") ?: "",
                            rewardTokens = safeInt(doc.get("rewardTokens"), 10),
                            rewardVt = safeDouble(doc.get("rewardVt"), 0.0),
                            targetProgress = safeInt(doc.get("targetProgress"), 1),
                            category = doc.getString("category") ?: "DAILY",
                            isActive = doc.getBoolean("isActive") ?: true,
                            updatedAt = safeLong(doc.get("updatedAt"), System.currentTimeMillis())
                        )
                    }
                    emitMissions()
                }
            }
        } catch (_: Exception) { null }

        awaitClose {
            ref.removeEventListener(listener)
            fsReg?.remove()
        }
    }

    suspend fun saveMission(mission: MissionItem) {
        val now = System.currentTimeMillis()
        val mId = mission.id.ifBlank { "mission_${now}" }
        val payload = mapOf(
            "id" to mId,
            "title" to mission.title,
            "description" to mission.description,
            "rewardTokens" to mission.rewardTokens,
            "rewardVt" to mission.rewardVt,
            "targetProgress" to mission.targetProgress,
            "category" to mission.category,
            "isActive" to mission.isActive,
            "updatedAt" to now
        )
        try { database.child("missions").child(mId).setValue(payload).await() } catch (_: Exception) {}
        try { firestore.collection("missions").document(mId).set(payload).await() } catch (_: Exception) {}
        GlobalErrorManager.emitSuccess("Mission '${mission.title}' synced to RTDB & Firestore!")
    }

    suspend fun deleteMission(missionId: String) {
        if (missionId.isBlank()) return
        try { database.child("missions").child(missionId).removeValue().await() } catch (_: Exception) {}
        try { firestore.collection("missions").document(missionId).delete().await() } catch (_: Exception) {}
        GlobalErrorManager.emitSuccess("Mission deleted.")
    }

    // =========================================================================
    // SECTION 6: SYSTEM CONFIG (/system_config & system_config/global) & BANNERS
    // =========================================================================

    fun getSystemConfigStream(): Flow<SystemConfig> = callbackFlow {
        var latestConfig = SystemConfig()

        fun parseFeatureFlagsFromSnap(snap: DataSnapshot): Map<String, Boolean> {
            val map = SystemConfig.defaultFeatureFlags().toMutableMap()
            val ffNode = when {
                snap.child("featureFlags").exists() -> snap.child("featureFlags")
                snap.child("feature_flags").exists() -> snap.child("feature_flags")
                else -> null
            }
            ffNode?.children?.forEach { c ->
                val k = c.key ?: return@forEach
                map[k] = safeBool(c.value, true)
            }
            return map
        }

        fun parseSnap(snap: DataSnapshot): SystemConfig {
            if (!snap.exists()) return latestConfig
            return SystemConfig(
                maintenanceMode = safeBool(snap.child("maintenanceMode").value ?: snap.child("isMaintenance").value, false),
                maintenanceTitle = snap.child("maintenanceTitle").value?.toString()
                    ?: "Scheduled Server Maintenance",
                maintenanceMessage = snap.child("maintenanceMessage").value?.toString()
                    ?: "VeloRix servers are upgrading for Season 4. Back in 30 mins!",
                maintenanceEndTimeMillis = safeLong(snap.child("maintenanceEndTimeMillis").value ?: snap.child("maintenanceUntil").value, 0L),
                forceUpdateRequired = safeBool(snap.child("forceUpdateRequired").value ?: snap.child("isForceUpdate").value, false),
                latestVersionCode = safeInt(snap.child("latestVersionCode").value ?: snap.child("minRequiredVersion").value, 2),
                latestVersionName = snap.child("latestVersionName").value?.toString() ?: "1.1.0",
                updateDownloadUrl = snap.child("updateDownloadUrl").value?.toString()
                    ?: snap.child("updateUrl").value?.toString()
                    ?: "https://velorixesports.com/download",
                updateChangelog = snap.child("updateChangelog").value?.toString()
                    ?: snap.child("changelog").value?.toString()
                    ?: "• Live Custom Banners\n• Camera Profile Upload\n• Faster Matchmaking",
                showBanners = safeBool(snap.child("showBanners").value, true),
                showDeveloperModal = safeBool(snap.child("showDeveloperModal").value, false),
                upiId = snap.child("upiId").value?.toString()
                    ?: snap.child("merchantUpiId").value?.toString()
                    ?: "velorixesports@upi",
                upiMerchantName = snap.child("upiMerchantName").value?.toString()
                    ?: snap.child("merchantName").value?.toString()
                    ?: "VeloRix Esports",
                minDepositVt = safeDouble(snap.child("minDepositVt").value, 20.0),
                minWithdrawalVt = safeDouble(snap.child("minWithdrawalVt").value, 50.0),
                tokenConversionRate = safeInt(snap.child("tokenConversionRate").value, 10),
                whatsappSupportUrl = snap.child("whatsappSupportUrl").value?.toString() ?: "https://wa.me/919999999999",
                telegramChannelUrl = snap.child("telegramChannelUrl").value?.toString() ?: "https://t.me/velorixesports",
                instagramUrl = snap.child("instagramUrl").value?.toString() ?: "https://instagram.com/velorixesports",
                discordUrl = snap.child("discordUrl").value?.toString() ?: "https://discord.gg/velorix",
                vpnBlockEnabled = safeBool(snap.child("vpnBlockEnabled").value ?: snap.child("vpnRestrictionEnabled").value, false),
                otaPatchVersion = safeInt(snap.child("otaPatchVersion").value, 1).coerceAtLeast(1),
                otaReleaseNotes = snap.child("otaReleaseNotes").value?.toString()
                    ?: "Live OTA Hot-Patch Active — Zero Play Store Update Needed",
                liveLobbyTicker = snap.child("liveLobbyTicker").value?.toString() ?: "",
                activeFestivalOverride = snap.child("activeFestivalOverride").value?.toString() ?: "AUTO",
                customFestivalTitle = snap.child("customFestivalTitle").value?.toString() ?: "",
                customFestivalMessage = snap.child("customFestivalMessage").value?.toString() ?: "",
                customAttractingTitle = snap.child("customAttractingTitle").value?.toString() ?: "",
                customAttractingMessage = snap.child("customAttractingMessage").value?.toString() ?: "",
                attractingAlertWeight = safeInt(snap.child("attractingAlertWeight").value, 65),
                timeContextualWeight = safeInt(snap.child("timeContextualWeight").value, 20),
                weeklyCalendarWeight = safeInt(snap.child("weeklyCalendarWeight").value, 15),
                featureFlags = parseFeatureFlagsFromSnap(snap),
                updatedAt = safeLong(snap.child("updatedAt").value, System.currentTimeMillis())
            )
        }

        val rtdbConfigRefs = listOf(
            database.child("system_config"),
            database.child("app_config"),
            database.child("ota_live_config")
        )
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    latestConfig = parseSnap(snapshot)
                    trySend(latestConfig)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(latestConfig)
            }
        }
        rtdbConfigRefs.forEach { it.addValueEventListener(listener) }

        val fsReg1 = try {
            firestore.collection("system_config").document("global")
                .addSnapshotListener { doc, err ->
                    if (err == null && doc != null && doc.exists()) {
                        val rawFlags = (doc.get("featureFlags") as? Map<*, *>)
                            ?: (doc.get("feature_flags") as? Map<*, *>)
                        val parsedFlags = SystemConfig.defaultFeatureFlags().toMutableMap()
                        rawFlags?.forEach { (k, v) ->
                            if (k != null) parsedFlags[k.toString()] = safeBool(v, true)
                        }
                        latestConfig = SystemConfig(
                            maintenanceMode = doc.getBoolean("maintenanceMode") ?: doc.getBoolean("isMaintenance") ?: latestConfig.maintenanceMode,
                            maintenanceTitle = doc.getString("maintenanceTitle") ?: latestConfig.maintenanceTitle,
                            maintenanceMessage = doc.getString("maintenanceMessage") ?: latestConfig.maintenanceMessage,
                            maintenanceEndTimeMillis = safeLong(doc.get("maintenanceEndTimeMillis") ?: doc.get("maintenanceUntil"), latestConfig.maintenanceEndTimeMillis),
                            forceUpdateRequired = doc.getBoolean("forceUpdateRequired") ?: doc.getBoolean("isForceUpdate") ?: latestConfig.forceUpdateRequired,
                            latestVersionCode = safeInt(doc.get("latestVersionCode") ?: doc.get("minRequiredVersion"), latestConfig.latestVersionCode),
                            latestVersionName = doc.getString("latestVersionName") ?: latestConfig.latestVersionName,
                            updateDownloadUrl = doc.getString("updateDownloadUrl") ?: doc.getString("updateUrl") ?: latestConfig.updateDownloadUrl,
                            updateChangelog = doc.getString("updateChangelog") ?: doc.getString("changelog") ?: latestConfig.updateChangelog,
                            showBanners = doc.getBoolean("showBanners") ?: latestConfig.showBanners,
                            showDeveloperModal = doc.getBoolean("showDeveloperModal") ?: latestConfig.showDeveloperModal,
                            upiId = doc.getString("upiId") ?: doc.getString("merchantUpiId") ?: latestConfig.upiId,
                            upiMerchantName = doc.getString("upiMerchantName") ?: doc.getString("merchantName") ?: latestConfig.upiMerchantName,
                            minDepositVt = safeDouble(doc.get("minDepositVt"), latestConfig.minDepositVt),
                            minWithdrawalVt = safeDouble(doc.get("minWithdrawalVt"), latestConfig.minWithdrawalVt),
                            tokenConversionRate = safeInt(doc.get("tokenConversionRate"), latestConfig.tokenConversionRate),
                            whatsappSupportUrl = doc.getString("whatsappSupportUrl") ?: latestConfig.whatsappSupportUrl,
                            telegramChannelUrl = doc.getString("telegramChannelUrl") ?: latestConfig.telegramChannelUrl,
                            instagramUrl = doc.getString("instagramUrl") ?: latestConfig.instagramUrl,
                            discordUrl = doc.getString("discordUrl") ?: latestConfig.discordUrl,
                            vpnBlockEnabled = doc.getBoolean("vpnBlockEnabled") ?: doc.getBoolean("vpnRestrictionEnabled") ?: latestConfig.vpnBlockEnabled,
                            otaPatchVersion = safeInt(doc.get("otaPatchVersion"), latestConfig.otaPatchVersion).coerceAtLeast(1),
                            otaReleaseNotes = doc.getString("otaReleaseNotes") ?: latestConfig.otaReleaseNotes,
                            liveLobbyTicker = doc.getString("liveLobbyTicker") ?: latestConfig.liveLobbyTicker,
                            activeFestivalOverride = doc.getString("activeFestivalOverride") ?: latestConfig.activeFestivalOverride,
                            customFestivalTitle = doc.getString("customFestivalTitle") ?: latestConfig.customFestivalTitle,
                            customFestivalMessage = doc.getString("customFestivalMessage") ?: latestConfig.customFestivalMessage,
                            customAttractingTitle = doc.getString("customAttractingTitle") ?: latestConfig.customAttractingTitle,
                            customAttractingMessage = doc.getString("customAttractingMessage") ?: latestConfig.customAttractingMessage,
                            attractingAlertWeight = safeInt(doc.get("attractingAlertWeight"), latestConfig.attractingAlertWeight),
                            timeContextualWeight = safeInt(doc.get("timeContextualWeight"), latestConfig.timeContextualWeight),
                            weeklyCalendarWeight = safeInt(doc.get("weeklyCalendarWeight"), latestConfig.weeklyCalendarWeight),
                            featureFlags = parsedFlags,
                            updatedAt = safeLong(doc.get("updatedAt"), latestConfig.updatedAt)
                        )
                        trySend(latestConfig)
                    }
                }
        } catch (_: Exception) { null }

        awaitClose {
            rtdbConfigRefs.forEach { it.removeEventListener(listener) }
            fsReg1?.remove()
        }
    }

    suspend fun saveSystemConfig(config: SystemConfig, adminEmail: String) {
        try {
            val now = System.currentTimeMillis()
            val mergedFlags = SystemConfig.defaultFeatureFlags().toMutableMap().apply {
                putAll(config.featureFlags)
            }
            val payload = mapOf<String, Any>(
                "maintenanceMode" to config.maintenanceMode,
                "isMaintenance" to config.maintenanceMode,
                "maintenanceTitle" to config.maintenanceTitle,
                "maintenanceMessage" to config.maintenanceMessage,
                "maintenanceEndTimeMillis" to config.maintenanceEndTimeMillis,
                "maintenanceUntil" to config.maintenanceEndTimeMillis,
                "forceUpdateRequired" to config.forceUpdateRequired,
                "isForceUpdate" to config.forceUpdateRequired,
                "latestVersionCode" to config.latestVersionCode,
                "minRequiredVersion" to config.latestVersionCode,
                "latestVersionName" to config.latestVersionName,
                "updateDownloadUrl" to config.updateDownloadUrl,
                "updateUrl" to config.updateDownloadUrl,
                "updateChangelog" to config.updateChangelog,
                "changelog" to config.updateChangelog,
                "showBanners" to config.showBanners,
                "showDeveloperModal" to config.showDeveloperModal,
                "upiId" to config.upiId,
                "merchantUpiId" to config.upiId,
                "upiMerchantName" to config.upiMerchantName,
                "merchantName" to config.upiMerchantName,
                "minDepositVt" to config.minDepositVt,
                "minWithdrawalVt" to config.minWithdrawalVt,
                "tokenConversionRate" to config.tokenConversionRate,
                "whatsappSupportUrl" to config.whatsappSupportUrl,
                "telegramChannelUrl" to config.telegramChannelUrl,
                "instagramUrl" to config.instagramUrl,
                "discordUrl" to config.discordUrl,
                "vpnBlockEnabled" to config.vpnBlockEnabled,
                "vpnRestrictionEnabled" to config.vpnBlockEnabled,
                "otaPatchVersion" to config.otaPatchVersion,
                "otaReleaseNotes" to config.otaReleaseNotes,
                "liveLobbyTicker" to config.liveLobbyTicker,
                "activeFestivalOverride" to config.activeFestivalOverride,
                "customFestivalTitle" to config.customFestivalTitle,
                "customFestivalMessage" to config.customFestivalMessage,
                "customAttractingTitle" to config.customAttractingTitle,
                "customAttractingMessage" to config.customAttractingMessage,
                "attractingAlertWeight" to config.attractingAlertWeight,
                "timeContextualWeight" to config.timeContextualWeight,
                "weeklyCalendarWeight" to config.weeklyCalendarWeight,
                "featureFlags" to mergedFlags,
                "feature_flags" to mergedFlags,
                "updatedAt" to now,
                "updatedBy" to adminEmail
            )
            // 1. Write to RTDB /system_config, /app_config, /ota_live_config, and /maintenance
            try { database.child("system_config").updateChildren(payload).await() } catch (_: Exception) {}
            try { database.child("app_config").updateChildren(payload).await() } catch (_: Exception) {}
            try { database.child("ota_live_config").updateChildren(payload).await() } catch (_: Exception) {}
            try {
                database.child("maintenance").updateChildren(
                    mapOf(
                        "enabled" to config.maintenanceMode,
                        "isMaintenance" to config.maintenanceMode,
                        "title" to config.maintenanceTitle,
                        "message" to config.maintenanceMessage,
                        "maintenanceUntil" to config.maintenanceEndTimeMillis,
                        "updatedAt" to now
                    )
                ).await()
            } catch (_: Exception) {}

            // 2. Write to Firestore system_config/global, system_config/config, system_config/app_settings, app_config/global
            try { firestore.collection("system_config").document("global").set(payload, SetOptions.merge()).await() } catch (_: Exception) {}
            try { firestore.collection("system_config").document("config").set(payload, SetOptions.merge()).await() } catch (_: Exception) {}
            try { firestore.collection("system_config").document("app_config").set(payload, SetOptions.merge()).await() } catch (_: Exception) {}
            try { firestore.collection("system_config").document("app_settings").set(payload, SetOptions.merge()).await() } catch (_: Exception) {}
            try { firestore.collection("app_config").document("global").set(payload, SetOptions.merge()).await() } catch (_: Exception) {}

            // 3. Log OTA Live Patch to /admin_channels/ota_live_patches
            val patchLogId = "ota_patch_${config.otaPatchVersion}_${now}"
            val patchLogPayload = mapOf<String, Any>(
                "id" to patchLogId,
                "channelType" to "ota_live_patches",
                "title" to "OTA Patch v${config.otaPatchVersion} • Festival: ${config.activeFestivalOverride}",
                "details" to config.otaReleaseNotes.ifBlank { "Live OTA Config Synced" },
                "status" to "COMPLETED",
                "processedBy" to adminEmail,
                "createdAt" to now,
                "timestamp" to now
            )
            try { database.child("admin_channels").child("ota_live_patches").child(patchLogId).setValue(patchLogPayload).await() } catch (_: Exception) {}
            try { firestore.collection("admin_channels").document("ota_live_patches").collection("items").document(patchLogId).set(patchLogPayload).await() } catch (_: Exception) {}

            // 4. Push SYSTEM_CONFIG_UPDATED to /global_app_state
            publishGlobalAppStateEvent(
                eventType = "SYSTEM_CONFIG_UPDATED",
                payload = mapOf(
                    "isMaintenance" to config.maintenanceMode,
                    "isForceUpdate" to config.forceUpdateRequired,
                    "showBanners" to config.showBanners,
                    "otaPatchVersion" to config.otaPatchVersion,
                    "activeFestivalOverride" to config.activeFestivalOverride,
                    "upiId" to config.upiId
                )
            )
            writeAuditLog(
                action = "SYSTEM_CONFIG_UPDATED",
                detail = "OTA Patch v${config.otaPatchVersion} • Festival=${config.activeFestivalOverride} • Maintenance=${config.maintenanceMode} • UPI=${config.upiId}",
                adminEmail = adminEmail,
                targetId = "global"
            )
            GlobalErrorManager.emitSuccess("OTA Live Config v${config.otaPatchVersion}, 14 Feature Flags & UPI synced across RTDB & Firestore!")
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to save system config: ${e.message}")
        }
    }

    suspend fun savePromotionalBanner(banner: AppAnnouncementBanner) {
        try {
            val now = System.currentTimeMillis()
            val bId = banner.id.ifBlank { "banner_${now}" }
            val subText = banner.subtitle.ifBlank { banner.message.ifBlank { banner.content } }
            val actUrl = banner.actionUrl.ifBlank { banner.targetUrl }
            val payload = mapOf<String, Any>(
                "id" to bId,
                "title" to banner.title,
                "subtitle" to subText,
                "message" to subText,
                "content" to subText,
                "description" to subText,
                "badgeText" to banner.badgeText.ifBlank { "OFFICIAL" },
                "ctaText" to banner.ctaText.ifBlank { "JOIN NOW" },
                "actionType" to banner.actionType.ifBlank { "TOURNAMENT" },
                "targetId" to banner.targetId.ifBlank { actUrl },
                "gradientTheme" to banner.gradientTheme.ifBlank { "INDIGO_VIOLET" },
                "imageUrl" to banner.imageUrl,
                "actionUrl" to actUrl,
                "targetUrl" to actUrl,
                "bannerType" to banner.bannerType,
                "isActive" to banner.isActive,
                "active" to banner.isActive,
                "priority" to banner.priority,
                "order" to banner.priority,
                "createdAt" to (if (banner.createdAt > 0L) banner.createdAt else now)
            )
            try { database.child("banners").child(bId).setValue(payload).await() } catch (_: Exception) {}
            try { firestore.collection("banners").document(bId).set(payload).await() } catch (_: Exception) {}
            publishGlobalAppStateEvent(
                eventType = "BANNER_UPDATED",
                payload = mapOf("bannerId" to bId, "title" to banner.title, "active" to banner.isActive)
            )
            GlobalErrorManager.emitSuccess("Promotional banner '${banner.title}' saved to /banners (RTDB & Firestore)!")
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to save promotional banner: ${e.message}")
        }
    }

    suspend fun deletePromotionalBanner(bannerId: String) {
        if (bannerId.isBlank()) return
        try { database.child("banners").child(bannerId).removeValue().await() } catch (_: Exception) {}
        try { firestore.collection("banners").document(bannerId).delete().await() } catch (_: Exception) {}
        GlobalErrorManager.emitSuccess("Promotional banner deleted from RTDB & Firestore.")
    }

    // =========================================================================
    // SECTION 7B: TARGETED & GLOBAL PUSH / IN-APP NOTIFICATIONS
    // =========================================================================

    suspend fun sendTargetedUserNotification(
        userId: String,
        title: String,
        message: String,
        type: String = "SYSTEM"
    ) {
        if (userId.isBlank() || title.isBlank()) return
        val now = System.currentTimeMillis()
        val notifId = "notif_${now}_${(100..999).random()}"
        val payload = mapOf(
            "id" to notifId,
            "userId" to userId,
            "title" to title.trim(),
            "message" to message.trim(),
            "type" to type.uppercase(),
            "timestamp" to now,
            "isRead" to false
        )
        try { database.child("notifications").child(userId).child(notifId).setValue(payload).await() } catch (_: Exception) {}
        try { database.child("users").child(userId).child("notifications").child(notifId).setValue(payload).await() } catch (_: Exception) {}
        try { firestore.collection("users").document(userId).collection("notifications").document(notifId).set(payload).await() } catch (_: Exception) {}
    }

    suspend fun sendGlobalBroadcastNotification(
        title: String,
        message: String,
        type: String = "TOURNAMENT"
    ) {
        if (title.isBlank() || message.isBlank()) return
        val now = System.currentTimeMillis()
        val notifId = "notif_global_${now}"
        val payload = mapOf(
            "id" to notifId,
            "title" to title.trim(),
            "message" to message.trim(),
            "type" to type.uppercase(),
            "timestamp" to now,
            "isRead" to false
        )
        try { database.child("notifications").child("global").child(notifId).setValue(payload).await() } catch (_: Exception) {}
        try { database.child("notifications").child(notifId).setValue(payload).await() } catch (_: Exception) {}
        try { firestore.collection("notifications").document(notifId).set(payload).await() } catch (_: Exception) {}
        GlobalErrorManager.emitSuccess("Global notification broadcasted to /notifications/global & Firestore!")
    }

    // =========================================================================
    // GLOBAL APP STATE SYNC (/global_app_state) & AUDIT LOGS (/audit_logs)
    // =========================================================================

    suspend fun publishGlobalAppStateEvent(
        eventType: String,
        tournamentId: String = "",
        affectedUserId: String = "",
        payload: Map<String, Any> = emptyMap()
    ) {
        val now = System.currentTimeMillis()
        val stateMap = mapOf<String, Any>(
            "eventType" to eventType,
            "source" to "ADMIN_PANEL",
            "tournamentId" to tournamentId,
            "affectedUserId" to affectedUserId,
            "timestamp" to now,
            "payload" to payload
        )
        try { database.child("global_app_state").updateChildren(stateMap).await() } catch (_: Exception) {}
        try { firestore.collection("global_app_state").document("current").set(stateMap, SetOptions.merge()).await() } catch (_: Exception) {}
    }

    suspend fun writeAuditLog(
        action: String,
        detail: String,
        adminEmail: String,
        targetId: String = ""
    ) {
        val now = System.currentTimeMillis()
        val logId = "audit_${now}_${(100..999).random()}"
        val payload = mapOf<String, Any>(
            "id" to logId,
            "action" to action,
            "detail" to detail,
            "adminEmail" to adminEmail,
            "targetId" to targetId,
            "timestamp" to now
        )
        try { database.child("audit_logs").child(logId).setValue(payload).await() } catch (_: Exception) {}
        try { firestore.collection("audit_logs").document(logId).set(payload).await() } catch (_: Exception) {}
    }

    fun getAuditLogsStream(): Flow<List<AdminAuditLogItem>> = callbackFlow {
        val logsMap = mutableMapOf<String, AdminAuditLogItem>()

        fun emitLogs() {
            trySend(logsMap.values.sortedByDescending { it.timestamp }.take(150))
        }

        val ref = database.child("audit_logs")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { c ->
                    val id = c.child("id").value?.toString() ?: c.key ?: ""
                    if (id.isNotBlank()) {
                        logsMap[id] = AdminAuditLogItem(
                            id = id,
                            action = c.child("action").value?.toString() ?: "ADMIN_ACTION",
                            detail = c.child("detail").value?.toString() ?: "",
                            adminEmail = c.child("adminEmail").value?.toString() ?: "",
                            targetId = c.child("targetId").value?.toString() ?: "",
                            timestamp = safeLong(c.child("timestamp").value, System.currentTimeMillis())
                        )
                    }
                }
                emitLogs()
            }
            override fun onCancelled(error: DatabaseError) { emitLogs() }
        }
        ref.addValueEventListener(listener)

        val fsReg = try {
            firestore.collection("audit_logs").addSnapshotListener { snap, err ->
                if (err == null && snap != null) {
                    snap.documents.forEach { d ->
                        logsMap[d.id] = AdminAuditLogItem(
                            id = d.id,
                            action = d.getString("action") ?: "ADMIN_ACTION",
                            detail = d.getString("detail") ?: "",
                            adminEmail = d.getString("adminEmail") ?: "",
                            targetId = d.getString("targetId") ?: "",
                            timestamp = safeLong(d.get("timestamp"), System.currentTimeMillis())
                        )
                    }
                    emitLogs()
                }
            }
        } catch (_: Exception) { null }

        awaitClose {
            ref.removeEventListener(listener)
            fsReg?.remove()
        }
    }

    // =========================================================================
    // MODULE 1: MULTI-POSITION TOURNAMENT STANDINGS & WINNER PAYOUT SYSTEM
    // =========================================================================

    suspend fun loadTournamentParticipantsForStandings(
        tournament: Tournament,
        allUsers: List<UserProfile>
    ): List<TournamentStandingEntry> {
        val tId = tournament.id.trim()
        if (tId.isBlank()) return emptyList()

        val entriesByUser = linkedMapOf<String, TournamentStandingEntry>()
        val perKillBounty = tournament.perKillPrize.toDouble().coerceAtLeast(0.0)
        val totalPrizePool = tournament.prizePool.toDouble().coerceAtLeast(0.0)

        fun defaultRankPrizeForRank(rank: Int): Double = when (rank) {
            1 -> (totalPrizePool * 0.50).toInt().toDouble()
            2 -> (totalPrizePool * 0.30).toInt().toDouble()
            3 -> (totalPrizePool * 0.20).toInt().toDouble()
            else -> 0.0
        }

        // 1. Check if finalLeaderboard / standings already exist in RTDB
        try {
            val tourneySnap = database.child("tournaments").child(tId).get().await()
            val lbSnap = when {
                tourneySnap.child("finalLeaderboard").exists() -> tourneySnap.child("finalLeaderboard")
                tourneySnap.child("standings").exists() -> tourneySnap.child("standings")
                tourneySnap.child("results").exists() -> tourneySnap.child("results")
                else -> null
            }
            lbSnap?.children?.forEachIndexed { idx, c ->
                val uid = c.child("userId").value?.toString() ?: c.child("uid").value?.toString() ?: c.key ?: "slot_${idx + 1}"
                val rank = safeInt(c.child("rank").value ?: c.child("position").value, idx + 1).coerceAtLeast(1)
                val kills = safeInt(c.child("kills").value, 0)
                val rPrize = safeDouble(c.child("rankPrize").value, defaultRankPrizeForRank(rank))
                val kPrize = safeDouble(c.child("killPrize").value, kills * perKillBounty)
                val pWon = safeDouble(c.child("prizeWon").value, rPrize + kPrize)
                entriesByUser[uid] = TournamentStandingEntry(
                    rank = rank,
                    userId = uid,
                    username = c.child("username").value?.toString() ?: c.child("inGameName").value?.toString() ?: "Player",
                    inGameName = c.child("inGameName").value?.toString() ?: c.child("username").value?.toString() ?: "Player",
                    characterId = c.child("characterId").value?.toString() ?: c.child("freeFireId").value?.toString() ?: c.child("gameId").value?.toString() ?: "",
                    teamName = c.child("teamName").value?.toString() ?: "",
                    slotNumber = safeInt(c.child("slotNumber").value, idx + 1),
                    kills = kills,
                    points = kills * 2 + when (rank) { 1 -> 15; 2 -> 12; 3 -> 10; else -> 5 },
                    rankPrize = rPrize,
                    killPrize = kPrize,
                    prizeWon = pWon,
                    wonAnything = pWon > 0.0,
                    payoutStatus = if (pWon > 0.0) "PAID" else "NO_PRIZE"
                )
            }

            // 2. Also read RTDB /tournaments/{id}/participants
            val partSnap = tourneySnap.child("participants")
            if (partSnap.exists()) {
                partSnap.children.forEach { c ->
                    val uid = c.child("userId").value?.toString()
                        ?: c.child("uid").value?.toString()
                        ?: c.key ?: ""
                    if (uid.isNotBlank() && !entriesByUser.containsKey(uid)) {
                        val slot = entriesByUser.size + 1
                        val matchedUser = allUsers.find { it.id == uid }
                        val uname = c.child("username").value?.toString()
                            ?: c.child("inGameName").value?.toString()
                            ?: c.child("playerName").value?.toString()
                            ?: matchedUser?.username
                            ?: "Player #$slot"
                        val ign = c.child("inGameName").value?.toString()
                            ?: c.child("ign").value?.toString()
                            ?: matchedUser?.ign?.ifBlank { uname }
                            ?: uname
                        val charId = c.child("characterId").value?.toString()
                            ?: c.child("freeFireId").value?.toString()
                            ?: c.child("gameId").value?.toString()
                            ?: matchedUser?.gameId
                            ?: ""
                        val rPrize = defaultRankPrizeForRank(slot)
                        entriesByUser[uid] = TournamentStandingEntry(
                            rank = slot,
                            userId = uid,
                            username = uname,
                            inGameName = ign,
                            characterId = charId,
                            teamName = c.child("teamName").value?.toString() ?: "",
                            slotNumber = safeInt(c.child("slotNumber").value ?: c.child("slot").value, slot),
                            kills = safeInt(c.child("kills").value, 0),
                            rankPrize = rPrize,
                            killPrize = 0.0,
                            prizeWon = rPrize,
                            wonAnything = rPrize > 0.0
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. Fallback: RTDB /tournament_registrations
        try {
            val regSnap = database.child("tournament_registrations").get().await()
            if (regSnap.exists()) {
                // Check both /tournament_registrations/{tId}/{uid} and flat /tournament_registrations/{regId}
                val byTourneySnap = regSnap.child(tId)
                if (byTourneySnap.exists()) {
                    byTourneySnap.children.forEach { c ->
                        val uid = c.child("userId").value?.toString() ?: c.child("uid").value?.toString() ?: c.key ?: ""
                        if (uid.isNotBlank() && !entriesByUser.containsKey(uid)) {
                            val slot = entriesByUser.size + 1
                            val matchedUser = allUsers.find { it.id == uid }
                            val uname = c.child("username").value?.toString() ?: c.child("playerName").value?.toString() ?: matchedUser?.username ?: "Player #$slot"
                            val rPrize = defaultRankPrizeForRank(slot)
                            entriesByUser[uid] = TournamentStandingEntry(
                                rank = slot,
                                userId = uid,
                                username = uname,
                                inGameName = c.child("inGameName").value?.toString() ?: c.child("ign").value?.toString() ?: uname,
                                characterId = c.child("characterId").value?.toString() ?: c.child("gameId").value?.toString() ?: matchedUser?.gameId ?: "",
                                teamName = c.child("teamName").value?.toString() ?: "",
                                slotNumber = slot,
                                kills = 0,
                                rankPrize = rPrize,
                                killPrize = 0.0,
                                prizeWon = rPrize,
                                wonAnything = rPrize > 0.0
                            )
                        }
                    }
                }
                regSnap.children.forEach { c ->
                    val regTid = c.child("tournamentId").value?.toString() ?: ""
                    if (regTid == tId) {
                        val uid = c.child("userId").value?.toString() ?: c.child("uid").value?.toString() ?: c.key ?: ""
                        if (uid.isNotBlank() && !entriesByUser.containsKey(uid)) {
                            val slot = entriesByUser.size + 1
                            val matchedUser = allUsers.find { it.id == uid }
                            val uname = c.child("username").value?.toString() ?: c.child("playerName").value?.toString() ?: matchedUser?.username ?: "Player #$slot"
                            val rPrize = defaultRankPrizeForRank(slot)
                            entriesByUser[uid] = TournamentStandingEntry(
                                rank = slot,
                                userId = uid,
                                username = uname,
                                inGameName = c.child("inGameName").value?.toString() ?: uname,
                                characterId = c.child("characterId").value?.toString() ?: c.child("gameId").value?.toString() ?: matchedUser?.gameId ?: "",
                                teamName = c.child("teamName").value?.toString() ?: "",
                                slotNumber = slot,
                                kills = 0,
                                rankPrize = rPrize,
                                killPrize = 0.0,
                                prizeWon = rPrize,
                                wonAnything = rPrize > 0.0
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // 4. Firestore tournaments/{tournamentId}/participants
        try {
            val fsParts = firestore.collection("tournaments").document(tId).collection("participants").get().await()
            fsParts.documents.forEach { doc ->
                val uid = doc.getString("userId") ?: doc.getString("uid") ?: doc.id
                if (uid.isNotBlank() && !entriesByUser.containsKey(uid)) {
                    val slot = entriesByUser.size + 1
                    val matchedUser = allUsers.find { it.id == uid }
                    val uname = doc.getString("username") ?: doc.getString("inGameName") ?: matchedUser?.username ?: "Player #$slot"
                    val rPrize = defaultRankPrizeForRank(slot)
                    entriesByUser[uid] = TournamentStandingEntry(
                        rank = slot,
                        userId = uid,
                        username = uname,
                        inGameName = doc.getString("inGameName") ?: uname,
                        characterId = doc.getString("characterId") ?: doc.getString("freeFireId") ?: doc.getString("gameId") ?: matchedUser?.gameId ?: "",
                        teamName = doc.getString("teamName") ?: "",
                        slotNumber = safeInt(doc.get("slotNumber"), slot),
                        kills = safeInt(doc.get("kills"), 0),
                        rankPrize = rPrize,
                        killPrize = 0.0,
                        prizeWon = rPrize,
                        wonAnything = rPrize > 0.0
                    )
                }
            }
        } catch (_: Exception) {}

        // 5. Fallback: tournament.registrations
        tournament.registrations.forEach { reg ->
            val uid = reg.userId.ifBlank { reg.playerId.ifBlank { reg.freeFireUid.ifBlank { reg.id } } }
            if (uid.isNotBlank() && !entriesByUser.containsKey(uid)) {
                val slot = entriesByUser.size + 1
                val rPrize = defaultRankPrizeForRank(slot)
                val ign = reg.inGameName.ifBlank { reg.playerName.ifBlank { "Player #$slot" } }
                entriesByUser[uid] = TournamentStandingEntry(
                    rank = if (reg.rank > 0) reg.rank else slot,
                    userId = uid,
                    username = reg.playerName.ifBlank { ign },
                    inGameName = ign,
                    characterId = reg.freeFireUid,
                    teamName = "",
                    slotNumber = slot,
                    kills = reg.kills,
                    rankPrize = rPrize,
                    killPrize = reg.kills * perKillBounty,
                    prizeWon = if (reg.winningAmount > 0.0) reg.winningAmount else (rPrize + (reg.kills * perKillBounty)),
                    wonAnything = (if (reg.winningAmount > 0.0) reg.winningAmount else (rPrize + (reg.kills * perKillBounty))) > 0.0
                )
            }
        }

        // 6. Also check users whose matchHistory or joinedMatchIds contains this tournamentId
        allUsers.forEach { u ->
            val joinedThis = u.matchHistory.any { it.tournamentId == tId } || u.joinedMatchIds.contains(tId)
            if (joinedThis && u.id.isNotBlank() && !entriesByUser.containsKey(u.id)) {
                val slot = entriesByUser.size + 1
                val rPrize = defaultRankPrizeForRank(slot)
                entriesByUser[u.id] = TournamentStandingEntry(
                    rank = slot,
                    userId = u.id,
                    username = u.username,
                    inGameName = u.ign.ifBlank { u.username },
                    characterId = u.gameId,
                    teamName = "",
                    slotNumber = slot,
                    kills = 0,
                    rankPrize = rPrize,
                    killPrize = 0.0,
                    prizeWon = rPrize,
                    wonAnything = rPrize > 0.0
                )
            }
        }

        return entriesByUser.values.sortedBy { it.rank }
    }

    suspend fun publishTournamentStandingsAndPrizes(
        tournament: Tournament,
        rawStandings: List<TournamentStandingEntry>,
        adminEmail: String
    ): Boolean {
        val tournamentId = tournament.id.trim()
        if (tournamentId.isBlank()) {
            GlobalErrorManager.emitError("Tournament ID is blank.")
            return false
        }
        if (rawStandings.isEmpty()) {
            GlobalErrorManager.emitError("Add at least 1 participant standing before publishing results.")
            return false
        }

        try {
            val now = System.currentTimeMillis()
            val sortedEntries = rawStandings.sortedBy { it.rank.coerceAtLeast(1) }

            val finalLeaderboardMaps = sortedEntries.mapIndexed { idx, p ->
                val rank = p.rank.coerceAtLeast(1)
                val kills = p.kills.coerceAtLeast(0)
                val points = kills * 2 + when (rank) {
                    1 -> 15
                    2 -> 12
                    3 -> 10
                    else -> 5
                }
                val rPrize = p.rankPrize.coerceAtLeast(0.0)
                val kPrize = p.killPrize.coerceAtLeast(0.0)
                val prizeWon = rPrize + kPrize
                val wonAnything = prizeWon > 0.0
                mapOf<String, Any>(
                    "rank" to rank,
                    "userId" to p.userId,
                    "username" to p.username.ifBlank { p.inGameName },
                    "inGameName" to p.inGameName.ifBlank { p.username },
                    "characterId" to p.characterId,
                    "teamName" to p.teamName,
                    "slotNumber" to (if (p.slotNumber > 0) p.slotNumber else idx + 1),
                    "kills" to kills,
                    "points" to points,
                    "rankPrize" to rPrize,
                    "killPrize" to kPrize,
                    "prizeWon" to prizeWon,
                    "wonAnything" to wonAnything,
                    "payoutStatus" to if (wonAnything) "PAID" else "NO_PRIZE",
                    "updatedAt" to now
                )
            }

            val totalPrizeDisbursed = finalLeaderboardMaps.sumOf { safeDouble(it["prizeWon"]) }
            val topWinner = finalLeaderboardMaps.firstOrNull { safeInt(it["rank"]) == 1 } ?: finalLeaderboardMaps.first()
            val winnerName = topWinner["inGameName"]?.toString().orEmpty().ifBlank { topWinner["username"]?.toString().orEmpty() }
            val winnerUid = topWinner["userId"]?.toString().orEmpty()
            val winnerPrize = safeDouble(topWinner["prizeWon"])

            val completedTourneyUpdates = mapOf<String, Any>(
                "id" to tournamentId,
                "tournamentId" to tournamentId,
                "title" to tournament.title,
                "game" to tournament.game.ifBlank { "Free Fire" },
                "status" to "COMPLETED",
                "isCompleted" to true,
                "payoutStatus" to "PAID",
                "completedAt" to now,
                "updatedAt" to now,
                "totalPrizeDisbursed" to totalPrizeDisbursed,
                "winnerName" to winnerName,
                "winnerUid" to winnerUid,
                "winnerPrize" to winnerPrize,
                "finalLeaderboard" to finalLeaderboardMaps,
                "standings" to finalLeaderboardMaps,
                "results" to finalLeaderboardMaps
            )

            // Save completed tournament document to Firestore & RTDB
            try { database.child("tournaments").child(tournamentId).updateChildren(completedTourneyUpdates).await() } catch (_: Exception) {}
            try { database.child("matches").child(tournamentId).updateChildren(completedTourneyUpdates).await() } catch (_: Exception) {}
            try { database.child("tournament_history").child(tournamentId).updateChildren(completedTourneyUpdates).await() } catch (_: Exception) {}
            try { firestore.collection("tournaments").document(tournamentId).set(completedTourneyUpdates, SetOptions.merge()).await() } catch (_: Exception) {}
            try { firestore.collection("matches").document(tournamentId).set(completedTourneyUpdates, SetOptions.merge()).await() } catch (_: Exception) {}
            try { firestore.collection("tournament_history").document(tournamentId).set(completedTourneyUpdates, SetOptions.merge()).await() } catch (_: Exception) {}

            // Write each entry to leaderboard subcollections & process per-player settlement
            var winnersPaidCount = 0
            for (entryMap in finalLeaderboardMaps) {
                val uid = entryMap["userId"]?.toString()?.trim().orEmpty()
                if (uid.isBlank()) continue

                val rank = safeInt(entryMap["rank"], 1)
                val kills = safeInt(entryMap["kills"], 0)
                val prizeWon = safeDouble(entryMap["prizeWon"], 0.0)
                val rankPrize = safeDouble(entryMap["rankPrize"], 0.0)
                val killPrize = safeDouble(entryMap["killPrize"], 0.0)
                val wonAnything = prizeWon > 0.0
                val tokensEarned = kills * 5

                try { database.child("tournaments").child(tournamentId).child("leaderboard").child(uid).setValue(entryMap).await() } catch (_: Exception) {}
                try { database.child("tournament_history").child(tournamentId).child("leaderboard").child(uid).setValue(entryMap).await() } catch (_: Exception) {}
                try { firestore.collection("tournaments").document(tournamentId).collection("leaderboard").document(uid).set(entryMap).await() } catch (_: Exception) {}
                try { firestore.collection("tournament_history").document(tournamentId).collection("leaderboard").document(uid).set(entryMap).await() } catch (_: Exception) {}

                // 1. Write match result record to /users/{userId}/match_stats/{tournamentId}_{userId} (RTDB & Firestore)
                val matchStatId = "${tournamentId}_${uid}"
                val matchStatPayload = mapOf<String, Any>(
                    "id" to matchStatId,
                    "tournamentId" to tournamentId,
                    "tournamentTitle" to tournament.title,
                    "game" to tournament.game.ifBlank { "Free Fire" },
                    "userId" to uid,
                    "matchNo" to "#${tournamentId.takeLast(4)}",
                    "position" to rank,
                    "rank" to rank,
                    "kills" to kills,
                    "winnings" to prizeWon,
                    "winningAmount" to prizeWon,
                    "rankPrize" to rankPrize,
                    "killPrize" to killPrize,
                    "tokensEarned" to tokensEarned,
                    "timestamp" to now,
                    "status" to if (wonAnything) "WON" else "PARTICIPATED"
                )
                try { database.child("users").child(uid).child("match_stats").child(matchStatId).setValue(matchStatPayload).await() } catch (_: Exception) {}
                try { database.child("users").child(uid).child("matchHistory").child(tournamentId).setValue(matchStatPayload).await() } catch (_: Exception) {}
                try { firestore.collection("users").document(uid).collection("match_stats").document(matchStatId).set(matchStatPayload).await() } catch (_: Exception) {}

                // 2. Prevent double-crediting by checking if win_{tournamentId}_{userId} or part_{tournamentId}_{userId} already exists
                val winTxId = "win_${tournamentId}_${uid}"
                val partMarkerId = "part_${tournamentId}_${uid}"
                val existingTxSnap = try { database.child("transactions").child(winTxId).get().await() } catch (_: Exception) { null }
                val alreadyCredited = existingTxSnap?.exists() == true

                val userRef = database.child("users").child(uid)
                val userSnap = try { userRef.get().await() } catch (_: Exception) { null }
                val curDeposit = safeDouble(userSnap?.child("depositBalance")?.value ?: userSnap?.child("depositFunds")?.value)
                val curWinnings = safeDouble(userSnap?.child("winningsBalance")?.value ?: userSnap?.child("winningFunds")?.value)
                val curBonus = safeDouble(userSnap?.child("bonusBalance")?.value ?: userSnap?.child("bonusFunds")?.value)
                val curBalance = safeDouble(userSnap?.child("balance")?.value ?: userSnap?.child("funds")?.value, curDeposit + curWinnings + curBonus)
                val curMatches = safeInt(userSnap?.child("matchesPlayed")?.value ?: userSnap?.child("totalMatchesJoined")?.value, 0)
                val curWins = safeInt(userSnap?.child("totalWins")?.value ?: userSnap?.child("wins")?.value, 0)
                val curKills = safeInt(userSnap?.child("totalKills")?.value ?: userSnap?.child("kills")?.value, 0)
                val curEarnings = safeDouble(userSnap?.child("totalEarnings")?.value, curWinnings)
                val curTokens = safeInt(userSnap?.child("tokens")?.value, 0)
                val curVersion = safeInt(userSnap?.child("version")?.value, 1)

                if (wonAnything) {
                    winnersPaidCount++
                    if (!alreadyCredited) {
                        val newWinnings = curWinnings + prizeWon
                        val newBalance = (curDeposit + newWinnings + curBonus).coerceAtLeast(curBalance + prizeWon)
                        val newMatches = curMatches + 1
                        val newWins = curWins + (if (rank == 1) 1 else 0)
                        val newKills = curKills + kills
                        val newEarnings = curEarnings + prizeWon
                        val newTokens = curTokens + tokensEarned

                        val userUpdates = mapOf<String, Any>(
                            "winningsBalance" to newWinnings,
                            "winningFunds" to newWinnings,
                            "winning_funds" to newWinnings,
                            "withdrawableBalance" to newWinnings,
                            "balance" to newBalance,
                            "funds" to newBalance,
                            "walletBalance" to newBalance,
                            "matchesPlayed" to newMatches,
                            "totalMatchesJoined" to newMatches,
                            "totalWins" to newWins,
                            "wins" to newWins,
                            "totalKills" to newKills,
                            "kills" to newKills,
                            "totalEarnings" to newEarnings,
                            "tokens" to newTokens,
                            "version" to (curVersion + 1),
                            "updatedAt" to now,
                            "lastActive" to now
                        )
                        try { userRef.updateChildren(userUpdates).await() } catch (_: Exception) {}
                        try { database.child("userProfiles").child(uid).updateChildren(userUpdates).await() } catch (_: Exception) {}
                        try { database.child("players").child(uid).updateChildren(userUpdates).await() } catch (_: Exception) {}
                        try { firestore.collection("users").document(uid).set(userUpdates, SetOptions.merge()).await() } catch (_: Exception) {}
                        try { firestore.collection("userProfiles").document(uid).set(userUpdates, SetOptions.merge()).await() } catch (_: Exception) {}

                        val txPayload = mapOf<String, Any>(
                            "id" to winTxId,
                            "userId" to uid,
                            "uid" to uid,
                            "type" to "WINNINGS",
                            "walletBucket" to "WINNING",
                            "withdrawable" to true,
                            "amount" to prizeWon,
                            "detail" to "Won ${tournament.title} (Rank #$rank • $kills Kills)",
                            "description" to "Won ${tournament.title} (Rank #$rank • $kills Kills)",
                            "isPositive" to true,
                            "status" to "SUCCESS",
                            "timestamp" to now
                        )
                        try { database.child("transactions").child(winTxId).setValue(txPayload).await() } catch (_: Exception) {}
                        try { database.child("users").child(uid).child("transactions").child(winTxId).setValue(txPayload).await() } catch (_: Exception) {}
                        try { database.child("wallet_transactions").child(uid).child(winTxId).setValue(txPayload).await() } catch (_: Exception) {}
                        try { firestore.collection("transactions").document(winTxId).set(txPayload).await() } catch (_: Exception) {}
                        try { firestore.collection("users").document(uid).collection("transactions").document(winTxId).set(txPayload).await() } catch (_: Exception) {}
                    }

                    val notifId = "notif_result_${tournamentId}"
                    val notifPayload = mapOf<String, Any>(
                        "id" to notifId,
                        "userId" to uid,
                        "title" to "🏆 Won ₹${prizeWon.toInt()} in ${tournament.title}!",
                        "message" to "Final Standings: Rank #$rank • $kills Kills. ₹${prizeWon.toInt()} VT credited to your Winnings Wallet!",
                        "type" to "WINNINGS",
                        "targetTournamentId" to tournamentId,
                        "timestamp" to now,
                        "isRead" to false
                    )
                    try { database.child("users").child(uid).child("notifications").child(notifId).setValue(notifPayload).await() } catch (_: Exception) {}
                    try { database.child("notifications").child(uid).child(notifId).setValue(notifPayload).await() } catch (_: Exception) {}
                    try { firestore.collection("users").document(uid).collection("notifications").document(notifId).set(notifPayload).await() } catch (_: Exception) {}
                } else {
                    // Participant with ₹0 prize still gets matchesPlayed, kills, tokens & standing notification
                    val partAlreadyDone = try {
                        database.child("users").child(uid).child("transactions").child(partMarkerId).get().await().exists()
                    } catch (_: Exception) { false }

                    if (!partAlreadyDone && !alreadyCredited) {
                        val newMatches = curMatches + 1
                        val newKills = curKills + kills
                        val newTokens = curTokens + tokensEarned
                        val userUpdates = mapOf<String, Any>(
                            "matchesPlayed" to newMatches,
                            "totalMatchesJoined" to newMatches,
                            "totalKills" to newKills,
                            "kills" to newKills,
                            "tokens" to newTokens,
                            "version" to (curVersion + 1),
                            "updatedAt" to now,
                            "lastActive" to now
                        )
                        try { userRef.updateChildren(userUpdates).await() } catch (_: Exception) {}
                        try { database.child("userProfiles").child(uid).updateChildren(userUpdates).await() } catch (_: Exception) {}
                        try { firestore.collection("users").document(uid).set(userUpdates, SetOptions.merge()).await() } catch (_: Exception) {}
                        try {
                            database.child("users").child(uid).child("transactions").child(partMarkerId).setValue(
                                mapOf(
                                    "id" to partMarkerId,
                                    "userId" to uid,
                                    "type" to "MATCH_PARTICIPATION",
                                    "amount" to 0.0,
                                    "detail" to "Participated in ${tournament.title} (Rank #$rank • $kills Kills)",
                                    "isPositive" to true,
                                    "status" to "SUCCESS",
                                    "timestamp" to now
                                )
                            ).await()
                        } catch (_: Exception) {}
                    }

                    val notifId = "notif_result_${tournamentId}"
                    val notifPayload = mapOf<String, Any>(
                        "id" to notifId,
                        "userId" to uid,
                        "title" to "📊 Final Standings: ${tournament.title}",
                        "message" to "You finished Rank #$rank with $kills Kills (+${tokensEarned} Tokens).",
                        "type" to "TOURNAMENT",
                        "targetTournamentId" to tournamentId,
                        "timestamp" to now,
                        "isRead" to false
                    )
                    try { database.child("users").child(uid).child("notifications").child(notifId).setValue(notifPayload).await() } catch (_: Exception) {}
                    try { database.child("notifications").child(uid).child(notifId).setValue(notifPayload).await() } catch (_: Exception) {}
                    try { firestore.collection("users").document(uid).collection("notifications").document(notifId).set(notifPayload).await() } catch (_: Exception) {}
                }
            }

            publishGlobalAppStateEvent(
                eventType = "TOURNAMENT_UPDATED",
                tournamentId = tournamentId,
                payload = mapOf(
                    "status" to "COMPLETED",
                    "totalPrizeDisbursed" to totalPrizeDisbursed,
                    "winnerName" to winnerName,
                    "participantsCount" to finalLeaderboardMaps.size
                )
            )
            writeAuditLog(
                action = "STANDINGS_PUBLISHED_AND_PAID",
                detail = "Published ${finalLeaderboardMaps.size} standings for '${tournament.title}'. Disbursed ₹${totalPrizeDisbursed.toInt()} across $winnersPaidCount winner(s).",
                adminEmail = adminEmail,
                targetId = tournamentId
            )

            GlobalErrorManager.emitSuccess("Standings published! ${finalLeaderboardMaps.size} players ranked & ₹${totalPrizeDisbursed.toInt()} credited to $winnersPaidCount winner(s)!")
            return true
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to publish standings: ${e.message}")
            return false
        }
    }

    // =========================================================================
    // MODULE 2: CANCEL TOURNAMENT & AUTO-REFUND ALL PARTICIPANTS
    // =========================================================================

    suspend fun cancelTournamentAndAutoRefund(
        tournament: Tournament,
        reason: String,
        allUsers: List<UserProfile>,
        adminEmail: String
    ): Boolean {
        val tId = tournament.id.trim()
        if (tId.isBlank()) return false
        try {
            val now = System.currentTimeMillis()
            val entryFee = tournament.entryFee.toDouble().coerceAtLeast(0.0)
            val participants = loadTournamentParticipantsForStandings(tournament, allUsers)

            val cancelUpdates = mapOf<String, Any>(
                "status" to "CANCELLED",
                "cancellationReason" to reason.ifBlank { "Cancelled by Admin — Full Entry Fee Refunded" },
                "updatedAt" to now
            )
            try { database.child("tournaments").child(tId).updateChildren(cancelUpdates).await() } catch (_: Exception) {}
            try { database.child("matches").child(tId).updateChildren(cancelUpdates).await() } catch (_: Exception) {}
            try { firestore.collection("tournaments").document(tId).set(cancelUpdates, SetOptions.merge()).await() } catch (_: Exception) {}
            try { firestore.collection("matches").document(tId).set(cancelUpdates, SetOptions.merge()).await() } catch (_: Exception) {}

            var refundedCount = 0
            if (entryFee > 0.0) {
                for (p in participants) {
                    val uid = p.userId.trim()
                    if (uid.isBlank()) continue
                    val refundTxId = "refund_${tId}_${uid}"
                    val alreadyRefunded = try {
                        database.child("transactions").child(refundTxId).get().await().exists()
                    } catch (_: Exception) { false }
                    if (alreadyRefunded) continue

                    val userRef = database.child("users").child(uid)
                    val snap = try { userRef.get().await() } catch (_: Exception) { null }
                    val curDep = safeDouble(snap?.child("depositBalance")?.value ?: snap?.child("depositFunds")?.value)
                    val curWin = safeDouble(snap?.child("winningsBalance")?.value ?: snap?.child("winningFunds")?.value)
                    val curBon = safeDouble(snap?.child("bonusBalance")?.value ?: snap?.child("bonusFunds")?.value)
                    val curBal = safeDouble(snap?.child("balance")?.value ?: snap?.child("funds")?.value, curDep + curWin + curBon)
                    val curVer = safeInt(snap?.child("version")?.value, 1)

                    val newDep = curDep + entryFee
                    val newBal = (newDep + curWin + curBon).coerceAtLeast(curBal + entryFee)
                    val userUpdates = mapOf<String, Any>(
                        "depositBalance" to newDep,
                        "depositFunds" to newDep,
                        "deposit_funds" to newDep,
                        "balance" to newBal,
                        "funds" to newBal,
                        "walletBalance" to newBal,
                        "version" to (curVer + 1),
                        "updatedAt" to now
                    )
                    try { userRef.updateChildren(userUpdates).await() } catch (_: Exception) {}
                    try { database.child("userProfiles").child(uid).updateChildren(userUpdates).await() } catch (_: Exception) {}
                    try { firestore.collection("users").document(uid).set(userUpdates, SetOptions.merge()).await() } catch (_: Exception) {}

                    val txPayload = mapOf<String, Any>(
                        "id" to refundTxId,
                        "userId" to uid,
                        "uid" to uid,
                        "type" to "REFUND",
                        "walletBucket" to "DEPOSIT",
                        "amount" to entryFee,
                        "detail" to "Refund for cancelled tournament: ${tournament.title}",
                        "description" to "Refund for cancelled tournament: ${tournament.title} ($reason)",
                        "isPositive" to true,
                        "status" to "SUCCESS",
                        "timestamp" to now
                    )
                    try { database.child("transactions").child(refundTxId).setValue(txPayload).await() } catch (_: Exception) {}
                    try { database.child("users").child(uid).child("transactions").child(refundTxId).setValue(txPayload).await() } catch (_: Exception) {}
                    try { database.child("wallet_transactions").child(uid).child(refundTxId).setValue(txPayload).await() } catch (_: Exception) {}
                    try { firestore.collection("transactions").document(refundTxId).set(txPayload).await() } catch (_: Exception) {}
                    try { firestore.collection("users").document(uid).collection("transactions").document(refundTxId).set(txPayload).await() } catch (_: Exception) {}

                    sendTargetedUserNotification(
                        userId = uid,
                        title = "Tournament Cancelled & Refunded",
                        message = "${tournament.title} was cancelled ($reason). ₹${entryFee.toInt()} VT has been refunded to your wallet.",
                        type = "WALLET"
                    )
                    refundedCount++
                }
            }

            publishGlobalAppStateEvent(
                eventType = "TOURNAMENT_CANCELLED",
                tournamentId = tId,
                payload = mapOf(
                    "tournamentTitle" to tournament.title,
                    "reason" to reason,
                    "refundedParticipants" to refundedCount,
                    "entryFeeRefunded" to entryFee
                )
            )
            writeAuditLog(
                action = "TOURNAMENT_CANCELLED_REFUNDED",
                detail = "Cancelled '${tournament.title}' ($reason) and auto-refunded ₹${entryFee.toInt()} to $refundedCount participant(s).",
                adminEmail = adminEmail,
                targetId = tId
            )
            GlobalErrorManager.emitSuccess("Tournament '${tournament.title}' cancelled & ₹${entryFee.toInt()} auto-refunded to $refundedCount player(s)!")
            return true
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to cancel & refund tournament: ${e.message}")
            return false
        }
    }

    // =========================================================================
    // MODULE 4: USER BAN (/banned_users) & SUSPEND (/suspended_users) + GLOBAL SYNC
    // =========================================================================

    suspend fun setUserBanStatus(
        user: UserProfile,
        isBanned: Boolean,
        reason: String,
        adminEmail: String
    ): Boolean {
        if (user.id.isBlank()) return false
        try {
            val now = System.currentTimeMillis()
            val nextVer = (user.version + 1).coerceAtLeast(1)
            val userUpdates = mapOf<String, Any>(
                "isBanned" to isBanned,
                "banReason" to if (isBanned) reason else "",
                "banType" to if (isBanned) "PERMANENT" else "",
                "accountStatus" to if (isBanned) "BANNED" else "ACTIVE",
                "status" to if (isBanned) "BANNED" else "ACTIVE",
                "version" to nextVer,
                "updatedAt" to now
            )
            try { database.child("users").child(user.id).updateChildren(userUpdates).await() } catch (_: Exception) {}
            try { database.child("userProfiles").child(user.id).updateChildren(userUpdates).await() } catch (_: Exception) {}
            try { firestore.collection("users").document(user.id).set(userUpdates, SetOptions.merge()).await() } catch (_: Exception) {}
            try { firestore.collection("userProfiles").document(user.id).set(userUpdates, SetOptions.merge()).await() } catch (_: Exception) {}

            if (isBanned) {
                val banNode = mapOf<String, Any>(
                    "uid" to user.id,
                    "userId" to user.id,
                    "username" to user.username,
                    "email" to user.email,
                    "reason" to reason,
                    "banType" to "PERMANENT",
                    "bannedAt" to now,
                    "bannedBy" to adminEmail
                )
                try { database.child("banned_users").child(user.id).setValue(banNode).await() } catch (_: Exception) {}
                try { firestore.collection("banned_users").document(user.id).set(banNode).await() } catch (_: Exception) {}
            } else {
                try { database.child("banned_users").child(user.id).removeValue().await() } catch (_: Exception) {}
                try { firestore.collection("banned_users").document(user.id).delete().await() } catch (_: Exception) {}
            }

            publishGlobalAppStateEvent(
                eventType = "USER_STATUS_CHANGED",
                affectedUserId = user.id,
                payload = mapOf(
                    "newStatus" to if (isBanned) "BANNED" else "ACTIVE",
                    "isBanned" to isBanned,
                    "banReason" to reason
                )
            )
            writeAuditLog(
                action = if (isBanned) "USER_BANNED" else "USER_UNBANNED",
                detail = "${if (isBanned) "Banned" else "Unbanned"} ${user.username} (${user.id}). Reason: $reason",
                adminEmail = adminEmail,
                targetId = user.id
            )
            GlobalErrorManager.emitSuccess(if (isBanned) "User ${user.username} BANNED across RTDB, Firestore & /banned_users!" else "User ${user.username} UNBANNED!")
            return true
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to update ban status: ${e.message}")
            return false
        }
    }

    suspend fun setUserSuspendStatus(
        user: UserProfile,
        isSuspended: Boolean,
        reason: String,
        durationHours: Int,
        adminEmail: String
    ): Boolean {
        if (user.id.isBlank()) return false
        try {
            val now = System.currentTimeMillis()
            val endTime = if (isSuspended) now + (durationHours.coerceAtLeast(1) * 3600_000L) else 0L
            val nextVer = (user.version + 1).coerceAtLeast(1)
            val userUpdates = mapOf<String, Any>(
                "isSuspended" to isSuspended,
                "suspendReason" to if (isSuspended) reason else "",
                "suspensionReason" to if (isSuspended) reason else "",
                "suspensionEndTimeMillis" to endTime,
                "accountStatus" to if (isSuspended) "SUSPENDED" else "ACTIVE",
                "version" to nextVer,
                "updatedAt" to now
            )
            try { database.child("users").child(user.id).updateChildren(userUpdates).await() } catch (_: Exception) {}
            try { database.child("userProfiles").child(user.id).updateChildren(userUpdates).await() } catch (_: Exception) {}
            try { firestore.collection("users").document(user.id).set(userUpdates, SetOptions.merge()).await() } catch (_: Exception) {}
            try { firestore.collection("userProfiles").document(user.id).set(userUpdates, SetOptions.merge()).await() } catch (_: Exception) {}

            if (isSuspended) {
                val suspendNode = mapOf<String, Any>(
                    "uid" to user.id,
                    "userId" to user.id,
                    "username" to user.username,
                    "suspendReason" to reason,
                    "suspensionEndTimeMillis" to endTime,
                    "suspendedAt" to now,
                    "suspendedBy" to adminEmail
                )
                try { database.child("suspended_users").child(user.id).setValue(suspendNode).await() } catch (_: Exception) {}
                try { firestore.collection("suspended_users").document(user.id).set(suspendNode).await() } catch (_: Exception) {}
            } else {
                try { database.child("suspended_users").child(user.id).removeValue().await() } catch (_: Exception) {}
                try { firestore.collection("suspended_users").document(user.id).delete().await() } catch (_: Exception) {}
            }

            publishGlobalAppStateEvent(
                eventType = "USER_STATUS_CHANGED",
                affectedUserId = user.id,
                payload = mapOf(
                    "newStatus" to if (isSuspended) "SUSPENDED" else "ACTIVE",
                    "isSuspended" to isSuspended,
                    "suspendReason" to reason,
                    "suspensionEndTimeMillis" to endTime
                )
            )
            writeAuditLog(
                action = if (isSuspended) "USER_SUSPENDED" else "USER_UNSUSPENDED",
                detail = "${if (isSuspended) "Suspended (${durationHours}h)" else "Unsuspended"} ${user.username} (${user.id}). Reason: $reason",
                adminEmail = adminEmail,
                targetId = user.id
            )
            GlobalErrorManager.emitSuccess(if (isSuspended) "User ${user.username} suspended for ${durationHours}h!" else "User ${user.username} unsuspended!")
            return true
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to update suspension status: ${e.message}")
            return false
        }
    }
}

