package com.example

import com.example.data.agent.AgentActionType
import com.example.data.agent.VelorixAgentActionParser
import com.example.data.agent.VelorixLocalAgentEngine
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testOutOfScopeGuardrail() {
        val outOfScopeQuery = "Can you give me a recipe for chocolate cake?"
        val response = VelorixLocalAgentEngine.processMessage(outOfScopeQuery)
        assertTrue(response.contains("Out of App Scope"))
    }

    @Test
    fun testCreateTournamentIntentAndParsing() {
        val prompt = "Add a tournament for CS 4v4 category in Bermuda map with entry fee ₹50 and prize pool ₹350"
        val response = VelorixLocalAgentEngine.processMessage(prompt)

        assertTrue(response.contains("Tournament Draft Configured"))
        val parsed = VelorixAgentActionParser.parse(response)
        assertNotNull(parsed.action)
        assertEquals(AgentActionType.CREATE_TOURNAMENT, parsed.action?.actionType)
        assertEquals("CS", parsed.action?.params?.get("category"))
        assertEquals("4v4", parsed.action?.params?.get("format"))
        assertEquals("Bermuda", parsed.action?.params?.get("map"))
        assertEquals("50", parsed.action?.params?.get("entryFee"))
        assertEquals("350", parsed.action?.params?.get("prizePool"))
    }

    @Test
    fun testUpdateRoomDetailsIntentAndParsing() {
        val prompt = "Set room ID 98214 and password 1234 for CS match"
        val response = VelorixLocalAgentEngine.processMessage(prompt, "Live tournament ID: tourney_cs_99")
        val parsed = VelorixAgentActionParser.parse(response)

        assertNotNull(parsed.action)
        assertEquals(AgentActionType.UPDATE_ROOM_DETAILS, parsed.action?.actionType)
        assertEquals("98214", parsed.action?.params?.get("roomId"))
        assertEquals("1234", parsed.action?.params?.get("roomPassword"))
    }

    @Test
    fun testBanPlayerIntentAndParsing() {
        val prompt = "Ban user uid_test_88 for speed hack"
        val response = VelorixLocalAgentEngine.processMessage(prompt)
        val parsed = VelorixAgentActionParser.parse(response)

        assertNotNull(parsed.action)
        assertEquals(AgentActionType.BAN_PLAYER, parsed.action?.actionType)
        assertEquals("uid_test_88", parsed.action?.params?.get("target"))
    }
}
