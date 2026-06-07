package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.TournamentRepositoryImpl
import com.example.domain.model.Match
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
                val initialMatches = repository.getMatches(tournamentId)
                if (initialMatches.isEmpty()) {
                    _uiState.value = BracketState.Success(emptyList())
                } else {
                    _uiState.value = BracketState.Success(initialMatches)
                }
                
                // Start collecting realtime updates
                repository.getLiveMatchesStream(tournamentId).collect { realtimeMatches ->
                    if (realtimeMatches.isNotEmpty()) {
                        _uiState.value = BracketState.Success(realtimeMatches)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = BracketState.Error(e.message ?: "Failed to load matches.")
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
                // Ignore failure for UI
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
