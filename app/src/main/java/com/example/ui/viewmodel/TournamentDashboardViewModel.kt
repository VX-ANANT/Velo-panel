package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.TournamentRepositoryImpl
import com.example.domain.model.Tournament
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DashboardState {
    object Loading : DashboardState()
    data class Success(
        val tournaments: List<Tournament>,
        val pendingRegistrationsCount: Int = 0,
        val totalRegistrationsCount: Int = 0,
        val payoutPool: Float = 0f,
        val currentUserEmail: String? = null
    ) : DashboardState()
    data class Error(val message: String) : DashboardState()
}

class TournamentDashboardViewModel(
    private val repository: TournamentRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardState>(DashboardState.Loading)
    val uiState: StateFlow<DashboardState> = _uiState.asStateFlow()

    init {
        fetchTournaments()
    }

    fun fetchTournaments() {
        viewModelScope.launch {
            _uiState.value = DashboardState.Loading
            try {
                val tournaments = repository.getAvailableTournaments()
                val pendingCount = repository.getPendingRegistrations().size
                val payout = tournaments.sumOf { it.prizePool.toDouble() }.toFloat()
                val userEmail = repository.supabase.auth.currentSessionOrNull()?.user?.email
                
                _uiState.value = DashboardState.Success(
                    tournaments = tournaments,
                    pendingRegistrationsCount = pendingCount,
                    totalRegistrationsCount = tournaments.sumOf { it.registeredPlayers },
                    payoutPool = payout,
                    currentUserEmail = userEmail
                )
                
                repository.getLiveTournamentsStream().collect { realtimeTournaments ->
                    if (realtimeTournaments.isNotEmpty()) {
                        val currentPending = repository.getPendingRegistrations().size
                        val runPayout = realtimeTournaments.sumOf { it.prizePool.toDouble() }.toFloat()
                        _uiState.value = DashboardState.Success(
                            tournaments = realtimeTournaments,
                            pendingRegistrationsCount = currentPending,
                            totalRegistrationsCount = realtimeTournaments.sumOf { it.registeredPlayers },
                            payoutPool = runPayout,
                            currentUserEmail = userEmail
                        )
                    }
                }
            } catch (e: Exception) {
                // If realtime fails, at least we might have fetched the initial list or an error
                if (_uiState.value !is DashboardState.Success) {
                    _uiState.value = DashboardState.Error(e.message ?: "Unknown error occurred")
                }
            }
        }
    }

    fun createTournament(tournament: Tournament) {
        viewModelScope.launch {
            try {
                repository.createTournament(tournament)
                // The flow will automatically update the UI state when it's created.
            } catch (e: Exception) {
                _uiState.value = DashboardState.Error(e.message ?: "Failed to create tournament")
            }
        }
    }

    fun updateTournament(tournament: Tournament) {
        viewModelScope.launch {
            try {
                repository.updateTournament(tournament)
                fetchTournaments() // Refresh list
            } catch (e: Exception) {
                _uiState.value = DashboardState.Error(e.message ?: "Failed to update tournament")
            }
        }
    }

    companion object {
        fun provideFactory(
            repository: TournamentRepositoryImpl
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(TournamentDashboardViewModel::class.java)) {
                    return TournamentDashboardViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
