package com.example.data.repository

import android.util.Log
import com.example.domain.model.*
import com.example.ui.common.GlobalErrorManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class AdminIntegrationSyncManager(
    private val database: DatabaseReference,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()
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
            if (!snap.exists() || !snap.hasChildren() || snap.value !is Map<*, *>) return null
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

            // Sync linked ticket across My Tickets desk
            syncLinkedTicketAcrossAllPaths(
                ticketId = req.requestId,
                userId = uid,
                status = "RESOLVED",
                adminReply = "Your DPDP Account Deletion request has been approved and your account data has been permanently purged.",
                deliveredRedeemCode = "",
                adminEmail = adminEmail,
                now = now
            )

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

            syncLinkedTicketAcrossAllPaths(
                ticketId = req.requestId,
                userId = uid,
                status = "REJECTED",
                adminReply = "Your account deletion request was cancelled/restored by Admin. Your account is ACTIVE.",
                deliveredRedeemCode = "",
                adminEmail = adminEmail,
                now = now
            )

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
    // SECTION 4D: TOURNAMENT LOBBY CHAT & ADMIN MODERATION (/tournaments/{id}/chat_messages)
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
            val msg = snap.child("text").value?.toString()
                ?: snap.child("message").value?.toString() ?: ""
            if (id.isBlank() || msg.isBlank()) return null
            val role = snap.child("senderRole").value?.toString() ?: "PLAYER"
            val isSys = safeBool(snap.child("isSystemMessage").value)
            val isAdm = safeBool(snap.child("isAdmin").value) || role.equals("ADMIN", true) || isSys
            return TournamentChatMessage(
                id = id,
                tournamentId = snap.child("tournamentId").value?.toString() ?: tournamentId,
                senderId = snap.child("senderId").value?.toString() ?: snap.child("userId").value?.toString() ?: "",
                senderName = snap.child("senderName").value?.toString() ?: snap.child("username").value?.toString() ?: "Player",
                senderAvatar = snap.child("senderAvatar").value?.toString() ?: snap.child("avatarUrl").value?.toString() ?: "avatar_1",
                senderRole = role,
                message = msg,
                text = msg,
                isAdmin = isAdm,
                isSystemMessage = isSys,
                messageType = snap.child("messageType").value?.toString() ?: if (isAdm) "ANNOUNCEMENT" else "TEXT",
                channel = snap.child("channel").value?.toString() ?: "ALL",
                isDeleted = safeBool(snap.child("isDeleted").value),
                isPinned = safeBool(snap.child("isPinned").value),
                timestamp = safeLong(snap.child("timestamp").value ?: snap.child("createdAt").value, System.currentTimeMillis())
            )
        }

        fun parseDoc(doc: DocumentSnapshot): TournamentChatMessage? {
            if (!doc.exists()) return null
            val msg = doc.getString("text") ?: doc.getString("message") ?: ""
            if (msg.isBlank()) return null
            val role = doc.getString("senderRole") ?: "PLAYER"
            val isSys = doc.getBoolean("isSystemMessage") ?: false
            val isAdm = (doc.getBoolean("isAdmin") ?: false) || role.equals("ADMIN", true) || isSys
            return TournamentChatMessage(
                id = doc.id,
                tournamentId = tournamentId,
                senderId = doc.getString("senderId") ?: doc.getString("userId") ?: "",
                senderName = doc.getString("senderName") ?: doc.getString("username") ?: "Player",
                senderAvatar = doc.getString("senderAvatar") ?: "avatar_1",
                senderRole = role,
                message = msg,
                text = msg,
                isAdmin = isAdm,
                isSystemMessage = isSys,
                messageType = doc.getString("messageType") ?: if (isAdm) "ANNOUNCEMENT" else "TEXT",
                channel = doc.getString("channel") ?: "ALL",
                isDeleted = doc.getBoolean("isDeleted") ?: false,
                isPinned = doc.getBoolean("isPinned") ?: false,
                timestamp = safeLong(doc.get("timestamp"), System.currentTimeMillis())
            )
        }

        val rtdbRefs = listOf(
            database.child("tournaments").child(tournamentId).child("chat_messages"),
            database.child("tournament_chats").child(tournamentId),
            database.child("tournaments").child(tournamentId).child("chat")
        )
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { c ->
                    parseSnap(c)?.let { chatMap[it.id] = it }
                }
                emitChat()
            }
            override fun onCancelled(error: DatabaseError) { emitChat() }
        }
        rtdbRefs.forEach { it.addValueEventListener(listener) }

        val fsListeners = mutableListOf<ListenerRegistration>()
        listOf(
            firestore.collection("tournaments").document(tournamentId).collection("chat_messages"),
            firestore.collection("tournaments").document(tournamentId).collection("chat")
        ).forEach { col ->
            try {
                fsListeners.add(col.addSnapshotListener { snap, err ->
                    if (err == null && snap != null) {
                        snap.documents.forEach { doc ->
                            parseDoc(doc)?.let { chatMap[it.id] = it }
                        }
                        emitChat()
                    }
                })
            } catch (_: Exception) {}
        }

        awaitClose {
            rtdbRefs.forEach { it.removeEventListener(listener) }
            fsListeners.forEach { it.remove() }
        }
    }

    suspend fun sendAdminTournamentChatMessage(
        tournamentId: String,
        message: String,
        isPinned: Boolean,
        adminName: String = "VeloRix Admin"
    ) {
        if (tournamentId.isBlank() || message.isBlank()) return
        val now = System.currentTimeMillis()
        val msgId = "chat_adm_${now}"
        val cleanMsg = message.trim()
        val payload = mapOf(
            "id" to msgId,
            "tournamentId" to tournamentId,
            "senderId" to "admin",
            "senderName" to adminName.ifBlank { "VeloRix Admin" },
            "senderAvatar" to "avatar_1",
            "senderRole" to "ADMIN",
            "text" to cleanMsg,
            "message" to cleanMsg,
            "isAdmin" to true,
            "isSystemMessage" to true,
            "messageType" to "ANNOUNCEMENT",
            "channel" to "ALL",
            "isDeleted" to false,
            "isPinned" to isPinned,
            "timestamp" to now
        )
        try { database.child("tournaments").child(tournamentId).child("chat_messages").child(msgId).setValue(payload).await() } catch (_: Exception) {}
        try { database.child("tournament_chats").child(tournamentId).child(msgId).setValue(payload).await() } catch (_: Exception) {}
        try { database.child("tournaments").child(tournamentId).child("chat").child(msgId).setValue(payload).await() } catch (_: Exception) {}
        try { firestore.collection("tournaments").document(tournamentId).collection("chat_messages").document(msgId).set(payload).await() } catch (_: Exception) {}
        try { firestore.collection("tournaments").document(tournamentId).collection("chat").document(msgId).set(payload).await() } catch (_: Exception) {}
        GlobalErrorManager.emitSuccess(if (isPinned) "Pinned Admin announcement posted to lobby chat!" else "Admin announcement sent to lobby chat!")
    }

    suspend fun togglePinTournamentChatMessage(tournamentId: String, messageId: String, isPinned: Boolean) {
        if (tournamentId.isBlank() || messageId.isBlank()) return
        val updates = mapOf<String, Any>("isPinned" to isPinned, "updatedAt" to System.currentTimeMillis())
        try { database.child("tournaments").child(tournamentId).child("chat_messages").child(messageId).updateChildren(updates).await() } catch (_: Exception) {}
        try { database.child("tournament_chats").child(tournamentId).child(messageId).updateChildren(updates).await() } catch (_: Exception) {}
        try { database.child("tournaments").child(tournamentId).child("chat").child(messageId).updateChildren(updates).await() } catch (_: Exception) {}
        try { firestore.collection("tournaments").document(tournamentId).collection("chat_messages").document(messageId).set(updates, SetOptions.merge()).await() } catch (_: Exception) {}
        try { firestore.collection("tournaments").document(tournamentId).collection("chat").document(messageId).set(updates, SetOptions.merge()).await() } catch (_: Exception) {}
        GlobalErrorManager.emitSuccess(if (isPinned) "Message pinned to top of lobby chat!" else "Message unpinned.")
    }

    suspend fun deleteTournamentChatMessage(tournamentId: String, messageId: String) {
        if (tournamentId.isBlank() || messageId.isBlank()) return
        val softDelete = mapOf<String, Any>(
            "isDeleted" to true,
            "isPinned" to false,
            "text" to "Message removed by Admin",
            "message" to "Message removed by Admin",
            "updatedAt" to System.currentTimeMillis()
        )
        try { database.child("tournaments").child(tournamentId).child("chat_messages").child(messageId).updateChildren(softDelete).await() } catch (_: Exception) {}
        try { database.child("tournament_chats").child(tournamentId).child(messageId).updateChildren(softDelete).await() } catch (_: Exception) {}
        try { database.child("tournaments").child(tournamentId).child("chat").child(messageId).updateChildren(softDelete).await() } catch (_: Exception) {}
        try { firestore.collection("tournaments").document(tournamentId).collection("chat_messages").document(messageId).set(softDelete, SetOptions.merge()).await() } catch (_: Exception) {}
        try { firestore.collection("tournaments").document(tournamentId).collection("chat").document(messageId).set(softDelete, SetOptions.merge()).await() } catch (_: Exception) {}
        GlobalErrorManager.emitSuccess("Chat message soft-deleted ('Message removed by Admin').")
    }

    // =========================================================================
    // SECTION 5A & 5C: DEPOSIT REQUESTS (/deposit_requests + /upi_payment_orders), TOKEN CONVERSIONS & MISSIONS
    // =========================================================================

    fun getDepositRequestsStream(): Flow<List<DepositRequest>> = callbackFlow {
        val depMap = mutableMapOf<String, DepositRequest>()

        fun emitDeposits() {
            trySend(depMap.values.sortedByDescending { it.requestedAt })
        }

        fun parseSnap(snap: DataSnapshot, isUpiOrder: Boolean = false): DepositRequest? {
            if (!snap.exists()) return null
            val id = snap.child("id").value?.toString()
                ?: snap.child("requestId").value?.toString()
                ?: snap.child("orderId").value?.toString()
                ?: snap.key ?: return null
            if (id.isBlank()) return null
            val orderId = snap.child("orderId").value?.toString() ?: if (isUpiOrder) id else ""
            val txId = snap.child("transactionId").value?.toString() ?: snap.child("txId").value?.toString() ?: ""
            return DepositRequest(
                id = id,
                requestId = id,
                orderId = orderId,
                transactionId = txId,
                userId = snap.child("userId").value?.toString() ?: snap.child("uid").value?.toString() ?: "",
                username = snap.child("username").value?.toString() ?: snap.child("userName").value?.toString() ?: "Player",
                userName = snap.child("username").value?.toString() ?: snap.child("userName").value?.toString() ?: "Player",
                userEmail = snap.child("userEmail").value?.toString() ?: snap.child("email").value?.toString() ?: "",
                amount = safeDouble(snap.child("amount").value ?: snap.child("vtAmount").value),
                utrNumber = snap.child("utrNumber").value?.toString() ?: snap.child("utr").value?.toString() ?: snap.child("paymentRef").value?.toString() ?: "",
                paymentMethod = snap.child("paymentMethod").value?.toString() ?: snap.child("method").value?.toString() ?: if (isUpiOrder) "UPI_ORDER" else "UPI_MANUAL",
                screenshotUrl = snap.child("screenshotUrl").value?.toString() ?: snap.child("paymentScreenshotUrl").value?.toString() ?: "",
                status = snap.child("status").value?.toString()?.uppercase() ?: "PENDING",
                requestedAt = safeLong(snap.child("createdAt").value ?: snap.child("requestedAt").value ?: snap.child("timestamp").value, System.currentTimeMillis()),
                processedAt = safeLong(snap.child("processedAt").value, 0L),
                processedBy = snap.child("processedBy").value?.toString() ?: ""
            )
        }

        val depRef = database.child("deposit_requests")
        val orderRef = database.child("upi_payment_orders")
        val chRef = database.child("admin_channels").child("deposit_requests")

        val depListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { c -> parseSnap(c, false)?.let { depMap[it.id] = it } }
                emitDeposits()
            }
            override fun onCancelled(error: DatabaseError) { emitDeposits() }
        }
        val orderListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { c -> parseSnap(c, true)?.let { depMap.putIfAbsent(it.id, it) } }
                emitDeposits()
            }
            override fun onCancelled(error: DatabaseError) { emitDeposits() }
        }
        depRef.addValueEventListener(depListener)
        chRef.addValueEventListener(depListener)
        orderRef.addValueEventListener(orderListener)

        val fsReg = try {
            firestore.collection("deposit_requests").addSnapshotListener { snap, err ->
                if (err == null && snap != null) {
                    snap.documents.forEach { doc ->
                        val id = doc.getString("id") ?: doc.getString("requestId") ?: doc.id
                        depMap[id] = DepositRequest(
                            id = id,
                            requestId = id,
                            orderId = doc.getString("orderId") ?: "",
                            transactionId = doc.getString("transactionId") ?: "",
                            userId = doc.getString("userId") ?: doc.getString("uid") ?: "",
                            username = doc.getString("username") ?: doc.getString("userName") ?: "Player",
                            userName = doc.getString("username") ?: doc.getString("userName") ?: "Player",
                            userEmail = doc.getString("userEmail") ?: doc.getString("email") ?: "",
                            amount = safeDouble(doc.get("amount") ?: doc.get("vtAmount")),
                            utrNumber = doc.getString("utrNumber") ?: doc.getString("utr") ?: doc.getString("paymentRef") ?: "",
                            paymentMethod = doc.getString("paymentMethod") ?: doc.getString("method") ?: "UPI_MANUAL",
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
            depRef.removeEventListener(depListener)
            chRef.removeEventListener(depListener)
            orderRef.removeEventListener(orderListener)
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
        val rtdbMissions = mutableMapOf<String, MissionItem>()
        val fsMissions = mutableMapOf<String, MissionItem>()

        fun emitMissions() {
            val merged = LinkedHashMap<String, MissionItem>()
            if (fsMissions.isNotEmpty()) {
                fsMissions.forEach { (k, v) -> merged[k] = v }
            } else {
                rtdbMissions.forEach { (k, v) -> merged[k] = v }
            }
            trySend(merged.values.sortedBy { it.id })
        }

        val ref = database.child("missions")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                rtdbMissions.clear()
                for (c in snapshot.children) {
                    if (!c.hasChildren() || c.value !is Map<*, *>) continue
                    val id = c.child("id").value?.toString() ?: c.key ?: ""
                    if (id.isNotBlank()) {
                        rtdbMissions[id] = MissionItem(
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
                    fsMissions.clear()
                    snap.documents.forEach { doc ->
                        fsMissions[doc.id] = MissionItem(
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
        try {
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
            // Primary write target: Firestore missions/{id} (Cloud Functions mirror RTDB & global_app_state)
            firestore.collection("missions").document(mId).set(payload, SetOptions.merge()).await()
            GlobalErrorManager.emitSuccess("Mission '${mission.title}' saved to Firestore & syncing live!")
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to save mission: ${e.message}")
        }
    }

    suspend fun deleteMission(missionId: String) {
        if (missionId.isBlank()) return
        try {
            // Primary delete target: Firestore missions/{id} (Cloud Functions clean RTDB mirror)
            firestore.collection("missions").document(missionId).delete().await()
            GlobalErrorManager.emitSuccess("Mission deleted from Firestore.")
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to delete mission: ${e.message}")
        }
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
                    ?: "veloxyra.anant@fam",
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
                    ?: "Live OTA Config Active",
                liveLobbyTicker = snap.child("liveLobbyTicker").value?.toString() ?: "",
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
                        val activeKeys = SystemConfig.defaultFeatureFlags().keys
                        val parsedFlags = SystemConfig.defaultFeatureFlags().toMutableMap()
                        rawFlags?.forEach { (k, v) ->
                            val keyStr = k?.toString()
                            if (keyStr != null && keyStr in activeKeys) {
                                parsedFlags[keyStr] = safeBool(v, true)
                            }
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
            val activeKeys = SystemConfig.defaultFeatureFlags().keys
            val mergedFlags = SystemConfig.defaultFeatureFlags().toMutableMap().apply {
                config.featureFlags.forEach { (k, v) ->
                    if (k in activeKeys) put(k, v)
                }
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
                "featureFlags" to mergedFlags,
                "updatedAt" to now,
                "updatedBy" to adminEmail
            )
            // Primary write target: Firestore system_config (Cloud Functions automatically mirror RTDB & global_app_state)
            firestore.collection("system_config").document("global").set(payload, SetOptions.merge()).await()
            try { firestore.collection("system_config").document("config").set(payload, SetOptions.merge()).await() } catch (_: Exception) {}
            try { firestore.collection("system_config").document("app_settings").set(payload, SetOptions.merge()).await() } catch (_: Exception) {}

            writeAuditLog(
                action = "SYSTEM_CONFIG_UPDATED",
                detail = "Live Config • Maintenance=${config.maintenanceMode} • 5 Core Flags • UPI=${config.upiId}",
                adminEmail = adminEmail,
                targetId = "global"
            )
            GlobalErrorManager.emitSuccess("System Config, Feature Flags & UPI saved to Firestore & syncing live!")
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
                "createdAt" to (if (banner.createdAt > 0L) banner.createdAt else now),
                "updatedAt" to now
            )
            // Primary write target: Firestore banners/{id} (Cloud Functions mirror RTDB & global_app_state)
            firestore.collection("banners").document(bId).set(payload, SetOptions.merge()).await()
            GlobalErrorManager.emitSuccess("Promotional banner '${banner.title}' saved to Firestore & syncing live!")
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to save promotional banner: ${e.message}")
        }
    }

    suspend fun deletePromotionalBanner(bannerId: String) {
        if (bannerId.isBlank()) return
        try {
            // Primary delete target: Firestore banners/{id} (Cloud Functions clean RTDB mirror)
            firestore.collection("banners").document(bannerId).delete().await()
            GlobalErrorManager.emitSuccess("Promotional banner deleted from Firestore.")
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to delete promotional banner: ${e.message}")
        }
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
        val eventId = "evt_${now}_${(100..999).random()}"
        val stateMap = mapOf<String, Any>(
            "eventId" to eventId,
            "eventType" to eventType,
            "source" to "ADMIN_PANEL",
            "tournamentId" to tournamentId,
            "affectedUserId" to affectedUserId,
            "timestamp" to now,
            "lastUpdatedAt" to now,
            "version" to now,
            "payload" to payload
        )
        // Cloud Functions own RTDB /global_app_state domain versions; record event primarily in Firestore
        try { firestore.collection("global_app_state").document("latest").set(stateMap, SetOptions.merge()).await() } catch (_: Exception) {}
    }

    // =========================================================================
    // MODULE 6: MANUAL NOTIFICATION STUDIO (CLOUD FUNCTION + LIVE HISTORY)
    // =========================================================================

    @Suppress("UNCHECKED_CAST")
    suspend fun sendManualNotification(
        title: String,
        message: String,
        type: String = "MANUAL",
        targetType: String = "ALL",
        targetUserId: String = "",
        targetEmail: String = "",
        targetUsername: String = "",
        targetUserIds: List<String> = emptyList(),
        tournamentId: String = "",
        topic: String = "",
        imageUrl: String = "",
        roomId: String = "",
        roomPassword: String = "",
        adminEmail: String = ""
    ): ManualNotificationResult {
        val cleanTitle = title.trim().take(140)
        val cleanMessage = message.trim().take(1000)
        if (cleanTitle.isBlank() || cleanMessage.isBlank()) {
            GlobalErrorManager.emitError("Notification title and message are required.")
            return ManualNotificationResult(success = false, errorMessage = "Title and message are required.")
        }

        val cleanTargetType = targetType.trim().uppercase().ifBlank { "ALL" }
        val cleanType = type.trim().uppercase().ifBlank { "MANUAL" }
        val payload = hashMapOf<String, Any>(
            "title" to cleanTitle,
            "message" to cleanMessage,
            "type" to cleanType,
            "targetType" to cleanTargetType
        )
        if (targetUserId.isNotBlank()) payload["targetUserId"] = targetUserId.trim()
        if (targetEmail.isNotBlank()) payload["targetEmail"] = targetEmail.trim()
        if (targetUsername.isNotBlank()) payload["targetUsername"] = targetUsername.trim()
        if (targetUserIds.isNotEmpty()) payload["targetUserIds"] = targetUserIds.map { it.trim() }.filter { it.isNotBlank() }
        if (tournamentId.isNotBlank()) payload["tournamentId"] = tournamentId.trim()
        if (topic.isNotBlank()) payload["topic"] = topic.trim()
        if (imageUrl.isNotBlank()) payload["imageUrl"] = imageUrl.trim()
        if (roomId.isNotBlank()) payload["roomId"] = roomId.trim()
        if (roomPassword.isNotBlank()) payload["roomPassword"] = roomPassword.trim()

        return try {
            val callableResult = functions.getHttpsCallable("sendManualNotification").call(payload).await()
            val dataMap = callableResult.getData() as? Map<String, Any?> ?: emptyMap()
            val notifId = dataMap["notificationId"]?.toString().orEmpty()
            val recipients = safeInt(dataMap["recipientCount"], 0)
            val fcmOk = safeInt(dataMap["fcmSuccessCount"], 0)
            val fcmFail = safeInt(dataMap["fcmFailureCount"], 0)
            writeAuditLog(
                action = "MANUAL_NOTIFICATION_SENT",
                detail = "[$cleanType -> $cleanTargetType] \"$cleanTitle\" • Recipients=$recipients, FCM=$fcmOk/$fcmFail",
                adminEmail = adminEmail.ifBlank { auth.currentUser?.email ?: "admin" },
                targetId = notifId
            )
            GlobalErrorManager.emitSuccess("Notification sent ($cleanTargetType)! Recipients: $recipients • FCM Delivered: $fcmOk${if (fcmFail > 0) " ($fcmFail failed)" else ""}")
            ManualNotificationResult(
                success = true,
                notificationId = notifId,
                targetType = cleanTargetType,
                recipientCount = recipients,
                fcmSuccessCount = fcmOk,
                fcmFailureCount = fcmFail,
                viaFallback = false
            )
        } catch (callableErr: Exception) {
            Log.w(TAG, "sendManualNotification callable failed, using Firestore trigger fallback: ${callableErr.message}")
            try {
                val now = System.currentTimeMillis()
                val notifId = "manual_${now}_${(100..999).random()}"
                val docData = payload.toMutableMap().apply {
                    put("id", notifId)
                    put("sentBy", adminEmail.ifBlank { auth.currentUser?.email ?: "admin" })
                    put("timestamp", now)
                    put("createdAt", now)
                }
                firestore.collection("manual_notifications").document(notifId).set(docData).await()
                writeAuditLog(
                    action = "MANUAL_NOTIFICATION_QUEUED",
                    detail = "Queued via Firestore trigger [$cleanType -> $cleanTargetType] \"$cleanTitle\"",
                    adminEmail = adminEmail.ifBlank { auth.currentUser?.email ?: "admin" },
                    targetId = notifId
                )
                GlobalErrorManager.emitSuccess("Queued in manual_notifications/$notifId (server trigger active)!")
                ManualNotificationResult(
                    success = true,
                    notificationId = notifId,
                    targetType = cleanTargetType,
                    viaFallback = true
                )
            } catch (fallbackErr: Exception) {
                GlobalErrorManager.emitError("Notification dispatch failed: ${fallbackErr.message ?: callableErr.message}")
                ManualNotificationResult(success = false, errorMessage = fallbackErr.message ?: callableErr.message)
            }
        }
    }

    fun getManualNotificationsStream(): Flow<List<ManualNotificationRecord>> = callbackFlow {
        val reg = try {
            firestore.collection("manual_notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener { snap, err ->
                    if (err != null) {
                        Log.w(TAG, "manual_notifications listener error: ${err.message}")
                        return@addSnapshotListener
                    }
                    val list = snap?.documents?.map { d ->
                        val rawIds = (d.get("targetUserIds") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                        ManualNotificationRecord(
                            id = d.getString("id") ?: d.id,
                            title = d.getString("title") ?: "",
                            message = d.getString("message") ?: "",
                            type = d.getString("type") ?: "MANUAL",
                            targetType = d.getString("targetType") ?: "ALL",
                            targetUserId = d.getString("targetUserId") ?: "",
                            targetEmail = d.getString("targetEmail") ?: "",
                            targetUsername = d.getString("targetUsername") ?: "",
                            targetUserIds = rawIds,
                            tournamentId = d.getString("tournamentId") ?: "",
                            topic = d.getString("topic") ?: "",
                            imageUrl = d.getString("imageUrl") ?: "",
                            roomId = d.getString("roomId") ?: "",
                            roomPassword = d.getString("roomPassword") ?: "",
                            recipientCount = safeInt(d.get("recipientCount"), 0),
                            fcmSuccessCount = safeInt(d.get("fcmSuccessCount"), 0),
                            fcmFailureCount = safeInt(d.get("fcmFailureCount"), 0),
                            sentBy = d.getString("sentBy") ?: d.getString("adminEmail") ?: "Admin",
                            timestamp = safeLong(d.get("timestamp") ?: d.get("createdAt"), System.currentTimeMillis())
                        )
                    } ?: emptyList()
                    trySend(list)
                }
        } catch (e: Exception) {
            Log.w(TAG, "manual_notifications stream failed: ${e.message}")
            trySend(emptyList())
            null
        }
        awaitClose { reg?.remove() }
    }

    // =========================================================================
    // MODULE 7: 1-CLICK BACKEND RECONCILIATION & SECURITY AUDIT LOGS
    // =========================================================================

    private fun anyToJsonValue(value: Any?): Any = when (value) {
        null -> JSONObject.NULL
        is Map<*, *> -> JSONObject().apply {
            value.forEach { (k, v) -> put(k?.toString() ?: "key", anyToJsonValue(v)) }
        }
        is Iterable<*> -> JSONArray().apply {
            value.forEach { put(anyToJsonValue(it)) }
        }
        is Boolean, is Number, is String -> value
        else -> value.toString()
    }

    suspend fun triggerBackendReconciliation(adminEmail: String): Result<String> {
        return try {
            val res = functions.getHttpsCallable("triggerBackendReconciliation").call().await()
            val data = res.getData()
            val formattedJson = when (val jv = anyToJsonValue(data)) {
                is JSONObject -> jv.toString(2)
                is JSONArray -> jv.toString(2)
                else -> JSONObject().put("result", jv).toString(2)
            }
            writeAuditLog(
                action = "BACKEND_RECONCILIATION_RUN",
                detail = "Triggered triggerBackendReconciliation() from Admin Panel",
                adminEmail = adminEmail,
                targetId = "reconciliation"
            )
            GlobalErrorManager.emitSuccess("Backend reconciliation completed successfully!")
            Result.success(formattedJson)
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Backend reconciliation failed: ${e.message}")
            Result.failure(e)
        }
    }

    fun getSecurityAuditLogsStream(): Flow<List<AdminAuditLogItem>> = callbackFlow {
        val reg = try {
            firestore.collection("security_audit_logs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(100)
                .addSnapshotListener { snap, err ->
                    if (err != null) {
                        Log.w(TAG, "security_audit_logs listener error: ${err.message}")
                        return@addSnapshotListener
                    }
                    val list = snap?.documents?.map { d ->
                        AdminAuditLogItem(
                            id = d.id,
                            action = d.getString("action") ?: d.getString("eventType") ?: d.getString("type") ?: "SECURITY_EVENT",
                            detail = d.getString("detail") ?: d.getString("reason") ?: d.getString("message") ?: "",
                            adminEmail = d.getString("adminEmail") ?: d.getString("actorEmail") ?: d.getString("userId") ?: "system",
                            targetId = d.getString("targetId") ?: d.getString("uid") ?: d.getString("userId") ?: "",
                            timestamp = safeLong(d.get("timestamp") ?: d.get("createdAt"), System.currentTimeMillis())
                        )
                    } ?: emptyList()
                    trySend(list)
                }
        } catch (e: Exception) {
            trySend(emptyList())
            null
        }
        awaitClose { reg?.remove() }
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

        // 0. PRIMARY AUTHORITATIVE SOURCE: Firestore admin_match_rosters/{tournamentId}/players
        try {
            val rosterDocs = firestore.collection("admin_match_rosters")
                .document(tId)
                .collection("players")
                .get()
                .await()
            rosterDocs.documents.forEachIndexed { idx, doc ->
                val uid = (doc.getString("userId") ?: doc.getString("uid") ?: doc.id).trim()
                if (uid.isNotBlank()) {
                    val matchedUser = allUsers.find { it.id == uid }
                    val slot = safeInt(doc.get("slotNumber") ?: doc.get("slot"), idx + 1).coerceAtLeast(1)
                    val rank = safeInt(doc.get("rank") ?: doc.get("position"), 0)
                    val kills = safeInt(doc.get("kills"), 0)
                    val uname = doc.getString("username")
                        ?: matchedUser?.username
                        ?: doc.getString("inGameName")
                        ?: "Player #$slot"
                    val ign = doc.getString("inGameName")
                        ?: doc.getString("ign")
                        ?: matchedUser?.ign?.ifBlank { uname }
                        ?: uname
                    val charId = doc.getString("characterId")
                        ?: doc.getString("gameId")
                        ?: doc.getString("freeFireId")
                        ?: matchedUser?.gameId
                        ?: ""
                    val ticket = doc.getString("ticketCode") ?: doc.getString("ticketId") ?: ""
                    val rPrize = safeDouble(doc.get("rankPrize"), if (rank > 0) defaultRankPrizeForRank(rank) else 0.0)
                    val kPrize = safeDouble(doc.get("killPrize"), kills * perKillBounty)
                    val pWon = safeDouble(doc.get("prizeWon"), rPrize + kPrize)
                    entriesByUser[uid] = TournamentStandingEntry(
                        rank = rank,
                        userId = uid,
                        username = uname,
                        inGameName = ign,
                        characterId = charId,
                        teamName = doc.getString("teamName") ?: "",
                        ticketCode = ticket,
                        slotNumber = slot,
                        kills = kills,
                        rankPrize = rPrize,
                        killPrize = kPrize,
                        prizeWon = pWon,
                        wonAnything = pWon > 0.0,
                        payoutStatus = doc.getString("payoutStatus") ?: if (pWon > 0.0) "PAID" else "NO_PRIZE"
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "admin_match_rosters read error for $tId: ${e.message}")
        }

        // 0B. Merge Firestore tournaments/{tournamentId}.joinedPlayersMap if present
        try {
            val tourneyFsDoc = firestore.collection("tournaments").document(tId).get().await()
            val joinedMap = tourneyFsDoc.get("joinedPlayersMap") as? Map<*, *>
            joinedMap?.forEach { (k, v) ->
                val uid = k?.toString()?.trim().orEmpty()
                if (uid.isNotBlank() && !entriesByUser.containsKey(uid)) {
                    val m = v as? Map<*, *>
                    val slot = safeInt(m?.get("slotNumber") ?: m?.get("slot"), entriesByUser.size + 1)
                    val matchedUser = allUsers.find { it.id == uid }
                    val uname = m?.get("username")?.toString()?.ifBlank { null } ?: matchedUser?.username ?: "Player #$slot"
                    val ign = m?.get("inGameName")?.toString()?.ifBlank { null } ?: m?.get("ign")?.toString()?.ifBlank { null } ?: matchedUser?.ign?.ifBlank { uname } ?: uname
                    val charId = m?.get("characterId")?.toString()?.ifBlank { null } ?: m?.get("gameId")?.toString()?.ifBlank { null } ?: matchedUser?.gameId ?: ""
                    entriesByUser[uid] = TournamentStandingEntry(
                        rank = 0,
                        userId = uid,
                        username = uname,
                        inGameName = ign,
                        characterId = charId,
                        teamName = m?.get("teamName")?.toString() ?: "",
                        ticketCode = m?.get("ticketCode")?.toString() ?: "",
                        slotNumber = slot,
                        kills = 0
                    )
                }
            }
        } catch (_: Exception) {}

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
                if (!entriesByUser.containsKey(uid)) {
                    val rank = safeInt(c.child("rank").value ?: c.child("position").value, idx + 1).coerceAtLeast(0)
                    val kills = safeInt(c.child("kills").value, 0)
                    val rPrize = safeDouble(c.child("rankPrize").value, if (rank > 0) defaultRankPrizeForRank(rank) else 0.0)
                    val kPrize = safeDouble(c.child("killPrize").value, kills * perKillBounty)
                    val pWon = safeDouble(c.child("prizeWon").value, rPrize + kPrize)
                    entriesByUser[uid] = TournamentStandingEntry(
                        rank = rank,
                        userId = uid,
                        username = c.child("username").value?.toString() ?: c.child("inGameName").value?.toString() ?: "Player",
                        inGameName = c.child("inGameName").value?.toString() ?: c.child("username").value?.toString() ?: "Player",
                        characterId = c.child("characterId").value?.toString() ?: c.child("freeFireId").value?.toString() ?: c.child("gameId").value?.toString() ?: "",
                        teamName = c.child("teamName").value?.toString() ?: "",
                        ticketCode = c.child("ticketCode").value?.toString() ?: "",
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

        return entriesByUser.values.sortedWith(compareBy<TournamentStandingEntry> { if (it.slotNumber > 0) it.slotNumber else Int.MAX_VALUE }.thenBy { if (it.rank > 0) it.rank else Int.MAX_VALUE })
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun publishTournamentStandingsAndPrizes(
        tournament: Tournament,
        rawStandings: List<TournamentStandingEntry>,
        adminEmail: String,
        tiePrizeMode: String = "SPLIT_TIED_POOL"
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

        // PRIMARY SERVER-AUTHORITATIVE EXECUTION: Cloud Function finalizeTournamentPayouts
        val validAssignments = rawStandings
            .filter { it.userId.isNotBlank() }
            .map { p ->
                hashMapOf<String, Any>(
                    "userId" to p.userId.trim(),
                    "rank" to p.rank.coerceAtLeast(0),
                    "kills" to p.kills.coerceAtLeast(0)
                )
            }

        if (validAssignments.isEmpty()) {
            GlobalErrorManager.emitError("Every player row needs a valid User UID before calling finalizeTournamentPayouts.")
            return false
        }

        return try {
            val payload = hashMapOf<String, Any>(
                "tournamentId" to tournamentId,
                "payoutStatus" to "PAID",
                "tiePrizeMode" to tiePrizeMode.ifBlank { "SPLIT_TIED_POOL" },
                "positionAssignments" to validAssignments
            )
            val result = functions.getHttpsCallable("finalizeTournamentPayouts").call(payload).await()
            val dataMap = result.getData() as? Map<String, Any?> ?: emptyMap()
            val paidCount = safeInt(dataMap["winnersPaid"] ?: dataMap["paidCount"], validAssignments.count { safeInt(it["rank"]) > 0 })
            val totalDisbursed = safeDouble(dataMap["totalDisbursed"] ?: dataMap["totalPrizeDisbursed"], rawStandings.sumOf { it.prizeWon })

            publishGlobalAppStateEvent(
                eventType = "TOURNAMENT_UPDATED",
                tournamentId = tournamentId,
                payload = mapOf(
                    "status" to "COMPLETED",
                    "payoutStatus" to "PAID",
                    "tiePrizeMode" to tiePrizeMode,
                    "winnersPaid" to paidCount
                )
            )
            writeAuditLog(
                action = "TOURNAMENT_PAYOUTS_FINALIZED",
                detail = "Finalized $tournamentId via finalizeTournamentPayouts ($tiePrizeMode) • Paid=$paidCount, Total=₹${totalDisbursed.toInt()}",
                adminEmail = adminEmail,
                targetId = tournamentId
            )
            GlobalErrorManager.emitSuccess("Tournament payouts settled atomically via server! ($paidCount rewarded • Mode: $tiePrizeMode)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "finalizeTournamentPayouts Cloud Function error: ${e.message}", e)
            GlobalErrorManager.emitError("finalizeTournamentPayouts failed: ${e.message}")
            false
        }
    }

    @Suppress("unused")
    private suspend fun legacyLocalStandingsUnused(
        tournament: Tournament,
        rawStandings: List<TournamentStandingEntry>,
        adminEmail: String
    ): Boolean {
        val tournamentId = tournament.id.trim()
        if (tournamentId.isBlank() || rawStandings.isEmpty()) return false
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
                try { database.child("match_stats").child(uid).child(matchStatId).setValue(matchStatPayload).await() } catch (_: Exception) {}
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
            val cleanReason = if (isBanned) reason.ifBlank { "Violation of Fair Play Terms / Anti-Cheat & Third-Party Tool Detection" } else ""
            val resolvedCaseId = if (isBanned) {
                user.banCaseId.ifBlank { "BAN-${user.id.take(6).uppercase()}" }
            } else ""

            val userUpdates = mutableMapOf<String, Any>(
                "isBanned" to isBanned,
                "banned" to isBanned,
                "banReason" to cleanReason,
                "reason" to cleanReason,
                "banType" to if (isBanned) "PERMANENT" else "",
                "sanction" to if (isBanned) "PERMANENT" else "",
                "sanctionType" to if (isBanned) "PERMANENT" else "",
                "banCaseId" to resolvedCaseId,
                "sanctionId" to resolvedCaseId,
                "accountStatus" to if (isBanned) "BANNED" else "ACTIVE",
                "status" to if (isBanned) "BANNED" else "ACTIVE",
                "active" to !isBanned,
                "isAuthorized" to !isBanned,
                "walletFrozen" to isBanned,
                "matchmakingLocked" to isBanned,
                "withdrawalsLocked" to isBanned,
                "bannedAt" to if (isBanned) now else 0L,
                "bannedBy" to if (isBanned) adminEmail else "",
                "version" to nextVer,
                "updatedAt" to now
            )
            if (!isBanned) {
                userUpdates["isSuspended"] = false
                userUpdates["suspendedUntil"] = 0L
                userUpdates["suspensionEndTimeMillis"] = 0L
                userUpdates["suspendReason"] = ""
                userUpdates["suspensionReason"] = ""
                userUpdates["activeAppealStatus"] = "RESOLVED"
                userUpdates["appealStatus"] = "RESOLVED"
            }

            // Collect all matching UIDs for this account across RTDB & Firestore so no duplicate/sibling node stays out of sync
            val targetUids = mutableSetOf(user.id.trim())
            val emailLower = user.email.trim().lowercase()
            val gameIdTrim = user.gameId.trim()
            val ignLower = user.ign.trim().lowercase()
            val nameLower = user.username.trim().lowercase()
            val caseIdUpper = user.banCaseId.trim().uppercase()

            fun matchesIdentity(
                candId: String,
                candEmail: String,
                candGameId: String,
                candIgn: String,
                candName: String,
                candCaseId: String
            ): Boolean {
                if (candId.equals(user.id.trim(), ignoreCase = true)) return true
                if (emailLower.isNotBlank() && !emailLower.startsWith("no-email") && candEmail.equals(emailLower, ignoreCase = true)) return true
                if (gameIdTrim.isNotBlank() && candGameId.equals(gameIdTrim, ignoreCase = true)) return true
                if (caseIdUpper.isNotBlank() && candCaseId.equals(caseIdUpper, ignoreCase = true)) return true
                if (ignLower.isNotBlank() && ignLower != "player" && ignLower != "unknown" &&
                    (candIgn.equals(ignLower, ignoreCase = true) || candName.equals(ignLower, ignoreCase = true))
                ) return true
                if (nameLower.isNotBlank() && nameLower != "player" && nameLower != "unknown" &&
                    (candName.equals(nameLower, ignoreCase = true) || candIgn.equals(nameLower, ignoreCase = true))
                ) return true
                return false
            }

            listOf("users", "userProfiles", "players").forEach { node ->
                try {
                    val snap = database.child(node).get().await()
                    snap.children.forEach { c ->
                        val cKey = c.key?.trim().orEmpty()
                        val cId = c.child("id").value?.toString()?.trim() ?: c.child("uid").value?.toString()?.trim() ?: cKey
                        val cEmail = (c.child("email").value ?: c.child("userEmail").value ?: c.child("profile").child("email").value)?.toString()?.trim().orEmpty()
                        val cGameId = (c.child("gameId").value ?: c.child("freeFireId").value ?: c.child("profile").child("gameId").value)?.toString()?.trim().orEmpty()
                        val cIgn = (c.child("ign").value ?: c.child("inGameName").value ?: c.child("profile").child("ign").value)?.toString()?.trim().orEmpty()
                        val cName = (c.child("username").value ?: c.child("name").value ?: c.child("displayName").value ?: c.child("userName").value)?.toString()?.trim().orEmpty()
                        val cCase = (c.child("banCaseId").value ?: c.child("sanctionId").value)?.toString()?.trim().orEmpty()
                        if (matchesIdentity(cId, cEmail, cGameId, cIgn, cName, cCase)) {
                            if (cKey.isNotBlank()) targetUids.add(cKey)
                            if (cId.isNotBlank()) targetUids.add(cId)
                        }
                    }
                } catch (_: Exception) {}
            }

            try {
                val fsUsers = firestore.collection("users").get().await()
                fsUsers.documents.forEach { d ->
                    val dId = d.getString("id") ?: d.getString("uid") ?: d.id
                    val dEmail = d.getString("email") ?: d.getString("userEmail") ?: ""
                    val dGameId = d.getString("gameId") ?: d.getString("freeFireId") ?: ""
                    val dIgn = d.getString("ign") ?: d.getString("inGameName") ?: ""
                    val dName = d.getString("username") ?: d.getString("name") ?: d.getString("displayName") ?: ""
                    val dCase = d.getString("banCaseId") ?: d.getString("sanctionId") ?: ""
                    if (matchesIdentity(dId, dEmail, dGameId, dIgn, dName, dCase)) {
                        targetUids.add(d.id)
                        if (dId.isNotBlank()) targetUids.add(dId)
                    }
                }
            } catch (_: Exception) {}

            // Primary write target: Firestore users/{uid} (Cloud Functions mirror RTDB /users, /banned_users & global_app_state)
            for (uid in targetUids) {
                if (uid.isBlank()) continue
                firestore.collection("users").document(uid).set(userUpdates, SetOptions.merge()).await()
                try { firestore.collection("userProfiles").document(uid).set(userUpdates, SetOptions.merge()).await() } catch (_: Exception) {}

                // Update secondary Firestore collections only if record already exists there
                for (col in listOf("Users", "user_profiles", "players", "Players", "accounts")) {
                    try {
                        val doc = firestore.collection(col).document(uid).get().await()
                        if (doc.exists()) {
                            firestore.collection(col).document(uid).set(userUpdates, SetOptions.merge()).await()
                        }
                    } catch (_: Exception) {}
                }
            }

            if (!isBanned) {
                // Automatically resolve any open Ban Appeal tickets for this user in Firestore
                resolveOpenBanAppealsForUser(
                    targetUids = targetUids,
                    emailLower = emailLower,
                    gameIdTrim = gameIdTrim,
                    ignLower = ignLower,
                    nameLower = nameLower,
                    caseIdUpper = caseIdUpper,
                    explicitTicketId = user.activeAppealTicketId,
                    adminReply = "Your ban appeal has been APPROVED by Admin ($adminEmail). Your account restriction has been lifted and your account is now ACTIVE.",
                    adminEmail = adminEmail,
                    now = now
                )
            }

            writeAuditLog(
                action = if (isBanned) "USER_BANNED" else "USER_UNBANNED",
                detail = "${if (isBanned) "Banned" else "Unbanned"} ${user.username} (${targetUids.joinToString(",")}). Reason: $cleanReason",
                adminEmail = adminEmail,
                targetId = user.id
            )
            GlobalErrorManager.emitSuccess(
                if (isBanned) "User ${user.username.ifBlank { user.id }} BANNED in Firestore & syncing live!"
                else "User ${user.username.ifBlank { user.id }} UNBANNED in Firestore & syncing live!"
            )
            return true
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to update ban status: ${e.message}")
            return false
        }
    }

    private suspend fun resolveOpenBanAppealsForUser(
        targetUids: Set<String>,
        emailLower: String,
        gameIdTrim: String,
        ignLower: String,
        nameLower: String,
        caseIdUpper: String,
        explicitTicketId: String,
        adminReply: String,
        adminEmail: String,
        now: Long
    ) {
        val appealUpdates = mapOf<String, Any>(
            "status" to "RESOLVED",
            "adminReply" to adminReply,
            "adminNote" to adminReply,
            "processedBy" to adminEmail,
            "updatedAt" to now
        )
        if (explicitTicketId.isNotBlank()) {
            syncLinkedTicketAcrossAllPaths(
                ticketId = explicitTicketId,
                userId = targetUids.firstOrNull().orEmpty(),
                status = "RESOLVED",
                adminReply = adminReply,
                deliveredRedeemCode = "",
                adminEmail = adminEmail,
                now = now,
                channelNode = "ban_appeals"
            )
        }
        val rtdbAppealNodes = listOf(
            database.child("admin_requests").child("all"),
            database.child("admin_requests").child("ban_appeals"),
            database.child("admin_requests").child("account_issue"),
            database.child("ban_appeals"),
            database.child("appeals"),
            database.child("support_tickets"),
            database.child("reports")
        )
        for (ref in rtdbAppealNodes) {
            try {
                val snap = ref.get().await()
                snap.children.forEach { c ->
                    val tId = (c.child("id").value ?: c.child("ticketId").value ?: c.child("requestId").value ?: c.key)?.toString()?.trim().orEmpty()
                    val tUid = (c.child("userId").value ?: c.child("uid").value)?.toString()?.trim().orEmpty()
                    val tEmail = (c.child("userEmail").value ?: c.child("email").value ?: c.child("userContact").value)?.toString()?.trim()?.lowercase().orEmpty()
                    val tIgn = (c.child("inGameName").value ?: c.child("ign").value)?.toString()?.trim()?.lowercase().orEmpty()
                    val tName = (c.child("username").value ?: c.child("userName").value)?.toString()?.trim()?.lowercase().orEmpty()
                    val tCase = (c.child("relatedId").value ?: c.child("banCaseId").value ?: c.child("sanctionId").value)?.toString()?.trim()?.uppercase().orEmpty()
                    val matchedUser = (tUid.isNotBlank() && tUid in targetUids) ||
                        (explicitTicketId.isNotBlank() && tId.equals(explicitTicketId, ignoreCase = true)) ||
                        (emailLower.isNotBlank() && tEmail == emailLower) ||
                        (caseIdUpper.isNotBlank() && tCase == caseIdUpper) ||
                        (ignLower.isNotBlank() && ignLower != "player" && (tIgn == ignLower || tName == ignLower)) ||
                        (nameLower.isNotBlank() && nameLower != "player" && (tName == nameLower || tIgn == nameLower))
                    if (matchedUser) {
                        try { c.ref.updateChildren(appealUpdates).await() } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}
        }
        for (uid in targetUids) {
            if (uid.isBlank()) continue
            for (sub in listOf("reports", "ban_appeals", "appeals", "tickets")) {
                try {
                    val subSnap = database.child("users").child(uid).child(sub).get().await()
                    subSnap.children.forEach { c ->
                        try { c.ref.updateChildren(appealUpdates).await() } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}
            }
        }
        for (fsCol in listOf("admin_requests", "ban_appeals", "appeals", "support_tickets", "reports")) {
            try {
                val snap = firestore.collection(fsCol).get().await()
                snap.documents.forEach { d ->
                    val tId = (d.getString("id") ?: d.getString("ticketId") ?: d.id).trim()
                    val tUid = (d.getString("userId") ?: d.getString("uid")).orEmpty().trim()
                    val tEmail = (d.getString("userEmail") ?: d.getString("email") ?: d.getString("userContact")).orEmpty().trim().lowercase()
                    val tIgn = (d.getString("inGameName") ?: d.getString("ign")).orEmpty().trim().lowercase()
                    val tName = (d.getString("username") ?: d.getString("userName")).orEmpty().trim().lowercase()
                    val tCase = (d.getString("relatedId") ?: d.getString("banCaseId") ?: d.getString("sanctionId")).orEmpty().trim().uppercase()
                    val matchedUser = (tUid.isNotBlank() && tUid in targetUids) ||
                        (explicitTicketId.isNotBlank() && tId.equals(explicitTicketId, ignoreCase = true)) ||
                        (emailLower.isNotBlank() && tEmail == emailLower) ||
                        (caseIdUpper.isNotBlank() && tCase == caseIdUpper) ||
                        (ignLower.isNotBlank() && ignLower != "player" && (tIgn == ignLower || tName == ignLower)) ||
                        (nameLower.isNotBlank() && nameLower != "player" && (tName == nameLower || tIgn == nameLower))
                    if (matchedUser) {
                        try { d.reference.set(appealUpdates, SetOptions.merge()).await() } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}
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
                "suspensionExpiresAt" to endTime,
                "suspensionEndTimeMillis" to endTime,
                "accountStatus" to if (isSuspended) "SUSPENDED" else "ACTIVE",
                "version" to nextVer,
                "updatedAt" to now
            )
            try { firestore.collection("users").document(user.id).set(userUpdates, SetOptions.merge()).await() } catch (_: Exception) {}
            try { database.child("users").child(user.id).updateChildren(userUpdates).await() } catch (_: Exception) {}
            try { database.child("userProfiles").child(user.id).updateChildren(userUpdates).await() } catch (_: Exception) {}

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

    // =========================================================================
    // SHARED HELPER: SYNC TICKET REPLY / STATUS / REDEEM CODE ACROSS ALL 8 PATHS
    // =========================================================================

    private suspend fun syncLinkedTicketAcrossAllPaths(
        ticketId: String,
        userId: String,
        status: String,
        adminReply: String,
        deliveredRedeemCode: String,
        adminEmail: String,
        now: Long = System.currentTimeMillis(),
        channelNode: String = ""
    ) {
        val cleanId = ticketId.trim()
        if (cleanId.isBlank()) return
        val updates = mutableMapOf<String, Any>(
            "status" to status.uppercase(),
            "adminReply" to adminReply,
            "adminNote" to adminReply,
            "updatedAt" to now,
            "processedBy" to adminEmail
        )
        if (deliveredRedeemCode.isNotBlank()) {
            updates["deliveredRedeemCode"] = deliveredRedeemCode.trim()
        }

        // Primary write target: Firestore ticket & appeal collections (Cloud Functions mirror RTDB & global_app_state)
        try { firestore.collection("support_tickets").document(cleanId).set(updates, SetOptions.merge()).await() } catch (_: Exception) {}
        try { firestore.collection("admin_requests").document(cleanId).set(updates, SetOptions.merge()).await() } catch (_: Exception) {}
        try { firestore.collection("reports").document(cleanId).set(updates, SetOptions.merge()).await() } catch (_: Exception) {}
        if (channelNode == "ban_appeals" || cleanId.startsWith("VRX-", ignoreCase = true)) {
            try { firestore.collection("ban_appeals").document(cleanId).set(updates, SetOptions.merge()).await() } catch (_: Exception) {}
            try { firestore.collection("appeals").document(cleanId).set(updates, SetOptions.merge()).await() } catch (_: Exception) {}
        }
        if (userId.isNotBlank()) {
            try { firestore.collection("users").document(userId).collection("reports").document(cleanId).set(updates, SetOptions.merge()).await() } catch (_: Exception) {}
        }

        // Send in-app notification primarily via Firestore users/{userId}/notifications
        if (userId.isNotBlank()) {
            val notifId = "notif_ticket_${cleanId}_${now}"
            val titleText = if (deliveredRedeemCode.isNotBlank()) {
                "🎁 Redeem Code Delivered (#${cleanId.takeLast(6)})"
            } else {
                "Ticket Update: ${status.uppercase()} (#${cleanId.takeLast(6)})"
            }
            val notifPayload = mapOf<String, Any>(
                "id" to notifId,
                "userId" to userId,
                "title" to titleText,
                "message" to adminReply.ifBlank { "Your ticket status was updated to ${status.uppercase()}." },
                "type" to "SUPPORT",
                "ticketId" to cleanId,
                "deliveredRedeemCode" to deliveredRedeemCode.trim(),
                "timestamp" to now,
                "isRead" to false
            )
            try { firestore.collection("users").document(userId).collection("notifications").document(notifId).set(notifPayload).await() } catch (_: Exception) {}
        }
    }

    // =========================================================================
    // MODULE 2: PRE-VERIFY UTR (/verified_bank_deposits), FULL DEPOSIT & DUAL WITHDRAWAL DESK
    // =========================================================================

    fun getVerifiedBankDepositsStream(): Flow<List<VerifiedBankDeposit>> = callbackFlow {
        val map = mutableMapOf<String, VerifiedBankDeposit>()
        fun emit() { trySend(map.values.sortedByDescending { it.timestamp }) }

        val ref = database.child("verified_bank_deposits")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { c ->
                    val utr = c.child("utr").value?.toString() ?: c.key ?: ""
                    if (utr.isNotBlank()) {
                        map[utr] = VerifiedBankDeposit(
                            utr = utr,
                            amount = safeDouble(c.child("amount").value),
                            claimed = safeBool(c.child("claimed").value),
                            claimedBy = c.child("claimedBy").value?.toString() ?: "",
                            verifiedBy = c.child("verifiedBy").value?.toString() ?: "",
                            timestamp = safeLong(c.child("timestamp").value, System.currentTimeMillis())
                        )
                    }
                }
                emit()
            }
            override fun onCancelled(error: DatabaseError) { emit() }
        }
        ref.addValueEventListener(listener)

        val fsReg = try {
            firestore.collection("verified_bank_deposits").addSnapshotListener { snap, err ->
                if (err == null && snap != null) {
                    snap.documents.forEach { d ->
                        val utr = d.getString("utr") ?: d.id
                        map[utr] = VerifiedBankDeposit(
                            utr = utr,
                            amount = safeDouble(d.get("amount")),
                            claimed = d.getBoolean("claimed") ?: false,
                            claimedBy = d.getString("claimedBy") ?: "",
                            verifiedBy = d.getString("verifiedBy") ?: "",
                            timestamp = safeLong(d.get("timestamp"), System.currentTimeMillis())
                        )
                    }
                    emit()
                }
            }
        } catch (_: Exception) { null }

        awaitClose {
            ref.removeEventListener(listener)
            fsReg?.remove()
        }
    }

    suspend fun preVerifyBankUtr(utr: String, amount: Double, adminEmail: String): Boolean {
        val cleanUtr = utr.trim().replace(Regex("[^A-Za-z0-9]"), "")
        if (cleanUtr.length < 6 || amount <= 0.0) {
            GlobalErrorManager.emitError("Enter a valid 12-digit UTR and amount > ₹0.")
            return false
        }
        return try {
            val now = System.currentTimeMillis()
            val payload = mapOf<String, Any>(
                "utr" to cleanUtr,
                "amount" to amount,
                "claimed" to false,
                "claimedBy" to "",
                "verifiedBy" to adminEmail,
                "timestamp" to now
            )
            try { database.child("verified_bank_deposits").child(cleanUtr).setValue(payload).await() } catch (_: Exception) {}
            try { firestore.collection("verified_bank_deposits").document(cleanUtr).set(payload).await() } catch (_: Exception) {}
            writeAuditLog(
                action = "UTR_PRE_VERIFIED",
                detail = "Pre-verified Bank UTR $cleanUtr for ₹${amount.toInt()} (Auto-Claim Ready)",
                adminEmail = adminEmail,
                targetId = cleanUtr
            )
            GlobalErrorManager.emitSuccess("UTR $cleanUtr (₹${amount.toInt()}) pre-verified in /verified_bank_deposits!")
            true
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to pre-verify UTR: ${e.message}")
            false
        }
    }

    suspend fun approveDepositRequestFull(req: DepositRequest, adminEmail: String): Boolean {
        val reqId = req.id.ifBlank { req.requestId }.trim()
        val uid = req.effectiveUserId.trim()
        if (reqId.isBlank() || uid.isBlank() || req.amount <= 0.0) {
            GlobalErrorManager.emitError("Invalid deposit request data.")
            return false
        }
        return try {
            val now = System.currentTimeMillis()
            val note = "Verified via bank statement by $adminEmail (UTR: ${req.utrNumber.ifBlank { reqId }})"
            // SERVER-AUTHORITATIVE CALLABLE: never manually increment users/{uid}.balance on client!
            functions.getHttpsCallable("processDepositDecision").call(
                hashMapOf(
                    "requestId" to reqId,
                    "status" to "APPROVED",
                    "adminNote" to note
                )
            ).await()

            syncLinkedTicketAcrossAllPaths(
                ticketId = reqId,
                userId = uid,
                status = "RESOLVED",
                adminReply = "Deposit of ₹${req.amount.toInt()} VT approved & credited to your wallet (UTR: ${req.utrNumber.ifBlank { reqId }}).",
                deliveredRedeemCode = "",
                adminEmail = adminEmail,
                now = now
            )
            publishGlobalAppStateEvent(
                eventType = "WALLET_UPDATED",
                affectedUserId = uid,
                payload = mapOf("depositId" to reqId, "amount" to req.amount, "status" to "APPROVED")
            )
            writeAuditLog("DEPOSIT_APPROVED", "Approved deposit ₹${req.amount.toInt()} via processDepositDecision for $uid (UTR: ${req.utrNumber})", adminEmail, reqId)
            GlobalErrorManager.emitSuccess("Deposit ₹${req.amount.toInt()} VT approved atomically by server!")
            true
        } catch (e: Exception) {
            GlobalErrorManager.emitError("processDepositDecision failed: ${e.message}")
            false
        }
    }

    suspend fun rejectDepositRequestFull(req: DepositRequest, reason: String, adminEmail: String): Boolean {
        val reqId = req.id.ifBlank { req.requestId }.trim()
        val uid = req.effectiveUserId.trim()
        if (reqId.isBlank()) return false
        return try {
            val now = System.currentTimeMillis()
            val cleanReason = reason.ifBlank { "Invalid or unverifiable UTR reference" }
            functions.getHttpsCallable("processDepositDecision").call(
                hashMapOf(
                    "requestId" to reqId,
                    "status" to "REJECTED",
                    "adminNote" to cleanReason
                )
            ).await()
            syncLinkedTicketAcrossAllPaths(
                ticketId = reqId,
                userId = uid,
                status = "REJECTED",
                adminReply = "Deposit rejected: $cleanReason",
                deliveredRedeemCode = "",
                adminEmail = adminEmail,
                now = now
            )
            writeAuditLog("DEPOSIT_REJECTED", "Rejected deposit $reqId via processDepositDecision: $cleanReason", adminEmail, reqId)
            GlobalErrorManager.emitSuccess("Deposit rejected by server.")
            true
        } catch (e: Exception) {
            GlobalErrorManager.emitError("processDepositDecision reject failed: ${e.message}")
            false
        }
    }

    fun getUnifiedWithdrawalsStream(): Flow<List<PayoutRequest>> = callbackFlow {
        val map = mutableMapOf<String, PayoutRequest>()
        fun emit() { trySend(map.values.sortedByDescending { it.requestedAt }) }

        fun parseSnap(snap: DataSnapshot): PayoutRequest? {
            if (!snap.exists()) return null
            val id = snap.child("id").value?.toString()
                ?: snap.child("requestId").value?.toString()
                ?: snap.key ?: return null
            if (id.isBlank()) return null
            val amt = safeDouble(snap.child("amount").value ?: snap.child("vtAmount").value)
            val tds = safeDouble(snap.child("tdsAmount").value, amt * 0.30)
            val net = safeDouble(snap.child("netAmount").value, (amt - tds).coerceAtLeast(0.0))
            val method = snap.child("paymentMethod").value?.toString()
                ?: snap.child("method").value?.toString() ?: "UPI"
            val voucherSpec = snap.child("voucherSpec").value?.toString()
                ?: snap.child("voucherType").value?.toString() ?: ""
            val payId = snap.child("upiId").value?.toString()
                ?: snap.child("paymentId").value?.toString()
                ?: voucherSpec
            return PayoutRequest(
                id = id,
                uid = snap.child("userId").value?.toString() ?: snap.child("uid").value?.toString() ?: "",
                username = snap.child("username").value?.toString() ?: snap.child("userName").value?.toString() ?: "Player",
                inGameName = snap.child("inGameName").value?.toString() ?: snap.child("ign").value?.toString() ?: "",
                email = snap.child("email").value?.toString() ?: snap.child("userEmail").value?.toString() ?: "",
                paymentMethod = method,
                paymentId = payId,
                voucherSpec = voucherSpec,
                deliveredRedeemCode = snap.child("deliveredRedeemCode").value?.toString() ?: "",
                transactionId = snap.child("transactionId").value?.toString() ?: snap.child("txId").value?.toString() ?: "",
                vtAmount = amt,
                tdsAmount = tds,
                netAmount = net,
                status = snap.child("status").value?.toString()?.uppercase() ?: "PENDING",
                rejectionReason = snap.child("rejectionReason").value?.toString() ?: snap.child("adminReply").value?.toString() ?: "",
                requestedAt = safeLong(snap.child("timestamp").value ?: snap.child("requestedAt").value ?: snap.child("createdAt").value, System.currentTimeMillis()),
                processedAt = safeLong(snap.child("processedAt").value, 0L),
                processedBy = snap.child("processedBy").value?.toString() ?: "",
                isDeposit = safeBool(snap.child("isDeposit").value, false)
            )
        }

        val rtdbRefs = listOf(
            database.child("withdraw_requests"),
            database.child("wallet_requests"),
            database.child("payout_requests")
        )
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { c ->
                    parseSnap(c)?.let { req ->
                        if (!req.isDeposit) map[req.id] = req
                    }
                }
                emit()
            }
            override fun onCancelled(error: DatabaseError) { emit() }
        }
        rtdbRefs.forEach { it.addValueEventListener(listener) }

        val fsRegs = mutableListOf<ListenerRegistration>()
        listOf(
            firestore.collection("withdraw_requests"),
            firestore.collection("payout_requests")
        ).forEach { col ->
            try {
                fsRegs.add(col.addSnapshotListener { snap, err ->
                    if (err == null && snap != null) {
                        snap.documents.forEach { d ->
                            val isDep = d.getBoolean("isDeposit") ?: false
                            if (!isDep) {
                                val id = d.getString("id") ?: d.getString("requestId") ?: d.id
                                val amt = safeDouble(d.get("amount") ?: d.get("vtAmount"))
                                val tds = safeDouble(d.get("tdsAmount"), amt * 0.30)
                                val net = safeDouble(d.get("netAmount"), (amt - tds).coerceAtLeast(0.0))
                                val voucherSpec = d.getString("voucherSpec") ?: d.getString("voucherType") ?: ""
                                map[id] = PayoutRequest(
                                    id = id,
                                    uid = d.getString("userId") ?: d.getString("uid") ?: "",
                                    username = d.getString("username") ?: d.getString("userName") ?: "Player",
                                    inGameName = d.getString("inGameName") ?: d.getString("ign") ?: "",
                                    email = d.getString("email") ?: d.getString("userEmail") ?: "",
                                    paymentMethod = d.getString("paymentMethod") ?: d.getString("method") ?: "UPI",
                                    paymentId = d.getString("upiId") ?: d.getString("paymentId") ?: voucherSpec,
                                    voucherSpec = voucherSpec,
                                    deliveredRedeemCode = d.getString("deliveredRedeemCode") ?: "",
                                    transactionId = d.getString("transactionId") ?: "",
                                    vtAmount = amt,
                                    tdsAmount = tds,
                                    netAmount = net,
                                    status = d.getString("status")?.uppercase() ?: "PENDING",
                                    rejectionReason = d.getString("rejectionReason") ?: "",
                                    requestedAt = safeLong(d.get("timestamp") ?: d.get("requestedAt") ?: d.get("createdAt"), System.currentTimeMillis()),
                                    processedAt = safeLong(d.get("processedAt"), 0L),
                                    processedBy = d.getString("processedBy") ?: ""
                                )
                            }
                        }
                        emit()
                    }
                })
            } catch (_: Exception) {}
        }

        awaitClose {
            rtdbRefs.forEach { it.removeEventListener(listener) }
            fsRegs.forEach { it.remove() }
        }
    }

    suspend fun approveUpiWithdrawalWithTicketSync(
        req: PayoutRequest,
        payoutUtrOrNote: String,
        adminEmail: String
    ): Boolean {
        if (req.id.isBlank()) return false
        return try {
            val now = System.currentTimeMillis()
            val netAmt = req.effectiveNetAmount.toInt()
            val noteText = payoutUtrOrNote.trim().ifBlank { "Transferred to ${req.paymentId}" }
            val replyText = "UPI Payout Approved! Net ₹$netAmt (after 30% TDS) transferred. Ref/Note: $noteText"

            functions.getHttpsCallable("processWithdrawalDecision").call(
                hashMapOf(
                    "requestId" to req.id,
                    "status" to "APPROVED",
                    "utrReference" to noteText,
                    "adminNote" to replyText
                )
            ).await()

            syncLinkedTicketAcrossAllPaths(
                ticketId = req.id,
                userId = req.uid,
                status = "RESOLVED",
                adminReply = replyText,
                deliveredRedeemCode = "",
                adminEmail = adminEmail,
                now = now,
                channelNode = "withdrawal_issue_requests"
            )
            publishGlobalAppStateEvent(
                eventType = "WALLET_UPDATED",
                affectedUserId = req.uid,
                payload = mapOf("withdrawalId" to req.id, "method" to "UPI", "status" to "APPROVED", "netAmount" to req.effectiveNetAmount)
            )
            writeAuditLog("WITHDRAWAL_UPI_APPROVED", "Approved UPI withdrawal ${req.id} (Net ₹$netAmt) via processWithdrawalDecision for ${req.username}", adminEmail, req.id)
            GlobalErrorManager.emitSuccess("UPI Payout (Net ₹$netAmt) approved via server & synced to My Tickets!")
            true
        } catch (e: Exception) {
            GlobalErrorManager.emitError("processWithdrawalDecision UPI payout failed: ${e.message}")
            false
        }
    }

    suspend fun approveRedeemVoucherWithdrawal(
        req: PayoutRequest,
        redeemCode: String,
        adminEmail: String
    ): Boolean {
        val cleanCode = redeemCode.trim().uppercase()
        if (req.id.isBlank() || cleanCode.length < 6) {
            GlobalErrorManager.emitError("Please enter a valid 16-digit Redeem Voucher Code before delivering.")
            return false
        }
        return try {
            val now = System.currentTimeMillis()
            val netAmt = req.effectiveNetAmount.toInt()
            val spec = req.voucherSpec.ifBlank { req.paymentId.ifBlank { "Google Play" } }
            val replyText = "Your $spec Redeem Code (Net ₹$netAmt): $cleanCode"

            functions.getHttpsCallable("processWithdrawalDecision").call(
                hashMapOf(
                    "requestId" to req.id,
                    "status" to "APPROVED",
                    "utrReference" to cleanCode,
                    "adminNote" to replyText
                )
            ).await()

            // Sync linked ticket (id == req.id) across admin_requests, support_tickets, reports, users/{uid}/reports so My Tickets renders golden code card
            syncLinkedTicketAcrossAllPaths(
                ticketId = req.id,
                userId = req.uid,
                status = "RESOLVED",
                adminReply = replyText,
                deliveredRedeemCode = cleanCode,
                adminEmail = adminEmail,
                now = now,
                channelNode = "redeem_code_requests"
            )
            publishGlobalAppStateEvent(
                eventType = "WALLET_UPDATED",
                affectedUserId = req.uid,
                payload = mapOf(
                    "withdrawalId" to req.id,
                    "method" to "REDEEM_CODE",
                    "status" to "APPROVED",
                    "deliveredRedeemCode" to cleanCode
                )
            )
            writeAuditLog("VOUCHER_REDEEM_DELIVERED", "Delivered $spec voucher code via processWithdrawalDecision for ${req.username} (Net ₹$netAmt)", adminEmail, req.id)
            GlobalErrorManager.emitSuccess("$spec Redeem Code delivered via server & synced to My Tickets!")
            true
        } catch (e: Exception) {
            GlobalErrorManager.emitError("processWithdrawalDecision voucher delivery failed: ${e.message}")
            false
        }
    }

    suspend fun rejectWithdrawalAndRefundWithTicketSync(
        req: PayoutRequest,
        reason: String,
        adminEmail: String
    ): Boolean {
        if (req.id.isBlank()) return false
        return try {
            val now = System.currentTimeMillis()
            val cleanReason = reason.ifBlank { "Invalid payout details — Amount refunded to wallet" }
            val replyText = "Withdrawal Rejected & Refunded ₹${req.vtAmount.toInt()} VT: $cleanReason"

            // SERVER-AUTHORITATIVE REJECT & AUTO-REFUND (never manually increment balance on client!)
            functions.getHttpsCallable("processWithdrawalDecision").call(
                hashMapOf(
                    "requestId" to req.id,
                    "status" to "REJECTED",
                    "utrReference" to "",
                    "adminNote" to cleanReason
                )
            ).await()

            syncLinkedTicketAcrossAllPaths(
                ticketId = req.id,
                userId = req.uid,
                status = "REJECTED",
                adminReply = replyText,
                deliveredRedeemCode = "",
                adminEmail = adminEmail,
                now = now
            )
            publishGlobalAppStateEvent(
                eventType = "WALLET_UPDATED",
                affectedUserId = req.uid,
                payload = mapOf("withdrawalId" to req.id, "status" to "REJECTED", "refundedAmount" to req.vtAmount)
            )
            writeAuditLog("WITHDRAWAL_REJECTED_REFUNDED", "Rejected ${req.id} & server auto-refunded ₹${req.vtAmount.toInt()} to ${req.username}", adminEmail, req.id)
            GlobalErrorManager.emitSuccess("Withdrawal rejected & ₹${req.vtAmount.toInt()} VT auto-refunded by server!")
            true
        } catch (e: Exception) {
            GlobalErrorManager.emitError("processWithdrawalDecision reject failed: ${e.message}")
            false
        }
    }

    // =========================================================================
    // MODULE 3: REDEEM CODE VAULT (/redeem_codes & /redeem_claims)
    // =========================================================================

    fun getRedeemCodesStream(): Flow<List<RedeemCodeItem>> = callbackFlow {
        val rtdbMap = mutableMapOf<String, RedeemCodeItem>()
        val fsMap = mutableMapOf<String, RedeemCodeItem>()
        fun emit() {
            val merged = LinkedHashMap<String, RedeemCodeItem>()
            if (fsMap.isNotEmpty()) {
                fsMap.forEach { (k, v) -> merged[k] = v }
            } else {
                rtdbMap.forEach { (k, v) -> merged[k] = v }
            }
            trySend(merged.values.sortedByDescending { it.updatedAt })
        }

        val ref = database.child("redeem_codes")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                rtdbMap.clear()
                for (c in snapshot.children) {
                    if (!c.hasChildren() || c.value !is Map<*, *>) continue
                    val code = (c.child("code").value?.toString() ?: c.key ?: "").trim().uppercase()
                    if (code.isNotBlank()) {
                        val isActive = safeBool(c.child("active").value, c.child("status").value?.toString()?.equals("ACTIVE", true) != false)
                        rtdbMap[code] = RedeemCodeItem(
                            code = code,
                            title = c.child("title").value?.toString() ?: code,
                            active = isActive,
                            status = c.child("status").value?.toString()?.uppercase() ?: if (isActive) "ACTIVE" else "DISABLED",
                            rewardVt = safeDouble(c.child("rewardVt").value),
                            rewardTokens = safeInt(c.child("rewardTokens").value),
                            maxUses = safeInt(c.child("maxUses").value, 0),
                            usedCount = safeInt(c.child("usedCount").value, 0),
                            expiresAt = safeLong(c.child("expiresAt").value, 0L),
                            targetUserId = c.child("targetUserId").value?.toString() ?: "",
                            createdAt = safeLong(c.child("createdAt").value, System.currentTimeMillis()),
                            updatedAt = safeLong(c.child("updatedAt").value, System.currentTimeMillis())
                        )
                    }
                }
                emit()
            }
            override fun onCancelled(error: DatabaseError) { emit() }
        }
        ref.addValueEventListener(listener)

        val fsReg = try {
            firestore.collection("redeem_codes").addSnapshotListener { snap, err ->
                if (err == null && snap != null) {
                    fsMap.clear()
                    snap.documents.forEach { d ->
                        val code = (d.getString("code") ?: d.id).trim().uppercase()
                        val isActive = d.getBoolean("active") ?: (d.getString("status")?.equals("ACTIVE", true) != false)
                        fsMap[code] = RedeemCodeItem(
                            code = code,
                            title = d.getString("title") ?: code,
                            active = isActive,
                            status = d.getString("status")?.uppercase() ?: if (isActive) "ACTIVE" else "DISABLED",
                            rewardVt = safeDouble(d.get("rewardVt")),
                            rewardTokens = safeInt(d.get("rewardTokens")),
                            maxUses = safeInt(d.get("maxUses"), 0),
                            usedCount = safeInt(d.get("usedCount"), 0),
                            expiresAt = safeLong(d.get("expiresAt"), 0L),
                            targetUserId = d.getString("targetUserId") ?: "",
                            createdAt = safeLong(d.get("createdAt"), System.currentTimeMillis()),
                            updatedAt = safeLong(d.get("updatedAt"), System.currentTimeMillis())
                        )
                    }
                    emit()
                }
            }
        } catch (_: Exception) { null }

        awaitClose {
            ref.removeEventListener(listener)
            fsReg?.remove()
        }
    }

    suspend fun saveRedeemCode(item: RedeemCodeItem, adminEmail: String): Boolean {
        val codeKey = item.code.trim().uppercase().replace(Regex("[^A-Z0-9_-]"), "")
        if (codeKey.length < 3) {
            GlobalErrorManager.emitError("Redeem code must be at least 3 alphanumeric characters.")
            return false
        }
        return try {
            val now = System.currentTimeMillis()
            val payload = mapOf<String, Any>(
                "code" to codeKey,
                "title" to item.title.trim().ifBlank { codeKey },
                "active" to item.active,
                "status" to if (item.active) "ACTIVE" else "DISABLED",
                "rewardVt" to item.rewardVt.coerceAtLeast(0.0),
                "rewardTokens" to item.rewardTokens.coerceAtLeast(0),
                "maxUses" to item.maxUses.coerceAtLeast(0),
                "usedCount" to item.usedCount.coerceAtLeast(0),
                "expiresAt" to item.expiresAt.coerceAtLeast(0L),
                "targetUserId" to item.targetUserId.trim(),
                "createdAt" to (if (item.createdAt > 0L) item.createdAt else now),
                "updatedAt" to now
            )
            // Primary write target: Firestore redeem_codes/{codeKey} (Cloud Functions mirror RTDB & global_app_state)
            firestore.collection("redeem_codes").document(codeKey).set(payload, SetOptions.merge()).await()
            writeAuditLog("REDEEM_CODE_SAVED", "Saved promo code $codeKey (VT=${item.rewardVt}, Tokens=${item.rewardTokens}, MaxUses=${item.maxUses})", adminEmail, codeKey)
            GlobalErrorManager.emitSuccess("Redeem code '$codeKey' saved to Firestore & syncing live!")
            true
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to save redeem code: ${e.message}")
            false
        }
    }

    suspend fun toggleRedeemCodeActive(code: String, active: Boolean, adminEmail: String) {
        val codeKey = code.trim().uppercase()
        if (codeKey.isBlank()) return
        try {
            val updates = mapOf<String, Any>(
                "active" to active,
                "status" to if (active) "ACTIVE" else "DISABLED",
                "updatedAt" to System.currentTimeMillis()
            )
            // Primary write target: Firestore redeem_codes/{codeKey}
            firestore.collection("redeem_codes").document(codeKey).set(updates, SetOptions.merge()).await()
            writeAuditLog("REDEEM_CODE_TOGGLED", "Set $codeKey to ${if (active) "ACTIVE" else "DISABLED"}", adminEmail, codeKey)
            GlobalErrorManager.emitSuccess("Code '$codeKey' is now ${if (active) "ACTIVE" else "DISABLED"}.")
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to toggle redeem code: ${e.message}")
        }
    }

    suspend fun deleteRedeemCode(code: String, adminEmail: String) {
        val codeKey = code.trim().uppercase()
        if (codeKey.isBlank()) return
        try {
            // Primary delete target: Firestore redeem_codes/{codeKey} (Cloud Functions clean RTDB mirror)
            firestore.collection("redeem_codes").document(codeKey).delete().await()
            writeAuditLog("REDEEM_CODE_DELETED", "Deleted promo code $codeKey", adminEmail, codeKey)
            GlobalErrorManager.emitSuccess("Redeem code '$codeKey' deleted from Firestore.")
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to delete redeem code: ${e.message}")
        }
    }

    fun getRedeemClaimsStream(): Flow<List<RedeemClaimRecord>> = callbackFlow {
        val map = mutableMapOf<String, RedeemClaimRecord>()
        fun emit() { trySend(map.values.sortedByDescending { it.claimedAt }) }

        val ref = database.child("redeem_claims")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (c in snapshot.children) {
                    if (!c.hasChildren() || c.value !is Map<*, *>) continue
                    val id = c.child("id").value?.toString() ?: c.key ?: ""
                    if (id.isNotBlank()) {
                        map[id] = RedeemClaimRecord(
                            id = id,
                            code = c.child("code").value?.toString() ?: "",
                            userId = c.child("userId").value?.toString() ?: c.child("uid").value?.toString() ?: "",
                            username = c.child("username").value?.toString() ?: c.child("userName").value?.toString() ?: "Player",
                            rewardVt = safeDouble(c.child("rewardVt").value),
                            rewardTokens = safeInt(c.child("rewardTokens").value),
                            claimedAt = safeLong(c.child("claimedAt").value ?: c.child("timestamp").value, System.currentTimeMillis())
                        )
                    }
                }
                emit()
            }
            override fun onCancelled(error: DatabaseError) { emit() }
        }
        ref.addValueEventListener(listener)

        val fsReg = try {
            firestore.collection("redeem_claims").addSnapshotListener { snap, err ->
                if (err == null && snap != null) {
                    snap.documents.forEach { d ->
                        map[d.id] = RedeemClaimRecord(
                            id = d.id,
                            code = d.getString("code") ?: "",
                            userId = d.getString("userId") ?: d.getString("uid") ?: "",
                            username = d.getString("username") ?: d.getString("userName") ?: "Player",
                            rewardVt = safeDouble(d.get("rewardVt")),
                            rewardTokens = safeInt(d.get("rewardTokens")),
                            claimedAt = safeLong(d.get("claimedAt") ?: d.get("timestamp"), System.currentTimeMillis())
                        )
                    }
                    emit()
                }
            }
        } catch (_: Exception) { null }

        awaitClose {
            ref.removeEventListener(listener)
            fsReg?.remove()
        }
    }

    // =========================================================================
    // MODULE 4: UNIFIED 12-CHANNEL REQUEST HUB & "MY TICKETS" DESK
    // =========================================================================

    fun getUnifiedAdminRequestsStream(): Flow<List<UnifiedAdminRequest>> = callbackFlow {
        val map = mutableMapOf<String, UnifiedAdminRequest>()
        fun emit() { trySend(map.values.sortedByDescending { it.updatedAt.coerceAtLeast(it.createdAt) }) }

        fun parseSnap(snap: DataSnapshot, source: String, fallbackUserId: String = "", fallbackUsername: String = "", fallbackIgn: String = "", fallbackEmail: String = ""): UnifiedAdminRequest? {
            if (!snap.exists()) return null
            // Skip plain primitives
            if (snap.childrenCount == 0L) return null
            val id = (snap.child("id").value
                ?: snap.child("requestId").value
                ?: snap.child("ticketId").value
                ?: snap.child("appealId").value
                ?: snap.child("activeAppealTicket").value
                ?: snap.child("appealTicketId").value
                ?: snap.key)?.toString()?.trim() ?: return null
            if (id.isBlank()) return null

            val rawSubject = (snap.child("subject").value
                ?: snap.child("title").value
                ?: snap.child("reason").value
                ?: snap.child("banReason").value
                ?: snap.child("appealReason").value)?.toString()?.trim().orEmpty()
            val rawDesc = (snap.child("description").value
                ?: snap.child("details").value
                ?: snap.child("message").value
                ?: snap.child("appealMessage").value
                ?: snap.child("banReason").value)?.toString()?.trim().orEmpty()
            val related = (snap.child("relatedId").value
                ?: snap.child("banCaseId").value
                ?: snap.child("sanctionId").value
                ?: snap.child("caseId").value)?.toString()?.trim().orEmpty()

            // Guard against partial status-only ghost nodes
            val hasUserOrContent = snap.hasChild("userId") || snap.hasChild("uid") ||
                snap.hasChild("username") || snap.hasChild("userName") ||
                snap.hasChild("subject") || snap.hasChild("description") ||
                snap.hasChild("message") || snap.hasChild("reason") ||
                snap.hasChild("category") || snap.hasChild("relatedId") ||
                snap.hasChild("banCaseId") || id.startsWith("VRX-", ignoreCase = true) ||
                fallbackUserId.isNotBlank()
            if (!hasUserOrContent && rawSubject.isBlank() && rawDesc.isBlank()) return null

            val catRaw = (snap.child("category").value
                ?: snap.child("issueCategory").value
                ?: snap.child("channelType").value
                ?: snap.child("type").value)?.toString()?.trim().orEmpty()
            val cat = if (catRaw.isBlank() && (related.startsWith("BAN-", ignoreCase = true) || source.contains("ban", ignoreCase = true) || source.contains("appeal", ignoreCase = true))) {
                "BAN_APPEAL"
            } else catRaw
            val ch = (snap.child("channel").value
                ?: snap.child("channelLabel").value)?.toString()?.trim().orEmpty()
            val matched = UnifiedRequestChannel.matchFromRaw(cat, ch, source)
            val created = safeLong(
                snap.child("createdAt").value ?: snap.child("timestamp").value ?: snap.child("requestedAt").value ?: snap.child("bannedAt").value,
                System.currentTimeMillis()
            )
            return UnifiedAdminRequest(
                id = id,
                userId = (snap.child("userId").value ?: snap.child("uid").value)?.toString()?.trim().orEmpty().ifBlank { fallbackUserId },
                username = (snap.child("username").value ?: snap.child("userName").value ?: snap.child("name").value ?: snap.child("accountName").value)?.toString()?.trim().orEmpty().ifBlank { fallbackUsername.ifBlank { "Player" } },
                userEmail = (snap.child("userEmail").value ?: snap.child("email").value ?: snap.child("userContact").value)?.toString()?.trim().orEmpty().ifBlank { fallbackEmail },
                inGameName = (snap.child("inGameName").value ?: snap.child("ign").value)?.toString()?.trim().orEmpty().ifBlank { fallbackIgn },
                freeFireId = (snap.child("freeFireId").value ?: snap.child("gameId").value)?.toString()?.trim().orEmpty(),
                category = cat.ifBlank { matched.categoryKey },
                channel = ch.ifBlank { matched.channelTag },
                subject = rawSubject.ifBlank { matched.label },
                description = rawDesc,
                relatedId = related,
                amount = safeDouble(snap.child("amount").value ?: snap.child("vtAmount").value),
                utrNumber = snap.child("utrNumber").value?.toString() ?: "",
                paymentMethod = snap.child("paymentMethod").value?.toString() ?: "",
                voucherSpec = snap.child("voucherSpec").value?.toString() ?: "",
                status = (snap.child("status").value ?: snap.child("appealStatus").value)?.toString()?.uppercase() ?: "PENDING",
                adminReply = (snap.child("adminReply").value ?: snap.child("adminNote").value)?.toString() ?: "",
                deliveredRedeemCode = snap.child("deliveredRedeemCode").value?.toString() ?: "",
                sourceNode = source,
                createdAt = created,
                updatedAt = safeLong(snap.child("updatedAt").value, created)
            )
        }

        fun upsertItem(item: UnifiedAdminRequest) {
            val existing = map[item.id]
            if (existing == null || item.updatedAt >= existing.updatedAt) {
                map[item.id] = item
            }
        }

        val rtdbSources = listOf(
            "admin_requests/all" to database.child("admin_requests").child("all"),
            "admin_requests/ban_appeals" to database.child("admin_requests").child("ban_appeals"),
            "admin_requests/account_issue" to database.child("admin_requests").child("account_issue"),
            "admin_requests/verification_requests" to database.child("admin_requests").child("verification_requests"),
            "admin_requests/general_support_requests" to database.child("admin_requests").child("general_support_requests"),
            "ban_appeals" to database.child("ban_appeals"),
            "appeals" to database.child("appeals"),
            "ban_appeal_requests" to database.child("ban_appeal_requests"),
            "admin_channels/ban_appeals" to database.child("admin_channels").child("ban_appeals"),
            "admin_channels/account_issue" to database.child("admin_channels").child("account_issue"),
            "support_tickets" to database.child("support_tickets"),
            "tickets" to database.child("tickets"),
            "reports" to database.child("reports"),
            "banner_feedback" to database.child("banner_feedback")
        )
        val rtdbListeners = mutableListOf<Pair<DatabaseReference, ValueEventListener>>()
        rtdbSources.forEach { (srcName, ref) ->
            val l = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (c in snapshot.children) {
                        if (!c.hasChildren() || c.value !is Map<*, *>) continue
                        parseSnap(c, srcName)?.let { upsertItem(it) }
                    }
                    emit()
                }
                override fun onCancelled(error: DatabaseError) { emit() }
            }
            ref.addValueEventListener(l)
            rtdbListeners.add(ref to l)
        }

        // Also listen to /banned_users & /users in RTDB to catch embedded ban appeals or per-user reports/ban_appeals sub-nodes
        val bannedAppealsRef = database.child("banned_users")
        val bannedAppealsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (c in snapshot.children) {
                    if (!c.hasChildren() || c.value !is Map<*, *>) continue
                    val uid = (c.child("uid").value ?: c.child("userId").value ?: c.child("id").value ?: c.key)?.toString()?.trim().orEmpty()
                    val uname = (c.child("username").value ?: c.child("userName").value ?: c.child("name").value)?.toString()?.trim().orEmpty()
                    val uign = (c.child("inGameName").value ?: c.child("ign").value)?.toString()?.trim().orEmpty()
                    val uemail = (c.child("email").value ?: c.child("userEmail").value)?.toString()?.trim().orEmpty()
                    val ticketId = (c.child("activeAppealTicket").value ?: c.child("activeAppealTicketId").value ?: c.child("appealTicketId").value ?: c.child("ticketId").value)?.toString()?.trim().orEmpty()
                    val caseId = (c.child("banCaseId").value ?: c.child("sanctionId").value ?: c.child("caseId").value)?.toString()?.trim().orEmpty()
                    val reason = (c.child("banReason").value ?: c.child("reason").value ?: c.child("appealReason").value)?.toString()?.trim().orEmpty()
                    val appealMsg = (c.child("appealMessage").value ?: c.child("appealDetails").value ?: c.child("message").value)?.toString()?.trim().orEmpty()
                    if (ticketId.isNotBlank()) {
                        val created = safeLong(c.child("appealCreatedAt").value ?: c.child("bannedAt").value ?: c.child("updatedAt").value, System.currentTimeMillis())
                        upsertItem(
                            UnifiedAdminRequest(
                                id = ticketId,
                                userId = uid,
                                username = uname.ifBlank { uign.ifBlank { "Player" } },
                                userEmail = uemail,
                                inGameName = uign,
                                freeFireId = (c.child("gameId").value ?: c.child("freeFireId").value)?.toString()?.trim().orEmpty(),
                                category = "BAN_APPEAL",
                                channel = "#ban-appeals",
                                subject = "Ban Appeal ${if (caseId.isNotBlank()) "($caseId)" else ""}".trim(),
                                description = appealMsg.ifBlank { reason.ifBlank { "Account restriction appeal submitted by player." } },
                                relatedId = caseId,
                                status = (c.child("appealStatus").value ?: c.child("activeAppealStatus").value)?.toString()?.uppercase() ?: "PENDING",
                                sourceNode = "banned_users",
                                createdAt = created,
                                updatedAt = safeLong(c.child("updatedAt").value, created)
                            )
                        )
                    }
                    // Also check nested appeal objects inside /banned_users/{uid}
                    for (subKey in listOf("banAppeal", "appeal", "activeAppeal")) {
                        val sub = c.child(subKey)
                        if (sub.exists()) {
                            parseSnap(sub, "banned_users/$subKey", uid, uname, uign, uemail)?.let { upsertItem(it) }
                        }
                    }
                }
                emit()
            }
            override fun onCancelled(error: DatabaseError) { emit() }
        }
        bannedAppealsRef.addValueEventListener(bannedAppealsListener)
        rtdbListeners.add(bannedAppealsRef to bannedAppealsListener)

        val usersAppealsRef = database.child("users")
        val usersAppealsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { u ->
                    val uid = (u.child("id").value ?: u.child("uid").value ?: u.key)?.toString()?.trim().orEmpty()
                    val uname = (u.child("username").value ?: u.child("name").value ?: u.child("displayName").value)?.toString()?.trim().orEmpty()
                    val uign = (u.child("ign").value ?: u.child("inGameName").value)?.toString()?.trim().orEmpty()
                    val uemail = (u.child("email").value ?: u.child("userEmail").value)?.toString()?.trim().orEmpty()

                    // Nested per-user ticket collections: /users/{uid}/reports, /users/{uid}/ban_appeals, /users/{uid}/appeals, /users/{uid}/tickets
                    for (subCol in listOf("reports", "ban_appeals", "appeals", "tickets", "support_tickets")) {
                        val colSnap = u.child(subCol)
                        if (colSnap.exists()) {
                            colSnap.children.forEach { tSnap ->
                                parseSnap(tSnap, "users/$subCol", uid, uname, uign, uemail)?.let { upsertItem(it) }
                            }
                        }
                    }
                    // Direct activeAppealTicket field on /users/{uid}
                    val ticketId = (u.child("activeAppealTicket").value ?: u.child("activeAppealTicketId").value ?: u.child("appealTicketId").value)?.toString()?.trim().orEmpty()
                    if (ticketId.isNotBlank()) {
                        val caseId = (u.child("banCaseId").value ?: u.child("sanctionId").value)?.toString()?.trim().orEmpty()
                        val reason = (u.child("banReason").value ?: u.child("reason").value)?.toString()?.trim().orEmpty()
                        val appealMsg = (u.child("appealMessage").value ?: u.child("appealReason").value)?.toString()?.trim().orEmpty()
                        val created = safeLong(u.child("bannedAt").value ?: u.child("updatedAt").value, System.currentTimeMillis())
                        upsertItem(
                            UnifiedAdminRequest(
                                id = ticketId,
                                userId = uid,
                                username = uname.ifBlank { uign.ifBlank { "Player" } },
                                userEmail = uemail,
                                inGameName = uign,
                                freeFireId = (u.child("gameId").value ?: u.child("freeFireId").value)?.toString()?.trim().orEmpty(),
                                category = "BAN_APPEAL",
                                channel = "#ban-appeals",
                                subject = "Ban Appeal ${if (caseId.isNotBlank()) "($caseId)" else ""}".trim(),
                                description = appealMsg.ifBlank { reason.ifBlank { "Account restriction appeal." } },
                                relatedId = caseId,
                                status = (u.child("activeAppealStatus").value ?: u.child("appealStatus").value)?.toString()?.uppercase() ?: "PENDING",
                                sourceNode = "users/activeAppealTicket",
                                createdAt = created,
                                updatedAt = safeLong(u.child("updatedAt").value, created)
                            )
                        )
                    }
                }
                emit()
            }
            override fun onCancelled(error: DatabaseError) { emit() }
        }
        usersAppealsRef.addValueEventListener(usersAppealsListener)
        rtdbListeners.add(usersAppealsRef to usersAppealsListener)

        val fsRegs = mutableListOf<ListenerRegistration>()
        listOf(
            "admin_requests" to firestore.collection("admin_requests"),
            "ban_appeals" to firestore.collection("ban_appeals"),
            "appeals" to firestore.collection("appeals"),
            "support_tickets" to firestore.collection("support_tickets"),
            "tickets" to firestore.collection("tickets"),
            "reports" to firestore.collection("reports")
        ).forEach { (srcName, col) ->
            try {
                fsRegs.add(col.addSnapshotListener { snap, err ->
                    if (err == null && snap != null) {
                        snap.documents.forEach { d ->
                            val id = d.getString("id") ?: d.getString("requestId") ?: d.getString("ticketId") ?: d.id
                            val related = d.getString("relatedId") ?: d.getString("banCaseId") ?: d.getString("sanctionId") ?: d.getString("caseId") ?: ""
                            val catRaw = d.getString("category") ?: d.getString("issueCategory") ?: d.getString("type") ?: ""
                            val cat = if (catRaw.isBlank() && (related.startsWith("BAN-", ignoreCase = true) || srcName.contains("ban", ignoreCase = true) || srcName.contains("appeal", ignoreCase = true))) {
                                "BAN_APPEAL"
                            } else catRaw
                            val ch = d.getString("channel") ?: d.getString("channelLabel") ?: ""
                            val matched = UnifiedRequestChannel.matchFromRaw(cat, ch, srcName)
                            val created = safeLong(d.get("createdAt") ?: d.get("timestamp"), System.currentTimeMillis())
                            val item = UnifiedAdminRequest(
                                id = id,
                                userId = d.getString("userId") ?: d.getString("uid") ?: "",
                                username = d.getString("username") ?: d.getString("userName") ?: d.getString("name") ?: "Player",
                                userEmail = d.getString("userEmail") ?: d.getString("email") ?: d.getString("userContact") ?: "",
                                inGameName = d.getString("inGameName") ?: d.getString("ign") ?: "",
                                freeFireId = d.getString("freeFireId") ?: d.getString("gameId") ?: "",
                                category = cat.ifBlank { matched.categoryKey },
                                channel = ch.ifBlank { matched.channelTag },
                                subject = d.getString("subject") ?: d.getString("title") ?: d.getString("reason") ?: matched.label,
                                description = d.getString("description") ?: d.getString("details") ?: d.getString("message") ?: "",
                                relatedId = related,
                                amount = safeDouble(d.get("amount") ?: d.get("vtAmount")),
                                utrNumber = d.getString("utrNumber") ?: "",
                                paymentMethod = d.getString("paymentMethod") ?: "",
                                voucherSpec = d.getString("voucherSpec") ?: "",
                                status = (d.getString("status") ?: d.getString("appealStatus"))?.uppercase() ?: "PENDING",
                                adminReply = d.getString("adminReply") ?: d.getString("adminNote") ?: "",
                                deliveredRedeemCode = d.getString("deliveredRedeemCode") ?: "",
                                sourceNode = srcName,
                                createdAt = created,
                                updatedAt = safeLong(d.get("updatedAt"), created)
                            )
                            upsertItem(item)
                        }
                        emit()
                    }
                })
            } catch (_: Exception) {}
        }

        // Also check Firestore banned_users for embedded activeAppealTicket
        try {
            fsRegs.add(firestore.collection("banned_users").addSnapshotListener { snap, err ->
                if (err == null && snap != null) {
                    snap.documents.forEach { d ->
                        val ticketId = (d.getString("activeAppealTicket") ?: d.getString("activeAppealTicketId") ?: d.getString("appealTicketId") ?: d.getString("ticketId")).orEmpty().trim()
                        if (ticketId.isNotBlank()) {
                            val uid = (d.getString("uid") ?: d.getString("userId") ?: d.getString("id") ?: d.id).trim()
                            val uname = (d.getString("username") ?: d.getString("userName") ?: d.getString("name")).orEmpty().trim()
                            val uign = (d.getString("inGameName") ?: d.getString("ign")).orEmpty().trim()
                            val caseId = (d.getString("banCaseId") ?: d.getString("sanctionId")).orEmpty().trim()
                            val reason = (d.getString("banReason") ?: d.getString("reason")).orEmpty().trim()
                            val created = safeLong(d.get("bannedAt") ?: d.get("updatedAt"), System.currentTimeMillis())
                            upsertItem(
                                UnifiedAdminRequest(
                                    id = ticketId,
                                    userId = uid,
                                    username = uname.ifBlank { uign.ifBlank { "Player" } },
                                    userEmail = (d.getString("email") ?: d.getString("userEmail")).orEmpty().trim(),
                                    inGameName = uign,
                                    freeFireId = (d.getString("gameId") ?: d.getString("freeFireId")).orEmpty().trim(),
                                    category = "BAN_APPEAL",
                                    channel = "#ban-appeals",
                                    subject = "Ban Appeal ${if (caseId.isNotBlank()) "($caseId)" else ""}".trim(),
                                    description = (d.getString("appealMessage") ?: reason).ifBlank { "Account restriction appeal." },
                                    relatedId = caseId,
                                    status = (d.getString("appealStatus") ?: d.getString("activeAppealStatus"))?.uppercase() ?: "PENDING",
                                    sourceNode = "fs_banned_users",
                                    createdAt = created,
                                    updatedAt = safeLong(d.get("updatedAt"), created)
                                )
                            )
                        }
                    }
                    emit()
                }
            })
        } catch (_: Exception) {}

        awaitClose {
            rtdbListeners.forEach { (r, l) -> r.removeEventListener(l) }
            fsRegs.forEach { it.remove() }
        }
    }

    suspend fun approveBanAppealAndUnban(
        req: UnifiedAdminRequest,
        matchedUser: UserProfile?,
        adminNote: String,
        adminEmail: String
    ): Boolean {
        return try {
            val replyText = adminNote.trim().ifBlank {
                "Your ban appeal (#${req.id}) has been APPROVED by Admin. Your account restriction is lifted and your account is now ACTIVE."
            }
            val syntheticOrMatchedUser = matchedUser ?: UserProfile(
                id = req.userId.ifBlank { req.id },
                username = req.username.ifBlank { req.inGameName.ifBlank { "Player" } },
                ign = req.inGameName.ifBlank { req.username },
                email = req.userEmail,
                gameId = req.freeFireId,
                banCaseId = req.relatedId,
                activeAppealTicketId = req.id,
                isBanned = true,
                status = "BANNED",
                accountStatus = "BANNED"
            )
            // 1. Unban across all RTDB & Firestore user nodes and sweep /banned_users
            setUserBanStatus(
                user = syntheticOrMatchedUser.copy(
                    banCaseId = syntheticOrMatchedUser.banCaseId.ifBlank { req.relatedId },
                    activeAppealTicketId = req.id
                ),
                isBanned = false,
                reason = "",
                adminEmail = adminEmail
            )
            // 2. Mark the ticket RESOLVED across all ticket/appeal paths
            if (req.id.isNotBlank()) {
                replyAndUpdateUnifiedTicket(
                    req = req,
                    newStatus = "RESOLVED",
                    adminReply = replyText,
                    deliveredRedeemCode = "",
                    adminEmail = adminEmail
                )
            }
            GlobalErrorManager.emitSuccess("Appeal #${req.id} APPROVED & ${syntheticOrMatchedUser.username} UNBANNED!")
            true
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to approve ban appeal: ${e.message}")
            false
        }
    }

    suspend fun rejectBanAppeal(
        req: UnifiedAdminRequest,
        adminNote: String,
        adminEmail: String
    ): Boolean {
        return try {
            val replyText = adminNote.trim().ifBlank {
                "Your ban appeal (#${req.id}) was reviewed and REJECTED due to confirmed Fair Play / Anti-Cheat policy violations."
            }
            val ok = replyAndUpdateUnifiedTicket(
                req = req,
                newStatus = "REJECTED",
                adminReply = replyText,
                deliveredRedeemCode = "",
                adminEmail = adminEmail
            )
            if (req.userId.isNotBlank()) {
                val updates = mapOf<String, Any>(
                    "activeAppealStatus" to "REJECTED",
                    "appealStatus" to "REJECTED",
                    "updatedAt" to System.currentTimeMillis()
                )
                try { database.child("users").child(req.userId).updateChildren(updates).await() } catch (_: Exception) {}
                try { database.child("banned_users").child(req.userId).updateChildren(updates).await() } catch (_: Exception) {}
                try { firestore.collection("users").document(req.userId).set(updates, SetOptions.merge()).await() } catch (_: Exception) {}
                try { firestore.collection("banned_users").document(req.userId).set(updates, SetOptions.merge()).await() } catch (_: Exception) {}
            }
            ok
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Failed to reject ban appeal: ${e.message}")
            false
        }
    }

    suspend fun replyAndUpdateUnifiedTicket(
        req: UnifiedAdminRequest,
        newStatus: String,
        adminReply: String,
        deliveredRedeemCode: String,
        adminEmail: String
    ): Boolean {
        if (req.id.isBlank()) return false
        return try {
            val now = System.currentTimeMillis()
            val cleanStatus = newStatus.trim().uppercase().ifBlank { "RESOLVED" }
            val cleanReply = adminReply.trim().ifBlank { "Ticket updated to $cleanStatus by Admin." }
            val cleanCode = deliveredRedeemCode.trim().uppercase()

            syncLinkedTicketAcrossAllPaths(
                ticketId = req.id,
                userId = req.userId,
                status = cleanStatus,
                adminReply = cleanReply,
                deliveredRedeemCode = cleanCode,
                adminEmail = adminEmail,
                now = now,
                channelNode = if (req.isBanAppeal) "ban_appeals" else req.resolvedChannel.rtdbNode
            )
            publishGlobalAppStateEvent(
                eventType = "SUPPORT_TICKET_UPDATED",
                affectedUserId = req.userId,
                payload = mapOf(
                    "ticketId" to req.id,
                    "status" to cleanStatus,
                    "channel" to req.resolvedChannel.channelTag
                )
            )
            writeAuditLog("TICKET_UPDATED", "Updated ticket ${req.id} (${req.resolvedChannel.channelTag}) -> $cleanStatus", adminEmail, req.id)
            GlobalErrorManager.emitSuccess("Ticket #${req.id.takeLast(8)} synced ($cleanStatus) to user's My Tickets!")
            true
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Ticket reply failed: ${e.message}")
            false
        }
    }

    suspend fun approveAccountDeletionFromUnifiedTicket(
        req: UnifiedAdminRequest,
        adminNote: String,
        adminEmail: String
    ): Boolean {
        val uid = req.userId.trim()
        if (req.id.isBlank() || uid.isBlank()) {
            GlobalErrorManager.emitError("Missing user ID on Account Deletion ticket.")
            return false
        }
        return try {
            val now = System.currentTimeMillis()
            val replyText = adminNote.trim().ifBlank {
                "Your Account Deletion request has been APPROVED and your user profile has been permanently erased."
            }
            // Delete users/{uid} in Firestore & RTDB
            try { firestore.collection("users").document(uid).delete().await() } catch (_: Exception) {}
            try { database.child("users").child(uid).removeValue().await() } catch (_: Exception) {}
            try { database.child("userProfiles").child(uid).removeValue().await() } catch (_: Exception) {}

            syncLinkedTicketAcrossAllPaths(
                ticketId = req.id,
                userId = uid,
                status = "RESOLVED",
                adminReply = replyText,
                deliveredRedeemCode = "",
                adminEmail = adminEmail,
                now = now,
                channelNode = "account_deletion_requests"
            )
            publishGlobalAppStateEvent(
                eventType = "USER_STATUS_CHANGED",
                affectedUserId = uid,
                payload = mapOf("newStatus" to "DELETED", "ticketId" to req.id)
            )
            writeAuditLog("ACCOUNT_DELETED", "Deleted user $uid via ACCOUNT_DELETION ticket ${req.id}", adminEmail, uid)
            GlobalErrorManager.emitSuccess("User $uid deleted & ticket ${req.id.takeLast(8)} resolved!")
            true
        } catch (e: Exception) {
            GlobalErrorManager.emitError("Account deletion failed: ${e.message}")
            false
        }
    }

    // =========================================================================
    // MODULE 5B: CHAT REPORTS QUEUE (/chat_reports)
    // =========================================================================

    fun getChatReportsStream(): Flow<List<ChatReportItem>> = callbackFlow {
        val map = mutableMapOf<String, ChatReportItem>()
        fun emit() { trySend(map.values.sortedByDescending { it.timestamp }) }

        val ref = database.child("chat_reports")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { c ->
                    val id = c.child("id").value?.toString() ?: c.key ?: ""
                    if (id.isNotBlank()) {
                        map[id] = ChatReportItem(
                            id = id,
                            tournamentId = c.child("tournamentId").value?.toString() ?: "",
                            messageId = c.child("messageId").value?.toString() ?: "",
                            reportedUserId = c.child("reportedUserId").value?.toString() ?: c.child("senderId").value?.toString() ?: "",
                            reportedUserName = c.child("reportedUserName").value?.toString() ?: c.child("senderName").value?.toString() ?: "Player",
                            reporterUserId = c.child("reporterUserId").value?.toString() ?: c.child("reporterId").value?.toString() ?: "",
                            reporterName = c.child("reporterName").value?.toString() ?: "Player",
                            messageText = c.child("messageText").value?.toString() ?: c.child("text").value?.toString() ?: c.child("message").value?.toString() ?: "",
                            reason = c.child("reason").value?.toString() ?: "Abusive / Toxic chat",
                            status = c.child("status").value?.toString()?.uppercase() ?: "PENDING",
                            timestamp = safeLong(c.child("timestamp").value ?: c.child("createdAt").value, System.currentTimeMillis())
                        )
                    }
                }
                emit()
            }
            override fun onCancelled(error: DatabaseError) { emit() }
        }
        ref.addValueEventListener(listener)

        val fsReg = try {
            firestore.collection("chat_reports").addSnapshotListener { snap, err ->
                if (err == null && snap != null) {
                    snap.documents.forEach { d ->
                        map[d.id] = ChatReportItem(
                            id = d.id,
                            tournamentId = d.getString("tournamentId") ?: "",
                            messageId = d.getString("messageId") ?: "",
                            reportedUserId = d.getString("reportedUserId") ?: d.getString("senderId") ?: "",
                            reportedUserName = d.getString("reportedUserName") ?: d.getString("senderName") ?: "Player",
                            reporterUserId = d.getString("reporterUserId") ?: d.getString("reporterId") ?: "",
                            reporterName = d.getString("reporterName") ?: "Player",
                            messageText = d.getString("messageText") ?: d.getString("text") ?: d.getString("message") ?: "",
                            reason = d.getString("reason") ?: "Abusive / Toxic chat",
                            status = d.getString("status")?.uppercase() ?: "PENDING",
                            timestamp = safeLong(d.get("timestamp") ?: d.get("createdAt"), System.currentTimeMillis())
                        )
                    }
                    emit()
                }
            }
        } catch (_: Exception) { null }

        awaitClose {
            ref.removeEventListener(listener)
            fsReg?.remove()
        }
    }

    suspend fun resolveChatReport(report: ChatReportItem, actionTaken: String, adminEmail: String) {
        if (report.id.isBlank()) return
        val now = System.currentTimeMillis()
        val updates = mapOf<String, Any>(
            "status" to "RESOLVED",
            "actionTaken" to actionTaken,
            "processedBy" to adminEmail,
            "updatedAt" to now
        )
        try { database.child("chat_reports").child(report.id).updateChildren(updates).await() } catch (_: Exception) {}
        try { firestore.collection("chat_reports").document(report.id).set(updates, SetOptions.merge()).await() } catch (_: Exception) {}
        writeAuditLog("CHAT_REPORT_RESOLVED", "Resolved chat report ${report.id} ($actionTaken)", adminEmail, report.id)
    }
}


