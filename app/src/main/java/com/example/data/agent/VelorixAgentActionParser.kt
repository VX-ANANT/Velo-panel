package com.example.data.agent

data class ParsedAgentResponse(
    val displayText: String,
    val action: VelorixAgentAction?
)

object VelorixAgentActionParser {

    fun parse(rawResponse: String): ParsedAgentResponse {
        var text = rawResponse
        var parsedAction: VelorixAgentAction? = null

        try {
            val actionBlockRegex = Regex("```(?:velorix_action|json)?\\s*\\{([\\s\\S]*?\"action\"[\\s\\S]*?)\\}\\s*```", RegexOption.IGNORE_CASE)
            val match = actionBlockRegex.find(rawResponse)

            if (match != null) {
                val jsonString = "{" + match.groupValues[1] + "}"

                // Pure-Kotlin regex extraction for robust execution everywhere
                val actionMatch = Regex("\"action\"\\s*:\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE).find(jsonString)
                val summaryMatch = Regex("\"summary\"\\s*:\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE).find(jsonString)

                val actionName = actionMatch?.groupValues?.get(1)?.trim()?.uppercase() ?: ""
                val summary = summaryMatch?.groupValues?.get(1)?.trim() ?: "System Action Requested"

                val paramsMap = mutableMapOf<String, String>()
                val paramsBlockMatch = Regex("\"params\"\\s*:\\s*\\{([\\s\\S]*?)\\}", RegexOption.IGNORE_CASE).find(jsonString)
                if (paramsBlockMatch != null) {
                    val keyValRegex = Regex("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"")
                    keyValRegex.findAll(paramsBlockMatch.groupValues[1]).forEach { kv ->
                        paramsMap[kv.groupValues[1]] = kv.groupValues[2]
                    }
                }

                val actionType = try {
                    AgentActionType.valueOf(actionName)
                } catch (_: Exception) {
                    AgentActionType.UNKNOWN
                }

                if (actionType != AgentActionType.UNKNOWN) {
                    parsedAction = VelorixAgentAction(
                        actionType = actionType,
                        params = paramsMap,
                        summary = summary
                    )
                }

                // Remove the action codeblock from the user-visible display text
                text = rawResponse.replace(match.value, "").trim()
            }
        } catch (_: Throwable) {}

        return ParsedAgentResponse(
            displayText = text.ifBlank { "Action prepared for your review." },
            action = parsedAction
        )
    }
}
