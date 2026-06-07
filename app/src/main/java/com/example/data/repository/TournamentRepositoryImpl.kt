package com.example.data.repository

import com.example.domain.model.LeaderboardEntry
import com.example.domain.model.PlayerRegistration
import com.example.domain.model.Tournament
import com.example.domain.model.Match
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TournamentRepositoryImpl(
    val supabase: SupabaseClient
) {
    suspend fun getLiveTournamentsStream(): Flow<List<Tournament>> {
        val channel = supabase.channel("public:tournaments")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "tournaments"
        }
        
        channel.subscribe()
        
        return changes.map {
            getAvailableTournaments()
        }
    }

    suspend fun getAvailableTournaments(): List<Tournament> {
        return try {
            supabase.postgrest["tournaments"]
                .select()
                .decodeList<Tournament>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getPendingRegistrations(): List<PlayerRegistration> {
        return try {
            supabase.postgrest["transactions"]
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("*, profiles(*), tournaments(*)")) {
                    filter { eq("status", "pending") }
                }
                .decodeList<PlayerRegistration>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateRegistrationStatus(registrationId: String, status: String) {
        // Create an anonymous inner class mapping or generic parameter logic 
        // to update "status". 
        @kotlinx.serialization.Serializable
        data class RegistrationUpdate(val status: String)
        
        supabase.postgrest["transactions"]
            .update(RegistrationUpdate(status)) {
                filter { eq("id", registrationId) }
            }
    }

    suspend fun createTournament(tournament: Tournament) {
        supabase.postgrest["tournaments"]
            .insert(tournament)
    }

    suspend fun updateTournament(tournament: Tournament) {
        supabase.postgrest["tournaments"]
            .update(tournament) {
                filter { eq("id", tournament.id) }
            }
    }

    suspend fun getMatches(tournamentId: String): List<Match> {
        return try {
            supabase.postgrest["matches"]
                .select {
                    filter { eq("tournament_id", tournamentId) }
                }
                .decodeList<Match>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateMatch(match: Match) {
        supabase.postgrest["matches"]
            .update(match) {
                filter { eq("id", match.id) }
            }
    }

    suspend fun getLiveMatchesStream(tournamentId: String): Flow<List<Match>> {
        val channel = supabase.channel("public:matches")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "matches"
            filter("tournament_id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.EQ, tournamentId)
        }
        
        channel.subscribe()
        
        return changes.map {
            getMatches(tournamentId)
        }
    }

    suspend fun getLiveLeaderboardStream(tournamentId: String): Flow<List<LeaderboardEntry>> {
        // Initial fetch logic omitted for brevity in stream, normally we'd merge initial state with realtime updates
        // Here we just attach to the channel for realtime inserts/updates
        val channel = supabase.channel("public:leaderboard")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "leaderboard"
            filter("tournament_id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.EQ, tournamentId)
        }
        
        channel.subscribe()
        
        // This is a simplified flow, mapping raw postgres changes. 
        // In a real app we'd accumulate changes in a state or refetch. 
        return changes.map { 
           // When a change arrives we refetch the leaderboard to keep it simple and accurate
           supabase.postgrest["leaderboard"]
                .select(columns = Columns.Companion.raw("*, profiles(*)")) {
                    filter {
                        eq("tournament_id", tournamentId)
                    }
                }
                .decodeList<LeaderboardEntry>()
                .sortedByDescending { entry -> entry.score }
        }
    }
}
