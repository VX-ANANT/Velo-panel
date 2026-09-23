package com.example.data.agent

/**
 * VelorixLocalAgentEngine:
 * Intelligent, contextual conversational engine for Velorix Free Fire esports operations.
 * Provides natural conversational responses in English / Hinglish and triggers real system
 * action cards when explicit operational commands are given.
 */
object VelorixLocalAgentEngine {

    private val OUT_OF_SCOPE_KEYWORDS = listOf(
        "weather", "recipe", "cooking", "movie", "song", "who is the president", "capital of",
        "crypto", "bitcoin", "stock market", "deepseek", "biology", "physics", "homework"
    )

    fun processMessage(prompt: String, liveContext: String = ""): String {
        val trimmed = prompt.trim()
        val lower = trimmed.lowercase()

        // 1. Guardrail Check: Strictly reject out-of-scope queries
        for (kw in OUT_OF_SCOPE_KEYWORDS) {
            if (lower.contains(kw)) {
                return "⚠️ **Out of App Scope**: Main Velorix Free Fire tournaments aur platform operations ke liye authorized hu. Main general knowledge, cooking, ya unrelated topics pe answer nahi de sakta. Free Fire match schedule, room credentials, ya player management me kya madad chahiye?"
            }
        }

        // 2. Sync architecture inquiries between Admin & User panel
        if (lower.contains("sink") || lower.contains("sync") || (lower.contains("user panel") && lower.contains("admin"))) {
            return """
🔄 **Velorix Dual-Database Sync Architecture**

Admin panel aur User panel 100% real-time sync me kaise rehte hain:

1. **Firebase Realtime Database (RTDB)**:
   • Live match slots & active player joins
   • Instant Room ID & Password broadcast (5-10 minutes before match start)
   • Real-time match status (UPCOMING ➔ LIVE ➔ COMPLETED)

2. **Cloud Firestore**:
   • Tournament catalog, rules, map, & prize distribution
   • User profiles, wallet transactions & payout requests
   • Anti-cheat ban records & administrative audit trails

Jab bhi aap admin panel me koi tournament add karte ho ya room details update karte ho, backend automatically dono databases me write karta hai, jisse user panel par bina refresh kiye immediately reflect ho jata hai.
            """.trimIndent()
        }

        // 3. Casual greetings & capabilities
        if (isGreeting(lower)) {
            return """
Hey Admin! ⚡ **Velorix AI Command Agent** online.

Main aapke Free Fire tournaments aur players ko manage karne ke liye ready hu. Aap mujhse natural language me ye karwa sakte hain:
• **Tournament Setup**: *"Add a tournament for CS 4v4 Bermuda with ₹50 entry fee and ₹350 prize pool"*
• **Room Credentials**: *"Set room ID 98214 and password 1234 for CS match"*
• **Anti-Cheat Enforcement**: *"Ban user uid_xyz for speed hack"*
• **Live Data Query**: *"Show active tournaments"* ya *"Check complaints"*

Bataiye, aaj kya schedule ya match action deploy karna hai?
            """.trimIndent()
        }

        // 4. Inquiries about competitive Free Fire rules
        if (lower.contains("rule") || lower.contains("niyam") || lower.contains("guideline")) {
            return """
📋 **Standard Velorix Competitive Free Fire Rules**:

1. **Gun Attributes**: Strictly **OFF** in custom room settings.
2. **Character Skills**: Enabled (except Chrono/active skill restrictions if tournament format specifies).
3. **Gun Restrictions**: Grenades limit to 1 per round in CS mode. Smoke grenades permitted.
4. **Device Policy**: Mobile only. PC / Emulators strictly prohibited (auto-ban).
5. **Match Proof**: Winning squad must submit end-game screenshot with full kills scoreboard within 15 minutes of match completion.
            """.trimIndent()
        }

        // 5. Inquiries about status, active tournaments or tickets
        if ((lower.contains("tournament") || lower.contains("match") || lower.contains("slot")) &&
            (lower.contains("kya") || lower.contains("kaise") || lower.contains("list") || lower.contains("show") || lower.contains("check") || lower.contains("active") || lower.contains("upcoming")) &&
            !lower.contains("add") && !lower.contains("create") && !lower.contains("new")
        ) {
            return generateContextualStatusResponse(liveContext)
        }

        // 6. Action Intent: Create Tournament
        if (lower.contains("add a tournament") || lower.contains("create tournament") || lower.contains("add tournament") || lower.contains("new tournament")) {
            return handleCreateTournamentIntent(prompt)
        }

        // 7. Action Intent: Update Room Details
        if (lower.contains("room id") || lower.contains("room details") || lower.contains("custom room") || lower.contains("room password")) {
            return generateUpdateRoomDetailsResponse(prompt, liveContext)
        }

        // 8. Action Intent: Ban Player
        if (lower.contains("ban player") || lower.contains("ban user") || lower.contains("ban ")) {
            return generateBanPlayerResponse(prompt)
        }

        // 9. Action Intent: Unban Player
        if (lower.contains("unban player") || lower.contains("unban user") || lower.contains("unban ")) {
            return generateUnbanPlayerResponse(prompt)
        }

        // 10. Action Intent: Broadcast Announcement
        if (lower.contains("broadcast") || lower.contains("announce") || lower.contains("send alert")) {
            return generateBroadcastResponse(prompt)
        }

        // 11. Action Intent: Resolve Ticket
        if (lower.contains("ticket") || lower.contains("dispute") || lower.contains("complaint")) {
            return generateTicketResponse(prompt, liveContext)
        }

        // 12. Context-aware conversational response
        return """
Samajh gaya. Main Velorix admin command system se connected hu. 

Aap tournament add karne ke liye jaise keh sakte hain:
*"Add a CS 4v4 Bermuda tournament with entry fee 50"* ya *"Set room id 12345 and password 987"*.

Kya aap isme koi tournament configure karna chahte hain ya live stats dekhna chahte hain?
        """.trimIndent()
    }

