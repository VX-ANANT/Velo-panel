package com.example.data.agent

import android.util.Log
import com.example.data.repository.TournamentRepositoryImpl
import com.example.domain.model.AppAnnouncementBanner
import com.example.domain.model.RoomDetails
import com.example.domain.model.SupportTicket
import com.example.domain.model.Tournament
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ExecutionResult(
    val success: Boolean,
    val message: String,
    val createdTournament: Tournament? = null
)

object VelorixAgentActionExecutor {
    private const val TAG = "VelorixAgentExecutor"

    suspend fun execute(
        action: VelorixAgentAction,
        repository: TournamentRepositoryImpl
    ): ExecutionResult = withContext(Dispatchers.IO) {
        val currUser = repository.auth.currentUser
        val adminEmail = currUser?.email?.ifBlank { null } ?: repository.activeAdminEmail.ifBlank { "anantisback47@gmail.com" }
        val adminUid = currUser?.uid?.ifBlank { null } ?: repository.activeAdminUid

        try {
            when (action.actionType) {
                AgentActionType.CREATE_TOURNAMENT -> {
                    val p = action.params
                    val category = p["category"]?.uppercase()?.trim() ?: "BR"
                    val format = p["format"]?.uppercase()?.trim() ?: if (category == "CS") "4V4" else if (category == "LONE_WOLF") "1V1" else "SOLO"
                    val map = p["map"]?.trim()?.ifBlank { "Bermuda" } ?: "Bermuda"
                    val entryFee = p["entryFee"]?.toFloatOrNull() ?: 0f
                    val prizePool = p["prizePool"]?.toFloatOrNull() ?: (entryFee * 7f)
                    val maxPlayers = p["maxPlayers"]?.toIntOrNull() ?: when (category) {
                        "CS" -> if (format.contains("1V1")) 2 else 8
                        "LONE_WOLF" -> 2
                        else -> if (format == "SOLO") 48 else if (format == "DUO") 48 else 48
                    }

                    val prefix = when (category) {
                        "CS" -> "FF-CS"
                        "LONE_WOLF" -> "FF-LW"
                        "SCRIMS" -> "FF-SC"
                        else -> "FF-BR"
                    }
                    val tournamentId = "$prefix-${System.currentTimeMillis().toString().takeLast(6)}"
                    val title = p["title"]?.trim()?.ifBlank { null }
                        ?: "$category $format $map Championship"

                    val tournament = Tournament(
                        id = tournamentId,
                        title = title,
                        category = category,
                        format = format,
                        map = map,
                        entryFee = entryFee,
                        prizePool = prizePool,
                        maxPlayers = maxPlayers,
                        registeredPlayers = 0,
                        status = "UPCOMING",
                        game = "Free Fire",
                        rules = p["rules"]?.ifBlank { null } ?: "Standard Free Fire Competitive Esports Rules. Emulators & hacks strictly forbidden. Room ID provided 15m before start."
                    )

                    repository.createTournament(tournament, adminEmail, adminUid)
                    repository.logAuditAction(
                        action = "AI Agent: Created Tournament $tournamentId",
                        details = "$title | Cat: $category | Fee: ₹$entryFee | Prize: ₹$prizePool",
                        category = "AI_COMMAND"
                    )

                    ExecutionResult(
                        success = true,
                        message = "✅ Tournament \"$title\" created successfully with ID: $tournamentId! Synced to both RTDB and Firestore.",
                        createdTournament = tournament
                    )
                }

                AgentActionType.UPDATE_ROOM_DETAILS -> {
                    val tournamentId = action.params["tournamentId"] ?: ""
                    val roomId = action.params["roomId"] ?: ""
                    val password = action.params["roomPassword"] ?: ""

                    if (tournamentId.isBlank() || roomId.isBlank()) {
                        return@withContext ExecutionResult(false, "Tournament ID and Room ID are required.")
                    }

                    val roomDetails = RoomDetails(
                        roomId = roomId,
                        roomPassword = password,
                        updatedAt = System.currentTimeMillis()
                    )

                    // Update in RTDB & Firestore via repository
                    repository.database.child("tournaments").child(tournamentId).child("roomDetails").setValue(roomDetails)
                    repository.firestore.collection("tournaments").document(tournamentId).update(
                        mapOf(
                            "roomDetails.roomId" to roomId,
                            "roomDetails.roomPassword" to password,
                            "roomDetails.updatedAt" to System.currentTimeMillis()
                        )
                    )

                    repository.logAuditAction(
                        action = "AI Agent: Updated Room Details for $tournamentId",
                        details = "Room ID: $roomId set by AI Command",
                        category = "AI_COMMAND"
                    )

                    ExecutionResult(
                        success = true,
                        message = "✅ Room details updated! Room ID: $roomId distributed to registered players in $tournamentId."
                    )
                }

                AgentActionType.UPDATE_TOURNAMENT -> {
                    val tournamentId = action.params["tournamentId"] ?: ""
                    if (tournamentId.isBlank()) {
                        return@withContext ExecutionResult(false, "Tournament ID is required.")
                    }

                    val updates = mutableMapOf<String, Any>()
                    action.params["title"]?.let { if (it.isNotBlank()) updates["title"] = it }
                    action.params["status"]?.let { if (it.isNotBlank()) updates["status"] = it.uppercase() }
                    action.params["entryFee"]?.toFloatOrNull()?.let { updates["entryFee"] = it }
                    action.params["prizePool"]?.toFloatOrNull()?.let { updates["prizePool"] = it }

                    if (updates.isEmpty()) {
                        return@withContext ExecutionResult(false, "No valid updates specified.")
                    }

                    repository.database.child("tournaments").child(tournamentId).updateChildren(updates)
                    repository.firestore.collection("tournaments").document(tournamentId).update(updates)

                    repository.logAuditAction(
                        action = "AI Agent: Modified Tournament $tournamentId",
                        details = updates.toString(),
                        category = "AI_COMMAND"
                    )

                    ExecutionResult(
                        success = true,
                        message = "✅ Tournament $tournamentId updated with new parameters: $updates"
                    )
                }

                AgentActionType.DELETE_TOURNAMENT -> {
                    val tournamentId = action.params["tournamentId"] ?: ""
                    val reason = action.params["reason"] ?: "Removed by AI Command Admin"

                    if (tournamentId.isBlank()) {
                        return@withContext ExecutionResult(false, "Tournament ID is required.")
                    }

                    repository.deleteTournament(tournamentId, adminEmail, adminUid)
                    repository.logAuditAction(
                        action = "AI Agent: Deleted Tournament $tournamentId",
                        details = "Reason: $reason",
                        category = "AI_COMMAND"
                    )

                    ExecutionResult(
                        success = true,
                        message = "🗑️ Tournament $tournamentId successfully removed from active rotation."
                    )
                }

                AgentActionType.BAN_PLAYER -> {
                    val target = action.params["target"] ?: ""
                    val reason = action.params["reason"] ?: "Anti-Cheat violation flagged by Admin AI Command"

                    if (target.isBlank()) {
                        return@withContext ExecutionResult(false, "Target player UID or email is required.")
                    }

                    repository.banPlayer(
                        uid = target,
                        email = if (target.contains("@")) target else "",
                        reason = reason,
                        bannedBy = adminEmail,
                        gameId = target
                    )

                    repository.logAuditAction(
                        action = "AI Agent: Banned Player $target",
                        details = "Reason: $reason",
                        category = "AI_COMMAND"
                    )

                    ExecutionResult(
                        success = true,
                        message = "🛡️ Player $target has been blacklisted and banned from all tournament matchmaking."
                    )
                }

                AgentActionType.UNBAN_PLAYER -> {
                    val target = action.params["target"] ?: ""
                    if (target.isBlank()) {
                        return@withContext ExecutionResult(false, "Target player UID or email is required.")
                    }

                    repository.unbanPlayer(
                        uid = target,
                        email = if (target.contains("@")) target else "",
                        gameId = target
                    )

                    repository.logAuditAction(
                        action = "AI Agent: Unbanned Player $target",
                        details = "Unbanned by AI Command Admin",
                        category = "AI_COMMAND"
                    )

                    ExecutionResult(
                        success = true,
                        message = "✅ Player $target has been reinstated into matchmaking."
                    )
                }

                AgentActionType.RESOLVE_TICKET -> {
                    val ticketId = action.params["ticketId"] ?: ""
                    val note = action.params["note"] ?: "Resolved by AI Command Assistant"
                    val status = action.params["status"] ?: "resolved"

                    if (ticketId.isBlank()) {
                        return@withContext ExecutionResult(false, "Ticket ID is required.")
                    }

                    val ticket = SupportTicket(
                        id = ticketId,
                        status = status,
                        adminNote = note,
                        updatedAt = System.currentTimeMillis()
                    )
                    repository.updateSupportTicket(ticket)

                    repository.logAuditAction(
                        action = "AI Agent: Resolved Ticket $ticketId",
                        details = "Status: $status | Note: $note",
                        category = "AI_COMMAND"
                    )

                    ExecutionResult(
                        success = true,
                        message = "🎫 Support Ticket $ticketId updated to $status."
                    )
                }

                AgentActionType.BROADCAST_ANNOUNCEMENT -> {
                    val title = action.params["title"] ?: "Tournament Alert"
                    val message = action.params["message"] ?: ""

                    if (message.isBlank()) {
                        return@withContext ExecutionResult(false, "Announcement message is required.")
                    }

                    val banner = AppAnnouncementBanner(
                        id = "ann_${System.currentTimeMillis()}",
                        title = title,
                        content = message,
                        bannerType = "ALERT",
                        isActive = true
                    )
                    repository.saveBanner(banner)

                    repository.logAuditAction(
                        action = "AI Agent: Broadcast Announcement",
                        details = "$title: $message",
                        category = "AI_COMMAND"
                    )

                    ExecutionResult(
                        success = true,
                        message = "📢 Global Announcement \"$title\" broadcasted to all active players."
                    )
                }

                else -> {
                    ExecutionResult(false, "Action ${action.actionType} is not directly executable.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing action ${action.actionType}: ${e.message}", e)
            ExecutionResult(false, "Failed to execute: ${e.message}")
        }
    }
}
