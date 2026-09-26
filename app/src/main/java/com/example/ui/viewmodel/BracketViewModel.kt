package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.TournamentRepositoryImpl
import com.example.domain.model.Match
import com.example.ui.common.GlobalErrorManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed class BracketState {
    object Loading : BracketState()
    data class Success(val matches: List<Match>) : BracketState()
    data class Error(val message: String) : BracketState()
}

class BracketViewModel(
    private val repository: TournamentRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow<BracketState>(BracketState.Loading)
    val uiState: StateFlow<BracketState> = _uiState.asStateFlow()

    fun loadMatches(tournamentId: String) {
        viewModelScope.launch {
            _uiState.value = BracketState.Loading
            try {
                val initialMatches = try {
                    repository.getMatches(tournamentId)
                } catch (e: Exception) {
                    if (e is CancellationException) return@launch
                    emptyList()
                }
                
                _uiState.value = BracketState.Success(initialMatches)
                
                // Start collecting realtime updates with safe flow catch
                repository.getLiveMatchesStream(tournamentId)
                    .catch { err ->
                        if (err !is CancellationException) {
                            // Non-fatal stream error; keep existing state
                        }
                    }
                    .collect { realtimeMatches ->
                        _uiState.value = BracketState.Success(realtimeMatches)
                    }
            } catch (e: Exception) {
                if (e is CancellationException) return@launch
                val errMsg = e.message ?: "Failed to load matches."
                if (!errMsg.contains("cancelled", ignoreCase = true) && !errMsg.contains("canceled", ignoreCase = true)) {
                    GlobalErrorManager.emitFirestoreError("Tournament Bracket Matches", e, actionLabel = "Retry") {
                        loadMatches(tournamentId)
                    }
                }
                _uiState.value = BracketState.Success(emptyList())
            }
        }
    }

    fun updateMatchStatus(match: Match, newStatus: String, winnerId: String?) {
        viewModelScope.launch {
            try {
                val updated = match.copy(status = newStatus, winnerId = winnerId)
                repository.updateMatch(updated)
                // Flow will handle the state update automatically if connection is successful
            } catch (e: Exception) {
                GlobalErrorManager.emitFirestoreError("Update Match", e)
            }
        }
    }

    fun generateBracket(tournamentId: String) {
        viewModelScope.launch {
            try {
                val participants = repository.getTournamentParticipants(tournamentId)
                repository.generateSingleEliminationBracket(tournamentId, participants)
            } catch (e: Exception) {
                GlobalErrorManager.emitError("Failed to generate bracket: ${e.message}", e)
            }
        }
    }

    fun advancePlayer(
        tournamentId: String,
        sourceMatchId: String?,
        targetMatchId: String,
        playerId: String,
        targetSlot: Int
    ) {
        viewModelScope.launch {
            try {
                repository.advancePlayerInBracket(
                    tournamentId = tournamentId,
                    sourceMatchId = sourceMatchId,
                    targetMatchId = targetMatchId,
                    playerId = playerId,
                    targetSlot = targetSlot
                )
            } catch (e: Exception) {
                GlobalErrorManager.emitFirestoreError("Advance Player in Bracket", e)
            }
        }
    }

    fun recordMatchScore(tournamentId: String, matchId: String, winnerId: String, score1: Int, score2: Int) {
        viewModelScope.launch {
            try {
                repository.recordMatchScore(
                    tournamentId = tournamentId,
                    matchId = matchId,
                    winnerId = winnerId,
                    score1 = score1,
                    score2 = score2,
                    status = "COMPLETED"
                )
            } catch (e: Exception) {
                GlobalErrorManager.emitError("Failed to record score: ${e.message}", e)
            }
        }
    }

    companion object {
        fun provideFactory(
            repository: TournamentRepositoryImpl
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(BracketViewModel::class.java)) {
                    return BracketViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