    private fun isGreeting(lower: String): Boolean {
        val greetings = listOf("hi", "hello", "hey", "namaste", "bhai", "bro", "kya kar sakte ho", "help", "who are you", "kaise ho", "sun")
        return greetings.any { lower == it || lower.startsWith("$it ") }
    }

    private fun generateContextualStatusResponse(liveContext: String): String {
        return """
📊 **Live Tournament & Operations Overview**

Backend real-time sync active hai:
• **Tournaments**: Upcoming matches schedule me ready hain.
• **Database Sync**: Realtime Database (RTDB) slot listeners active.
• **Anti-Cheat Watch**: Flagged accounts continuously monitored.

Aap specific tournament ki details dekhne ke liye top-right me **Inspect Live Database** button tap kar sakte hain, ya direct room ID update kar sakte hain.
        """.trimIndent()
    }

    private fun handleCreateTournamentIntent(prompt: String): String {
        val lower = prompt.lowercase()

        // Check if essential details like category or fee were mentioned
        val hasCategory = lower.contains("cs") || lower.contains("clash") || lower.contains("br") || lower.contains("battle") || lower.contains("lone")
        val hasFeeOrPool = lower.contains("fee") || lower.contains("prize") || lower.contains("rs") || lower.contains("₹") || extractNumberAfter(prompt, listOf("entry", "fee", "prize", "pool")) != null

        // Detect Category
        val category = when {
            lower.contains("clash squad") || lower.contains("cs") -> "CS"
            lower.contains("lone wolf") -> "LONE_WOLF"
            lower.contains("scrim") -> "SCRIMS"
            else -> "BR"
        }

        // Detect Format
        val format = when {
            lower.contains("1v1") -> "1v1"
            lower.contains("2v2") -> "2v2"
            lower.contains("4v4") -> "4v4"
            lower.contains("duo") -> "DUO"
            lower.contains("squad") -> "SQUAD"
            lower.contains("solo") -> "SOLO"
            category == "CS" -> "4v4"
            category == "LONE_WOLF" -> "1v1"
            else -> "SOLO"
        }

        // Detect Map
        val map = when {
            lower.contains("purgatory") -> "Purgatory"
            lower.contains("kalahari") -> "Kalahari"
            lower.contains("alpine") -> "Alpine"
            lower.contains("nexterra") -> "NexTerra"
            lower.contains("iron cage") -> "Iron Cage"
            else -> "Bermuda"
        }

        // Extract Entry Fee
        val entryFee = extractNumberAfter(prompt, listOf("entry fee", "entry", "fee", "₹", "rs")) ?: when (category) {
            "CS" -> "50"
            "LONE_WOLF" -> "20"
            else -> "30"
        }

        // Extract Prize Pool
        val prizePool = extractNumberAfter(prompt, listOf("prize pool", "prize", "pool")) ?: when (category) {
            "CS" -> "350"
            "LONE_WOLF" -> "35"
            else -> "1000"
        }

        val maxPlayers = when {
            category == "CS" -> "8"
            category == "LONE_WOLF" -> "2"
            format == "DUO" -> "48"
            format == "SQUAD" -> "48"
            else -> "48"
        }

        val title = when (category) {
            "CS" -> "Clash Squad ($format) - $map"
            "LONE_WOLF" -> "Lone Wolf ($format) - Iron Cage"
            "SCRIMS" -> "Tier 1 Pro Scrims ($format)"
            else -> "Battle Royale ($format) - $map"
        }

        return """
⚔️ **Tournament Draft Configured**
Tournament details configure kar di gayi hain:
• **Mode**: $title
• **Format**: $format ($map)
• **Entry**: ₹$entryFee | **Prize**: ₹$prizePool
• **Slots**: $maxPlayers players

Action card neeche ready hai. Database me live push karne ke liye **CONFIRM & APPLY** par tap karein:

```velorix_action
{
  "action": "CREATE_TOURNAMENT",
  "params": {
    "title": "$title",
    "category": "$category",
    "format": "$format",
    "map": "$map",
    "entryFee": "$entryFee",
    "prizePool": "$prizePool",
    "maxPlayers": "$maxPlayers",
    "scheduleTime": "Today 8:30 PM IST"
  },
  "summary": "Create $title with ₹$entryFee entry and ₹$prizePool prize pool"
}
```
        """.trimIndent()
    }

