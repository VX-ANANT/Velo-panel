package com.example.data.agent

import kotlinx.serialization.Serializable

enum class AgentActionType {
    CREATE_TOURNAMENT,
    UPDATE_TOURNAMENT,
    DELETE_TOURNAMENT,
    UPDATE_ROOM_DETAILS,
    BAN_PLAYER,
    UNBAN_PLAYER,
    RESOLVE_TICKET,
    BROADCAST_ANNOUNCEMENT,
    SEARCH_DATA,
    UNKNOWN
}

enum class ActionExecutionStatus {
    PENDING_CONFIRMATION,
    EXECUTING,
    COMPLETED,
    REJECTED,
    FAILED
}

@Serializable
data class VelorixAgentAction(
    val id: String = "act_${System.currentTimeMillis()}_${(100..999).random()}",
    val actionType: AgentActionType,
    val params: Map<String, String>,
    val summary: String,
    var status: ActionExecutionStatus = ActionExecutionStatus.PENDING_CONFIRMATION,
    var resultMessage: String? = null
)
