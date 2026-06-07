package com.example.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    @SerialName("id")
    val id: String,
    @SerialName("username")
    val username: String,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("role")
    val role: String = "player" // Using string for roles (e.g., admin, player)
)

@Serializable
data class Tournament(
    @SerialName("id")
    val id: String,
    @SerialName("title")
    val title: String,
    @SerialName("game")
    val game: String,
    @SerialName("entry_fee")
    val entryFee: Float,
    @SerialName("prize_pool")
    val prizePool: Float,
    @SerialName("registered_players")
    val registeredPlayers: Int,
    @SerialName("max_players")
    val maxPlayers: Int,
    @SerialName("status")
    val status: String,
    @SerialName("starts_at")
    val startsAt: String? = null,
    @SerialName("ends_at")
    val endsAt: String? = null,
    @SerialName("time_zone")
    val timeZone: String? = null,
    @SerialName("phase")
    val phase: String? = null,
    @SerialName("eligibility_criteria")
    val eligibilityCriteria: String? = null,
    @SerialName("game_version")
    val gameVersion: String? = null,
    @SerialName("rules")
    val rules: String? = null
)

@Serializable
data class PlayerRegistration(
    @SerialName("id")
    val id: String,
    @SerialName("tournament_id")
    val tournamentId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("status")
    val status: String, // pending, approved, rejected
    @SerialName("game_account_id")
    val gameAccountId: String? = null,
    @SerialName("region")
    val region: String? = null,
    @SerialName("age")
    val age: Int? = null,
    @SerialName("profiles")
    val profile: UserProfile? = null,
    @SerialName("tournaments")
    val tournament: Tournament? = null 
)

@Serializable
data class LeaderboardEntry(
    @SerialName("id")
    val id: String,
    @SerialName("tournament_id")
    val tournamentId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("score")
    val score: Int,
    @SerialName("rank")
    val rank: Int? = null,
    // Joined field typically coming from postgrest joins
    @SerialName("profiles")
    val profile: UserProfile? = null
)

@Serializable
data class Match(
    @SerialName("id")
    val id: String,
    @SerialName("tournament_id")
    val tournamentId: String,
    @SerialName("round")
    val round: Int,
    @SerialName("match_number")
    val matchNumber: Int,
    @SerialName("player1_id")
    val player1Id: String? = null,
    @SerialName("player2_id")
    val player2Id: String? = null,
    @SerialName("winner_id")
    val winnerId: String? = null,
    @SerialName("status")
    val status: String,
    @SerialName("score1")
    val score1: Int? = null,
    @SerialName("score2")
    val score2: Int? = null
)