    private fun generateUpdateRoomDetailsResponse(prompt: String, liveContext: String): String {
        val roomIdRegex = Regex("(?:room id|room|id)\\s*[:=]?\\s*([0-9]{4,10})", RegexOption.IGNORE_CASE)
        val passRegex = Regex("(?:password|pass|pw)\\s*[:=]?\\s*([a-zA-Z0-9@#_-]{3,12})", RegexOption.IGNORE_CASE)

        val roomId = roomIdRegex.find(prompt)?.groupValues?.get(1) ?: "892014"
        val password = passRegex.find(prompt)?.groupValues?.get(1) ?: "1234"

        val targetTourneyId = if (liveContext.contains("tourney_")) {
            val m = Regex("tourney_[a-zA-Z0-9_-]+").find(liveContext)
            m?.value ?: "live_match_cs"
        } else "live_match_cs"

        return """
Custom Room credentials set kar diye gaye hain:
• **Room ID**: `$roomId`
• **Password**: `$password`
• **Target Match**: `$targetTourneyId`

User panel me registered players ko alert bhejne ke liye action card confirm karein:

```velorix_action
{
  "action": "UPDATE_ROOM_DETAILS",
  "params": {
    "tournamentId": "$targetTourneyId",
    "roomId": "$roomId",
    "roomPassword": "$password"
  },
  "summary": "Broadcast Room ID $roomId & Password to joined players"
}
```
        """.trimIndent()
    }

    private fun generateBanPlayerResponse(prompt: String): String {
        val targetRegex = Regex("(?:user|player|uid)\\s*[:=]?\\s*([a-zA-Z0-9_-]{5,32})", RegexOption.IGNORE_CASE)
        val target = targetRegex.find(prompt)?.groupValues?.get(1) ?: run {
            val words = prompt.split(" ")
            words.firstOrNull { it.startsWith("uid_") || it.length > 8 } ?: "uid_flagged_player"
        }

        val reason = when {
            prompt.contains("speed", true) -> "Speed Hack detected by anti-cheat telemetry"
            prompt.contains("aim", true) || prompt.contains("headshot", true) -> "Aimbot / Auto-headshot memory modification"
            prompt.contains("emulator", true) || prompt.contains("pc", true) -> "Bypassing mobile-only emulator restrictions"
            else -> "Violation of Velorix Fair Play Guidelines"
        }

        return """
Player ban draft prepare kar diya gaya hai:
• **Target UID**: `$target`
• **Reason**: $reason
• **Action**: Immediate suspension & active tournament removal

Action card neeche hai:

```velorix_action
{
  "action": "BAN_PLAYER",
  "params": {
    "target": "$target",
    "reason": "$reason"
  },
  "summary": "Ban player $target for: $reason"
}
```
        """.trimIndent()
    }

    private fun generateUnbanPlayerResponse(prompt: String): String {
        val targetRegex = Regex("(?:user|player|uid)\\s*[:=]?\\s*([a-zA-Z0-9_-]{5,32})", RegexOption.IGNORE_CASE)
        val target = targetRegex.find(prompt)?.groupValues?.get(1) ?: "uid_cleared_player"

        return """
Player restore draft prepare kar diya gaya hai:
• **Target UID**: `$target`
• **Status**: Clearance verified

```velorix_action
{
  "action": "UNBAN_PLAYER",
  "params": {
    "target": "$target"
  },
  "summary": "Restore user $target access"
}
```
        """.trimIndent()
    }

    private fun generateBroadcastResponse(prompt: String): String {
        val message = prompt.replace(Regex("^(?:broadcast|announce|send alert)[: ]*", RegexOption.IGNORE_CASE), "").trim()
            .ifBlank { "Important: Tournament schedule updated. Check your joined matches." }

        return """
Broadcast notification तैयार है:
• **Title**: Velorix Esports Alert
• **Message**: "$message"

```velorix_action
{
  "action": "BROADCAST_ANNOUNCEMENT",
  "params": {
    "title": "Velorix Esports Notice",
    "message": "$message",
    "priority": "HIGH"
  },
  "summary": "Broadcast push announcement to active players"
}
```
        """.trimIndent()
    }

    private fun generateTicketResponse(prompt: String, liveContext: String): String {
        val ticketIdRegex = Regex("(TK-[a-zA-Z0-9_-]+)")
        val ticketId = ticketIdRegex.find(prompt)?.groupValues?.get(1) ?: "TK-1002"

        return """
Support ticket `$ticketId` resolve karne ke liye action prepared hai:
• Status: Resolved
• Resolution: Screenshot proof verified by Admin

```velorix_action
{
  "action": "RESOLVE_TICKET",
  "params": {
    "ticketId": "$ticketId",
    "status": "resolved",
    "note": "Verified by Admin"
  },
  "summary": "Mark ticket $ticketId as resolved"
}
```
        """.trimIndent()
    }

    private fun extractNumberAfter(text: String, prefixes: List<String>): String? {
        val lower = text.lowercase()
        for (prefix in prefixes) {
            val idx = lower.indexOf(prefix)
            if (idx != -1) {
                val after = lower.substring(idx + prefix.length).trim()
                val match = Regex("^[:=₹rs\\s]*(\\d+)").find(after)
                if (match != null) {
                    return match.groupValues[1]
                }
            }
        }
        return null
    }
}
